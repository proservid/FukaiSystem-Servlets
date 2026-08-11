package fukaisystem.domain.attendance;

import java.io.Serializable;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 就業時間（就業規則で決まる時刻・計算単位）。
 *
 * <p>始業・終業・休憩・残業開始といった時刻のほか、残業・早出の丸め単位、
 * 深夜時間帯、実労働時間の上限までを一括で保持する。
 * 日次勤怠集計（{@link DailyAggregator}）と加工実績の実働算出
 * （{@link #calcWorkMinutes(int, int)}）は、いずれもここに集約した設定値だけを参照する。
 *
 * <p>時刻はすべて「勤務日 00:00 を 0 とした分数」で保持する（例: 08:20 → 500）。
 * 丸め単位・上限は分数そのもの。
 *
 * <p>設定値の定義元は就業時間マスタ（{@code M_就業時間}）だけとし、
 * 本クラスは既定値を持たない。すべての項目を {@link Builder} で明示的に設定すること
 * （未設定のまま {@link Builder#build()} を呼ぶとエラーになる）。
 * 既定値を持たせるとマスタと食い違ったまま気付けなくなるため。
 *
 * <p>本クラスは不変オブジェクトとして設計する。
 */
public final class WorkSchedule implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 始業（分）。これより前は実働に含めない（例: 08:20 → 500） */
    private final int startMinutes;

    /** 終業（分）（例: 17:00 → 1020） */
    private final int endMinutes;

    /** 休憩開始（分）（例: 12:00 → 720） */
    private final int breakStartMinutes;

    /** 休憩終了（分）（例: 12:45 → 765） */
    private final int breakEndMinutes;

    /** 出張時の休憩終了（分）。出張は休憩を長めにとる（例: 13:00 → 780） */
    private final int businessTripBreakEndMinutes;

    /** 残業開始（分）＝定時後休憩の終了（例: 17:15 → 1035。出張は定時後休憩なしのため終業が起点） */
    private final int overtimeStartMinutes;

    /** 残業の計算単位（分）。切り捨てに使う（出張には適用せず1分単位とする） */
    private final int overtimeUnitMinutes;

    /**
     * 早出として認める最も早い時刻（分）（例: 05:50 → 350）。
     * 始業までが早出の対象になる（出張のみ）。
     */
    private final int earlyWorkStartMinutes;

    /** 早出の計算単位（分）。切り捨てに使う（早出は出張のみが対象のため出張でも切り捨てる） */
    private final int earlyWorkUnitMinutes;

    /** 深夜時間帯の開始（分）。労働基準法第37条（例: 22:00 → 1320） */
    private final int lateNightStartMinutes;

    /** 深夜時間帯の終了（分）。翌日の時刻を指す（例: 05:00 → 300） */
    private final int lateNightEndMinutes;

    /** 実労働時間の上限（分）。超過はデータ異常とみなす（例: 24時間 → 1440） */
    private final int maxWorkMinutes;

    private WorkSchedule(Builder b) {
        this.startMinutes                = b.startMinutes;
        this.endMinutes                  = b.endMinutes;
        this.breakStartMinutes           = b.breakStartMinutes;
        this.breakEndMinutes             = b.breakEndMinutes;
        this.businessTripBreakEndMinutes = b.businessTripBreakEndMinutes;
        this.overtimeStartMinutes        = b.overtimeStartMinutes;
        this.overtimeUnitMinutes         = b.overtimeUnitMinutes;
        this.earlyWorkStartMinutes       = b.earlyWorkStartMinutes;
        this.earlyWorkUnitMinutes        = b.earlyWorkUnitMinutes;
        this.lateNightStartMinutes       = b.lateNightStartMinutes;
        this.lateNightEndMinutes         = b.lateNightEndMinutes;
        this.maxWorkMinutes              = b.maxWorkMinutes;
    }

    // ---- ゲッター ----------------------------------------------------------------

    public int getStartMinutes()                { return startMinutes;                }
    public int getEndMinutes()                  { return endMinutes;                  }
    public int getBreakStartMinutes()           { return breakStartMinutes;           }
    public int getBreakEndMinutes()             { return breakEndMinutes;             }
    public int getBusinessTripBreakEndMinutes() { return businessTripBreakEndMinutes; }
    public int getOvertimeStartMinutes()        { return overtimeStartMinutes;        }
    public int getOvertimeUnitMinutes()         { return overtimeUnitMinutes;         }
    public int getEarlyWorkStartMinutes()       { return earlyWorkStartMinutes;       }
    public int getEarlyWorkUnitMinutes()        { return earlyWorkUnitMinutes;        }
    public int getLateNightStartMinutes()       { return lateNightStartMinutes;       }
    public int getLateNightEndMinutes()         { return lateNightEndMinutes;         }
    public int getMaxWorkMinutes()              { return maxWorkMinutes;              }

    /**
     * 深夜時間帯の終了を翌日側の分数で返す（例: 05:00 → 1740）。
     *
     * <p>深夜時間帯は「早朝深夜帯 [0, {@link #getLateNightEndMinutes()})」と
     * 「夜間深夜帯 [{@link #getLateNightStartMinutes()}, 本メソッドの戻り値)」の2帯域で表す。
     * 後者は日付またぎ分（翌05:00 まで）を含むため、翌日側の分数が必要になる。
     *
     * @return 深夜時間帯の終了（翌日側の分数）
     */
    public int getLateNightEndNextDayMinutes() {
        return lateNightEndMinutes + WorkIntervals.MINUTES_PER_DAY;
    }

    /**
     * 指定した区間の実働時間（分）を算出する。
     *
     * <p>[from, to) から以下を順に控除した残りの合計を返す。
     * <ol>
     *   <li>始業前（[0, 始業)）— 早出は認めない</li>
     *   <li>休憩（[休憩開始, 休憩終了)）</li>
     *   <li>定時後休憩（[終業, 残業開始)）</li>
     * </ol>
     *
     * <p>区間の端が控除帯の内側にある場合は端が繰り上げ・繰り下げられる
     * （例: 休憩中に終了した区間は休憩開始で打ち切られる）。
     * {@code to} が {@code from} 以下の場合は 0 を返す。
     *
     * <p>日付またぎは扱わない（呼び出し元が同日内の区間を渡すこと）。
     *
     * @param fromMinutes 区間の開始（00:00 起点の分）
     * @param toMinutes   区間の終了（00:00 起点の分）
     * @return 実働時間（分）。負にはならない。
     */
    public int calcWorkMinutes(int fromMinutes, int toMinutes) {
        List<int[]> intervals = new ArrayList<>();
        intervals.add(new int[]{fromMinutes, toMinutes});
        intervals = WorkIntervals.deductPeriod(intervals, 0, startMinutes);
        intervals = WorkIntervals.deductPeriod(intervals, breakStartMinutes, breakEndMinutes);
        intervals = WorkIntervals.deductPeriod(intervals, endMinutes, overtimeStartMinutes);
        return WorkIntervals.sumIntervals(intervals);
    }

    @Override
    public String toString() {
        return String.format(
            "WorkSchedule{始業=%s, 終業=%s, 休憩=%s〜%s(出張は%sまで), 残業開始=%s, 深夜=%s〜翌%s}",
            format(startMinutes), format(endMinutes),
            format(breakStartMinutes), format(breakEndMinutes), format(businessTripBreakEndMinutes),
            format(overtimeStartMinutes),
            format(lateNightStartMinutes), format(lateNightEndMinutes));
    }

    /** 分数を "H:mm" 形式に変換する（{@link #toString()} 用） */
    private static String format(int minutes) {
        return (minutes / 60) + ":" + String.format("%02d", minutes % 60);
    }

    // ---- Builder ----------------------------------------------------------------

    public static Builder builder() { return new Builder(); }

    /**
     * {@link WorkSchedule} のビルダー。
     *
     * <p>時刻は {@link LocalTime} で受け取る（秒・ナノ秒は切り捨てる）。
     * 計算単位・上限は分数で受け取る。
     */
    public static final class Builder {

        /** 未設定を表す値。負の値も設定値として検証したいため、通常あり得ない値を使う */
        private static final int UNSET = Integer.MIN_VALUE;

        private int startMinutes                = UNSET;
        private int endMinutes                  = UNSET;
        private int breakStartMinutes           = UNSET;
        private int breakEndMinutes             = UNSET;
        private int businessTripBreakEndMinutes = UNSET;
        private int overtimeStartMinutes        = UNSET;
        private int overtimeUnitMinutes         = UNSET;
        private int earlyWorkStartMinutes       = UNSET;
        private int earlyWorkUnitMinutes        = UNSET;
        private int lateNightStartMinutes       = UNSET;
        private int lateNightEndMinutes         = UNSET;
        private int maxWorkMinutes              = UNSET;

        private Builder() {}

        public Builder startTime(LocalTime v)                { this.startMinutes                = toMinutes("始業",         v); return this; }
        public Builder endTime(LocalTime v)                  { this.endMinutes                  = toMinutes("終業",         v); return this; }
        public Builder breakStartTime(LocalTime v)           { this.breakStartMinutes           = toMinutes("休憩開始",     v); return this; }
        public Builder breakEndTime(LocalTime v)             { this.breakEndMinutes             = toMinutes("休憩終了",     v); return this; }
        public Builder businessTripBreakEndTime(LocalTime v) { this.businessTripBreakEndMinutes = toMinutes("出張休憩終了", v); return this; }
        public Builder overtimeStartTime(LocalTime v)        { this.overtimeStartMinutes        = toMinutes("残業開始",     v); return this; }
        public Builder earlyWorkStartTime(LocalTime v)       { this.earlyWorkStartMinutes       = toMinutes("早出開始",     v); return this; }
        public Builder lateNightStartTime(LocalTime v)       { this.lateNightStartMinutes       = toMinutes("深夜開始",     v); return this; }
        public Builder lateNightEndTime(LocalTime v)         { this.lateNightEndMinutes         = toMinutes("深夜終了",     v); return this; }

        public Builder overtimeUnit(int minutes)   { this.overtimeUnitMinutes  = minutes; return this; }
        public Builder earlyWorkUnit(int minutes)  { this.earlyWorkUnitMinutes = minutes; return this; }
        public Builder maxWorkMinutes(int minutes) { this.maxWorkMinutes       = minutes; return this; }

        /**
         * 就業時間を組み立てる。
         *
         * @return 組み立てた就業時間
         * @throws IllegalStateException    未設定の項目がある場合
         * @throws IllegalArgumentException 時刻の前後関係または計算単位が不正な場合
         */
        public WorkSchedule build() {
            require(startMinutes,                "始業");
            require(endMinutes,                  "終業");
            require(breakStartMinutes,           "休憩開始");
            require(breakEndMinutes,             "休憩終了");
            require(businessTripBreakEndMinutes, "出張休憩終了");
            require(overtimeStartMinutes,        "残業開始");
            require(overtimeUnitMinutes,         "残業単位");
            require(earlyWorkStartMinutes,       "早出開始");
            require(earlyWorkUnitMinutes,        "早出単位");
            require(lateNightStartMinutes,       "深夜開始");
            require(lateNightEndMinutes,         "深夜終了");
            require(maxWorkMinutes,              "実労働上限");

            order("早出開始",     earlyWorkStartMinutes,       "始業",         startMinutes);
            order("始業",         startMinutes,                "休憩開始",     breakStartMinutes);
            order("休憩開始",     breakStartMinutes,           "休憩終了",     breakEndMinutes);
            order("休憩終了",     breakEndMinutes,             "出張休憩終了", businessTripBreakEndMinutes);
            order("出張休憩終了", businessTripBreakEndMinutes, "終業",         endMinutes);
            order("終業",         endMinutes,                  "残業開始",     overtimeStartMinutes);
            order("深夜終了",     lateNightEndMinutes,         "深夜開始",     lateNightStartMinutes);

            positive(overtimeUnitMinutes,  "残業単位");
            positive(earlyWorkUnitMinutes, "早出単位");
            positive(maxWorkMinutes,       "実労働上限");

            return new WorkSchedule(this);
        }

        /** 時刻を 00:00 起点の分数に変換する（秒・ナノ秒は切り捨て） */
        private static int toMinutes(String name, LocalTime time) {
            if (time == null) {
                throw new IllegalArgumentException(name + "は null にできません");
            }
            return WorkIntervals.toMinutes(time);
        }

        /** 未設定を検出する */
        private static void require(int value, String name) {
            if (value == UNSET) {
                throw new IllegalStateException(name + "は必須です");
            }
        }

        /** 時刻の前後関係を検証する（同時刻は許容する） */
        private static void order(String earlierName, int earlier, String laterName, int later) {
            if (earlier > later) {
                throw new IllegalArgumentException(
                    earlierName + "は" + laterName + "以前である必要があります: "
                        + earlierName + "=" + format(earlier) + " " + laterName + "=" + format(later));
            }
        }

        /** 正の値であることを検証する */
        private static void positive(int value, String name) {
            if (value <= 0) {
                throw new IllegalArgumentException(name + "は1以上で指定してください: " + value);
            }
        }
    }
}
