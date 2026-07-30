package fukaisystem.domain.attendance;

/**
 * 集計処理中に発生した業務例外。
 *
 * <p>バリデーションエラー（時刻順序不正・超過時間等）を
 * {@link DailyAggregator} から呼び出し元へ通知するために使用する。
 */
public class AggregationException extends RuntimeException {

    private final AggregationError.Type errorType;

    public AggregationException(AggregationError.Type errorType,
                                int employeeNo,
                                String workDate,
                                String message) {
        super("[" + errorType + "] 従業員=" + employeeNo + " 日付=" + workDate + " : " + message);
        this.errorType = errorType;
    }

    public AggregationError.Type getErrorType() {
        return errorType;
    }
}
