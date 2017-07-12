package fukaisystem.group;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import javax.servlet.GenericServlet;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import org.apache.log4j.Logger;

import fukaisystem.sql.DBConnection;
import fukaisystem.util.Logging;

public class SetHoliday extends GenericServlet {

	private static final long serialVersionUID = 1L;
    private static final Logger lg = Logger.getLogger("A1");
	private static final String className = "SetHoliday\n";
	private Date holiday;

	public void service(ServletRequest request, ServletResponse response) {

		DBConnection dbc = new DBConnection();
		Connection c = dbc.getConnection();
		PreparedStatement ps = null;
		ResultSet rs = null;
		StringBuilder err = new StringBuilder();

		/**
		 * ƒNƒ‰ƒCƒAƒ“ƒgƒf[ƒ^ó‚¯æ‚è
		 */
		ObjectInputStream in;
		try {
			in = new ObjectInputStream(request.getInputStream());
			Object obj = in.readObject();
			in.close();
			if(obj == null) {
				err.append(className + "readObject‚ªnull‚Å‚·\n");
				lg.error(className + "readObject‚ªnull‚Å‚·");
			} else {
				if(obj instanceof Date) {
					holiday = (Date)obj;
				} else {
					err.append("Œ^‚ªˆê’v‚µ‚Ü‚¹‚ñ\n");
					lg.error(className + "Œ^‚ªˆê’v‚µ‚Ü‚¹‚ñ");
				}
			}

			try {
				ps = c.prepareStatement(
					"DELETE FROM T_j“ú WHERE j“ú=?");
				ps.setDate(1, holiday);
				if(ps.executeUpdate() == 0) {//íœ‚Å‚«‚È‚¯‚ê‚Î“o˜^‚³‚ê‚Ä‚¢‚È‚¢‚Ì‚Å“o˜^
					ps = c.prepareStatement("INSERT INTO T_j“ú VALUES (?)");
					ps.setDate(1, holiday);
					ps.executeUpdate();
				}

	 		} catch(SQLException ex) {
				ex.printStackTrace();
				err.append(className + "XV‚É¸”s‚µ‚Ü‚µ‚½\n");
				Logging.logStackTrace(ex, lg, className);
			}

		} catch (Exception e) {
			// TODO ©“®¶¬‚³‚ê‚½ catch ƒuƒƒbƒN
			e.printStackTrace();
		}

		/**
		 * ƒNƒ‰ƒCƒAƒ“ƒg‚É‘—M
		 */
		try {
			response.setContentType("application/octet-stream");
			ObjectOutputStream out = new ObjectOutputStream(response.getOutputStream());

			out.writeObject(-1);//»ì”Ô†‚ª0‚È‚çAŠY“–‚·‚é»ìƒf[ƒ^‚ª“o˜^‚³‚ê‚Ä‚¢‚È‚­‚Ä‚àOK‚É‚·‚é
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
