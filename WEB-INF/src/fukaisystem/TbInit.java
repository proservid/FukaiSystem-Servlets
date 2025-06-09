package fukaisystem;

import java.io.ObjectOutputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.GenericServlet;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import org.apache.log4j.Logger;

import fukaisystem.sql.DBConnection;

public class TbInit extends GenericServlet {

	private static final Logger lg = Logger.getLogger("A1");
	private static final String className = "TbInit\n";

	public void service(ServletRequest request, ServletResponse response) {

		DBConnection dbc = new DBConnection();
		Connection c = dbc.getConnection();
		PreparedStatement ps = null;
		ResultSet rs = null;

		StringBuilder err = new StringBuilder();

		Map<String, String> validMembers = new LinkedHashMap<String, String>();
		Map<String, String> allMembers = new LinkedHashMap<String, String>();
		Map<String, String> validWorks = new LinkedHashMap<String, String>();
		Map<String, String> allWorks = new LinkedHashMap<String, String>();
		List<Map<String, String>> list = new ArrayList<Map<String, String>>();

		try {
			ps = c.prepareStatement(
				"select distinct 担当者CD,姓+名 as 氏名 from T_加工実績 h"
					+ " left outer join M_人員 m on h.担当者CD=m.CD"
					+ " where 在籍FLG='true'"
					+ " order by 担当者CD"
			);
			rs = ps.executeQuery();
			while (rs.next()) {
				validMembers.put(rs.getString("担当者CD"), rs.getString("氏名"));
			}
			list.add(validMembers);
			ps = c.prepareStatement(
				"select distinct 担当者CD,姓+名 as 氏名 from T_加工実績 h"
					+ " left outer join M_人員 m on h.担当者CD=m.CD"
					+ " order by 担当者CD"
			);
			rs = ps.executeQuery();
			while (rs.next()) {
				allMembers.put(rs.getString("担当者CD"), rs.getString("氏名"));
			}
			list.add(allMembers);
			ps = c.prepareStatement(
				"select distinct 加工CD,小分類名 from T_加工実績 h"
					+ " left outer join M_加工_子 w on h.加工CD=w.CD"
					+ " where 使用FLG='true'"
					+ " order by 加工CD"
			);
			rs = ps.executeQuery();
			while (rs.next()) {
				validWorks.put(rs.getString("加工CD"), rs.getString("小分類名"));
			}
			list.add(validWorks);
			ps = c.prepareStatement(
				"select distinct 加工CD,小分類名 from T_加工実績 h"
					+ " left outer join M_加工_子 w on h.加工CD=w.CD"
					+ " order by 加工CD"
			);
			rs = ps.executeQuery();
			while (rs.next()) {
				allWorks.put(rs.getString("加工CD"), rs.getString("小分類名"));
			}
			list.add(allWorks);
		} catch (SQLException ex) {
			ex.printStackTrace();
			err.append(ex);
		}

		/**
		 * クライアントに送信
		 */
		try {
			response.setContentType("application/octet-stream");
			ObjectOutputStream out = new ObjectOutputStream(response.getOutputStream());
			out.writeObject(list);
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
