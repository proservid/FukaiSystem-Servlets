package fukaisystem;

import java.io.ObjectOutputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import javax.servlet.GenericServlet;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import org.apache.log4j.Logger;

import fukaisystem.sql.DBConnection;

public class TbInit extends GenericServlet {

	private static final Logger lg = Logger.getLogger("A1");
	private static final String className = "TbInit\n";

	public void service(ServletRequest request, ServletResponse response) {

		DBConnection dbc = new DBConnection();
		Connection c = dbc.getConnection();
		PreparedStatement ps = null;
		ResultSet rs = null;
		
		StringBuilder err = new StringBuilder();

		Map<String, String> validMembers = new LinkedHashMap<String, String>();
		Map<String, String> allMembers = new LinkedHashMap<String, String>();
		Map<String, String> validWorks = new LinkedHashMap<String, String>();
		Map<String, String> allWorks = new LinkedHashMap<String, String>();
		List<Map<String, String>> list = new ArrayList<Map<String, String>>();

		try {
			ps = c.prepareStatement("select distinct íSìñé“CD,ê©+ñº as éÅñº from T_â¡çHé¿ê— h" +
					" left outer join M_êlàı m on h.íSìñé“CD=m.CD" +
					" where ç›ê–FLG='true'" +
					" order by íSìñé“CD");
			rs = ps.executeQuery();
			while(rs.next()) {
				validMembers.put(rs.getString("íSìñé“CD"), rs.getString("éÅñº"));
			}
			list.add(validMembers);
			ps = c.prepareStatement("select distinct íSìñé“CD,ê©+ñº as éÅñº from T_â¡çHé¿ê— h" +
					" left outer join M_êlàı m on h.íSìñé“CD=m.CD" +
					" order by íSìñé“CD");
			rs = ps.executeQuery();
			while(rs.next()) {
				allMembers.put(rs.getString("íSìñé“CD"), rs.getString("éÅñº"));
			}
			list.add(allMembers);
			ps = c.prepareStatement("select distinct â¡çHCD,è¨ï™óﬁñº from T_â¡çHé¿ê— h" +
					" left outer join M_â¡çH_éq w on h.â¡çHCD=w.CD" +
					" where égópFLG='true'" +
					" order by â¡çHCD");
			rs = ps.executeQuery();
			while(rs.next()) {
				validWorks.put(rs.getString("â¡çHCD"), rs.getString("è¨ï™óﬁñº"));
			}
			list.add(validWorks);
			ps = c.prepareStatement("select distinct â¡çHCD,è¨ï™óﬁñº from T_â¡çHé¿ê— h" +
					" left outer join M_â¡çH_éq w on h.â¡çHCD=w.CD" +
					" order by â¡çHCD");
			rs = ps.executeQuery();
			while(rs.next()) {
				allWorks.put(rs.getString("â¡çHCD"), rs.getString("è¨ï™óﬁñº"));
			}
			list.add(allWorks);
		} catch(SQLException ex) {
			ex.printStackTrace();
			err.append(ex);
		}

		/**
		 * ÉNÉâÉCÉAÉìÉgÇ…ëóêM
		 */
		try {
			response.setContentType("application/octet-stream");
			ObjectOutputStream out = new ObjectOutputStream(response.getOutputStream());
			out.writeObject(list);
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
