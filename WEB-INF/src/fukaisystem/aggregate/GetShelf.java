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
 * 
 * @author kameura
 *
 */
public class GetShelf extends GenericServlet {

	private static final long serialVersionUID = 1L;
	private static Logger lg = Logger.getLogger("dbtool");

	public void service(ServletRequest request, ServletResponse response) {

		DBConnection dbc = new DBConnection();
		Connection c = dbc.getConnection();
		PreparedStatement ps = null;
		ResultSet rs = null;

		Date from = null;
		Date to = null;
		Date from2 = null;
		Date to2 = null;
		int month = 0;

		StringBuilder err = new StringBuilder("");

		Vector<Vector<Object>> data = new Vector<Vector<Object>>();
		try {
			// クライアントから読み込み

			ObjectInputStream in = new ObjectInputStream(request.getInputStream());
			Object obj = in.readObject();
			if (obj instanceof Date) {
				from = (Date) obj;
				Calendar target = Calendar.getInstance();
				target.setTime(from); // 今月1日
				month = target.get(Calendar.MONTH) + 1;
				target.add(Calendar.MONTH, 1); // 翌月1日
				to = new Date(target.getTimeInMillis());
				target.setTime(from); // 今月1日（次の2014年判定のためにこのタイミングでセットする必要がある）
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
			}
			in.close();
			String sql = "select distinct" + "\n"
				+ "substring(convert(varchar,case when p2.製作番号 is null then z.製作番号 else p2.製作番号 end),1,1) as x," + "\n"
				+ "case when p2.製作番号 is null" + "\n"
				+ " then convert(varchar,z.製作期)+'-'+convert(varchar,z.製作番号)+convert(varchar,z.製作枝番)" + "\n"
				+ " else convert(varchar,p2.製作期)+'-'+convert(varchar,p2.製作番号)+convert(varchar,p2.製作枝番) end as 製番,"
				+ "\n"
				+ "case when (納品区分CD=2 or 納品区分CD=4 or 納品区分CD=5 or 納品区分CD=6) then (case when 売上年月日<? then '#' else (case when 売上年月日<? then '*' else '' end) end) else '' end as 売,"
				+ "\n"
				+ "case when 繰越仕入 is null and 繰越出庫 is null then 0" + "\n"
				+ "when 繰越仕入 is null then 繰越出庫" + "\n"
				+ "when 繰越出庫 is null then 繰越仕入" + "\n"
				+ "else 繰越仕入+繰越出庫" + "\n"
				+ "end as 繰越金額," + "\n"
				+ "case when sum(繰越90) is null then 0 else sum(繰越90) end as 繰越90," + "\n"
				+ "case when 仕入金額 is null then 0 else 仕入金額 end as 仕入金額," + "\n"
				+ "case when 出庫金額 is null then 0 else 出庫金額 end as 出庫金額," + "\n"
				+ "case when sum(出庫90) is null then 0 else sum(出庫90) end as 出庫90," + "\n"
				+ "case when 工数 is null then '0.00' else 工数 end as 工数," + "\n"
				+ "case when 工数累計 is null then '0.00' else 工数累計 end as 工数累計" + "\n"
				// "-------------------------------------------------------------------------------------------------------"
				// + "\n" +
				// " --表示製番" + "\n" +
				// " --今月売上＋売上がまだ or 今月仕入 or 今月出庫 or 今月工数" + "\n" +
				+ " from (" + "\n"
				+ "	select p.製作親ID,pc.ID,p.製作期,p.製作番号,p.製作枝番,売上年月日,納品区分CD from T_製作_親 p" + "\n"
				+ "	left outer join T_売上_子 sc on p.製作親ID=sc.製作親ID" + "\n"
				+ "	left outer join T_製作_子 pc on p.製作親ID=pc.製作親ID and (pc.ID=sc.製作子ID or (pc.ID is not null and sc.製作子ID is null))"
				+ "\n"
				+ "	left outer join T_売上_親 sp on sc.売上親ID=sp.売上親ID" + "\n"
				+ "	left outer join (" + "\n"
				+ "		select 注文期,注文番号,注文枝番,納品書日 from T_在庫_親 op " + "\n"
				+ "		left outer join T_在庫_子 oc on op.在庫親ID=oc.在庫親ID" + "\n"
				+ "		left outer join T_指定納品書 s on oc.納品書番号=s.ID" + "\n"
				+ "		where 納品書日>=? and 納品書日<?" + "\n"
				+ "	) o on p.製作期=注文期 and p.製作番号=注文番号 and p.製作枝番=注文枝番" + "\n"
				+ "	left outer join (" + "\n"
				+ "		select 製作期,製作番号,製作枝番,出庫年月日 from T_出庫_親 dp " + "\n"
				+ "		where 出庫年月日>=? and 出庫年月日<?" + "\n"
				+ "	) d on p.製作期=d.製作期 and p.製作番号=d.製作番号 and p.製作枝番=d.製作枝番" + "\n"
				+ "	left outer join (" + "\n"
				+ "		select 製作期,製作番号,製作枝番,着手日時 from T_加工実績" + "\n"
				+ "		where 着手日時>=? and 着手日時<?" + "\n"
				+ "	) w on p.製作期=w.製作期 and p.製作番号=w.製作番号 and p.製作枝番=w.製作枝番" + "\n"
				+ "	where 売上年月日>=? or 売上年月日 is null or 納品書日 is not null or 出庫年月日 is not null or 着手日時 is not null) p2"
				+ "\n"
				// "-------------------------------------------------------------------------------------------------------"
				// + "\n" +
				// "月初累計" + "\n" +
				+ "left outer join (" + "\n"
				+ "	select 注文期,p.注文番号,p.注文枝番,sum(金額) as 繰越仕入 from T_在庫_親 p " + "\n"
				+ "	left outer join T_在庫_子 c on p.在庫親ID=c.在庫親ID" + "\n"
				+ "	left outer join T_指定納品書 s on c.納品書番号=s.ID" + "\n"
				+ "	where 納品書日<? " + "\n"
				+ "	group by 注文期,注文番号,注文枝番) stp" + "\n"
				+ "	on p2.製作期=stp.注文期 and p2.製作番号=stp.注文番号 and p2.製作枝番=stp.注文枝番" + "\n"
				+ "left outer join (" + "\n"
				+ "	select 製作期,製作番号,製作枝番,sum(金額) as 繰越出庫 from T_出庫_親 p" + "\n"
				+ "	left outer join T_出庫_子 c on p.出庫親ID=c.出庫親ID" + "\n"
				+ "	where 出庫年月日<? " + "\n"
				+ "	group by 製作期,製作番号,製作枝番) sdp" + "\n"
				+ "	on p2.製作期=sdp.製作期 and p2.製作番号=sdp.製作番号 and p2.製作枝番=sdp.製作枝番" + "\n"
				+ "left outer join (" + "\n"
				+ "	select 在庫親ID,在庫子ID,sum(金額)*-1 as 繰越90 from T_出庫_子 dc" + "\n"
				+ "	left outer join T_出庫_親 dp on dc.出庫親ID=dp.出庫親ID" + "\n"
				+ "	where 出庫年月日<?" + "\n"
				+ "	group by 在庫親ID,在庫子ID) sdp90" + "\n"
				+ "	on 在庫親ID=p2.製作親ID AND 在庫子ID=p2.ID" + "\n"
				+ "left outer join (" + "\n"
				+ "	select 在庫親ID,在庫子ID,sum(金額)*-1 as 出庫90 from T_出庫_子 dc" + "\n"
				+ "	left outer join T_出庫_親 dp on dc.出庫親ID=dp.出庫親ID" + "\n"
				+ "	where 出庫年月日<? AND 出庫年月日>=?" + "\n"
				+ "	group by 在庫親ID,在庫子ID) dp90" + "\n"
				+ "	on dp90.在庫親ID=p2.製作親ID AND dp90.在庫子ID=p2.ID" + "\n"
				+ "left outer join (" + "\n"
				+ "	select 製作期,製作番号,製作枝番,convert(varchar,convert(money,sum(時間))/100) as 工数累計 from T_加工実績" + "\n"
				+ "	where 着手日時<?" + "\n"
				+ "	group by 製作期,製作番号,製作枝番) w on w.製作期=p2.製作期 and w.製作番号=p2.製作番号 and w.製作枝番=p2.製作枝番" + "\n"
				// "-------------------------------------------------------------------------------------------------------"
				// + "\n" +
				// "--当月の仕入、出庫、工数の合計" + "\n" +
				+ "left outer join (" + "\n"
				+ "select" + "\n"
				+ "pp.製作期,pp.製作番号,pp.製作枝番,仕入金額,出庫金額,工数" + "\n"
				+ " from T_製作_親 pp" + "\n"
				+ " left outer join (" + "\n"
				+ "	select 注文期,注文番号,注文枝番,sum(金額) as 仕入金額 from T_在庫_親 p" + "\n"
				+ "	left outer join T_在庫_子 c on p.在庫親ID=c.在庫親ID" + "\n"
				+ "	left outer join T_指定納品書 s on c.納品書番号=s.ID" + "\n"
				+ "	where 納品書日>=? and 納品書日<?" + "\n"
				+ "	group by 注文期,注文番号,注文枝番) stp" + "\n"
				+ "	on stp.注文期=pp.製作期 and stp.注文番号=pp.製作番号 and stp.注文枝番=pp.製作枝番" + "\n"
				+ "left outer join (" + "\n"
				+ "	select 製作期,製作番号,製作枝番,sum(金額) as 出庫金額 from T_出庫_親 p" + "\n"
				+ "	left outer join T_出庫_子 c on p.出庫親ID=c.出庫親ID" + "\n"
				+ "	where 出庫年月日>=? and 出庫年月日<?" + "\n"
				+ "	group by 製作期,製作番号,製作枝番) sdp" + "\n"
				+ "	on sdp.製作期=pp.製作期 and sdp.製作番号=pp.製作番号 and sdp.製作枝番=pp.製作枝番" + "\n"
				+ "left outer join (" + "\n"
				+ "	select 製作期,製作番号,製作枝番,convert(varchar,convert(money,sum(時間))/100) as 工数 from T_加工実績" + "\n"
				+ "	where 着手日時>=? and 着手日時<?" + "\n"
				+ "	group by 製作期,製作番号,製作枝番) w" + "\n"
				+ "	on w.製作期=pp.製作期 and w.製作番号=pp.製作番号 and w.製作枝番=pp.製作枝番" + "\n"
				+ "where pp.製作番号>0 and (仕入金額<>0 or 出庫金額<>0 or 工数<>'0')" + "\n"
				+ ") z on z.製作期=p2.製作期 and z.製作番号=p2.製作番号 and z.製作枝番=p2.製作枝番" + "\n"
				// "-------------------------------------------------------------------------------------------------------"
				// + "\n" +
				+ "where (p2.製作番号>0 or p2.製作番号 is null) and (z.製作番号>0 or z.製作番号 is null) and (繰越仕入<>0 or 繰越出庫<>0 or 工数累計<>'0' or 仕入金額<>0 or 出庫金額<>0 or 工数<>'0')"
				+ "\n"
				+ "group by p2.製作期,p2.製作番号,p2.製作枝番,z.製作期,z.製作番号,z.製作枝番,仕入金額,出庫金額,工数,工数累計,p2.売上年月日,p2.納品区分CD,繰越仕入,繰越出庫"
				+ "\n"
				+ "order by substring(convert(varchar,case when p2.製作番号 is null then z.製作番号 else p2.製作番号 end),1,1),製番";
			try {
				ps = c.prepareStatement(sql);
				int n = 1;
				ps.setDate(n++, from); // 売
				ps.setDate(n++, to);
				ps.setDate(n++, month == 1 ? from : from2); // 納品
				ps.setDate(n++, month == 12 ? to : to2); // 納品
				ps.setDate(n++, from); // 出庫
				ps.setDate(n++, to); // 出庫
				ps.setDate(n++, from); // 着手
				ps.setDate(n++, to); // 着手
				ps.setDate(n++, from); // 売
				ps.setDate(n++, month == 1 ? from : from2); // 納品
				ps.setDate(n++, from); // 出庫
				ps.setDate(n++, from); // 繰越90
				ps.setDate(n++, to); // 出庫90
				ps.setDate(n++, from); // 出庫90
				ps.setDate(n++, to); // 着手

				ps.setDate(n++, month == 1 ? from : from2); // 納品
				ps.setDate(n++, month == 12 ? to : to2); // 納品
				ps.setDate(n++, from); // 出庫
				ps.setDate(n++, to); // 出庫
				ps.setDate(n++, from); // 着手
				ps.setDate(n++, to); // 着手
				rs = ps.executeQuery();

				while (rs.next()) {
					Vector<Object> record = new Vector<Object>();
					record.add(rs.getString("製番"));
					record.add(rs.getString("売"));
					record.add(rs.getInt("繰越金額") + rs.getInt("繰越90"));
					record.add(rs.getInt("仕入金額"));
					record.add(rs.getInt("出庫金額") + rs.getInt("出庫90"));
					record.add(rs.getInt("仕入金額") + rs.getInt("出庫金額") + rs.getInt("出庫90"));
					record.add(
						rs.getInt("繰越金額") + rs.getInt("繰越90") + rs.getInt("仕入金額") + rs.getInt("出庫金額")
							+ rs.getInt("出庫90")
					);
					record.add(rs.getString("工数"));
					record.add(rs.getString("工数累計"));
					data.add(record);
				}
				rs.close();

			} catch (SQLException ex) {
				ex.printStackTrace();
				err.append(ex + "\n");
				lg.error("GetElements3 " + ex);
			}

			// クライアントに送信

			response.setContentType("application/octet-stream");
			ObjectOutputStream out = new ObjectOutputStream(response.getOutputStream());
			out.writeObject(data);
			out.writeUTF(err.toString());
			out.flush();
			out.close();
		} catch (Exception ex) {
			ex.printStackTrace();
			lg.error(ex);
		} finally {
			try {
				if (c != null && !c.isClosed())
					c.close();
			} catch (SQLException ex) {
				lg.error("c:" + ex);
			}
			// The following processes requires JDBC4.0.
			try {
				if (ps != null && !ps.isClosed()) {
					ps.close();
					lg.debug("ps is closed by jdbc4.0");
				}
			} catch (SQLException ex) {
				lg.error("ps:" + ex);
			}
			try {
				if (rs != null && !rs.isClosed()) {
					rs.close();
					lg.debug("rs is closed by jdbc4.0");
				}
			} catch (SQLException ex) {
				lg.error("rs:" + ex);
			}
		}
	}
}