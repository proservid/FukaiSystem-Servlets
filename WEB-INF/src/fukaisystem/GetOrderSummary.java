package fukaisystem;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Vector;

import javax.servlet.GenericServlet;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import org.apache.log4j.Logger;

import fukaisystem.dto.OrderDocumentDTO;
import fukaisystem.sql.DBConnection;
import fukaisystem.util.Logging;


public class GetOrderSummary extends GenericServlet {

	private static final long serialVersionUID = 1L;
	private static final Logger lg = Logger.getLogger("A1");
	private static final String className = "GetOrderSummary\n";

	@Override
	public void service(ServletRequest request, ServletResponse response) {
		DBConnection dbc = new DBConnection();
		Connection c = dbc.getConnection();
		PreparedStatement ps = null;
		ResultSet rs = null;
		OrderDocumentDTO odDTO = null;
		StringBuilder err = new StringBuilder();

		int id = 0;

		Vector<Vector<Object>> data = new Vector<Vector<Object>>();

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
				if(obj instanceof Number) {
					id = ((Number)obj).intValue();
				} else {
					err.append(className + "readObject‚ªSetSummaryDTOŒ^‚Å‚Í‚ ‚è‚Ü‚¹‚ñ\n");
					lg.error(className + "readObject‚ªSetSummaryDTOŒ^‚Å‚Í‚ ‚è‚Ü‚¹‚ñ");
				}
			}
			try {
				ps = c.prepareStatement("SELECT * FROM T_İŒÉ_q s LEFT OUTER JOIN T_w’è”[•i‘ slip " +
						"ON s.”[•i‘”Ô†=slip.ID WHERE İŒÉeID=?");
				ps.setInt(1, id);
				rs = ps.executeQuery();
				while(rs.next()) {
					Vector<Object> line = new Vector<Object>();
					line.add(rs.getInt("•\¦CD"));
					line.add(rs.getInt("‘å•ª—ŞCD"));
					line.add(rs.getInt("’†•ª—ŞCD"));
					line.add(rs.getInt("¬•ª—ŞCD"));
					line.add(rs.getString("Ş—¿•i–¼"));
					line.add(rs.getBoolean("ŠeFLG"));
					line.add(rs.getInt("”—Ê"));
					line.add(rs.getInt("”—Ê’PˆÊCD"));
					line.add(rs.getDouble("d—Ê’·‚³"));
					line.add(rs.getInt("’P‰¿"));
					line.add(rs.getInt("‹àŠz"));
					line.add(rs.getString("”õl"));
					line.add(rs.getDate("“üŒÉ”NŒ“ú"));
					line.add(rs.getDate("“üŒÉ”NŒ“ú") != null);
					line.add(rs.getInt("”[•i‘”Ô†"));
					line.add(rs.getDate("”[•i‘“ú"));
					line.add(rs.getInt("Á”ïÅ"));
					line.add(rs.getInt("”[•i‘”Ô†") != 0);
					line.add(rs.getBoolean("YFLG"));
					data.add(line);
				}

				ps = c.prepareStatement("SELECT İŒÉeID,’•¶Šú,’•¶”Ô†,’•¶}”Ô," +
					 " “`•[”Ô†,s.d“üæCD,’•¶”NŒ“ú,w’è”[Šú," +
					 " “E—v,”[“üæw’è," +
					 " CASE" +
						" WHEN í•ÊCD = 1 THEN '‡Š'+‰ïĞ–¼ + CASE WHEN x“X–¼ IS NULL THEN '' ELSE ' ' + x“X–¼ END" +
						" WHEN í•ÊCD = 2 THEN ‰ïĞ–¼+'‡Š' + CASE WHEN x“X–¼ IS NULL THEN '' ELSE ' ' + x“X–¼ END" +
						" WHEN í•ÊCD = 3 THEN '‡‹'+‰ïĞ–¼ + CASE WHEN x“X–¼ IS NULL THEN '' ELSE ' ' + x“X–¼ END" +
						" WHEN í•ÊCD = 4 THEN ‰ïĞ–¼+'‡‹' + CASE WHEN x“X–¼ IS NULL THEN '' ELSE ' ' + x“X–¼ END" +
						" ELSE ‰ïĞ–¼ END AS Ğ–¼" +
					 " FROM T_İŒÉ_e s" +
					 " LEFT OUTER JOIN M_–@l c" +
					 " ON s.d“üæCD=c.d“üæCD" +
					 " WHERE İŒÉeID=?");
				ps.setInt(1, id);
				rs = ps.executeQuery();
				while(rs.next()) {
					odDTO = new OrderDocumentDTO(
					 data,
					 rs.getString("Ğ–¼"),
					 rs.getString("’•¶}”Ô"),
					 rs.getString("“E—v"),
					 rs.getString("”[“üæw’è"),
					 rs.getInt("d“üæCD"),
					 rs.getInt("’•¶Šú"),
					 rs.getInt("’•¶”Ô†"),
					 rs.getInt("“`•[”Ô†"),
					 rs.getInt("İŒÉeID"),
					 rs.getDate("’•¶”NŒ“ú"),
					 rs.getDate("w’è”[Šú"),
					 null,null);
				}


			} catch(SQLException ex) {
				err.append(className + "DBƒGƒ‰[‚ª”­¶‚µ‚Ü‚µ‚½\n");
				Logging.logStackTrace(ex, lg, className);
			}
		}catch(Exception ex) {
			Logging.logStackTrace(ex, lg, className);
		}

		/**
		 * ƒNƒ‰ƒCƒAƒ“ƒg‚É‘—M
		 */
		try {
			response.setContentType("application/octet-stream");
			ObjectOutputStream out = new ObjectOutputStream(response.getOutputStream());
			out.writeObject(odDTO);
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
