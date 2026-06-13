package fukaisystem.domain.attendance;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 日次勤怠集計クラス。
 *
 * <h2>責務</h2>
 * <ul>
 *   <li>打刻データ（{@link TimeRecord}）と休日マスタを受け取り、日次集計結果（{@link WorkDaily}）を算出する。</li>
 *   <li>DBアクセス・CSV読み込みは行わない（呼び出し元が注入する）。</li>
 * </ul>
 *
 * <h2>集計ルール</h2>
 * <dl>
 *   <dt>通常勤務</dt>
 *   <dd>08:20〜17:00</dd>
 *
 *   <dt>実労働時間</dt>
 *   <dd>退勤 − 出勤 から以下を自動控除した時間。
 *       ① 昼休み（12:00〜12:45）、② 外出打刻区間、③ 定時後休憩（17:00〜17:15）</dd>
 *
 *   <dt>時間外労働</dt>
 *   <dd>17:15 以降の実労働時間を 30分単位（端数切り捨て）で算出。
 *       36協定の月間・年間監視にもこの値を使用する。</dd>
 *
 *   <dt>深夜労働</dt>
 *   <dd>実労働のうち 22:00〜翌06:00 または 00:00〜06:00 に重なる時間。
 *       残業・休日との重複は独立してカウントする。</dd>
 *
 *   <dt>休日労働</dt>
 *   <dd>法定休日（日曜）または登録済み祝日・所定休日の全実労働時間。</dd>
 *
 *   <dt>日付またぎ</dt>
 *   <dd>退勤 &lt; 出勤 の場合、退勤を翌日時刻（+1440分）として扱う。</dd>
 * </dl>
 *
 * <h2>使用例</h2>
 * <pre>{@code
 *   DailyAggregator aggregator = new DailyAggregator();
 *
 *   // 単件処理
 *   WorkDaily result = aggregator.aggregate(date, isHoliday, isSunday, record);
 *
 *   // バッチ処理
 *   DailyAggregationResult batchResult = aggregator.aggregateAll(records);
 *   batchResult.getSuccessList(); // DB登録へ
 *   batchResult.getErrorList();   // ログ出力・画面表示へ
 * }</pre>
 */
public class DailyAggregator {

    // =========================================================================
    // 定数（すべて「勤務日 00:00 を 0 とした分数」で表現）
    // =========================================================================

    // ---- 昼休み ----------------------------------------------------------------

    /** 昼休み開始: 12:00 = 720分 */
    static final int LUNCH_START = 12 * 60;           // 720

    /** 昼休み終了: 12:45 = 765分 */
    static final int LUNCH_END   = 12 * 60 + 45;      // 765

    // ---- 定時・残業 -------------------------------------------------------------

    /** 定時始業: 8:20 = 500分 */
    static final int STANDARD_START   = 8 * 60 + 20;   // 500

    /** 定時終業: 17:00 = 1020分 */
    static final int STANDARD_END     = 17 * 60;       // 1020

    /** 定時後休憩終了 ＝ 残業開始: 17:15 = 1035分 */
    static final int OVERTIME_START   = 17 * 60 + 15;  // 1035

    /** 残業の計算単位（分）: 30分切り捨て */
    static final int OVERTIME_UNIT    = 30;

    // ---- 深夜時間帯 -------------------------------------------------------------

    /**
     * 深夜時間帯の定義（労働基準法第37条: 22:00〜翌05:00 を本システムでは〜06:00 に拡張）。
     *
     * <p>勤務日の分数表現では以下の2帯域と重なる時間を合算する。
     * <ul>
     *   <li>早朝深夜帯: [0, 360]   →  00:00〜06:00</li>
     *   <li>夜間深夜帯: [1320, 1800] →  22:00〜翌06:00（日付またぎ分を含む）</li>
     * </ul>
     */
    static final int LATE_NIGHT_EARLY_END = 6  * 60;              // 360:  06:00
    static final int LATE_NIGHT_START     = 22 * 60;              // 1320: 22:00
    static final int LATE_NIGHT_END       = 24 * 60 + 6 * 60;     // 1800: 翌06:00

