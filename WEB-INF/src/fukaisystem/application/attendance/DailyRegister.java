package fukaisystem.application.attendance;

import java.io.IOException;
import java.sql.Connection;
import java.util.Date;
// import java.sql.PreparedStatement;
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
		// try (
		// 	PreparedStatement ps = c.prepareStatement(
		// 		"DELETE FROM T_打刻 WHERE 得意先CD=?"
		// 	);
		// ) {
		// 	ps.setInt(1, accountCD);
		// 	ps.executeUpdate();
		// }
		// try (
		// 	PreparedStatement ps = c.prepareStatement(
		// 		"INSERT INTO T_打刻 VALUES(?, ?, ?)"
		// 	);
		// ) {
		// 	int i = 1;
		// 	for (Vector<Object> record : accountNames) {
		// 		String destination = (String) record.get(0);
		// 		if (destination.isEmpty()) {
		// 			continue;
		// 		}
		// 		ps.setInt(1, accountCD);
		// 		ps.setInt(2, i++); // 宛名ID
		// 		ps.setString(3, destination);
		// 		ps.addBatch();
		// 	}
		// 	ps.executeBatch();
		// }
		return DailyRecords.builder()
			.workDate(date)
			.records(records)
			.holiday(true)
			.registerDate(new Date())
			.build();
	}
}
