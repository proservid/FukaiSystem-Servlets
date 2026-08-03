package fukaisystem.application.aggregate;

import java.io.IOException;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.Vector;

import javax.servlet.ServletResponse;

import fukaisystem.foundation.ServiceFoundation;

/**
 * 棚を取得するためのクラス
 * 導入時の在庫 43-8014、47-8009 と、50期以降のものを対象とする
 * 
 * @author kameura
 *
 */
public class GetShelf2 extends ServiceFoundation {

	@Override
	public Object access(Connection c, ServletResponse response, Object o) throws IOException, SQLException {

		Map<Integer, Map<String, Shelf>> shelfMaps = new HashMap<Integer, Map<String, Shelf>>();
		for (int i = 1; i < 10; i++) { // 10～90
			Map<String, Shelf> shelfMap = new TreeMap<String, Shelf>();
			shelfMaps.put(i, shelfMap);
		}
		List<Seiban> added = new ArrayList<Seiban>();
		Vector<Vector<Object>> data = new Vector<Vector<Object>>();

		Date from = cast(response, o, Date.class); // 売り集計の初日
		Calendar target = Calendar.getInstance();
		target.setTime(from); // 今月1日
		// int month = target.get(Calendar.MONTH) + 1;
		target.add(Calendar.MONTH, 1); // 翌月1日
		Date to = new Date(target.getTimeInMillis()); // 翌月売り集計の初日
		target.setTime(from); // 今月1日（次の2014年判定のためにこのタイミングでセットする必要がある）

		Date from2 = null; // 注文集計の初日
		Date to2 = null; // 翌月注文集計の初日

		if (target.get(Calendar.YEAR) < 2014) { // 2013年以前は25日〆
			if (target.get(Calendar.MONTH) == 11) { // 12月は11月26日から12月31日
				to2 = to;
				target.add(Calendar.DATE, 25); // 今月26日
				target.add(Calendar.MONTH, -1); // 先月26日
				from2 = new Date(target.getTimeInMillis());
			} else {
				target.add(Calendar.DATE, 25); // 今月26日
				to2 = new Date(target.getTimeInMillis());
				if (target.get(Calendar.MONTH) == 0) { // 1月は1月1日から1月25日
					from2 = from;
				} else {
					target.add(Calendar.MONTH, -1); // 先月26日
					from2 = new Date(target.getTimeInMillis());
				}
			}
		} else { // 2014年以後は月末〆
			to2 = to;
			from2 = from;
		}

		/////////////////////////////////////////////////////////////////////////////////////
		// 当月以降の売上に対応する集計
		/////////////////////////////////////////////////////////////////////////////////////
		// long t2 = System.currentTimeMillis();
		// System.out.println("a:"+(t2-t1));t1=t2;

		// 前月までに売上になっていない製番を抽出（ * は今月売上、今月も売り上げになっていなければ無印）
		// まずはこの製番に対応するもののみを集計する
		String sql = "select "
			+ "	floor(製作番号/1000) as 台, convert(varchar, 製作期)+'-'+convert(varchar, 製作番号)+製作枝番 as 製番,"
			+ "	case when (売上年月日>=? and 売上年月日<?) and (納品区分CD=2 or 納品区分CD=4 or 納品区分CD=5 or 納品区分CD=6) then '*'"
			+ "		else '' end as 売"
			+ " from (select * from T_製作_親 where (製作期>50 or (製作期=43 and 製作番号=8014) or (製作期=47 and 製作番号=8009)) and 製作番号<>0) pp"
			+ " left outer join (select 売上親ID, 製作親ID from T_売上_子 group by 売上親ID, 製作親ID) sc on pp.製作親ID=sc.製作親ID"
			+ " left outer join T_売上_親 sp on sc.売上親ID=sp.売上親ID"
			+ "	where 売上年月日>=? or 売上年月日 is null";
		try (PreparedStatement ps = c.prepareStatement(sql);) {
			int n = 1;
			ps.setDate(n++, from); // 売
			ps.setDate(n++, to);
			ps.setDate(n++, from);
			ResultSet rs = ps.executeQuery();
			while (rs.next()) {
				if (shelfMaps.containsKey(rs.getInt("台"))) {
					shelfMaps.get(rs.getInt("台")).put(rs.getString("製番"), new Shelf(rs.getString("売")));
				}
			}
		}

		// t2 = System.currentTimeMillis();
		// System.out.println("b:"+(t2-t1));t1=t2;
		// 仕入の繰越額
		sql = "select"
			+ "	 floor(注文番号/1000) as 台, convert(varchar, 注文期)+'-'+convert(varchar, 注文番号)+注文枝番 as 注番, sum(oc.金額) as 合計額"
			+ "	 from (select * from T_在庫_親 z where exists ("
			+ "    select * from (select * from T_製作_親 where (製作期>50 or (製作期=43 and 製作番号=8014) or (製作期=47 and 製作番号=8009)) and 製作番号<>0) pp"
			+ "    left outer join ("
			+ "	     select 売上親ID, 製作親ID from T_売上_子 group by 売上親ID, 製作親ID) sc on sc.製作親ID=pp.製作親ID"
			+ "		 left outer join T_売上_親 sp on sc.売上親ID=sp.売上親ID"
			+ "		 where z.注文期=製作期 and z.注文番号=製作番号 and z.注文枝番=製作枝番 and (売上年月日>=? or 売上年月日 is null))"
			+ "	) op"
			+ "	left outer join T_在庫_子 oc on oc.在庫親ID=op.在庫親ID"
			+ "	left outer join T_指定納品書 s on oc.納品書番号=s.ID"
			+ "	where 納品書日<?"
			+ "	group by 注文期, 注文番号, 注文枝番";
		try (PreparedStatement ps = c.prepareStatement(sql);) {
			int n = 1;
			ps.setDate(n++, from2);
			ps.setDate(n++, from);
			ResultSet rs = ps.executeQuery();
			while (rs.next()) {
				Map<String, Shelf> shelfMap = shelfMaps.get(rs.getInt("台"));
				if (shelfMap != null) {
					if (shelfMap.containsKey(rs.getString("注番"))) {
						Shelf shelf = shelfMap.get(rs.getString("注番"));
						shelf.setCarriedO(rs.getInt("合計額"));
					}
				}
			}
		}

		// t2 = System.currentTimeMillis();
		// System.out.println("c2:"+(t2-t1));t1=t2;
		// 出庫の繰越額
		sql = "select floor(d.製作番号/1000) as 台, convert(varchar, d.製作期)+'-'+convert(varchar, d.製作番号)+d.製作枝番 as 出庫番, sum(d.金額) as 合計額"
			+ "		 from ("
			+ "			   select dc.出庫親ID,製作期,製作番号,製作枝番,在庫親ID,出庫年月日,金額 from T_出庫_子 dc"
			+ "			   left outer join T_出庫_親 dp on dc.出庫親ID=dp.出庫親ID) d"
			+ "			 left outer join T_製作_親 pd on d.在庫親ID=pd.製作親ID"
			+ "			 left outer join (select 売上親ID, 製作親ID from T_売上_子 group by 売上親ID, 製作親ID) sc on pd.製作親ID=sc.製作親ID"
			+ "			 where 出庫年月日<? and exists ("
			+ "				select *"
			+ "				from (select * from T_製作_親 where 製作期>50 or (製作期=43 and 製作番号=8014) or (製作期=47 and 製作番号=8009) and 製作番号<>0) pp"
			+ "				left outer join (select 売上親ID, 製作親ID from T_売上_子 group by 売上親ID, 製作親ID) sc on pp.製作親ID=sc.製作親ID"
			+ "				left outer join T_売上_親 sp on sc.売上親ID=sp.売上親ID"
			+ "				where (売上年月日>=? or 売上年月日 is null) and d.製作期=pp.製作期 and d.製作番号=pp.製作番号 and d.製作枝番=pp.製作枝番"
			+ "			 )"
			+ "			 group by d.製作期, d.製作番号, d.製作枝番";
		try (PreparedStatement ps = c.prepareStatement(sql);) {
			int n = 1;
			ps.setDate(n++, from2);
			ps.setDate(n++, from);
			ResultSet rs = ps.executeQuery();
			while (rs.next()) {
				Map<String, Shelf> shelfMap = shelfMaps.get(rs.getInt("台"));
				if (shelfMap != null) {
					if (shelfMap.containsKey(rs.getString("出庫番"))) {
						Shelf shelf = shelfMap.get(rs.getString("出庫番"));
						shelf.addCarriedD(rs.getInt("合計額"));
					}
				}
			}
		}

		// t2 = System.currentTimeMillis();
		// System.out.println("d:"+(t2-t1));t1=t2;
		// 出庫の繰越額から９０伝票の出庫された標準原価をマイナスする
		sql = "select floor(pd.製作番号/1000) as 台, convert(varchar, pd.製作期)+'-'+convert(varchar, pd.製作番号)+pd.製作枝番 as 出庫番,"
			+ "	sum(dc.金額)*-1 as 合計額"
			+ " from T_出庫_子 dc"
			+ " left outer join T_出庫_親 dp on dc.出庫親ID=dp.出庫親ID"
			+ " left outer join T_製作_親 pd on dc.在庫親ID=pd.製作親ID"
			+ " left outer join (select 売上親ID, 製作親ID from T_売上_子 group by 売上親ID, 製作親ID) sc on pd.製作親ID=sc.製作親ID"
			+ " left outer join T_売上_親 sp on sc.売上親ID=sp.売上親ID"
			+ " where 出庫年月日<? and"
			+ "	 (convert(varchar, pd.製作期)+'-'+convert(varchar, pd.製作番号)+pd.製作枝番 in ("
			+ "		select "
			+ "			convert(varchar, pp.製作期)+'-'+convert(varchar, pp.製作番号)+pp.製作枝番 as 製番"
			+ "		from T_製作_親 pp"
			+ "		left outer join (select 売上親ID, 製作親ID from T_売上_子 group by 売上親ID, 製作親ID) sc on pp.製作親ID=sc.製作親ID"
			+ "		left outer join T_売上_親 sp on sc.売上親ID=sp.売上親ID"
			+ "			where"
			+ "			(売上年月日>=? or 売上年月日 is null)"
			+ "			and (pp.製作期>50 or (pp.製作期=43 and pp.製作番号=8014) or (pp.製作期=47 and pp.製作番号=8009)) and pp.製作番号<>0"
			+ "	 ))"
			+ " group by pd.製作番号, convert(varchar, pd.製作期)+'-'+convert(varchar, pd.製作番号)+pd.製作枝番";
		try (PreparedStatement ps = c.prepareStatement(sql);) {
			int n = 1;
			ps.setDate(n++, from2);
			ps.setDate(n++, from);
			ResultSet rs = ps.executeQuery();
			while (rs.next()) {
				Map<String, Shelf> shelfMap = shelfMaps.get(rs.getInt("台"));
				if (shelfMap != null) {
					if (shelfMap.containsKey(rs.getString("出庫番"))) {
						Shelf shelf = shelfMap.get(rs.getString("出庫番"));
						shelf.addCarriedD(rs.getInt("合計額"));
					}
				}
			}
		}

		// t2 = System.currentTimeMillis();
		// System.out.println("e:"+(t2-t1));t1=t2;
		// 当月仕入
		sql = "select floor(注文番号/1000) as 台, 注文期, 注文番号, 注文枝番,"
			+ " convert(varchar, 注文期)+'-'+convert(varchar, 注文番号)+注文枝番 as 注番,"
			+ "	sum(oc.金額) as 合計額 from T_在庫_子 oc"
			+ " left outer join T_在庫_親 op on oc.在庫親ID=op.在庫親ID"
			+ " left outer join T_指定納品書 s on oc.納品書番号=s.ID"
			+ " left outer join T_製作_親 pp on op.注文期=pp.製作期 and op.注文番号=pp.製作番号 and op.注文枝番=pp.製作枝番"
			+ " where 納品書日>=? and 納品書日<? and 製作親ID is not null"
			+ " group by 注文期, 注文番号, 注文枝番";
		try (PreparedStatement ps = c.prepareStatement(sql);) {
			int n = 1;
			ps.setDate(n++, from2);
			ps.setDate(n++, to2);
			ResultSet rs = ps.executeQuery();
			while (rs.next()) {
				Map<String, Shelf> shelfMap = shelfMaps.get(rs.getInt("台"));
				if (shelfMap != null) {
					if (shelfMap.containsKey(rs.getString("注番"))) {
						Shelf shelf = shelfMap.get(rs.getString("注番"));
						shelf.setO(rs.getInt("合計額"));
					} else { // 前月までの売上に対応する仕入ならいったん保留にしてあとで集計する
						shelfMap.put(rs.getString("注番"), new Shelf("#"));
						added.add(new Seiban(rs.getInt("注文期"), rs.getInt("注文番号"), rs.getString("注文枝番")));
					}
				}
			}
		}

		// t2 = System.currentTimeMillis();
		// System.out.println("f:"+(t2-t1));t1=t2;
		// 当月出庫
		sql = "select floor(dp.製作番号/1000) as 台, dp.製作期, dp.製作番号, dp.製作枝番,"
			+ "convert(varchar, dp.製作期)+'-'+convert(varchar, dp.製作番号)+dp.製作枝番 as 出庫番,"
			+ "	sum(dc.金額) as 合計額 from (select * from T_出庫_親 where 出庫年月日>=? and 出庫年月日<?) dp"
			+ " left outer join T_出庫_子 dc on dc.出庫親ID=dp.出庫親ID"
			+ " left outer join T_製作_親 pp on dp.製作期=pp.製作期 and dp.製作番号=pp.製作番号 and dp.製作枝番=pp.製作枝番"
			+ " where 製作親ID is not null"
			+ " group by dp.製作期, dp.製作番号, dp.製作枝番";
		try (
			PreparedStatement ps = c.prepareStatement(sql);
		) {
			int n = 1;
			ps.setDate(n++, from2);
			ps.setDate(n++, to2);
			ResultSet rs = ps.executeQuery();
			while (rs.next()) {
				Map<String, Shelf> shelfMap = shelfMaps.get(rs.getInt("台"));
				if (shelfMap != null) {
					if (shelfMap.containsKey(rs.getString("出庫番"))) {
						Shelf shelf = shelfMap.get(rs.getString("出庫番"));
						if (!shelf.getS().equals("#")) { // 後で集計する保留製番が混じらないように
							shelf.addD(rs.getInt("合計額"));
						}
					} else { // 前月までの売上に対応する出庫ならいったん保留にしてあとで集計する
						shelfMap.put(rs.getString("出庫番"), new Shelf("#"));
						added.add(new Seiban(rs.getInt("製作期"), rs.getInt("製作番号"), rs.getString("製作枝番")));
					}
				}
			}
		}

		// t2 = System.currentTimeMillis();
		// System.out.println("g:"+(t2-t1));
		// 当月出庫額から９０伝票の出庫された標準原価をマイナスする
		sql = "select floor(pd.製作番号/1000) as 台, pd.製作期, pd.製作番号, pd.製作枝番,"
			+ " convert(varchar, pd.製作期)+'-'+convert(varchar, pd.製作番号)+pd.製作枝番 as 出庫番,"
			+ "	sum(dc.金額)*-1 as 合計額 from (select * from T_出庫_親 where 出庫年月日>=? and 出庫年月日<?) dp"
			+ " left outer join T_出庫_子 dc on dc.出庫親ID=dp.出庫親ID"
			+ " left outer join T_製作_親 pd on dc.在庫親ID=pd.製作親ID"
			+ " where 製作親ID is not null"
			+ " group by pd.製作期, pd.製作番号, pd.製作枝番";
		try (PreparedStatement ps = c.prepareStatement(sql);) {
			int n = 1;
			ps.setDate(n++, from2);
			ps.setDate(n++, to2);
			ResultSet rs = ps.executeQuery();
			while (rs.next()) {
				Map<String, Shelf> shelfMap = shelfMaps.get(rs.getInt("台"));
				if (shelfMap != null) {
					if (shelfMap.containsKey(rs.getString("出庫番"))) {
						Shelf shelf = shelfMap.get(rs.getString("出庫番"));
						if (!shelf.getS().equals("#")) { // 後で集計する保留製番が混じらないように
							shelf.addD(rs.getInt("合計額"));
						}
					} else { // 前月までの売上に対応する出庫ならいったん保留にしてあとで集計する
						shelfMap.put(rs.getString("出庫番"), new Shelf("#"));
						added.add(new Seiban(rs.getInt("製作期"), rs.getInt("製作番号"), rs.getString("製作枝番")));
					}
				}
			}
		}

		// t2 = System.currentTimeMillis();
		// System.out.println("h:"+(t2-t1));
		// 当月工数
		sql = "select"
			+ "	floor(w.製作番号/1000) as 台, w.製作期, w.製作番号, w.製作枝番,"
			+ " convert(varchar, w.製作期)+'-'+convert(varchar, w.製作番号)+w.製作枝番 as 製番,"
			+ "	convert(varchar,convert(money,sum(時間))/100) as 工数"
			+ " from (select * from T_加工実績 where 製作期<>0 and 製作番号<>0 and 着手日時>=? and 着手日時<?) w"
			+ " left outer join T_製作_親 pp on w.製作期=pp.製作期 and w.製作番号=pp.製作番号 and w.製作枝番=pp.製作枝番"
			+ " where 製作親ID is not null"
			+ " group by w.製作期, w.製作番号, w.製作枝番";
		try (
			PreparedStatement ps = c.prepareStatement(sql);
		) {
			int n = 1;
			ps.setDate(n++, from);
			ps.setDate(n++, to);
			ResultSet rs = ps.executeQuery();
			while (rs.next()) {
				Map<String, Shelf> shelfMap = shelfMaps.get(rs.getInt("台"));
				if (shelfMap != null) {
					if (shelfMap.containsKey(rs.getString("製番"))) {
						Shelf shelf = shelfMap.get(rs.getString("製番"));
						if (!shelf.getS().equals("#")) { // 後で集計する保留製番が混じっても上書きになるので問題はないが一応
							shelf.setW(rs.getString("工数"));
						}
					} else { // 前月までの売上に対応する工数ならいったん保留にしてあとで集計する
						shelfMap.put(rs.getString("製番"), new Shelf("#"));
						added.add(new Seiban(rs.getInt("製作期"), rs.getInt("製作番号"), rs.getString("製作枝番")));
					}
				}
			}
		}

		// t2 = System.currentTimeMillis();
		// System.out.println("i:"+(t2-t1));t1=t2;
		// 工数累計
		sql = "select"
			+ "	floor(w.製作番号/1000) as 台, w.製作期, w.製作番号, w.製作枝番,"
			+ " convert(varchar, w.製作期)+'-'+convert(varchar, w.製作番号)+w.製作枝番 as 製番,"
			+ "	convert(varchar,convert(money,sum(時間))/100) as 工数"
			+ " from (select * from T_加工実績 where 着手日時<?) w"
			+ " where"
			+ "	 (convert(varchar, w.製作期)+'-'+convert(varchar, w.製作番号)+w.製作枝番 in ("
			+ "		select "
			+ "			convert(varchar, pp.製作期)+'-'+convert(varchar, pp.製作番号)+pp.製作枝番 as 製番"
			+ "		from T_製作_親 pp"
			+ "		left outer join (select 売上親ID, 製作親ID from T_売上_子 group by 売上親ID, 製作親ID) sc on pp.製作親ID=sc.製作親ID"
			+ "		left outer join T_売上_親 sp on sc.売上親ID=sp.売上親ID"
			+ "			where"
			+ "			(売上年月日>=? or 売上年月日 is null)"
			+ "			and (pp.製作期>50 or (pp.製作期=43 and pp.製作番号=8014) or (pp.製作期=47 and pp.製作番号=8009)) and pp.製作番号<>0"
			+ "	 ))"
			+ " group by w.製作期, w.製作番号, w.製作枝番";
		try (
			PreparedStatement ps = c.prepareStatement(sql);
		) {
			int n = 1;
			ps.setDate(n++, to);
			ps.setDate(n++, from);
			ResultSet rs = ps.executeQuery();
			while (rs.next()) {
				Map<String, Shelf> shelfMap = shelfMaps.get(rs.getInt("台"));
				if (shelfMap != null) {
					if (shelfMap.containsKey(rs.getString("製番"))) {
						Shelf shelf = shelfMap.get(rs.getString("製番"));
						if (!shelf.getS().equals("#")) { // 後で集計する保留製番が混じっても上書きになるので問題はないが一応
							shelf.setCarriesW(rs.getString("工数"));
						}
					} else { // 前月までの売上に対応する工数ならいったん保留にしてあとで集計する
						shelfMap.put(rs.getString("製番"), new Shelf("#"));
						added.add(new Seiban(rs.getInt("製作期"), rs.getInt("製作番号"), rs.getString("製作枝番")));
					}
				}
			}
		}

		/////////////////////////////////////////////////////////////////////////////////////
		// 前月までの売上に対応する集計
		/////////////////////////////////////////////////////////////////////////////////////
		// t2 = System.currentTimeMillis();
		// System.out.println("j:"+(t2-t1));t1=t2;
		// 仕入の繰越額
		if (added.size() > 0) {
			StringBuilder sb = new StringBuilder(
				"select floor(注文番号/1000) as 台, convert(varchar, 注文期)+'-'+convert(varchar, 注文番号)+注文枝番 as 注番,"
					+ "	sum(金額) as 合計額 from"
					+ "  (select 在庫親ID, 注文期, 注文番号, 注文枝番 from T_在庫_親 where"
			);
			for (int i = 0; i < added.size(); i++) {
				if (i > 0)
					sb.append(" OR");
				sb.append(" (注文期=? and 注文番号=? and 注文枝番=?)");
			}
			sb.append(
				") op"
					+ " left outer join T_在庫_子 oc on oc.在庫親ID=op.在庫親ID"
					+ " left outer join T_指定納品書 s on oc.納品書番号=s.ID"
					+ " where 納品書日<?"
			);
			sb.append(" group by 注文期, 注文番号, 注文枝番");
			try (
				PreparedStatement ps = c.prepareStatement(sb.toString());
			) {
				int n = 1;
				for (Seiban num : added) {
					ps.setInt(n++, num.getPeriod());
					ps.setInt(n++, num.getNumber());
					ps.setString(n++, num.getBranch());
				}
				ps.setDate(n++, from2);
				ResultSet rs = ps.executeQuery();
				while (rs.next()) {
					Map<String, Shelf> shelfMap = shelfMaps.get(rs.getInt("台"));
					if (shelfMap != null) {
						if (shelfMap.containsKey(rs.getString("注番"))) {
							Shelf shelf = shelfMap.get(rs.getString("注番"));
							shelf.setCarriedO(rs.getInt("合計額"));
						}
					}
				}
			}

			// t2 = System.currentTimeMillis();
			// System.out.println("k:"+(t2-t1));t1=t2;
			// 出庫の繰越額
			sb = new StringBuilder(
				"select floor(dp.製作番号/1000) as 台,"
					+ " convert(varchar, dp.製作期)+'-'+convert(varchar, dp.製作番号)+dp.製作枝番 as 出庫番,"
					+ "	sum(dc.金額) as 合計額"
					+ " from (select * from T_出庫_親 where 出庫年月日<?) dp"
					+ " left outer join T_出庫_子 dc on dc.出庫親ID=dp.出庫親ID"
					+ " left outer join T_製作_親 pd on dc.在庫親ID=pd.製作親ID"
					+ " left outer join (select 売上親ID, 製作親ID from T_売上_子 group by 売上親ID, 製作親ID) sc on pd.製作親ID=sc.製作親ID"
					+ " where"
			);
			for (int i = 0; i < added.size(); i++) {
				if (i > 0)
					sb.append(" OR");
				sb.append(" (dp.製作期=? and dp.製作番号=? and dp.製作枝番=?)");
			}
			sb.append(" group by dp.製作期, dp.製作番号, dp.製作枝番");
			try (
				PreparedStatement ps = c.prepareStatement(sb.toString());
			) {
				int n = 1;
				ps.setDate(n++, from2);
				for (Seiban num : added) {
					ps.setInt(n++, num.getPeriod());
					ps.setInt(n++, num.getNumber());
					ps.setString(n++, num.getBranch());
				}
				ResultSet rs = ps.executeQuery();
				while (rs.next()) {
					Map<String, Shelf> shelfMap = shelfMaps.get(rs.getInt("台"));
					if (shelfMap != null) {
						if (shelfMap.containsKey(rs.getString("出庫番"))) {
							Shelf shelf = shelfMap.get(rs.getString("出庫番"));
							shelf.addCarriedD(rs.getInt("合計額"));
						}
					}
				}
			}

			// t2 = System.currentTimeMillis();
			// System.out.println("l:"+(t2-t1));t1=t2;
			// 出庫の繰越額から９０伝票の出庫された標準原価をマイナスする
			sb = new StringBuilder(
				"select floor(pd.製作番号/1000) as 台,"
					+ " convert(varchar, pd.製作期)+'-'+convert(varchar, pd.製作番号)+pd.製作枝番 as 出庫番,"
					+ "	sum(dc.金額)*-1 as 合計額"
					+ " from (select * from T_出庫_親 where 出庫年月日<?) dp"
					+ " left outer join T_出庫_子 dc on dc.出庫親ID=dp.出庫親ID"
					+ " left outer join T_製作_親 pd on dc.在庫親ID=pd.製作親ID"
					+ " left outer join (select 売上親ID, 製作親ID from T_売上_子 group by 売上親ID, 製作親ID) sc on pd.製作親ID=sc.製作親ID"
					+ " left outer join T_売上_親 sp on sc.売上親ID=sp.売上親ID"
					+ " where"
			);
			for (int i = 0; i < added.size(); i++) {
				if (i > 0)
					sb.append(" OR");
				sb.append(" (pd.製作期=? and pd.製作番号=? and pd.製作枝番=?)");
			}
			sb.append(" group by pd.製作期, pd.製作番号, pd.製作枝番");
			try (
				PreparedStatement ps = c.prepareStatement(sb.toString());
			) {
				int n = 1;
				ps.setDate(n++, from2);
				for (Seiban num : added) {
					ps.setInt(n++, num.getPeriod());
					ps.setInt(n++, num.getNumber());
					ps.setString(n++, num.getBranch());
				}
				ResultSet rs = ps.executeQuery();
				while (rs.next()) {
					Map<String, Shelf> shelfMap = shelfMaps.get(rs.getInt("台"));
					if (shelfMap != null) {
						if (shelfMap.containsKey(rs.getString("出庫番"))) {
							Shelf shelf = shelfMap.get(rs.getString("出庫番"));
							shelf.addCarriedD(rs.getInt("合計額"));
						}
					}
				}
			}

			// t2 = System.currentTimeMillis();
			// System.out.println("m:"+(t2-t1));t1=t2;
			// 当月仕入
			sb = new StringBuilder(
				"select floor(注文番号/1000) as 台,"
					+ " convert(varchar, 注文期)+'-'+convert(varchar, 注文番号)+注文枝番 as 注番,"
					+ "	sum(oc.金額) as 合計額 from T_在庫_子 oc"
					+ " left outer join T_在庫_親 op on oc.在庫親ID=op.在庫親ID"
					+ " left outer join T_指定納品書 s on oc.納品書番号=s.ID"
					+ " where 納品書日>=? and 納品書日<? and ("
			);
			for (int i = 0; i < added.size(); i++) {
				if (i > 0)
					sb.append(" OR");
				sb.append(" (注文期=? and 注文番号=? and 注文枝番=?)");
			}
			sb.append(")");
			sb.append(" group by 注文期, 注文番号, 注文枝番");
			try (
				PreparedStatement ps = c.prepareStatement(sb.toString());
			) {
				int n = 1;
				ps.setDate(n++, from2);
				ps.setDate(n++, to2);
				for (Seiban num : added) {
					ps.setInt(n++, num.getPeriod());
					ps.setInt(n++, num.getNumber());
					ps.setString(n++, num.getBranch());
				}
				ResultSet rs = ps.executeQuery();
				while (rs.next()) {
					Map<String, Shelf> shelfMap = shelfMaps.get(rs.getInt("台"));
					if (shelfMap != null) {
						if (shelfMap.containsKey(rs.getString("注番"))) {
							Shelf shelf = shelfMap.get(rs.getString("注番"));
							shelf.setO(rs.getInt("合計額"));
						}
					}
				}
			}

			// t2 = System.currentTimeMillis();
			// System.out.println("n:"+(t2-t1));t1=t2;
			// 当月出庫
			sb = new StringBuilder(
				"select floor(dp.製作番号/1000) as 台,"
					+ " convert(varchar, dp.製作期)+'-'+convert(varchar, dp.製作番号)+dp.製作枝番 as 出庫番,"
					+ "	sum(dc.金額) as 合計額 from (select * from T_出庫_親 where 出庫年月日>=? and 出庫年月日<?) dp"
					+ " left outer join T_出庫_子 dc on dc.出庫親ID=dp.出庫親ID"
					+ " where"
			);
			for (int i = 0; i < added.size(); i++) {
				if (i > 0)
					sb.append(" OR");
				sb.append(" (dp.製作期=? and dp.製作番号=? and dp.製作枝番=?)");
			}
			sb.append(" group by dp.製作期, dp.製作番号, dp.製作枝番");
			try (
				PreparedStatement ps = c.prepareStatement(sb.toString());
			) {
				int n = 1;
				ps.setDate(n++, from2);
				ps.setDate(n++, to2);
				for (Seiban num : added) {
					ps.setInt(n++, num.getPeriod());
					ps.setInt(n++, num.getNumber());
					ps.setString(n++, num.getBranch());
				}
				ResultSet rs = ps.executeQuery();
				while (rs.next()) {
					Map<String, Shelf> shelfMap = shelfMaps.get(rs.getInt("台"));
					if (shelfMap != null) {
						if (shelfMap.containsKey(rs.getString("出庫番"))) {
							Shelf shelf = shelfMap.get(rs.getString("出庫番"));
							shelf.addD(rs.getInt("合計額"));
						}
					}
				}
			}

			// t2 = System.currentTimeMillis();
			// System.out.println("o:"+(t2-t1));t1=t2;
			// 当月出庫額から９０伝票の出庫された標準原価をマイナスする
			sb = new StringBuilder(
				"select floor(pp.製作番号/1000) as 台, convert(varchar, pp.製作期)+'-'+convert(varchar, pp.製作番号)+pp.製作枝番 as 出庫番,"
					+ "	sum(dc.金額)*-1 as 合計額 from (select * from T_出庫_親 where 出庫年月日>=? and 出庫年月日<?) dp"
					+ " left outer join T_出庫_子 dc on dc.出庫親ID=dp.出庫親ID"
					+ " left outer join T_製作_親 pp on dc.在庫親ID=pp.製作親ID"
					+ " where"
			);
			for (int i = 0; i < added.size(); i++) {
				if (i > 0)
					sb.append(" OR");
				sb.append(" (pp.製作期=? and pp.製作番号=? and pp.製作枝番=?)");
			}
			sb.append(" group by pp.製作期, pp.製作番号, pp.製作枝番");
			try (
				PreparedStatement ps = c.prepareStatement(sb.toString());
			) {
				int n = 1;
				ps.setDate(n++, from2);
				ps.setDate(n++, to2);
				for (Seiban num : added) {
					ps.setInt(n++, num.getPeriod());
					ps.setInt(n++, num.getNumber());
					ps.setString(n++, num.getBranch());
				}
				ResultSet rs = ps.executeQuery();
				while (rs.next()) {
					Map<String, Shelf> shelfMap = shelfMaps.get(rs.getInt("台"));
					if (shelfMap != null) {
						if (shelfMap.containsKey(rs.getString("出庫番"))) {
							Shelf shelf = shelfMap.get(rs.getString("出庫番"));
							shelf.addD(rs.getInt("合計額"));
						}
					}
				}
			}

			// t2 = System.currentTimeMillis();
			// System.out.println("p:"+(t2-t1));t1=t2;
			// 当月工数
			sb = new StringBuilder(
				"select"
					+ "	floor(w.製作番号/1000) as 台, convert(varchar, w.製作期)+'-'+convert(varchar, w.製作番号)+w.製作枝番 as 製番,"
					+ "	convert(varchar,convert(money,sum(時間))/100) as 工数"
					+ " from (select * from T_加工実績 where 着手日時>=? and 着手日時<?) w"
					+ " where "
			);
			for (int i = 0; i < added.size(); i++) {
				if (i > 0)
					sb.append(" OR");
				sb.append(" (w.製作期=? and w.製作番号=? and w.製作枝番=?)");
			}
			sb.append(" group by w.製作期, w.製作番号, w.製作枝番");
			try (
				PreparedStatement ps = c.prepareStatement(sb.toString());
			) {
				int n = 1;
				ps.setDate(n++, from);
				ps.setDate(n++, to);
				for (Seiban num : added) {
					ps.setInt(n++, num.getPeriod());
					ps.setInt(n++, num.getNumber());
					ps.setString(n++, num.getBranch());
				}
				ResultSet rs = ps.executeQuery();
				while (rs.next()) {
					Map<String, Shelf> shelfMap = shelfMaps.get(rs.getInt("台"));
					if (shelfMap != null) {
						if (shelfMap.containsKey(rs.getString("製番"))) {
							Shelf shelf = shelfMap.get(rs.getString("製番"));
							shelf.setW(rs.getString("工数"));
						}
					}
				}
			}

			// t2 = System.currentTimeMillis();
			// System.out.println("q:"+(t2-t1));t1=t2;
			// 工数累計
			sb = new StringBuilder(
				"select"
					+ "	floor(w.製作番号/1000) as 台, convert(varchar, w.製作期)+'-'+convert(varchar, w.製作番号)+w.製作枝番 as 製番,"
					+ "	convert(varchar,convert(money,sum(時間))/100) as 工数"
					+ " from (select * from T_加工実績 where 着手日時<?) w"
					+ " where"
			);
			for (int i = 0; i < added.size(); i++) {
				if (i > 0)
					sb.append(" OR");
				sb.append(" (w.製作期=? and w.製作番号=? and w.製作枝番=?)");
			}
			sb.append(" group by w.製作期, w.製作番号, w.製作枝番");
			try (
				PreparedStatement ps = c.prepareStatement(sb.toString());
			) {
				int n = 1;
				ps.setDate(n++, to);
				for (Seiban num : added) {
					ps.setInt(n++, num.getPeriod());
					ps.setInt(n++, num.getNumber());
					ps.setString(n++, num.getBranch());
				}
				ResultSet rs = ps.executeQuery();
				while (rs.next()) {
					Map<String, Shelf> shelfMap = shelfMaps.get(rs.getInt("台"));
					if (shelfMap != null) {
						if (shelfMap.containsKey(rs.getString("製番"))) {
							Shelf shelf = shelfMap.get(rs.getString("製番"));
							shelf.setCarriesW(rs.getString("工数"));
						}
					}
				}
			}
		}

		// t2 = System.currentTimeMillis();
		// System.out.println("r:"+(t2-t1));t1=t2;
		// map → vector
		for (int i = 1; i < 10; i++) {
			for (Map.Entry<String, Shelf> e : shelfMaps.get(i).entrySet()) {
				Shelf shelf = e.getValue();
				if (shelf.hasData()) {
					Vector<Object> record = new Vector<Object>();
					record.add(e.getKey()); // 製番
					record.add(shelf.getS()); // 売上のマーク
					int carried = shelf.getO(true) + shelf.getD(true);
					record.add(carried); // 繰越金額
					record.add(shelf.getO(false)); // 当月仕入
					record.add(shelf.getD(false)); // 当月出庫
					int od = shelf.getO(false) + shelf.getD(false);
					record.add(od); // 当月仕入＋当月出庫
					record.add(carried + od); // 合計金額
					record.add(shelf.getW(false)); // 当月工数
					record.add(shelf.getW(true)); // 工数累計
					data.add(record);
				}
			}
		}
		return data;
	}

