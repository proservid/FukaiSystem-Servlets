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

import fukaisystem.dto.business.SalesDTO;
import fukaisystem.foundation.ServiceFoundation;

/**
 * 売上伝票データを登録する
 */
public class SalesRegister extends ServiceFoundation {

	protected static final Logger logger = Logger.getLogger("A1");

	@Override
	public Object transaction(Connection c, ServletResponse response, Object o) throws IOException, SQLException {
		
		int salesID = 0;
		int productionID = 0;
		
		SalesDTO salesDTO = cast(response, o, SalesDTO.class);
		salesID = salesDTO.getInt(0);
		productionID = salesDTO.getInt(1);

		boolean isEmpty = true;
		for (Vector<Object> record : salesDTO.getVector()) {
			if ((Integer) record.get(2) != 0) {
				isEmpty = false;
			}
		}

		if (isEmpty) {
			addError("明細データがありません。\n");
			return null;
		}
		if (salesID == 0) { // 新規
			try (
				PreparedStatement ps = c.prepareStatement(
					"INSERT INTO T_売上_親"
						+ " OUTPUT inserted.売上親ID as newId, inserted.更新日"
						+ " VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)"
				);
			) {
				int i = 1;
				ps.setInt(i++, salesDTO.getInt(2)); // 得意先CD
				ps.setDate(i++, salesDTO.getDate()); // 売上年月日
				ps.setBoolean(i++, (salesDTO.getInt(7) != 2)); // 売上FLG
				ps.setBoolean(i++, (salesDTO.getInt(7) != 1)); // 請求FLG
				ps.setInt(i++, salesDTO.getInt(3)); // 納品区分
				ps.setInt(i++, salesDTO.getInt(4)); // 納品手段
				if (salesDTO.getInt(5) < 0) {
					ps.setNull(i++, Types.INTEGER);
				} else {
					ps.setInt(i++, salesDTO.getInt(5)); // 消費税
				}
				ps.setInt(i++, salesDTO.getInt(6)); // 値引き
				ps.setString(i++, salesDTO.getString()); // 摘要
				ps.setTimestamp(i++, new Timestamp(new java.util.Date().getTime())); // 更新日
				ps.setInt(i, 0); // 更新者CD
				boolean isResultSet = ps.execute();
				int updateCount = 0;
				while (true) {
					if (isResultSet) {
						try (ResultSet rs = ps.getResultSet();) {
							while (rs.next()) {
								salesID = rs.getInt(1);
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
		} else { // 更新
			try (
				PreparedStatement ps = c.prepareStatement(
					"UPDATE T_売上_親 SET 得意先CD=?, 売上年月日=?, 売上FLG=?, 請求FLG=?, 納品区分CD=?, 納品手段CD=?, 消費税=?, 値引き=?, 摘要=?, 更新日=?, 更新者CD=?"
						+ " WHERE 売上親ID=?"
				);
			) {
				int i = 1;
				ps.setInt(i++, salesDTO.getInt(2)); // 得意先CD
				ps.setDate(i++, salesDTO.getDate()); // 売上年月日
				ps.setBoolean(i++, (salesDTO.getInt(7) != 2)); // 売上FLG
				ps.setBoolean(i++, (salesDTO.getInt(7) != 1)); // 請求FLG
				ps.setInt(i++, salesDTO.getInt(3)); // 納品区分
				ps.setInt(i++, salesDTO.getInt(4)); // 納品手段
				if (salesDTO.getInt(5) < 0) {
					ps.setNull(i++, Types.INTEGER);
				} else {
					ps.setInt(i++, salesDTO.getInt(5)); // 消費税
				}
				ps.setInt(i++, salesDTO.getInt(6)); // 値引き
				ps.setString(i++, salesDTO.getString()); // 摘要
				ps.setTimestamp(i++, new Timestamp(new java.util.Date().getTime())); // 更新日
				ps.setInt(i++, 0); // 更新者CD
				ps.setInt(i, salesID);
				ps.executeUpdate();
			}
			try (PreparedStatement ps = c.prepareStatement("DELETE FROM T_売上_子 WHERE 売上親ID=?");) {
				ps.setInt(1, salesID);
				ps.executeUpdate();
			}
		}
		// UPDATE失敗したらINSERTさせない
		int k = 1;
		try (
			PreparedStatement ps = c
				.prepareStatement("INSERT INTO T_売上_子 VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?);");
		) {
			// "UPDATE T_製作_子 SET 納品年月日=? WHERE ID=? AND 製作親ID=?");
			for (Vector<Object> record : salesDTO.getVector()) {
				int tag = (Integer) record.get(2);
				if (tag != 0) {
					int i = 1;
					int j = 1;
					ps.setInt(i++, salesID); // 売上親ID
					ps.setInt(i++, k); // ID
					ps.setInt(i++, productionID); // 製作親ID
					ps.setInt(i++, (Integer) record.get(j++)); // 製作子ID=1
					ps.setInt(i++, tag);
					j++; // 表示CD=2
					ps.setString(i++, (String) record.get(j)); // 出荷伝票番号=3
					j += 3;// 出荷伝票番号=3
					ps.setString(i++, (String) record.get(j++)); // 品名=6
					ps.setBoolean(i++, (Boolean) record.get(j++)); // 各=7
					ps.setInt(i++, (Integer) record.get(j++)); // 数量=8
					ps.setInt(i++, (Integer) record.get(j++)); // 単位=9
					ps.setInt(i++, (Integer) record.get(j++)); // 単価=10
					ps.setInt(i++, (Integer) record.get(j++)); // 金額=11
					ps.setString(i++, (String) record.get(j)); // 備考=12
					// ps.setDate(i++, salesDTO.getDate()); j = 1; //売上年月日
					// ps.setInt(i++, (Integer)record.get(j)); j = 0; //製作子ID
					// ps.setInt(i, (Integer)record.get(j)); //製作親ID
					ps.addBatch();
					k++;
				}
			}
			int[] updateCounts = ps.executeBatch();
			logger.info("T_売上_子は" + updateCounts.length + "件処理されました。");
		}
		return salesID;
	}
}
