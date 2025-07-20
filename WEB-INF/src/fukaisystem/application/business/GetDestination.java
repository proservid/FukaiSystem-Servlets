package fukaisystem.application.business;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Vector;

import javax.servlet.ServletResponse;

import fukaisystem.foundation.ServiceFoundation;

/**
 * 入力された得意先の納入先リストを取得する
 */
public class GetDestination extends ServiceFoundation {

	@Override
	public Object access(Connection c, ServletResponse response, Object o) throws IOException, SQLException {

		Integer code = cast(response, o, Integer.class);
		Vector<String> destinations = new Vector<>();
		destinations.add("");

		try (
			PreparedStatement ps = c.prepareStatement(
				"SELECT 納入先名 FROM M_納入先 WHERE 得意先CD=? ORDER BY 納入先ID"
			);
		) {
			ps.setInt(1, code);
			try (ResultSet rs = ps.executeQuery();) {
				while (rs.next()) {
					destinations.add(rs.getString("納入先名"));
				}
			}
		}
		return destinations;
	}
}
