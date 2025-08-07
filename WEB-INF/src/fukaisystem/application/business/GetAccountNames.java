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
 * 入力された得意先の宛名リストを取得する
 */
public class GetAccountNames extends ServiceFoundation {

	protected static final String[] TABLE_NAMES = { "M_見積宛名", "M_出荷宛名" };

	@Override
	public Object access(Connection c, ServletResponse response, Object o) throws IOException, SQLException {

		int[] tableAndCode = cast(response, o, int[].class);
		Vector<String> names = new Vector<>();
		names.add("");

		try (
			PreparedStatement ps = c.prepareStatement(
				"SELECT 宛名 FROM " + TABLE_NAMES[tableAndCode[0]] + " WHERE 得意先CD=? ORDER BY 宛名ID"
			);
		) {
			ps.setInt(1, tableAndCode[1]);
			try (ResultSet rs = ps.executeQuery();) {
				while (rs.next()) {
					names.add(rs.getString("宛名"));
				}
			}
		}
		return names;
	}
}
