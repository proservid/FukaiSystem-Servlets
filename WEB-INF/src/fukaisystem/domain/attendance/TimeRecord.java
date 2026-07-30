package fukaisystem.domain.attendance;

import java.io.Serializable;
import java.time.LocalTime;

/**
 * タイムレコーダー由来の打刻データ（1レコード = 1従業員 × 1日）。
 *
 * <p>CSVフォーマット対応カラム：
 * <pre>
 *  従業員番号, 従業員氏名, 年/月/日, 出勤, 外出, 戻り, 退勤, 例外1, 例外2
 * </pre>
 *
 * <p>本クラスは不変オブジェクトとして設計する。
 * インスタンス生成には {@link Builder} を使用すること。
 */
public final class TimeRecord implements Serializable {

	private static final long serialVersionUID = 1L;

    /** 従業員番号（CSVカラム1：先頭ゼロ埋め8桁） */
    private final int employeeNo;

    /** タイムカード番号（CSVカラム1：先頭ゼロ埋め8桁） */
    private final int timeCardNo;

    /** 従業員氏名（CSVカラム2） */
    private final String employeeName;

    /** 出勤時刻（CSVカラム4：空欄は null） */
    private final LocalTime clockIn;

    /** 外出時刻（CSVカラム5：任意、空欄は null） */
    private final LocalTime goOut;

    /** 戻り時刻（CSVカラム6：任意、空欄は null） */
    private final LocalTime returnIn;

    /** 退勤時刻（CSVカラム7：空欄は null） */
    private final LocalTime clockOut;

    /** 出張FLG */
    private final boolean isBusinessTrip;

    /** 有給FLG */
    private final boolean isCompDay;

    /** 有給FLG */
    private final boolean isPaidHoliday;

    /** 午前半休FLG（午前半日分の有給） */
    private final boolean isAmPaidHoliday;

    /** 午後半休FLG（午後半日分の有給） */
    private final boolean isPmPaidHoliday;

    /** 備考 */
    private final String note;

    private TimeRecord(Builder b) {
        this.employeeNo      = b.employeeNo;
        this.timeCardNo      = b.timeCardNo;
        this.employeeName    = b.employeeName;
        this.clockIn         = b.clockIn;
        this.goOut           = b.goOut;
        this.returnIn        = b.returnIn;
        this.clockOut        = b.clockOut;
        this.isBusinessTrip  = b.isBusinessTrip;
        this.isPaidHoliday   = b.isPaidHoliday;
        this.isCompDay       = b.isCompDay;
        this.isAmPaidHoliday = b.isAmPaidHoliday;
        this.isPmPaidHoliday = b.isPmPaidHoliday;
        this.note            = b.note;
    }

    // ---- ゲッター ----------------------------------------------------------------

    public int       getEmployeeNo()   { return employeeNo;      }
    public int       getTimeCardNo()   { return timeCardNo;      }
    public String    getEmployeeName() { return employeeName;    }
    public LocalTime getClockIn()      { return clockIn;         }
    public LocalTime getGoOut()        { return goOut;           }
    public LocalTime getReturnIn()     { return returnIn;        }
    public LocalTime getClockOut()     { return clockOut;        }
    public boolean   isBusinessTrip()  { return isBusinessTrip;  }
    public boolean   isCompDay()       { return isCompDay;       }
    public boolean   isPaidHoliday()   { return isPaidHoliday;   }
    public boolean   isAmPaidHoliday() { return isAmPaidHoliday; }
    public boolean   isPmPaidHoliday() { return isPmPaidHoliday; }
    public String    getNote()         { return note;            }

    @Override
    public String toString() {
        return String.format(
            "TimeRecord{employeeNo='%d', clockIn=%s, goOut=%s, returnIn=%s, clockOut=%s}",
            employeeNo, clockIn, goOut, returnIn, clockOut);
    }

    // ---- Builder ----------------------------------------------------------------

    public static Builder builder() { return new Builder(); }

    public static final class Builder {
        private int       employeeNo;
        private int       timeCardNo;
        private String    employeeName;
        private LocalTime clockIn;
        private LocalTime goOut;
        private LocalTime returnIn;
        private LocalTime clockOut;
        private boolean   isBusinessTrip;
        private boolean   isCompDay;
        private boolean   isPaidHoliday;
        private boolean   isAmPaidHoliday;
        private boolean   isPmPaidHoliday;
        private String    note;

        private Builder() {}

        public Builder employeeNo(int v)        { this.employeeNo      = v; return this; }
        public Builder timeCardNo(int v)        { this.timeCardNo      = v; return this; }
        public Builder employeeName(String v)   { this.employeeName    = v; return this; }
        public Builder clockIn(LocalTime v)     { this.clockIn         = v; return this; }
        public Builder goOut(LocalTime v)       { this.goOut           = v; return this; }
        public Builder returnIn(LocalTime v)    { this.returnIn        = v; return this; }
        public Builder clockOut(LocalTime v)    { this.clockOut        = v; return this; }
        public Builder businessTrip(boolean v)  { this.isBusinessTrip  = v; return this; }
        public Builder compDay(boolean v)       { this.isCompDay       = v; return this; }
        public Builder paidHoliday(boolean v)   { this.isPaidHoliday   = v; return this; }
        public Builder amPaidHoliday(boolean v) { this.isAmPaidHoliday = v; return this; }
        public Builder pmPaidHoliday(boolean v) { this.isPmPaidHoliday = v; return this; }
        public Builder note(String v)           { this.note            = v; return this; }

        /** @throws IllegalStateException 必須項目（employeeNo）が null の場合 */
        public TimeRecord build() {
            if (employeeNo == 0) {
                throw new IllegalStateException("employeeNo は必須です");
            }
            return new TimeRecord(this);
        }
    }
}
