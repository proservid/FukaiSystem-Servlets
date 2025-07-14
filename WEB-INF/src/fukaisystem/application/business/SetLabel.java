package fukaisystem.application.business;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import javax.servlet.ServletResponse;

import fukaisystem.foundation.ServiceFoundation;

/**
 * コードから取引先名を取得する
 */
public class SetLabel extends ServiceFoundation {

	@Override
	public Object access(Connection c, ServletResponse response, Object o) throws IOException, SQLException {

		int code = cast(response, o, Integer.class);
		String name = "";

		try (
			PreparedStatement ps = c.prepareStatement(
				"SELECT"
					+ " CASE"
					+ " WHEN 種別CD = 1 THEN '㈱'+会社名 + CASE WHEN 支店名 IS NULL THEN '' ELSE ' ' + 支店名 END"
					+ " WHEN 種別CD = 2 THEN 会社名+'㈱' + CASE WHEN 支店名 IS NULL THEN '' ELSE ' ' + 支店名 END"
					+ " WHEN 種別CD = 3 THEN '㈲'+会社名 + CASE WHEN 支店名 IS NULL THEN '' ELSE ' ' + 支店名 END"
					+ " WHEN 種別CD = 4 THEN 会社名+'㈲' + CASE WHEN 支店名 IS NULL THEN '' ELSE ' ' + 支店名 END"
					+ " ELSE 会社名 END"
					+ " FROM M_法人 c"
					+ " WHERE 仕入先CD=?"
			);
		) {
			ps.setInt(1, code);
			try (ResultSet rs = ps.executeQuery();) {
				if (rs.next()) {
					name = rs.getString(1);
				}
			}
		}
		return name;
	}

}
