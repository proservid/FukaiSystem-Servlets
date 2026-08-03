package fukaisystem.domain.attendance;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 日次集計結果（work_daily テーブルへの登録データ）。
 *
 * <p>時間はすべて「分」単位で保持する（DBのINT型と対応）。
 * 時・分への変換は {@link #toHoursMinutes(int)} を利用すること。
 *
 * <p>本クラスは不変オブジェクトとして設計する。
 * インスタンス生成には {@link Builder} を使用すること。
 */
public final class WorkDaily implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 従業員番号 */
    private final int employeeNo;

    /** 対象日 */
    private final LocalDate workDate;

    /** 実労働時間（分）: 退勤 - 出勤 - 外出時間 - 昼休み - 定時後休憩 */
    private final int totalMinutes;

    /**
     * 所定内労働時間（分）: 実労働のうち残業にも深夜にも該当しない時間。
     *
     * <p>時間外・深夜は30分単位で切り捨てるため、「実労働 - 時間外 - 深夜」で求めると
     * 切り捨てた端数が混ざる。混入を避けるため打刻から直接算出した値を保持する。
     */
    private final int scheduledMinutes;

    /** 日次時間外労働（分）: 17:15 以降の実労働時間（30分単位切り捨て） */
    private final int overtimeMinutes;

    /** 深夜労働（分）: 00:00〜05:00 および 22:00〜翌05:00 に重なる実労働時間（30分単位切り捨て） */
    private final int lateNightMinutes;

    /** 遅刻早退累計（分）: 遅刻時間（定時始業08:20より後の出勤分）と早退時間（定時終業17:00より前の退勤分）の合計 */
    private final int lateEarlyMinutes;

    /** 休日フラグ: 祝日・所定休日の場合 true（法定休日の日曜は isSunday で表す） */
    private final boolean isHoliday;

    /** 日曜フラグ: 日曜の場合 true */
    private final boolean isSunday;

    /** 出張フラグ: 出張の場合 true */
    private final boolean isBusinessTrip;

    /** 遅早フラグ: 遅刻・早退の場合 true */
    private final boolean isLateEarly;

    /** 欠勤フラグ: 欠勤の場合 true */
    private final boolean isAbsence;

    /** 有給フラグ: 代休の場合 true */
    private final boolean isCompDay;

    /** 有給フラグ: 有給休暇の場合 true */
    private final boolean isPaidHoliday;

    /** 午前半休フラグ: 午前半日分の有給の場合 true（時間計算は打刻から通常どおり行い、午前分は遅刻早退累計に加えない） */
    private final boolean isAmPaidHoliday;

    /** 午後半休フラグ: 午後半日分の有給の場合 true（時間計算は打刻から通常どおり行い、午後分は遅刻早退累計に加えない） */
    private final boolean isPmPaidHoliday;

    /** 集計実行日時 */
    private final LocalDateTime calcAt;

    private WorkDaily(Builder b) {
        this.employeeNo       = b.employeeNo;
        this.workDate         = b.workDate;
        this.totalMinutes     = b.totalMinutes;
        this.scheduledMinutes = b.scheduledMinutes;
        this.overtimeMinutes  = b.overtimeMinutes;
        this.lateNightMinutes = b.lateNightMinutes;
        this.lateEarlyMinutes = b.lateEarlyMinutes;
        this.isHoliday        = b.isHoliday;
        this.isSunday         = b.isSunday;
        this.isBusinessTrip   = b.isBusinessTrip;
        this.isLateEarly      = b.isLateEarly;
        this.isAbsence        = b.isAbsence;
        this.isCompDay        = b.isCompDay;
        this.isPaidHoliday    = b.isPaidHoliday;
        this.isAmPaidHoliday  = b.isAmPaidHoliday;
        this.isPmPaidHoliday  = b.isPmPaidHoliday;
        this.calcAt           = b.calcAt;
    }

    // ---- ゲッター ----------------------------------------------------------------

    public int           getEmployeeNo()       { return employeeNo;       }
    public LocalDate     getWorkDate()         { return workDate;         }
    public int           getTotalMinutes()     { return totalMinutes;     }
    public int           getScheduledMinutes() { return scheduledMinutes; }
    public int           getOvertimeMinutes()  { return overtimeMinutes;  }
    public int           getLateNightMinutes() { return lateNightMinutes; }
    public int           getLateEarlyMinutes() { return lateEarlyMinutes; }
    public boolean       isHoliday()           { return isHoliday;        }
    public boolean       isSunday()            { return isSunday;         }
    public boolean       isBusinessTrip()      { return isBusinessTrip;   }
    public boolean       isLateEarly()         { return isLateEarly;      }
    public boolean       isAbsence()           { return isAbsence;        }
    public boolean       isCompDay()           { return isCompDay;        }
    public boolean       isPaidHoliday()       { return isPaidHoliday;    }
    public boolean       isAmPaidHoliday()     { return isAmPaidHoliday;  }
    public boolean       isPmPaidHoliday()     { return isPmPaidHoliday;  }
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
            "WorkDaily{employeeNo='%d', workDate=%s, isHoliday=%b, total=%s, overtime=%s, lateNight=%s, holiday=%s}",
            employeeNo, workDate, isHoliday,
            toHoursMinutes(totalMinutes),
            toHoursMinutes(overtimeMinutes),
            toHoursMinutes(lateNightMinutes));
    }

    // ---- Builder ----------------------------------------------------------------

    public static Builder builder() { return new Builder(); }

    public static final class Builder {
        private int           employeeNo;
        private LocalDate     workDate;
        private int           totalMinutes;
        private int           scheduledMinutes;
        private int           overtimeMinutes;
        private int           lateNightMinutes;
        private int           lateEarlyMinutes;
        private boolean       isHoliday;
        private boolean       isSunday;
        private boolean       isBusinessTrip;
        private boolean       isLateEarly;
        private boolean       isAbsence;
        private boolean       isCompDay;
        private boolean       isPaidHoliday;
        private boolean       isAmPaidHoliday;
        private boolean       isPmPaidHoliday;
        private LocalDateTime calcAt = LocalDateTime.now();

        private Builder() {}

        public Builder employeeNo(int v)            { this.employeeNo       = v; return this; }
        public Builder workDate(LocalDate v)        { this.workDate         = v; return this; }
        public Builder totalMinutes(int v)          { this.totalMinutes     = v; return this; }
        public Builder scheduledMinutes(int v)      { this.scheduledMinutes = v; return this; }
        public Builder overtimeMinutes(int v)       { this.overtimeMinutes  = v; return this; }
        public Builder lateNightMinutes(int v)      { this.lateNightMinutes = v; return this; }
        public Builder lateEarlyMinutes(int v)      { this.lateEarlyMinutes = v; return this; }
        public Builder isHoliday(boolean v)         { this.isHoliday        = v; return this; }
        public Builder isSunday(boolean v)          { this.isSunday         = v; return this; }
        public Builder isBusinessTrip(boolean v)    { this.isBusinessTrip   = v; return this; }
        public Builder isLateEarly(boolean v)       { this.isLateEarly      = v; return this; }
        public Builder isAbsence(boolean v)         { this.isAbsence        = v; return this; }
        public Builder isCompDay(boolean v)         { this.isCompDay        = v; return this; }
        public Builder isPaidHoliday(boolean v)     { this.isPaidHoliday    = v; return this; }
        public Builder isAmPaidHoliday(boolean v)   { this.isAmPaidHoliday  = v; return this; }
        public Builder isPmPaidHoliday(boolean v)   { this.isPmPaidHoliday  = v; return this; }
        public Builder calcAt(LocalDateTime v)      { this.calcAt           = v; return this; }

        public WorkDaily build() {
            if (employeeNo == 0) {
                throw new IllegalStateException("employeeNo は必須です");
            }
            if (workDate == null) {
                throw new IllegalStateException("workDate は必須です");
            }
            return new WorkDaily(this);
        }
    }
}
