package fukaisystem.address;

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
			 * クライアントデータ受け取り
			 */
			ObjectInputStream in = new ObjectInputStream(request.getInputStream());
			Object obj = in.readObject();
			in.close();

			if (obj == null) {
				err.append(className + "readObjectがnullです\n");
				lg.error(className + "readObjectがnullです");
			} else {
				if (obj instanceof String) {
					input = (String) obj;
				} else {
					err.append(className + "readObjectがString型ではありません\n");
					lg.error(className + "readObjectがString型ではありません");
				}
			}
			try {
				ps = c.prepareStatement(
					"select 都道府県,市区町村,"
						+ "case when 町域 is null then 町域補足 else 町域 end as 町域,"
						+ "case when 京都通り名 is null then '' else 京都通り名 end as 京都通り名,"
						+ "case when 字丁目 is null then '' else 字丁目 end as 字丁目,"
						+ "case when 補足 is null then '' else 補足 end as 補足,"
						+ "case when 事業所名 is null then '' else 事業所名 end as 事業所名,"
						+ "case when 事業所住所 is null then '' else 事業所住所 end as 事業所住所,"
						+ "郵便枝番"
						+ " from V_郵便番号 pc"
						+ " left outer join M_都道府県 p on pc.都道府県CD=p.CD"
						+ " left outer join M_市区町村 c on pc.都道府県CD=c.都道府県CD and pc.市区町村CD=c.CD" +
						(input.length() == 7 ? " where 郵便番号=?" : " where 郵便番号+郵便枝番=?")
				);
				ps.setString(1, input);
				rs = ps.executeQuery();
				while (rs.next()) {
					Vector<String> v = new Vector<String>();
					v.add(rs.getString("都道府県"));
					v.add(rs.getString("市区町村"));
					v.add(rs.getString("町域") + rs.getString("京都通り名"));
					v.add(rs.getString("字丁目") + rs.getString("補足"));
					v.add(rs.getString("事業所名"));
					v.add(rs.getString("事業所住所"));
					v.add(rs.getString("郵便枝番"));
					output.add(v);
				}
				if (output.size() == 0) {
					Vector<String> v = new Vector<String>();
					for (int i = 0; i < 7; i++) {
						v.add("");
					}
					output.add(v);
				}
			} catch (SQLException ex) {
				err.append("テーブル「T_テーブル名」の読込に失敗しました\n");
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
			out.writeObject(output);
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