    // ---- その他 ----------------------------------------------------------------

    /** 実労働時間の上限（分）= 24時間。超過はデータ異常とみなす */
    static final int MAX_WORK_MINUTES = 24 * 60;                  // 1440

    // =========================================================================
    // 公開メソッド
    // =========================================================================

    /**
     * 1件の打刻データから日次集計結果を算出する。
     *
     * <p>以下の順で処理する。
     * <ol>
     *   <li>外出打刻による区間分割</li>
     *   <li>昼休み（12:00〜12:45）の自動控除</li>
     *   <li>定時後休憩（17:00〜17:15）の自動控除</li>
     *   <li>実労働時間・時間外・深夜・休日の算出</li>
     * </ol>
     *
     * @param date      打刻日
     * @param isHoliday 休日FLG
     * @param isSunday  日曜FLG
     * @param record    打刻データ（null 不可）
     * @return 集計結果（null を返さない）
     * @throws IllegalArgumentException record が null の場合
     * @throws AggregationException     出勤・退勤の片方が未打刻、外出・戻りの片方が未打刻、
     *                                  打刻の時刻順序が不正、または実労働24時間超の場合
     */
    public WorkDaily aggregate(LocalDate date, boolean isHoliday, boolean isSunday, TimeRecord record) {
        if (record == null) throw new IllegalArgumentException("record は null にできません");

        WorkDaily.Builder builder = WorkDaily.builder()
            .employeeNo(record.getEmployeeNo())
            .workDate(date);

        // 有給
        if (record.isPaidHoliday()) {
            return builder.isPaidHoliday(true).build();
        }
        // 欠勤
        if (record.getClockIn() == null && record.getClockOut() == null) {
            return builder.isAbsence(true).build();
        }

        // 出勤・退勤のどちらかが未打刻 → 不完全データ
        if (record.getClockIn() == null || record.getClockOut() == null) {
            throw new AggregationException(
                AggregationError.Type.MISSING_PUNCH,
                record.getEmployeeNo(),
                date.toString(),
                "出勤・退勤のどちらか一方が未打刻です: clockIn=" + record.getClockIn()
                    + " clockOut=" + record.getClockOut());
        }

        // 外出・戻りの片方のみ → 不完全データ
        if ((record.getGoOut() == null) != (record.getReturnIn() == null)) {
            throw new AggregationException(
                AggregationError.Type.INCOMPLETE_EXTERNAL,
                record.getEmployeeNo(),
                date.toString(),
                "外出・戻りのどちらか一方が未打刻です: goOut=" + record.getGoOut()
                    + " returnIn=" + record.getReturnIn());
        }

        // ---- 1. 外出打刻による区間分割（日付またぎ対応） ----
        List<int[]> intervals = buildPunchIntervals(date, record);

        // ---- 2. 昼休み自動控除（12:00〜12:45） ----
        intervals = deductPeriod(intervals, LUNCH_START, LUNCH_END);

        // ---- 3. 定時後休憩自動控除（17:00〜17:15） ----
        intervals = deductPeriod(intervals, STANDARD_END, OVERTIME_START);

        // ---- 4. 各区分の算出 ----
        int totalMinutes     = sumIntervals(intervals);

        // 24時間超は異常データ
        if (totalMinutes > MAX_WORK_MINUTES) {
            throw new AggregationException(
                AggregationError.Type.EXCESSIVE_WORK_HOURS,
                record.getEmployeeNo(),
                date.toString(),
                "実労働時間が24時間を超えています: " + totalMinutes + "分");
        }

        int overtimeMinutes   = calcOvertimeMinutes(intervals);
        int lateNightMinutes  = calcLateNightMinutes(intervals);

        return builder
                .totalMinutes(totalMinutes)
                .overtimeMinutes(overtimeMinutes)
                .lateNightMinutes(lateNightMinutes)
                .isHoliday(isHoliday)
                .isSunday(isSunday)
                .isBusinessTrip(record.isBusinessTrip())
                .isLateEarly(isLateEarly(record))
                .build();
    }

