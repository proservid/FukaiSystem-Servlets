package fukaisystem.group;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
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

import javax.servlet.GenericServlet;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import org.apache.log4j.Logger;

import fukaisystem.sql.DBConnection;
import fukaisystem.util.Logging;
import fukaisystem.dto.Banner;
import fukaisystem.dto.Repeat;
import fukaisystem.dto.Schedule;
import fukaisystem.dto.ScheduleDTO;

public class GetSchedule extends GenericServlet {

	/**
	 *
	 */
	private static final long serialVersionUID = 1L;
	static final Logger lg = Logger.getLogger("A1");
	private static final String className = "GetSchedule\n";

	public void service(ServletRequest request, ServletResponse response) {

		DBConnection dbc = new DBConnection();
		Connection c = dbc.getConnection();
		PreparedStatement ps = null;
		ResultSet rs = null;
		Date from = null;
		Date to = null;
		StringBuilder err = new StringBuilder("");
		List<Date> holidays = new ArrayList<Date>();
		Map<String, Schedule> map = new HashMap<String, Schedule>();

		try {

			/**
			 * クライアントデータ受け取り
			 */
			ObjectInputStream in = new ObjectInputStream(request.getInputStream());
			Object obj = in.readObject();
			in.close();

			if(obj == null) {
				err.append(className + "readObjectがnullです\n");
				lg.error(className + "readObjectがnullです");
			} else {
				if(obj instanceof Date) {
					from = (Date)obj;
					Calendar cal = Calendar.getInstance();
					cal.setTime(from);
					cal.add(Calendar.DATE, 6);
					to = new Date(cal.getTimeInMillis());
				} else {
					err.append(className + "readObjectがString型ではありません\n");
					lg.error(className + "readObjectがString型ではありません");
				}
			}

			try {
				ps = c.prepareStatement("SELECT * FROM T_祝日 WHERE 祝日>=? and 祝日<=?");
				ps.setDate(1, from);
				ps.setDate(2, to);
				rs = ps.executeQuery();
				while(rs.next()) {
					holidays.add(rs.getDate("祝日"));
				}

				ps = c.prepareStatement("SELECT * FROM T_予定 WHERE 年月日>=? and 年月日<=? order by 人員CD,年月日");
				ps.setDate(1, from);
				ps.setDate(2, to);
				rs = ps.executeQuery();
				while(rs.next()) {
					String cd = rs.getString("人員CD");
					String contents = rs.getString("内容");
					if(map.containsKey(cd)) {
						Map<Date, String> innerMap = map.get(cd).getText();
						Date d = rs.getDate("年月日");
						if(innerMap.containsKey(d)) {
							//重複するはずはない
						} else {
							innerMap.put(d, contents);
						}
					} else {
						Map<Date, String> innerMap = new HashMap<Date, String>();
						innerMap.put(rs.getDate("年月日"), contents);
						map.put(cd, new Schedule(innerMap, null, null));
					}
				}
				//開始日が今週または終了日が今週または開始日が先週以前かつ終了日が来週以降
				ps = c.prepareStatement("SELECT * FROM T_バナー WHERE (開始日>=? and 開始日<=?) or (終了日>=? and 終了日<=?) or (開始日<? and 終了日>?) order by 人員CD,開始日,終了日");
				ps.setDate(1, from);
				ps.setDate(2, to);
				ps.setDate(3, from);
				ps.setDate(4, to);
				ps.setDate(5, from);
				ps.setDate(6, to);
				rs = ps.executeQuery();
				while(rs.next()) {
					String cd = rs.getString("人員CD");
					if(map.containsKey(cd)) {
						Schedule schedule = map.get(cd);
						List<Banner> banners = schedule.getBanners();
						if(banners == null) banners = new ArrayList<Banner>();
						banners.add(new Banner(rs.getInt("ID"), rs.getDate("開始日"), rs.getDate("終了日"), rs.getString("内容")));
						schedule.setBanners(banners);
					} else {
						List<Banner> banners = new ArrayList<Banner>();
						banners.add(new Banner(rs.getInt("ID"), rs.getDate("開始日"), rs.getDate("終了日"), rs.getString("内容")));
						map.put(cd, new Schedule(null, banners, null));
					}
				}

				ps = c.prepareStatement("SELECT * FROM T_繰り返し order by 人員CD");
				rs = ps.executeQuery();
				while(rs.next()) {
					String cd = rs.getString("人員CD");
					if(map.containsKey(cd)) {
						Schedule schedule = map.get(cd);
						List<Repeat> repeats = schedule.getRepeats();
						if(repeats == null) repeats = new ArrayList<Repeat>();
						repeats.add(new Repeat(rs.getBoolean("曜日ごとFLG"), rs.getInt("ごと"), rs.getString("内容")));
						schedule.setRepeats(repeats);
					} else {
						List<Repeat> repeats = new ArrayList<Repeat>();
						repeats.add(new Repeat(rs.getBoolean("曜日ごとFLG"), rs.getInt("ごと"), rs.getString("内容")));
						map.put(cd, new Schedule(null, null, repeats));
					}
				}
			} catch(SQLException ex) {
				err.append("テーブル「T_テーブル名」の読込に失敗しました\n");
				Logging.logStackTrace(ex, lg, className);
			}

		} catch(Exception ex) {
			Logging.logStackTrace(ex, lg, className);
		}

		/**
		 * クライアントに送信
		 */
		try {
			response.setContentType("application/octet-stream");
			ObjectOutputStream out = new ObjectOutputStream(response.getOutputStream());
			out.writeObject(new ScheduleDTO(map, holidays));
			out.writeUTF(err.toString());
			out.flush();
			out.close();
		}catch(Exception ex) {
			Logging.logStackTrace(ex, lg, className);
		} finally {
			try {
				if(c != null && !c.isClosed()) c.close();
			} catch(SQLException ex) {
				Logging.logStackTrace(ex, lg, className);
			}
// The following processes requires JDBC4.0.
			try {
				if(ps != null && !ps.isClosed()) {
					ps.close();
					lg.debug(className + "ps is closed by jdbc4.0");
				}
			} catch(SQLException ex) {
				Logging.logStackTrace(ex, lg, className);
			}
			try {
				if(rs != null && !rs.isClosed()) {
					rs.close();
					lg.debug(className + "rs is closed by jdbc4.0");
				}
			} catch(SQLException ex) {
				Logging.logStackTrace(ex, lg, className);
			}
		}
	}
}
