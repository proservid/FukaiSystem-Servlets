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

import fukaisystem.sql.DBConnection;
import fukaisystem.util.Logging;


public class SetSlipFormat extends GenericServlet {

	/**
	 *
	 */
	private static final long serialVersionUID = 1L;
	static final Logger lg = Logger.getLogger("A1");
	private static final String className = "SetSlipFormat\n";

	@SuppressWarnings("unchecked")
	public void service(ServletRequest request, ServletResponse response) {

		DBConnection dbc = new DBConnection();
		Connection c = dbc.getConnection();
		PreparedStatement ps = null;
		ResultSet rs = null;
		Vector<Vector<String>> input = new Vector<Vector<String>>();
		String output = "";
		StringBuilder err = new StringBuilder("");

		try {

			/**
			 * クライアントデータ受け取り
			 */
			ObjectInputStream in = new ObjectInputStream(request.getInputStream());
			Object obj = in.readObject();
			in.close();

			if(obj == null) {
				err.append(className + "readObjectがnullです\n");
				lg.error(className + "readObjectがnullです");
			} else {
				if(obj instanceof Vector) {
					input = (Vector<Vector<String>>)obj;
				} else {
					err.append(className + "readObjectがString型ではありません\n");
					lg.error(className + "readObjectがString型ではありません");
				}
			}

			try {
				ps = c.prepareStatement(
						 "UPDATE T_伝票 SET フォーマット名=? WHERE 伝票名=?");
				for(Vector<String> v : input) {
					ps.setString(1, v.get(1));
					ps.setString(2, v.get(0));
					ps.addBatch();
				}
				int[] updateCounts = ps.executeBatch();
				output = updateCounts.length + "件更新されました。";
			} catch(SQLException ex) {
				err.append("テーブル「T_伝票」を更新できません\n");
				Logging.logStackTrace(ex, lg, className);
			}

		} catch(Exception ex) {
			Logging.logStackTrace(ex, lg, className);
		}

		/**
		 * クライアントに送信
		 */
		try {
			response.setContentType("application/octet-stream");
			ObjectOutputStream out = new ObjectOutputStream(response.getOutputStream());
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
