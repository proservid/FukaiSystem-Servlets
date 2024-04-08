package fukaisystem;

import java.io.*;
import java.sql.*;
import java.sql.Date;
import java.util.*;
import javax.servlet.*;

import fukaisystem.sql.DBConnection;

import org.apache.log4j.Logger;

/**
 * ƒe[ƒuƒ‹‚Ì“à—e‚Æ—ñî•ñ‚ğæ“¾‚·‚é‚½‚ß‚ÌƒNƒ‰ƒX
 * @author kameura
 *
 */
public class GetShelf extends GenericServlet {

	private static final long serialVersionUID = 1L;
	private static Logger lg = Logger.getLogger("A1");

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
	//ƒNƒ‰ƒCƒAƒ“ƒg‚©‚ç“Ç‚İ‚İ

			ObjectInputStream in = new ObjectInputStream(request.getInputStream());
			Object obj = in.readObject();
			if(obj instanceof Date) {
				from = (Date)obj;
				Calendar target = Calendar.getInstance();
				target.setTime(from);//¡Œ1“ú
				month = target.get(Calendar.MONTH) + 1;
				target.add(Calendar.MONTH, 1);//—‚Œ1“ú
				to = new Date(target.getTimeInMillis());
				target.setTime(from);//¡Œ1“úiŸ‚Ì2014”N”»’è‚Ì‚½‚ß‚É‚±‚Ìƒ^ƒCƒ~ƒ“ƒO‚ÅƒZƒbƒg‚·‚é•K—v‚ª‚ ‚éj
				if(target.get(Calendar.YEAR) < 2014) {//2013”NˆÈ‘O‚Í25“úY
					if(target.get(Calendar.MONTH) == 11) {//12Œ‚Í11Œ26“ú‚©‚ç12Œ31“ú
						to2 = to;
						target.add(Calendar.DATE, 25);//¡Œ26“ú
						target.add(Calendar.MONTH, -1);//æŒ26“ú
						from2 = new Date(target.getTimeInMillis());
					} else {
						target.add(Calendar.DATE, 25);//¡Œ26“ú
						to2 = new Date(target.getTimeInMillis());
						if(target.get(Calendar.MONTH) == 0) {//1Œ‚Í1Œ1“ú‚©‚ç1Œ25“ú
							from2 = from;
						} else {
							target.add(Calendar.MONTH, -1);//æŒ26“ú
							from2 = new Date(target.getTimeInMillis());
						}
					}
				} else {//2014”NˆÈŒã‚ÍŒ––Y
					to2 = to;
					from2 = from;
				}
			}
			in.close();
String sql =
"select distinct" + "\n" +
"substring(convert(varchar,case when p2.»ì”Ô† is null then z.»ì”Ô† else p2.»ì”Ô† end),1,1) as x," + "\n" +
"case when p2.»ì”Ô† is null" + "\n" +
" then convert(varchar,z.»ìŠú)+'-'+convert(varchar,z.»ì”Ô†)+convert(varchar,z.»ì}”Ô)" + "\n" +
" else convert(varchar,p2.»ìŠú)+'-'+convert(varchar,p2.»ì”Ô†)+convert(varchar,p2.»ì}”Ô) end as »”Ô," + "\n" +
"case when (”[•i‹æ•ªCD=2 or ”[•i‹æ•ªCD=4 or ”[•i‹æ•ªCD=5 or ”[•i‹æ•ªCD=6) then (case when ”„ã”NŒ“ú<? then '#' else (case when ”„ã”NŒ“ú<? then '*' else '' end) end) else '' end as ”„," + "\n" +
"case when ŒJ‰zd“ü is null and ŒJ‰zoŒÉ is null then 0" + "\n" +
"when ŒJ‰zd“ü is null then ŒJ‰zoŒÉ" + "\n" +
"when ŒJ‰zoŒÉ is null then ŒJ‰zd“ü" + "\n" +
"else ŒJ‰zd“ü+ŒJ‰zoŒÉ" + "\n" +
"end as ŒJ‰z‹àŠz," + "\n" +
"case when d“ü‹àŠz is null then 0 else d“ü‹àŠz end as d“ü‹àŠz," + "\n" +
"case when oŒÉ‹àŠz is null then 0 else oŒÉ‹àŠz end as oŒÉ‹àŠz," + "\n" +
"case when H” is null then '0.00' else H” end as H”," + "\n" +
"case when H”—İŒv is null then '0.00' else H”—İŒv end as H”—İŒv" + "\n" +
//"-------------------------------------------------------------------------------------------------------" + "\n" +
//" --•\¦»”Ô" + "\n" +
//" --¡Œ”„ã{”„ã‚ª‚Ü‚¾ or ¡Œd“ü or ¡ŒoŒÉ or ¡ŒH”" + "\n" +
" from (" + "\n" +
"	select p.»ìŠú,p.»ì”Ô†,p.»ì}”Ô,”„ã”NŒ“ú,”[•i‹æ•ªCD from T_»ì_e p" + "\n" +
"	left outer join T_”„ã_q sc on p.»ìeID=sc.»ìeID" + "\n" +
"	left outer join T_”„ã_e sp on sc.”„ãeID=sp.”„ãeID" + "\n" +
"	left outer join (" + "\n" +
"		select ’•¶Šú,’•¶”Ô†,’•¶}”Ô,”[•i‘“ú from T_İŒÉ_e op " + "\n" +
"		left outer join T_İŒÉ_q oc on op.İŒÉeID=oc.İŒÉeID" + "\n" +
"		left outer join T_w’è”[•i‘ s on oc.”[•i‘”Ô†=s.ID" + "\n" +
"		where ”[•i‘“ú>=? and ”[•i‘“ú<?" + "\n" +
"	) o on p.»ìŠú=’•¶Šú and p.»ì”Ô†=’•¶”Ô† and p.»ì}”Ô=’•¶}”Ô" + "\n" +
"	left outer join (" + "\n" +
"		select »ìŠú,»ì”Ô†,»ì}”Ô,oŒÉ”NŒ“ú from T_oŒÉ_e dp " + "\n" +
"		where oŒÉ”NŒ“ú>=? and oŒÉ”NŒ“ú<?" + "\n" +
"	) d on p.»ìŠú=d.»ìŠú and p.»ì”Ô†=d.»ì”Ô† and p.»ì}”Ô=d.»ì}”Ô" + "\n" +
"	left outer join (" + "\n" +
"		select »ìŠú,»ì”Ô†,»ì}”Ô,’…è“ú from T_‰ÁHÀÑ" + "\n" +
"		where ’…è“ú>=? and ’…è“ú<?" + "\n" +
"	) w on p.»ìŠú=w.»ìŠú and p.»ì”Ô†=w.»ì”Ô† and p.»ì}”Ô=w.»ì}”Ô" + "\n" +
"	where ”„ã”NŒ“ú>=? or ”„ã”NŒ“ú is null or ”[•i‘“ú is not null or oŒÉ”NŒ“ú is not null or ’…è“ú is not null) p2" + "\n" +
//"-------------------------------------------------------------------------------------------------------" + "\n" +
//"Œ‰—İŒv" + "\n" +
"left outer join (" + "\n" +
"	select ’•¶Šú,p.’•¶”Ô†,p.’•¶}”Ô,sum(‹àŠz) as ŒJ‰zd“ü from T_İŒÉ_e p " + "\n" +
"	left outer join T_İŒÉ_q c on p.İŒÉeID=c.İŒÉeID" + "\n" +
"	left outer join T_w’è”[•i‘ s on c.”[•i‘”Ô†=s.ID" + "\n" +
"	where ”[•i‘“ú<? " + "\n" +
	"	group by ’•¶Šú,’•¶”Ô†,’•¶}”Ô) stp" + "\n" +
"	on p2.»ìŠú=stp.’•¶Šú and p2.»ì”Ô†=stp.’•¶”Ô† and p2.»ì}”Ô=stp.’•¶}”Ô" + "\n" +
"left outer join (" + "\n" +
"	select »ìŠú,»ì”Ô†,»ì}”Ô,sum(‹àŠz) as ŒJ‰zoŒÉ from T_oŒÉ_e p" + "\n" +
"	left outer join T_oŒÉ_q c on p.oŒÉeID=c.oŒÉeID" + "\n" +
"	where oŒÉ”NŒ“ú<? " + "\n" +
"	group by »ìŠú,»ì”Ô†,»ì}”Ô) sdp" + "\n" +
"	on p2.»ìŠú=sdp.»ìŠú and p2.»ì”Ô†=sdp.»ì”Ô† and p2.»ì}”Ô=sdp.»ì}”Ô" + "\n" +
"left outer join (" + "\n" +
"	select »ìŠú,»ì”Ô†,»ì}”Ô,convert(varchar,convert(money,sum(ŠÔ))/100) as H”—İŒv from T_‰ÁHÀÑ" + "\n" +
"	where ’…è“ú<?" + "\n" +
"	group by »ìŠú,»ì”Ô†,»ì}”Ô) w on w.»ìŠú=p2.»ìŠú and w.»ì”Ô†=p2.»ì”Ô† and w.»ì}”Ô=p2.»ì}”Ô" + "\n" +
//"-------------------------------------------------------------------------------------------------------" + "\n" +
//"--“–Œ‚Ìd“üAoŒÉAH”‚Ì‡Œv" + "\n" +
"left outer join (" + "\n" +
"select" + "\n" +
"pp.»ìŠú,pp.»ì”Ô†,pp.»ì}”Ô,d“ü‹àŠz,oŒÉ‹àŠz,H”" + "\n" +
" from T_»ì_e pp" + "\n" +
" left outer join (" + "\n" +
"	select ’•¶Šú,’•¶”Ô†,’•¶}”Ô,sum(‹àŠz) as d“ü‹àŠz from T_İŒÉ_e p" + "\n" +
"	left outer join T_İŒÉ_q c on p.İŒÉeID=c.İŒÉeID" + "\n" +
"	left outer join T_w’è”[•i‘ s on c.”[•i‘”Ô†=s.ID" + "\n" +
"	where ”[•i‘“ú>=? and ”[•i‘“ú<?" + "\n" +
"	group by ’•¶Šú,’•¶”Ô†,’•¶}”Ô) stp" + "\n" +
"	on stp.’•¶Šú=pp.»ìŠú and stp.’•¶”Ô†=pp.»ì”Ô† and stp.’•¶}”Ô=pp.»ì}”Ô" + "\n" +
"left outer join (" + "\n" +
"	select »ìŠú,»ì”Ô†,»ì}”Ô,sum(‹àŠz) as oŒÉ‹àŠz from T_oŒÉ_e p" + "\n" +
"	left outer join T_oŒÉ_q c on p.oŒÉeID=c.oŒÉeID" + "\n" +
"	where oŒÉ”NŒ“ú>=? and oŒÉ”NŒ“ú<?" + "\n" +
"	group by »ìŠú,»ì”Ô†,»ì}”Ô) sdp" + "\n" +
"	on sdp.»ìŠú=pp.»ìŠú and sdp.»ì”Ô†=pp.»ì”Ô† and sdp.»ì}”Ô=pp.»ì}”Ô" + "\n" +
"left outer join (" + "\n" +
"	select »ìŠú,»ì”Ô†,»ì}”Ô,convert(varchar,convert(money,sum(ŠÔ))/100) as H” from T_‰ÁHÀÑ" + "\n" +
"	where ’…è“ú>=? and ’…è“ú<?" + "\n" +
"	group by »ìŠú,»ì”Ô†,»ì}”Ô) w" + "\n" +
"	on w.»ìŠú=pp.»ìŠú and w.»ì”Ô†=pp.»ì”Ô† and w.»ì}”Ô=pp.»ì}”Ô" + "\n" +
"where pp.»ì”Ô†>0 and (d“ü‹àŠz<>0 or oŒÉ‹àŠz<>0 or H”<>'0')" + "\n" +
") z on z.»ìŠú=p2.»ìŠú and z.»ì”Ô†=p2.»ì”Ô† and z.»ì}”Ô=p2.»ì}”Ô" + "\n" +
//"-------------------------------------------------------------------------------------------------------" + "\n" +
"where (p2.»ì”Ô†>0 or p2.»ì”Ô† is null) and (z.»ì”Ô†>0 or z.»ì”Ô† is null) and (ŒJ‰zd“ü<>0 or ŒJ‰zoŒÉ<>0 or H”—İŒv<>'0' or d“ü‹àŠz<>0 or oŒÉ‹àŠz<>0 or H”<>'0')" + "\n" +
"order by substring(convert(varchar,case when p2.»ì”Ô† is null then z.»ì”Ô† else p2.»ì”Ô† end),1,1),»”Ô";
			try {
				ps = c.prepareStatement(sql);
int n = 1;
				ps.setDate(n++, from);//”„
				ps.setDate(n++, to);
				ps.setDate(n++, month == 1 ? from : from2);//”[•i
				ps.setDate(n++, month == 12 ? to : to2);//”[•i
				ps.setDate(n++, from);//oŒÉ
				ps.setDate(n++, to);//oŒÉ
				ps.setDate(n++, from);//’…è
				ps.setDate(n++, to);//’…è
				ps.setDate(n++, from);//”„
				ps.setDate(n++, month == 1 ? from : from2);//”[•i
				ps.setDate(n++, from);//oŒÉ
				ps.setDate(n++, to);//’…è
				ps.setDate(n++, month == 1 ? from : from2);//”[•i
				ps.setDate(n++, month == 12 ? to : to2);//”[•i
				ps.setDate(n++, from);//oŒÉ
				ps.setDate(n++, to);//oŒÉ
				ps.setDate(n++, from);//’…è
				ps.setDate(n++, to);//’…è
				rs = ps.executeQuery();

				while(rs.next()) {
					Vector<Object> row = new Vector<Object>();
					row.add(rs.getString("»”Ô"));
					row.add(rs.getString("”„"));
					row.add(rs.getInt("ŒJ‰z‹àŠz"));
					row.add(rs.getInt("d“ü‹àŠz"));
					row.add(rs.getInt("oŒÉ‹àŠz"));
					row.add(rs.getInt("d“ü‹àŠz")+rs.getInt("oŒÉ‹àŠz"));
					row.add(rs.getInt("ŒJ‰z‹àŠz")+rs.getInt("d“ü‹àŠz")+rs.getInt("oŒÉ‹àŠz"));
					row.add(rs.getString("H”"));
					row.add(rs.getString("H”—İŒv"));
					data.add(row);
				}
				rs.close();

			} catch(SQLException ex) {
				ex.printStackTrace();
				err.append(ex + "\n");
				lg.error("GetElements3 " + ex);
			}

	//ƒNƒ‰ƒCƒAƒ“ƒg‚É‘—M

			response.setContentType("application/octet-stream");
			ObjectOutputStream out = new ObjectOutputStream(response.getOutputStream());
			out.writeObject(data);
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
}