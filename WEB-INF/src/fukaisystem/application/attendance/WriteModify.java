package fukaisystem.application.attendance;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;
import java.time.LocalDateTime;
import java.sql.Date;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletResponse;

import fukaisystem.dto.attendance.ModifiedDTO;
import fukaisystem.foundation.ServiceFoundation;

/**
 * 打刻修正データを登録し、修正日時を返す
 */
public class WriteModify extends ServiceFoundation {

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
				"INSERT INTO T_打刻修正 VALUES(?, ?, (SELECT COUNT(*) + 1 FROM T_打刻修正 WHERE 年月日=? AND 人員CD=?), ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)"
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
				ps.setNull(i++, Types.TIMESTAMP);
				ps.addBatch();
			}
			ps.executeBatch();
		}

		return null;
	}
}
