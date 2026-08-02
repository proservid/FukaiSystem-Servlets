package fukaisystem.application.group;

import java.io.IOException;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import javax.servlet.ServletResponse;

import fukaisystem.foundation.ServiceFoundation;

public class SetHoliday extends ServiceFoundation {

	private Date holiday;

	@Override
	public Object access(Connection c, ServletResponse response, Object o) throws IOException, SQLException {

		holiday = cast(response, o, Date.class);

		try (
			PreparedStatement ps = c.prepareStatement("DELETE FROM T_祝日 WHERE 祝日=?");
		) {
			ps.setDate(1, holiday);
			if (ps.executeUpdate() == 0) { // 削除できなければ登録されていないので登録
				try (PreparedStatement ps2 = c.prepareStatement("INSERT INTO T_祝日 VALUES (?)");) {
					ps2.setDate(1, holiday);
					ps2.executeUpdate();
				}
			}
			return -1;
		}
	}
}
