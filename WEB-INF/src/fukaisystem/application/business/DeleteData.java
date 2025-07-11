package fukaisystem.application.business;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import javax.servlet.ServletResponse;

import fukaisystem.foundation.ServiceFoundation;

/**
 * 指定した登録データを削除する
 */
public class DeleteData extends ServiceFoundation {

	@Override
	public Object access(Connection c, ServletResponse response, Object o) throws IOException, SQLException {
		String query1 = "", query2 = "";
		int type = 0, id = 0;

		Integer[] param = cast(response, o, Integer[].class);

		type = param[0];
		id = param[1];
		switch (type) {
			case 1:
				query1 = "DELETE FROM T_見積_親 WHERE 見積親ID=?";
				query2 = "DELETE FROM T_見積_子 WHERE 見積親ID=?";
				break;
			case 2:
				query1 = "DELETE FROM T_製作_親 WHERE 製作親ID=?";
				query2 = "DELETE FROM T_製作_子 WHERE 製作親ID=?";
				break;
			case 3:
				query1 = "DELETE FROM T_在庫_親 WHERE 在庫親ID=?";
				query2 = "DELETE FROM T_在庫_子 WHERE 在庫親ID=?";
				break;
			case 4:
				query1 = "DELETE FROM T_売上_親 WHERE 売上親ID=?";
				query2 = "DELETE FROM T_売上_子 WHERE 売上親ID=?";
				break;
			case 5:
				query1 = "DELETE FROM T_出庫_親 WHERE 出庫親ID=?";
				query2 = "DELETE FROM T_出庫_子 WHERE 出庫親ID=?";
				break;
		}

		if (type > 0) {
			try (PreparedStatement ps = c.prepareStatement(query1);) {
				ps.setInt(1, id);
				ps.executeUpdate();
			}
			// 明細
			try (PreparedStatement ps = c.prepareStatement(query2);) {
				ps.setInt(1, id);
				ps.executeUpdate();
			}
		}
		return null;
	}

}
