package fukaisystem.util;

import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import org.apache.log4j.Logger;

/**
 * 部品入荷確認アプリ(FugaTracker)へ入荷を通知する
 *
 * 注文登録で入荷年月日が新たに入力された部品を、同じ Tomcat 上の
 * FugaTracker の入荷通知 API へ POST する。受け取った FugaTracker が
 * WebSocket で工場モニタ(入荷表示アプリ)と部品入荷確認アプリへ配信する。
 *
 * 入荷そのものは T_在庫_子 への登録で成立しており、FugaTracker はそれを読む。
 * この通知は「今入荷した」ことを即座に伝えるためのもので、内容は登録済みの
 * 在庫明細と一致している。
 *
 * 通知は業務処理の付随であり、失敗しても注文登録そのものは成立させる。
 * このため送信はデーモンスレッドで非同期に行い、規定回数の再送に失敗したら
 * ログに残すだけとする。同期送信にすると FugaTracker の停止時に
 * クライアントを待たせることにもなる。
 */
public final class ArrivalNotifier {

	protected static final Logger logger = Logger.getLogger("A1");

	/** 通知先 URL を差し替えるためのコンテキストパラメータ名 */
	public static final String URL_PARAMETER = "tracker.arrivals.url";

	/** 通知先 URL の既定値。同一 Tomcat 内のためループバックで届く */
	private static final String DEFAULT_URL = "http://localhost:8080/FugaTracker/api/arrivals";

	/** 接続・読み取りのタイムアウト(ミリ秒) */
	private static final int TIMEOUT_MILLIS = 3000;

	/** 1 件の入荷を送信する最大試行回数 */
	private static final int MAX_ATTEMPTS = 5;

	/** 再送までの待ち時間の基準(ミリ秒)。試行のたびに倍にする */
	private static final long BASE_BACKOFF_MILLIS = 500;

	/** 再送までの待ち時間の上限(ミリ秒) */
	private static final long MAX_BACKOFF_MILLIS = 8000;

	/** 再送する価値のある失敗理由に付ける印 */
	private static final String RETRIABLE = "[再送可] ";

	private ArrivalNotifier() {}

	/**
	 * 入荷を非同期で通知する
	 *
	 * 送信ごとに使い捨てのデーモンスレッドを起こす。スレッドプールを持つと
	 * コンテキストの再読み込み後もスレッドが残り、クラスローダリークの原因になる。
	 * 注文登録は人手の操作であり頻度が低く、1 スレッドの寿命もタイムアウトの
	 * 範囲に収まるため、都度生成で足りる。
	 *
	 * @param url 通知先 URL。null または空の場合は既定値を使う
	 * @param arrivals 通知する入荷。空の場合は何もしない
	 */
	public static void notifyArrivals(String url, List<Arrival> arrivals) {
		if (arrivals.isEmpty()) {
			return;
		}
		final String endpoint = (url == null || url.trim().isEmpty()) ? DEFAULT_URL : url;
		final List<Arrival> targets = new ArrayList<Arrival>(arrivals);
		Thread thread = new Thread(new Runnable() {
			@Override
			public void run() {
				for (Arrival arrival : targets) {
					send(endpoint, arrival);
				}
			}
		}, "arrival-notifier");
		thread.setDaemon(true);
		thread.start();
	}

