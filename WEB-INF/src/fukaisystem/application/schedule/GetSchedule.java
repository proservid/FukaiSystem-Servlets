package fukaisystem.application.schedule;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Date;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletResponse;

import fukaisystem.dto.schedule.Banner;
import fukaisystem.dto.schedule.Repeat;
import fukaisystem.dto.schedule.Schedule;
import fukaisystem.dto.schedule.ScheduleDTO;
import fukaisystem.foundation.ServiceFoundation;

public class GetSchedule extends ServiceFoundation {

	@Override
	public Object access(Connection c, ServletResponse response, Object o) throws IOException, SQLException {
		Date from = null;
		Date to = null;
		List<Date> holidays = new ArrayList<Date>();
		Map<String, Schedule> map = new HashMap<String, Schedule>();

		from = cast(response, o, Date.class);
		;
		Calendar cal = Calendar.getInstance();
		cal.setTime(from);
		cal.add(Calendar.DATE, 6);
		to = new Date(cal.getTimeInMillis());

		try (
			PreparedStatement ps = c.prepareStatement("SELECT * FROM T_祝日 WHERE 祝日>=? and 祝日<=?");
		) {
			ps.setDate(1, from);
			ps.setDate(2, to);
			ResultSet rs = ps.executeQuery();
			while (rs.next()) {
				holidays.add(rs.getDate("祝日"));
			}
		}
		try (
			PreparedStatement ps = c
				.prepareStatement("SELECT * FROM T_予定 WHERE 年月日>=? and 年月日<=? order by 人員CD,年月日");
		) {
			ps.setDate(1, from);
			ps.setDate(2, to);
			ResultSet rs = ps.executeQuery();
			while (rs.next()) {
				String cd = rs.getString("人員CD");
				String contents = rs.getString("内容");
				if (map.containsKey(cd)) {
					Map<Date, String> innerMap = map.get(cd).getText();
					Date d = rs.getDate("年月日");
					if (innerMap.containsKey(d)) {
						// 重複するはずはない
					} else {
						innerMap.put(d, contents);
					}
				} else {
					Map<Date, String> innerMap = new HashMap<Date, String>();
					innerMap.put(rs.getDate("年月日"), contents);
					map.put(cd, new Schedule(innerMap, null, null));
				}
			}
		}
		// 開始日が今週または終了日が今週または開始日が先週以前かつ終了日が来週以降
		try (
			PreparedStatement ps = c.prepareStatement(
				"SELECT * FROM T_バナー WHERE (開始日>=? and 開始日<=?) or (終了日>=? and 終了日<=?) or (開始日<? and 終了日>?) order by 人員CD,開始日,終了日"
			);
		) {
			ps.setDate(1, from);
			ps.setDate(2, to);
			ps.setDate(3, from);
			ps.setDate(4, to);
			ps.setDate(5, from);
			ps.setDate(6, to);
			ResultSet rs = ps.executeQuery();
			while (rs.next()) {
				String cd = rs.getString("人員CD");
				if (map.containsKey(cd)) {
					Schedule schedule = map.get(cd);
					List<Banner> banners = schedule.getBanners();
					if (banners == null)
						banners = new ArrayList<Banner>();
					banners
						.add(
							new Banner(
								rs.getInt("ID"), rs.getDate("開始日"), rs.getDate("終了日"), rs.getString("内容")
							)
						);
					schedule.setBanners(banners);
				} else {
					List<Banner> banners = new ArrayList<Banner>();
					banners
						.add(
							new Banner(
								rs.getInt("ID"), rs.getDate("開始日"), rs.getDate("終了日"), rs.getString("内容")
							)
						);
					map.put(cd, new Schedule(null, banners, null));
				}
			}
		}
		try (
			PreparedStatement ps = c.prepareStatement("SELECT * FROM T_繰り返し order by 人員CD");
		) {
			ResultSet rs = ps.executeQuery();
			while (rs.next()) {
				String cd = rs.getString("人員CD");
				if (map.containsKey(cd)) {
					Schedule schedule = map.get(cd);
					List<Repeat> repeats = schedule.getRepeats();
					if (repeats == null)
						repeats = new ArrayList<Repeat>();
					repeats.add(new Repeat(rs.getBoolean("曜日ごとFLG"), rs.getInt("ごと"), rs.getString("内容")));
					schedule.setRepeats(repeats);
				} else {
					List<Repeat> repeats = new ArrayList<Repeat>();
					repeats.add(new Repeat(rs.getBoolean("曜日ごとFLG"), rs.getInt("ごと"), rs.getString("内容")));
					map.put(cd, new Schedule(null, null, repeats));
				}
			}
		}
		return new ScheduleDTO(map, holidays);
	}
}
