package fukaisystem;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

import javax.servlet.GenericServlet;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import org.apache.log4j.Logger;

import fukaisystem.dto.HistoryDTO;
import fukaisystem.sql.DBConnection;
import fukaisystem.util.Logging;

public class HistorySearch extends GenericServlet {

	private static final long serialVersionUID = 1L;
	private static final Logger lg = Logger.getLogger("A1");
	private static final String className = "HistorySearch\n";

	@Override
	public void service(ServletRequest request, ServletResponse response) {
		DBConnection dbc = new DBConnection();
		Connection c = dbc.getConnection();
		PreparedStatement ps = null;
		ResultSet rs = null;
		HistoryDTO dto = null;
		StringBuilder err = new StringBuilder();
		List<Integer> stringConditionIndex = new ArrayList<Integer>();
		List<Integer> intConditionIndex = new ArrayList<Integer>();

		Vector<Vector<Object>> data = new Vector<Vector<Object>>();

		boolean isStock = false;
		String[] constStrs = {
			"材料品名 like ?", "注文枝番 like ?",
			"SUBSTRING(CONVERT(VARCHAR, 注文年月日),1,4) like ?",
			"SUBSTRING(CONVERT(VARCHAR, 注文年月日),6,2) like ?",
			"SUBSTRING(CONVERT(VARCHAR, 注文年月日),9,2) like ?",
			"SUBSTRING(CONVERT(VARCHAR, 指定納期),1,4) like ?",
			"SUBSTRING(CONVERT(VARCHAR, 指定納期),6,2) like ?",
			"SUBSTRING(CONVERT(VARCHAR, 指定納期),9,2) like ?",
			"SUBSTRING(CONVERT(VARCHAR, 入庫年月日),1,4) like ?",
			"SUBSTRING(CONVERT(VARCHAR, 入庫年月日),6,2) like ?",
			"SUBSTRING(CONVERT(VARCHAR, 入庫年月日),9,2) like ?",
		};
		String[] constInts = { "仕入先CD=?", "注文期=?", "注文番号=?", "大分類CD=?", "中分類CD=?", "小分類CD=?" };
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
				if (obj instanceof HistoryDTO) {
					dto = (HistoryDTO) obj;
				} else {
					err.append(className + "readObjectがProjectSearchDTO型ではありません\n");
					lg.error(className + "readObjectがProjectSearchDTO型ではありません");
				}
			}
			try {
				isStock = dto.getBool(1);
				StringBuilder query = new StringBuilder("");
				if (isStock) {
					query.append(
						"select top 30000 *,case when 数量=1 then 金額*残数 else 単価*残数 end as 金額 from ("
							+ "	select sc.在庫親ID,sc.ID,sp.仕入先CD,'在庫' as 種別,sc.大分類CD,sc.中分類CD,sc.小分類CD,材料品名,注文年月日,入庫年月日,指定納期,"
							+ "	注文期,注文番号,注文枝番,sc.数量,sc.数量-case when dis.使用数 is null then 0 else dis.使用数 end as 残数,"
							+ "	sc.数量単位CD,sc.重量長さ,sc.単価,sc.金額,伝票番号,納品書番号,sc.備考,摘要,"
							+ "	CASE WHEN 種別CD=1 THEN '㈱'+会社名"
							+ "		 WHEN 種別CD=2 THEN 会社名+'㈱'"
							+ "		 WHEN 種別CD=3 THEN '㈲'+会社名"
							+ "		 WHEN 種別CD=4 THEN 会社名+'㈲'"
							+ "		 ELSE 会社名 END AS 仕入先名"
							+ "	FROM T_在庫_親 sp"
							+ "	LEFT OUTER JOIN T_在庫_子 sc ON sp.在庫親ID=sc.在庫親ID"
							+ "	LEFT OUTER JOIN T_指定納品書 d on d.ID=sc.納品書番号"
							+ "	LEFT OUTER JOIN M_法人 c on sp.仕入先CD=c.仕入先CD"
							+ "	LEFT OUTER JOIN (select 在庫親ID,在庫子ID,sum(数量) as 使用数 from T_出庫_子 group by 在庫親ID,在庫子ID) dis"
							+ "	on sc.在庫親ID=dis.在庫親ID and sc.ID=dis.在庫子ID"
							+ "	where sc.大分類CD>=1 and sc.大分類CD<=2 and 注文期>=1000"
							+ " UNION ALL"
							+ "	select p.製作親ID as 在庫親ID,c.ID,p.得意先CD,'在庫' as 種別,99 as 大分類CD,0 as 中分類CD,0 as 小分類CD,名称,発行年月日,完成年月日,納期,"
							+ "	製作期,製作番号,製作枝番,c.数量,c.数量-case when d.使用数 is null then 0 else d.使用数 end as 残数,"
							+ "	c.数量単位CD,null as 重量,c.単価,c.金額,null as 伝票番号,null as 納品書番号,c.備考,摘要,null as 仕入先名"
							+ "	FROM T_製作_親 p"
							+ "	LEFT OUTER JOIN T_製作_子 c on p.製作親ID=c.製作親ID and 表示CD=2"
							+ "	LEFT OUTER JOIN (SELECT 在庫親ID,在庫子ID,sum(数量) as 使用数 FROM T_出庫_子 WHERE 大分類CD=99 group by 在庫親ID,在庫子ID) d"
							+ "	ON p.製作親ID=d.在庫親ID AND c.ID=d.在庫子ID"
							+ "	WHERE 製作番号>=9000 AND 製作番号<10000"
							+ ") s"
					);
				} else {
					query.append(
						"select top 30000 * from ("
							+ "SELECT sc.在庫親ID,sc.ID,sp.仕入先CD,'注文' as 種別,大分類CD,中分類CD,小分類CD,材料品名,注文年月日,入庫年月日,指定納期,"
							+ " 注文期,注文番号,注文枝番,数量,0 as 残数,数量単位CD,重量長さ,単価,金額,伝票番号,納品書番号,sc.備考,摘要,"
							+ " CASE WHEN 種別CD=1 THEN '㈱'+会社名"
							+ "      WHEN 種別CD=2 THEN 会社名+'㈱'"
							+ "      WHEN 種別CD=3 THEN '㈲'+会社名"
							+ "      WHEN 種別CD=4 THEN 会社名+'㈲'"
							+ "      ELSE 会社名 END AS 仕入先名"
							+ " FROM T_在庫_親 sp"
							+ " LEFT OUTER JOIN T_在庫_子 sc ON sp.在庫親ID=sc.在庫親ID"
							+ " LEFT OUTER JOIN T_指定納品書 d on d.ID=sc.納品書番号"
							+ " LEFT OUTER JOIN M_法人 c on sp.仕入先CD=c.仕入先CD"
							+ " where 表示CD=2 AND 納品書番号>-1"
							// " union all" +
							// " SELECT sc.在庫親ID,sc.ID,0 as
							// 仕入先CD,'出庫',大分類CD,中分類CD,小分類CD,品名,出庫年月日,null,null,"
							// +
							// " 製作期,製作番号,製作枝番,数量,0 as 残数,数量単位CD,重量長さ,単価,金額,''
							// as 伝票番号,'' as 納品書番号,sc.備考,用途,摘要" +
							// " FROM T_出庫_親 sp" +
							// " LEFT OUTER JOIN T_出庫_子 sc ON sp.出庫親ID=sc.出庫親ID"
							// +
							+ " union all"
							+ " SELECT distinct 0,em.ID,em.仕入先CD,'見積',大分類CD,中分類CD,小分類CD,em.名称,見積年月日,null,null,"
							+ " 製作期,製作番号,製作枝番,em.数量,0 as 残数, 0 as 数量単位CD,"
							+ " 重量,em.単価,em.数量*em.単価 as 金額,'' as 伝票番号,'' as 納品書番号,em.備考,ec.備考,"
							+ " CASE WHEN 種別CD=1 THEN '㈱'+会社名"
							+ "      WHEN 種別CD=2 THEN 会社名+'㈱'"
							+ "      WHEN 種別CD=3 THEN '㈲'+会社名"
							+ "      WHEN 種別CD=4 THEN 会社名+'㈲'"
							+ "      ELSE 会社名 END AS 仕入先名"
							+ " FROM T_見積_材料 em"
							+ " LEFT OUTER JOIN T_見積_子 ec ON em.見積子ID=ec.ID and em.見積親ID=ec.見積親ID"
							+ " LEFT OUTER JOIN T_見積_親 ep ON ec.見積親ID=ep.見積親ID"
							+ " LEFT OUTER JOIN T_見積製作 eppp on ec.見積親ID=eppp.見積親ID"
							+ " LEFT OUTER JOIN T_製作_親 pp ON eppp.製作親ID=pp.製作親ID"
							+ " LEFT OUTER JOIN M_法人 c on em.仕入先CD=c.仕入先CD"
							+ " where 仕入見積FLG='true') z"
					);
				}
				boolean isFirst = true;
				for (int i = 0; i < 12; i++) {
					if (!dto.getStr(i).equals("")) { // 検索条件が入っていれば
						if (isFirst) {
							query.append(" WHERE ");
							isFirst = false;
						} else {
							if (dto.isAnd())
								query.append(" AND ");
							else
								query.append(" OR ");
						}
						if (dto.getStr(i).equals("未")) {
							switch (i) {
								case 2:
									query.append("注文年月日 IS NULL");
									break;
								case 5:
									query.append("指定納期 IS NULL");
									break;
								case 8:
									query.append("納品書日 IS NULL");
									break;
							}
						} else {
							if (i == 0) {
								String words = dto.getStr(i).replaceAll("　", " ");
								for (int j = 0; j < words.split(" ").length; j++) {
									if (j > 0) {
										if (dto.isAnd())
											query.append(" AND ");
										else
											query.append(" OR ");
									}
									query.append(constStrs[i]);
								}
							} else {
								query.append(constStrs[i]);
							}
							stringConditionIndex.add(i);
						}
					}
				}

				// 数値の検索条件は、searchDTO.getInt(0～6)
				for (int i = 0; i < 7; i++) {
					if (dto.getInt(i) != 0) { // 検索条件が入っていれば
						intConditionIndex.add(i);
						if (isFirst) {
							query.append(" WHERE " + constInts[i]);
							isFirst = false;
						} else {
							if (dto.isAnd())
								query.append(" AND " + constInts[i]);
							else
								query.append(" OR " + constInts[i]);
						}
					}
				}
				if (isStock) {
					if (isFirst) {
						query.append(" WHERE 残数>0");
					} else {
						query.append(" AND 残数>0");
					}
				}
				query.append(" order by 注文年月日 desc,種別,ID");
				ps = c.prepareStatement(query.toString());
				int j = 1;
				for (int i : stringConditionIndex) {
					if (i == 0) {
						String words = dto.getStr(i).replaceAll("　", " ");
						for (String word : words.split(" ")) {
							ps.setString(j, "%" + word + "%"); j++;
						}
					} else {
						ps.setString(j, dto.getStr(i)); j++;
					}
				}
				for (int i : intConditionIndex) {
					ps.setInt(j, dto.getInt(i)); j++;
				}
				rs = ps.executeQuery();
				while (rs.next()) {
					Vector<Object> record = new Vector<Object>();
					record.add(rs.getString("種別"));
					record.add(rs.getInt("大分類CD"));
					record.add(rs.getInt("中分類CD"));
					record.add(rs.getInt("小分類CD"));
					record.add(rs.getString("材料品名"));
					record.add(rs.getInt("数量"));
					record.add(rs.getInt("残数"));
					record.add(rs.getInt("数量単位CD"));
					record.add(rs.getDouble("重量長さ"));
					record.add(rs.getInt("単価"));
					record.add(rs.getInt("金額"));
					record.add(rs.getString("仕入先名"));
					record.add(rs.getDate("注文年月日"));
					record.add(rs.getDate("入庫年月日"));
					record.add(rs.getDate("指定納期"));
					if (rs.getInt("注文期") != 0 && rs.getInt("注文番号") != 0) {
						record.add(rs.getInt("注文期") + "-" + rs.getInt("注文番号") + " " + rs.getString("注文枝番"));
					} else {
						record.add("");
					}
					record.add(rs.getInt("伝票番号"));
					record.add(rs.getInt("納品書番号"));
					record.add(rs.getString("備考"));
					record.add(rs.getString("摘要"));
					record.add(rs.getInt("在庫親ID"));
					record.add(rs.getInt("ID"));
					data.add(record);
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
			out.writeObject(data);
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
