package fukaisystem.application.mh;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import javax.servlet.GenericServlet;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import org.apache.log4j.Logger;

import fukaisystem.dto.InitialInputDTO;
import fukaisystem.sql.DBConnection;
import fukaisystem.util.Logging;

public class InitInputSystem extends GenericServlet {
	/**
	 *
	 */
	private static final long serialVersionUID = 1L;
	private static final Logger lg = Logger.getLogger("A1");
	private static final String className = "InitInputSystem\n";

	public void service(ServletRequest request, ServletResponse response) {

		DBConnection dbc = new DBConnection();
		Connection c = dbc.getConnection();
		PreparedStatement ps = null;
		ResultSet rs = null;

		TreeMap<String, String> dept = new TreeMap<String, String>();
		Map<String, String> process = new LinkedHashMap<String, String>();
		Map<List<String>, Map<String, String>> name = new LinkedHashMap<List<String>, Map<String, String>>();
		int period = 0;

		try {

			/**
			 * クライアントデータ受け取り
			 */
			ObjectInputStream in = new ObjectInputStream(request.getInputStream());
			in.close();

			try {
				ps = c.prepareStatement("SELECT RIGHT('00' + CONVERT(varchar, CD), 2) AS 部署CD,部署名 FROM M_部署");
				rs = ps.executeQuery();
				while (rs.next()) {
					dept.put(rs.getString("部署CD"), rs.getString("部署名"));
				}

				ps = c.prepareStatement(
					"SELECT CASE WHEN 所属部署CD IN(2,3,4) THEN RIGHT('00' + CONVERT(varchar, CD), 2) ELSE CONVERT(varchar, CD) END AS 個人CD,姓+' '+名 AS 氏名,"
						+ "RIGHT('00' + CONVERT(varchar, 所属部署CD), 2) AS 部署CD"
						+ " FROM M_人員 WHERE 在籍FLG='true' ORDER BY 表示CD"
				);
				rs = ps.executeQuery();
				while (rs.next()) {
					List<String> subKey = new ArrayList<String>();
					subKey.add(rs.getString("部署CD"));
					if (!name.containsKey(subKey)) {
						name.put(subKey, new LinkedHashMap<String, String>());
					}
					name.get(subKey).put(rs.getString("個人CD"), rs.getString("氏名"));
				}

				ps = c.prepareStatement(
					"SELECT RIGHT('00' + CONVERT(varchar, CD), 2) AS 小分類CD,小分類名 FROM M_加工_子"
						+ " WHERE 使用FLG='true' AND CD<200 ORDER BY 大分類CD,中分類CD"
				);
				rs = ps.executeQuery();
				while (rs.next()) {
					process.put(rs.getString("小分類CD"), rs.getString("小分類名"));
				}

				ps = c.prepareStatement("SELECT MAX(期) AS 当期 FROM M_期 WHERE 自 < ?");
				ps.setDate(1, new java.sql.Date(new java.util.Date().getTime()));
				rs = ps.executeQuery();
				if (rs.next()) {
					period = rs.getInt("当期");
				}

			} catch (SQLException ex) {
				Logging.logStackTrace(ex, lg, className);
			}

			/**
			 * クライアントに送信
			 */

			InitialInputDTO iid = new InitialInputDTO(dept, name, process, period);

			response.setContentType("application/octet-stream");
			ObjectOutputStream out = new ObjectOutputStream(response.getOutputStream());
			out.writeObject(iid);
			out.writeUTF("");
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
