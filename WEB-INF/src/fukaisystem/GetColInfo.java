package fukaisystem;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.Vector;

import javax.servlet.GenericServlet;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import org.apache.log4j.Logger;

import fukaisystem.sql.DBConnection;
import fukaisystem.util.Logging;



public class GetColInfo extends GenericServlet {
	private static final long serialVersionUID = 1L;
	private static Logger lg = Logger.getLogger("A1");
	private static final String className = "GetColInfo\n";

	public void service(ServletRequest request, ServletResponse response) {
		StringBuilder err = new StringBuilder("");

		DBConnection dbc = new DBConnection();
		Connection c = dbc.getConnection();

		Vector<String> v = new Vector<String>();

		try {

	//クライアントから読み込み
			ObjectInputStream in = new ObjectInputStream(request.getInputStream());
			String tableName = (String)in.readObject();
			in.close();

			try {
				Statement st = c.createStatement();
				ResultSet rs = st.executeQuery("SELECT * FROM " + tableName);
				ResultSetMetaData rsmd = rs.getMetaData();

				for(int i = 1; i <= rsmd.getColumnCount(); i++) {
					if (!rs.wasNull()) {
						switch(rsmd.getColumnType(i)) {
							case Types.CHAR:
							case Types.VARCHAR:
								v.add(rsmd.getColumnName(i) + " [" + rsmd.getColumnTypeName(i) +
								 "(" + String.valueOf(rsmd.getColumnDisplaySize(i)) + ")]");
								break;
							default:
								v.add(rsmd.getColumnName(i) + " [" + rsmd.getColumnTypeName(i) + "]");

						}
					}
				}
			} catch(SQLException ex) {
				Logging.logStackTrace(ex, lg, className);
			}
		}catch(Exception ex) {
			Logging.logStackTrace(ex, lg, className);
		}

		/**
		 * クライアントに送信
		 */
		try {
			response.setContentType("application/octet-stream");
			ObjectOutputStream out = new ObjectOutputStream(response.getOutputStream());
			out.writeObject(v);
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
		}
	}

}