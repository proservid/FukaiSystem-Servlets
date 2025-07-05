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

import fukaisystem.dto.IDDTO;
import fukaisystem.sql.DBConnection;
import fukaisystem.util.Logging;

public class GetRecord extends GenericServlet {

	private static final long serialVersionUID = 1L;
	private static final Logger lg = Logger.getLogger("A1");
	private static final String className = "GetLine\n";

	@Override
	public void service(ServletRequest request, ServletResponse response) {
		DBConnection dbc = new DBConnection();
		Connection c = dbc.getConnection();
		PreparedStatement ps = null;
		ResultSet rs = null;
		StringBuilder err = new StringBuilder();

		Vector<Object> record = new Vector<Object>();

		int quotationID = 0;
		int productionID = 0;

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
				if (obj instanceof IDDTO) {
					quotationID = ((IDDTO) obj).getQuotationID();
					productionID = ((IDDTO) obj).getProductionID();
				} else {
					err.append(className + "readObjectがIDDTO型ではありません\n");
					lg.error(className + "readObjectがIDDTO型ではありません");
				}
			}
			try {
				StringBuilder query = new StringBuilder(
					"SELECT e.見積親ID,p.製作親ID,s.売上親ID,"
						+ "convert(varchar, 見積期) AS 見積期,"
						+ "見積番号,"
						+ "見積枝番,"
						+ "CASE WHEN e.見積枝番 IS NULL THEN '' ELSE e.見積枝番 END AS 見積枝番," + "\n"
						+ " CASE WHEN p.得意先CD=0 OR p.得意先CD IS NULL THEN e.案件名 ELSE p.案件名 END AS 案件名," + "\n"
						+ " CASE WHEN p.得意先CD=0 OR p.得意先CD IS NULL THEN e.種類 ELSE p.種類 END AS 種類,"
						+ " CASE WHEN p.得意先CD=0 OR p.得意先CD IS NULL THEN "
						+ "right('00' + convert(varchar, e.誕生期), 2) "
						+ "ELSE "
						+ "right('00' + convert(varchar, p.誕生期), 2) "
						+ "END AS 誕生期,"
						+ " CASE WHEN p.得意先CD=0 OR p.得意先CD IS NULL THEN e.誕生番号 ELSE p.誕生番号 END AS 誕生番号,"
						+ " CASE WHEN p.得意先CD=0 OR p.得意先CD IS NULL THEN e.誕生枝番 ELSE p.誕生枝番 END AS 誕生枝番,"
						+ " CASE WHEN p.得意先CD=0 OR p.得意先CD IS NULL THEN e.得意先CD ELSE p.得意先CD END AS 得意先CD,"
						+ " CASE"
						+ " WHEN 種別CD = 1 THEN '㈱'+会社名 + CASE WHEN 支店名 IS NULL THEN '' ELSE ' ' + 支店名 END"
						+ " WHEN 種別CD = 2 THEN 会社名+'㈱' + CASE WHEN 支店名 IS NULL THEN '' ELSE ' ' + 支店名 END"
						+ " WHEN 種別CD = 3 THEN '㈲'+会社名 + CASE WHEN 支店名 IS NULL THEN '' ELSE ' ' + 支店名 END"
						+ " WHEN 種別CD = 4 THEN 会社名+'㈲' + CASE WHEN 支店名 IS NULL THEN '' ELSE ' ' + 支店名 END"
						+ " ELSE 会社名 END AS 社名,"
						+ " 提出年月日,"
						+ " 見積金額,受注番号,受注年月日,p.納期,p.納入先名,"
						+ "right('00' + convert(varchar, 製作期), 2) AS 製作期,"
						+ "製作番号,製作枝番,"
						+ " p.発行年月日 AS 製作年月日,契約金額,出荷年月日,検収年月日"
						+ " FROM (SELECT "
						+ "ee.見積親ID, 見積期, 見積番号, 見積枝番, ee.案件名, ee.得意先CD, 納期CD, 受渡場所CD, 取引条件CD, "
						+ "有効期間CD, 提出済CD, 見積年月日, 提出年月日, ee.通貨CD, 見積金額, ee.摘要, ee.更新日, ee.更新者CD, "
						+ "製作期 AS 誕生期, 製作番号 AS 誕生番号, 製作枝番 AS 誕生枝番, 機械番号 AS 種類"
						+ " FROM T_見積_親 ee LEFT OUTER JOIN T_製作_親 eo ON ee.元製作親ID=eo.製作親ID) e"
						+ " LEFT OUTER JOIN T_見積製作 map ON e.見積親ID=map.見積親ID"
						+ " FULL OUTER JOIN (SELECT "
						+ "pp.製作親ID, pp.製作期, pp.製作番号, pp.製作枝番, h.元製作親ID, pp.受注番号, pp.案件名, pp.見積親ID, pp.得意先CD,"
						+ " pp.機械番号, pp.納入先名, pp.納期, pp.受注年月日, pp.発行年月日, pp.出荷年月日, pp.検収年月日,"
						+ " pp.通貨CD, pp.契約金額, pp.摘要, pp.出図FLG, pp.手配FLG, pp.更新日, pp.更新者CD,"
						+ " po.製作期 AS 誕生期, po.製作番号 AS 誕生番号, po.製作枝番 AS 誕生枝番, po.機械番号 AS 種類"
						+ " FROM T_製作_親 pp"
						+ " LEFT OUTER JOIN T_カルテ履歴 h ON pp.製作親ID=h.製作親ID"
						+ " LEFT OUTER JOIN T_製作_親 po ON h.元製作親ID=po.製作親ID) p"
						+ " ON map.製作親ID=p.製作親ID"
						+ " LEFT OUTER JOIN (SELECT MIN(sp.売上親ID) AS 売上親ID,製作親ID FROM T_売上_親 sp LEFT OUTER JOIN T_売上_子 sc ON sp.売上親ID=sc.売上親ID GROUP BY 製作親ID) s"
						+ " ON p.製作親ID=s.製作親ID"
						+ " LEFT OUTER JOIN M_法人 c"
						+ " ON (CASE WHEN e.得意先CD=0 OR e.得意先CD IS NULL THEN p.得意先CD ELSE e.得意先CD END)=c.得意先CD"
						+ " WHERE "
				);
				if (quotationID != 0) {
					query.append("e.見積親ID=? AND ");
				}
				if (productionID == 0) {
					query.append("p.製作親ID IS NULL");
				} else {
					query.append("p.製作親ID=?");
				}
				ps = c.prepareStatement(query.toString());
				int i = 1;
				if (quotationID != 0) {
					ps.setInt(i, quotationID);
					i++;
				}
				if (productionID != 0) {
					ps.setInt(i, productionID);
				}
				rs = ps.executeQuery();
				while (rs.next()) {
					record.add(new IDDTO(rs.getInt("見積親ID"), rs.getInt("製作親ID"), rs.getInt("売上親ID")));
					int quotationNum = rs.getInt("見積番号");
					if (quotationNum != 0) {
						if (quotationNum < 10) {
							record.add(
								rs.getString("見積期") + "-00" + rs.getString("見積番号") + " " + rs.getString("見積枝番")
							);
						} else if (quotationNum < 100) {
							record.add(
								rs.getString("見積期") + "-0" + rs.getString("見積番号") + " " + rs.getString("見積枝番")
							);
						} else {
							record.add(rs.getString("見積期") + "-" + rs.getString("見積番号") + " " + rs.getString("見積枝番"));
						}
					} else {
						record.add("");
					}
					if (rs.getInt("製作期") != 0 && rs.getInt("製作番号") != 0) {
						record.add(rs.getInt("製作期") + "-" + rs.getInt("製作番号") + " " + rs.getString("製作枝番"));
					} else {
						record.add("");
					}
					if (rs.getInt("誕生期") != 0 && rs.getInt("誕生番号") != 0) {
						record.add(rs.getInt("誕生期") + "-" + rs.getInt("誕生番号") + " " + rs.getString("誕生枝番"));
					} else {
						record.add("");
					}
					if (rs.getInt("得意先CD") != 0) {
						record.add(/* rs.getInt("得意先CD") + "：" + */rs.getString("社名"));
					} else {
						record.add("");
					}
					record.add(rs.getString("納入先名"));
					record.add(rs.getString("案件名"));
					// record.add(rs.getInt("種類"));
					record.add(rs.getDate("提出年月日"));
					record.add(rs.getInt("見積金額"));
					record.add(rs.getString("受注番号"));
					record.add(rs.getDate("受注年月日"));
					record.add(rs.getDate("製作年月日"));
					record.add(rs.getDate("納期"));
					record.add(rs.getInt("契約金額"));
					record.add(rs.getDate("出荷年月日"));
					record.add(rs.getDate("検収年月日"));

				}

			} catch (SQLException ex) {
				err.append(className + "テーブル「T_見積_親」の読み出しに失敗しました\n");
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
			out.writeObject(record);
			out.writeUTF(err.toString());
			out.flush();
			out.close();
		} catch (Exception ex) {
			lg.error(ex);
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
