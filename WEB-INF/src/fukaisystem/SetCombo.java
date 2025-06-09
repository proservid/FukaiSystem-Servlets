package fukaisystem;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.servlet.GenericServlet;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import org.apache.log4j.Logger;

import fukaisystem.dto.ChartDTO;
import fukaisystem.dto.ProductNumber;
import fukaisystem.sql.DBConnection;
import fukaisystem.util.Logging;

public class SetCombo extends GenericServlet {

	/**
	 *
	 */
	private static final long serialVersionUID = 1L;
	private static final Logger lg = Logger.getLogger("A1");
	private static final String className = "SetCombo\n";

	public void service(ServletRequest request, ServletResponse response) {

		DBConnection dbc = new DBConnection();
		Connection c = dbc.getConnection();
		PreparedStatement ps = null;
		ResultSet rs = null;
		StringBuilder err = new StringBuilder();

		int code = 0;
		String name = "";
		String display = "";

		Map<Integer, String> contacts = new LinkedHashMap<Integer, String>();
		Map<Integer, String> models = new LinkedHashMap<Integer, String>();
		Map<Integer, ProductNumber> numbers = new HashMap<Integer, ProductNumber>();

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
				if (obj instanceof Integer) {
					code = (Integer) obj;
				} else {
					err.append(className + "readObjectがString型ではありません\n");
					lg.error(className + "readObjectがString型ではありません");
				}
			}

			try {
				ps = c.prepareStatement(
					"SELECT"
						+ " CASE"
						+ " WHEN 種別CD = 1 THEN '㈱'+会社名 + CASE WHEN 支店名 IS NULL THEN '' ELSE ' ' + 支店名 END"
						+ " WHEN 種別CD = 2 THEN 会社名+'㈱' + CASE WHEN 支店名 IS NULL THEN '' ELSE ' ' + 支店名 END"
						+ " WHEN 種別CD = 3 THEN '㈲'+会社名 + CASE WHEN 支店名 IS NULL THEN '' ELSE ' ' + 支店名 END"
						+ " WHEN 種別CD = 4 THEN 会社名+'㈲' + CASE WHEN 支店名 IS NULL THEN '' ELSE ' ' + 支店名 END"
						+ " ELSE 会社名 END,"
						+ " 表示名"
						+ " FROM  M_法人 c"
						+ " WHERE 得意先CD=?"
				);
				ps.setInt(1, code);
				rs = ps.executeQuery();
				if (rs.next()) {
					name = rs.getString(1);
					display = rs.getString(2);
				}
				ps = c.prepareStatement(
					"SELECT 機械番号,案件名,製作期,製作番号,製作枝番 FROM T_製作_親"
						+ " WHERE 得意先CD=? AND 機械番号>0 ORDER BY 製作期,製作番号"
				);
				ps.setInt(1, code);
				rs = ps.executeQuery();
				while (rs.next()) {
					models.put(rs.getInt("機械番号"), rs.getString("案件名"));
					numbers.put(
						rs.getInt("機械番号"),
						new ProductNumber(rs.getInt("製作期"), rs.getInt("製作番号"), rs.getString("製作枝番"))
					);
				}

				ps = c.prepareStatement(
					"SELECT p.CD,氏名 FROM M_個人 p"
						+ " LEFT OUTER JOIN M_法人 c"
						+ " ON p.法人CD=c.CD"
						+ " WHERE 得意先CD=? ORDER BY p.CD"
				);
				ps.setInt(1, code);
				rs = ps.executeQuery();
				while (rs.next()) {
					contacts.put(rs.getInt("CD"), rs.getString("氏名"));
				}
			} catch (SQLException ex) {
				ex.printStackTrace();
				Logging.logStackTrace(ex, lg, className);
			}

			/**
			 * クライアントに送信
			 */

			ChartDTO cd = new ChartDTO(name, display, contacts, models, numbers);
			response.setContentType("application/octet-stream");
			ObjectOutputStream out = new ObjectOutputStream(response.getOutputStream());
			out.writeObject(cd);
			out.writeUTF("acitve:" + dbc.getNumActive() + " idle:" + dbc.getNumIdle());
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
					lg.debug(className + "ps closed by jdbc4.0");
				}
			} catch (SQLException ex) {
				Logging.logStackTrace(ex, lg, className);
			}
			try {
				if (rs != null && !rs.isClosed()) {
					rs.close();
					lg.debug(className + "rs closed by jdbc4.0");
				}
			} catch (SQLException ex) {
				Logging.logStackTrace(ex, lg, className);
			}
		}
	}

}
