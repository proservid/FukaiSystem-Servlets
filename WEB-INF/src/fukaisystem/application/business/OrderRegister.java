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

import fukaisystem.dto.OrderDocumentDTO;
import fukaisystem.foundation.ServiceFoundation;

/**
 * 注文データを登録する
 */
public class OrderRegister extends ServiceFoundation {

	protected static final Logger logger = Logger.getLogger("A1");

	@Override
	public Object transaction(Connection c, ServletResponse response, Object o) throws IOException, SQLException {

		OrderDocumentDTO odd = cast(response, o, OrderDocumentDTO.class);
		int orderID = -1;
		Map<Integer, DeliverySlip> taxMap = null;

		boolean isParentEditable = true; // 納品書番号が入ったデータが１つでもあるか
		taxMap = new HashMap<Integer, DeliverySlip>(); // 指定納品書のデータ
		List<Vector<Object>> regVector = new ArrayList<Vector<Object>>();
		for (Vector<Object> record : odd.getDataVector()) {
			boolean closeFlg = false;
			boolean matchFlg = false;
			if (record.get(14) == null || record.get(15) == null || !(Boolean) record.get(17)) { // 納品書番号か納品日がnullまたは納品書チェックなし
				// 納品書入力をクリアし、登録用データに追加
				record.set(14, 0);
				record.set(15, null);
				record.set(16, 0);
				record.set(17, false);
				regVector.add(record);
			} else {
				// 〆後の日付の指定納品書を追加させない
				try (
					PreparedStatement ps = c.prepareStatement(
						"select * from T_指定納品書 WHERE 納品書日>=? and 納品書日<? and 〆FLG='true'"
					);
				) {
					java.util.Date d = (java.util.Date) record.get(15);
					Calendar cal = Calendar.getInstance();
					cal.setTime(d);
					cal.set(Calendar.DATE, 1); // その月の１日
					ps.setDate(1, new Date(cal.getTimeInMillis()));
					cal.add(Calendar.MONTH, 1); // 翌月１日
					ps.setDate(2, new Date(cal.getTimeInMillis()));
					try (ResultSet rs = ps.executeQuery();) {
						while (rs.next()) {
							// 一致する納品書データがあるか
							if (rs.getInt("ID") == (Integer) record.get(14)) {
								if ((Boolean) record.get(18)) {
									matchFlg = true;
									// 1行でも指定納品書が〆られていたら親データの編集を不可に
									isParentEditable = false;
									// 〆後なので変更されてはいないはずだが念のため登録データをセットしておく(IDは一致確認済)
									record.set(15, rs.getDate("納品書日"));
									record.set(16, rs.getInt("消費税"));
									record.set(17, true);
									record.set(18, true);
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
					if (!taxMap.containsKey(record.get(14)) || !(Boolean) record.get(18)) {
						// 納品書番号が同じデータ又は〆後データは省く
						taxMap.put(
							(Integer) record.get(14),
							new DeliverySlip(
								new Date(((java.util.Date) record.get(15)).getTime()),
								record.get(16) == null ? 0 : (Integer) record.get(16)
							)
						);
					}
				} else {
					// 〆後の日付で
					if (!matchFlg) {
						// 登録されていない番号 または
						// 登録はあるが〆Flgがfalse(=番号入力ミス)
						// クリアし、登録用データに追加
						record.set(14, 0);
						record.set(15, null);
						record.set(16, 0);
						record.set(17, false);
						regVector.add(record);
						addError("〆後の納品書日では登録できません");
					}
				}
			}
		}
		// if(closeFlg) addError("〆後の納品書日が入力されている行があります\n納品書データは〆後の日付で登録できません");
		//////////////////////////////////////////////////////////////////////////////////////////////////////
		orderID = odd.orderID();
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
						"SELECT CASE WHEN MAX(注文番号) IS NULL THEN 1 ELSE MAX(注文番号)+1 END AS 新注文番号 FROM T_在庫_親 WHERE 注文期=? AND 注文番号<9999"
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
							" SELECT ?, ?, ?, MAX(伝票番号)+1, ?, ?, ?, ?, ?, ?, ? FROM T_在庫_親"
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
					"INSERT INTO T_在庫_子 VALUES("
						+ "?, ?, ?, ?, ?, ?, ?, ?, ?, ?,"
						+ "?, ?, ?, ?, ?, ?)"
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
						ps.setString(i++, (String) record.get(j++)); // 名称
						ps.setBoolean(i++, (Boolean) record.get(j++)); // 各FLG
						ps.setInt(i++, (Integer) record.get(j++)); // 数量
						ps.setInt(i++, (Integer) record.get(j++)); // 数量単位CD
						ps.setDouble(i++, (Double) record.get(j++)); // 重量長さ（単位を要検討のこと）
						ps.setInt(i++, (Integer) record.get(j++)); // 単価
						ps.setInt(i++, (Integer) record.get(j++)); // 金額
						ps.setString(i++, (String) record.get(j++)); // 備考
						ps.setDate(
							i++,
							record.get(j) == null
								? null
								: new java.sql.Date(((java.util.Date) record.get(j)).getTime())
						);
						j += 2; // 入庫年月日（チェックボックスを飛ばすためj+=2）
						ps.setInt(i, record.get(j) == null ? 0 : (Integer) record.get(j)); // 納品書番号
						ps.addBatch();
						k++;
					}
				}
				int[] updateCounts = ps.executeBatch();
				logger.info("T_在庫_子は" + updateCounts.length + "件処理されました。");
			}

			try (
				PreparedStatement ps = c.prepareStatement(
					"MERGE INTO T_指定納品書 AS t"
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
		c.commit();
		return orderID;
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
