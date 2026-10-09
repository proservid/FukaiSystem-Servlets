package fukaisystem.application.business;

import java.io.IOException;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import javax.servlet.ServletResponse;

import org.apache.log4j.Logger;

import fukaisystem.dto.business.OrderDocumentDTO;
import fukaisystem.foundation.ServiceFoundation;
import fukaisystem.util.ArrivalNotifier;

/**
 * 注文データを登録する
 *
 * 入荷年月日が新たに入力された部品は、部品入荷確認アプリ(FugaTracker)へ
 * 入荷として通知する。通知はコミット後に行う({@link #access} を参照)。
 *
 * <p>注文番号と伝票番号は {@code MAX()+1} でアプリ側が採番する。既定の READ COMMITTED では
 * 最大値を読むために取った共有ロックが読み終えた時点で解放されるため、
 * 同時に登録すると同じ番号を採ってしまう。これを防ぐため、採番のために読む T_在庫_親 には
 * {@code WITH (UPDLOCK, HOLDLOCK)} を指定し、登録し終えるまでロックを保持する。
 *
 * <p>既存の注文を扱う場合は、入荷済件数を数える前に対象の在庫親IDをロックし、
 * 数えてから明細を入れ直し終えるまで別の登録が割り込まないようにする。
 * 割り込まれると入荷通知の差分がずれ、鳴るはずのチャイムが鳴らない・
 * 鳴らないはずのチャイムが鳴るといったことが起きる。
 *
 * <p>〆後の納品書日を弾く判定でも T_指定納品書 をロックする。判定と登録の間に
 * {@link fukaisystem.application.aggregate.SetClose} で〆られると、〆後のデータが登録できてしまう。
 */
public class OrderRegister extends ServiceFoundation {

	protected static final Logger logger = Logger.getLogger("A1");

	/*
	 * 明細行(Vector)の列位置
	 *
	 * 並びは T_在庫_子 の列順に合わせてあり、納品書まわりの 5 項目がその後ろに続く。
	 *   0:表示CD 1:大分類CD 2:中分類CD 3:小分類CD 4:材料品名 5:各FLG 6:数量 7:数量単位CD
	 *   8:重量長さ 9:単価 10:金額 11:図面番号 12:部品番号 13:備考 14:入庫年月日 15:入庫場所
	 *   16:入荷チェック 17:納品書番号 18:納品書日 19:消費税 20:納品書チェック 21:〆FLG
	 *
	 * 図面番号・部品番号の追加で位置が動いたため、以降の項目は定数で参照する。
	 * 画面側(OrderSelectedInfoPanel)の列順と必ず一致させること。
	 */

	/** 納品書番号の列位置 */
	private static final int SLIP_NO_INDEX = 17;

	/** 納品書日の列位置 */
	private static final int SLIP_DATE_INDEX = 18;

	/** 消費税の列位置 */
	private static final int SLIP_TAX_INDEX = 19;

	/** 納品書チェックの列位置 */
	private static final int SLIP_CHECKED_INDEX = 20;

	/** 〆FLGの列位置 */
	private static final int SLIP_CLOSED_INDEX = 21;

	/**
	 * 今回の登録で新たに入荷となった部品。コミット後に通知する
	 *
	 * サーブレットは単一のインスタンスを全リクエストで共有するため、
	 * 同時に登録が行われても混ざらないようスレッドごとに保持する。
	 */
	private final ThreadLocal<List<ArrivalNotifier.Arrival>> arrivals =
		new ThreadLocal<List<ArrivalNotifier.Arrival>>();

	/**
	 * コミットが完了してから入荷を通知する
	 *
	 * {@link ServiceFoundation#access} は transaction() が null を返すとロールバックする。
	 * transaction() の中で通知してしまうと、ロールバックされた入荷でも
	 * 工場モニタのチャイムが鳴ってしまうため、ここで通知する。
	 */
	@Override
	protected Object access(Connection c, ServletResponse response, Object obj) throws Exception {
		arrivals.set(new ArrayList<ArrivalNotifier.Arrival>());
		try {
			Object result = super.access(c, response, obj);
			if (result != null) {
				ArrivalNotifier.notifyArrivals(
					getServletContext().getInitParameter(ArrivalNotifier.URL_PARAMETER),
					arrivals.get()
				);
			}
			return result;
		} finally {
			// リクエストのスレッドは使い回されるため必ず片付ける
			arrivals.remove();
		}
	}

