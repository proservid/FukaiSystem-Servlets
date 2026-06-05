package fukaisystem.domain.attendance;

import java.time.LocalDate;
import java.time.LocalDateTime;

import fukaisystem.util.Util;

/**
 * 日次集計結果（work_daily テーブルへの登録データ）。
 *
 * <p>時間はすべて「分」単位で保持する（DBのINT型と対応）。
 * 時・分への変換は {@link #toHoursMinutes(int)} を利用すること。
 *
 * <p>本クラスは不変オブジェクトとして設計する。
 * インスタンス生成には {@link Builder} を使用すること。
 */
public final class WorkDaily {

    /** 従業員番号 */
    private final String employeeNo;

    /** 対象日 */
    private final LocalDate workDate;

    /** 実労働時間（分）: 退勤 - 出勤 - 外出時間 */
    private final int totalMinutes;

    /** 日次時間外労働（分）: max(0, 実労働 - 480分) */
    private final int overtimeMinutes;

    /** 深夜労働（分）: 22:00〜翌05:00 に重なる実労働時間 */
    private final int lateNightMinutes;

    /** 休日労働（分）: 休日フラグが true の場合の実労働時間全体 */
    private final int holidayMinutes;

    /** 休日フラグ: 日曜・祝日・所定休日の場合 true */
    private final boolean isHoliday;

    /** 集計実行日時 */
    private final LocalDateTime calcAt;

    private WorkDaily(Builder b) {
        this.employeeNo      = b.employeeNo;
        this.workDate        = b.workDate;
        this.totalMinutes    = b.totalMinutes;
        this.overtimeMinutes = b.overtimeMinutes;
        this.lateNightMinutes = b.lateNightMinutes;
        this.holidayMinutes  = b.holidayMinutes;
        this.isHoliday       = b.isHoliday;
        this.calcAt          = b.calcAt;
    }

    // ---- ゲッター ----------------------------------------------------------------

    public String        getEmployeeNo()       { return employeeNo;       }
    public LocalDate     getWorkDate()         { return workDate;         }
    public int           getTotalMinutes()     { return totalMinutes;     }
    public int           getOvertimeMinutes()  { return overtimeMinutes;  }
    public int           getLateNightMinutes() { return lateNightMinutes; }
    public int           getHolidayMinutes()   { return holidayMinutes;   }
    public boolean       isHoliday()           { return isHoliday;        }
    public LocalDateTime getCalcAt()           { return calcAt;           }

    /**
     * 分数を "H時間M分" 形式の文字列に変換する。
     *
     * @param minutes 変換する分数（負でないこと）
     * @return 例: 125 → "2時間5分"
     */
    public static String toHoursMinutes(int minutes) {
        if (minutes < 0) throw new IllegalArgumentException("minutes は0以上を指定してください: " + minutes);
        return (minutes / 60) + "時間" + (minutes % 60) + "分";
    }

    @Override
    public String toString() {
        return String.format(
            "WorkDaily{employeeNo='%s', workDate=%s, isHoliday=%b, total=%s, overtime=%s, lateNight=%s, holiday=%s}",
            employeeNo, workDate, isHoliday,
            toHoursMinutes(totalMinutes),
            toHoursMinutes(overtimeMinutes),
            toHoursMinutes(lateNightMinutes),
            toHoursMinutes(holidayMinutes));
    }

    // ---- Builder ----------------------------------------------------------------

    public static Builder builder() { return new Builder(); }

    public static final class Builder {
        private String        employeeNo;
        private LocalDate     workDate;
        private int           totalMinutes;
        private int           overtimeMinutes;
        private int           lateNightMinutes;
        private int           holidayMinutes;
        private boolean       isHoliday;
        private LocalDateTime calcAt = LocalDateTime.now();

        private Builder() {}

        public Builder employeeNo(String v)         { this.employeeNo       = v; return this; }
        public Builder workDate(LocalDate v)        { this.workDate         = v; return this; }
        public Builder totalMinutes(int v)          { this.totalMinutes     = v; return this; }
        public Builder overtimeMinutes(int v)       { this.overtimeMinutes  = v; return this; }
        public Builder lateNightMinutes(int v)      { this.lateNightMinutes = v; return this; }
        public Builder holidayMinutes(int v)        { this.holidayMinutes   = v; return this; }
        public Builder isHoliday(boolean v)         { this.isHoliday        = v; return this; }
        public Builder calcAt(LocalDateTime v)      { this.calcAt           = v; return this; }

        public WorkDaily build() {
            if (Util.isBlank(employeeNo)) {
                throw new IllegalStateException("employeeNo は必須です");
            }
            if (workDate == null) {
                throw new IllegalStateException("workDate は必須です");
            }
            return new WorkDaily(this);
        }
    }
}
