package fukaisystem.application.business;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Vector;

import javax.servlet.ServletResponse;

import org.apache.log4j.Logger;

import fukaisystem.dto.business.DispatchingDTO;
import fukaisystem.foundation.ServiceFoundation;

/**
 * 出庫データを登録する
 */
public class DispatchingRegister extends ServiceFoundation {

	protected static final Logger logger = Logger.getLogger("A1");

	@Override
	public Object transaction(Connection c, ServletResponse response, Object o) throws IOException, SQLException {

		int dispatchingID = 0;
		DispatchingDTO dto = cast(response, o, DispatchingDTO.class);
		dispatchingID = dto.dispatchingID();
		if (dto.dispatchNum1() * dto.dispatchNum2() == 0) {
			// 注文期または注文番号を0に変更したということは、消去せよということ
			if (dispatchingID != 0) {
				try (PreparedStatement ps = c.prepareStatement("DELETE FROM T_出庫_親 WHERE 出庫親ID=?");) {
					ps.setInt(1, dispatchingID);
					ps.executeUpdate();
				}
				try (PreparedStatement ps = c.prepareStatement("DELETE FROM T_出庫_子 WHERE 出庫親ID=?");) {
					ps.setInt(1, dispatchingID);
					ps.executeUpdate();
					dispatchingID = 0;
				}
			}
		} else {
			if (dispatchingID == 0) {
				try (
					PreparedStatement ps = c.prepareStatement(
						"INSERT INTO T_出庫_親"
							+ " OUTPUT inserted.出庫親ID as newId, inserted.更新日"
							+ " VALUES(?, ?, ?, ?, ?, ?, ?, ?)"
					);
				) {
					int i = 1;
					ps.setInt(i++, dto.dispatchNum1()); // 製作期
					ps.setInt(i++, dto.dispatchNum2()); // 製作番号
					ps.setString(i++, dto.dispatchNum3()); // 製作枝番
					ps.setDate(i++, dto.date()); // 出庫年月日
					ps.setString(i++, dto.use()); // 用途
					ps.setString(i++, dto.remark()); // 用途2
					ps.setTimestamp(i++, new Timestamp(new java.util.Date().getTime())); // 更新日
					ps.setInt(i, 0); // 更新者CD
					boolean isResultSet = ps.execute();
					int updateCount = 0;
					while (true) {
						if (isResultSet) {
							try (ResultSet rs = ps.getResultSet();) {
								while (rs.next()) {
									dispatchingID = rs.getInt(1);
								}
								rs.close();
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
				try (
					PreparedStatement ps = c.prepareStatement(
						"UPDATE T_出庫_親 SET 製作期=?, 製作番号=?, 製作枝番=?, 出庫年月日=?, 用途=?, 摘要=?, 更新日=?, 更新者CD=?"
							+ " WHERE 出庫親ID=?"
					);
				) {
					int i = 1;
					ps.setInt(i++, dto.dispatchNum1()); // 製作期
					ps.setInt(i++, dto.dispatchNum2()); // 製作番号
					ps.setString(i++, dto.dispatchNum3()); // 製作枝番
					ps.setDate(i++, dto.date()); // 出庫年月日
					ps.setString(i++, dto.use()); // 用途
					ps.setString(i++, dto.remark()); // 用途2
					ps.setTimestamp(i++, new Timestamp(new java.util.Date().getTime())); // 更新日
					ps.setInt(i++, 0); // 更新者CD
					ps.setInt(i, dispatchingID);
					ps.executeUpdate();
				}
				try (PreparedStatement ps = c.prepareStatement("DELETE FROM T_出庫_子 WHERE 出庫親ID=?");) {
					ps.setInt(1, dispatchingID);
					ps.executeUpdate();
				}
			}
			// UPDATE失敗したらINSERTさせない
			int k = 1;
			try (
				PreparedStatement ps = c
					.prepareStatement("INSERT INTO T_出庫_子 VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?);");
			) {
				// "UPDATE T_製作_子 SET 納品年月日=? WHERE ID=? AND 製作親ID=?");
				for (Vector<Object> record : dto.getVector()) {
					int i = 1;
					int j = 0;
					if (record.get(0) != null && (Integer) record.get(0) != 0) {
						ps.setInt(i++, dispatchingID); // 出庫親ID
						ps.setInt(i++, k); // ID
						ps.setInt(i++, (record.get(j) == null) ? 0 : (Integer) record.get(j));
						j++; // 大分類CD
						ps.setInt(i++, (record.get(j) == null) ? 0 : (Integer) record.get(j));
						j++; // 中分類CD
						ps.setInt(i++, (record.get(j) == null) ? 0 : (Integer) record.get(j));
						j++; // 小分類CD
						ps.setString(i++, (String) record.get(j++)); // 名称
						ps.setBoolean(i++, (Boolean) record.get(j++)); // 各Flg
						ps.setDouble(i++, (Double) record.get(j++)); // 数量
						ps.setInt(i++, (Integer) record.get(j++)); // 数量単位CD
						ps.setDouble(i++, (Double) record.get(j++)); // 重量長さ（単位を要検討のこと）
						ps.setInt(i++, (Integer) record.get(j++)); // 単価
						ps.setInt(i++, (Integer) record.get(j++)); // 金額
						ps.setString(i++, (String) record.get(j++)); // 備考
						ps.setInt(i++, (Integer) record.get(j++)); // 在庫親ID
						ps.setInt(i, (Integer) record.get(j++)); // 在庫子ID
						ps.addBatch();
						k++;
					}
				}

				int[] updateCounts = ps.executeBatch();
				logger.info("T_出庫_子は" + updateCounts.length + "件処理されました。");
			}
		}
		return dispatchingID;
	}

}