	@Override
	public Object transaction(Connection c, ServletResponse response, Object o) throws IOException, SQLException {

		OrderDocumentDTO odd = cast(response, o, OrderDocumentDTO.class);
		int orderID = -1;
		Map<Integer, DeliverySlip> taxMap = null;
		// 登録前に入荷済だった部品の件数(図面番号+部品番号ごと)。新規登録の場合は空のまま
		Map<String, Integer> arrivedCounts = new HashMap<String, Integer>();

		boolean isParentEditable = true; // 納品書番号が入ったデータが１つでもあるか
		taxMap = new HashMap<Integer, DeliverySlip>(); // 指定納品書のデータ
		List<Vector<Object>> regVector = new ArrayList<Vector<Object>>();
		for (Vector<Object> record : odd.getDataVector()) {
			boolean closeFlg = false;
			boolean matchFlg = false;
			if (record.get(SLIP_NO_INDEX) == null || record.get(SLIP_DATE_INDEX) == null
				|| !(Boolean) record.get(SLIP_CHECKED_INDEX)) { // 納品書番号か納品日がnullまたは納品書チェックなし
				// 納品書入力をクリアし、登録用データに追加
				record.set(SLIP_NO_INDEX, 0);
				record.set(SLIP_DATE_INDEX, null);
				record.set(SLIP_TAX_INDEX, 0);
				record.set(SLIP_CHECKED_INDEX, false);
				regVector.add(record);
			} else {
				// 〆後の日付の指定納品書を追加させない
				try (
					PreparedStatement ps = c.prepareStatement(
						"select * from T_指定納品書 WITH (UPDLOCK, HOLDLOCK)"
							+ " WHERE 納品書日>=? and 納品書日<? and 〆FLG='true'"
					);
				) {
					java.util.Date d = (java.util.Date) record.get(SLIP_DATE_INDEX);
					Calendar cal = Calendar.getInstance();
					cal.setTime(d);
					cal.set(Calendar.DATE, 1); // その月の１日
					ps.setDate(1, new Date(cal.getTimeInMillis()));
					cal.add(Calendar.MONTH, 1); // 翌月１日
					ps.setDate(2, new Date(cal.getTimeInMillis()));
					try (ResultSet rs = ps.executeQuery();) {
						while (rs.next()) {
							// 一致する納品書データがあるか
							if (rs.getInt("ID") == (Integer) record.get(SLIP_NO_INDEX)) {
								if ((Boolean) record.get(SLIP_CLOSED_INDEX)) {
									matchFlg = true;
									// 1行でも指定納品書が〆られていたら親データの編集を不可に
									isParentEditable = false;
									// 〆後なので変更されてはいないはずだが念のため登録データをセットしておく(IDは一致確認済)
									record.set(SLIP_DATE_INDEX, rs.getDate("納品書日"));
									record.set(SLIP_TAX_INDEX, rs.getInt("消費税"));
									record.set(SLIP_CHECKED_INDEX, true);
									record.set(SLIP_CLOSED_INDEX, true);
									regVector.add(record);
								}
							}
							// 一致する納品書データがなくてもその月が〆られてさえいれば(その行の)closeFlgをtrueに
							closeFlg = true;
						}
					}
				}
				if (!closeFlg) { // その行の納品書日が〆後の日付でなければ
					// データはそのまま使用
					regVector.add(record);
					// 納品書番号、納品書日、消費税、〆FLGをMapにセット（後でT_指定納品書にmergeするため）
					if (!taxMap.containsKey(record.get(SLIP_NO_INDEX))
						|| !(Boolean) record.get(SLIP_CLOSED_INDEX)) {
						// 納品書番号が同じデータ又は〆後データは省く
						taxMap.put(
							(Integer) record.get(SLIP_NO_INDEX),
							new DeliverySlip(
								new Date(((java.util.Date) record.get(SLIP_DATE_INDEX)).getTime()),
								record.get(SLIP_TAX_INDEX) == null ? 0 : (Integer) record.get(SLIP_TAX_INDEX)
							)
						);
					}
				} else {
					// 〆後の日付で
					if (!matchFlg) {
						// 登録されていない番号 または
						// 登録はあるが〆Flgがfalse(=番号入力ミス)
						// クリアし、登録用データに追加
						record.set(SLIP_NO_INDEX, 0);
						record.set(SLIP_DATE_INDEX, null);
						record.set(SLIP_TAX_INDEX, 0);
						record.set(SLIP_CHECKED_INDEX, false);
						regVector.add(record);
						addError("〆後の納品書日では登録できません");
					}
				}
			}
		}
		// if(closeFlg) addError("〆後の納品書日が入力されている行があります\n納品書データは〆後の日付で登録できません");
		//////////////////////////////////////////////////////////////////////////////////////////////////////
		orderID = odd.orderID();
		if (orderID != 0) {
			// 入荷済の件数を数えてから明細を入れ直し終えるまで、この注文への割り込みを防ぐ
			lock(c, "T_在庫_親", "在庫親ID=?", orderID);
			// 明細を入れ直す前に、どの部品が既に入荷済だったかを控えておく(入荷通知の差分判定用)
			arrivedCounts = countArrived(c, orderID);
		}
		if (odd.orderNum1() + odd.orderNum2() == 0) {
			// 注文期と注文番号を0に変更したということは、消去せよということ
			if (isParentEditable) {
				// 削除
				if (orderID != 0) {
					try (PreparedStatement ps = c.prepareStatement("DELETE FROM T_在庫_親 WHERE 在庫親ID=?");) {
						ps.setInt(1, orderID);
						ps.executeUpdate();
					}
					try (PreparedStatement ps = c.prepareStatement("DELETE FROM T_在庫_子 WHERE 在庫親ID=?");) {
						ps.setInt(1, orderID);
						ps.executeUpdate();
						orderID = 0;
					}
				}
			}
		} else {
			int orderNum = odd.orderNum2();
			// 在庫用注文番号（≠注文書番号）自動採番
			if (orderNum == 0) {
				try (
					PreparedStatement ps = c.prepareStatement(
						"SELECT CASE WHEN MAX(注文番号) IS NULL THEN 1 ELSE MAX(注文番号)+1 END AS 新注文番号"
							+ " FROM T_在庫_親 WITH (UPDLOCK, HOLDLOCK) WHERE 注文期=? AND 注文番号<9999"
					);
				) {
					ps.setInt(1, odd.orderNum1()); // 注文期
					try (ResultSet rs = ps.executeQuery();) {
						if (rs.next()) {
							orderNum = rs.getInt("新注文番号");
						}
					}
				}
			}

			if (orderID == 0 || odd.orderSlipNum() == 0) { // ID又は注文書番号が0 → 新規

				try (
					PreparedStatement ps = c.prepareStatement(
						"INSERT INTO T_在庫_親" +
							" OUTPUT inserted.在庫親ID as newId, inserted.更新日" +
							" SELECT ?, ?, ?, MAX(伝票番号)+1, ?, ?, ?, ?, ?, ?, ? FROM T_在庫_親 WITH (UPDLOCK, HOLDLOCK)"
					);
				) {
					int i = 1;
					ps.setInt(i++, odd.orderNum1()); // 注文期
					ps.setInt(i++, orderNum); // 注文番号
					ps.setString(i++, odd.orderNum3()); // 注文枝番
					// 自動採番
					// ps.setInt(i++, odd.getInt(3)); //伝票番号
					ps.setInt(i++, odd.accountID()); // 仕入先CD
					ps.setDate(i++, odd.publishDate()); // 注文年月日
					ps.setDate(i++, odd.dueDate()); // 指定納期
					ps.setString(i++, odd.note1()); // 摘要
					ps.setString(i++, odd.note2()); // 納入先指定
					ps.setTimestamp(i++, new Timestamp(new java.util.Date().getTime())); // 更新日
					ps.setInt(i++, 0); // 更新者CD
					boolean isResultSet = ps.execute();
					int updateCount = 0;
					while (true) {
						if (isResultSet) {
							try (ResultSet rs = ps.getResultSet();) {
								while (rs.next()) {
									orderID = rs.getInt(1);
								}
							}
						} else {
							updateCount = ps.getUpdateCount();
							if (updateCount == -1) {
								break;
							}
						}
						isResultSet = ps.getMoreResults();
					}
				}

			} else {
				// 更新
				if (isParentEditable) {
					try (
						PreparedStatement ps = c.prepareStatement(
							"UPDATE T_在庫_親 SET"
								+ " 注文期=?, 注文番号=?, 注文枝番=?, 仕入先CD=?, 注文年月日=?, 指定納期=?,"
								+ " 摘要=?, 納入先指定=?, 更新日=?, 更新者CD=?"
								+ " WHERE 在庫親ID=?"
						);
					) {
						int i = 1;
						ps.setInt(i++, odd.orderNum1()); // 注文期
						ps.setInt(i++, orderNum); // 注文番号
						ps.setString(i++, odd.orderNum3()); // 注文枝番
						// 自動採番
						// ps.setInt(i++, odd.getInt(3)); //伝票番号
						ps.setInt(i++, odd.accountID()); // 仕入先CD
						ps.setDate(i++, odd.publishDate()); // 注文年月日
						ps.setDate(i++, odd.dueDate()); // 指定納期
						ps.setString(i++, odd.note1()); // 摘要
						ps.setString(i++, odd.note2()); // 納入先指定
						ps.setTimestamp(i++, new Timestamp(new java.util.Date().getTime())); // 更新日
						ps.setInt(i++, 0); // 更新者CD
						ps.setInt(i++, orderID); // 在庫親ID
						ps.executeUpdate();
					}
				}
				// 子孫のデータ更新は、削除→追加にて
				// T_在庫_子とT_指定納品書のinnerjoinにおいて、それぞれの〆FLGに不一致のものがあったときは
				// 〆後に〆前の画面から登録しようとした→エラー
				try (PreparedStatement ps = c.prepareStatement("DELETE FROM T_在庫_子 WHERE 在庫親ID=?");) {
					// where以降
					ps.setInt(1, orderID); // 在庫親ID
					ps.executeUpdate();
				}
			}

			int k = 1;
			try (
				PreparedStatement ps = c.prepareStatement(
					"INSERT INTO T_在庫_子 ("
						+ "在庫親ID, ID, 表示CD, 大分類CD, 中分類CD, 小分類CD, 材料品名, 各FLG,"
						+ " 数量, 数量単位CD, 重量長さ, 単価, 金額, 図面番号, 部品番号,"
						+ " 備考, 入庫年月日, 入庫場所, 納品書番号"
						+ ") VALUES("
						+ "?, ?, ?, ?, ?, ?, ?, ?, ?, ?,"
						+ "?, ?, ?, ?, ?, ?, ?, ?, ?)"
				);
			) {
				for (Vector<Object> record : regVector) {
					int tag = (Integer) record.get(0);
					if (tag != 0) {
						int i = 1;
						int j = 0;
						ps.setInt(i++, orderID); // 親ID
						ps.setInt(i++, k); // ID
						ps.setInt(i++, (Integer) record.get(j++)); // 表示CD
						ps.setInt(i++, (record.get(j) == null) ? 0 : (Integer) record.get(j));
						j++; // 大分類CD
						ps.setInt(i++, (record.get(j) == null) ? 0 : (Integer) record.get(j));
						j++; // 中分類CD
						ps.setInt(i++, (record.get(j) == null) ? 0 : (Integer) record.get(j));
						j++; // 小分類CD
						String partName = (String) record.get(j++); // 名称
						ps.setString(i++, partName);
						ps.setBoolean(i++, (Boolean) record.get(j++)); // 各FLG
						int quantity = (Integer) record.get(j++); // 数量
						ps.setInt(i++, quantity);
						ps.setInt(i++, (Integer) record.get(j++)); // 数量単位CD
						ps.setDouble(i++, (Double) record.get(j++)); // 重量長さ（単位を要検討のこと）
						ps.setInt(i++, (Integer) record.get(j++)); // 単価
						ps.setInt(i++, (Integer) record.get(j++)); // 金額
						String drawingNo = (String) record.get(j++);
						if (drawingNo != null && drawingNo.trim().isEmpty()) {
							drawingNo = null; // 未入力は空文字ではなく NULL で持つ
						}
						ps.setString(i++, drawingNo); // 図面番号
						Integer partNo = (Integer) record.get(j++);
						if (partNo == null) {
							ps.setNull(i++, java.sql.Types.SMALLINT); // 部品番号
						} else {
							ps.setInt(i++, partNo);
						}
						ps.setString(i++, (String) record.get(j++)); // 備考
						java.util.Date arrivedDate = (java.util.Date) record.get(j++);
						ps.setDate(i++, arrivedDate == null ? null : new java.sql.Date(arrivedDate.getTime()));
						String location = (String) record.get(j);
						if (location != null && location.trim().isEmpty()) {
							location = null; // 未入力は空文字ではなく NULL で持つ
						}
						ps.setString(i++, location); // 入庫場所
						j += 2; // 入庫場所（チェックボックスを飛ばすためj+=2）
						ps.setInt(i, record.get(j) == null ? 0 : (Integer) record.get(j)); // 納品書番号
						ps.addBatch();

						if (arrivedDate != null
							&& isNewArrival(arrivedCounts, drawingNo, partNo, partName, quantity)) {
							String supplier = odd.accountName();
							if (supplier == null || supplier.trim().isEmpty()) {
								// 仕入先名は工場のモニタに表示されるため、欠けたままでは通知しない
								logger.warn("仕入先名が取得できないため入荷を通知しません: " + partName);
							} else {
								arrivals.get().add(
									new ArrivalNotifier.Arrival(
										orderID,
										k,
										productionNo(odd.orderNum1(), orderNum, odd.orderNum3()),
										supplier,
										partName,
										drawingNo,
										partNo,
										location
									)
								);
							}
						}
						k++;
					}
				}
				int[] updateCounts = ps.executeBatch();
				logger.info("T_在庫_子は" + updateCounts.length + "件処理されました。");
			}

			try (
				PreparedStatement ps = c.prepareStatement(
					"MERGE INTO T_指定納品書 WITH (HOLDLOCK) AS t"
						+ " USING (SELECT ? AS ID, ? AS 納品書日, ? AS 消費税) AS w"
						+ "  ON t.ID=w.ID AND t.〆FLG='false'"
						+ " WHEN MATCHED THEN"
						+ "  UPDATE SET"
						+ "   t.ID = w.ID,"
						+ "   t.納品書日 = w.納品書日,"
						+ "   t.消費税 = w.消費税"
						+ " WHEN NOT MATCHED THEN"
						+ "  INSERT VALUES(w.ID, w.納品書日, w.消費税, 'false');"
				);
			) {
				for (Map.Entry<Integer, DeliverySlip> e : taxMap.entrySet()) {
					ps.setInt(1, e.getKey()); // ID
					ps.setDate(2, e.getValue().getDate());
					ps.setInt(3, e.getValue().getTax()); // 消費税
					ps.addBatch();
				}
				int[] updateCounts = ps.executeBatch();
				logger.info("T_指定納品書は" + updateCounts.length + "件処理されました。");
			}

			// 注文書データで使用していない納品書番号は削除する
			try (
				PreparedStatement ps = c.prepareStatement(
					"delete from T_指定納品書 where ID IN ("
						+ "select ID from T_指定納品書 f WHERE NOT EXISTS("
						+ "SELECT 1 FROM T_在庫_子 c WHERE c.納品書番号=f.ID))"
				);
			) {
				ps.executeUpdate();
			}
		}
		return orderID;
	}

