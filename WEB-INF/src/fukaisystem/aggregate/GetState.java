package fukaisystem.aggregate;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Calendar;

import javax.servlet.GenericServlet;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import org.apache.log4j.Logger;

import fukaisystem.sql.DBConnection;

public class GetState extends GenericServlet {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private static Logger lg = Logger.getLogger("dbtool");

	public void service(ServletRequest request, ServletResponse response) {
		StringBuilder err = new StringBuilder("");

		DBConnection dbc = new DBConnection();
		Connection c = dbc.getConnection();
		PreparedStatement ps = null;
		ResultSet rs = null;
		Date from = null;
		Date to = null;
		Date last = null;
		int month = 0;
		boolean b = false;
		try {
			ObjectInputStream in = new ObjectInputStream(request.getInputStream());
			Object obj = in.readObject();
			if (obj instanceof Date) {
				from = (Date) obj;
				Calendar target = Calendar.getInstance();
				target.setTime(from);
				month = target.get(Calendar.MONTH) + 1;
				target.add(Calendar.MONTH, 1);
				to = new Date(target.getTimeInMillis());
				target.add(Calendar.DATE, -1);
				last = new Date(target.getTimeInMillis());
			}
			in.close();
			try {
				ps = c.prepareStatement(
					"SELECT 〆FLG from T_在庫_子 c"
						+ " INNER JOIN T_指定納品書 s"
						+ " ON c.納品書番号 = s.ID"
						+ " AND 納品書日>=? and 納品書日<?"
				);
				ps.setDate(1, from);
				ps.setDate(2, to);
				rs = ps.executeQuery();
				if (rs.next()) {
					b = rs.getBoolean("〆FLG");
				}
			} catch (SQLException ex) {
				ex.printStackTrace();
				err.append(ex + "\n");
				lg.error("GetState " + ex);
			}
			// クライアントに送信
			response.setContentType("application/octet-stream");
			ObjectOutputStream out = new ObjectOutputStream(response.getOutputStream());
			out.writeObject(b);
			out.writeUTF(err.toString());
			out.flush();
			out.close();
		} catch (Exception ex) {
			ex.printStackTrace();
			lg.error(ex);
		} finally {
			try {
				if (c != null && !c.isClosed())
					c.close();
			} catch (SQLException ex) {
				lg.error("c:" + ex);
			}
			// The following processes requires JDBC4.0.
			try {
				if (ps != null && !ps.isClosed()) {
					ps.close();
					lg.debug("ps is closed by jdbc4.0");
				}
			} catch (SQLException ex) {
				lg.error("ps:" + ex);
			}
			try {
				if (rs != null && !rs.isClosed()) {
					rs.close();
					lg.debug("rs is closed by jdbc4.0");
				}
			} catch (SQLException ex) {
				lg.error("rs:" + ex);
			}
		}
	}
}
