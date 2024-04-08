package fukaisystem.aggregate;

import java.io.*;
import java.sql.*;
import java.sql.Date;
import java.util.*;
import javax.servlet.*;

import fukaisystem.sql.DBConnection;

import org.apache.log4j.Logger;

/**
 * テーブルの内容と列情報を取得するためのクラス
 * @author kameura
 *
 */
public class GetShelf2 extends GenericServlet {

	private static final long serialVersionUID = 1L;
	private static Logger lg = Logger.getLogger("dbtool");

	public void service(ServletRequest request, ServletResponse response) {
long t1 = System.currentTimeMillis();
		DBConnection dbc = new DBConnection();
		Connection c = dbc.getConnection();
		PreparedStatement ps = null;
		ResultSet rs = null;

		Date from = null;//売り集計の初日
		Date to = null;//翌月売り集計の初日
		Date from2 = null;//注文集計の初日
		Date to2 = null;//翌月注文集計の初日
		int month = 0;

		StringBuilder err = new StringBuilder("");

		Map<Integer, Map<String, Shelf>> shelfMaps = new HashMap<Integer, Map<String, Shelf>>();
		for(int i = 1; i < 10; i++) {//10～90
			Map<String, Shelf> shelfMap = new TreeMap<String, Shelf>();
			shelfMaps.put(i, shelfMap);
		}
		List<Seiban> added = new ArrayList<Seiban>();
		Vector<Vector<Object>> data = new Vector<Vector<Object>>();
		try {
			//クライアントから読み込み
			ObjectInputStream in = new ObjectInputStream(request.getInputStream());
			Object obj = in.readObject();
			if(obj instanceof Date) {
				from = (Date)obj;
				Calendar target = Calendar.getInstance();
				target.setTime(from);//今月1日
				month = target.get(Calendar.MONTH) + 1;
				target.add(Calendar.MONTH, 1);//翌月1日
				to = new Date(target.getTimeInMillis());
				target.setTime(from);//今月1日（次の2014年判定のためにこのタイミングでセットする必要がある）
				if(target.get(Calendar.YEAR) < 2014) {//2013年以前は25日〆
					if(target.get(Calendar.MONTH) == 11) {//12月は11月26日から12月31日
						to2 = to;
						target.add(Calendar.DATE, 25);//今月26日
						target.add(Calendar.MONTH, -1);//先月26日
						from2 = new Date(target.getTimeInMillis());
					} else {
						target.add(Calendar.DATE, 25);//今月26日
						to2 = new Date(target.getTimeInMillis());
						if(target.get(Calendar.MONTH) == 0) {//1月は1月1日から1月25日
							from2 = from;
						} else {
							target.add(Calendar.MONTH, -1);//先月26日
							from2 = new Date(target.getTimeInMillis());
						}
					}
				} else {//2014年以後は月末〆
					to2 = to;
					from2 = from;
				}
			}
			in.close();
//long t2 = System.currentTimeMillis();
//System.out.println("a:"+(t2-t1));t1=t2;
			try {
				String sql
						= "select "
						+ "	floor(製作番号/1000) as 台, convert(varchar, 製作期)+'-'+convert(varchar, 製作番号)+製作枝番 as 製番,"
						+ "	case when (売上年月日>=? and 売上年月日<?) and (納品区分CD=2 or 納品区分CD=4 or 納品区分CD=5 or 納品区分CD=6) then '*'"
						+ "		else '' end as 売"
						+ " from (select * from T_製作_親 where (製作期>50 or (製作期=43 and 製作番号=8014) or (製作期=47 and 製作番号=8009)) and 製作番号<>0) pp"
						+ " left outer join (select 売上親ID, 製作親ID from T_売上_子 group by 売上親ID, 製作親ID) sc on pp.製作親ID=sc.製作親ID"
						+ " left outer join T_売上_親 sp on sc.売上親ID=sp.売上親ID"
						+ "	where 売上年月日>=? or 売上年月日 is null";
				//System.out.println(sql);
				ps = c.prepareStatement(sql);
				int n = 1;
				ps.setDate(n++, from);//売
				ps.setDate(n++, to);
				ps.setDate(n++, from);
				rs = ps.executeQuery();
				while(rs.next()) {
					if(shelfMaps.containsKey(rs.getInt("台"))) {
						shelfMaps.get(rs.getInt("台")).put(rs.getString("製番"), new Shelf(rs.getString("売")));
					}
				}
				ps.close();
				rs.close();
//t2 = System.currentTimeMillis();
//System.out.println("b:"+(t2-t1));t1=t2;

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
				//System.out.println(sql);
				ps = c.prepareStatement(sql);
				n = 1;
				ps.setDate(n++, from2);
				ps.setDate(n++, from);
				rs = ps.executeQuery();
				while(rs.next()) {
					Map<String, Shelf> shelfMap = shelfMaps.get(rs.getInt("台"));
					if(shelfMap != null) {
						if(shelfMap.containsKey(rs.getString("注番"))) {
							Shelf shelf = shelfMap.get(rs.getString("注番"));
							shelf.setCarriedO(rs.getInt("合計額"));
						}
					}
				}
				ps.close();
				rs.close();
//t2 = System.currentTimeMillis();
//System.out.println("c2:"+(t2-t1));t1=t2;

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
				//System.out.println(sql);
				ps = c.prepareStatement(sql);
				n = 1;
				ps.setDate(n++, from2);
				ps.setDate(n++, from);
				rs = ps.executeQuery();
				while(rs.next()) {
					Map<String, Shelf> shelfMap = shelfMaps.get(rs.getInt("台"));
					if(shelfMap != null) {
						if(shelfMap.containsKey(rs.getString("出庫番"))) {
							Shelf shelf = shelfMap.get(rs.getString("出庫番"));
							shelf.setCarriedD(rs.getInt("合計額"));
						}
					}
				}
				ps.close();
				rs.close();
//t2 = System.currentTimeMillis();
//System.out.println("d:"+(t2-t1));t1=t2;
				sql
						= "select floor(pd.製作番号/1000) as 台, convert(varchar, pd.製作期)+'-'+convert(varchar, pd.製作番号)+pd.製作枝番 as 出庫番,"
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
				//System.out.println(sql);
				ps = c.prepareStatement(sql);
				n = 1;
				ps.setDate(n++, from2);
				ps.setDate(n++, from);
				rs = ps.executeQuery();
				while(rs.next()) {
					Map<String, Shelf> shelfMap = shelfMaps.get(rs.getInt("台"));
					if(shelfMap != null) {
						if(shelfMap.containsKey(rs.getString("出庫番"))) {
							Shelf shelf = shelfMap.get(rs.getString("出庫番"));
							shelf.setCarriedD(rs.getInt("合計額"));
						}
					}
				}
				ps.close();
				rs.close();

//t2 = System.currentTimeMillis();
//System.out.println("e:"+(t2-t1));t1=t2;
				sql
						= "select floor(注文番号/1000) as 台, 注文期, 注文番号, 注文枝番,"
						+ " convert(varchar, 注文期)+'-'+convert(varchar, 注文番号)+注文枝番 as 注番,"
						+ "	sum(oc.金額) as 合計額 from T_在庫_子 oc"
						+ " left outer join T_在庫_親 op on oc.在庫親ID=op.在庫親ID"
						+ " left outer join T_指定納品書 s on oc.納品書番号=s.ID"
						+ " left outer join T_製作_親 pp on op.注文期=pp.製作期 and op.注文番号=pp.製作番号 and op.注文枝番=pp.製作枝番"
						+ " where 納品書日>=? and 納品書日<? and 製作親ID is not null"
						+ " group by 注文期, 注文番号, 注文枝番";
				//System.out.println(sql);
				ps = c.prepareStatement(sql);
				n = 1;
				ps.setDate(n++, from2);
				ps.setDate(n++, to2);
				rs = ps.executeQuery();
				while(rs.next()) {
					Map<String, Shelf> shelfMap = shelfMaps.get(rs.getInt("台"));
					if(shelfMap != null) {
						if(shelfMap.containsKey(rs.getString("注番"))) {
							Shelf shelf = shelfMap.get(rs.getString("注番"));
							shelf.setO(rs.getInt("合計額"));
						} else {
							shelfMap.put(rs.getString("注番"), new Shelf("#"));
							added.add(new Seiban(rs.getInt("注文期"), rs.getInt("注文番号"), rs.getString("注文枝番")));
						}
					}
				}
				ps.close();
				rs.close();
//t2 = System.currentTimeMillis();
//System.out.println("f:"+(t2-t1));t1=t2;
				sql
						= "select floor(dp.製作番号/1000) as 台, dp.製作期, dp.製作番号, dp.製作枝番,"
						+ "convert(varchar, dp.製作期)+'-'+convert(varchar, dp.製作番号)+dp.製作枝番 as 出庫番,"
						+ "	sum(dc.金額) as 合計額 from (select * from T_出庫_親 where 出庫年月日>=? and 出庫年月日<?) dp"
						+ " left outer join T_出庫_子 dc on dc.出庫親ID=dp.出庫親ID"
						+ " left outer join T_製作_親 pp on dp.製作期=pp.製作期 and dp.製作番号=pp.製作番号 and dp.製作枝番=pp.製作枝番"
						+ " where 製作親ID is not null"
						+ " group by dp.製作期, dp.製作番号, dp.製作枝番";
				//System.out.println(sql);
				ps = c.prepareStatement(sql);
				n = 1;
				ps.setDate(n++, from2);
				ps.setDate(n++, to2);
				rs = ps.executeQuery();
				while(rs.next()) {
					Map<String, Shelf> shelfMap = shelfMaps.get(rs.getInt("台"));
					if(shelfMap != null) {
						if(shelfMap.containsKey(rs.getString("出庫番"))) {
							Shelf shelf = shelfMap.get(rs.getString("出庫番"));
							shelf.setD(rs.getInt("合計額"));
						} else {
							shelfMap.put(rs.getString("出庫番"), new Shelf("#"));
							added.add(new Seiban(rs.getInt("製作期"), rs.getInt("製作番号"), rs.getString("製作枝番")));
						}
					}
				}
				ps.close();
				rs.close();
//t2 = System.currentTimeMillis();
//System.out.println("g:"+(t2-t1));
				sql
						= "select floor(pd.製作番号/1000) as 台, pd.製作期, pd.製作番号, pd.製作枝番,"
						+ " convert(varchar, pd.製作期)+'-'+convert(varchar, pd.製作番号)+pd.製作枝番 as 出庫番,"
						+ "	sum(dc.金額)*-1 as 合計額 from (select * from T_出庫_親 where 出庫年月日>=? and 出庫年月日<?) dp"
						+ " left outer join T_出庫_子 dc on dc.出庫親ID=dp.出庫親ID"
						+ " left outer join T_製作_親 pd on dc.在庫親ID=pd.製作親ID"
						+ " where 製作親ID is not null"
						+ " group by pd.製作期, pd.製作番号, pd.製作枝番";
				//System.out.println(sql);
				ps = c.prepareStatement(sql);
				n = 1;
				ps.setDate(n++, from2);
				ps.setDate(n++, to2);
				rs = ps.executeQuery();
				while(rs.next()) {
					Map<String, Shelf> shelfMap = shelfMaps.get(rs.getInt("台"));
					if(shelfMap != null) {
						if(shelfMap.containsKey(rs.getString("出庫番"))) {
							Shelf shelf = shelfMap.get(rs.getString("出庫番"));
							shelf.setD(rs.getInt("合計額"));
						} else {
							shelfMap.put(rs.getString("出庫番"), new Shelf("#"));
							added.add(new Seiban(rs.getInt("製作期"), rs.getInt("製作番号"), rs.getString("製作枝番")));
						}
					}
				}
				ps.close();
				rs.close();
//t2 = System.currentTimeMillis();
//System.out.println("h:"+(t2-t1));
				sql
						= "select"
						+ "	floor(w.製作番号/1000) as 台, w.製作期, w.製作番号, w.製作枝番,"
						+ " convert(varchar, w.製作期)+'-'+convert(varchar, w.製作番号)+w.製作枝番 as 製番,"
						+ "	convert(varchar,convert(money,sum(時間))/100) as 工数"
						+ " from (select * from T_加工実績 where 製作期<>0 and 製作番号<>0 and 着手日時>=? and 着手日時<?) w"
						+ " left outer join T_製作_親 pp on w.製作期=pp.製作期 and w.製作番号=pp.製作番号 and w.製作枝番=pp.製作枝番"
						+ " where 製作親ID is not null"
						+ " group by w.製作期, w.製作番号, w.製作枝番";
				//System.out.println(sql);
				ps = c.prepareStatement(sql);
				n = 1;
				ps.setDate(n++, from);
				ps.setDate(n++, to);
				rs = ps.executeQuery();
				while(rs.next()) {
					Map<String, Shelf> shelfMap = shelfMaps.get(rs.getInt("台"));
					if(shelfMap != null) {
						if(shelfMap.containsKey(rs.getString("製番"))) {
							Shelf shelf = shelfMap.get(rs.getString("製番"));
							shelf.setW(rs.getString("工数"));
						} else {
							shelfMap.put(rs.getString("製番"), new Shelf("#"));
							added.add(new Seiban(rs.getInt("製作期"), rs.getInt("製作番号"), rs.getString("製作枝番")));
						}
					}
				}
				ps.close();
				rs.close();
//t2 = System.currentTimeMillis();
//System.out.println("i:"+(t2-t1));t1=t2;
				sql
						= "select"
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
				//System.out.println(sql);
				ps = c.prepareStatement(sql);
				n = 1;
				ps.setDate(n++, to);
				ps.setDate(n++, from);
				rs = ps.executeQuery();
				while(rs.next()) {
					Map<String, Shelf> shelfMap = shelfMaps.get(rs.getInt("台"));
					if(shelfMap != null) {
						if(shelfMap.containsKey(rs.getString("製番"))) {
							Shelf shelf = shelfMap.get(rs.getString("製番"));
							shelf.setCarriesW(rs.getString("工数"));
						} else {
							shelfMap.put(rs.getString("製番"), new Shelf("#"));
							added.add(new Seiban(rs.getInt("製作期"), rs.getInt("製作番号"), rs.getString("製作枝番")));
						}
					}
				}
				ps.close();
				rs.close();
//t2 = System.currentTimeMillis();
//System.out.println("j:"+(t2-t1));t1=t2;
				if(added.size() > 0) {
					StringBuilder sb = new StringBuilder
							( "select floor(注文番号/1000) as 台, convert(varchar, 注文期)+'-'+convert(varchar, 注文番号)+注文枝番 as 注番,"
							+ "	sum(金額) as 合計額 from"
							+ "  (select 在庫親ID, 注文期, 注文番号, 注文枝番 from T_在庫_親 where");
					for(int i = 0; i < added.size(); i++) {
						if(i > 0) sb.append(" OR");
						sb.append(" (注文期=? and 注文番号=? and 注文枝番=?)");
					}
					sb.append(") op"
						+ " left outer join T_在庫_子 oc on oc.在庫親ID=op.在庫親ID"
						+ " left outer join T_指定納品書 s on oc.納品書番号=s.ID"
						+ " where 納品書日<?");
					sb.append( " group by 注文期, 注文番号, 注文枝番");
					ps = c.prepareStatement(sb.toString());
					n = 1;
					for(Seiban num : added) {
						ps.setInt(n++, num.getPeriod());
						ps.setInt(n++, num.getNumber());
						ps.setString(n++, num.getBranch());
					}
					ps.setDate(n++, from2);
					rs = ps.executeQuery();
					while(rs.next()) {
						Map<String, Shelf> shelfMap = shelfMaps.get(rs.getInt("台"));
						if(shelfMap != null) {
							if(shelfMap.containsKey(rs.getString("注番"))) {
								Shelf shelf = shelfMap.get(rs.getString("注番"));
								shelf.setCarriedO(rs.getInt("合計額"));
							}
						}
					}
					ps.close();
					rs.close();
//t2 = System.currentTimeMillis();
//System.out.println("k:"+(t2-t1));t1=t2;
					sb = new StringBuilder
							( "select floor(dp.製作番号/1000) as 台,"
							+ " convert(varchar, dp.製作期)+'-'+convert(varchar, dp.製作番号)+dp.製作枝番 as 出庫番,"
							+ "	sum(dc.金額) as 合計額"
							+ " from (select * from T_出庫_親 where 出庫年月日<?) dp"
							+ " left outer join T_出庫_子 dc on dc.出庫親ID=dp.出庫親ID"
							+ " left outer join T_製作_親 pd on dc.在庫親ID=pd.製作親ID"
							+ " left outer join (select 売上親ID, 製作親ID from T_売上_子 group by 売上親ID, 製作親ID) sc on pd.製作親ID=sc.製作親ID"
							+ " where");
							for(int i = 0; i < added.size(); i++) {
								if(i > 0) sb.append(" OR");
								sb.append(" (dp.製作期=? and dp.製作番号=? and dp.製作枝番=?)");
							}
							sb.append(" group by dp.製作期, dp.製作番号, dp.製作枝番");
					ps = c.prepareStatement(sb.toString());
					n = 1;
					ps.setDate(n++, from2);
					for(Seiban num : added) {
						ps.setInt(n++, num.getPeriod());
						ps.setInt(n++, num.getNumber());
						ps.setString(n++, num.getBranch());
					}
					rs = ps.executeQuery();
					while(rs.next()) {
						Map<String, Shelf> shelfMap = shelfMaps.get(rs.getInt("台"));
						if(shelfMap != null) {
							if(shelfMap.containsKey(rs.getString("出庫番"))) {
								Shelf shelf = shelfMap.get(rs.getString("出庫番"));
								shelf.setCarriedD(rs.getInt("合計額"));
							}
						}
					}
					ps.close();
					rs.close();
//t2 = System.currentTimeMillis();
//System.out.println("l:"+(t2-t1));t1=t2;
					sb = new StringBuilder
							( "select floor(pd.製作番号/1000) as 台,"
							+ " convert(varchar, pd.製作期)+'-'+convert(varchar, pd.製作番号)+pd.製作枝番 as 出庫番,"
							+ "	sum(dc.金額)*-1 as 合計額"
							+ " from (select * from T_出庫_親 where 出庫年月日<?) dp"
							+ " left outer join T_出庫_子 dc on dc.出庫親ID=dp.出庫親ID"
							+ " left outer join T_製作_親 pd on dc.在庫親ID=pd.製作親ID"
							+ " left outer join (select 売上親ID, 製作親ID from T_売上_子 group by 売上親ID, 製作親ID) sc on pd.製作親ID=sc.製作親ID"
							+ " left outer join T_売上_親 sp on sc.売上親ID=sp.売上親ID"
							+ " where");
							for(int i = 0; i < added.size(); i++) {
								if(i > 0) sb.append(" OR");
								sb.append(" (pd.製作期=? and pd.製作番号=? and pd.製作枝番=?)");
							}
					sb.append(" group by pd.製作期, pd.製作番号, pd.製作枝番");
					ps = c.prepareStatement(sb.toString());
					n = 1;
					ps.setDate(n++, from2);
					for(Seiban num : added) {
						ps.setInt(n++, num.getPeriod());
						ps.setInt(n++, num.getNumber());
						ps.setString(n++, num.getBranch());
					}
					rs = ps.executeQuery();
					while(rs.next()) {
						Map<String, Shelf> shelfMap = shelfMaps.get(rs.getInt("台"));
						if(shelfMap != null) {
							if(shelfMap.containsKey(rs.getString("出庫番"))) {
								Shelf shelf = shelfMap.get(rs.getString("出庫番"));
								shelf.setCarriedD(rs.getInt("合計額"));
							}
						}
					}
					ps.close();
					rs.close();
		/////////////////////////////////////////////
//t2 = System.currentTimeMillis();
//System.out.println("m:"+(t2-t1));t1=t2;
					sb = new StringBuilder
							( "select floor(注文番号/1000) as 台,"
							+ " convert(varchar, 注文期)+'-'+convert(varchar, 注文番号)+注文枝番 as 注番,"
							+ "	sum(oc.金額) as 合計額 from T_在庫_子 oc"
							+ " left outer join T_在庫_親 op on oc.在庫親ID=op.在庫親ID"
							+ " left outer join T_指定納品書 s on oc.納品書番号=s.ID"
							+ " left outer join T_製作_親 pp on op.注文期=pp.製作期 and op.注文番号=pp.製作番号 and op.注文枝番=pp.製作枝番"
							+ " where 納品書日>=? and 納品書日<? and (");
					for(int i = 0; i < added.size(); i++) {
						if(i > 0) sb.append(" OR");
						sb.append(" (注文期=? and 注文番号=? and 注文枝番=?)");
					}
					sb.append( ")");
					sb.append(" group by 注文期, 注文番号, 注文枝番");
					ps = c.prepareStatement(sb.toString());
					n = 1;
					ps.setDate(n++, from2);
					ps.setDate(n++, to2);
					for(Seiban num : added) {
						ps.setInt(n++, num.getPeriod());
						ps.setInt(n++, num.getNumber());
						ps.setString(n++, num.getBranch());
					}
					rs = ps.executeQuery();
					while(rs.next()) {
						Map<String, Shelf> shelfMap = shelfMaps.get(rs.getInt("台"));
						if(shelfMap != null) {
							if(shelfMap.containsKey(rs.getString("注番"))) {
								Shelf shelf = shelfMap.get(rs.getString("注番"));
								shelf.setO(rs.getInt("合計額"));
							}
						}
					}
					ps.close();
					rs.close();
//t2 = System.currentTimeMillis();
//System.out.println("n:"+(t2-t1));t1=t2;
					sb = new StringBuilder
							( "select floor(dp.製作番号/1000) as 台,"
							+ " convert(varchar, dp.製作期)+'-'+convert(varchar, dp.製作番号)+dp.製作枝番 as 出庫番,"
							+ "	sum(dc.金額) as 合計額 from (select * from T_出庫_親 where 出庫年月日>=? and 出庫年月日<?) dp"
							+ " left outer join T_出庫_子 dc on dc.出庫親ID=dp.出庫親ID"
							+ " left outer join T_製作_親 pp on dp.製作期=pp.製作期 and dp.製作番号=pp.製作番号 and dp.製作枝番=pp.製作枝番"
							+ " where");
							for(int i = 0; i < added.size(); i++) {
								if(i > 0) sb.append(" OR");
								sb.append(" (dp.製作期=? and dp.製作番号=? and dp.製作枝番=?)");
							}
					sb.append(" group by dp.製作期, dp.製作番号, dp.製作枝番");
					ps = c.prepareStatement(sb.toString());
					n = 1;
					ps.setDate(n++, from2);
					ps.setDate(n++, to2);
					for(Seiban num : added) {
						ps.setInt(n++, num.getPeriod());
						ps.setInt(n++, num.getNumber());
						ps.setString(n++, num.getBranch());
					}
					rs = ps.executeQuery();
					while(rs.next()) {
						Map<String, Shelf> shelfMap = shelfMaps.get(rs.getInt("台"));
						if(shelfMap != null) {
							if(shelfMap.containsKey(rs.getString("出庫番"))) {
								Shelf shelf = shelfMap.get(rs.getString("出庫番"));
								shelf.setD(rs.getInt("合計額"));
							}
						}
					}
					ps.close();
					rs.close();
//t2 = System.currentTimeMillis();
//System.out.println("o:"+(t2-t1));t1=t2;
					sb = new StringBuilder
							( "select floor(pd.製作番号/1000) as 台, convert(varchar, pd.製作期)+'-'+convert(varchar, pd.製作番号)+pd.製作枝番 as 出庫番,"
							+ "	sum(dc.金額)*-1 as 合計額 from (select * from T_出庫_親 where 出庫年月日>=? and 出庫年月日<?) dp"
							+ " left outer join T_出庫_子 dc on dc.出庫親ID=dp.出庫親ID"
							+ " left outer join T_製作_親 pd on dc.在庫親ID=pd.製作親ID"
							+ " where");
							for(int i = 0; i < added.size(); i++) {
								if(i > 0) sb.append(" OR");
								sb.append(" (pd.製作期=? and pd.製作番号=? and pd.製作枝番=?)");
							}
					sb.append(" group by pd.製作期, pd.製作番号, pd.製作枝番");
					ps = c.prepareStatement(sb.toString());
					n = 1;
					ps.setDate(n++, from2);
					ps.setDate(n++, to2);
					for(Seiban num : added) {
						ps.setInt(n++, num.getPeriod());
						ps.setInt(n++, num.getNumber());
						ps.setString(n++, num.getBranch());
					}
					rs = ps.executeQuery();
					while(rs.next()) {
						Map<String, Shelf> shelfMap = shelfMaps.get(rs.getInt("台"));
						if(shelfMap != null) {
							if(shelfMap.containsKey(rs.getString("出庫番"))) {
								Shelf shelf = shelfMap.get(rs.getString("出庫番"));
								shelf.setD(rs.getInt("合計額"));
							}
						}
					}
					ps.close();
					rs.close();
//t2 = System.currentTimeMillis();
//System.out.println("p:"+(t2-t1));t1=t2;
					sb = new StringBuilder
							( "select"
							+ "	floor(w.製作番号/1000) as 台, convert(varchar, w.製作期)+'-'+convert(varchar, w.製作番号)+w.製作枝番 as 製番,"
							+ "	convert(varchar,convert(money,sum(時間))/100) as 工数"
							+ " from (select * from T_加工実績 where 着手日時>=? and 着手日時<?) w"
							+ " left outer join T_製作_親 pp on w.製作期=pp.製作期 and w.製作番号=pp.製作番号 and w.製作枝番=pp.製作枝番"
							+ " where ");
							for(int i = 0; i < added.size(); i++) {
								if(i > 0) sb.append(" OR");
								sb.append(" (w.製作期=? and w.製作番号=? and w.製作枝番=?)");
							}
					sb.append(" group by w.製作期, w.製作番号, w.製作枝番");
					ps = c.prepareStatement(sb.toString());
					n = 1;
					ps.setDate(n++, from);
					ps.setDate(n++, to);
					for(Seiban num : added) {
						ps.setInt(n++, num.getPeriod());
						ps.setInt(n++, num.getNumber());
						ps.setString(n++, num.getBranch());
					}
					rs = ps.executeQuery();
					while(rs.next()) {
						Map<String, Shelf> shelfMap = shelfMaps.get(rs.getInt("台"));
						if(shelfMap != null) {
							if(shelfMap.containsKey(rs.getString("製番"))) {
								Shelf shelf = shelfMap.get(rs.getString("製番"));
								shelf.setW(rs.getString("工数"));
							}
						}
					}
					ps.close();
					rs.close();
//t2 = System.currentTimeMillis();
//System.out.println("q:"+(t2-t1));t1=t2;
////////////////////////////////////////////////////////////////////////////////////////////////////
					sb = new StringBuilder
							( "select"
							+ "	floor(w.製作番号/1000) as 台, convert(varchar, w.製作期)+'-'+convert(varchar, w.製作番号)+w.製作枝番 as 製番,"
							+ "	convert(varchar,convert(money,sum(時間))/100) as 工数"
							+ " from (select * from T_加工実績 where 着手日時<?) w"
							+ " where");
							for(int i = 0; i < added.size(); i++) {
								if(i > 0) sb.append(" OR");
								sb.append(" (w.製作期=? and w.製作番号=? and w.製作枝番=?)");
							}
					sb.append(" group by w.製作期, w.製作番号, w.製作枝番");
					ps = c.prepareStatement(sb.toString());
					n = 1;
					ps.setDate(n++, to);
					for(Seiban num : added) {
						ps.setInt(n++, num.getPeriod());
						ps.setInt(n++, num.getNumber());
						ps.setString(n++, num.getBranch());
					}
					rs = ps.executeQuery();
					while(rs.next()) {
						Map<String, Shelf> shelfMap = shelfMaps.get(rs.getInt("台"));
						if(shelfMap != null) {
							if(shelfMap.containsKey(rs.getString("製番"))) {
								Shelf shelf = shelfMap.get(rs.getString("製番"));
								shelf.setCarriesW(rs.getString("工数"));
							}
						}
					}
					ps.close();
					rs.close();
				}
			} catch(SQLException ex) {
				ex.printStackTrace();
				err.append(ex + "\n");
				lg.error("GetElements3 " + ex);
			}
//t2 = System.currentTimeMillis();
//System.out.println("r:"+(t2-t1));t1=t2;
			//map → vector
			for(int i = 1; i < 10; i++) {
				for(Map.Entry<String, Shelf> e : shelfMaps.get(i).entrySet()) {
					Shelf shelf = e.getValue();
					if(shelf.isTarget()) {
						Vector<Object> v = new Vector<Object>();
						v.add(e.getKey());
						v.add(shelf.getS());
						int carried = shelf.getO(true) + shelf.getD(true);
						v.add(carried);
						v.add(shelf.getO(false));
						v.add(shelf.getD(false));
						int od = shelf.getO(false) + shelf.getD(false);
						v.add(od);
						v.add(carried + od);
						v.add(shelf.getW(false));
						v.add(shelf.getW(true));
						data.add(v);
					}
				}
			}
//t2 = System.currentTimeMillis();
//System.out.println("s:"+(t2-t1));t1=t2;
			//クライアントに送信
			response.setContentType("application/octet-stream");
			ObjectOutputStream out = new ObjectOutputStream(response.getOutputStream());
			out.writeObject(data);
			out.writeUTF(err.toString());
			out.flush();
			out.close();
		} catch(Exception ex) {
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

	private class Shelf {
		String s = "", w = "0.00", carriesW = "0.00";
		int o, d, carriedO, carriedD;
		boolean isTarget;
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
			isTarget = true;
		}
		private boolean isTarget() {
			return isTarget;
		}
		private String getS() {
			return s;
		}
		private String getW(boolean isCarries) {
			return isCarries ? carriesW : w;
		}
		private int getO(boolean isCarried) {
			return isCarried ? carriedO : o;
		}
		private int getD(boolean isCarried) {
			return isCarried ? carriedD : d;
		}
		private void setW(String w) {
			this.w = w;
			isTarget = true;
		}
		private void setCarriesW(String w) {
			this.carriesW = w;
			isTarget = true;
		}
		private void setO(int o) {
			this.o = o;
			isTarget = true;
		}
		private void setCarriedO(int o) {
			this.carriedO = o;
			isTarget = true;
		}
		private void setD(int d) {
			this.d += d;//90のマイナスを加味
			isTarget = true;
		}
		private void setCarriedD(int d) {
			this.carriedD += d;//90のマイナスを加味
			isTarget = true;
		}
	}

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