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

public class DeleteSlip extends GenericServlet {

	private static final long serialVersionUID = 1L;
	private static final Logger lg = Logger.getLogger("A1");
	private static final String className = "DeleteSlip\n";

	@Override
	public void service(ServletRequest request, ServletResponse response) {
		DBConnection dbc = new DBConnection();
		Connection c = dbc.getConnection();
		PreparedStatement ps = null;
		ResultSet rs = null;
		StringBuilder err = new StringBuilder();

		String query1 = "", query2 = "";
		int type = 0, id = 0;

		try {

			/**
			 * クライアントデータ受け取り
			 */
			ObjectInputStream in = new ObjectInputStream(request.getInputStream());
			Object obj = in.readObject();
			in.close();

			if (obj == null) {
				id = 0;
			} else {
				if (obj instanceof Integer[]) {
					Integer[] param = (Integer[]) obj;
					type = param[0];
					id = param[1];
					switch (type) {
						case 1:
							query1 = "DELETE FROM T_見積_親 WHERE 見積親ID=?";
							query2 = "DELETE FROM T_見積_子 WHERE 見積親ID=?";
							break;
						case 2:
							query1 = "DELETE FROM T_製作_親 WHERE 製作親ID=?";
							query2 = "DELETE FROM T_製作_子 WHERE 製作親ID=?";
							break;
						case 3:
							query1 = "DELETE FROM T_在庫_親 WHERE 在庫親ID=?";
							query2 = "DELETE FROM T_在庫_子 WHERE 在庫親ID=?";
							break;
						case 4:
							query1 = "DELETE FROM T_売上_親 WHERE 売上親ID=?";
							query2 = "DELETE FROM T_売上_子 WHERE 売上親ID=?";
							break;
						case 5:
							query1 = "DELETE FROM T_出庫_親 WHERE 出庫親ID=?";
							query2 = "DELETE FROM T_出庫_子 WHERE 出庫親ID=?";
							break;
					}
				} else {
					err.append(className + "readObjectがInteger型ではありません\n");
					lg.error(className + "readObjectがInteger型ではありません");
				}
			}
		} catch (Exception ex) {
			Logging.logStackTrace(ex, lg, className);
		}

		if (type > 0) {
			try {
				ps = c.prepareStatement(query1);
				ps.setInt(1, id);
				ps.executeUpdate();

				// 明細
				ps = c.prepareStatement(query2);
				ps.setInt(1, id);
				ps.executeUpdate();

			} catch (SQLException ex) {
				err.append(ex.toString());
				Logging.logStackTrace(ex, lg, className);
			}
		}

		/**
		 * クライアントに送信
		 */
		try {
			response.setContentType("application/octet-stream");
			ObjectOutputStream out = new ObjectOutputStream(response.getOutputStream());
			out.writeObject(null);
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
