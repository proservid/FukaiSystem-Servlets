package fukaisystem.group;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Calendar;
import java.util.Map;

import javax.servlet.GenericServlet;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import org.apache.log4j.Logger;

import fukaisystem.dto.Banner;
import fukaisystem.dto.Daily;
import fukaisystem.dto.Daily2;
import fukaisystem.dto.Repeat;
import fukaisystem.sql.DBConnection;
import fukaisystem.util.Logging;

public class ScheduleRegistration extends GenericServlet {

	private static final long serialVersionUID = 1L;
    private static final Logger lg = Logger.getLogger("A1");
	private static final String className = "ScheduleRegistration\n";
	private Date from, to;

	public void service(ServletRequest request, ServletResponse response) {

		DBConnection dbc = new DBConnection();
		Connection c = dbc.getConnection();
		PreparedStatement ps = null;
		ResultSet rs = null;
		Daily2 daily = null;
		StringBuilder err = new StringBuilder();

		/**
		 * クライアントデータ受け取り
		 */
		ObjectInputStream in;
		try {
			in = new ObjectInputStream(request.getInputStream());
			Object obj = in.readObject();
			in.close();
			if(obj == null) {
				err.append(className + "readObjectがnullです\n");
				lg.error(className + "readObjectがnullです");
			} else {
				if(obj instanceof Daily2) {
					daily = (Daily2)obj;
				} else {
					err.append("型が一致しません\n");
					lg.error(className + "型が一致しません");
				}
			}
			String myCD = daily.getCD();
			Object[] members = daily.getMembers();
			int withBit = daily.getWithBit();
			int limit = 0;

			try {
				if((withBit & daily.DAILY) == daily.DAILY) {
					limit = members.length;
				} else {
					limit = 0;
				}
				ps = c.prepareStatement(
					"MERGE INTO T_予定 AS y"+
					" USING (SELECT ? AS 人員CD, ? AS 年月日, ? AS 内容, ? AS 登録日時) AS w" +
					" ON y.人員CD=w.人員CD AND y.年月日=w.年月日" +
					" WHEN MATCHED THEN" +
					"   UPDATE SET y.人員CD=w.人員CD, y.年月日=w.年月日, y.内容=w.内容, y.登録日時=w.登録日時 " +
					" WHEN NOT MATCHED THEN" +
					"   INSERT VALUES(w.人員CD, w.年月日, w.内容, w.登録日時);");
				for(int i = 0; i <= limit; i++) {
					if(i == limit) ps.setString(1, myCD);//自分
					else ps.setString(1, members[i].toString());//同行者
					ps.setDate(2, daily.getDate());
					ps.setString(3, daily.getText());
					ps.setTimestamp(4, new Timestamp(new java.util.Date().getTime()));
					ps.addBatch();
				}
				ps.executeBatch();

				if((withBit & daily.REPEAT) == daily.REPEAT) {
					limit = members.length;
				} else {
					limit = 0;
				}
				StringBuilder query = new StringBuilder("DELETE FROM T_繰り返し WHERE 人員CD IN (?");
				for(int i = 0; i < limit; i++) {
					query.append(",?");
				}
				query.append(")");
				ps = c.prepareStatement(query.toString());
				for(int i = 0; i <= limit; i++) {
					if(i == limit) ps.setString(i + 1, myCD);//自分
					else ps.setString(i + 1, members[i].toString());//同行者
				}
				ps.executeUpdate();

				ps = c.prepareStatement(
					"INSERT INTO T_繰り返し (人員CD,曜日ごとFLG,ごと,内容,登録日時) VALUES(?,?,?,?,?)");
				for(Repeat r : daily.getRepeats()) {
					if(r.getValue() > 0 && !r.getContent().equals("")) {
						for(int i = 0; i <= limit; i++) {
							if(i == limit) ps.setString(1, myCD);//自分
							else ps.setString(1, members[i].toString());//同行者
							ps.setBoolean(2, r.isWeekly());
							ps.setInt(3, r.getValue());
							ps.setString(4, r.getContent());
							ps.setTimestamp(5, new Timestamp(new java.util.Date().getTime()));
							ps.addBatch();
						}
					}
				}
				ps.executeBatch();

				if((withBit & daily.BANNER) == daily.BANNER) {
					limit = members.length;
				} else {
					limit = 0;
				}
				query = new StringBuilder("DELETE FROM T_バナー WHERE 人員CD IN (?");
				for(int i = 0; i < limit; i++) {
					query.append(",?");
				}
				query.append(") AND (開始日<=? and 終了日>=?)");
				if(daily.getIds().size() > 0) {
					query.append(" AND (");
					boolean flg = false;
					for(Integer i : daily.getIds()) {
						if(flg) {
							query.append(" OR ");
						} else flg = true;
						query.append("ID=" + i);
					}
					query.append(")");
				}
				ps = c.prepareStatement(query.toString());
				for(int i = 0; i <= limit; i++) {
					if(i == limit) ps.setString(i + 1, myCD);//自分
					else ps.setString(i + 1, members[i].toString());//同行者
				}
				ps.setDate(limit + 2, daily.getDate());
				ps.setDate(limit + 3, daily.getDate());
				ps.executeUpdate();

				ps = c.prepareStatement(
					"INSERT INTO T_バナー (人員CD,開始日,終了日,内容,登録日時) VALUES(?,?,?,?,?)");
				for(Banner b : daily.getBanners()) {
					if(!b.getContent().equals("")) {
						for(int i = 0; i <= limit; i++) {
							if(i == limit) ps.setString(1, myCD);//自分
							else ps.setString(1, members[i].toString());//同行者
							ps.setDate(2, b.getFrom());
							ps.setDate(3, b.getTo());
							ps.setString(4, b.getContent());
							ps.setTimestamp(5, new Timestamp(new java.util.Date().getTime()));
							ps.addBatch();
						}
					}
				}
				ps.executeBatch();
	 		} catch(SQLException ex) {
				ex.printStackTrace();
				err.append(className + "更新に失敗しました\n");
				Logging.logStackTrace(ex, lg, className);
			}

		} catch (Exception e) {
			// TODO 自動生成された catch ブロック
			e.printStackTrace();
		}

		/**
		 * クライアントに送信
		 */
		try {
			response.setContentType("application/octet-stream");
			ObjectOutputStream out = new ObjectOutputStream(response.getOutputStream());
			//未入力の製番があるので当面製番が見つかりませんエラーを出さない
			//out.writeObject(inputDTO.getInt(1) == 0 ? -1 : update);//製作番号が0なら、該当する製作データが登録されていなくてもOKにする
			out.writeObject(-1);//製作番号が0なら、該当する製作データが登録されていなくてもOKにする
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
