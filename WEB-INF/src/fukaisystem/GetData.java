package fukaisystem;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import javax.servlet.GenericServlet;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import org.apache.log4j.Logger;

import fukaisystem.dto.GetTableDTO;
import fukaisystem.sql.DBConnection;
import fukaisystem.util.Logging;


public class GetData extends GenericServlet {

	private static final long serialVersionUID = 1L;
	private static final Logger lg = Logger.getLogger("A1");
	private static final String className = "GetTableData\n";


	public void service(ServletRequest request, ServletResponse response) {

		DBConnection dbc = new DBConnection();
		Connection c = dbc.getConnection();
		PreparedStatement ps = null;
		ResultSet rs = null;
		GetTableDTO input = null;
		StringBuilder err = new StringBuilder("");
		Object data = null;
		Map<String, List<String>> dataMap = new HashMap<String, List<String>>();
		Vector<Vector<Object>> dataVector = new Vector<Vector<Object>>();

		try {

			/**
			 * クライアントデータ受け取り
			 */
			ObjectInputStream in = new ObjectInputStream(request.getInputStream());
			Object obj = in.readObject();
			in.close();

			if(obj == null) {
				err.append("readObjectがnullです\n");
				lg.error(className + "readObjectがnullです");
			} else {
				if(obj instanceof GetTableDTO) {
					input = (GetTableDTO)obj;
				} else {
					err.append("readObjectがString型ではありません\n");
					lg.error(className + "readObjectがString型ではありません");
				}
			}
			String[] keys = input.getKeys();
			String[] signs = input.getSigns();
			String[] values = input.getIDs();
			String orderStr = input.getOrder();

			try {
//				int columns = 0;
//				ps = c.prepareStatement("SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_NAME=?");
//				ps.setString(1, input.get(0));
//				rs = ps.executeQuery();
//				if(rs.next()) {
//					columns = rs.getInt(1);
//					for(int i = 0; i < columns; i++) {
//						List<String> al = new ArrayList<String>();
//						data.add(al);
//					}
//				}
				StringBuilder query = new StringBuilder("");
				//売上集計ヘッダは特別な処理（例外扱い）
				if(input.getString(0).equals("V_売上集計ヘッダ")) {
					SimpleDateFormat format = new SimpleDateFormat("yyyy/MM/dd");
					Calendar cal = Calendar.getInstance();
					cal.setTimeInMillis(format.parse(values[0]).getTime());
					Date current = new Date(cal.getTimeInMillis());
					//System.out.println(current);
					cal.add(Calendar.MONTH, 1);
					Date next = new Date(cal.getTimeInMillis());
					cal.add(Calendar.DATE, -1);
					Date last = new Date(cal.getTimeInMillis());

					query.append(
						"SELECT " +
						"得意先CD," +
						"出荷伝票番号," +
						"社名," +
						"見出し+' ( \\'+REPLACE(CONVERT(VARCHAR, CAST(納入合計 AS MONEY), 1), '.00', '')+' )\n* 消費税' AS 納入見出し1," +
						"見出し+'\n* 消費税' AS 納入見出し2," +
						"REPLACE(CONVERT(VARCHAR, CAST(納入合計 AS MONEY), 1), '.00', '') AS 納入額," +
						"数量," +
						"REPLACE(CONVERT(varchar, CAST(ROUND(CAST(税合計 AS DECIMAL(18,9)),2) AS MONEY), 1), '.00', '') AS 税額," +
						"REPLACE(CONVERT(VARCHAR, CAST(納入合計+税合計 AS MONEY), 1), '.00', '') AS 合計額," +
						"売上日" +
						" FROM (" +
							"SELECT " +
							"得意先CD," +
							"'9-'+CONVERT(varchar,ROW_NUMBER() OVER (" + orderStr + ")) AS 出荷伝票番号," +
							"MIN(社名) AS 社名," +
							"CONVERT(varchar,MONTH(?))+'月度納入額' AS 見出し," +//current
							"SUM(金額) AS 納入合計," +
							"'1式' AS 数量," +
							"ROUND(SUM(金額) * " +
							"(SELECT 税率 FROM M_消費税 t WHERE 適用開始日<=? AND NOT EXISTS" +//last
							" (SELECT 1 FROM M_消費税 t2 WHERE t.適用開始日<t2.適用開始日 AND 適用開始日<=?)),0) AS 税合計," +//last
							"dbo.F_和暦表示(?) AS 売上日" +//last
							" FROM V_売上集計ヘッダ" +
							" WHERE 売上年月日>=? AND 売上年月日<?" +//current,next
							" GROUP BY 得意先CD) a");
					if(values[2].equals("0")) {//その月の対象社一覧を表示する場合
						ps = c.prepareStatement(query.toString());
						ps.setDate(1, current);//月度納入額
						ps.setDate(2, last);//消費税
						ps.setDate(3, last);//消費税
						ps.setDate(4, last);//売上日
						ps.setDate(5, current);//抽出(from)
						ps.setDate(6, next);//抽出(to)
						rs = ps.executeQuery();
						while(rs.next()) {
							Vector<Object> v = new Vector<Object>();
							v.add(rs.getString("得意先CD"));
							v.add(rs.getString("出荷伝票番号"));
							v.add(rs.getString("社名"));
							v.add(rs.getString("納入額"));
							v.add(rs.getString("税額"));
							dataVector.add(v);
						}
						data = dataVector;
					} else {//対象社のうちの1社を選択した場合
						query.append(" WHERE 得意先CD=" + values[2]);
						ps = c.prepareStatement(query.toString());
						ps.setDate(1, current);//月度納入額
						ps.setDate(2, last);//消費税
						ps.setDate(3, last);//消費税
						ps.setDate(4, last);//売上日
						ps.setDate(5, current);//抽出(from)
						ps.setDate(6, next);//抽出(to)
						rs = ps.executeQuery();
						ResultSetMetaData metaData = rs.getMetaData();
						int columnCount = metaData.getColumnCount();

						while(rs.next()) {
							for (int k = 0; k < columnCount; k++) {
								String columnName = metaData.getColumnName(k + 1);
								if(dataMap.containsKey(columnName)) dataMap.get(columnName).add(rs.getString(columnName));
								else {
									List<String> columnData = new ArrayList<String>();
									columnData.add(rs.getString(columnName));
									dataMap.put(columnName, columnData);
								}
							}
							data = dataMap;
						}
					}
				//見積原簿も特別な処理（例外扱い）
				} else if(input.getString(0).equals("V_見積原簿")) {
					query.append("select " +
							" z.ID,見積子ID," +
							" min(大分類CD) as 大分類CD," +
							" min(中分類CD) as 中分類CD," +
							" min(小分類CD) as 小分類CD," +
							" case when z.ID is null then case when 見積子ID is null then '総合計' else min(c.名称) end else min(z.名称) end as 名称," +
							" case when z.ID is null then REPLACE(CONVERT(VARCHAR, CAST(sum(case when z.ID=1 then 提示額 else 0 end) AS MONEY), 1), '.00', '') else '' end as 提示額," +
							" case when z.ID is null then '' else REPLACE(CONVERT(VARCHAR, CAST(min(z.単価) AS MONEY), 1), '.00', '') end as 単価," +
							" case when z.ID is null then '' else CONVERT(VARCHAR,CONVERT(FLOAT,min(z.数量))) end as 数量," +
							" REPLACE(CONVERT(VARCHAR, CAST(sum(原価) AS MONEY), 1), '.00', '') as 原価," +
							" case when z.ID is null then '' else CONVERT(VARCHAR,min(掛率)) end as 掛率," +
							" REPLACE(CONVERT(VARCHAR, CAST(sum(小計) AS MONEY), 1), '.00', '') as 小計," +
							" case when z.ID is null then case when 見積子ID is null then '' else min(c.備考) end else min(z.備考) end as 備考" +
							"   from (" +
							"	SELECT em.ID,em.見積親ID,見積子ID,大分類CD,中分類CD,小分類CD,em.名称," +
							"	重量,em.単価,em.数量,round(em.数量*em.単価,0) as 原価,掛率,round(em.数量*em.単価*掛率,0) as 小計,em.備考," +
							"	CASE WHEN 種別CD=1 THEN '㈱'+会社名" +
							"		WHEN 種別CD=2 THEN 会社名+'㈱'" +
							"		WHEN 種別CD=3 THEN '㈲'+会社名" +
							"		WHEN 種別CD=4 THEN 会社名+'㈲'" +
							"		ELSE 会社名 END AS 仕入先名" +
							"	FROM T_見積_材料 em" +
							"	LEFT OUTER JOIN T_見積_子 ec ON em.見積子ID=ec.ID and em.見積親ID=ec.見積親ID" +
							"	LEFT OUTER JOIN T_見積_親 ep ON ec.見積親ID=ep.見積親ID" +
							"	LEFT OUTER JOIN T_製作_親 pp ON ec.見積親ID=pp.見積親ID" +
							"	LEFT OUTER JOIN M_法人 c on em.仕入先CD=c.仕入先CD" +
							"	union all" +
							"	select ID,見積親ID,見積子ID,大分類CD,中分類CD,小分類CD,ew.名称," +
							"	0,単価,時間,round(単価*時間,0) as 原価,掛率,round(単価*時間*掛率,0) as 小計,ew.備考,'' from T_見積_加工 ew" +
							") z " +
							" left outer join T_見積_子 c on c.見積親ID=z.見積親ID and c.ID=z.見積子ID" +
							" left outer join T_見積_親 p on c.見積親ID=p.見積親ID" +
							"	where z.見積親ID=?" +
							"	group by rollup(見積子ID,z.ID)" +
							"	order by 見積子ID,z.ID");
					ps = c.prepareStatement(query.toString());
					ps.setString(1, values[0]);//見積親ID
					rs = ps.executeQuery();
					ResultSetMetaData metaData = rs.getMetaData();
					int columnCount = metaData.getColumnCount();

					while(rs.next()) {
						for (int k = 0; k < columnCount; k++) {
							String columnName = metaData.getColumnName(k + 1);
							if(dataMap.containsKey(columnName)) dataMap.get(columnName).add(rs.getString(columnName));
							else {
								List<String> columnData = new ArrayList<String>();
								columnData.add(rs.getString(columnName));
								dataMap.put(columnName, columnData);
							}
						}
					}
					data = dataMap;
				//それ以外は通常処理
				} else {
					query.append("SELECT " + input.getString(1) + " FROM " + input.getString(0) + " WHERE ");
					int i = 0;

					for(String key : keys) {
						if(i > 0) {
							query.append(" AND ");
						}
						query.append(key + signs[i] + "?");
						i++;
					}
					ps = c.prepareStatement(query.toString() + orderStr);
					int j = 1;
					for(String id : values) {
						ps.setString(j, id);
						j++;
					}
					rs = ps.executeQuery();
					ResultSetMetaData metaData = rs.getMetaData();
					int columnCount = metaData.getColumnCount();

					while(rs.next()) {
						for (int k = 0; k < columnCount; k++) {
							String columnName = metaData.getColumnName(k + 1);
							if(dataMap.containsKey(columnName)) dataMap.get(columnName).add(rs.getString(columnName));
							else {
								List<String> columnData = new ArrayList<String>();
								columnData.add(rs.getString(columnName));
								dataMap.put(columnName, columnData);
							}
						}
					}
					data = dataMap;
				}

			} catch(SQLException ex) {
				err.append("テーブル「T_テーブル名」の読込に失敗しました\n");
				Logging.logStackTrace(ex, lg, className);
			}

		} catch(Exception ex) {
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
			Logging.logStackTrace(ex, lg, className);
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
