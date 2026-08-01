package fukaisystem.application.attendance;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

import javax.servlet.ServletResponse;

import fukaisystem.dto.attendance.InitDTO;
import fukaisystem.foundation.ServiceFoundation;

/**
 * 在籍従業員マスタを取得する
 */
public class Init extends ServiceFoundation {

	@Override
	public Object access(Connection c, ServletResponse response, Object o) throws IOException, SQLException {

		Map<Integer, String> validMemberMap = new TreeMap<>();
		Map<Integer, Map<Integer, Boolean>> holidayMap = new HashMap<>();

		try (
			PreparedStatement ps = c.prepareStatement(
				"SELECT CD, 姓 + ' ' + 名 AS 氏名 FROM M_人員 WHERE CD > 9 AND CD < 10000 AND 在籍FLG='true'"
			);
		) {
			ResultSet rs = ps.executeQuery();
			while (rs.next()) {
				validMemberMap.put(rs.getInt("CD"), rs.getString("氏名"));
			}
		}

		LocalDate from = LocalDate.now().withDayOfYear(1);
		try (
			PreparedStatement ps = c.prepareStatement(
				"SELECT MONTH(祝日) AS 月, DAY(祝日) AS 日 FROM T_祝日 WHERE 祝日 >= ? AND 祝日 < ?"
			);
		) {
			ps.setString(1, from.toString());
			ps.setString(2, from.plusYears(1).toString());
			ResultSet rs = ps.executeQuery();
			while (rs.next()) {
				holidayMap.computeIfAbsent(rs.getInt("月"), k -> new HashMap<>())
					.put(rs.getInt("日"), true);
			}
		}
		return new InitDTO(validMemberMap, holidayMap);
	}
}
