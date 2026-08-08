package fukaisystem.application.mh;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import javax.servlet.ServletResponse;

import fukaisystem.foundation.ServiceFoundation;

/**
 * 加工実績を1件削除するためのクラス
 *
 * @author kameura
 *
 */
public class DeleteInputRecord extends ServiceFoundation {

	@Override
	public Object access(Connection c, ServletResponse response, Object o) throws IOException, SQLException {

		Integer id = cast(response, o, Integer.class);
		if (id == null) {
			return null;
		}

		try (
			PreparedStatement ps = c.prepareStatement("DELETE FROM T_加工実績 WHERE ID=?");
		) {
			ps.setInt(1, id);
			return ps.executeUpdate();
		}
	}
}