	/**
	 * 登録前に入荷済だった部品の件数を、部品を表すキーごとに数える
	 *
	 * 注文登録は明細を全行 DELETE してから INSERT し直すため、入庫年月日が
	 * 入っているというだけでは、今回入荷したのか以前から入荷済だったのかを
	 * 区別できない。登録前の状態と突き合わせるためにここで控えておく。
	 *
	 * 同じキーの行は複数ありうるので、キーごとに件数で持つ。
	 *
	 * @param c Connectionオブジェクト
	 * @param orderID 在庫親ID
	 *
	 * @return 部品を表すキーごとの、入庫年月日が入っていた行の件数
	 * @throws SQLException
	 */
	private Map<String, Integer> countArrived(Connection c, int orderID) throws SQLException {
		Map<String, Integer> counts = new HashMap<String, Integer>();
		try (
			PreparedStatement ps = c.prepareStatement(
				"SELECT 図面番号, 部品番号, 材料品名, 数量 FROM T_在庫_子"
					+ " WHERE 在庫親ID=? AND 入庫年月日 IS NOT NULL"
			);
		) {
			ps.setInt(1, orderID);
			try (ResultSet rs = ps.executeQuery();) {
				while (rs.next()) {
					int partNoValue = rs.getInt("部品番号");
					Integer partNo = rs.wasNull() ? null : Integer.valueOf(partNoValue);
					String key = arrivalKey(
						rs.getString("図面番号"), partNo, rs.getString("材料品名"), rs.getInt("数量")
					);
					Integer count = counts.get(key);
					counts.put(key, count == null ? 1 : count + 1);
				}
			}
		}
		return counts;
	}

