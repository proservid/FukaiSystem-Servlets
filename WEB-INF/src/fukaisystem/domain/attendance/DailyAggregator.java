package fukaisystem.domain.attendance;

import java.time.DayOfWeek;
import java.time.LocalDate;
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
 * <h2>就業時間の設定</h2>
 * <p>始業・終業・休憩・残業開始・丸め単位・深夜時間帯・実労働上限は、
 * すべてコンストラクタで受け取る {@link WorkSchedule} から取得する。
 * 本クラスは時刻や単位をハードコードせず、既定値も持たない。
 *
 * <p>以下の集計ルールの説明は就業時間マスタが下記の内容である場合の値。
 * マスタを変更すればそれに追従する。
 *
 * <h2>集計ルール</h2>
 * <dl>
 *   <dt>通常勤務</dt>
 *   <dd>08:20〜17:00</dd>
 *
 *   <dt>実労働時間</dt>
 *   <dd>退勤 − 出勤 から以下を自動控除した時間。
 *       ① 始業前（08:20より前。出張のみ早出として 05:50 まで認める）、② 外出打刻区間、
 *       ③ 昼休み（12:00〜12:45。出張は1時間として 12:00〜13:00）、④ 定時後休憩（17:00〜17:15）。
 *       出張の場合は定時後休憩なしとみなし、④は控除しない。</dd>
 *
 *   <dt>早出</dt>
 *   <dd>始業（08:20）より前の実労働時間を 30分単位（端数切り捨て）で算出。
 *       出張のみが対象で、認めるのは 05:50 以降の最大2時間30分まで
 *       （例: 07:48 出勤なら生32分 → 30分。05:50 より前の勤務は実労働にも含めない）。
 *       出張以外は始業前を切り捨てるため常に 0。</dd>
 *
 *   <dt>時間外労働</dt>
 *   <dd>17:15 以降の実労働時間を 30分単位（端数切り捨て）で算出。
 *       出張の場合は定時後休憩がないため 17:00 以降を対象とし、切り捨てを行わず1分単位で算出する。
 *       36協定の月間・年間監視にもこの値を使用する。</dd>
 *
 *   <dt>遅刻早退累計</dt>
 *   <dd>遅刻時間（始業基準より後に出勤した分）と
 *       早退時間（終業基準より前に退勤した分）の合計（分）。
 *       基準は通常 08:20〜17:00。午前半休は午後始業（12:45。出張は 13:00）〜17:00、
 *       午後半休は 08:20〜午前終業（12:00）とし、半休で免除された半日分は累計に加えない。</dd>
 *
 *   <dt>半休（午前・午後）</dt>
 *   <dd>半日分の有給。全日有給と異なり時間計算は打刻から通常どおり行い、
 *       午前半休・午後半休フラグを集計結果へ引き継ぐ。</dd>
 *
 *   <dt>深夜労働</dt>
 *   <dd>実労働のうち 22:00〜翌05:00 または 00:00〜05:00 に重なる時間。分単位で数えるが、
 *       時間外労働の30分切り捨てで計上されなかった末尾は深夜にも計上しない
 *       （＝残業として計上した区間と深夜帯の重なり）。
 *       例: 08:20〜22:15 の勤務は 21:45〜22:15 の30分が通常の残業15分と深夜15分に分かれる。
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
 *   // 就業時間は WorkScheduleReader で就業時間マスタから読み込む
 *   DailyAggregator aggregator = new DailyAggregator(workSchedule);
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

    /** 集計に使う就業時間（時刻・丸め単位・深夜時間帯・実労働上限） */
    private final WorkSchedule schedule;

    /**
     * 指定した就業時間で集計する。
     *
     * @param schedule 就業時間（null 不可）
     * @throws IllegalArgumentException schedule が null の場合
     */
    public DailyAggregator(WorkSchedule schedule) {
        if (schedule == null) throw new IllegalArgumentException("schedule は null にできません");
        this.schedule = schedule;
    }

    // =========================================================================
    // 公開メソッド
    // =========================================================================

    /**
     * 集計に使っている就業時間を返す。
     *
     * @return 就業時間
     */
    public WorkSchedule getWorkSchedule() {
        return schedule;
    }

    /**
     * 1件の打刻データから日次集計結果を算出する。
     *
     * <p>以下の順で処理する。
     * <ol>
     *   <li>外出打刻による区間分割</li>
     *   <li>始業前（08:20より前。出張は早出として認める 05:50 より前）の切り捨て</li>
     *   <li>昼休み（12:00〜12:45。出張は 12:00〜13:00）の自動控除</li>
     *   <li>定時後休憩（17:00〜17:15）の自動控除（出張の場合は控除しない）</li>
     *   <li>実労働時間・早出・時間外・深夜・休日・遅刻早退累計の算出</li>
     * </ol>
     *
     * @param date      打刻日
     * @param isHoliday 休日FLG
     * @param isSunday  日曜FLG
     * @param record    打刻データ（null 不可）
     * @return 集計結果（null を返さない）
     * @throws IllegalArgumentException record が null の場合
     * @throws AggregationException     出勤・退勤の片方が未打刻、外出・戻りの片方が未打刻、
     *                                  打刻の時刻順序が不正、または実労働が上限を超えた場合
     */
    public WorkDaily aggregate(LocalDate date, boolean isHoliday, boolean isSunday, TimeRecord record) {
        if (record == null) throw new IllegalArgumentException("record は null にできません");

        WorkDaily.Builder builder = WorkDaily.builder()
            .employeeNo(record.getEmployeeNo())
            .workDate(date);

        // 代休
        if (record.isCompDay()) {
            return builder.isCompDay(true).build();
        }
        // 有給
        if (record.isPaidHoliday()) {
            return builder.isPaidHoliday(true).build();
        }
        // 欠勤
        if (record.getClockIn() == null && record.getClockOut() == null && !record.isBusinessTrip()) {
            return builder.isAbsence(true).build();
        }

        // 出勤・退勤のどちらかが未打刻 → 不完全データ
        if (record.getClockIn() == null || record.getClockOut() == null) {
            // 出張は打刻がなくても欠勤にしないため、未打刻は入力漏れとして通知する
            String reason = record.isBusinessTrip()
                ? "出張ですが出勤・退勤が未打刻です: "
                : "出勤・退勤のどちらか一方が未打刻です: ";
            throw new AggregationException(
                AggregationError.Type.MISSING_PUNCH,
                record.getEmployeeNo(),
                date.toString(),
                reason + "clockIn=" + record.getClockIn()
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

        // ---- 2. 始業前（08:20より前）の切り捨て ----
        //         早出を認めないため、始業より前の勤務は実労働に含めない。
        //         ただし出張は 05:50〜08:20 を早出として認めるため、05:50 を境界にする。
        //         日付またぎの勤務は翌日側の時刻（+1440分）で表すため、この控除の対象外となる。
        int beforeWorkEnd = record.isBusinessTrip()
            ? schedule.getEarlyWorkStartMinutes()
            : schedule.getStartMinutes();
        intervals = WorkIntervals.deductPeriod(intervals, 0, beforeWorkEnd);

        // ---- 3. 昼休み自動控除（12:00〜12:45。出張は1時間として 12:00〜13:00。
        //         日付またぎで翌昼を通過する場合は翌日分も控除） ----
        int lunchStart = schedule.getBreakStartMinutes();
        int lunchEnd = record.isBusinessTrip()
            ? schedule.getBusinessTripBreakEndMinutes()
            : schedule.getBreakEndMinutes();
        intervals = WorkIntervals.deductPeriod(intervals, lunchStart, lunchEnd);
        intervals = WorkIntervals.deductPeriod(intervals,
            lunchStart + WorkIntervals.MINUTES_PER_DAY, lunchEnd + WorkIntervals.MINUTES_PER_DAY);

        // ---- 4. 定時後休憩自動控除（17:00〜17:15。同上、翌日分も控除。出張は休憩なしのため控除しない） ----
        if (!record.isBusinessTrip()) {
            int restStart = schedule.getEndMinutes();
            int restEnd   = schedule.getOvertimeStartMinutes();
            intervals = WorkIntervals.deductPeriod(intervals, restStart, restEnd);
            intervals = WorkIntervals.deductPeriod(intervals,
                restStart + WorkIntervals.MINUTES_PER_DAY, restEnd + WorkIntervals.MINUTES_PER_DAY);
        }

        // ---- 5. 各区分の算出 ----
        int totalMinutes     = WorkIntervals.sumIntervals(intervals);

        // 上限（既定24時間）超は異常データ
        if (totalMinutes > schedule.getMaxWorkMinutes()) {
            throw new AggregationException(
                AggregationError.Type.EXCESSIVE_WORK_HOURS,
                record.getEmployeeNo(),
                date.toString(),
                "実労働時間が上限（" + schedule.getMaxWorkMinutes() + "分）を超えています: " + totalMinutes + "分");
        }

        int earlyWorkMinutes  = calcEarlyWorkMinutes(intervals, record.isBusinessTrip());
        int overtimeMinutes   = calcOvertimeMinutes(intervals, record.isBusinessTrip());
        int lateNightMinutes  = calcLateNightMinutes(intervals, record.isBusinessTrip());
        int scheduledMinutes  = calcScheduledMinutes(intervals, record.isBusinessTrip());
        int lateEarlyMinutes  = calcLateEarlyMinutes(record);

        return builder
                .totalMinutes(totalMinutes)
                .scheduledMinutes(scheduledMinutes)
                .earlyWorkMinutes(earlyWorkMinutes)
                .overtimeMinutes(overtimeMinutes)
                .lateNightMinutes(lateNightMinutes)
                .isHoliday(isHoliday)
                .isSunday(isSunday)
                .isBusinessTrip(record.isBusinessTrip())
                .isLateEarly(lateEarlyMinutes > 0)
                .lateEarlyMinutes(lateEarlyMinutes)
                .isAmPaidHoliday(record.isAmPaidHoliday())
                .isPmPaidHoliday(record.isPmPaidHoliday())
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
     * <p>{@link #calcLateEarlyMinutes} による遅刻早退累計が 1 分以上かで判定する
     * （半休による基準時刻の変更・日付またぎの扱いも同メソッドに従う）。
     *
     * @param record 打刻データ（出勤・退勤とも打刻済みであること）
     * @return 遅刻または早退の場合は true, そうでない場合は false
     */
    public boolean isLateEarly(TimeRecord record) {
        return calcLateEarlyMinutes(record) > 0;
    }

    /**
     * 遅刻早退累計（分）を算出する。
     *
     * <p>遅刻時間（始業基準より後に出勤した分）と
     * 早退時間（終業基準より前に退勤した分）の合計を返す。
     * 始業基準は通常 08:20、午前半休の場合は午後始業（＝昼休み終了。通常 12:45、出張は 13:00）。
     * 終業基準は通常 17:00、午後半休の場合は午前終業（12:00 ＝ 昼休み開始）。
     * 半休で免除された半日分は累計に加えない。
     *
     * <p>日付またぎ（退勤 &lt; 出勤）の場合は退勤を翌日時刻として扱い、早退時間は 0 とする。
     * 始業基準より後の出勤は深夜シフトであっても遅刻として扱う。
     *
     * @param record 打刻データ（出勤・退勤とも打刻済みであること）
     * @return 遅刻早退累計（分）。遅刻も早退もない場合は 0。
     */
    public int calcLateEarlyMinutes(TimeRecord record) {
        int clockInMin  = WorkIntervals.toMinutes(record.getClockIn());
        int clockOutMin = WorkIntervals.toMinutes(record.getClockOut());
        // 日付またぎは退勤を翌日時刻に補正してから早退時間を計算する
        if (clockOutMin < clockInMin) clockOutMin += WorkIntervals.MINUTES_PER_DAY;
        // 午前半休は午後始業（＝昼休み終了。通常 12:45、出張は 13:00）、
        // 午後半休は午前終業（12:00）を基準にする
        int lunchEnd = record.isBusinessTrip()
            ? schedule.getBusinessTripBreakEndMinutes()
            : schedule.getBreakEndMinutes();
        int startMin = record.isAmPaidHoliday() ? lunchEnd                          : schedule.getStartMinutes();
        int endMin   = record.isPmPaidHoliday() ? schedule.getBreakStartMinutes()   : schedule.getEndMinutes();
        int lateMinutes  = Math.max(0, clockInMin - startMin);
        int earlyMinutes = Math.max(0, endMin - clockOutMin);
        return lateMinutes + earlyMinutes;
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
                // 休日・日曜で打刻がないレコードは欠勤ではないためスキップする
                // （休日・日曜に有給は存在しないため、有給を考慮する必要はない）。
                // ただし出張FLGが立っている場合は打刻の入力漏れとみなし、
                // スキップせず MISSING_PUNCH エラーとして通知する。
                if (
                    (isHoliday || isSunday)
                        && record.getClockIn() == null
                        && record.getClockOut() == null
                        && !record.isBusinessTrip()
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
     * <p>昼休み・定時後休憩の控除は {@link WorkIntervals#deductPeriod} で後処理する。
     *
     * @return 労働区間のリスト。各要素は {@code int[]{開始分, 終了分}}。
     * @throws AggregationException 時刻順序が不正な場合
     */
    private List<int[]> buildPunchIntervals(LocalDate date, TimeRecord record) {
        int rawClockIn  = WorkIntervals.toMinutes(record.getClockIn());
        int rawClockOut = WorkIntervals.toMinutes(record.getClockOut());

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
        int clockOutMin = midnightShift ? rawClockOut + WorkIntervals.MINUTES_PER_DAY : rawClockOut;

        List<int[]> intervals = new ArrayList<>();

        if (record.getGoOut() != null) {
            int rawGoOut    = WorkIntervals.toMinutes(record.getGoOut());
            int rawReturnIn = WorkIntervals.toMinutes(record.getReturnIn());
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
     * 早出（分）を算出する。
     *
     * <p>早出は出張のみが対象で、始業（既定 08:20）より前の実労働のうち
     * 早出開始（既定 05:50）以降と重なる時間を早出単位（既定30分）で
     * 切り捨てて算出する。既定では対象帯域が 2時間30分のため、結果は最大 150分になる。
     *
     * <p>早出開始より前の勤務は早出として認めず、実労働にも含めない
     * （{@link #aggregate} の始業前切り捨てで除去済みのため、ここでは下限を設ける必要はないが、
     * 帯域の定義を明示するため重なりで算出する）。
     *
     * @param intervals      控除済みの労働区間リスト
     * @param isBusinessTrip 出張FLG
     * @return 早出（分）。出張以外は常に 0。
     */
    private int calcEarlyWorkMinutes(List<int[]> intervals, boolean isBusinessTrip) {
        // 出張以外は始業前を切り捨てているため早出は発生しない
        if (!isBusinessTrip) return 0;
        int raw = 0;
        for (int[] iv : intervals) {
            raw += WorkIntervals.overlap(iv[0], iv[1],
                schedule.getEarlyWorkStartMinutes(), schedule.getStartMinutes());
        }
        int unit = schedule.getEarlyWorkUnitMinutes();
        return (raw / unit) * unit;
    }

    /**
     * 時間外労働（分）を算出する。
     *
     * <p>残業開始（既定 17:15）以降の実労働時間を合計し、
     * 残業単位（既定30分）で切り捨てる。
     * 定時後休憩（17:00〜17:15）は {@link WorkIntervals#deductPeriod} で事前に除去済みのため、
     * 残業開始以降のみを対象とすれば良い。
     *
     * <p>出張の場合は定時後休憩がないため終業（既定 17:00）以降を対象とし、
     * 切り捨てを行わず1分単位で算出する。
     *
     * @param intervals      控除済みの労働区間リスト
     * @param isBusinessTrip 出張FLG
     * @return 時間外労働（分）。出張以外は残業単位に切り捨てた値。
     */
    private int calcOvertimeMinutes(List<int[]> intervals, boolean isBusinessTrip) {
        int raw = calcRawOvertimeMinutes(intervals, isBusinessTrip);
        // 出張は切り捨てを行わない
        if (isBusinessTrip) return raw;
        int unit = schedule.getOvertimeUnitMinutes();
        return (raw / unit) * unit;
    }

    /**
     * 切り捨て前の時間外労働（分）を算出する。
     *
     * <p>出張は定時後休憩がないため終業（既定 17:00）以降、
     * それ以外は残業開始（既定 17:15）以降を対象とする。
     *
     * @param intervals      控除済みの労働区間リスト
     * @param isBusinessTrip 出張FLG
     * @return 切り捨てる前の時間外労働（分）
     */
    private int calcRawOvertimeMinutes(List<int[]> intervals, boolean isBusinessTrip) {
        int overtimeStart = isBusinessTrip ? schedule.getEndMinutes() : schedule.getOvertimeStartMinutes();
        int raw = 0;
        for (int[] iv : intervals) {
            if (iv[1] > overtimeStart) {
                raw += iv[1] - Math.max(iv[0], overtimeStart);
            }
        }
        return raw;
    }

    /**
     * 深夜労働時間（分）を算出する。
     *
     * <p>深夜時間帯は以下の2帯域の和（既定値の場合）:
     * <ul>
     *   <li>早朝深夜帯: [0, 300)   → 00:00〜05:00</li>
     *   <li>夜間深夜帯: [1320, 1740) → 22:00〜翌05:00（日付またぎ分を含む）</li>
     * </ul>
     *
     * <p>深夜かつ残業、深夜かつ休日の重複は各集計で独立してカウントする（仕様書 4.1.1）。
     *
     * <p>深夜は分単位で数えるが、時間外労働の切り捨て（既定30分単位）で計上されなかった末尾は
     * 深夜にも計上しない（{@link #dropTail}）。深夜帯はすべて残業の時間帯に含まれるため、
     * これにより「残業として計上した区間のうち深夜帯と重なる分」が深夜労働になり、
     * 集計画面の「残業 ＝ 時間外労働 − 深夜労働」と内訳が常に一致する。
     *
     * <p>例えば 08:20〜22:15 の勤務は時間外労働 300分（17:15〜22:15）のうち
     * 22:00〜22:15 の15分が深夜となり、21:45〜22:15 の30分が通常の残業15分と深夜15分に分かれる。
     * 08:20〜23:00 は時間外労働が 345分 → 330分に切り捨てられるため、
     * 末尾の 22:45〜23:00 を除いた 22:00〜22:45 の45分が深夜になる。
     *
     * <p>出張の場合は時間外労働を切り捨てないため、深夜も実際に重なった分をそのまま計上する。
     *
     * @param intervals      控除済みの労働区間リスト
     * @param isBusinessTrip 出張FLG
     * @return 深夜労働時間（分）
     */
    private int calcLateNightMinutes(List<int[]> intervals, boolean isBusinessTrip) {
        List<int[]> counted = intervals;
        if (!isBusinessTrip) {
            // 切り捨てで時間外労働に計上されなかった末尾は深夜にも計上しない
            int rawOvertime = calcRawOvertimeMinutes(intervals, false);
            counted = dropTail(intervals, rawOvertime % schedule.getOvertimeUnitMinutes());
        }
        int lateNight = 0;
        for (int[] iv : counted) {
            lateNight += WorkIntervals.overlap(iv[0], iv[1], 0, schedule.getLateNightEndMinutes());
            lateNight += WorkIntervals.overlap(iv[0], iv[1],
                schedule.getLateNightStartMinutes(), schedule.getLateNightEndNextDayMinutes());
        }
        return lateNight;
    }

    /**
     * 労働区間リストの末尾から {@code dropMinutes} 分を取り除いた区間リストを返す。
     *
     * <p>時間外労働の切り捨てで落ちる端数は必ず勤務の末尾にあたるため、
     * 「残業として計上した区間」を求める用途に使う。
     * 区間リストは時刻の昇順に並んでいることを前提とする。
     *
     * @param intervals   元の労働区間リスト
     * @param dropMinutes 末尾から取り除く時間（分）。0 の場合は元のリストをそのまま返す。
     * @return 末尾を取り除いた労働区間リスト
     */
    private List<int[]> dropTail(List<int[]> intervals, int dropMinutes) {
        if (dropMinutes <= 0) return intervals;
        List<int[]> result = new ArrayList<>(intervals);
        for (int i = result.size() - 1; i >= 0 && dropMinutes > 0; i--) {
            int[] iv = result.get(i);
            int length = iv[1] - iv[0];
            if (length <= dropMinutes) {
                // 区間ごと落ちる
                dropMinutes -= length;
                result.remove(i);
            } else {
                result.set(i, new int[]{iv[0], iv[1] - dropMinutes});
                dropMinutes = 0;
            }
        }
        return result;
    }

    /**
     * 所定内労働時間（分）を算出する。
     *
     * <p>実労働のうち、早出（始業より前）にも
     * 残業（残業開始以降。出張は終業以降）にも
     * 深夜時間帯にも該当しない時間。割増賃金の対象にならない部分にあたる。
     * 出張の所定内は 08:20〜17:00（昼休み1時間を除く 460分）が上限になる。
     *
     * <p>早出・時間外・深夜は丸め単位（既定30分）で切り捨てるため、
     * 「実労働 − 早出 − 残業 − 深夜」で所定内を求めると切り捨てた端数が所定内に混ざってしまう。
     * それを避けるため、所定内は打刻から直接（切り捨てずに）算出する。
     *
     * @param intervals      控除済みの労働区間リスト
     * @param isBusinessTrip 出張FLG
     * @return 所定内労働時間（分）
     */
    private int calcScheduledMinutes(List<int[]> intervals, boolean isBusinessTrip) {
        // 出張は定時後休憩がなく終業から残業扱い、それ以外は残業開始が起点
        int overtimeStart = isBusinessTrip ? schedule.getEndMinutes() : schedule.getOvertimeStartMinutes();
        List<int[]> scheduled = WorkIntervals.deductPeriod(intervals, overtimeStart, Integer.MAX_VALUE);
        // 始業前は所定内に含めない。出張の早出区間（05:50〜08:20）を除くほか、
        // 早朝深夜帯 [0, 深夜終了) もこの範囲に含まれる
        scheduled = WorkIntervals.deductPeriod(scheduled, 0, schedule.getStartMinutes());
        scheduled = WorkIntervals.deductPeriod(scheduled,
            schedule.getLateNightStartMinutes(), schedule.getLateNightEndNextDayMinutes());
        return WorkIntervals.sumIntervals(scheduled);
    }

    /**
     * {@code value} が {@code baseline} 以下の場合に +1440 (24h) して返す。
     * 外出・戻り・退勤の日付またぎ補正に使用する。
     */
    private int normalizeAfter(int value, int baseline) {
        return (value <= baseline) ? value + WorkIntervals.MINUTES_PER_DAY : value;
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
}
