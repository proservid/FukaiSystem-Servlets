package fukaisystem.aggregate;

import java.io.*;
import java.sql.*;
import java.sql.Date;
import java.util.*;
import javax.servlet.*;

import fukaisystem.sql.DBConnection;
import org.apache.log4j.Logger;

/**
 * テーブルの内容と列情報を取得するためのクラス
 * @author kameura
 *
 */
public class SetRelease extends GenericServlet {

	private static final long serialVersionUID = 1L;
	private static Logger lg = Logger.getLogger("dbtool");

	public void service(ServletRequest request, ServletResponse response) {

		DBConnection dbc = new DBConnection();
		Connection c = dbc.getConnection();
		PreparedStatement ps = null;
		ResultSet rs = null;

		Date from = null;
		Date to = null;
		Date from2 = null;
		Date to2 = null;
		int month = 0;
		Boolean b = null;
		StringBuilder err = new StringBuilder("");

		try {
	//クライアントから読み込み

			ObjectInputStream in = new ObjectInputStream(request.getInputStream());
			Object obj = in.readObject();
			if(obj instanceof Date) {
				from = (Date)obj;
				Calendar target = Calendar.getInstance();
				target.setTime(from);//今月1日
				month = target.get(Calendar.MONTH) + 1;
				target.add(Calendar.MONTH, 1);//翌月1日
				to = new Date(target.getTimeInMillis());
				target.setTime(from);//今月1日（次の2014年判定のためにこのタイミングでセットする必要がある）
				if(target.get(Calendar.YEAR) < 2014) {//2013年以前は25日〆
					if(target.get(Calendar.MONTH) == 11) {//12月は11月26日から12月31日
						to2 = to;
						target.add(Calendar.DATE, 25);//今月26日
						target.add(Calendar.MONTH, -1);//先月26日
						from2 = new Date(target.getTimeInMillis());
					} else {
						target.add(Calendar.DATE, 25);//今月26日
						to2 = new Date(target.getTimeInMillis());
						if(target.get(Calendar.MONTH) == 0) {//1月は1月1日から1月25日
							from2 = from;
						} else {
							target.add(Calendar.MONTH, -1);//先月26日
							from2 = new Date(target.getTimeInMillis());
						}
					}
				} else {//2014年以後は月末〆
					to2 = to;
					from2 = from;
				}
			}
			in.close();
			String sql = "UPDATE T_指定納品書"
					+ " SET 〆FLG = 'false'"
					+ " WHERE 納品書日>=? and 納品書日<?";
			try {
				ps = c.prepareStatement(sql);
				int n = 1;
				ps.setDate(n++, month == 1 ? from : from2);//納品
				ps.setDate(n++, month == 12 ? to : to2);//納品
				ps.executeUpdate();
				b = false;
			} catch(SQLException ex) {
				ex.printStackTrace();
				err.append(ex + "\n");
				lg.error("GetElements3 " + ex);
			}

	//クライアントに送信

			response.setContentType("application/octet-stream");
			ObjectOutputStream out = new ObjectOutputStream(response.getOutputStream());
			out.writeObject(b);
			out.writeUTF(err.toString());
			out.flush();
			out.close();
		}catch(Exception ex) {
			ex.printStackTrace();
			lg.error(ex);
		} finally {
			try {
				if(c != null && !c.isClosed()) c.close();
			} catch(SQLException ex) {
				lg.error("c:" + ex);
			}
			// The following processes requires JDBC4.0.
			try {
				if(ps != null && !ps.isClosed()) {
					ps.close();
					lg.debug("ps is closed by jdbc4.0");
				}
			} catch(SQLException ex) {
				lg.error("ps:" + ex);
			}
			try {
				if(rs != null && !rs.isClosed()) {
					rs.close();
					lg.debug("rs is closed by jdbc4.0");
				}
			} catch(SQLException ex) {
				lg.error("rs:" + ex);
			}
		}
	}
}