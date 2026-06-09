package fukaisystem.application.attendance;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.sql.Date;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletResponse;

import fukaisystem.dto.attendance.ModifiedDTO;
import fukaisystem.foundation.ServiceFoundation;

/**
 * 打刻修正データを登録し、修正日時 (これを登録年月日とする) を返す
 */
public class DailyModifier extends ServiceFoundation {

	@Override
	public Object access(Connection c, ServletResponse response, Object o) throws IOException, SQLException {
		ModifiedDTO dto = cast(response, o, ModifiedDTO.class);
		Date date = new Date(dto.getDate().getTime()); // java.util.Date を java.sql.Date に変換
		Map<Integer, List<Object>> map = dto.getMap();

		if (date == null || map == null) {
			return null;
		}
		LocalDateTime now = LocalDateTime.now();
		try (
			PreparedStatement ps = c.prepareStatement(
				"INSERT INTO T_打刻修正 VALUES(?, ?, (SELECT COUNT(*) + 1 FROM T_打刻修正 WHERE 年月日=? AND 人員CD=?), ?, ?, ?, ?, ?, ?, ?, ?)"
			);
		) {
			for (Map.Entry<Integer, List<Object>> m : map.entrySet()) {
				int i = 1;
				ps.setDate(i++, date);
				ps.setInt(i++, m.getKey());
				ps.setDate(i++, date);
				ps.setInt(i++, m.getKey());
				for (Object value : m.getValue()) {
					ps.setObject(i++, value == null ? null : value);
				}
				ps.setObject(i++, now);
				ps.addBatch();
			}
			ps.executeBatch();
		}
		try (PreparedStatement ps = c.prepareStatement(
				"UPDATE T_打刻 SET 登録日時=? WHERE 年月日=?"
			);
		) {
			ps.setObject(1, now);
			ps.setDate(2, date);
			ps.executeUpdate();
		}

		return now;
	}
}