	/**
	 * 棚データ保持用クラス
	 */
	private class Shelf {
		// 売上のマーク, 工数, 工数累計
		String s = "", w = "0.00", carriesW = "0.00";
		// 仕入, 出庫, 繰越仕入, 繰越出庫
		int o, d, carriedO, carriedD;
		boolean hasData;

		private Shelf(String s) {
			this.s = s;
		}

		private Shelf(String s, int carriedO, int carriedD, int o, int d, String w, String carriesW) {
			this.s = s;
			this.carriedO = carriedO;
			this.carriedD = carriedD;
			this.o = o;
			this.d = d;
			this.w = w;
			this.carriesW = carriesW;
			hasData = true;
		}

		/**
		 * 金額または工数があるかどうかを返す
		 * 
		 * @return 金額または工数がある場合はtrue, ない場合はfalse
		 */
		private boolean hasData() {
			return hasData;
		}

		/**
		 * 売上のマークを返す
		 * 
		 * @return 当月売上=*, 前月以前売上=#
		 */
		private String getS() {
			return s;
		}

		/**
		 * 工数または累計工数を返す
		 * 
		 * @param isCarries 累計工数の場合はtrue, 当月工数の場合はfalse
		 * 
		 * @return 工数または累計工数
		 */
		private String getW(boolean isCarries) {
			return isCarries ? carriesW : w;
		}

