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

import fukaisystem.dto.InitialDTO;
import fukaisystem.sql.DBConnection;
import fukaisystem.util.Logging;

public class UpdateCandidate extends GenericServlet {

	/**
	 *
	 */
	private static final long serialVersionUID = 1L;
    private static final Logger lg = Logger.getLogger("A1");
	private static final String className = "UpdateCandidate\n";

	public void service(ServletRequest request, ServletResponse response) {

		DBConnection dbc = new DBConnection();
		Connection c = dbc.getConnection();
		PreparedStatement ps = null;
		ResultSet rs = null;
		StringBuilder err = new StringBuilder("");

		Vector<String> deadlines = new Vector<String>();
		Vector<String> places = new Vector<String>();
		Vector<String> terms = new Vector<String>();
		Vector<String> validities = new Vector<String>();

		try {

			/**
			 * クライアントデータ受け取り
			 */
			ObjectInputStream in = new ObjectInputStream(request.getInputStream());
			in.close();
			try {
				ps = c.prepareStatement("SELECT * FROM M_納期");
				rs = ps.executeQuery();
				while(rs.next()) {
					deadlines.add(rs.getString("納期"));
				}
				ps = c.prepareStatement("SELECT * FROM M_受渡場所");
				rs = ps.executeQuery();
				while(rs.next()) {
					places.add(rs.getString("受渡場所"));
				}
				ps = c.prepareStatement("SELECT * FROM M_取引条件");
				rs = ps.executeQuery();
				while(rs.next()) {
					terms.add(rs.getString("取引条件"));
				}
				ps = c.prepareStatement("SELECT * FROM M_有効期間");
				rs = ps.executeQuery();
				while(rs.next()) {
					validities.add(rs.getString("有効期間"));
				}

			} catch(SQLException ex) {
				err.append(ex.getMessage());
				err.append("ErrorCode："+ex.getErrorCode());
				err.append("SQLState："+ex.getSQLState());
				Logging.logStackTrace(ex, lg, className);
			}


			/**
			 * クライアントに送信
			 */

			InitialDTO id = new InitialDTO(null, null, null, deadlines, places, terms,
				validities, null, null, null, null, null, null, null, null, null,
				null, null, null, null, null, null);

			response.setContentType("application/octet-stream");
			ObjectOutputStream out = new ObjectOutputStream(response.getOutputStream());
			out.writeObject(id);
			out.writeUTF(err.toString());
			out.flush();
			out.close();

		} catch(Exception ex) {
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
