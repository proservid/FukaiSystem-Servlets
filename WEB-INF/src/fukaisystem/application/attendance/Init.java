package fukaisystem.application.attendance;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletResponse;

import fukaisystem.domain.attendance.DailyRecords;
import fukaisystem.domain.attendance.TimeRecord;
import fukaisystem.foundation.ServiceFoundation;

/**
 * 日次打刻データを登録し、表示用データを返す
 */
public class Init extends ServiceFoundation {

	@Override
	public Object transaction(Connection c, ServletResponse response, Object o) throws IOException, SQLException {
		LocalDate date = cast(response, o, LocalDate.class);

		if (date == null) {
			return null;
		}
		Map<Integer, TimeRecord.Builder> builders = new HashMap<>();
		LocalDateTime registerDate = null;
		try (
			PreparedStatement ps = c.prepareStatement(
				"SELECT * FROM T_打刻 WHERE 年月日=?"
			);
		) {
			ps.setObject(1, date);
			ResultSet rs = ps.executeQuery();
			while (rs.next()) {
				registerDate = rs.getObject("登録日時", LocalDateTime.class);
				TimeRecord.Builder builder = TimeRecord.builder()
					.employeeNo(rs.getInt("人員CD"))
					.timeCardNo(rs.getInt("カード番号"))
					.clockIn(rs.getObject("出勤時刻", LocalTime.class))
					.goOut(rs.getObject("外出時刻", LocalTime.class))
					.returnIn(rs.getObject("戻り時刻", LocalTime.class))
					.clockOut(rs.getObject("退勤時刻", LocalTime.class))
					.businessTrip(rs.getBoolean("出張FLG"))
					.paidHoliday(rs.getBoolean("有給FLG"))
					.note(rs.getString("備考"));
				builders.put(rs.getInt("人員CD"), builder);
			}
		}
		try (
			PreparedStatement ps = c.prepareStatement(
				"SELECT * FROM T_打刻修正 WHERE 年月日=?"
			);
		) {
			ps.setObject(1, date);
			ResultSet rs = ps.executeQuery();
			while (rs.next()) {
				TimeRecord.Builder builder = builders.get(rs.getInt("人員CD"));
				LocalTime clockIn = rs.getObject("出勤時刻", LocalTime.class);
				if (clockIn != null) {
					builder.clockIn(clockIn);
				}
				LocalTime goOut = rs.getObject("外出時刻", LocalTime.class);
				if (goOut != null) {
					builder.goOut(goOut);
				}
				LocalTime returnIn = rs.getObject("戻り時刻", LocalTime.class);
				if (returnIn != null) {
					builder.returnIn(returnIn);
				}
				LocalTime clockOut = rs.getObject("退勤時刻", LocalTime.class);
				if (clockOut != null) {
					builder.clockOut(clockOut);
				}
				boolean businessTrip = rs.getBoolean("出張FLG");
				if (!rs.wasNull()) {
					builder.businessTrip(businessTrip);
				}
				boolean paidHoliday = rs.getBoolean("有給FLG");
				if (!rs.wasNull()) {
					builder.paidHoliday(paidHoliday);
				}
				String note = rs.getString("備考");
				if (note != null) {
					builder.note(note);
				}
			}
		}
		List<TimeRecord> records = new ArrayList<>();
		for (Map.Entry<Integer, TimeRecord.Builder> m : builders.entrySet()) {
			records.add(m.getValue().build());
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
			.registerDate(registerDate)
			.build();
	}
}
