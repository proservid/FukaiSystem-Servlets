package fukaisystem.input;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Calendar;

import javax.servlet.GenericServlet;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;



import org.apache.log4j.Logger;

import fukaisystem.dto.InputDTO;
import fukaisystem.sql.DBConnection;
import fukaisystem.util.Logging;


public class InputRegistration extends GenericServlet {


	private static final long serialVersionUID = 1L;
    private static final Logger lg = Logger.getLogger("A1");
	private static final String className = "InputRegistration\n";
	private Date from, to, t0820, t1200, t1245, t1700, t1715;
	private int time;

	public void service(ServletRequest request, ServletResponse response) {

		DBConnection dbc = new DBConnection();
		Connection c = dbc.getConnection();
		PreparedStatement ps = null;
		ResultSet rs = null;
		InputDTO inputDTO = null;
		StringBuilder err = new StringBuilder();

		/**
		 * ƒNƒ‰ƒCƒAƒ“ƒgƒf[ƒ^Žó‚¯Žæ‚è
		 */
		ObjectInputStream in;
		try {
			in = new ObjectInputStream(request.getInputStream());
			Object obj = in.readObject();
			in.close();
			if(obj == null) {
				err.append(className + "readObject‚ªnull‚Å‚·\n");
				lg.error(className + "readObject‚ªnull‚Å‚·");
			} else {
				if(obj instanceof InputDTO) {
					inputDTO = (InputDTO)obj;
				} else {
					err.append(className + "readObject‚ªShippingDTOŒ^‚Å‚Í‚ ‚è‚Ü‚¹‚ñ\n");
					lg.error(className + "readObject‚ªShippingDTOŒ^‚Å‚Í‚ ‚è‚Ü‚¹‚ñ");
				}
			}
		} catch (Exception e) {
			// TODO Ž©“®¶¬‚³‚ê‚½ catch ƒuƒƒbƒN
			e.printStackTrace();
		}

		int year = inputDTO.getInt(2);
		int month = inputDTO.getInt(3) - 1;
		int day = inputDTO.getInt(4);
		int fromT = inputDTO.getInt(5);
		int fromM = inputDTO.getInt(6);
		int toT = inputDTO.getInt(7);
		int toM = inputDTO.getInt(8);
		int rest = inputDTO.getInt(9);
		int id = inputDTO.getInt(10);
		Calendar cal = Calendar.getInstance();
		cal.set(year, month, day, fromT, fromM, 0);
		cal.set(Calendar.MILLISECOND, 0);
		from = new Date(cal.getTimeInMillis());
		cal.set(year, month, day, toT, toM, 0);
		cal.set(Calendar.MILLISECOND, 0);
		to = new Date(cal.getTimeInMillis());

		try {
			ps = c.prepareStatement("SELECT * FROM M_A‹ÆŽžŠÔ");
			rs = ps.executeQuery();
			if(rs.next()) {
				int w0 = rs.getInt("Žn‹ÆŽž");
				int w1 = rs.getInt("Žn‹Æ•ª");
				int w2 = rs.getInt("I‹ÆŽž");
				int w3 = rs.getInt("I‹Æ•ª");
				int w4 = rs.getInt("‹xŒeŽnŽž");
				int w5 = rs.getInt("‹xŒeŽn•ª");
				int w6 = rs.getInt("‹xŒeIŽž");
				int w7 = rs.getInt("‹xŒeI•ª");
				int w8 = rs.getInt("Žc‹ÆŽnŽž");
				int w9 = rs.getInt("Žc‹ÆŽn•ª");
				//Šî–{î•ñ
				cal.set(year, month, day, w0, w1, -1);
				t0820 = new Date(cal.getTimeInMillis());
				cal.set(year, month, day, w4, w5, -1);
				t1200 = new Date(cal.getTimeInMillis());
				cal.set(year, month, day, w6, w7, -1);
				t1245 = new Date(cal.getTimeInMillis());
				cal.set(year, month, day, w2, w3, -1);
				t1700 = new Date(cal.getTimeInMillis());
				cal.set(year, month, day, w8, w9, -1);
				t1715 = new Date(cal.getTimeInMillis());

				int br1 = (int)((t1245.getTime() - t1200.getTime()) / (1000 * 60));//45•ª
				//System.out.println("br1:"+br1);
				int br2 = (int)((t1715.getTime() - t1700.getTime()) / (1000 * 60));//15•ª
				//System.out.println("br2:"+br2);

				if(inputDTO.getString(2).equals("17")) {//o’£‚Ìê‡
					cal.setTime(to);
					cal.add(Calendar.MINUTE, rest*(-1));
					setTime(cal.getTimeInMillis() - from.getTime());
				} else {
					if(from.before(t0820)) {
						//System.out.println("f1:"+new Timestamp(from.getTime()));
						//ŠJŽn‚ð0820‚É
						cal.set(year, month, day, w0, w1, 0);
						long adjustedFrom = cal.getTimeInMillis();
						if(to.after(t1715)){
							//I—¹‚©‚ç45•ª+15•ª=1ŽžŠÔˆø‚­
							cal.setTime(to);
							cal.add(Calendar.MINUTE, -br1-br2);
							//System.out.println("t11:"+cal);
						} else if(to.after(t1700)) {
							//45•ªˆø‚«AI‚è‚ð1700‚ÅŒvŽZ
							cal.set(year, month, day, w2, w3, 0);
							cal.add(Calendar.MINUTE, -br1);
							//System.out.println("t12:"+cal);
						} else if(to.after(t1245)) {
							//45•ªˆø‚­‚Ì‚Ý
							cal.setTime(to);
							cal.add(Calendar.MINUTE, -br1);
							//System.out.println("t13:"+cal);
						} else if(to.before(t1200)) {
							//‚»‚Ì‚Ü‚Ü
							cal.setTime(to);
						} else {
							//I—¹‚ð1200‚É
							//System.out.println(new Timestamp(to.getTime()));
							cal.set(year, month, day, w4, w5, 0);
							//System.out.println("t14:"+cal);
						}
						setTime(cal.getTimeInMillis() - adjustedFrom);
					} else if(from.before(t1200)) {
						if(to.after(t1715)){
							//I—¹‚©‚ç45•ª+15•ª=1ŽžŠÔˆø‚­
							cal.setTime(to);
							cal.add(Calendar.MINUTE, -br1-br2);
							//System.out.println("t1:"+cal);
						} else if(to.after(t1700)) {//TODO 1659
							//I—¹‚ð1700‚Æ‚µA45•ªˆø‚­
							cal.set(year, month, day, w2, w3, 0);
							cal.add(Calendar.MINUTE, -br1);
							//System.out.println("f2:"+new Timestamp(from.getTime()));
							//System.out.println("t2:"+new Timestamp(cal.getTimeInMillis()));
						} else if(to.after(t1245)) {
							//I—¹‚©‚ç45•ªˆø‚­
							cal.setTime(to);
							cal.add(Calendar.MINUTE, -br1);
							//System.out.println("t3:"+cal);
						} else if(to.after(t1200)) {
							//I—¹‚ð1200‚É
							cal.set(year, month, day, w4, w5, 0);
							//System.out.println("t4:"+cal);
						} else {
							//I—¹‚ª12Žž‘O‚È‚ç‚»‚Ì‚Ü‚Ü
							cal.setTime(to);
							//System.out.println("t5:"+cal);
						}
						setTime(cal.getTimeInMillis() - from.getTime());
					} else if(from.after(t1715)) {
						//ˆêØ’²®•s—v
						setTime(to.getTime() - from.getTime());
					} else if(from.after(t1700)) {
						//ŠJŽn‚ð1715‚É‚·‚é‚Ì‚Ý
						cal.set(year, month, day, w8, w9, 0);
						setTime(to.getTime() - cal.getTimeInMillis());
					} else if(from.after(t1245)) {
						//ŠJŽnŽž‚Í’²®•s—v
						if(to.after(t1715)) {
							cal.setTime(to);
							cal.add(Calendar.MINUTE, -br2);//I—¹ŽžŠÔ‚©‚ç15•ªˆø‚­
							setTime(cal.getTimeInMillis() - from.getTime());//
						} else if(to.after(t1700)) {
							//I—¹‚ð1700‚É
							cal.set(year, month, day, w2, w3);
							setTime(cal.getTimeInMillis() - from.getTime());
							//System.out.println(new Timestamp(cal.getTimeInMillis()));
							//System.out.println(new Timestamp(from.getTime()));
						} else {
							setTime(to.getTime() - from.getTime());
						}
					} else {//12Žž‚©‚ç12Žž45•ª‚ÌŠÔ‚ÉŽn‚Ü‚Á‚Ä‚¢‚½‚ç
						//ŠJŽn‚ð1245‚É
						cal.set(year, month, day, w6, w7, 0);
						long adjustedFrom = cal.getTimeInMillis();
						if(to.after(t1715)) {
							cal.setTime(to);
							cal.add(Calendar.MINUTE, -br2);//15•ªˆø‚­
							setTime(cal.getTimeInMillis() - adjustedFrom);
						} else if(to.after(t1700)) {
							//I—¹‚ð1700‚É
							cal.set(year, month, day, w2, w3, 0);
							setTime(cal.getTimeInMillis() - adjustedFrom);
						} else {
							setTime(to.getTime() - adjustedFrom);
						}
					}
				}
			}
		} catch(SQLException ex) {
			ex.printStackTrace();
			err.append(className + "ƒe[ƒuƒ‹uM_A‹ÆŽžŠÔv‚Ì“Ç‚Ýo‚µ‚ÉŽ¸”s‚µ‚Ü‚µ‚½\n");
			Logging.logStackTrace(ex, lg, className);
		}


		if(id == 0) {
			try {
				ps = c.prepareStatement("INSERT INTO T_‰ÁHŽÀÑ (»ìŠú,»ì”Ô†,»ìŽ}”Ô,‰ÁHCD,ŽžŠÔ,’…Žè“úŽž,I—¹“úŽž,’S“–ŽÒCD,”õl,“ü—Í“úŽž) VALUES(?,?,?,?,?,?,?,?,?,?)");
				/*
				"MERGE INTO T_‰ÁHŽÀÑ2 AS t"+//(»ìŠú,»ì”Ô†,»ìŽ}”Ô,‰ÁHCD,ŽžŠÔ,’…Žè“úŽž,I—¹“úŽž,’S“–ŽÒCD,”õl)" +
				" USING (SELECT ? AS »ìŠú, ? AS »ì”Ô†, ? AS »ìŽ}”Ô, ? AS ‰ÁHCD, ? AS ŽžŠÔ, ? AS ’…Žè“úŽž, ? AS I—¹“úŽž, ? AS ’S“–ŽÒCD, ? AS ”õl) AS w" +
				" ON t.’…Žè“úŽž=w.’…Žè“úŽž AND t.’S“–ŽÒCD=w.’S“–ŽÒCD" +
				" WHEN MATCHED THEN" +
				"   UPDATE SET t.»ìŠú=w.»ìŠú, t.»ì”Ô†=w.»ì”Ô†, t.»ìŽ}”Ô=w.»ìŽ}”Ô, t.‰ÁHCD=w.‰ÁHCD, t.ŽžŠÔ=w.ŽžŠÔ, t.’…Žè“úŽž=w.’…Žè“úŽž, t.I—¹“úŽž=w.I—¹“úŽž, t.’S“–ŽÒCD=w.’S“–ŽÒCD, t.”õl=w.”õl, t.“ü—Í”NŒŽ“ú=?" +
				" WHEN NOT MATCHED THEN" +
				"   INSERT VALUES(w.»ìŠú, w.»ì”Ô†, w.»ìŽ}”Ô, w.‰ÁHCD, w.ŽžŠÔ, w.’…Žè“úŽž, w.I—¹“úŽž, w.’S“–ŽÒCD, w.”õl, ?)" +
				" OUTPUT deleted.ID as oldId, inserted.’…Žè“úŽž as newId;");
	*/
				System.out.println(inputDTO.getInt(0));
				System.out.println(inputDTO.getInt(1));
				int i = 1;
				ps.setInt(i, inputDTO.getInt(1) == 0 ? 0 : inputDTO.getInt(0)); i++;//”Ô†‚ª0‚È‚çŠú‚à0
				ps.setInt(i, inputDTO.getInt(1)); i++;
				ps.setString(i, inputDTO.getInt(1) == 0 ? "" : inputDTO.getString(3));i++;//”Ô†‚ª0‚È‚çŽ}”Ô‚È‚µ
				ps.setString(i, inputDTO.getString(2)); i++;//‰ÁHCD
				ps.setInt(i, time); i++;//ŽžŠÔ
				ps.setTimestamp(i, new Timestamp(from.getTime())); i++;//’…Žè“úŽž
				ps.setTimestamp(i, new Timestamp(to.getTime())); i++;//I—¹“úŽž
				ps.setString(i, inputDTO.getString(1)); i++;//’S“–ŽÒCD
				ps.setString(i, inputDTO.getString(2).equals("17") ? "‹xŒe"+rest+"•ª" : ""); i++;//”õl
				ps.setTimestamp(i, new Timestamp(new java.util.Date().getTime())); i++;
				ps.executeUpdate();
	 		} catch(SQLException ex) {
				ex.printStackTrace();
				err.append(className + "ƒe[ƒuƒ‹uT_‰ÁHŽÀÑv‚ÌXV‚ÉŽ¸”s‚µ‚Ü‚µ‚½\n");
				Logging.logStackTrace(ex, lg, className);
			}
		} else {
			try {
				ps = c.prepareStatement("UPDATE T_‰ÁHŽÀÑ  SET »ìŠú=?,»ì”Ô†=?,»ìŽ}”Ô=?,‰ÁHCD=?,ŽžŠÔ=?,’…Žè“úŽž=?,I—¹“úŽž=?,’S“–ŽÒCD=?,”õl=?,“ü—Í“úŽž=? WHERE ID=?");
				int i = 1;
				ps.setInt(i, inputDTO.getInt(1) == 0 ? 0 : inputDTO.getInt(0)); i++;//”Ô†‚ª0‚È‚çŠú‚à0
				ps.setInt(i, inputDTO.getInt(1)); i++;
				ps.setString(i, inputDTO.getInt(1) == 0 ? "" : inputDTO.getString(3));i++;//”Ô†‚ª0‚È‚çŽ}”Ô‚È‚µ
				ps.setString(i, inputDTO.getString(2)); i++;//‰ÁHCD
				ps.setInt(i, time); i++;//ŽžŠÔ
				ps.setTimestamp(i, new Timestamp(from.getTime())); i++;//’…Žè“úŽž
				ps.setTimestamp(i, new Timestamp(to.getTime())); i++;//I—¹“úŽž
				ps.setString(i, inputDTO.getString(1)); i++;//’S“–ŽÒCD
				ps.setString(i, inputDTO.getString(2).equals("17") ? "‹xŒe"+rest+"•ª" : ""); i++;//”õl
				ps.setTimestamp(i, new Timestamp(new java.util.Date().getTime())); i++;
				ps.setInt(i, id);
				ps.executeUpdate();
	 		} catch(SQLException ex) {
				ex.printStackTrace();
				err.append(className + "ƒe[ƒuƒ‹uT_‰ÁHŽÀÑv‚ÌXV‚ÉŽ¸”s‚µ‚Ü‚µ‚½\n");
				Logging.logStackTrace(ex, lg, className);
			}
		}


		/**
		 * ƒNƒ‰ƒCƒAƒ“ƒg‚É‘—M
		 */
		try {
			response.setContentType("application/octet-stream");
			ObjectOutputStream out = new ObjectOutputStream(response.getOutputStream());
			//–¢“ü—Í‚Ì»”Ô‚ª‚ ‚é‚Ì‚Å“––Ê»”Ô‚ªŒ©‚Â‚©‚è‚Ü‚¹‚ñƒGƒ‰[‚ðo‚³‚È‚¢
			//out.writeObject(inputDTO.getInt(1) == 0 ? -1 : update);//»ì”Ô†‚ª0‚È‚çAŠY“–‚·‚é»ìƒf[ƒ^‚ª“o˜^‚³‚ê‚Ä‚¢‚È‚­‚Ä‚àOK‚É‚·‚é
			out.writeObject(-1);//»ì”Ô†‚ª0‚È‚çAŠY“–‚·‚é»ìƒf[ƒ^‚ª“o˜^‚³‚ê‚Ä‚¢‚È‚­‚Ä‚àOK‚É‚·‚é
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

	private void setTime(long value) {
		long remainder = value % (1000 * 60 * 15);//ƒ~ƒŠ•b‚ð100•ª‚Ì1•b‚É‚µ‚Ä•ª‚É‚µ‚Ä15•ª’PˆÊ‚É‚·‚é
		//System.out.println("value"+value);
		//‚RŽÌ‚S“ü
		//System.out.println(remainder);
		//System.out.println((float)remainder / (1000 * 60 * 15));
		time = ((float)remainder / (1000 * 60 * 15) >= 0.4) ? ((int)(value / (1000 * 60 * 15)) + 1) * 25 : ((int)(value / (1000 * 60 * 15))) * 25;
		//System.out.println("t:"+time);
	}

}
