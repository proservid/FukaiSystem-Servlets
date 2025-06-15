package fukaisystem;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Vector;

import javax.servlet.GenericServlet;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import org.apache.log4j.Logger;

import fukaisystem.dto.CostDTO;
import fukaisystem.sql.DBConnection;
import fukaisystem.util.Logging;

public class GetEstimatedCost extends GenericServlet {

	/**
	 *
	 */
	private static final long serialVersionUID = 1L;
	static final Logger lg = Logger.getLogger("A1");
	private static final String className = "GetEstDetail\n";

	@SuppressWarnings("unchecked")
	public void service(ServletRequest request, ServletResponse response) {

		DBConnection dbc = new DBConnection();
		Connection c = dbc.getConnection();
		PreparedStatement ps = null;
		ResultSet rs = null;

		String caption = "";
		int estimateID = 0;
		int l = 0;
		int m = 0;
		double ntotal = 0;
		int total = 0;

		StringBuilder err = new StringBuilder("");
		Vector<String> title = new Vector<String>();
		Vector<Vector<Object>> data = new Vector<Vector<Object>>();

		List<Integer> param = null;

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
				if (obj instanceof List<?>) {
					param = (List<Integer>) obj;
					estimateID = param.get(0);
				} else {
					err.append(className + "readObjectがList型ではありません\n");
					lg.error(className + "readObjectがList型ではありません");
				}
			}

