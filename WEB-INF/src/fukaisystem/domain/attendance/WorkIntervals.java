package fukaisystem.domain.attendance;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 労働区間（{@code int[]{開始分, 終了分}} のリスト）に対する区間演算ユーティリティ。
 *
 * <p>時刻はすべて「勤務日 00:00 を 0 とした分数」で表現する。
 * 区間は半開区間 [開始, 終了) として扱い、境界がちょうど一致する場合は重ならないものとみなす。
 *
 * <p>{@link DailyAggregator}（打刻からの日次集計）と
 * {@link WorkSchedule#calcWorkMinutes(int, int)}（任意区間の実働算出）の双方がこのクラスを使う。
 * 控除の合成順序を問わず結果が一意に定まるため、
 * 「始業前を切り捨てる」「休憩を控除する」といった操作を順に適用するだけで実働時間が求まる。
 *
 * <p>本クラスはインスタンス化しない。
 */
public final class WorkIntervals {

    /** 1日の分数（24時間）。日付またぎの補正に使う */
    public static final int MINUTES_PER_DAY = 24 * 60;   // 1440

    private WorkIntervals() {
        throw new AssertionError("インスタンス化できません");
    }

    /**
     * 指定した控除期間 [deductStart, deductEnd) を労働区間リストから除去する
     * （境界がちょうど一致する区間は控除されない）。
     *
     * <p>控除期間と重なる各区間を「控除前」「控除後」の2部分に分割し、
     * 長さゼロの区間は除去して返す。
     *
     * <p>呼び出し例:
     * <pre>
     *   // 昼休み控除
     *   intervals = WorkIntervals.deductPeriod(intervals, 休憩開始, 休憩終了);
     *   // 定時後休憩控除
     *   intervals = WorkIntervals.deductPeriod(intervals, 終業, 残業開始);
     * </pre>
     *
     * @param intervals   元の労働区間リスト
     * @param deductStart 控除開始（分）
     * @param deductEnd   控除終了（分）
     * @return 控除後の労働区間リスト（新規リスト）
     */
    public static List<int[]> deductPeriod(List<int[]> intervals, int deductStart, int deductEnd) {
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
     *
     * @param intervals 労働区間リスト
     * @return 合計時間（分）
     */
    public static int sumIntervals(List<int[]> intervals) {
        int total = 0;
        for (int[] iv : intervals) {
            total += iv[1] - iv[0];
        }
        return total;
    }

    /**
     * 2区間 [aStart, aEnd) と [bStart, bEnd) の重複時間（分）を返す。
     * 重複がない場合は 0。
     *
     * @param aStart 区間Aの開始（分）
     * @param aEnd   区間Aの終了（分）
     * @param bStart 区間Bの開始（分）
     * @param bEnd   区間Bの終了（分）
     * @return 重複時間（分）
     */
    public static int overlap(int aStart, int aEnd, int bStart, int bEnd) {
        return Math.max(0, Math.min(aEnd, bEnd) - Math.max(aStart, bStart));
    }

    /**
     * {@link LocalTime} を 00:00 起点の分数に変換する（秒・ナノ秒は切り捨て）。
     *
     * @param time 変換する時刻
     * @return 00:00 起点の分数
     */
    public static int toMinutes(LocalTime time) {
        return time.getHour() * 60 + time.getMinute();
    }
}
