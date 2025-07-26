package fukaisystem.application.business;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

import javax.servlet.ServletResponse;

import org.apache.log4j.Logger;

import fukaisystem.dto.ProjectSummaryDTO;
import fukaisystem.foundation.ServiceFoundation;

/**
 * 見積もりデータを登録する
 */
public class QuotationRegister extends ServiceFoundation {

	protected static final Logger logger = Logger.getLogger("A1");

	@Override
	public Object transaction(Connection c, ServletResponse response, Object o) throws IOException, SQLException {

		List<Integer> keys = new ArrayList<Integer>();
		int quotationID = 0;
		int productionID = 0;
		int quotationNum1 = 0;
		int quotationNum2 = 0;
		
		ProjectSummaryDTO summaryDTO = cast(response, o, ProjectSummaryDTO.class);
		quotationNum1 = summaryDTO.quotationNum1();
		quotationNum2 = summaryDTO.quotationNum2();
		quotationID = summaryDTO.quotationID();
		productionID = summaryDTO.productionID();

		if (quotationNum1 == 0 && quotationNum2 == 0 && quotationID != 0) {
			// 見積IDがあるデータの見積期と見積番号を0に変更したということは、消去せよということ
			try (
				PreparedStatement ps = c.prepareStatement(
					"DELETE FROM T_見積_親 WHERE 見積親ID=?;"
						+ "DELETE FROM T_見積_子 WHERE 見積親ID=?;"
						+ "DELETE FROM T_見積_材料 WHERE 見積親ID=?;"
						+ "DELETE FROM T_見積_加工 WHERE 見積親ID=?;"
						+ "UPDATE T_製作_親 SET 見積親ID=0 WHERE 見積親ID=?;"
						+ "DELETE FROM T_見積製作 WHERE 見積親ID=?"
				);
			) {
				ps.setInt(1, quotationID);
				ps.setInt(2, quotationID);
				ps.setInt(3, quotationID);
				ps.setInt(4, quotationID);
				ps.setInt(5, quotationID);
				ps.setInt(6, quotationID);
				ps.executeUpdate();
				quotationID = 0;
			}
			return quotationID;
		}

		// 納期
		int due = 0;
		int place = 0;
		int terms = 0;
		int validity = 0;
		try (
			PreparedStatement ps = c.prepareStatement(
				"MERGE INTO M_有効期間 AS v"
					+ " USING (SELECT ? AS 有効期間) AS w"
					+ " ON replace(replace(v.有効期間,' ',''),'　','')=replace(replace(w.有効期間,' ',''),'　','')"
					+ " WHEN MATCHED THEN"
					+ "	UPDATE SET v.有効期間=v.有効期間"
					+ " WHEN NOT MATCHED THEN"
					+ "	INSERT VALUES(w.有効期間)"
					+ " OUTPUT deleted.CD as oldId, inserted.CD as newId;"
			);
		) {
			ps.setString(1, summaryDTO.validity());
			boolean isResultSet = ps.execute();
			int updateCount = 0;
			while (true) {
				if (isResultSet) {
					try (ResultSet rs = ps.getResultSet();) {
						while (rs.next()) {
							validity = rs.getInt(2);
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
		try (
			PreparedStatement ps = c.prepareStatement(
				"MERGE INTO M_納期 AS v"
					+ " USING (SELECT ? AS 納期) AS w"
					+ " ON replace(replace(v.納期,' ',''),'　','')=replace(replace(w.納期,' ',''),'　','')"
					+ " WHEN MATCHED THEN"
					+ "	UPDATE SET v.納期=v.納期"
					+ " WHEN NOT MATCHED THEN"
					+ "	INSERT VALUES(w.納期)"
					+ " OUTPUT deleted.CD as oldId, inserted.CD as newId;"
			);
		) {
			ps.setString(1, summaryDTO.due());
			boolean isResultSet = ps.execute();
			int updateCount = 0;
			while (true) {
				if (isResultSet) {
					try (ResultSet rs = ps.getResultSet();) {
						while (rs.next()) {
							due = rs.getInt(2);
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
		try (
			PreparedStatement ps = c.prepareStatement(
				"MERGE INTO M_受渡場所 AS v"
					+ " USING (SELECT ? AS 受渡場所) AS w"
					+ " ON replace(replace(v.受渡場所,' ',''),'　','')=replace(replace(w.受渡場所,' ',''),'　','')"
					+ " WHEN MATCHED THEN"
					+ "	UPDATE SET v.受渡場所=v.受渡場所"
					+ " WHEN NOT MATCHED THEN"
					+ "	INSERT VALUES(w.受渡場所)"
					+ " OUTPUT deleted.CD as oldId, inserted.CD as newId;"
			);
		) {
			ps.setString(1, summaryDTO.place());
			boolean isResultSet = ps.execute();
			int updateCount = 0;
			while (true) {
				if (isResultSet) {
					try (ResultSet rs = ps.getResultSet();) {
						while (rs.next()) {
							place = rs.getInt(2);
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
		try (
			PreparedStatement ps = c.prepareStatement(
				"MERGE INTO M_取引条件 AS v"
					+ " USING (SELECT ? AS 取引条件) AS w"
					+ " ON replace(replace(v.取引条件,' ',''),'　','')=replace(replace(w.取引条件,' ',''),'　','')"
					+ " WHEN MATCHED THEN"
					+ "	UPDATE SET v.取引条件=v.取引条件"
					+ " WHEN NOT MATCHED THEN"
					+ "	INSERT VALUES(w.取引条件)"
					+ " OUTPUT deleted.CD as oldId, inserted.CD as newId;"
			);
		) {
			ps.setString(1, summaryDTO.terms());
			boolean isResultSet = ps.execute();
			int updateCount = 0;
			while (true) {
				if (isResultSet) {
					try (ResultSet rs = ps.getResultSet();) {
						while (rs.next()) {
							terms = rs.getInt(2);
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
		if (quotationNum2 == 0) {
			try (PreparedStatement ps = c.prepareStatement("SELECT MAX(見積番号) AS 最終見積番号 FROM T_見積_親 WHERE 見積期=?");) {
				ps.setInt(1, quotationNum1); // 見積期
				try (ResultSet rs = ps.executeQuery();) {
					if (rs.next()) {
						quotationNum2 = rs.getInt("最終見積番号") + 1;
						quotationID = 0; // 既存の見積もりを流用した新規登録
					}
				}
			}
		} else {
			try (
				PreparedStatement ps = c.prepareStatement(
					"SELECT 見積親ID FROM T_見積_親 WHERE 見積期=? AND 見積番号=? AND 見積枝番=?"
				);
			) {
				int i = 1;
				ps.setInt(i++, quotationNum1); // 見積期
				ps.setInt(i++, quotationNum2); // 見積番号
				ps.setString(i++, summaryDTO.quotationNum3()); // 見積枝番
				try (ResultSet rs = ps.executeQuery();) {
					if (rs.next()) {
						if (rs.getInt("見積親ID") != 0) {
							quotationID = rs.getInt("見積親ID");
						}
					}
				}
			}
		}
		if (quotationID == 0) { // 見積書新規作成
			try (
				PreparedStatement ps = c.prepareStatement(
					"INSERT INTO T_見積_親"
						+ " (見積期, 見積番号, 見積枝番, 元製作親ID, 案件名, 案内文, 得意先CD, 得意先表示名,"
						+ " 納期CD, 受渡場所CD, 取引条件CD, 有効期間CD, 提出済CD, 見積年月日, 提出年月日,"
						+ " 通貨CD, 見積金額, 摘要, 更新日, 更新者CD)"
						+ " OUTPUT inserted.見積親ID as newId, inserted.更新日"
						+ " SELECT"
						+ " ?, ?, ?,"
						+ " CASE WHEN (?=0 AND ?=0 AND ?='' AND ?=0)"
						+ "  THEN 0" // 種類、誕生製番がすべて空なら自身が新機となるため、親なしとして登録
						+ "  ELSE (SELECT CASE WHEN MIN(製作親ID) IS NULL THEN 0 ELSE MIN(製作親ID) END FROM T_製作_親"
						+ " WHERE (得意先CD=? AND 機械番号=?) OR (製作期=? AND 製作番号=? AND 製作枝番=?)) END,"
						+ " ?, ?, ?, ?, ?, ?, ?, ?, ?, ?,"
						+ " ?, ?, ?, ?, ?, ?"
				);
			) {
				int i = 1;
				ps.setInt(i++, quotationNum1); // 見積期
				ps.setInt(i++, quotationNum2); // 見積番号
				ps.setString(i++, summaryDTO.quotationNum3()); // 見積枝番
				// 元製作親IDサブクエリ---------------------------------
				ps.setInt(i++, summaryDTO.birthNum1()); // 誕生期
				ps.setInt(i++, summaryDTO.birthNum2()); // 誕生番号
				ps.setString(i++, summaryDTO.birthNum3()); // 誕生枝番
				ps.setInt(i++, summaryDTO.projectCode()); // 納入機・・・これらが入力されていなければ0、入力されていればそれ（入力に一致するデータがなければ0)
				ps.setInt(i++, summaryDTO.buyerCode()); // 購入者CD
				ps.setInt(
					i++,
					summaryDTO.projectCode() == 0
						? -1
						: summaryDTO.projectCode()
				); // 納入機・・・これが0だとヒットしてしまうので、-1にする
				ps.setInt(i++, summaryDTO.birthNum1()); // 誕生期
				ps.setInt(i++, summaryDTO.birthNum2()); // 誕生番号
				ps.setString(i++, summaryDTO.birthNum3()); // 誕生枝番
				// -----------------------------------------------------
				ps.setString(i++, summaryDTO.quotationProjectName()); // 案件名
				ps.setString(i++, summaryDTO.announcement()); // 案内文
				ps.setInt(i++, summaryDTO.quotationAccountID()); // 得意先CD
				ps.setString(i++, summaryDTO.quotationAccountName()); // 得意先表示名
				// ps.setInt(i++, summaryDTO.contactCode()); //個人CD
				// ps.setInt(i++, summaryDTO.inquiryCode()); //依頼手段CD
				ps.setInt(i++, due); // 納期CD
				ps.setInt(i++, place); // 受渡場所CD
				ps.setInt(i++, terms); // 取引条件CD
				ps.setInt(i++, validity); // 有効期間CD
				ps.setInt(i++, summaryDTO.submitCD()); // 提出済CD
				// ps.setDate(i++, summaryDTO.inquiryDate()); //依頼年月日
				ps.setDate(i++, summaryDTO.quotationDate()); // 見積年月日
				ps.setDate(i++, summaryDTO.submitDate()); // 提出年月日
				ps.setInt(i++, summaryDTO.quotationCurrencyCD()); // 通貨CD
				ps.setInt(i++, summaryDTO.quotationAmount()); // 見積金額
				ps.setString(i++, summaryDTO.quotationNote()); // 摘要
				ps.setTimestamp(i++, new Timestamp(new java.util.Date().getTime())); // 更新日
				ps.setInt(i++, 0); // 更新者CD
				boolean isResultSet = ps.execute();
				int updateCount = 0;
				while (true) {
					if (isResultSet) {
						try (ResultSet rs = ps.getResultSet();) {
							while (rs.next()) {
								quotationID = rs.getInt(1);
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
			// 製作伝票が作成されていたら、T_製作_親テーブルの見積親IDを更新する（旧仕様）
			if (productionID != 0) {
				try (PreparedStatement ps = c.prepareStatement("UPDATE T_製作_親 SET 見積親ID=? WHERE 製作親ID=?");) {
					ps.setInt(1, quotationID); // 見積親ID
					ps.setInt(2, productionID);
					ps.executeUpdate();
				}
				// 新仕様
				try (PreparedStatement ps = c.prepareStatement("INSERT INTO T_見積製作 VALUES(?,?)");) {
					ps.setInt(1, quotationID);
					ps.setInt(2, productionID);
					ps.executeUpdate();
				}
			}

		} else { // 既存見積書更新
			try (
				PreparedStatement ps = c.prepareStatement(
					"UPDATE T_見積_親 SET"
						+ " 見積期=?, 見積番号=?, 見積枝番=?,"
						+ " 元製作親ID="
						+ " CASE WHEN (?=0 AND ?=0 AND ?='' AND ?=0)"
						+ "  THEN 0" // 種類、誕生製番がすべて空なら自身が新機となるため、親なしとして登録
						+ "  ELSE (SELECT CASE WHEN MIN(製作親ID) IS NULL THEN 0 ELSE MIN(製作親ID) END FROM T_製作_親"
						+ " WHERE (得意先CD=? AND 機械番号=?) OR (製作期=? AND 製作番号=? AND 製作枝番=?)) END,"
						+ " 案件名=?, 案内文=?, 得意先CD=?, 得意先表示名=?,"
						+ " 納期CD=?, 受渡場所CD=?, 取引条件CD=?, 有効期間CD=?, 提出済CD=?, 見積年月日=?, 提出年月日=?,"
						+ " 通貨CD=?, 見積金額=?, 摘要=?, 更新日=?, 更新者CD=? WHERE 見積親ID=?"
				);
			) {
				int i = 1;
				ps.setInt(i++, quotationNum1); // 見積期
				ps.setInt(i++, quotationNum2); // 見積番号
				ps.setString(i++, summaryDTO.quotationNum3()); // 見積枝番
				// 元製作親IDサブクエリ---------------------------------
				ps.setInt(i++, summaryDTO.birthNum1()); // 誕生期
				ps.setInt(i++, summaryDTO.birthNum2()); // 誕生番号
				ps.setString(i++, summaryDTO.birthNum3()); // 誕生枝番
				ps.setInt(i++, summaryDTO.projectCode()); // 納入機・・・これらが入力されていなければ0、入力されていればそれ（入力に一致するデータがなければ0)
				ps.setInt(i++, summaryDTO.buyerCode()); // 購入者CD
				ps.setInt(
					i++,
					summaryDTO.projectCode() == 0
						? -1
						: summaryDTO.projectCode()
				); // 納入機・・・これが0だとヒットしてしまうので、-1にする
				ps.setInt(i++, summaryDTO.birthNum1()); // 誕生期
				ps.setInt(i++, summaryDTO.birthNum2()); // 誕生番号
				ps.setString(i++, summaryDTO.birthNum3()); // 誕生枝番
				// -----------------------------------------------------
				ps.setString(i++, summaryDTO.quotationProjectName()); // 案件名
				ps.setString(i++, summaryDTO.announcement()); // 案内文
				ps.setInt(i++, summaryDTO.quotationAccountID()); // 得意先CD
				ps.setString(i++, summaryDTO.quotationAccountName()); // 得意先表示名
				// ps.setInt(i++, summaryDTO.contactCode()); //個人CD
				// ps.setInt(i++, summaryDTO.inquiryCode()); //依頼手段CD
				ps.setInt(i++, due); // 納期CD
				ps.setInt(i++, place); // 受渡場所CD
				ps.setInt(i++, terms); // 取引条件CD
				ps.setInt(i++, validity); // 有効期間CD
				ps.setInt(i++, summaryDTO.submitCD()); // 提出済CD
				// ps.setDate(i++, summaryDTO.inquiryDate()); //依頼年月日
				ps.setDate(i++, summaryDTO.quotationDate()); // 見積年月日
				ps.setDate(i++, summaryDTO.submitDate()); // 提出年月日
				ps.setInt(i++, summaryDTO.quotationCurrencyCD()); // 通貨CD
				ps.setInt(i++, summaryDTO.quotationAmount()); // 見積金額
				ps.setString(i++, summaryDTO.quotationNote()); // 摘要
				ps.setTimestamp(i++, new Timestamp(new java.util.Date().getTime())); // 更新日
				ps.setInt(i++, 0); // 更新者CD
				ps.setInt(i, quotationID); // ID
				ps.executeUpdate();
			}
			// 子孫のデータ更新は、削除→追加にて
			try (PreparedStatement ps = c.prepareStatement("DELETE FROM T_見積_子 WHERE 見積親ID=?");) {
				ps.setInt(1, quotationID);
				ps.executeUpdate();
			}
			try (PreparedStatement ps = c.prepareStatement("DELETE FROM T_見積_材料 WHERE 見積親ID=?");) {
				ps.setInt(1, quotationID);
				ps.executeUpdate();
			}
			try (PreparedStatement ps = c.prepareStatement("DELETE FROM T_見積_加工 WHERE 見積親ID=?");) {
				ps.setInt(1, quotationID);
				ps.executeUpdate();
			}
		}
		// mainTable
		int k = 1;
		try (
			PreparedStatement ps = c.prepareStatement(
				"INSERT INTO T_見積_子 VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)"
			);
		) {
			for (Vector<Object> record : summaryDTO.quotationVector()) {
				int tag = (Integer) record.get(1);
				if (tag != 0) {
					keys.add((Integer) record.get(0));
					int i = 1;
					int j = 2;
					ps.setInt(i++, quotationID); // 見積親ID
					ps.setInt(i++, k); // ID
					ps.setInt(i++, tag); // 表示CD
					ps.setString(i++, (String) record.get(j++)); // 名称
					ps.setBoolean(i++, (Boolean) record.get(j++)); // 各FLG
					ps.setInt(i++, (Integer) record.get(j++)); // 数量
					ps.setInt(i++, (Integer) record.get(j++)); // 数量単位CD
					ps.setInt(i++, (Integer) record.get(j++)); // 単価
					ps.setInt(i++, (Integer) record.get(j++)); // 提示額
					ps.setString(i++, (String) record.get(j++)); // 図番
					ps.setString(i++, (String) record.get(j)); // 備考
					ps.addBatch();
					k++;
				}
			}
			int[] updateCounts = ps.executeBatch();
			logger.info("T_見積_子は" + updateCounts.length + "件処理されました。");
		}

		// subTable
		int coarseCD = 1;
		try (
			PreparedStatement ps1 = c.prepareStatement(
				"INSERT INTO T_見積_加工 VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)"
			);
		) {
			try (
				PreparedStatement ps2 = c.prepareStatement(
					"INSERT INTO T_見積_材料 VALUES(" +
						" ?, ?, ?, ?, ?, ?, ?, ?, ?, ?," +
						" ?, ?, ?, ?, ?, ?)"
				);
			) {
				for (int key : keys) {

					if (summaryDTO.getMap().containsKey(key)) {
						int middleCD = 1;
						for (Vector<Object> record : summaryDTO.getMap().get(key)) {

							if ((Integer) record.get(1) != 0 || !((String) record.get(4)).equals("")) {
								if (((Integer) record.get(1)).intValue() > 100) {
									// 加工等
									int i = 1;

									ps1.setInt(i++, quotationID); // 親ID
									ps1.setInt(i++, coarseCD); // 子ID
									ps1.setInt(i++, middleCD); // ID
									ps1.setInt(i++, (Integer) record.get(1)); // 大分類
									ps1.setInt(i++, (record.get(2) == null) ? 0 : (Integer) record.get(2)); // 加工CD
									ps1.setInt(i++, (record.get(3) == null) ? 0 : (Integer) record.get(3)); // 加工CD
									ps1.setString(i++, (String) record.get(4)); // 名称
									ps1.setInt(i++, (Integer) record.get(5)); // 単価
									ps1.setDouble(i++, (Double) record.get(6)); // 数量
									ps1.setDouble(i++, (Double) record.get(8)); // 掛率
									ps1.setString(i, (String) record.get(15)); // 備考
									ps1.addBatch();

								} else {
									// 材料
									int i = 1;
									ps2.setInt(i++, quotationID); // 親ID
									ps2.setInt(i++, coarseCD); // 子ID
									ps2.setInt(i++, middleCD); // 孫ID
									ps2.setInt(i++, (record.get(1) == null) ? 0 : (Integer) record.get(1)); // 大分類
									ps2.setInt(i++, (record.get(2) == null) ? 0 : (Integer) record.get(2)); // 中分類
									ps2.setInt(i++, (record.get(3) == null) ? 0 : (Integer) record.get(3)); // 小分類
									ps2.setString(i++, (String) record.get(4)); // 名称
									ps2.setInt(i++, (Integer) record.get(5)); // 単価
									ps2.setDouble(i++, (Double) record.get(6)); // 数量
									ps2.setDouble(i++, (Double) record.get(8)); // 掛率
									ps2.setInt(i++, (Integer) record.get(10)); // 品番
									ps2.setDouble(i++, (Double) record.get(11)); // 重量
									ps2.setInt(i++, (Integer) record.get(12)); // 仕入先CD
									// ps2.setInt(i++, 0); //仕入先CD
									ps2.setBoolean(i++, (Boolean) record.get(13)); // 仕入見積FLG
									ps2.setString(i++, (String) record.get(14)); // 仕入納期
									ps2.setString(i, (String) record.get(15)); // 備考
									ps2.addBatch();
								}
								middleCD++;
							}
						}
					}
					coarseCD++;
				}
				int[] updateCounts1 = ps1.executeBatch();
				int[] updateCounts2 = ps2.executeBatch();

				logger.info("T_見積_加工は" + updateCounts1.length + "件処理されました。");
				logger.info("T_見積_材料は" + updateCounts2.length + "件処理されました。");
			}
		}
		return quotationID;
	}
}
