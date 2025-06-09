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

import fukaisystem.dto.ProductNumberDTO;
import fukaisystem.sql.DBConnection;
import fukaisystem.util.Logging;

public class CopyData extends GenericServlet {

	private static final long serialVersionUID = 1L;
	private static final Logger lg = Logger.getLogger("A1");
	private static final String className = "CopyData\n";

	@Override
	public void service(ServletRequest request, ServletResponse response) {
		DBConnection dbc = new DBConnection();
		Connection c = dbc.getConnection();
		PreparedStatement ps = null;
		ResultSet rs = null;
		StringBuilder err = new StringBuilder();
		int period = 0;
		int number = 0;
		String branch = "";

		Vector<Vector<Object>> deliveryData = new Vector<Vector<Object>>();

		try {

			/**
			 * クライアントデータ受け取り
			 */
			ObjectInputStream in = new ObjectInputStream(request.getInputStream());
			Object obj = in.readObject();
			in.close();

			if (obj == null) {
			} else {
				if (obj instanceof ProductNumberDTO) {
					period = ((ProductNumberDTO) obj).getPeriod();
					number = ((ProductNumberDTO) obj).getNumber();
					branch = ((ProductNumberDTO) obj).getBranch();
				} else {
					err.append(className + "readObjectがIDDTO型ではありません\n");
					lg.error(className + "readObjectがIDDTO型ではありません");
				}
			}
		} catch (Exception ex) {
			Logging.logStackTrace(ex, lg, className);
		}

		try {

			// 製作明細
			ps = c.prepareStatement(
				"SELECT 注文月日,注文番号,表示CD,名称,各FLG,数量,数量単位CD,単価,金額,図番,備考,完成年月日,納品年月日"
					+ " FROM T_製作_子 c"
					+ " LEFT OUTER JOIN T_製作_親 p ON c.製作親ID=p.製作親ID WHERE 製作期=? AND 製作番号=? AND 製作枝番=?"
			);
			ps.setInt(1, period);
			ps.setInt(2, number);
			ps.setString(3, branch);
			rs = ps.executeQuery();
			while (rs.next()) {
				Vector<Object> line = new Vector<Object>();
				line.add(rs.getInt("表示CD"));
				line.add("");
				line.add(rs.getDate("注文月日"));
				line.add(rs.getString("注文番号"));
				line.add(rs.getString("名称"));
				line.add(rs.getBoolean("各FLG"));
				line.add(rs.getInt("数量"));
				line.add(rs.getInt("数量単位CD"));
				line.add(rs.getInt("単価"));
				line.add(rs.getInt("金額"));
				line.add(0);
				line.add("");
				line.add(rs.getDate("完成年月日"));
				line.add(rs.getDate("納品年月日"));
				deliveryData.add(line);
			}
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
			out.writeObject(deliveryData);
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
