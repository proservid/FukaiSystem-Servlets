package fukaisystem;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;

import javax.servlet.GenericServlet;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import org.apache.log4j.Logger;

import fukaisystem.sql.DBConnection;
import fukaisystem.util.Logging;



public class Summary extends GenericServlet {

	private static final long serialVersionUID = 1L;
	private static final Logger lg = Logger.getLogger("A1");
	private static final String className = "Search\n";

	@Override
	public void service(ServletRequest request, ServletResponse response) {
		DBConnection dbc = new DBConnection();
		Connection c = dbc.getConnection();
		PreparedStatement ps = null;
		ResultSet rs = null;
		Date date = null;
		StringBuilder err = new StringBuilder();

		Vector<Vector<Object>> data = new Vector<Vector<Object>>();
		Map<Integer, Integer> taxes = new HashMap<Integer, Integer>();

		try {

			/**
			 * クライアントデータ受け取り
			 */
			ObjectInputStream in = new ObjectInputStream(request.getInputStream());
			Object obj = in.readObject();
			in.close();

			if(obj == null) {
				err.append(className + "readObjectがnullです\n");
				lg.error(className + "readObjectがnullです");
			} else {
				if(obj instanceof Date) {
					date = (Date)obj;
				} else {
					err.append(className + "readObjectがProjectSearchDTO型ではありません\n");
					lg.error(className + "readObjectがProjectSearchDTO型ではありません");
				}
			}

			Calendar cal = Calendar.getInstance();
			cal.setTimeInMillis(date.getTime());
			cal.add(Calendar.MONTH, 1);
			cal.add(Calendar.DATE, -1);
			try {
				ps = c.prepareStatement(
				"SELECT 得意先CD,SUM(税合計) AS 消費税 FROM (" +
					"SELECT " +
						"得意先CD," +
						"SUM(金額) AS 納入合計," +
						"ROUND(SUM(金額) *" +
						"(SELECT 税率 FROM M_消費税 t WHERE 適用開始日<='2013/3/31' AND NOT EXISTS" +
							"(SELECT 1 FROM M_消費税 t2 WHERE t.適用開始日<t2.適用開始日 AND 適用開始日<='2013/3/31')),0)" +
							" AS 税合計" +
					 "FROM V_売上集計ヘッダ" +
					 "WHERE 売上年月日>='2013/3/1' AND 売上年月日<'2013/4/1'" +
					 "GROUP BY 得意先CD" +
					 "UNION" +
					 "SELECT 0,0,金額 FROM T_売上_子 c" +
					 "LEFT OUTER JOIN T_売上_親 p" +
					 "ON c.売上親ID=p.売上親ID" +
					 "WHERE 表示CD=5 AND 売上年月日>='2013/3/1' AND 売上年月日<'2013/4/1'" +
				") a" +
				"GROUP BY 得意先CD");
//				ps.setDate(1, searchDTO.getStr(i));
//				ps.setDate(2, x);
				rs = ps.executeQuery();
				while(rs.next()) {
					taxes.put(rs.getInt("得意先CD"), rs.getInt("消費税"));
				}

				ps = c.prepareStatement(
				"SELECT 得意先CD,得意先名,受注番号,SUM(金額) AS 金額 FROM (" +
					"SELECT " +
						"sp.得意先CD," +
						"CASE" +
						"WHEN 種別CD=1 THEN '㈱'+会社名 + CASE WHEN 支店名 IS NULL THEN '' ELSE ' ' + 支店名 END" +
						"WHEN 種別CD=2 THEN 会社名+'㈱' + CASE WHEN 支店名 IS NULL THEN '' ELSE ' ' + 支店名 END" +
						"WHEN 種別CD=3 THEN '㈲'+会社名 + CASE WHEN 支店名 IS NULL THEN '' ELSE ' ' + 支店名 END" +
						"WHEN 種別CD=4 THEN 会社名+'㈲' + CASE WHEN 支店名 IS NULL THEN '' ELSE ' ' + 支店名 END" +
						"ELSE 会社名 END AS 得意先名," +
						"CONVERT(VARCHAR, pp.製作期)+'-'+CONVERT(VARCHAR, pp.製作番号) AS 受注番号," +
						"sc.金額" +
					"FROM T_売上_子 sc" +
					"LEFT OUTER JOIN T_売上_親 sp ON sc.売上親ID=sp.売上親ID" +
					"LEFT OUTER JOIN M_法人 c ON sp.得意先CD=c.得意先CD" +
					"LEFT OUTER JOIN T_製作_親 pp ON sc.製作親ID=pp.製作親ID" +
					"WHERE 売上年月日>='2013/3/1' AND 売上年月日<'2013/4/1'" +
				") a GROUP BY 得意先CD,得意先名,受注番号");
				//				ps.setDate(1, searchDTO.getStr(i));
//				ps.setDate(2, x)
				rs = ps.executeQuery();
				int accountID = 0;
				int subTotal = 0;
				while(rs.next()) {
					int newAccountID = rs.getInt("得意先CD");
					if(accountID != 0 && accountID == newAccountID) {
						if(taxes.containsKey(accountID)) {
							Vector<Object> v = new Vector<Object>();
							v.add(accountID);
							v.add("");
							v.add(taxes.get(accountID));
							data.add(v);
						}
						Vector<Object> v = new Vector<Object>();
						v.add(accountID);
						v.add("");
						v.add(subTotal);
						data.add(v);
					}
					Vector<Object> v = new Vector<Object>();
					v.add(newAccountID);
					v.add(rs.getString("製番"));
					v.add(rs.getString("金額"));
					data.add(v);
				}
				if(taxes.containsKey(accountID)) {
					Vector<Object> v = new Vector<Object>();
					v.add(accountID);
					v.add("");
					v.add(taxes.get(accountID));
					data.add(v);
				}
				Vector<Object> v = new Vector<Object>();
				v.add(accountID);
				v.add("");
				v.add(subTotal);
				data.add(v);
				v = new Vector<Object>();
				v.add(accountID);
				v.add("");
				v.add(subTotal);
				data.add(v);

			} catch(SQLException ex) {
				err.append(className + "テーブル「T_見積_親」の読み出しに失敗しました\n");
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
			out.writeObject(data);
			out.writeUTF(err.toString());
			out.flush();
			out.close();
		}catch(Exception ex) {
			lg.error(ex);
		} finally {
			try {
				if(c != null && !c.isClosed()) c.close();
			} catch(SQLException ex) {
				Logging.logStackTrace(ex, lg, className);
			}
// The following processes requires JDBC4.0.
			try {
				if(ps != null && !ps.isClosed()) {
					ps.close();
					lg.debug(className + "ps is closed by jdbc4.0");
				}
			} catch(SQLException ex) {
				Logging.logStackTrace(ex, lg, className);
			}
			try {
				if(rs != null && !rs.isClosed()) {
					rs.close();
					lg.debug(className + "rs is closed by jdbc4.0");
				}
			} catch(SQLException ex) {
				Logging.logStackTrace(ex, lg, className);
			}
		}
	}

}
