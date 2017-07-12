package fukaisystem.address;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import javax.servlet.GenericServlet;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import org.apache.log4j.Logger;

import fukaisystem.dto.CorpDTO;
import fukaisystem.dto.IndDTO;
import fukaisystem.sql.DBConnection;
import fukaisystem.util.Logging;


public class GetIndData extends GenericServlet {

	/**
	 *
	 */
	private static final long serialVersionUID = 1L;
	static final Logger lg = Logger.getLogger("A1");
	private static final String className = "GetIndData\n";

	public void service(ServletRequest request, ServletResponse response) {

		DBConnection dbc = new DBConnection();
		Connection c = dbc.getConnection();
		PreparedStatement ps = null;
		ResultSet rs = null;
		String input = "";
		IndDTO output = null;
		StringBuilder err = new StringBuilder("");

		try {

			/**
			 * ƒNƒ‰ƒCƒAƒ“ƒgƒf[ƒ^ó‚¯æ‚è
			 */
			ObjectInputStream in = new ObjectInputStream(request.getInputStream());
			Object obj = in.readObject();
			in.close();

			if(obj == null) {
				err.append(className + "readObject‚ªnull‚Å‚·\n");
				lg.error(className + "readObject‚ªnull‚Å‚·");
			} else {
				if(obj instanceof String) {
					input = (String)obj;
				} else {
					err.append(className + "readObject‚ªStringŒ^‚Å‚Í‚ ‚è‚Ü‚¹‚ñ\n");
					lg.error(className + "readObject‚ªStringŒ^‚Å‚Í‚ ‚è‚Ü‚¹‚ñ");
				}
			}

			try {
				ps = c.prepareStatement("select " +
					"ind.–@lCD as i–@lCD,ind.•”–¼ as i•”–¼,ind.–ğE–¼ as i–ğE–¼,ind.–¼ as i–¼,ind.ƒVƒƒC as iƒVƒƒC,ind.ŒhÌ as iŒhÌ," +
					"ind.TEL1 as iTEL1,ind.TEL2 as iTEL2,ind.TEL3 as iTEL3,ind.FAX1 as iFAX1,ind.FAX2 as iFAX2,ind.FAX3 as iFAX3,ind.ƒ[ƒ‹ as iƒ[ƒ‹,ind.”õl as i”õl," +
					"ind.ZŠFLG as iZŠFLG,ind.—LŒøFLG as i—LŒøFLG,ind.‘¡“šFLG as i‘¡“šFLG,ind.‘rFLG as i‘rFLG,ind.”N‰êóCD as i”N‰êóCD," +
					"indad.alpha_2 as ialpha_2,indad.—X•Ö”Ô† as i—X•Ö”Ô†,indad.—X•Ö}”Ô as i—X•Ö}”Ô,indp.“s“¹•{Œ§ as i“s“¹•{Œ§,indc.s‹æ’¬‘º as is‹æ’¬‘º,indpc.’¬ˆæ as i’¬ˆæ,indad.”Ô’n as i”Ô’n,indad.Œš•¨“™ as iŒš•¨“™,indad.©‘îFLG as i©‘îFLG," +
					"co.d“üæCD as cd“üæCD,co.“¾ˆÓæCD as c“¾ˆÓæCD,co.í•ÊCD as cí•ÊCD,co.‰ïĞ–¼ as c‰ïĞ–¼,co.ƒJƒCƒVƒƒƒƒC as cƒJƒCƒVƒƒƒƒC,co.x“X–¼ as cx“X–¼,co.ƒVƒeƒ“ƒƒC as cƒVƒeƒ“ƒƒC,co.•\¦–¼ as c•\¦–¼,co.ƒAƒ‹ƒtƒ@ƒxƒbƒg as cƒAƒ‹ƒtƒ@ƒxƒbƒg," +
					"co.alpha_2 as calpha_2,co.—X•Ö”Ô† as c—X•Ö”Ô†,co.—X•Ö}”Ô as c—X•Ö}”Ô,cop.“s“¹•{Œ§ as c“s“¹•{Œ§,coc.s‹æ’¬‘º as cs‹æ’¬‘º,copc.’¬ˆæ as c’¬ˆæ,co.”Ô’n as c”Ô’n,co.Œš•¨“™ as cŒš•¨“™," +
					"co.TEL1 as cTEL1,co.TEL2 as cTEL2,co.TEL3 as cTEL3,co.FAX1 as cFAX1,co.FAX2 as cFAX2,co.FAX3 as cFAX3,co.ƒ[ƒ‹ as cƒ[ƒ‹,co.URL as cURL,co.”õl as c”õl,co.—LŒøFLG as c—LŒøFLG,co.‘¡“šFLG as c‘¡“šFLG,co.”N‰êóCD as c”N‰êóCD," +
					"CASE" +
					" WHEN í•ÊCD = 1 THEN '‡Š'+‰ïĞ–¼ + CASE WHEN x“X–¼ IS NULL THEN '' ELSE ' ' + x“X–¼ END" +
					" WHEN í•ÊCD = 2 THEN ‰ïĞ–¼+'‡Š' + CASE WHEN x“X–¼ IS NULL THEN '' ELSE ' ' + x“X–¼ END" +
					" WHEN í•ÊCD = 3 THEN '‡‹'+‰ïĞ–¼ + CASE WHEN x“X–¼ IS NULL THEN '' ELSE ' ' + x“X–¼ END" +
					" WHEN í•ÊCD = 4 THEN ‰ïĞ–¼+'‡‹' + CASE WHEN x“X–¼ IS NULL THEN '' ELSE ' ' + x“X–¼ END" +
					" ELSE ‰ïĞ–¼ + CASE WHEN x“X–¼ IS NULL THEN '' ELSE ' ' + x“X–¼ END" +
					" END AS Ğ–¼" +
					" from M_ŒÂl ind" +
					" left outer join M_ŒÂlZŠ indad on ind.CD=indad.ŒÂlCD" +
					" left outer join V_—X•Ö”Ô† indpc on replace(indad.—X•Ö”Ô†,'-','')=indpc.—X•Ö”Ô† and indad.—X•Ö}”Ô=indpc.—X•Ö}”Ô" +
					" left outer join M_“s“¹•{Œ§ indp on indpc.“s“¹•{Œ§CD=indp.CD" +
					" left outer join M_s‹æ’¬‘º indc on indpc.“s“¹•{Œ§CD=indc.“s“¹•{Œ§CD and indpc.s‹æ’¬‘ºCD=indc.CD" +
					" left outer join M_–@l co on ind.–@lCD=co.CD" +
					" left outer join V_—X•Ö”Ô† copc on replace(co.—X•Ö”Ô†,'-','')=copc.—X•Ö”Ô† and co.—X•Ö}”Ô=copc.—X•Ö}”Ô" +
					" left outer join M_“s“¹•{Œ§ cop on copc.“s“¹•{Œ§CD=cop.CD" +
					" left outer join M_s‹æ’¬‘º coc on copc.“s“¹•{Œ§CD=coc.“s“¹•{Œ§CD and copc.s‹æ’¬‘ºCD=coc.CD" +
					" where ind.CD=?");
				ps.setString(1, input);
				System.out.println("input:"+input);
				rs = ps.executeQuery();
				if(rs.next()) {
					CorpDTO corpDTO = null;
					boolean hasCorpDTO = false;
					if(rs.getInt("i–@lCD") > 0) {
						hasCorpDTO = true;
						corpDTO = new CorpDTO(
								rs.getString("c‰ïĞ–¼"), rs.getString("cƒJƒCƒVƒƒƒƒC"), rs.getString("cx“X–¼"), rs.getString("cƒVƒeƒ“ƒƒC"), rs.getString("c•\¦–¼"),
								rs.getString("calpha_2"), rs.getString("c—X•Ö”Ô†"), rs.getString("c—X•Ö}”Ô"), rs.getString("c“s“¹•{Œ§"), rs.getString("cs‹æ’¬‘º"),
								rs.getString("c’¬ˆæ"), rs.getString("c”Ô’n"), rs.getString("cŒš•¨“™"), rs.getString("cTEL1"), rs.getString("cTEL2"), rs.getString("cTEL3"),
								rs.getString("cFAX1"), rs.getString("cFAX2"), rs.getString("cFAX3"), rs.getString("cƒ[ƒ‹"), rs.getString("cURL"), rs.getString("c”õl"),
								rs.getString("i–@lCD"), rs.getString("cƒAƒ‹ƒtƒ@ƒxƒbƒg"), rs.getInt("cd“üæCD"), rs.getInt("c“¾ˆÓæCD"), rs.getInt("cí•ÊCD"), rs.getInt("c”N‰êóCD"),
								rs.getBoolean("c—LŒøFLG"), rs.getBoolean("c‘¡“šFLG")
								);
					}
					output = new IndDTO(
						rs.getString("i–¼"), rs.getString("iƒVƒƒC"), rs.getString("i•”–¼"), rs.getString("i–ğE–¼"), rs.getString("i–@lCD"),
						rs.getString("ialpha_2"), rs.getString("i—X•Ö”Ô†"), rs.getString("i—X•Ö}”Ô"), rs.getString("i“s“¹•{Œ§"), rs.getString("is‹æ’¬‘º"), rs.getString("i’¬ˆæ"), rs.getString("i”Ô’n"),
						rs.getString("iŒš•¨“™"), rs.getString("iTEL1"), rs.getString("iTEL2"), rs.getString("iTEL3"), rs.getString("iFAX1"), rs.getString("iFAX2"), rs.getString("iFAX3"), rs.getString("iƒ[ƒ‹"), "", rs.getString("i”õl"), input, rs.getString("Ğ–¼"),
						rs.getInt("iŒhÌ"), rs.getInt("i”N‰êóCD"),
						rs.getBoolean("i—LŒøFLG"), rs.getBoolean("i‘¡“šFLG"), rs.getBoolean("i‘rFLG"), rs.getBoolean("i©‘îFLG"), rs.getBoolean("iZŠFLG"), hasCorpDTO, corpDTO
						);
				}
			} catch(SQLException ex) {
				err.append("ƒe[ƒuƒ‹uT_ƒe[ƒuƒ‹–¼v‚Ì“Ç‚É¸”s‚µ‚Ü‚µ‚½\n");
				Logging.logStackTrace(ex, lg, className);
			}

		} catch(Exception ex) {
			Logging.logStackTrace(ex, lg, className);
		}

		/**
		 * ƒNƒ‰ƒCƒAƒ“ƒg‚É‘—M
		 */
		try {
			response.setContentType("application/octet-stream");
			ObjectOutputStream out = new ObjectOutputStream(response.getOutputStream());
			out.writeObject(output);
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
