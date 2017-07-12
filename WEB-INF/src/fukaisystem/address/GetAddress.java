package fukaisystem.address;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

import javax.servlet.GenericServlet;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import org.apache.log4j.Logger;

import fukaisystem.dto.CorpDTO;
import fukaisystem.sql.DBConnection;
import fukaisystem.util.Logging;


public class GetAddress extends GenericServlet {

	/**
	 *
	 */
	private static final long serialVersionUID = 1L;
	static final Logger lg = Logger.getLogger("A1");
	private static final String className = "GetZip\n";

	public void service(ServletRequest request, ServletResponse response) {

		DBConnection dbc = new DBConnection();
		Connection c = dbc.getConnection();
		PreparedStatement ps = null;
		ResultSet rs = null;
		String input = "";
		Vector<Vector<String>> output = new Vector<Vector<String>>();
		StringBuilder err = new StringBuilder("");

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
				if(obj instanceof String) {
					input = (String)obj;
				} else {
					err.append(className + "readObject‚ªStringŒ^‚Å‚Í‚ ‚è‚Ü‚¹‚ñ\n");
					lg.error(className + "readObject‚ªStringŒ^‚Å‚Í‚ ‚è‚Ü‚¹‚ñ");
				}
			}
			try {
				ps = c.prepareStatement(
					"select “s“¹•{Œ§,s‹æ’¬‘º," +
					"case when ’¬ˆæ is null then ’¬ˆæ•â‘« else ’¬ˆæ end as ’¬ˆæ," +
					"case when ‹“s’Ê‚è–¼ is null then '' else ‹“s’Ê‚è–¼ end as ‹“s’Ê‚è–¼," +
					"case when š’š–Ú is null then '' else š’š–Ú end as š’š–Ú," +
					"case when •â‘« is null then '' else •â‘« end as •â‘«," +
					"case when –‹ÆŠ–¼ is null then '' else –‹ÆŠ–¼ end as –‹ÆŠ–¼," +
					"case when –‹ÆŠZŠ is null then '' else –‹ÆŠZŠ end as –‹ÆŠZŠ," +
					"—X•Ö}”Ô" +
					" from V_—X•Ö”Ô† pc" +
					" left outer join M_“s“¹•{Œ§ p on pc.“s“¹•{Œ§CD=p.CD" +
					" left outer join M_s‹æ’¬‘º c on pc.“s“¹•{Œ§CD=c.“s“¹•{Œ§CD and pc.s‹æ’¬‘ºCD=c.CD" +
					(input.length() == 7 ? " where —X•Ö”Ô†=?" : " where —X•Ö”Ô†+—X•Ö}”Ô=?"));
				System.out.println("inputzip"+input);
				ps.setString(1, input);
				rs = ps.executeQuery();
				while(rs.next()) {
					Vector<String> v = new Vector<String>();
					v.add(rs.getString("“s“¹•{Œ§"));
					v.add(rs.getString("s‹æ’¬‘º"));
					v.add(rs.getString("’¬ˆæ") + rs.getString("‹“s’Ê‚è–¼"));
					v.add(rs.getString("š’š–Ú") + rs.getString("•â‘«"));
					v.add(rs.getString("–‹ÆŠ–¼"));
					v.add(rs.getString("–‹ÆŠZŠ"));
					v.add(rs.getString("—X•Ö}”Ô"));
					output.add(v);System.out.println("v:"+v);
				}
				if(output.size() == 0) {
					Vector<String> v = new Vector<String>();
					for(int i = 0; i < 7; i++) {
						v.add("");
					}
					output.add(v);
				}
			} catch(SQLException ex) {
				err.append("ƒe[ƒuƒ‹uT_ƒe[ƒuƒ‹–¼v‚Ì“Ç‚É¸”s‚µ‚Ü‚µ‚½\n");
				Logging.logStackTrace(ex, lg, className);
			}

		} catch(Exception ex) {
			Logging.logStackTrace(ex, lg, className);
		}

		/**
		 * ƒNƒ‰ƒCƒAƒ“ƒg‚É‘—M
		 */
		try {
			response.setContentType("application/octet-stream");
			ObjectOutputStream out = new ObjectOutputStream(response.getOutputStream());
			System.out.println("output:"+output);
			out.writeObject(output);
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
