package fukaisystem.application.attendance;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

import javax.servlet.ServletResponse;

import fukaisystem.domain.attendance.DailyRecords;
import fukaisystem.domain.attendance.TimeRecord;
import fukaisystem.foundation.ServiceFoundation;

/**
 * 日次打刻データを登録し、表示用データを返す
 */
public class DailyRegister extends ServiceFoundation {

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
		try (
			PreparedStatement ps = c.prepareStatement(
				"INSERT INTO T_打刻 VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)"
			);
		) {
			for (TimeRecord record : records) {
				ps.setObject(1, date);
				ps.setInt(2, record.getEmployeeNo());
				ps.setInt(3, record.getTimeCardNo());
				ps.setObject(4, record.getClockIn());
				ps.setObject(5, record.getGoOut());
				ps.setObject(6, record.getReturnIn());
				ps.setObject(7, record.getClockOut());
				ps.setBoolean(8, record.isBusinessTrip());
				ps.setBoolean(9, record.isPaidHoliday());
				ps.setString(10, record.getNote());
				ps.setObject(11, null);
				ps.addBatch();
			}
			ps.executeBatch();
		}
		boolean isHoliday = false;
		try (PreparedStatement ps = c.prepareStatement(
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
			.registerDate(null)
			.build();
	}
}
