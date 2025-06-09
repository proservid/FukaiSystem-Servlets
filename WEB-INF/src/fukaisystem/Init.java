package fukaisystem;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import javax.servlet.GenericServlet;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import org.apache.log4j.Logger;

import fukaisystem.dto.InitialDTO;
import fukaisystem.sql.DBConnection;
import fukaisystem.util.Logging;

public class Init extends GenericServlet {

	/**
	 *
	 */
	private static final long serialVersionUID = 1L;
	private static final Logger lg = Logger.getLogger("A1");
	private static final String className = "Init\n";

	public void service(ServletRequest request, ServletResponse response) {

		DBConnection dbc = new DBConnection();
		Connection c = dbc.getConnection();
		PreparedStatement ps = null;
		ResultSet rs = null;
		StringBuilder err = new StringBuilder("");

		List<String> slips = new ArrayList<String>();
		Map<Integer, String> types = new LinkedHashMap<Integer, String>();
		Map<Integer, String> routes = new HashMap<Integer, String>();
		Vector<String> deadlines = new Vector<String>();
		Vector<String> places = new Vector<String>();
		Vector<String> terms = new Vector<String>();
		Vector<String> validities = new Vector<String>();
		Map<Integer, String> submits = new HashMap<Integer, String>();
		Map<Integer, String> currencies = new HashMap<Integer, String>();
		Map<Integer, String> states = new HashMap<Integer, String>();
		Map<Integer, String> ways = new HashMap<Integer, String>();
		Map<Integer, String> partials = new HashMap<Integer, String>();
		Map<Integer, String> indication = new HashMap<Integer, String>();
		Map<Integer, String> units = new HashMap<Integer, String>();
		Map<Integer, String> suppliers = new HashMap<Integer, String>();
		// Map<Integer, Integer> works = new HashMap<Integer, Integer>();
		Map<Integer, String> material = new LinkedHashMap<Integer, String>();
		Map<Integer, String> categoryL = new LinkedHashMap<Integer, String>();
		Map<List<Integer>, Map<Integer, String>> categoryM = new HashMap<List<Integer>, Map<Integer, String>>();
		Map<List<Integer>, Map<Integer, String>> categoryS = new HashMap<List<Integer>, Map<Integer, String>>();
		Map<Integer, Map<Integer, Map<Integer, Integer>>> prices = new HashMap<Integer, Map<Integer, Map<Integer, Integer>>>();
		Map<Integer, Double> sg = new HashMap<Integer, Double>();

		try {

			/**
			 * クライアントデータ受け取り
			 */
			ObjectInputStream in = new ObjectInputStream(request.getInputStream());
			in.close();
			try {
				ps = c.prepareStatement("SELECT フォーマット名 FROM T_伝票フォーマット ORDER BY フォーマット名");
				rs = ps.executeQuery();
				while (rs.next()) {
					slips.add(rs.getString("フォーマット名"));
				}

				ps = c.prepareStatement("SELECT * FROM M_製品種別");
				rs = ps.executeQuery();
				while (rs.next()) {
					types.put(rs.getInt("CD"), rs.getString("製品種別"));
				}

				ps = c.prepareStatement("SELECT * FROM M_依頼手段");
				rs = ps.executeQuery();
				while (rs.next()) {
					routes.put(rs.getInt("CD"), rs.getString("依頼手段"));
				}

				ps = c.prepareStatement("SELECT * FROM M_納期 WHERE CD<25");
				rs = ps.executeQuery();
				while (rs.next()) {
					deadlines.add(rs.getString("納期"));
				}
				ps = c.prepareStatement("SELECT * FROM M_受渡場所");
				rs = ps.executeQuery();
				while (rs.next()) {
					places.add(rs.getString("受渡場所"));
				}
				ps = c.prepareStatement("SELECT * FROM M_取引条件");
				rs = ps.executeQuery();
				while (rs.next()) {
					terms.add(rs.getString("取引条件"));
				}
				ps = c.prepareStatement("SELECT * FROM M_有効期間");
				rs = ps.executeQuery();
				while (rs.next()) {
					validities.add(rs.getString("有効期間"));
				}

				ps = c.prepareStatement("SELECT * FROM M_見積提出");
				rs = ps.executeQuery();
				while (rs.next()) {
					submits.put(rs.getInt("CD"), rs.getString("見積提出"));
				}

				ps = c.prepareStatement("SELECT * FROM M_使用通貨");
				rs = ps.executeQuery();
				while (rs.next()) {
					currencies.put(rs.getInt("CD"), rs.getString("通貨記号"));
				}

				ps = c.prepareStatement("SELECT * FROM M_納品区分");
				rs = ps.executeQuery();
				while (rs.next()) {
					states.put(rs.getInt("CD"), rs.getString("納品区分"));
				}

				ps = c.prepareStatement("SELECT * FROM M_納入手段");
				rs = ps.executeQuery();
				while (rs.next()) {
					ways.put(rs.getInt("CD"), rs.getString("納入手段"));
				}

				ps = c.prepareStatement("SELECT * FROM M_納品区分");
				rs = ps.executeQuery();
				while (rs.next()) {
					partials.put(rs.getInt("CD"), rs.getString("納品区分"));
				}

				ps = c.prepareStatement("SELECT * FROM M_見積表示");
				rs = ps.executeQuery();
				while (rs.next()) {
					indication.put(rs.getInt("CD"), rs.getString("表示種別"));
				}

				ps = c.prepareStatement("SELECT * FROM M_数量単位");
				rs = ps.executeQuery();
				while (rs.next()) {
					units.put(rs.getInt("CD"), rs.getString("数量単位"));
				}

				ps = c.prepareStatement("SELECT 仕入先CD,会社名 FROM M_法人 WHERE 仕入先CD IS NOT NULL");
				rs = ps.executeQuery();
				while (rs.next()) {
					suppliers.put(rs.getInt("仕入先CD"), rs.getString("会社名"));
				}
				/*
				 * Calendar cal = Calendar.getInstance();
				 * ps = c.prepareStatement("SELECT wc.CD,wp.単価 FROM M_加工_子 wc"
				 * + " left outer join (select CD,単価 from M_加工_単価 wp1"
				 * + " where 適用開始日<? AND NOT EXISTS ("
				 * + "		SELECT 1 FROM M_加工_単価 wp2"
				 * + "		WHERE wp1.適用開始日<wp2.適用開始日 AND wp1.CD=wp2.CD AND 適用開始日<?)"
				 * + "	) wp on wp.CD=wc.CD");
				 * ps.setDate(1, new java.sql.Date(cal.getTimeInMillis()));
				 * ps.setDate(2, new java.sql.Date(cal.getTimeInMillis()));
				 * rs = ps.executeQuery();
				 * while(rs.next()) {
				 * works.put(rs.getInt("CD"), rs.getInt("単価"));
				 * }
				 */
				ps = c.prepareStatement("SELECT * FROM M_原価 WHERE CD<101");
				rs = ps.executeQuery();
				while (rs.next()) {
					material.put(rs.getInt("CD"), rs.getString("大分類名"));
				}

				ps = c.prepareStatement("SELECT * FROM M_原価");
				rs = ps.executeQuery();
				while (rs.next()) {
					categoryL.put(rs.getInt("CD"), rs.getString("大分類名"));
				}

				ps = c.prepareStatement("SELECT * FROM M_材料_親");
				rs = ps.executeQuery();
				while (rs.next()) {
					List<Integer> subKey = new ArrayList<Integer>();
					subKey.add(rs.getInt("大分類CD"));
					if (!categoryM.containsKey(subKey)) {
						categoryM.put(subKey, new LinkedHashMap<Integer, String>());
					}
					categoryM.get(subKey).put(rs.getInt("CD"), rs.getString("中分類名"));
				}
				ps = c.prepareStatement("SELECT * FROM M_加工_親");
				rs = ps.executeQuery();
				while (rs.next()) {
					List<Integer> subKey = new ArrayList<Integer>();
					subKey.add(rs.getInt("大分類CD"));
					if (!categoryM.containsKey(subKey)) {
						categoryM.put(subKey, new LinkedHashMap<Integer, String>());
					}
					categoryM.get(subKey).put(rs.getInt("CD"), rs.getString("中分類名"));
				}

				ps = c.prepareStatement("SELECT * FROM M_比重");
				rs = ps.executeQuery();
				while (rs.next()) {
					sg.put(rs.getInt("種類"), rs.getDouble("比重"));
				}

			} catch (SQLException ex) {
				err.append(ex.getMessage());
				err.append("ErrorCode：" + ex.getErrorCode());
				err.append("SQLState：" + ex.getSQLState());
				Logging.logStackTrace(ex, lg, className);
			}

			try {
				ps = c.prepareStatement("SELECT * FROM M_材料_子");
				rs = ps.executeQuery();
				while (rs.next()) {
					List<Integer> subKey = new ArrayList<Integer>();
					subKey.add(rs.getInt("大分類CD"));
					subKey.add(rs.getInt("中分類CD"));
					if (!categoryS.containsKey(subKey)) {
						categoryS.put(subKey, new LinkedHashMap<Integer, String>());
					}
					categoryS.get(subKey).put(rs.getInt("CD"), rs.getString("小分類名"));
				}
			} catch (SQLException ex) {
				err.append(ex.getMessage());
				err.append("ErrorCode：" + ex.getErrorCode());
				err.append("SQLState：" + ex.getSQLState());
				Logging.logStackTrace(ex, lg, className);
			}
			try {
				Calendar cal = Calendar.getInstance();
				ps = c.prepareStatement(
					"SELECT wc.CD,中分類CD,大分類CD,小分類名,wp.単価 FROM M_加工_子 wc"
						+ " left outer join (select CD,単価 from M_加工_単価 wp1"
						+ " where 適用開始日<? AND NOT EXISTS ("
						+ "		SELECT 1 FROM M_加工_単価 wp2"
						+ "		WHERE wp1.適用開始日<wp2.適用開始日 AND wp1.CD=wp2.CD AND 適用開始日<?)"
						+ "	) wp on wp.CD=wc.CD"
						+ " WHERE 使用FLG='true'"
				);
				ps.setDate(1, new java.sql.Date(cal.getTimeInMillis()));
				ps.setDate(2, new java.sql.Date(cal.getTimeInMillis()));
				rs = ps.executeQuery();
				while (rs.next()) {
					int l = rs.getInt("大分類CD");
					int m = rs.getInt("中分類CD");
					int s = rs.getInt("CD");
					int price = rs.getInt("単価");
					String name = rs.getString("小分類名");
					if (prices.containsKey(l)) {
						Map<Integer, Map<Integer, Integer>> map_m = prices.get(l);
						if (map_m.containsKey(m)) {
							Map<Integer, Integer> map_s = map_m.get(m);
							map_s.put(s, price);
						} else {
							Map<Integer, Integer> map_s = new HashMap<Integer, Integer>();
							map_s.put(s, price);
							map_m.put(m, map_s);
						}
					} else {
						Map<Integer, Map<Integer, Integer>> map_m = new HashMap<Integer, Map<Integer, Integer>>();
						Map<Integer, Integer> map_s = new HashMap<Integer, Integer>();
						map_s.put(s, price);
						map_m.put(m, map_s);
						prices.put(l, map_m);
					}
					List<Integer> subKey = new ArrayList<Integer>();
					subKey.add(l);
					subKey.add(m);
					if (!categoryS.containsKey(subKey)) {
						categoryS.put(subKey, new LinkedHashMap<Integer, String>());
					}
					if (name != null)
						categoryS.get(subKey).put(s, name);
				}
			} catch (SQLException ex) {
				err.append(ex.getMessage());
				err.append("ErrorCode：" + ex.getErrorCode());
				err.append("SQLState：" + ex.getSQLState());
				Logging.logStackTrace(ex, lg, className);
			}

			/**
			 * クライアントに送信
			 */

			InitialDTO id = new InitialDTO(
				slips,
				types,
				routes,
				deadlines,
				places,
				terms,
				validities,
				submits,
				currencies,
				states,
				ways,
				partials,
				indication,
				units,
				suppliers,
				null,
				material,
				categoryL,
				categoryM,
				categoryS,
				prices,
				sg
			);

			response.setContentType("application/octet-stream");
			ObjectOutputStream out = new ObjectOutputStream(response.getOutputStream());
			out.writeObject(id);
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
