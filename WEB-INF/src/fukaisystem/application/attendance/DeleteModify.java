package fukaisystem.application.attendance;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.LocalDate;
import javax.servlet.ServletResponse;

import fukaisystem.foundation.ServiceFoundation;

/**
 * 打刻修正データを削除する
 */
public class DeleteModify extends ServiceFoundation {

	@Override
	public Object access(Connection c, ServletResponse response, Object o) throws IOException, SQLException {
		LocalDate date = cast(response, o, LocalDate.class);
		if (date == null) {
			return null;
		}

		try (
			PreparedStatement ps = c.prepareStatement("DELETE FROM T_打刻修正 WHERE 年月日=?");
		) {
			ps.setObject(1, date);
			ps.executeUpdate();
		}

		return null;
	}
}