	/**
	 * 今回の登録で新たに入荷となった行かを判定する
	 *
	 * 登録前から入荷済だった行を 1 件消費していき、消費しきった分を新規の入荷とみなす。
	 * 判定した行は件数から差し引くため、同じキーの行が複数あっても正しく数えられる。
	 *
	 * @param arrivedCounts 登録前に入荷済だった件数(判定に伴って減算する)
	 * @param drawingNo 図面番号(部品に紐付かない行では null)
	 * @param partNo 部品番号(同上)
	 * @param partName 品名
	 * @param quantity 数量
	 *
	 * @return 新たに入荷となった行であれば {@code true}
	 */
	private boolean isNewArrival(
		Map<String, Integer> arrivedCounts, String drawingNo, Integer partNo, String partName, int quantity
	) {
		String key = arrivalKey(drawingNo, partNo, partName, quantity);
		Integer count = arrivedCounts.get(key);
		if (count == null || count == 0) {
			return true;
		}
		arrivedCounts.put(key, count - 1);
		return false;
	}

	/**
	 * 入荷判定用のキーを組み立てる
	 *
	 * 図面番号と部品番号が揃っていればその組で部品が一意に定まるため、これをキーにする。
	 * 品名は同じものが複数の図面に現れるうえ、数量を直しただけで別の行と見なされ、
	 * 入荷済の行を編集しただけで再通知になってしまうため使わない。
	 *
	 * 部品に紐付かない材料の行は図面番号・部品番号を持たないので、
	 * 従来どおり品名と数量の組で数える。
	 *
	 * @param drawingNo 図面番号(部品に紐付かない行では null)
	 * @param partNo 部品番号(同上)
	 * @param partName 品名
	 * @param quantity 数量
	 *
	 * @return 入荷判定用のキー
	 */
	private String arrivalKey(String drawingNo, Integer partNo, String partName, int quantity) {
		// 区切りには品名・図面番号に現れない文字を使い、別の組と衝突しないようにする
		if (drawingNo != null && !drawingNo.trim().isEmpty() && partNo != null) {
			return "P" + " " + drawingNo + " " + partNo;
		}
		return "M" + " " + partName + " " + quantity;
	}

