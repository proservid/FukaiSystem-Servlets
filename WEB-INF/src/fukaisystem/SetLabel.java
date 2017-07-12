package fukaisystem;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import javax.servlet.GenericServlet;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import org.apache.log4j.Logger;

import fukaisystem.sql.DBConnection;
import fukaisystem.util.Logging;

public class SetLabel extends GenericServlet {

	/**
	 *
	 */
	private static final long serialVersionUID = 1L;
    private static final Logger lg = Logger.getLogger("A1");
	private static final String className = "SetLabel\n";

	public void service(ServletRequest request, ServletResponse response) {

		DBConnection dbc = new DBConnection();
		Connection c = dbc.getConnection();
		PreparedStatement ps = null;
		ResultSet rs = null;
		StringBuilder err = new StringBuilder();

		int code = 0;
		String name = "";

		try {

			/**
			 * ƒNƒ‰ƒCƒAƒ“ƒgƒf[ƒ^ó‚¯æ‚è
			 */
			ObjectInputStream in = new ObjectInputStream(request.getInputStream());
			Object obj = in.readObject();
			in.close();
			if(obj == null) {
				err.append(className + "readObject‚ªnull‚Å‚·\n");
				lg.error(className + "readObject‚ªnull‚Å‚·");
			} else {
				if(obj instanceof Integer) {
					code = (Integer)obj;
				} else {
					err.append(className + "readObject‚ªStringŒ^‚Å‚Í‚ ‚è‚Ü‚¹‚ñ\n");
					lg.error(className + "readObject‚ªStringŒ^‚Å‚Í‚ ‚è‚Ü‚¹‚ñ");
				}
			}

			try {
				ps = c.prepareStatement("SELECT" +
					" CASE" +
					" WHEN í•ÊCD = 1 THEN '‡Š'+‰ïĞ–¼ + CASE WHEN x“X–¼ IS NULL THEN '' ELSE ' ' + x“X–¼ END" +
					" WHEN í•ÊCD = 2 THEN ‰ïĞ–¼+'‡Š' + CASE WHEN x“X–¼ IS NULL THEN '' ELSE ' ' + x“X–¼ END" +
					" WHEN í•ÊCD = 3 THEN '‡‹'+‰ïĞ–¼ + CASE WHEN x“X–¼ IS NULL THEN '' ELSE ' ' + x“X–¼ END" +
					" WHEN í•ÊCD = 4 THEN ‰ïĞ–¼+'‡‹' + CASE WHEN x“X–¼ IS NULL THEN '' ELSE ' ' + x“X–¼ END" +
					" ELSE ‰ïĞ–¼ END" +
					" FROM M_–@l c" +
					" WHERE d“üæCD=?");
				ps.setInt(1, code);
				rs = ps.executeQuery();
				if(rs.next()) {
					name = rs.getString(1);
				}
			} catch(SQLException ex) {
				Logging.logStackTrace(ex, lg, className);
			}


			/**
			 * ƒNƒ‰ƒCƒAƒ“ƒg‚É‘—M
			 */

			response.setContentType("application/octet-stream");
			ObjectOutputStream out = new ObjectOutputStream(response.getOutputStream());
			out.writeObject(name);
			out.writeUTF("acitve:"+dbc.getNumActive()+" idle:"+dbc.getNumIdle());
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
					lg.debug(className + "ps closed by jdbc4.0");
				}
			} catch(SQLException ex) {
				Logging.logStackTrace(ex, lg, className);
			}
			try {
				if(rs != null && !rs.isClosed()) {
					rs.close();
					lg.debug(className + "rs closed by jdbc4.0");
				}
			} catch(SQLException ex) {
				Logging.logStackTrace(ex, lg, className);
			}
		}
	}

}