    /**
     * 日曜日かどうかを調べる。
     *
     * @param date 打刻日
     * @return 日曜日は true, 他の曜日は false
     */
    public boolean isSunday(LocalDate date) {
        return date.getDayOfWeek() == DayOfWeek.SUNDAY;
    }

    /**
     * 遅刻または早退かどうかを調べる。
     *
     * <p>日付またぎ（退勤 &lt; 出勤）の場合は退勤を翌日時刻として扱い、早退とは判定しない。
     * 定時始業（08:20）より後の出勤は深夜シフトであっても遅刻として扱う。
     *
     * @param record 打刻データ（出勤・退勤とも打刻済みであること）
     * @return 遅刻または早退の場合は true, そうでない場合は false
     */
    public boolean isLateEarly(TimeRecord record) {
        int clockInMin  = toMinutes(record.getClockIn());
        int clockOutMin = toMinutes(record.getClockOut());
        // 日付またぎは退勤を翌日時刻に補正してから早退判定する
        if (clockOutMin < clockInMin) clockOutMin += 1440;
        return clockInMin > STANDARD_START || clockOutMin < STANDARD_END;
    }

    /**
     * 複数の打刻データをバッチ集計する。
     * エラーが発生した個別レコードはスキップし、エラーリストに追記して処理を続行する。
     *
     * @param records 1日の全打刻データ（null 不可）
     * @return 成功リストとエラーリストを含む集計結果
     */
    public DailyAggregationResult aggregateAll(DailyRecords records) {
        if (records == null) throw new IllegalArgumentException("records は null にできません");

        List<WorkDaily>        successList = new ArrayList<>();
        List<AggregationError> errorList   = new ArrayList<>();

        for (TimeRecord record : records.getRecords()) {
            LocalDate date = records.getWorkDate();
            boolean isHoliday = records.isHoliday();
            boolean isSunday = isSunday(date);
            try {
                // 休日・日曜で打刻がないレコードは欠勤ではないためスキップする。
                if (
                    (isHoliday || isSunday)
                        && record.getClockIn() == null
                        && record.getClockOut() == null
                ) {
                    continue;
                }
                successList.add(aggregate(date, isHoliday, isSunday, record));
            } catch (AggregationException e) {
                errorList.add(new AggregationError(
                    e.getErrorType(),
                    record.getEmployeeNo(),
                    date.toString(),
                    e.getMessage()));
            }
        }

        return new DailyAggregationResult(successList, errorList);
    }

    // =========================================================================
    // 内部ロジック
    // =========================================================================

