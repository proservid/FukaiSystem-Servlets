package fukaisystem.application.attendance;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import javax.servlet.ServletResponse;

import fukaisystem.domain.attendance.DailyRecords;
import fukaisystem.domain.attendance.TimeRecord;
import fukaisystem.foundation.ServiceFoundation;

/**
 * 日次打刻データを登録し、表示用データを返す
 */
public class WriteRecords extends ServiceFoundation {

	@Override
	public Object transaction(Connection c, ServletResponse response, Object o) throws IOException, SQLException {
		DailyRecords dailyRecords = cast(response, o, DailyRecords.class);
		LocalDate date = dailyRecords.getWorkDate();
		List<TimeRecord> records = dailyRecords.getRecords();

		if (date == null || records == null) {
			return null;
		}
		try (
			PreparedStatement ps = c.prepareStatement(
				"DELETE FROM T_打刻 WHERE 年月日=?"
			);
		) {
			ps.setObject(1, date);
			ps.executeUpdate();
		}
		LocalDateTime now = LocalDateTime.now();
		try (
			PreparedStatement ps = c.prepareStatement(
				"INSERT INTO T_打刻 VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)"
			);
		) {
			for (TimeRecord record : records) {
				int i = 1;
				ps.setObject(i++, date);
				ps.setInt(i++, record.getEmployeeNo());
				ps.setInt(i++, record.getTimeCardNo());
				ps.setObject(i++, record.getClockIn());
				ps.setObject(i++, record.getGoOut());
				ps.setObject(i++, record.getReturnIn());
				ps.setObject(i++, record.getClockOut());
				ps.setBoolean(i++, record.isBusinessTrip());
				ps.setBoolean(i++, record.isPaidHoliday());
				ps.setString(i++, record.getNote());
				ps.setObject(i++, now);
				ps.setNull(i++, Types.TIMESTAMP);
				ps.addBatch();
			}
			ps.executeBatch();
		}
		boolean isHoliday = false;
		try (
			PreparedStatement ps = c.prepareStatement(
				"SELECT 1 FROM T_祝日 WHERE 祝日=?"
			);
		) {
			ps.setObject(1, date);
			ResultSet rs = ps.executeQuery();
			if (rs.next()) {
				isHoliday = true;
			}
		}

		return DailyRecords.builder()
			.workDate(date)
			.records(records)
			.holiday(isHoliday)
			.registerDate(now)
			.build();
	}
}
