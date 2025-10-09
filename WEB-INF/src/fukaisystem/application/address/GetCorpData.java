package fukaisystem.application.address;

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
import fukaisystem.sql.DBConnection;
import fukaisystem.util.Logging;

public class GetCorpData extends GenericServlet {

	/**
	 *
	 */
	private static final long serialVersionUID = 1L;
	static final Logger lg = Logger.getLogger("A1");
	private static final String className = "GetCorpData\n";

	public void service(ServletRequest request, ServletResponse response) {

		DBConnection dbc = new DBConnection();
		Connection c = dbc.getConnection();
		PreparedStatement ps = null;
		ResultSet rs = null;
		String input = "";
		CorpDTO output = null;
		StringBuilder err = new StringBuilder("");

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
				if (obj instanceof String) {
					input = (String) obj;
				} else {
					err.append(className + "readObjectがString型ではありません\n");
					lg.error(className + "readObjectがString型ではありません");
				}
			}

			try {
				ps = c.prepareStatement(
					"select "
						+ "会社名, カイシャメイ, 支店名, シテンメイ, 表示名,"
						+ "alpha_2, co.郵便番号, co.郵便枝番, p.都道府県,市区町村,町域,"
						+ "番地,建物等,TEL1,TEL2,TEL3,FAX1,FAX2,FAX3,メール,URL,"
						+ "備考,アルファベット,登録番号,仕入先CD,得意先CD,種別CD,有効FLG,贈答FLG,年賀状CD"
						+ " from M_法人 co"
						+ " left outer join V_郵便番号 pc on replace(co.郵便番号,'-','')=pc.郵便番号 and co.郵便枝番=pc.郵便枝番"
						+ " left outer join M_都道府県 p on pc.都道府県CD=p.CD"
						+ " left outer join M_市区町村 c on pc.都道府県CD=c.都道府県CD and pc.市区町村CD=c.CD"
						+ " where co.CD=?");
				ps.setString(1, input);
				rs = ps.executeQuery();
				if (rs.next()) {
					output = new CorpDTO(
						rs.getString("会社名"), rs.getString("カイシャメイ"), rs.getString("支店名"), rs.getString("シテンメイ"),
						rs.getString("表示名"),
						rs.getString("alpha_2"), rs.getString("郵便番号"), rs.getString("郵便枝番"), rs.getString("都道府県"),
						rs.getString("市区町村"),
						rs.getString("町域"), rs.getString("番地"), rs.getString("建物等"), rs.getString("TEL1"),
						rs.getString("TEL2"), rs.getString("TEL3"),
						rs.getString("FAX1"), rs.getString("FAX2"), rs.getString("FAX3"), rs.getString("メール"),
						rs.getString("URL"), rs.getString("備考"),
						input, rs.getString("アルファベット"), rs.getString("登録番号"),
						rs.getInt("仕入先CD"), rs.getInt("得意先CD"), rs.getInt("種別CD"), rs.getInt("年賀状CD"),
						rs.getBoolean("有効FLG"), rs.getBoolean("贈答FLG"));
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
			out.writeObject(output);
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
