package fukaisystem.application.business;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Vector;

import javax.servlet.ServletResponse;

import fukaisystem.dto.business.CostDTO;
import fukaisystem.foundation.ServiceFoundation;

/**
 * 見積もり原価を取得する
 */
public class GetEstimatedCost extends ServiceFoundation {

	@Override
	@SuppressWarnings("unchecked")
	public Object access(Connection c, ServletResponse response, Object o) throws IOException, SQLException {

		String caption = "";
		int quotationID = 0;
		int coarseCD = 0;
		int middleCD = 0;
		double qty = 0;
		int amount = 0;

		Vector<String> titles = new Vector<String>();
		Vector<Vector<Object>> data = new Vector<Vector<Object>>();
		List<Integer> params = cast(response, o, List.class);
		quotationID = params.get(0);
		if (params.size() > 1) { // 中分類コードが指定されている
			coarseCD = params.get(1);
			if (params.size() == 3) { // 小分類コードも指定されている＝小分類を取得
				middleCD = params.get(2);
				String query;
				if (coarseCD > 100) { // コード 101 以上は加工費
					query = "select wc.CD as 小分類CD,小分類名,名称 as 仕入先名,'' as 名,"
						+ " convert(money,時間) as 数量,w.単価,w.時間*wc.単価 as 金額 "
						+ " from T_見積_加工 w"
						+ " left outer join M_加工_子 wc on wc.CD=w.小分類CD"
						+ " left outer join M_加工_親 wp on wp.大分類CD=wc.大分類CD and wp.CD=wc.中分類CD"
						+ " where 見積親ID=? and w.大分類CD=? and w.中分類CD=?"
						+ " order by 小分類CD";
				} else { // コード 100 以下は材料費
					query = "select 小分類CD,小分類名,"
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
						+ " order by 小分類CD";
				}
				try (PreparedStatement ps = c.prepareStatement(query);) {
					ps.setInt(1, quotationID);
					ps.setInt(2, coarseCD);
					ps.setInt(3, middleCD);
					try (ResultSet rs = ps.executeQuery();) {
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
					}
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
			} else { // 中分類を取得
				String query;
				if (coarseCD > 100) { // コード 101 以上は加工費
					query = "select w.中分類CD,中分類名,sum(w.時間*wc.単価) as 金額 "
						+ " from T_見積_加工 w"
						+ " left outer join M_加工_子 wc on wc.CD=w.小分類CD"
						+ " left outer join M_加工_親 wp on wp.大分類CD=wc.大分類CD and wp.CD=wc.中分類CD"
						+ " left outer join M_原価 c on wp.大分類CD=c.CD"
						+ " where 見積親ID=? and wp.大分類CD=?"
						+ " group by w.中分類CD,中分類名"
						+ " order by 中分類CD";
				} else { // コード 100 以下は材料費
					query = "select 中分類CD,中分類名,sum(単価*数量) as 金額 "
						+ " from T_見積_材料 em"
						+ " left outer join M_原価 c on em.大分類CD=c.CD"
						+ " left outer join M_材料_親 mp on mp.大分類CD=em.大分類CD and mp.CD=em.中分類CD"
						+ " where 見積親ID=? and em.大分類CD=?"
						+ " group by 中分類CD,中分類名"
						+ " order by 中分類CD";
				}
				try (PreparedStatement ps = c.prepareStatement(query);) {
					ps.setInt(1, quotationID);
					ps.setInt(2, coarseCD);
					try (ResultSet rs = ps.executeQuery();) {
						while (rs.next()) {
							Vector<Object> record = new Vector<Object>();
							record.add(rs.getInt("中分類CD"));
							record.add(rs.getString("中分類名"));
							record.add(rs.getInt("金額"));
							amount += rs.getInt("金額");
							data.add(record);
						}
					}
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

		} else { // 大分類コードのみ指定されている＝大分類を取得
			try (
				PreparedStatement ps = c.prepareStatement(
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
			) {
				ps.setInt(1, quotationID);
				ps.setInt(2, quotationID);
				try (ResultSet rs = ps.executeQuery();) {
					while (rs.next()) {
						Vector<Object> record = new Vector<Object>();
						record.add(rs.getInt("大分類CD"));
						record.add(rs.getString("大分類名"));
						record.add(rs.getInt("金額"));
						amount += rs.getInt("金額");
						data.add(record);
					}
				}
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
		return new CostDTO(caption, amount, titles, data);
	}
}