	/**
	 * 入荷 1 件を送信する
	 *
	 * 相手が再起動中などで一時的に落ちていることがあるため、失敗したら
	 * 指数バックオフで間隔を空けながら {@link #MAX_ATTEMPTS} 回まで試みる。
	 * 待ち時間にはジッターを掛ける。複数の入荷をまとめて通知した直後に
	 * 相手が落ちていると、再送が同じ時刻に集中してしまうため。
	 *
	 * 送信内容の誤り(4xx)は何度送っても結果が変わらないので再送しない。
	 *
	 * @param endpoint 通知先 URL
	 * @param arrival 通知する入荷
	 */
	private static void send(String endpoint, Arrival arrival) {
		for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
			String failure = trySend(endpoint, arrival);
			if (failure == null) {
				return;
			}
			if (!failure.startsWith(RETRIABLE)) {
				logger.warn("入荷通知に失敗しました: " + arrival.toJson() + " / " + failure);
				return;
			}
			if (attempt == MAX_ATTEMPTS) {
				logger.warn(
					"入荷通知を" + MAX_ATTEMPTS + "回試みましたが送信できませんでした: "
						+ arrival.toJson() + " / " + failure.substring(RETRIABLE.length())
				);
				return;
			}
			if (!waitBeforeRetry(attempt)) {
				logger.warn("入荷通知の再送待ちが中断されました: " + arrival.toJson());
				return;
			}
		}
	}

	/**
	 * 入荷 1 件の送信を 1 回試みる
	 *
	 * @param endpoint 通知先 URL
	 * @param arrival 通知する入荷
	 *
	 * @return 成功した場合は {@code null}。失敗した場合はその理由。
	 *         再送する価値がある理由には先頭に {@link #RETRIABLE} を付ける
	 */
	private static String trySend(String endpoint, Arrival arrival) {
		HttpURLConnection connection = null;
		try {
			// new URL(String) は新しい JDK で非推奨のため URI 経由で組み立てる
			connection = (HttpURLConnection) new URI(endpoint).toURL().openConnection();
			connection.setRequestMethod("POST");
			connection.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
			connection.setConnectTimeout(TIMEOUT_MILLIS);
			connection.setReadTimeout(TIMEOUT_MILLIS);
			connection.setDoOutput(true);

			byte[] body = arrival.toJson().getBytes("UTF-8");
			OutputStream out = connection.getOutputStream();
			try {
				out.write(body);
				out.flush();
			} finally {
				out.close();
			}

			int status = connection.getResponseCode();
			if (status >= 200 && status < 300) {
				return null;
			}
			// 5xx は相手側の一時的な不調の可能性があるため再送する
			return (status >= 500 ? RETRIABLE : "") + "HTTP " + status;
		} catch (IOException e) {
			return RETRIABLE + e;
		} catch (URISyntaxException e) {
			return "通知先の URL が不正です: " + endpoint;
		} finally {
			if (connection != null) {
				connection.disconnect();
			}
		}
	}

	/**
	 * 次の送信まで待つ
	 *
	 * 待ち時間は 1 回目から 500ms, 1s, 2s, 4s と倍にしていき、その範囲内で
	 * ランダムに散らす(フルジッター)。
	 *
	 * @param attempt 何回目の送信が失敗したか(1 始まり)
	 *
	 * @return 待てた場合は {@code true}。割り込まれた場合は {@code false}
	 */
	private static boolean waitBeforeRetry(int attempt) {
		long ceiling = Math.min(MAX_BACKOFF_MILLIS, BASE_BACKOFF_MILLIS << (attempt - 1));
		try {
			Thread.sleep((long) (ceiling * ThreadLocalRandom.current().nextDouble()));
			return true;
		} catch (InterruptedException e) {
			// コンテキストの停止などで割り込まれた場合。フラグを戻して呼び出し元に任せる
			Thread.currentThread().interrupt();
			return false;
		}
	}

	/**
	 * 通知する入荷 1 件
	 *
	 * 在庫親ID・明細IDは在庫明細(T_在庫_子)の主キーで、部品入荷確認アプリはこの 2 つから
	 * 入荷イベント ID を組み立てる。入荷済み一覧取得(GET /api/arrivals)も同じ主キーから
	 * ID を作るため、同じ入荷が別の ID になることがなく、工場モニタが重複表示しない。
	 *
	 * 製番・仕入先名は必須。図面番号・部品番号を指定すると、部品入荷確認アプリ側で
	 * その部品を入荷済にする。部品に紐付かない材料の行では両方とも指定しない。
	 */
	public static final class Arrival {

		private final int stockParentId;

		private final int stockDetailId;

		private final String productionNo;

		private final String supplier;

		private final String partName;

		private final String drawingNo;

		private final Integer partNo;

		private final String location;

		public Arrival(int stockParentId, int stockDetailId, String productionNo, String supplier,
			String partName, String drawingNo, Integer partNo, String location) {
			this.stockParentId = stockParentId;
			this.stockDetailId = stockDetailId;
			this.productionNo = productionNo;
			this.supplier = supplier;
			this.partName = partName;
			this.drawingNo = drawingNo;
			this.partNo = partNo;
			this.location = location;
		}

		/**
		 * 入荷通知 API のリクエストボディを組み立てる
		 *
		 * このアプリには JSON ライブラリが無いため手組みする。項目数が少なく
		 * 文字列と整数しか無いため、エスケープさえ行えば十分である。
		 *
		 * 図面番号と部品番号は、受け取り側が「両方あるか両方無いか」を検査するため
		 * 揃っているときだけ送る。
		 *
		 * @return JSON 文字列
		 */
		String toJson() {
			StringBuilder sb = new StringBuilder();
			sb.append("{\"stockParentId\":").append(stockParentId);
			sb.append(",\"stockDetailId\":").append(stockDetailId);
			sb.append(",\"productionNo\":").append(quote(productionNo));
			sb.append(",\"supplier\":").append(quote(supplier));
			if (partName != null && !partName.trim().isEmpty()) {
				sb.append(",\"partName\":").append(quote(partName));
			}
			if (drawingNo != null && !drawingNo.trim().isEmpty() && partNo != null) {
				sb.append(",\"drawingNo\":").append(quote(drawingNo));
				sb.append(",\"partNo\":").append(partNo.intValue());
			}
			if (location != null && !location.trim().isEmpty()) {
				sb.append(",\"location\":").append(quote(location));
			}
			sb.append("}");
			return sb.toString();
		}
	}

	/** 文字列を JSON の文字列リテラルにする */
	static String quote(String value) {
		if (value == null) {
			return "null";
		}
		StringBuilder sb = new StringBuilder("\"");
		for (int i = 0; i < value.length(); i++) {
			char c = value.charAt(i);
			switch (c) {
				case '"':
					sb.append("\\\"");
					break;
				case '\\':
					sb.append("\\\\");
					break;
				case '\b':
					sb.append("\\b");
					break;
				case '\f':
					sb.append("\\f");
					break;
				case '\n':
					sb.append("\\n");
					break;
				case '\r':
					sb.append("\\r");
					break;
				case '\t':
					sb.append("\\t");
					break;
				default:
					if (c < 0x20) {
						sb.append(String.format("\\u%04x", (int) c));
					} else {
						sb.append(c);
					}
					break;
			}
		}
		return sb.append("\"").toString();
	}
}
