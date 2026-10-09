package fukaisystem.application.issue;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Vector;

import javax.servlet.ServletResponse;

import fukaisystem.dto.business.DispatchingDTO;
import fukaisystem.foundation.ServiceFoundation;

/**
 * QRコードから出庫データを登録する
 */
public class DispatchingRegister extends ServiceFoundation {

	@Override
	public Object transaction(Connection c, ServletResponse response, Object o) throws IOException, SQLException {

		int dispatchingID = 0;
		DispatchingDTO dto = cast(response, o, DispatchingDTO.class);
		dispatchingID = dto.dispatchingID();

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

		try (
			PreparedStatement ps = c
				.prepareStatement(
					"INSERT INTO T_出庫_子"
					+ " SELECT ?, ?, 大分類CD, 中分類CD, 小分類CD, 材料品名, 各FLG, ?, 数量単位CD, 重量長さ, 単価, 金額, 備考, 在庫親ID, ID"
					+ " FROM T_在庫_子 WHERE 在庫親ID=? AND ID=?");
		) {
			int k = 1;
			for (Vector<Object> record : dto.getVector()) {
				int i = 1;
				if (record.get(0) != null && (Integer) record.get(0) != 0) {
					ps.setInt(i++, dispatchingID); // 出庫親ID
					ps.setInt(i++, k); // ID
					ps.setInt(i++, (Integer) record.get(3)); // 数量
					ps.setInt(i++, (Integer) record.get(0)); // 在庫親ID
					ps.setInt(i, (Integer) record.get(1)); // 在庫子ID
					ps.addBatch();
					k++;
				}
			}

			int[] updateCounts = ps.executeBatch();
			logger.info("T_出庫_子は" + updateCounts.length + "件処理されました。");
		}
		return dispatchingID;
	}

}
