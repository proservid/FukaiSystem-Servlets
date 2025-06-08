package fukaisystem.aggregate;

import java.io.*;
import java.sql.*;
import java.sql.Date;
import java.text.DecimalFormat;
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
public class GetLedger extends GenericServlet {

	private static final long serialVersionUID = 1L;
	private static Logger lg = Logger.getLogger("dbtool");

	public void service(ServletRequest request, ServletResponse response) {

		DBConnection dbc = new DBConnection();
		Connection c = dbc.getConnection();
		PreparedStatement ps = null;
		ResultSet rs = null;

		Date from = null;
		Date to = null;
		Date last = null;

		int month = 0;
		String tableName = "";
		StringBuilder err = new StringBuilder("");

		Map<String, Amount> m = new HashMap<String, Amount>();
		DecimalFormat df = new DecimalFormat("#,###");
		try {
	//クライアントから読み込み

			ObjectInputStream in = new ObjectInputStream(request.getInputStream());
			Object obj = in.readObject();
			if(obj instanceof Date) {
				from = (Date)obj;
				Calendar target = Calendar.getInstance();
				target.setTime(from);
				month = target.get(Calendar.MONTH) + 1;
				target.add(Calendar.MONTH, 1);
				to = new Date(target.getTimeInMillis());
				target.add(Calendar.DATE, -1);
				last = new Date(target.getTimeInMillis());
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
					" 合計額," +
					" ROUND(CAST(税合計 AS DECIMAL(18,9)),2) AS 税額" +
					" FROM (" +
						"SELECT" +
						" 得意先CD," +
						" SUM(金額) AS 合計額," +
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
					m.put(rs.getString("得意先CD"), new Amount(rs.getInt("合計額"), rs.getInt("税額")));
				}
				
				ps = c.prepareStatement(
					 "select sp.得意先CD," +
//					 "sc.売上親ID,ID," +
					 "CASE WHEN 種別CD = 1 THEN '㈱' + 会社名 + CASE WHEN 支店名 IS NULL THEN '' ELSE ' ' + 支店名 END WHEN 種別CD = 2 THEN 会社名 + '㈱' + CASE WHEN 支店名 IS NULL" +
					 "                     THEN '' ELSE ' ' + 支店名 END WHEN 種別CD = 3 THEN '㈲' + 会社名 + CASE WHEN 支店名 IS NULL " +
					 "                     THEN '' ELSE ' ' + 支店名 END WHEN 種別CD = 4 THEN 会社名 + '㈲' + CASE WHEN 支店名 IS NULL THEN '' ELSE ' ' + 支店名 END ELSE 会社名 + CASE WHEN 支店名 IS NULL" +
					 "                     THEN '' ELSE ' ' + 支店名 END END AS 得意先名," +
					 "売上年月日 as 納入月日," +
					 "品名," +
					 "case when 数量=0 then ''" +
					 "     when 各FLG = 1 then '各' + convert(varchar,数量) + 数量単位 else convert(varchar,数量) + 数量単位 end as 数量," +
					 "金額," +
					 "convert(varchar,製作期)+'-'+convert(varchar,製作番号) + 製作枝番 as 受注番号," +
					 "受注番号 as 注文書番号" +
					 " from T_売上_子 sc" +
					 " left outer join T_売上_親 sp on sp.売上親ID=sc.売上親ID" +
					 " left outer join M_数量単位 u on sc.数量単位CD=u.CD" +
					 " left outer join M_法人 co on sp.得意先CD=co.得意先CD" +
					 " left outer join T_製作_親 pp on sc.製作親ID=pp.製作親ID" +
					 " where 売上年月日>=? and 売上年月日<? and 売上FLG='true' and 製作番号<9000 and 納品区分CD<5" +
					 " order by 得意先CD,売上年月日,受注番号,ID");
				ps.setDate(1, from);
				ps.setDate(2, to);
				rs = ps.executeQuery();
				ResultSetMetaData rsmd = rs.getMetaData();
				for(int i = 2; i <= rsmd.getColumnCount(); i++) {//最初の列（得意先CD）はスキップ
					ColInfoDTO ci = new ColInfoDTO(rsmd.getColumnName(i), rsmd.getColumnTypeName(i), rsmd.getColumnType(i), rsmd.getColumnDisplaySize(i));
					colInfos.add(ci);
				}

				String acNum = "";
				String acName = "";
				String accept = "";
				int subtotal = 0;
				int total = 0;
				int inclusive = 0;
				while(rs.next()) {
					//特殊データの追加
					if(rs.getString("受注番号") != null) {
					if(!accept.equals(rs.getString("受注番号")) && !accept.equals("")) {//次の受注番号へ変わるタイミングで小計を追加
						List<Object> row = new ArrayList<Object>();
//						row.add(acNum);
						row.add(acName);
						row.add("");
						row.add("小計");
						row.add("");//数量
						row.add(subtotal);//金額
						row.add("");//受注番号
						row.add("");//注文書番号
						contents.add(row);
						subtotal = 0;
						if(!acNum.equals(rs.getString("得意先CD")) && !acNum.equals("")) {//さらに次の得意先CDへ変わるタイミングで（消費税別途の得意先の消費税と）合計を追加
							if(m.containsKey(acNum)) {//消費税を別途計算していた得意先については、追加
								row = new ArrayList<Object>();
//								row.add(acNum);
								row.add(acName);
								row.add(last);
								row.add(month + "月度納入額(\\"+df.format(m.get(acNum).getPrice())+")");
								row.add("");//数量
								row.add("");//金額
								row.add("");//受注番号
								row.add("");//注文書番号
								contents.add(row);

								row = new ArrayList<Object>();
//								row.add(acNum);
								row.add(acName);
								row.add(last);
								row.add("* 消費税");
								row.add("");//数量
								row.add(m.get(acNum).getTax());//金額
								row.add("");//受注番号
								row.add("");//注文書番号
								contents.add(row);

								row = new ArrayList<Object>();
//								row.add(acNum);
								row.add(acName);
								row.add("");
								row.add("小計");
								row.add("");//数量
								row.add(m.get(acNum).getTax());//金額
								row.add("");//受注番号
								row.add("");//注文書番号
								contents.add(row);
								total += m.get(acNum).getTax();
								inclusive += m.get(acNum).getTax();
							}
							row = new ArrayList<Object>();
//							row.add(acNum);
							row.add(acName);
							row.add("");
							row.add("合計");
							row.add("");//数量
							row.add(total);//金額
							row.add("");//受注番号
							row.add("");//注文書番号
							contents.add(row);
							total = 0;
						}
					}
					//通常データの追加
					List<Object> row = new ArrayList<Object>();
//					row.add(rs.getString("得意先CD"));
					row.add(rs.getString("得意先名"));
					row.add(rs.getString("納入月日"));
					row.add(rs.getString("品名"));
					row.add(rs.getString("数量"));
					row.add(rs.getInt("金額"));
					row.add(rs.getString("受注番号"));
					row.add(rs.getString("注文書番号"));
					contents.add(row);
					acNum = rs.getString("得意先CD");
					acName = rs.getString("得意先名");
					accept = rs.getString("受注番号");
					subtotal += rs.getInt("金額");
					total += rs.getInt("金額");
					inclusive += rs.getInt("金額");
					}
				}
				rs.close();

				//最終データ分の小計合計そして総合計
				List<Object> row = new ArrayList<Object>();
//				row.add(acNum);
				row.add(acName);
				row.add("");
				row.add("小計");
				row.add("");//数量
				row.add(subtotal);//金額
				row.add("");//受注番号
				row.add("");//注文書番号
				contents.add(row);
				subtotal = 0;

				if(m.containsKey(acNum)) {//消費税を別途計算していた得意先については、追加
					row = new ArrayList<Object>();
//					row.add(acNum);
					row.add(acName);
					row.add(last);
					row.add(month + "月度納入額(\\"+df.format(m.get(acNum).getPrice())+")");
					row.add("");//数量
					row.add("");//金額
					row.add("");//受注番号
					row.add("");//注文書番号
					contents.add(row);

					row = new ArrayList<Object>();
//					row.add(acNum);
					row.add(acName);
					row.add(last);
					row.add("* 消費税");
					row.add("");//数量
					row.add(m.get(acNum).getTax());//金額
					row.add("");//受注番号
					row.add("");//注文書番号
					contents.add(row);

					row = new ArrayList<Object>();
//					row.add(acNum);
					row.add(acName);
					row.add("");
					row.add("小計");
					row.add("");//数量
					row.add(m.get(acNum).getTax());//金額
					row.add("");//受注番号
					row.add("");//注文書番号
					contents.add(row);
					total += m.get(acNum).getTax();
					inclusive += m.get(acNum).getTax();
				}
				row = new ArrayList<Object>();
//				row.add(acNum);
				row.add(acName);
				row.add("");
				row.add("合計");
				row.add("");//数量
				row.add(total);//金額
				row.add("");//受注番号
				row.add("");//注文書番号
				contents.add(row);
				row = new ArrayList<Object>();
//				row.add(acNum);
				row.add(acName);
				row.add("");
				row.add("総合計");
				row.add("");//数量
				row.add(inclusive);//金額
				row.add("");//受注番号
				row.add("");//注文書番号
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

	private class Amount {
		int price;
		int tax;
		private Amount(int price, int tax) {
			this.price = price;
			this.tax = tax;
		}
		private int getPrice() {
			return price;
		}
		private int getTax() {
			return tax;
		}
	}
}