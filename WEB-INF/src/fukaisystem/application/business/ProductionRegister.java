
package fukaisystem.application.business;

import java.io.IOException;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Vector;

import javax.servlet.ServletResponse;

import org.apache.log4j.Logger;

import fukaisystem.dto.ProjectSummaryDTO;
import fukaisystem.foundation.ServiceFoundation;

/**
 * 製作伝票データを登録する
 */
public class ProductionRegister extends ServiceFoundation {

	protected static final Logger logger = Logger.getLogger("A1");

	@Override
	public Object transaction(Connection c, ServletResponse response, Object o) throws IOException, SQLException {

		int quotationID = 0;
		int productionID = 0;
		int productNum2 = 0;
		
		ProjectSummaryDTO summaryDTO = cast(response, o, ProjectSummaryDTO.class);
		quotationID = summaryDTO.quotationID();
		productionID = summaryDTO.productionID();

		productNum2 = summaryDTO.productionNum2();
		if (productNum2 == 0 && productionID != 0) {
			// 製作期または製作番号が0なら
			try (
				PreparedStatement ps = c.prepareStatement(
					"DELETE FROM T_売上_親"
						+ " WHERE 売上親ID IN (SELECT MIN(売上親ID) FROM T_売上_子 WHERE 製作親ID=? group by 売上親ID);"
						+ "DELETE FROM T_製作_親 WHERE 製作親ID=?;"
						+ "DELETE FROM T_製作_子 WHERE 製作親ID=?;"
						+ "DELETE FROM T_売上_子 WHERE 製作親ID=?;"
						+ "DELETE FROM T_見積製作 WHERE 製作親ID=?"
				);
			) { // 売上親はリレーションで消える
				ps.setInt(1, productionID);
				ps.setInt(2, productionID);
				ps.setInt(3, productionID);
				ps.setInt(4, productionID);
				ps.setInt(5, productionID);
				ps.executeUpdate();
				productionID = 0;
			}
		} else {
			/*
			}
				try {
					ps = c.prepareStatement("SELECT 製作親ID FROM T_製作_親 WHERE 製作期=? AND 製作番号=? AND 製作枝番=?");
					int i = 1;
					ps.setInt(i, summaryDTO.getInt(10)); i++;//製作期
					ps.setInt(i, summaryDTO.getInt(11)); i++;//製作番号
					ps.setString(i, summaryDTO.getStr(11)); i++;//製作枝番
					rs = ps.executeQuery();
					if(rs.next()) {
						if(rs.getInt("製作親ID") != 0) {
							productionID = rs.getInt("製作親ID");
						}
					}
				} catch(SQLException ex) {
					ex.printStackTrace();
					isError = true;
					err.append(className + "製作親IDの読み込みに失敗しました\n");
					Logging.logStackTrace(ex, logger, className);
				}
				*/
			if (productionID == 0) { // 製作伝票新規作成
				try (
					PreparedStatement ps = c.prepareStatement(
						"INSERT INTO T_製作_親"
							+ " (製作親ID, 製作期, 製作番号, 製作枝番, 受注番号, 案件名, 見積親ID, 得意先CD, 機械番号, 納入先名, 納期,"
							+ "受注年月日, 発行年月日, 出荷年月日, 検収年月日, 通貨CD, 契約金額, 摘要, 出図FLG, 手配FLG, 更新日, 更新者CD)"
							+ " OUTPUT inserted.製作親ID as newId, inserted.更新日"
							+ " SELECT CASE WHEN MAX(製作親ID) IS NULL THEN 1 ELSE MAX(製作親ID)+1 END,"
							+ "?, ?, ?, ?, ?, ?, ?,"
							+ "CASE WHEN ?=1 THEN" // 新機なら、
							+ " (SELECT CASE WHEN MAX(機械番号) IS NULL THEN 1 ELSE MAX(機械番号)+1 END FROM T_製作_親 WHERE 得意先CD=?)" // 機種番号を登録
							+ " ELSE 0 END," // それ以外の場合はすべて0
							+ "?, ?, ?, ?, ?, ?, ?, ?, ?, ?,"
							+ "?, ?, ? FROM T_製作_親"
					);
				) {
					int i = 1;
					ps.setInt(i++, summaryDTO.productionNum1()); // 製作期
					ps.setInt(i++, summaryDTO.productionNum2()); // 製作番号
					ps.setString(i++, summaryDTO.productionNum3()); // 製作枝番
					ps.setString(i++, summaryDTO.acceptID()); // 受注番号
					ps.setString(i++, summaryDTO.productionProjectName()); // 案件名
					ps.setInt(i++, quotationID); // 見積親ID
					ps.setInt(i++, summaryDTO.quotationAccountID()); // 得意先CD
					// 機械番号サブクエリ-----------------------------------
					ps.setBoolean(i++, summaryDTO.isNew()); // 新機FLG
					ps.setInt(i++, summaryDTO.quotationAccountID()); // 得意先CD
					// -----------------------------------------------------
					ps.setString(i++, summaryDTO.placeName()); // 納入先名
					ps.setDate(i++, summaryDTO.deadlineDate()); // 納期
					ps.setDate(i++, summaryDTO.acceptDate()); // 受注年月日
					ps.setDate(i++, summaryDTO.productionSlipPublishDate()); // 発行年月日
					ps.setDate(i++, summaryDTO.shippingDate()); // 出荷年月日
					ps.setDate(i++, summaryDTO.inspectionDate()); // 検収年月日
					ps.setInt(i++, summaryDTO.productionCurrencyCD()); // 通貨CD
					ps.setInt(i++, summaryDTO.productionAmount()); // 契約金額
					ps.setString(i++, summaryDTO.productionNote()); // 摘要
					ps.setBoolean(i++, summaryDTO.isRelease()); // 出図FLG
					ps.setBoolean(i++, summaryDTO.isOrder()); // 手配FLG
					ps.setTimestamp(i++, new Timestamp(new java.util.Date().getTime())); // 更新日
					ps.setInt(i++, 0); // 更新者CD
					boolean isResultSet = ps.execute();
					int updateCount = 0;
					while (true) {
						if (isResultSet) {
							try (ResultSet rs = ps.getResultSet();) {
								while (rs.next()) {
									productionID = rs.getInt(1);
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

			} else { // 既存製作伝票の更新
				try (
					PreparedStatement ps = c.prepareStatement(
						"UPDATE T_製作_親 SET 製作期=?, 製作番号=?, 製作枝番=?,"
							+ "受注番号=?, 案件名=?, 見積親ID=?, 得意先CD=?," // これは受注者、カルテ履歴の得意先（購入者）と異なる可能性あり
							+ "機械番号="
							+ "CASE WHEN ?=1 THEN" // 新機なら、
							+ " (SELECT CASE WHEN MAX(機械番号) IS NULL THEN 1 ELSE MAX(機械番号)+1 END FROM T_製作_親 WHERE 得意先CD=?)" // 純粋な新機だから機種番号をセット
							+ " ELSE 0 END," // それ以外の場合はすべて0
							+ "納入先名=?, 納期=?, 受注年月日=?, 発行年月日=?, 出荷年月日=?, 検収年月日=?, 通貨CD=?, 契約金額=?, 摘要=?, 出図FLG=?, 手配FLG=?, 更新日=?, 更新者CD=? WHERE 製作親ID=?"
					);
				) {
					int i = 1;
					ps.setInt(i++, summaryDTO.productionNum1()); // 製作期
					ps.setInt(i++, summaryDTO.productionNum2()); // 製作番号
					ps.setString(i++, summaryDTO.productionNum3()); // 製作枝番
					ps.setString(i++, summaryDTO.acceptID()); // 受注番号
					ps.setString(i++, summaryDTO.productionProjectName()); // 案件名
					ps.setInt(i++, quotationID); // 見積親ID
					ps.setInt(i++, summaryDTO.productionAccountID()); // 得意先CD
					// 機械番号サブクエリ-----------------------------------
					ps.setBoolean(i++, summaryDTO.isNew()); // 新機FLG
					ps.setInt(i++, summaryDTO.productionAccountID()); // 得意先CD
					// -----------------------------------------------------
					ps.setString(i++, summaryDTO.placeName()); // 納入先名
					ps.setDate(i++, summaryDTO.deadlineDate()); // 納期
					ps.setDate(i++, summaryDTO.acceptDate()); // 受注年月日
					ps.setDate(i++, summaryDTO.productionSlipPublishDate()); // 発行年月日
					ps.setDate(i++, summaryDTO.shippingDate()); // 出荷年月日
					ps.setDate(i++, summaryDTO.inspectionDate()); // 検収年月日
					ps.setInt(i++, summaryDTO.productionCurrencyCD()); // 通貨CD
					ps.setInt(i++, summaryDTO.productionAmount()); // 契約金額
					ps.setString(i++, summaryDTO.productionNote()); // 摘要
					ps.setBoolean(i++, summaryDTO.isRelease()); // 出図FLG
					ps.setBoolean(i++, summaryDTO.isOrder()); // 手配FLG
					ps.setTimestamp(i++, new Timestamp(new java.util.Date().getTime())); // 更新日
					ps.setInt(i++, 0); // 更新者CD
					ps.setInt(i, productionID); // 製作親ID
					ps.executeUpdate();
				}
				try (PreparedStatement ps = c.prepareStatement("DELETE FROM T_製作_子 WHERE 製作親ID=?");) {
					ps.setInt(1, productionID);
					ps.executeUpdate();
				}
			}

			// if(summaryDTO.getBool(3)) { //カルテ追加
			try (PreparedStatement ps = c.prepareStatement("DELETE FROM T_カルテ履歴 WHERE 製作親ID=?");) {
				ps.setInt(1, productionID);
				ps.executeUpdate();
			}
			try (
				PreparedStatement ps = c.prepareStatement(
					"INSERT INTO T_カルテ履歴 (製作親ID,元製作親ID) " // 部品の製作ID,機械の製作ID
						+ " SELECT ?, CASE WHEN ?=1 THEN" // 新しい機種で
						+ " CASE WHEN MAX(製作親ID) IS NULL THEN ? ELSE MAX(製作親ID) END" // 製作履歴がなければその製作親ID、あれば直近の製作親ID
						+ " ELSE" // 修理部品等で
						+ " CASE WHEN MAX(製作親ID) IS NULL THEN 0 ELSE MAX(製作親ID) END" // 製作履歴がなければ0、あれば直近の製作親ID
						+ " END FROM T_製作_親 WHERE 得意先CD=? AND 機械番号=? AND 機械番号<>0"
				);
			) {
				for (Integer parentCode : summaryDTO.getParents()) {
					int i = 1;
					ps.setInt(i++, productionID); // 製作親ID
					ps.setBoolean(i++, summaryDTO.isNew()); // 新機FLG
					ps.setInt(i++, productionID); // 製作親ID
					ps.setInt(i++, summaryDTO.buyerCode()); // 購入者CD
					ps.setInt(i++, parentCode); // 納入機
					ps.addBatch();
				}
				ps.executeBatch();
			}
			if (productionID != 0) {
				int k = 1;
				try (
					PreparedStatement ps = c.prepareStatement(
						"INSERT INTO T_製作_子 VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)"
					);
				) {
					for (Vector<Object> record : summaryDTO.productionVector()) {
						int tag = (Integer) record.get(1);
						if (tag != 0) {
							int i = 1;
							int j = 2;
							ps.setInt(i++, k); // ID
							ps.setInt(i++, productionID); // 製作親ID
							ps.setInt(i++, tag); // 表示CD
							ps.setString(i++, (String) record.get(j++));
							// 名称 ps.setBoolean(i++, (Boolean) record.get(j++)); // 各FLG
							ps.setInt(i++, (Integer) record.get(j++)); // 数量
							ps.setInt(i++, (Integer) record.get(j++)); // 数量単位CD
							ps.setInt(i++, (Integer) record.get(j++)); // 単価
							ps.setInt(i++, (Integer) record.get(j++)); // 金額
							ps.setString(i++, (String) record.get(j++)); // 図番
							ps.setString(i++, (String) record.get(j++)); // 記事
							ps.setDate(
								i++,
								record.get(j) == null || record.get(j).equals("")
									? null
									: new Date(((java.util.Date) record.get(j)).getTime())
							);
							j += 2;
							ps.setDate(
								i,
								(record.get(j) == null || record.get(j).equals(""))
									? null
									: new Date(((java.util.Date) record.get(j)).getTime())
							);
							ps.addBatch();
							k++;
						}
					}
					int[] updateCounts = ps.executeBatch();
					logger.info("T_製作_子は" + updateCounts.length + "件処理されました。");
				}

				try (PreparedStatement ps = c.prepareStatement("DELETE FROM T_見積製作 WHERE 製作親ID=?");) {
					ps.setInt(1, productionID);
					ps.executeUpdate();
				}
				try (
					PreparedStatement ps = c.prepareStatement(
						"INSERT INTO T_見積製作"
							+ " select 見積親ID,? from ("
							+ " select 見積親ID,convert(varchar,見積期)+'-'+right('000' + convert(varchar, 見積番号), 3)+見積枝番 as 見積番 from T_見積_親) a"
							+ " where 見積番 like ?"
					);
				) {
					for (String s : summaryDTO.getQuotationNumbers()) {
						ps.setInt(1, productionID);
						ps.setString(2, s);
						ps.addBatch();
					}
					int[] updateCounts = ps.executeBatch();
					logger.info("T_見積製作は" + updateCounts.length + "件処理されました。");
				}
			}
		}
		c.commit();
		return productionID;
	}
}
