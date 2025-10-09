package fukaisystem.application.business;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import javax.servlet.ServletResponse;

import fukaisystem.foundation.ServiceFoundation;

/**
 * 指定した日付の消費税率を取得する
 */
public class GetTaxRate extends ServiceFoundation {

	@Override
	public Object access(Connection c, ServletResponse response, Object o) throws IOException, SQLException {

		String input = cast(response, o, String.class);
		String output = "";

		try (
			PreparedStatement ps = c.prepareStatement(
				"SELECT 税率 FROM M_消費税 t WHERE 適用開始日<=? AND NOT EXISTS"
					+ " (SELECT 1 FROM M_消費税 t2 WHERE t.適用開始日<t2.適用開始日 AND 適用開始日<=?)"
			);
		) {
			ps.setString(1, input);
			ps.setString(2, input);
			try (ResultSet rs = ps.executeQuery();) {
				while (rs.next()) {
					output = rs.getString("税率");
				}
			}
		}
		return output;
	}
}