    /**
     * 打刻データから外出控除済みの労働区間リストを構築する。
     *
     * <p>外出・戻りがある場合は「出勤〜外出」「戻り〜退勤」の2区間、
     * ない場合は「出勤〜退勤」の1区間を返す。
     *
     * <p>全時刻は「勤務日 00:00 を 0 とした分数」で表現する。
     * 日付またぎ（退勤 &lt; 出勤）は +1440 して翌日分として扱う。
     *
     * <p>昼休み・定時後休憩の控除は {@link #deductPeriod} で後処理する。
     *
     * @return 労働区間のリスト。各要素は {@code int[]{開始分, 終了分}}。
     * @throws AggregationException 時刻順序が不正な場合
     */
    private List<int[]> buildPunchIntervals(LocalDate date, TimeRecord record) {
        int rawClockIn  = toMinutes(record.getClockIn());
        int rawClockOut = toMinutes(record.getClockOut());

        // 出勤と退勤が同時刻 → 日付またぎと区別できないデータ異常として扱う
        if (rawClockOut == rawClockIn) {
            throw new AggregationException(
                AggregationError.Type.INVALID_TIME_ORDER,
                record.getEmployeeNo(),
                date.toString(),
                "出勤時刻と退勤時刻が同一です: clockIn=" + record.getClockIn()
                    + " clockOut=" + record.getClockOut());
        }

        // 退勤 < 出勤 → 日付またぎ（翌日扱い）
        boolean midnightShift = (rawClockOut < rawClockIn);
        int clockInMin  = rawClockIn;
        int clockOutMin = midnightShift ? rawClockOut + 1440 : rawClockOut;

        List<int[]> intervals = new ArrayList<>();

        if (record.getGoOut() != null) {
            int rawGoOut    = toMinutes(record.getGoOut());
            int rawReturnIn = toMinutes(record.getReturnIn());
            int goOutMin, returnInMin;

            if (midnightShift) {
                // 日付またぎシフト: 外出・戻りも単調増加になるよう +1440 補正
                goOutMin    = normalizeAfter(rawGoOut,    clockInMin);
                returnInMin = normalizeAfter(rawReturnIn, goOutMin);
                clockOutMin = normalizeAfter(clockOutMin, returnInMin);
            } else {
                // 通常シフト: 生時刻をそのまま使い、順序エラーは validatePunchOrder で検出する
                // （normalizeAfter で +1440 すると誤打刻が有効扱いになるため使用しない）
                goOutMin    = rawGoOut;
                returnInMin = rawReturnIn;
            }

            validatePunchOrder(date, record, clockInMin, goOutMin, returnInMin, clockOutMin);

            if (goOutMin   > clockInMin)  intervals.add(new int[]{clockInMin,  goOutMin});
            if (clockOutMin > returnInMin) intervals.add(new int[]{returnInMin, clockOutMin});
        } else {
            intervals.add(new int[]{clockInMin, clockOutMin});
        }

        return intervals;
    }

    /**
     * 指定した控除期間 (deductStart, deductEnd) を労働区間リストから除去する
     * （境界がちょうど一致する区間は控除されない）。
     *
     * <p>控除期間と重なる各区間を「控除前」「控除後」の2部分に分割し、
     * 長さゼロの区間は除去して返す。
     *
     * <p>呼び出し例:
     * <pre>
     *   // 昼休み控除
     *   intervals = deductPeriod(intervals, LUNCH_START, LUNCH_END);
     *   // 定時後休憩控除
     *   intervals = deductPeriod(intervals, STANDARD_END, OVERTIME_START);
     * </pre>
     *
     * @param intervals   元の労働区間リスト
     * @param deductStart 控除開始（分）
     * @param deductEnd   控除終了（分）
     * @return 控除後の労働区間リスト（新規リスト）
     */
    private List<int[]> deductPeriod(List<int[]> intervals, int deductStart, int deductEnd) {
        List<int[]> result = new ArrayList<>();
        for (int[] iv : intervals) {
            // 控除開始より前の部分
            if (iv[0] < deductStart) {
                int end = Math.min(iv[1], deductStart);
                if (end > iv[0]) result.add(new int[]{iv[0], end});
            }
            // 控除終了より後の部分
            if (iv[1] > deductEnd) {
                int start = Math.max(iv[0], deductEnd);
                if (iv[1] > start) result.add(new int[]{start, iv[1]});
            }
        }
        return result;
    }

    /**
     * 労働区間リストの合計時間（分）を返す。
     */
    private int sumIntervals(List<int[]> intervals) {
        int total = 0;
        for (int[] iv : intervals) {
            total += iv[1] - iv[0];
        }
        return total;
    }

