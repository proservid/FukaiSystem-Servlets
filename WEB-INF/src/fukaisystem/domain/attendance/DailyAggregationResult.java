package fukaisystem.domain.attendance;

import java.util.Collections;
import java.util.List;

/**
 * {@link DailyAggregator#aggregateAll} の戻り値。
 *
 * <p>バッチ処理で複数レコードを集計した際に、
 * 正常に集計できた結果とスキップされたエラー情報を一括で返す。
 */
public final class DailyAggregationResult {

    /** 正常に集計された日次集計結果 */
    private final List<WorkDaily> successList;

    /** バリデーションエラーや異常データのレコード一覧 */
    private final List<AggregationError> errorList;

    public DailyAggregationResult(List<WorkDaily> successList, List<AggregationError> errorList) {
        this.successList = Collections.unmodifiableList(successList);
        this.errorList   = Collections.unmodifiableList(errorList);
    }

    public List<WorkDaily>       getSuccessList() { return successList; }
    public List<AggregationError> getErrorList()  { return errorList;   }

    /** エラーが1件もなければ true */
    public boolean isAllSuccess() { return errorList.isEmpty(); }

    @Override
    public String toString() {
        return String.format("DailyAggregationResult{success=%d, error=%d}",
            successList.size(), errorList.size());
    }
}
