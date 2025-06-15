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

public class GetExpectedCost extends GenericServlet {

	/**
	 *
	 */
	private static final long serialVersionUID = 1L;
	static final Logger lg = Logger.getLogger("A1");
	private static final String className = "GetDetail2\n";

	@SuppressWarnings("unchecked")
	public void service(ServletRequest request, ServletResponse response) {

		DBConnection dbc = new DBConnection();
		Connection c = dbc.getConnection();
		PreparedStatement ps = null;
		ResultSet rs = null;

		String caption = "";
		int productID = 0;
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
					productID = param.get(0);
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
						ps = c.prepareStatement(
							"select 小分類CD,小分類名,"
								+ " CASE"
								+ " WHEN 種別CD = 1 THEN '㈱'+会社名"
								+ " WHEN 種別CD = 2 THEN 会社名+'㈱'"
								+ " WHEN 種別CD = 3 THEN '㈲'+会社名"
								+ " WHEN 種別CD = 4 THEN 会社名+'㈲'"
								+ " ELSE 会社名 END AS 仕入先名,"
								+ "材料品名 as 名,数量,単価,金額 "
								+ " from T_製作_親 pp"
								+ " left outer join T_在庫_親 sp on pp.製作期=sp.注文期 and pp.製作番号=sp.注文番号 and pp.製作枝番=sp.注文枝番"
								+ " left outer join T_在庫_子 sc on sp.在庫親ID=sc.在庫親ID"
								+ " left outer join M_法人 co on sp.仕入先CD=co.仕入先CD"
								+ " left outer join T_指定納品書 d on sc.納品書番号=d.ID"
								+ " left outer join M_原価 c on sc.大分類CD=c.CD"
								+ " left outer join M_材料_子 mc on mc.大分類CD=sc.大分類CD and mc.中分類CD=sc.中分類CD and mc.CD=sc.小分類CD"
								+ " where 製作親ID=? and sc.大分類CD=? and sc.中分類CD=? and 納品書日 is null and 表示CD=2"
								+ " order by 小分類CD"
						);
						ps.setInt(1, productID);
						ps.setInt(2, l);
						ps.setInt(3, m);

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
						title.add("仕入先名");
						title.add("品名");
						title.add("数量");
						title.add("単価");
						title.add("金額");
					} else { // 中
						ps = c.prepareStatement(
							"select 中分類CD,中分類名,sum(金額) as 金額 "
								+ " from T_製作_親 pp"
								+ " left outer join T_在庫_親 sp on pp.製作期=sp.注文期 and pp.製作番号=sp.注文番号 and pp.製作枝番=sp.注文枝番"
								+ " left outer join T_在庫_子 sc on sp.在庫親ID=sc.在庫親ID"
								+ " left outer join T_指定納品書 d on sc.納品書番号=d.ID"
								+ " left outer join M_原価 c on sc.大分類CD=c.CD"
								+ " left outer join M_材料_親 mp on mp.大分類CD=sc.大分類CD and mp.CD=sc.中分類CD"
								+ " where 製作親ID=? and sc.大分類CD=? and 納品書日 is null and 表示CD=2"
								+ " group by 中分類CD,中分類名"
								+ " order by 中分類CD"
						);
						ps.setInt(1, productID);
						ps.setInt(2, l);

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
						"select 大分類CD,case when 大分類名 is null then '(未分類)' else 大分類名 end as 大分類名,sum(金額) as 金額"
							+ " from T_製作_親 pp"
							+ " left outer join T_在庫_親 sp on pp.製作期=sp.注文期 and pp.製作番号=sp.注文番号 and pp.製作枝番=sp.注文枝番"
							+ " left outer join T_在庫_子 sc on sp.在庫親ID=sc.在庫親ID"
							+ " left outer join T_指定納品書 d on sc.納品書番号=d.ID"
							+ " left outer join M_原価 c on sc.大分類CD=c.CD"
							+ " where 製作親ID=? and 大分類CD is not null and 納品書日 is null and 表示CD=2 AND 納品書番号>-1"
							+ " group by 大分類CD,大分類名"
							+ " order by 大分類CD"
					);
					ps.setInt(1, productID);
					rs = ps.executeQuery();

					while (rs.next()) {
						Vector<Object> v = new Vector<Object>();
						// v.add(rs.getString("製作期") + "-" + rs.getString("製作番号") + rs.getString("製作枝番"));
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
