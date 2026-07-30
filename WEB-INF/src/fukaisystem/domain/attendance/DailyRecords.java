package fukaisystem.domain.attendance;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 1日の全打刻データ
 */
public final class DailyRecords implements Serializable {

	private static final long serialVersionUID = 1L;

    /** 対象日（CSVカラム3） */
    private final LocalDate workDate;

    /** 打刻データ */
    private final List<TimeRecord> records;

    /** 休日FLG */
    private final boolean isHoliday;

    /** 登録年月日 */
    private final LocalDateTime registerDate;

    private DailyRecords(Builder b) {
        this.workDate     = b.workDate;
        this.records      = b.records;
        this.isHoliday    = b.isHoliday;
        this.registerDate = b.registerDate;
    }

    // ---- ゲッター ----------------------------------------------------------------

    public LocalDate        getWorkDate()     { return workDate;     }
    public List<TimeRecord> getRecords()      { return records;      }
    public boolean          isHoliday()       { return isHoliday;    }
    public LocalDateTime    getRegisterDate() { return registerDate; }

    @Override
    public String toString() {
        return String.format(
            "DailyRecords{workDate=%s, isHoliday=%s, registerDate=%s}",
            workDate, isHoliday, registerDate);
    }

    // ---- Builder ----------------------------------------------------------------

    public static Builder builder() { return new Builder(); }

    public static final class Builder {
        private LocalDate        workDate;
        private List<TimeRecord> records;
        private boolean          isHoliday = false;
        private LocalDateTime    registerDate;

        private Builder() {}

        public Builder workDate(LocalDate v)         { this.workDate     = v; return this; }
        public Builder records(List<TimeRecord> v)   { this.records      = v; return this; }
        public Builder holiday(boolean v)            { this.isHoliday    = v; return this; }
        public Builder registerDate(LocalDateTime v) { this.registerDate = v; return this; }

        /** @throws IllegalStateException 必須項目（workDate）が null の場合 */
        public DailyRecords build() {
            if (workDate == null) {
                throw new IllegalStateException("workDate は必須です");
            }
            return new DailyRecords(this);
        }
    }
}
