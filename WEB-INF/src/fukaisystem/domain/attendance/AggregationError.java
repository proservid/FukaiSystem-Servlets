package fukaisystem.domain.attendance;

/**
 * 集計処理中に発生したエラーを表す値オブジェクト。
 *
 * <p>{@link DailyAggregationResult} の一部として返される。
 * エラーが存在する場合でも正常に集計できた他のレコードには影響しない。
 */
public final class AggregationError {

    /** エラー種別 */
    public enum Type {
        /** 出勤または退勤が未打刻 */
        MISSING_PUNCH,
        /** 打刻の時刻順序が不正（例: 退勤 < 出勤 かつ深夜またぎでもない） */
        INVALID_TIME_ORDER,
        /** 外出/戻りの片方のみ記録されている */
        INCOMPLETE_EXTERNAL,
        /** 実労働時間が24時間を超える（データ異常） */
        EXCESSIVE_WORK_HOURS
    }

    private final Type   type;
    private final int    employeeNo;
    private final String workDate;
    private final String message;

    public AggregationError(Type type, int employeeNo, String workDate, String message) {
        this.type       = type;
        this.employeeNo = employeeNo;
        this.workDate   = workDate;
        this.message    = message;
    }

    public Type   getType()       { return type;       }
    public int    getEmployeeNo() { return employeeNo; }
    public String getWorkDate()   { return workDate;   }
    public String getMessage()    { return message;    }

    @Override
    public String toString() {
        return String.format("[%s] %d %s : %s", type, employeeNo, workDate, message);
    }
}
