package fukaisystem.application.attendance;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletResponse;

import fukaisystem.dto.attendance.InitDTO;
import fukaisystem.foundation.ServiceFoundation;

/**
 * 日次打刻データを登録し、表示用データを返す
 */
public class Init extends ServiceFoundation {

	@Override
	public Object transaction(Connection c, ServletResponse response, Object o) throws IOException, SQLException {
		Map<Integer, String> validMembers = new HashMap<>();
		try (
			PreparedStatement ps = c.prepareStatement(
				"SELECT CD, 姓+名 AS 氏名 FROM M_人員 WHERE 在籍FLG='true'"
			);
		) {
			ResultSet rs = ps.executeQuery();
			while (rs.next()) {
				validMembers.put(rs.getInt("CD"), rs.getString("氏名"));
			}
		}
		return new InitDTO(validMembers);
	}
}