	/**
	 * 製番(製作期-製作番号 製作枝番)を組み立てる
	 *
	 * 在庫の注文期・注文番号・注文枝番は製作期・製作番号・製作枝番と対応しており、
	 * 集計処理でもこの組み合わせで結合している。
	 *
	 * 枝番は番号との間に半角スペースを空ける。枝番が無い場合はスペースも付けない。
	 * 部品入荷確認アプリ(FugaTracker)の fukaisystem.repository.Arrivals#productionNo と
	 * 同じ規則にすること。ずれると同じ入荷が入荷通知と入荷一覧で違う製番に見える。
	 *
	 * @param orderNum1 注文期
	 * @param orderNum2 注文番号
	 * @param orderNum3 注文枝番
	 *
	 * @return 製番の文字列(例: 72-2003、枝番ありなら 72-2003 A)
	 */
	private String productionNo(int orderNum1, int orderNum2, String orderNum3) {
		String branchNo = orderNum3 == null ? "" : orderNum3.trim();
		return orderNum1 + "-" + orderNum2 + (branchNo.isEmpty() ? "" : " " + branchNo);
	}

	private class DeliverySlip {
		Date date;
		int tax;

		DeliverySlip(Date date, int tax) {
			this.date = date;
			this.tax = tax;
		}

		Date getDate() {
			return date;
		}

		int getTax() {
			return tax;
		}
	}
}
