package fukaisystem.application.aggregate;

import java.io.IOException;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Calendar;

import javax.servlet.ServletResponse;

import fukaisystem.foundation.ServiceFoundation;

public class GetState extends ServiceFoundation {

	@Override
	public Object access(Connection c, ServletResponse response, Object o) throws IOException, SQLException {


		Date from = cast(response, o, Date.class);
		Calendar target = Calendar.getInstance();
		target.setTime(from);
		target.add(Calendar.MONTH, 1);
		Date to = new Date(target.getTimeInMillis());
		target.add(Calendar.DATE, -1);

		try (
			PreparedStatement ps = c.prepareStatement(
				"SELECT 〆FLG from T_在庫_子 c"
					+ " INNER JOIN T_指定納品書 s"
					+ " ON c.納品書番号 = s.ID"
					+ " AND 納品書日>=? and 納品書日<?"
			);
		) {
			ps.setDate(1, from);
			ps.setDate(2, to);
			ResultSet rs = ps.executeQuery();
			if (rs.next()) {
				return rs.getBoolean("〆FLG");
			}
		}
		return false;
	}
}
