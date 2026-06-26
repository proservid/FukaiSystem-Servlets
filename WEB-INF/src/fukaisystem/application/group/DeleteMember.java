package fukaisystem.application.group;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import javax.servlet.GenericServlet;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import org.apache.log4j.Logger;

import fukaisystem.sql.DBConnection;
import fukaisystem.util.Logging;

public class DeleteMember extends GenericServlet {

	private static final long serialVersionUID = 1L;
	private static final Logger lg = Logger.getLogger("A1");
	private static final String className = "DeleteMember\n";

	@SuppressWarnings("unchecked")
	@Override
	public void service(ServletRequest request, ServletResponse response) {
		DBConnection dbc = new DBConnection();
		Connection c = dbc.getConnection();
		PreparedStatement ps = null;
		ResultSet rs = null;
		StringBuilder err = new StringBuilder();
		List<Integer> delMembers = null;
		int[] updates = null;

		try {

			/**
			 * クライアントデータ受け取り
			 */
			ObjectInputStream in = new ObjectInputStream(request.getInputStream());
			Object obj = in.readObject();
			in.close();

			if (obj != null) {
				if (obj instanceof List<?>) {
					delMembers = (List<Integer>) obj;
				} else {
					// err.append(className + "readObjectがInteger[]型ではありません\n");
					// lg.error(className + "readObjectがInteger[]型ではありません");
				}
			}
		} catch (Exception ex) {
			Logging.logStackTrace(ex, lg, className);
		}
		try {
			ps = c.prepareStatement("delete from M_人員 where CD=?");
			for (int m : delMembers) {
				ps.setInt(1, m);
				ps.addBatch();
			}
			updates = ps.executeBatch();

		} catch (SQLException ex) {
			err.append(ex.toString());
			Logging.logStackTrace(ex, lg, className);
		}

		/**
		 * クライアントに送信
		 */
		try {
			response.setContentType("application/octet-stream");
			ObjectOutputStream out = new ObjectOutputStream(response.getOutputStream());
			out.writeObject(updates.length);
			out.writeUTF(err.toString());
			out.flush();
			out.close();
		} catch (Exception ex) {
			Logging.logStackTrace(ex, lg, className);
		} finally {
			try {
				if (c != null && !c.isClosed())
					c.close();
			} catch (SQLException ex) {
				Logging.logStackTrace(ex, lg, className);
			}
			// The following processes requires JDBC4.0.
			try {
				if (ps != null && !ps.isClosed()) {
					ps.close();
					lg.debug(className + "ps is closed by jdbc4.0");
				}
			} catch (SQLException ex) {
				Logging.logStackTrace(ex, lg, className);
			}
			try {
				if (rs != null && !rs.isClosed()) {
					rs.close();
					lg.debug(className + "rs is closed by jdbc4.0");
				}
			} catch (SQLException ex) {
				Logging.logStackTrace(ex, lg, className);
			}
		}
	}

}
