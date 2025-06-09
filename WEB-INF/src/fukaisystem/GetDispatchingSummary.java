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

import fukaisystem.dto.DispatchingDTO;
import fukaisystem.sql.DBConnection;
import fukaisystem.util.Logging;

public class GetDispatchingSummary extends GenericServlet {

	private static final long serialVersionUID = 1L;
	private static final Logger lg = Logger.getLogger("A1");
	private static final String className = "GetDispatchingSummary\n";

	@Override
	public void service(ServletRequest request, ServletResponse response) {
		DBConnection dbc = new DBConnection();
		Connection c = dbc.getConnection();
		PreparedStatement ps = null;
		ResultSet rs = null;
		DispatchingDTO dispDTO = null;
		StringBuilder err = new StringBuilder();

		int id = 0;

		Vector<Vector<Object>> data = new Vector<Vector<Object>>();

		try {

			/**
			 * クライアントデータ受け取り
			 */
			ObjectInputStream in = new ObjectInputStream(request.getInputStream());
			Object obj = in.readObject();
			in.close();

			if (obj == null) {
				err.append(className + "readObjectがnullです\n");
				lg.error(className + "readObjectがnullです");
			} else {
				if (obj instanceof Number) {
					id = ((Number) obj).intValue();
				} else {
					err.append(className + "readObjectがSetSummaryDTO型ではありません\n");
					lg.error(className + "readObjectがSetSummaryDTO型ではありません");
				}
			}
			try {
				ps = c.prepareStatement(
					"SELECT 大分類CD,中分類CD,小分類CD,品名,各FLG,数量,数量単位CD,重量長さ,"
						+ "単価,金額,備考,c.在庫親ID,在庫子ID,注文期,注文番号,注文枝番 FROM T_出庫_子 c"
						+ " LEFT OUTER JOIN ("
						+ "  SELECT 在庫親ID,注文期,注文番号,注文枝番 FROM T_在庫_親"
						+ "  UNION"
						+ "  SELECT 製作親ID,製作期,製作番号,製作枝番 FROM T_製作_親"
						+ " ) p ON c.在庫親ID=p.在庫親ID"
						+ " WHERE 出庫親ID=?"
				);
				ps.setInt(1, id);
				rs = ps.executeQuery();
				while (rs.next()) {
					Vector<Object> line = new Vector<Object>();
					line.add(rs.getInt("大分類CD"));
					line.add(rs.getInt("中分類CD"));
					line.add(rs.getInt("小分類CD"));
					line.add(rs.getString("品名"));
					line.add(rs.getBoolean("各FLG"));
					line.add(rs.getDouble("数量"));
					line.add(rs.getInt("数量単位CD"));
					line.add(rs.getDouble("重量長さ"));
					line.add(rs.getInt("単価"));
					line.add(rs.getInt("金額"));
					line.add(rs.getString("備考"));
					line.add(rs.getInt("在庫親ID"));
					line.add(rs.getInt("在庫子ID"));
					line.add(
						rs.getString("注文期") == null
							? ""
							: rs.getString("注文期") + "-" + rs.getString("注文番号") + " " + rs.getString("注文枝番")
					);
					data.add(line);
				}

				ps = c.prepareStatement("SELECT * FROM T_出庫_親 p WHERE 出庫親ID=?");
				ps.setInt(1, id);
				rs = ps.executeQuery();
				while (rs.next()) {
					dispDTO = new DispatchingDTO(
						rs.getInt("出庫親ID"),
						0,
						0,
						"",
						rs.getInt("製作期"),
						rs.getInt("製作番号"),
						rs.getString("製作枝番"),
						"",
						"",
						"",
						"",
						rs.getString("用途"),
						rs.getString("摘要"),
						data,
						rs.getDate("出庫年月日"),
						0,
						0,
						0,
						false
					);
				}
			} catch (SQLException ex) {
				err.append(className + "DBエラーが発生しました\n");
				Logging.logStackTrace(ex, lg, className);
			}
		} catch (Exception ex) {
			Logging.logStackTrace(ex, lg, className);
		}

		/**
		 * クライアントに送信
		 */
		try {
			response.setContentType("application/octet-stream");
			ObjectOutputStream out = new ObjectOutputStream(response.getOutputStream());
			out.writeObject(dispDTO);
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
