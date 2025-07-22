package fukaisystem.application.business;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.Vector;

import javax.servlet.ServletResponse;

import org.apache.log4j.Logger;

import fukaisystem.dto.ShippingDTO;
import fukaisystem.foundation.ServiceFoundation;

/**
 * 出荷データを登録する
 */
public class ShippingRegister extends ServiceFoundation {

	protected static final Logger logger = Logger.getLogger("A1");

	@Override
	public Object transaction(Connection c, ServletResponse response, Object o) throws IOException, SQLException {

		ShippingDTO dto = cast(response, o, ShippingDTO.class);
		int shippingID = dto.shippingID();
		int period = dto.shippingNum1();
		int number = dto.shippingNum2();

		// 期と番号を0に変更したということは、消去せよということ
		if (shippingID != 0 && period == 0 && number == 0) {
			try (PreparedStatement ps = c.prepareStatement("DELETE FROM T_出荷_親 WHERE 出荷親ID=?");) {
				ps.setInt(1, shippingID);
				ps.executeUpdate();
			}
			try (PreparedStatement ps = c.prepareStatement("DELETE FROM T_出荷_子 WHERE 出荷親ID=?");) {
				ps.setInt(1, shippingID);
				ps.executeUpdate();
				shippingID = 0;
			}
			c.commit();
			return 0;
		}
		if (period < 1) {
			throw new SQLException("期の指定が無効です");
		}

		// ロックはせず、スレッドによる出荷伝票番号の競合は UNIQUE 制約で回避
		if (number == 0) {
			try (PreparedStatement ps = c.prepareStatement("SELECT MAX(出荷伝票番号2) FROM T_出荷_親 WHERE 出荷伝票番号1=?");) {
				ps.setInt(1, period);
				try (ResultSet rs = ps.executeQuery();) {
					if (rs.next()) {
						number = rs.getInt(1) + 1; // MAX()+1 ではダメ（条件に合致するレコードがないと null）
						shippingID = 0; // 自動採番＝新規
					}
				}
			}
		} else {
			try (
				PreparedStatement ps = c.prepareStatement(
					"SELECT 1 FROM T_出荷_親 WHERE 出荷親ID<>? AND 出荷伝票番号1=? AND 出荷伝票番号2=?"
				);
			) {
				ps.setInt(1, shippingID);
				ps.setInt(2, period);
				ps.setInt(3, number);
				try (ResultSet rs = ps.executeQuery();) {
					if (rs.next()) {
						throw new SQLException("その出荷伝票番号は登録されています");
					}
				}
			}
		}
		if (shippingID == 0) {
			try (
				PreparedStatement ps = c.prepareStatement(
					"INSERT INTO T_出荷_親"
						+ " OUTPUT inserted.出荷親ID as newId, inserted.更新日"
						+ " VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)"
				);
			) {
				int i = 1;
				ps.setInt(i++, dto.accountCode()); // 得意先CD
				ps.setString(i++, dto.distinationName()); // 納入先名
				ps.setInt(i++, period); // 出荷伝票番号1
				ps.setInt(i++, number); // 出荷伝票番号2
				ps.setDate(i++, dto.publishDate()); // 発行年月日
				ps.setInt(i++, dto.shippingMonth()); // 出荷月
				ps.setInt(i++, dto.shippingDay()); // 出荷日
				ps.setInt(i++, dto.shippingWay()); // 納品手段CD
				ps.setInt(i++, dto.productionID()); // 製作親ID
				ps.setTimestamp(i++, new Timestamp(new java.util.Date().getTime())); // 更新日
				ps.setInt(i, 0); // 更新者CD
				boolean isResultSet = ps.execute();
				int updateCount = 0;
				while (true) {
					if (isResultSet) {
						try (ResultSet rs = ps.getResultSet();) {
							while (rs.next()) {
								shippingID = rs.getInt(1);
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
					"UPDATE T_出荷_親 SET 得意先CD=?, 納入先名=?, 出荷伝票番号1=?, 出荷伝票番号2=?, 発行年月日=?, 出荷月=?, 出荷日=?, 納品手段CD=?, 製作親ID=?, 更新日=?, 更新者CD=?"
						+ " WHERE 出荷親ID=?"
				);
			) {
				int i = 1;
				ps.setInt(i++, dto.accountCode()); // 得意先CD
				ps.setString(i++, dto.distinationName()); // 納入先名
				ps.setInt(i++, period); // 出荷伝票番号1
				ps.setInt(i++, number); // 出荷伝票番号2
				ps.setDate(i++, dto.publishDate()); // 発行年月日
				ps.setInt(i++, dto.shippingMonth()); // 出荷月
				ps.setInt(i++, dto.shippingDay()); // 出荷日
				ps.setInt(i++, dto.shippingWay()); // 納品手段CD
				ps.setInt(i++, dto.productionID()); // 製作親ID
				ps.setTimestamp(i++, new Timestamp(new java.util.Date().getTime())); // 更新日
				ps.setInt(i++, 0); // 更新者CD
				ps.setInt(i, shippingID);
				ps.executeUpdate();
			}
			try (PreparedStatement ps = c.prepareStatement("DELETE FROM T_出荷_子 WHERE 出荷親ID=?");) {
				ps.setInt(1, shippingID);
				ps.executeUpdate();
			}
		}
		// UPDATE失敗したらINSERTさせない
		int k = 1;
		try (
			PreparedStatement ps = c
				.prepareStatement("INSERT INTO T_出荷_子 VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?);");
		) {
			// "UPDATE T_製作_子 SET 納品年月日=? WHERE ID=? AND 製作親ID=?");
			for (Vector<Object> record : dto.getVector()) {
				int rowType = (Integer) record.get(0);
				if (rowType < 1) {
					// 内容（見出し, 項目,,,）が指定されていない行は対象外
					continue;
				}
				int i = 1;
				int j = 1;
				ps.setInt(i++, shippingID); // 出荷親ID
				ps.setInt(i++, k); // ID
				ps.setInt(i++, (Integer) rowType); // 表示CD
				ps.setDate(
					i++,
					record.get(j) == null
						? null
						: new java.sql.Date(((java.util.Date) record.get(j)).getTime())
				); // 注文年月日
				j++;
				
				ps.setString(i++, (String) record.get(j++)); // 注番
				ps.setString(i++, (String) record.get(j++)); // 品名
				ps.setBoolean(i++, (Boolean) record.get(j++)); // 各Flg
				ps.setInt(i++, (Integer) record.get(j++)); // 数量
				ps.setInt(i++, (Integer) record.get(j++)); // 数量単位CD
				if (rowType == 2) {
					ps.setBoolean(i++, (Boolean) record.get(j++)); // 分納Flg
				} else {
					ps.setNull(i++, Types.INTEGER);
					j++;
				}
				ps.setString(i++, (String) record.get(j++)); // 備考
				Object productionChildID = record.get(j); // 製作子ID
				if (productionChildID == null) {
					ps.setNull(i, Types.INTEGER);
				} else {
					ps.setInt(i, (Integer)productionChildID);
				}
				ps.addBatch();
				k++;
			}

			int[] updateCounts = ps.executeBatch();
			logger.info("T_出荷_子は" + updateCounts.length + "件処理されました。");
		}
		c.commit();
		return shippingID;
	}

}