		/**
		 * 仕入金額または繰越仕入金額を返す
		 * 
		 * @param isCarried 繰越仕入の場合はtrue, 当月仕入の場合はfalse
		 * 
		 * @return 仕入金額または繰越仕入金額
		 */
		private int getO(boolean isCarried) {
			return isCarried ? carriedO : o;
		}

		/**
		 * 出庫金額または繰越出庫金額を返す
		 * 
		 * @param isCarried 繰越出庫の場合はtrue, 当月出庫の場合はfalse
		 * 
		 * @return 出庫金額または繰越出庫金額
		 */
		private int getD(boolean isCarried) {
			return isCarried ? carriedD : d;
		}

		/**
		 * 工数をセットする
		 * 
		 * @param w 工数
		 */
		private void setW(String w) {
			this.w = w;
			hasData = true;
		}

		/**
		 * 累計工数をセットする
		 * 
		 * @param w 累計工数
		 */
		private void setCarriesW(String w) {
			this.carriesW = w;
			hasData = true;
		}

		/**
		 * 仕入金額をセットする
		 * 
		 * @param o 仕入金額
		 */
		private void setO(int o) {
			this.o = o;
			hasData = true;
		}

		/**
		 * 繰越仕入金額をセットする
		 * 
		 * @param o 繰越仕入金額
		 */
		private void setCarriedO(int o) {
			this.carriedO = o;
			hasData = true;
		}

		/**
		 * 出庫金額を加算する（９０の減算があるため）
		 * 
		 * @param o 出庫金額
		 */
		private void addD(int d) {
			this.d += d; // 90のマイナスを加味
			hasData = true;
		}

		/**
		 * 繰越出庫金額を加算する（９０の減算があるため）
		 * 
		 * @param o 繰越出庫金額
		 */
		private void addCarriedD(int d) {
			this.carriedD += d; // 90のマイナスを加味
			hasData = true;
		}
	}

	/**
	 * 製番保持用クラス
	 */
	class Seiban {
		int period, number;
		String branch;

		Seiban(int period, int number, String branch) {
			this.period = period;
			this.number = number;
			this.branch = branch;
		}

		private int getPeriod() {
			return period;
		}

		private int getNumber() {
			return number;
		}

		private String getBranch() {
			return branch;
		}
	}
}