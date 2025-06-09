package fukaisystem;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Vector;

import javax.servlet.GenericServlet;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import org.apache.log4j.Logger;

import fukaisystem.sql.DBConnection;
import fukaisystem.util.Logging;

public class GetEstData extends GenericServlet {

	private static final long serialVersionUID = 1L;
	private static final Logger lg = Logger.getLogger("A1");
	private static final String className = "GetEstData\n";

	@SuppressWarnings("unchecked")
	@Override
	public void service(ServletRequest request, ServletResponse response) {
		DBConnection dbc = new DBConnection();
		Connection c = dbc.getConnection();
		PreparedStatement ps = null;
		ResultSet rs = null;
		StringBuilder err = new StringBuilder();
		List<String> nums = null;
		Vector<Vector<Object>> v = new Vector<Vector<Object>>();

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
				if (obj instanceof List<?>) {
					nums = (List<String>) obj;
				} else {
					err.append(className + "readObjectがProjectSearchDTO型ではありません\n");
					lg.error(className + "readObjectがProjectSearchDTO型ではありません");
				}
			}
			try {
				StringBuilder query = new StringBuilder(
					"select * from T_見積_子 where 見積親ID IN (select 見積親ID from ("
						+ "select 見積親ID,convert(varchar,見積期)+'-'+right('000' + convert(varchar, 見積番号), 3)+見積枝番 as 見積番 from T_見積_親) a"
						+ " where "
				);
				boolean isFirst = true;
				for (String s : nums) {
					if (isFirst) {
						query.append("見積番 like '" + s + "'");
						isFirst = false;
					} else {
						query.append(" OR 見積番 like '" + s + "'");
					}
				}
				query.append(") order by 見積親ID,ID");
				ps = c.prepareStatement(query.toString());
				rs = ps.executeQuery();
				while (rs.next()) {
					Vector<Object> line = new Vector<Object>();
					line.add(rs.getInt("ID"));
					line.add(rs.getInt("表示CD"));
					line.add(rs.getString("名称"));
					line.add(rs.getBoolean("各FLG"));
					line.add(rs.getInt("数量"));
					line.add(rs.getInt("数量単位CD"));
					line.add(rs.getInt("単価"));
					line.add(rs.getInt("提示額"));
					line.add(rs.getString("図番"));
					line.add(rs.getString("備考"));
					v.add(line);
				}

			} catch (SQLException ex) {
				err.append(className + "テーブル「T_在庫_親」の読み出しに失敗しました\n");
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
			out.writeObject(v);
			out.writeUTF(err.toString());
			out.flush();
			out.close();
		} catch (Exception ex) {
			lg.error(ex);
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

	public String partialDateStr(String target, String ymd, int begin, int count) {
		switch (begin) {
			case 1:
			return "substring(convert(varchar(" + count + "), " + target + ", 120), " + begin + ", " + count + ")='" + ymd + "'";
			case 6:
			return "substring(convert(varchar(" + (count + 5) + "), " + target + ", 120), " + begin + ", " + count + ")='" + ymd + "'";
			default:
			return "substring(convert(varchar(10), " + target + ", 120), " + begin + ", " + count + ")='" + ymd + "'";
		}
	}
}
