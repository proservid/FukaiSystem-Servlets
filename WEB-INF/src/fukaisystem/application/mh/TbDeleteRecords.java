package fukaisystem.application.mh;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import javax.servlet.ServletResponse;

import fukaisystem.foundation.ServiceFoundation;

/**
 * 加工実績を複数件まとめて削除するためのクラス
 *
 * @author kameura
 *
 */
public class TbDeleteRecords extends ServiceFoundation {

	@Override
	public Object access(Connection c, ServletResponse response, Object o) throws IOException, SQLException {

		int[] ids = cast(response, o, int[].class);
		if (ids == null) {
			return null;
		}
		if (ids.length == 0) {
			return Integer.valueOf(0);
		}

		StringBuilder sql = new StringBuilder("DELETE FROM T_加工実績 WHERE ID IN (?");
		for (int i = 1; i < ids.length; i++) {
			sql.append(",?");
		}
		sql.append(")");

		try (
			PreparedStatement ps = c.prepareStatement(sql.toString());
		) {
			for (int i = 0; i < ids.length; i++) {
				ps.setInt(i + 1, ids[i]);
			}
			return ps.executeUpdate();
		}
	}
}
