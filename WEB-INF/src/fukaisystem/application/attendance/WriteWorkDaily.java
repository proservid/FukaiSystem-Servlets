package fukaisystem.application.attendance;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;

import javax.servlet.ServletResponse;

import fukaisystem.domain.attendance.WorkDaily;
import fukaisystem.dto.attendance.WorkDailiesDTO;
import fukaisystem.foundation.ServiceFoundation;

/**
 * 日次打刻データを登録し、表示用データを返す
 */
public class WriteWorkDaily extends ServiceFoundation {

	@Override
	public Object access(Connection c, ServletResponse response, Object o) throws IOException, SQLException {
		List<WorkDaily> workDailies = cast(response, o, WorkDailiesDTO.class).getWorkDailies();

		LocalDateTime now = LocalDateTime.now();
		try (
			PreparedStatement ps = c.prepareStatement(
				"INSERT INTO T_日次集計 VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)"
			);
		) {
			for (WorkDaily wd : workDailies) {
				int i = 1;
				ps.setObject(i++, wd.getWorkDate());
				ps.setInt(i++, wd.getEmployeeNo());
				ps.setInt(i++, wd.getTotalMinutes());
				ps.setInt(i++, wd.getOvertimeMinutes());
				ps.setInt(i++, wd.getLateNightMinutes());
				ps.setBoolean(i++, wd.isHoliday());
				ps.setBoolean(i++, wd.isSunday());
				ps.setBoolean(i++, wd.isBusinessTrip());
				ps.setBoolean(i++, wd.isLateEarly());
				ps.setBoolean(i++, wd.isAbsence());
				ps.setBoolean(i++, wd.isPaidHoliday());
				ps.setObject(i++, now);
				ps.addBatch();
			}
			ps.executeBatch();
		}

		return null;
	}
}