			try {
				if (param.size() > 1) {
					l = param.get(1);
					if (param.size() == 3) { // 小
						m = param.get(2);
						if (l > 100) { // 加工の場合
							ps = c.prepareStatement(
								"select wc.CD as 小分類CD,小分類名,名称 as 仕入先名,'' as 名,"
									+ " convert(money,時間) as 数量,w.単価,w.時間*wc.単価 as 金額 "
									+ " from T_見積_加工 w"
									+ " left outer join M_加工_子 wc on wc.CD=w.小分類CD"
									+ " left outer join M_加工_親 wp on wp.大分類CD=wc.大分類CD and wp.CD=wc.中分類CD"
									+ " where 見積親ID=? and w.大分類CD=? and w.中分類CD=?"
									+ " order by 小分類CD"
							);
							ps.setInt(1, estimateID);
							ps.setInt(2, l);
							ps.setInt(3, m);
						} else {
							ps = c.prepareStatement(
								"select 小分類CD,小分類名,"
									+ " CASE"
									+ " WHEN 種別CD = 1 THEN '㈱'+会社名"
									+ " WHEN 種別CD = 2 THEN 会社名+'㈱'"
									+ " WHEN 種別CD = 3 THEN '㈲'+会社名"
									+ " WHEN 種別CD = 4 THEN 会社名+'㈲'"
									+ " ELSE 会社名 END AS 仕入先名,"
									+ "名称 as 名,数量,単価,単価*数量 as 金額 "
									+ " from T_見積_材料 em"
									+ " left outer join M_法人 co on em.仕入先CD=co.仕入先CD"
									+ " left outer join M_原価 c on em.大分類CD=c.CD"
									+ " left outer join M_材料_子 mc on mc.大分類CD=em.大分類CD and mc.中分類CD=em.中分類CD and mc.CD=em.小分類CD"
									+ " where 見積親ID=? and em.大分類CD=? and em.中分類CD=?"
									+ " order by 小分類CD"
							);
							ps.setInt(1, estimateID);
							ps.setInt(2, l);
							ps.setInt(3, m);
						}
						rs = ps.executeQuery();
						while (rs.next()) {
							Vector<Object> v = new Vector<Object>();
							v.add(rs.getInt("小分類CD"));
							v.add(rs.getString("小分類名"));
							v.add(rs.getString("仕入先名"));
							v.add(rs.getString("名"));
							v.add(rs.getDouble("数量"));
							v.add(rs.getInt("単価"));
							v.add(rs.getInt("金額"));
							ntotal += rs.getDouble("数量");
							total += rs.getInt("金額");
							data.add(v);
						}
						Vector<Object> v = new Vector<Object>();
						v.add(0);
						v.add("");
						v.add("");
						v.add("合計");
						v.add(ntotal);
						v.add(0);
						v.add(total);
						data.add(v);
						title.add("小分類CD");
						title.add("小分類名");
						title.add((l > 100) ? "担当者名" : "仕入先名");
						title.add((l > 100) ? "日時" : "品名");
						title.add((l > 100) ? "時間" : "数量");
						title.add("単価");
						title.add("金額");
					} else { // 中
						if (l > 100) {
							ps = c.prepareStatement(
								"select w.中分類CD,中分類名,sum(w.時間*wc.単価) as 金額 "
									+ " from T_見積_加工 w"
									+ " left outer join M_加工_子 wc on wc.CD=w.小分類CD"
									+ " left outer join M_加工_親 wp on wp.大分類CD=wc.大分類CD and wp.CD=wc.中分類CD"
									+ " left outer join M_原価 c on wp.大分類CD=c.CD"
									+ " where 見積親ID=? and wp.大分類CD=?"
									+ " group by w.中分類CD,中分類名"
									+ " order by 中分類CD"
							);
							ps.setInt(1, estimateID);
							ps.setInt(2, l);
						} else {
							ps = c.prepareStatement(
								"select 中分類CD,中分類名,sum(単価*数量) as 金額 "
									+ " from T_見積_材料 em"
									+ " left outer join M_原価 c on em.大分類CD=c.CD"
									+ " left outer join M_材料_親 mp on mp.大分類CD=em.大分類CD and mp.CD=em.中分類CD"
									+ " where 見積親ID=? and em.大分類CD=?"
									+ " group by 中分類CD,中分類名"
									+ " order by 中分類CD"
							);
							ps.setInt(1, estimateID);
							ps.setInt(2, l);
						}
						rs = ps.executeQuery();
						while (rs.next()) {
							Vector<Object> v = new Vector<Object>();
							v.add(rs.getInt("中分類CD"));
							v.add(rs.getString("中分類名"));
							v.add(rs.getInt("金額"));
							total += rs.getInt("金額");
							data.add(v);
						}
						Vector<Object> v = new Vector<Object>();
						v.add(0);
						v.add("合計");
						v.add(total);
						data.add(v);
						title.add("中分類CD");
						title.add("中分類名");
						title.add("金額");
					}

				} else { // 大
					ps = c.prepareStatement(
						"select 大分類CD,case when 大分類名 is null then '(未分類)' else 大分類名 end as 大分類名,sum(金額) as 金額 from ("
							+ "select 大分類CD,大分類名,sum(単価*数量) as 金額 "
							+ " from T_見積_材料 em"
							+ " left outer join M_原価 c on em.大分類CD=c.CD"
							+ " where 見積親ID=? and 大分類CD is not null "
							+ " group by 大分類CD,大分類名"
							+ "  union all"
							+ " select 大分類CD,大分類名,sum(単価*時間) as 金額 "
							+ " from T_見積_加工 ew"
							+ " left outer join M_原価 c on ew.大分類CD=c.CD"
							+ " where 見積親ID=? and 大分類CD is not null  "
							+ " group by 大分類CD,大分類名"
							+ "  ) z"
							+ " group by 大分類CD,大分類名"
							+ " order by 大分類CD"
					);
					ps.setInt(1, estimateID);
					ps.setInt(2, estimateID);
					rs = ps.executeQuery();

					while (rs.next()) {
						Vector<Object> v = new Vector<Object>();
						// v.add(rs.getString("製作期") + "-" +
						// rs.getString("製作番号") + rs.getString("製作枝番"));
						v.add(rs.getInt("大分類CD"));
						v.add(rs.getString("大分類名"));
						v.add(rs.getInt("金額"));
						total += rs.getInt("金額");
						data.add(v);
					}
					Vector<Object> v = new Vector<Object>();
					v.add(0);
					v.add("合計");
					v.add(total);
					data.add(v);
					title.add("大分類CD");
					title.add("大分類名");
					title.add("金額");
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
			out.writeObject(new CostDTO(caption, total, title, data));
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
