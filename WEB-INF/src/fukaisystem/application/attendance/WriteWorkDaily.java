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
				"MERGE INTO T_日次集計 AS d"
				+ " USING (SELECT ? AS date, ? AS cd, ? AS total, ? AS overTime, ? AS night, ? AS early, ? AS holiday, ? AS sunday, ? AS trip, ? AS absence, ? AS amPaid, ? AS pmPaid, ? AS paid, ? AS now) AS w"
				+ "  ON d.年月日=w.date AND d.人員CD=w.cd"
				+ " WHEN MATCHED THEN"
				+ "  UPDATE SET"
				+ "   d.実労働時間 = w.total,"
				+ "   d.時間外労働 = w.overTime,"
				+ "   d.深夜労働 = w.night,"
				+ "   d.遅刻早退 = w.early,"
				+ "   d.土休FLG = w.holiday,"
				+ "   d.日曜FLG = w.sunday,"
				+ "   d.出張FLG = w.trip,"
				+ "   d.欠勤FLG = w.absence,"
				+ "   d.前休FLG = w.amPaid,"
				+ "   d.後休FLG = w.pmPaid,"
				+ "   d.有給FLG = w.paid,"
				+ "   d.集計日時 = w.now"
				+ " WHEN NOT MATCHED THEN"
				+ " INSERT VALUES(w.date, w.cd, w.total, w.overTime, w.night, w.early, w.holiday, w.sunday, w.trip, w.absence, w.amPaid, w.pmPaid, w.paid, w.now);"
			);
		) {
			for (WorkDaily wd : workDailies) {
				int i = 1;
				ps.setObject(i++, wd.getWorkDate());
				ps.setInt(i++, wd.getEmployeeNo());
				ps.setInt(i++, wd.getTotalMinutes());
				ps.setInt(i++, wd.getOvertimeMinutes());
				ps.setInt(i++, wd.getLateNightMinutes());
				ps.setInt(i++, wd.getLateEarlyMinutes());
				ps.setBoolean(i++, wd.isHoliday());
				ps.setBoolean(i++, wd.isSunday());
				ps.setBoolean(i++, wd.isBusinessTrip());
				ps.setBoolean(i++, wd.isAbsence());
				ps.setBoolean(i++, wd.isAmPaidHoliday());
				ps.setBoolean(i++, wd.isPmPaidHoliday());
				ps.setBoolean(i++, wd.isPaidHoliday());
				ps.setObject(i++, now);
				ps.addBatch();
			}
			ps.executeBatch();
		}

		return null;
	}
}
