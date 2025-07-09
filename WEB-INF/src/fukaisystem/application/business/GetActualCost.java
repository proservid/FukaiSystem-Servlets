package fukaisystem.application.business;

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

public class GetActualCost extends GenericServlet {

	/**
	 *
	 */
	private static final long serialVersionUID = 1L;
	static final Logger lg = Logger.getLogger("A1");
	private static final String className = "GetDetail\n";

	@SuppressWarnings("unchecked")
	public void service(ServletRequest request, ServletResponse response) {

		DBConnection dbc = new DBConnection();
		Connection c = dbc.getConnection();
		PreparedStatement ps = null;
		ResultSet rs = null;

		String caption = "";
		int productionID = 0;
		int coarseCD = 0;
		int middleCD = 0;
		double qty = 0;
		int amount = 0;

		StringBuilder err = new StringBuilder("");
		Vector<String> titles = new Vector<String>();
		Vector<Vector<Object>> data = new Vector<Vector<Object>>();

		List<Integer> params = null;

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
					params = (List<Integer>) obj;
					productionID = params.get(0);
				} else {
					err.append(className + "readObjectがList型ではありません\n");
					lg.error(className + "readObjectがList型ではありません");
				}
			}

			try {
				if (params.size() > 1) {
					coarseCD = params.get(1);
					if (params.size() == 3) { // 小
						middleCD = params.get(2);
						if (coarseCD > 100) {
							ps = c.prepareStatement(
								"select wc.CD as 小分類CD,小分類名,姓+' '+名 as 仕入先名,substring(convert(varchar,着手日時,20),1,16)+'～'+substring(convert(varchar,終了日時,108),1,5) as 名,"
									+ " convert(money,時間)/100 as 数量,w.単価,w.時間*w.単価/100 as 金額 "
									+ " from T_製作_親 pp"
									+ " left outer join T_加工実績 w on pp.製作期=w.製作期 and pp.製作番号=w.製作番号 and pp.製作枝番=w.製作枝番"
									+ " left outer join M_加工_子 wc on wc.CD=w.加工CD"
									+ " left outer join M_加工_親 wp on wp.大分類CD=wc.大分類CD and wp.CD=wc.中分類CD"
									+ " left outer join M_人員 m on w.担当者CD=m.CD"
									+ " where 製作親ID=? and wp.大分類CD=? and 中分類CD=?"
									+ " order by 小分類CD"
							);
							ps.setInt(1, productionID);
							ps.setInt(2, coarseCD);
							ps.setInt(3, middleCD);
						} else {
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
									+ " where 製作親ID=? and sc.大分類CD=? and sc.中分類CD=? and 納品書日 is not null"
									+ " union all"
									+ " select 小分類CD,小分類名,'(出庫)' as 仕入先名,品名,数量,単価,金額 "
									+ " from T_製作_親 pp"
									+ " left outer join T_出庫_親 sp on pp.製作期=sp.製作期 and pp.製作番号=sp.製作番号 and pp.製作枝番=sp.製作枝番"
									+ " left outer join T_出庫_子 sc on sp.出庫親ID=sc.出庫親ID"
									+ " left outer join M_原価 c on sc.大分類CD=c.CD"
									+ " left outer join M_材料_子 mc on mc.大分類CD=sc.大分類CD and mc.中分類CD=sc.中分類CD and mc.CD=sc.小分類CD"
									+ " where 製作親ID=? and sc.大分類CD=? and sc.中分類CD=?"
									+ " order by 小分類CD"
							);
							ps.setInt(1, productionID);
							ps.setInt(2, coarseCD);
							ps.setInt(3, middleCD);
							ps.setInt(4, productionID);
							ps.setInt(5, coarseCD);
							ps.setInt(6, middleCD);
						}
						rs = ps.executeQuery();
						while (rs.next()) {
							Vector<Object> record = new Vector<Object>();
							record.add(rs.getInt("小分類CD"));
							record.add(rs.getString("小分類名"));
							record.add(rs.getString("仕入先名"));
							record.add(rs.getString("名"));
							record.add(rs.getDouble("数量"));
							record.add(rs.getInt("単価"));
							record.add(rs.getInt("金額"));
							qty += rs.getDouble("数量");
							amount += rs.getInt("金額");
							data.add(record);
						}
						Vector<Object> record = new Vector<Object>();
						record.add(0);
						record.add("");
						record.add("");
						record.add("合計");
						record.add(qty);
						record.add(0);
						record.add(amount);
						data.add(record);
						titles.add("小分類CD");
						titles.add("小分類名");
						titles.add((coarseCD > 100) ? "担当者名" : "仕入先名");
						titles.add((coarseCD > 100) ? "日時" : "品名");
						titles.add((coarseCD > 100) ? "時間" : "数量");
						titles.add("単価");
						titles.add("金額");
					} else { // 中
						if (coarseCD > 100) {
							ps = c.prepareStatement(
								"select 中分類CD,中分類名,sum(w.時間*w.単価/100) as 金額 "
									+ " from T_製作_親 pp"
									+ " left outer join T_加工実績 w on pp.製作期=w.製作期 and pp.製作番号=w.製作番号 and pp.製作枝番=w.製作枝番"
									+ " left outer join M_加工_子 wc on wc.CD=w.加工CD"
									+ " left outer join M_加工_親 wp on wp.大分類CD=wc.大分類CD and wp.CD=wc.中分類CD"
									+ " left outer join M_原価 c on wp.大分類CD=c.CD"
									+ " where 製作親ID=? and wp.大分類CD=?"
									+ " group by 中分類CD,中分類名"
									+ " order by 中分類CD"
							);
							ps.setInt(1, productionID);
							ps.setInt(2, coarseCD);
						} else {
							ps = c.prepareStatement(
								"select 中分類CD,中分類名,sum(金額) as 金額  from ("
									+ "select 中分類CD,中分類名,sum(金額) as 金額 "
									+ " from T_製作_親 pp"
									+ " left outer join T_在庫_親 sp on pp.製作期=sp.注文期 and pp.製作番号=sp.注文番号 and pp.製作枝番=sp.注文枝番"
									+ " left outer join T_在庫_子 sc on sp.在庫親ID=sc.在庫親ID"
									+ " left outer join T_指定納品書 d on sc.納品書番号=d.ID"
									+ " left outer join M_原価 c on sc.大分類CD=c.CD"
									+ " left outer join M_材料_親 mp on mp.大分類CD=sc.大分類CD and mp.CD=sc.中分類CD"
									+ " where 製作親ID=? and sc.大分類CD=? and 納品書日 is not null"
									+ " group by 中分類CD,中分類名"
									+ " union all"
									+ " select 中分類CD,中分類名,sum(金額) as 金額 "
									+ " from T_製作_親 pp"
									+ " left outer join T_出庫_親 dp on pp.製作期=dp.製作期 and pp.製作番号=dp.製作番号 and pp.製作枝番=dp.製作枝番"
									+ " left outer join T_出庫_子 dc on dp.出庫親ID=dc.出庫親ID"
									+ " left outer join M_原価 c on dc.大分類CD=c.CD"
									+ " left outer join M_材料_親 mp on mp.大分類CD=dc.大分類CD and mp.CD=dc.中分類CD"
									+ " where 製作親ID=? and dc.大分類CD=?"
									+ " group by 中分類CD,中分類名"
									+ ") z"
									+ " group by 中分類CD,中分類名"
									+ " order by 中分類CD"
							);
							ps.setInt(1, productionID);
							ps.setInt(2, coarseCD);
							ps.setInt(3, productionID);
							ps.setInt(4, coarseCD);
						}
						rs = ps.executeQuery();
						while (rs.next()) {
							Vector<Object> record = new Vector<Object>();
							record.add(rs.getInt("中分類CD"));
							record.add(rs.getString("中分類名"));
							record.add(rs.getInt("金額"));
							amount += rs.getInt("金額");
							data.add(record);
						}
						Vector<Object> record = new Vector<Object>();
						record.add(0);
						record.add("合計");
						record.add(amount);
						data.add(record);
						titles.add("中分類CD");
						titles.add("中分類名");
						titles.add("金額");
					}

				} else { // 大
					ps = c.prepareStatement(
						"select 大分類CD,case when 大分類名 is null then '(未分類)' else 大分類名 end as 大分類名,sum(金額) as 金額 from ("
							+ "select 大分類CD,大分類名,sum(金額) as 金額 "
							+ " from T_製作_親 pp"
							+ " left outer join T_在庫_親 sp on pp.製作期=sp.注文期 and pp.製作番号=sp.注文番号 and pp.製作枝番=sp.注文枝番"
							+ " left outer join T_在庫_子 sc on sp.在庫親ID=sc.在庫親ID"
							+ " left outer join T_指定納品書 d on sc.納品書番号=d.ID"
							+ " left outer join M_原価 c on sc.大分類CD=c.CD"
							+ " where 製作親ID=? and 大分類CD is not null and 納品書日 is not null"
							+ " group by 大分類CD,大分類名"
							+ " union all"
							+ " select 大分類CD,大分類名,sum(金額) as 金額 "
							+ " from T_製作_親 pp"
							+ " left outer join T_出庫_親 dp on pp.製作期=dp.製作期 and pp.製作番号=dp.製作番号 and pp.製作枝番=dp.製作枝番"
							+ " left outer join T_出庫_子 dc on dp.出庫親ID=dc.出庫親ID"
							+ " left outer join M_原価 c on dc.大分類CD=c.CD"
							+ " where 製作親ID=? and 大分類CD is not null"
							+ " group by 大分類CD,大分類名"
							+ " union all"
							+ " select 大分類CD,大分類名,sum(w.時間*w.単価/100) as 金額 "
							+ " from T_製作_親 pp"
							+ " left outer join T_加工実績 w on pp.製作期=w.製作期 and pp.製作番号=w.製作番号 and pp.製作枝番=w.製作枝番"
							+ " left outer join M_加工_子 wc on wc.CD=w.加工CD"
							+ " left outer join M_原価 c on wc.大分類CD=c.CD"
							+ " where 製作親ID=? and 大分類CD is not null"
							+ " group by 大分類CD,大分類名"
							+ ") z"
							+ " group by 大分類CD,大分類名"
							+ " order by 大分類CD"
					);
					ps.setInt(1, productionID);
					ps.setInt(2, productionID);
					ps.setInt(3, productionID);
					rs = ps.executeQuery();

					while (rs.next()) {
						Vector<Object> record = new Vector<Object>();
						// record.add(rs.getString("製作期") + "-" + rs.getString("製作番号") + rs.getString("製作枝番"));
						record.add(rs.getInt("大分類CD"));
						record.add(rs.getString("大分類名"));
						record.add(rs.getInt("金額"));
						amount += rs.getInt("金額");
						data.add(record);
					}
					Vector<Object> record = new Vector<Object>();
					record.add(0);
					record.add("合計");
					record.add(amount);
					data.add(record);
					titles.add("大分類CD");
					titles.add("大分類名");
					titles.add("金額");
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
			out.writeObject(new CostDTO(caption, amount, titles, data));
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
