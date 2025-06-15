package fukaisystem;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Vector;

import javax.servlet.GenericServlet;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import org.apache.log4j.Logger;

import fukaisystem.dto.SalesDTO;
import fukaisystem.sql.DBConnection;
import fukaisystem.util.Logging;

public class GetSalesSlip extends GenericServlet {

	private static final long serialVersionUID = 1L;
	private static final Logger lg = Logger.getLogger("A1");
	private static final String className = "ChangeSlip\n";

	@Override
	public void service(ServletRequest request, ServletResponse response) {
		DBConnection dbc = new DBConnection();
		Connection c = dbc.getConnection();
		PreparedStatement ps = null;
		ResultSet rs = null;
		SalesDTO dDTO = null;
		StringBuilder err = new StringBuilder();

		int deliveryID = 0;
		int tax = 0, discount = 0;

		Vector<Vector<Object>> deliveryData = new Vector<Vector<Object>>();

		try {

			/**
			 * クライアントデータ受け取り
			 */
			ObjectInputStream in = new ObjectInputStream(request.getInputStream());
			Object obj = in.readObject();
			in.close();

			if (obj == null) {
				deliveryID = 0;
			} else {
				if (obj instanceof Integer) {
					deliveryID = (Integer) obj;
				} else {
					err.append(className + "readObjectがInteger型ではありません\n");
					lg.error(className + "readObjectがInteger型ではありません");
				}
			}
		} catch (Exception ex) {
			Logging.logStackTrace(ex, lg, className);
		}

		try {
			ps = c.prepareStatement(
				"SELECT 納品区分CD,納品手段CD,得意先CD,売上年月日,売上FLG,請求FLG,消費税,値引き,摘要 FROM T_売上_親"
					+ " WHERE 売上親ID=?"
			);
			ps.setInt(1, deliveryID);
			rs = ps.executeQuery();
			if (rs.next()) {
				int type = 0;
				if (!rs.getBoolean("売上FLG")) {
					type = 2;
				} else if (!rs.getBoolean("請求FLG")) {
					type = 1;
				}
				if (rs.getString("消費税") == null) {
					tax = -1;
				} else {
					tax = rs.getInt("消費税");
				}

				discount = rs.getInt("値引き");
				//sDTO = new SalesDTO(deliveryID, rs.getInt("納品区分CD"),rs.getInt("納品手段CD"),rs.getInt("得意先CD"),type,rs.getDate("売上年月日"),rs.getString("摘要"),null);
				dDTO = new SalesDTO(
					deliveryID,
					0,
					0,
					rs.getDate("売上年月日"),
					rs.getInt("納品区分CD"),
					rs.getInt("納品手段CD"),
					tax,
					discount,
					type,
					rs.getString("摘要"),
					null
				);
			}

			// 売上明細
			ps = c.prepareStatement(
				"SELECT sc.製作親ID,sc.製作子ID,sc.表示CD,出荷伝票番号,pp.受注年月日,sp.売上年月日,pp.受注番号,sc.品名,sc.各FLG,sc.数量,sc.数量単位CD,sc.単価,sc.金額,sc.備考,pc.完成年月日 FROM T_売上_子 sc"
					+ " LEFT OUTER JOIN T_製作_親 pp ON sc.製作親ID=pp.製作親ID"
					+ " LEFT OUTER JOIN T_製作_子 pc ON sc.製作親ID=pc.製作親ID AND sc.製作子ID=pc.ID"
					+ " LEFT OUTER JOIN T_売上_親 sp ON sc.売上親ID=sp.売上親ID"
					+ " WHERE sc.売上親ID=? ORDER BY sc.ID"
			);
			ps.setInt(1, deliveryID);
			rs = ps.executeQuery();
			while (rs.next()) {
				Vector<Object> line = new Vector<Object>();
				int price = rs.getInt("金額");
				if (rs.getInt("表示CD") == 5) {
					price = tax;
				} else if (rs.getInt("表示CD") == 6) {
					price = discount;
				}
				line.add(rs.getInt("製作親ID"));
				line.add(rs.getInt("製作子ID"));
				line.add(rs.getInt("表示CD"));
				line.add(rs.getString("出荷伝票番号"));
				line.add(rs.getDate("受注年月日"));
				line.add(rs.getString("受注番号"));
				line.add(rs.getString("品名"));
				line.add(rs.getBoolean("各FLG"));
				line.add(rs.getInt("数量"));
				line.add(rs.getInt("数量単位CD"));
				line.add(rs.getInt("単価"));
				line.add(price);
				line.add(rs.getString("備考"));
				line.add(rs.getDate("完成年月日"));
				line.add(rs.getDate("売上年月日"));
				deliveryData.add(line);
			}
			if (dDTO != null)
				dDTO.setVector(deliveryData);
		} catch (SQLException ex) {
			err.append(ex.toString());
			Logging.logStackTrace(ex, lg, className);
		}

		/**
		 * クライアントに送信
		 */
		try {
			response.setContentType("application/octet-stream");
			ObjectOutputStream out = new ObjectOutputStream(response.getOutputStream());
			out.writeObject(dDTO);
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
