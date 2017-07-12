package fukaisystem;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.servlet.GenericServlet;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import org.apache.log4j.Logger;

import fukaisystem.dto.ChartDTO;
import fukaisystem.dto.ProductNumber;
import fukaisystem.sql.DBConnection;
import fukaisystem.util.Logging;

public class SetCombo extends GenericServlet {

	/**
	 *
	 */
	private static final long serialVersionUID = 1L;
    private static final Logger lg = Logger.getLogger("A1");
	private static final String className = "SetCombo\n";

	public void service(ServletRequest request, ServletResponse response) {

		DBConnection dbc = new DBConnection();
		Connection c = dbc.getConnection();
		PreparedStatement ps = null;
		ResultSet rs = null;
		StringBuilder err = new StringBuilder();

		int code = 0;
		String name = "";
		String display = "";

		Map<Integer, String> contacts = new LinkedHashMap<Integer, String>();
		Map<Integer, String> models = new LinkedHashMap<Integer, String>();
		Map<Integer, ProductNumber> numbers = new HashMap<Integer, ProductNumber>();

		try {

			/**
			 * ÉNÉâÉCÉAÉìÉgÉfÅ[É^éÛÇØéÊÇË
			 */
			ObjectInputStream in = new ObjectInputStream(request.getInputStream());
			Object obj = in.readObject();
			in.close();

			if(obj == null) {
				err.append(className + "readObjectÇ™nullÇ≈Ç∑\n");
				lg.error(className + "readObjectÇ™nullÇ≈Ç∑");
			} else {
				if(obj instanceof Integer) {
					code = (Integer)obj;
				} else {
					err.append(className + "readObjectÇ™Stringå^Ç≈ÇÕÇ†ÇËÇ‹ÇπÇÒ\n");
					lg.error(className + "readObjectÇ™Stringå^Ç≈ÇÕÇ†ÇËÇ‹ÇπÇÒ");
				}
			}

			try {
				ps = c.prepareStatement("SELECT" +
					" CASE" +
					" WHEN éÌï CD = 1 THEN 'áä'+âÔé–ñº + CASE WHEN éxìXñº IS NULL THEN '' ELSE ' ' + éxìXñº END" +
					" WHEN éÌï CD = 2 THEN âÔé–ñº+'áä' + CASE WHEN éxìXñº IS NULL THEN '' ELSE ' ' + éxìXñº END" +
					" WHEN éÌï CD = 3 THEN 'áã'+âÔé–ñº + CASE WHEN éxìXñº IS NULL THEN '' ELSE ' ' + éxìXñº END" +
					" WHEN éÌï CD = 4 THEN âÔé–ñº+'áã' + CASE WHEN éxìXñº IS NULL THEN '' ELSE ' ' + éxìXñº END" +
					" ELSE âÔé–ñº END," +
					" ï\é¶ñº" +
					" FROM  M_ñ@êl c" +
					" WHERE ìæà”êÊCD=?");
				ps.setInt(1, code);
				rs = ps.executeQuery();
				if(rs.next()) {
					name = rs.getString(1);
					display = rs.getString(2);
				}
				ps = c.prepareStatement("SELECT ã@äBî‘çÜ,àƒåèñº,êªçÏä˙,êªçÏî‘çÜ,êªçÏé}î‘ FROM T_êªçÏ_êe" +
						" WHERE ìæà”êÊCD=? AND ã@äBî‘çÜ>0 ORDER BY êªçÏä˙,êªçÏî‘çÜ");
				ps.setInt(1, code);
				rs = ps.executeQuery();
				while(rs.next()) {
					models.put(rs.getInt("ã@äBî‘çÜ"), rs.getString("àƒåèñº"));
					numbers.put(rs.getInt("ã@äBî‘çÜ"), new ProductNumber(rs.getInt("êªçÏä˙"),rs.getInt("êªçÏî‘çÜ"),rs.getString("êªçÏé}î‘")));
				}

				ps = c.prepareStatement("SELECT p.CD,éÅñº FROM M_å¬êl p" +
						 " LEFT OUTER JOIN M_ñ@êl c" +
						 " ON p.ñ@êlCD=c.CD" +
						" WHERE ìæà”êÊCD=? ORDER BY p.CD");
				ps.setInt(1, code);
				rs = ps.executeQuery();
				while(rs.next()) {
					contacts.put(rs.getInt("CD"), rs.getString("éÅñº"));
				}
			} catch(SQLException ex) {
				ex.printStackTrace();
				Logging.logStackTrace(ex, lg, className);
			}


			/**
			 * ÉNÉâÉCÉAÉìÉgÇ…ëóêM
			 */

			ChartDTO cd = new ChartDTO(name, display, contacts, models, numbers);
			response.setContentType("application/octet-stream");
			ObjectOutputStream out = new ObjectOutputStream(response.getOutputStream());
			out.writeObject(cd);
			out.writeUTF("acitve:"+dbc.getNumActive()+" idle:"+dbc.getNumIdle());
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
					lg.debug(className + "ps closed by jdbc4.0");
				}
			} catch(SQLException ex) {
				Logging.logStackTrace(ex, lg, className);
			}
			try {
				if(rs != null && !rs.isClosed()) {
					rs.close();
					lg.debug(className + "rs closed by jdbc4.0");
				}
			} catch(SQLException ex) {
				Logging.logStackTrace(ex, lg, className);
			}
		}
	}

}
