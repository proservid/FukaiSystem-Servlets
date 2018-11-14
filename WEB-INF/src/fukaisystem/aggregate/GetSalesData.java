package fukaisystem.aggregate;

import java.io.*;
import java.sql.*;
import java.sql.Date;
import java.util.*;
import javax.servlet.*;

import fukaisystem.dto.ColInfoDTO;
import fukaisystem.dto.SqlDTO;
import fukaisystem.dto.TableAdapter;
import fukaisystem.sql.DBConnection;

import org.apache.log4j.Logger;

/**
 * テーブルの内容と列情報を取得するためのクラス
 * @author kameura
 *
 */
public class GetSalesData extends GenericServlet {

	private static final long serialVersionUID = 1L;
	private static Logger lg = Logger.getLogger("dbtool");

	public void service(ServletRequest request, ServletResponse response) {

		DBConnection dbc = new DBConnection();
		Connection c = dbc.getConnection();
		PreparedStatement ps = null;
		ResultSet rs = null;

		Date from = null;
		Date to = null;

		String tableName = "";
		StringBuilder err = new StringBuilder("");

		Map<String, Integer> m = new HashMap<String, Integer>();
		try {
	//クライアントから読み込み

			ObjectInputStream in = new ObjectInputStream(request.getInputStream());
			Object obj = in.readObject();
			if(obj instanceof Date) {
				from = (Date)obj;
				Calendar target = Calendar.getInstance();
				target.setTime(from);
				target.add(Calendar.MONTH, 1);
				to = new Date(target.getTimeInMillis());
			}
			in.close();

			Object output = null;

			List<String> keys = new ArrayList<String>();
			List<ColInfoDTO> colInfos = new ArrayList<ColInfoDTO>();
			List<List<Object>> contents = new ArrayList<List<Object>>();
			try {
				ps = c.prepareStatement(
					"SELECT" +
					" 得意先CD," +
					" ROUND(CAST(税合計 AS DECIMAL(18,9)),2) AS 税額" +
					" FROM (" +
						"SELECT" +
						" 得意先CD," +
						" ROUND(SUM(金額) * (" +
							"SELECT 税率" +
							" FROM M_消費税 t" +
							" WHERE 適用開始日<? AND NOT EXISTS (" +
								"SELECT 1 FROM M_消費税 t2" +
								" WHERE t.適用開始日<t2.適用開始日 AND 適用開始日<?" +
							")" +
						"),0) AS 税合計" +
						" FROM V_売上集計ヘッダ " +
						" WHERE 売上年月日>=? AND 売上年月日<?" +
						" GROUP BY 得意先CD" +
					") a");
				ps.setDate(1, to);
				ps.setDate(2, to);
				ps.setDate(3, from);
				ps.setDate(4, to);
				rs = ps.executeQuery();
				while(rs.next()) {
					m.put(rs.getString("得意先CD"), rs.getInt("税額"));
				}
				System.out.println("!!!!!");
				ps = c.prepareStatement(
					 "select 得意先CD,得意先名,受注番号,日付,sum(金額) as 金額 from (" +
						 " select " +
						 " s.得意先CD," +
						 " CASE WHEN 種別CD=1 THEN '㈱'+会社名 + CASE WHEN 支店名 IS NULL THEN '' ELSE ' ' + 支店名 END" +
						 "      WHEN 種別CD=2 THEN 会社名+'㈱' + CASE WHEN 支店名 IS NULL THEN '' ELSE ' ' + 支店名 END" +
						 "      WHEN 種別CD=3 THEN '㈲'+会社名 + CASE WHEN 支店名 IS NULL THEN '' ELSE ' ' + 支店名 END" +
						 "      WHEN 種別CD=4 THEN 会社名+'㈲' + CASE WHEN 支店名 IS NULL THEN '' ELSE ' ' + 支店名 END" +
						 "      ELSE 会社名 END AS 得意先名," +
						 " convert(varchar, pp.製作期)+'-'+convert(varchar, pp.製作番号)+pp.製作枝番 as 受注番号," +
						 " s.売上年月日 as 日付," +
						 " s.金額 " +
						 " FROM (select 製作親ID,金額,得意先CD,売上年月日,売上FLG from T_売上_子 sc" +
						 " left outer join T_売上_親 sp on sc.売上親ID=sp.売上親ID" +
						 " where 売上年月日>=? AND 売上年月日<? and 売上FLG='true' and 納品区分CD<5) s" +
						 " left outer join M_法人 c on s.得意先CD=c.得意先CD" +
						 " left outer join T_製作_親 pp on s.製作親ID=pp.製作親ID" +
						 " WHERE 製作番号<9000" +
					 ") a group by 得意先CD,得意先名,受注番号,日付");
				ps.setDate(1, from);
				ps.setDate(2, to);
				rs = ps.executeQuery();
				ResultSetMetaData rsmd = rs.getMetaData();
				for(int i = 1; i <= rsmd.getColumnCount(); i++) {
					ColInfoDTO ci = new ColInfoDTO(rsmd.getColumnName(i), rsmd.getColumnTypeName(i), rsmd.getColumnType(i), rsmd.getColumnDisplaySize(i));
					colInfos.add(ci);
				}

				String acNum = "";
				String acName = "";
				int subtotal = 0;
				int total = 0;
				while(rs.next()) {
					if(!acNum.equals(rs.getString("得意先CD")) && !acNum.equals("")) {
						if(m.containsKey(acNum)) {
							List<Object> row = new ArrayList<Object>();
							row.add(acNum);
							row.add(acName);
							row.add("消費税");
							row.add("");
							row.add(m.get(acNum));
							contents.add(row);
							subtotal += m.get(acNum);
							total += m.get(acNum);
						}
						List<Object> row = new ArrayList<Object>();
						row.add(acNum);
						row.add(acName);
						row.add("");
						row.add("");
						row.add(subtotal);
						contents.add(row);
						row = new ArrayList<Object>();
						row.add("");
						row.add("");
						row.add("");
						row.add("");
						row.add(null);
						contents.add(row);
						subtotal = 0;
					}
					List<Object> row = new ArrayList<Object>();
					row.add(rs.getString("得意先CD"));
					row.add(rs.getString("得意先名"));
					row.add(rs.getString("受注番号"));
					row.add(rs.getString("日付"));
					row.add(rs.getInt("金額"));
					contents.add(row);
					acNum = rs.getString("得意先CD");
					acName = rs.getString("得意先名");
					subtotal += rs.getInt("金額");
					total += rs.getInt("金額");
				}
				rs.close();
				if(m.containsKey(acNum)) {
					List<Object> row = new ArrayList<Object>();
					row.add(acNum);
					row.add(acName);
					row.add("消費税");
					row.add("");
					row.add(m.get(acNum));
					contents.add(row);
					subtotal += m.get(acNum);
					total += m.get(acNum);
				}
				List<Object> row = new ArrayList<Object>();
				row.add(acNum);
				row.add(acName);
				row.add("");
				row.add("");
				row.add(subtotal);
				contents.add(row);
				row = new ArrayList<Object>();
				row.add("");
				row.add("");
				row.add("");
				row.add("");
				row.add(null);
				contents.add(row);
				row = new ArrayList<Object>();
				row.add("");
				row.add("");
				row.add("");
				row.add("合計");
				row.add(total);
				contents.add(row);

				if(!tableName.equals("")) {
					ps = c.prepareStatement("SELECT COLUMN_NAME FROM information_schema.constraint_column_usage" +
							" WHERE table_name=? AND constraint_name LIKE 'PK_%'");
					ps.setString(1, tableName);
					rs = ps.executeQuery();
					while(rs.next()) {
						keys.add(rs.getString("COLUMN_NAME"));
					}
				}
				output = new TableAdapter(keys, colInfos, contents);
			} catch(SQLException ex) {
				ex.printStackTrace();
				err.append(ex + "\n");
				lg.error("GetElements3 " + ex);
			}

	//クライアントに送信

			response.setContentType("application/octet-stream");
			ObjectOutputStream out = new ObjectOutputStream(response.getOutputStream());
			out.writeObject(output);
			out.writeUTF(err.toString());
			out.flush();
			out.close();
		}catch(Exception ex) {
			ex.printStackTrace();
			lg.error(ex);
		} finally {
			try {
				if(c != null && !c.isClosed()) c.close();
			} catch(SQLException ex) {
				lg.error("c:" + ex);
			}
			// The following processes requires JDBC4.0.
			try {
				if(ps != null && !ps.isClosed()) {
					ps.close();
					lg.debug("ps is closed by jdbc4.0");
				}
			} catch(SQLException ex) {
				lg.error("ps:" + ex);
			}
			try {
				if(rs != null && !rs.isClosed()) {
					rs.close();
					lg.debug("rs is closed by jdbc4.0");
				}
			} catch(SQLException ex) {
				lg.error("rs:" + ex);
			}
		}
	}
}