    /**
     * 時間外労働（分）を算出する。
     *
     * <p>17:15（{@link #OVERTIME_START}）以降の実労働時間を合計し、
     * {@link #OVERTIME_UNIT}（30分）単位で切り捨てる。
     *
     * <p>17:00〜17:15 の定時後休憩は {@link #deductPeriod} で事前に除去済みのため、
     * 本メソッドは 17:15 以降のみを対象とすれば良い。
     *
     * @param intervals 控除済みの労働区間リスト
     * @return 30分単位に切り捨てた時間外労働（分）
     */
    private int calcOvertimeMinutes(List<int[]> intervals) {
        int raw = 0;
        for (int[] iv : intervals) {
            if (iv[1] > OVERTIME_START) {
                raw += iv[1] - Math.max(iv[0], OVERTIME_START);
            }
        }
        return (raw / OVERTIME_UNIT) * OVERTIME_UNIT;
    }

    /**
     * 深夜労働時間（分）を算出する。
     *
     * <p>深夜時間帯は以下の2帯域の和:
     * <ul>
     *   <li>早朝深夜帯: [0, {@value #LATE_NIGHT_EARLY_END}]（00:00〜06:00）</li>
     *   <li>夜間深夜帯: [{@value #LATE_NIGHT_START}, {@value #LATE_NIGHT_END}]（22:00〜翌06:00）</li>
     * </ul>
     *
     * <p>深夜かつ残業、深夜かつ休日の重複は各集計で独立してカウントする（仕様書 4.1.1）。
     *
     * @param intervals 控除済みの労働区間リスト
     * @return 深夜労働時間（分）
     */
    private int calcLateNightMinutes(List<int[]> intervals) {
        int lateNight = 0;
        for (int[] iv : intervals) {
            lateNight += overlap(iv[0], iv[1], 0,         LATE_NIGHT_EARLY_END);
            lateNight += overlap(iv[0], iv[1], LATE_NIGHT_START, LATE_NIGHT_END);
        }
        return lateNight;
    }

    /**
     * 2区間 [aStart, aEnd) と [bStart, bEnd) の重複時間（分）を返す。
     * 重複がない場合は 0。
     */
    private int overlap(int aStart, int aEnd, int bStart, int bEnd) {
        return Math.max(0, Math.min(aEnd, bEnd) - Math.max(aStart, bStart));
    }

    /**
     * {@code value} が {@code baseline} 以下の場合に +1440 (24h) して返す。
     * 外出・戻り・退勤の日付またぎ補正に使用する。
     */
    private int normalizeAfter(int value, int baseline) {
        return (value <= baseline) ? value + 1440 : value;
    }

    /**
     * 打刻時刻の前後順序を検証する（出勤 ≦ 外出 ≦ 戻り ≦ 退勤）。
     *
     * @throws AggregationException 順序が不正な場合
     */
    private void validatePunchOrder(LocalDate date,
                                    TimeRecord rec,
                                    int clockIn, int goOut,
                                    int returnIn, int clockOut) {
        if (clockIn > goOut) {
            throw new AggregationException(
                AggregationError.Type.INVALID_TIME_ORDER, rec.getEmployeeNo(),
                date.toString(),
                "外出時刻が出勤時刻より前です: clockIn=" + rec.getClockIn()
                    + " goOut=" + rec.getGoOut());
        }
        if (goOut > returnIn) {
            throw new AggregationException(
                AggregationError.Type.INVALID_TIME_ORDER, rec.getEmployeeNo(),
                date.toString(),
                "戻り時刻が外出時刻より前です: goOut=" + rec.getGoOut()
                    + " returnIn=" + rec.getReturnIn());
        }
        if (returnIn > clockOut) {
            throw new AggregationException(
                AggregationError.Type.INVALID_TIME_ORDER, rec.getEmployeeNo(),
                date.toString(),
                "退勤時刻が戻り時刻より前です: returnIn=" + rec.getReturnIn()
                    + " clockOut=" + rec.getClockOut());
        }
    }

    /**
     * {@link LocalTime} を 00:00 起点の分数に変換する（秒・ナノ秒は切り捨て）。
     */
    private int toMinutes(LocalTime time) {
        return time.getHour() * 60 + time.getMinute();
    }
}
