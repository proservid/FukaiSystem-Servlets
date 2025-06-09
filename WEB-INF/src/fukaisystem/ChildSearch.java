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
import fukaisystem.dto.ProductNumber;
import fukaisystem.sql.DBConnection;
import fukaisystem.util.Logging;

public class ChildSearch extends GenericServlet {

	private static final long serialVersionUID = 1L;
	private static final Logger lg = Logger.getLogger("A1");
	private static final String className = "ChildSearch\n";

	@Override
	public void service(ServletRequest request, ServletResponse response) {
		DBConnection dbc = new DBConnection();
		Connection c = dbc.getConnection();
		PreparedStatement ps = null;
		ResultSet rs = null;
		StringBuilder err = new StringBuilder();
		int type = 0;
		String str = "";
		String conjunction = "";

		Vector<Vector<Object>> v = new Vector<Vector<Object>>();

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
				// 流用
				if (obj instanceof ProductNumber) {
					ProductNumber pn = (ProductNumber) obj;
					type = pn.getPeriod(); // 検索対象テーブルの種類
					conjunction = pn.getNumber() == 0 ? "AND" : "OR";
					str = pn.getBranch(); // 検索キーワード
				} else {
					err.append(className + "readObjectがProjectSearchDTO型ではありません\n");
					lg.error(className + "readObjectがProjectSearchDTO型ではありません");
				}
			}
			try {
				StringBuilder query = new StringBuilder(
					"select top 30000 "
						+ "e.見積親ID,"
						+ "p.製作親ID,"
						+ "s.売上親ID,"
						+ "見積期,"
						+ "right('000' + convert(varchar, e.見積番号), 3) AS 見積番号,"
						+ "CASE WHEN e.見積枝番 IS NULL THEN '' ELSE e.見積枝番 END AS 見積枝番,"
						+ "CASE WHEN p.得意先CD=0 OR p.得意先CD IS NULL THEN e.案件名 ELSE p.案件名 END AS 案件名,"
						+ "right('00' + convert(varchar, p2.製作期), 2) AS 誕生期,"
						+ "p2.製作番号 AS 誕生番号,"
						+ "p2.製作枝番 AS 誕生枝番,"
						+ "CASE WHEN p.得意先CD=0 OR p.得意先CD IS NULL THEN e.得意先CD ELSE p.得意先CD END AS 得意先CD,"
						+ "CASE "
						+ "WHEN 種別CD = 1 THEN '㈱'+会社名 + CASE WHEN 支店名 IS NULL THEN '' ELSE ' ' + 支店名 END "
						+ "WHEN 種別CD = 2 THEN 会社名+'㈱' + CASE WHEN 支店名 IS NULL THEN '' ELSE ' ' + 支店名 END "
						+ "WHEN 種別CD = 3 THEN '㈲'+会社名 + CASE WHEN 支店名 IS NULL THEN '' ELSE ' ' + 支店名 END "
						+ "WHEN 種別CD = 4 THEN 会社名+'㈲' + CASE WHEN 支店名 IS NULL THEN '' ELSE ' ' + 支店名 END "
						+ "ELSE 会社名 END AS 社名,"
						+ "e.見積年月日,"
						+ "e.提出年月日,"
						+ "e.見積金額,"
						+ "p.受注番号,"
						+ "p.受注年月日,"
						+ "p.納期,"
						+ "p.納入先名,"
						+ "right('00' + convert(varchar, p.製作期), 2) AS 製作期,"
						+ "p.製作番号,"
						+ "CASE WHEN p.製作枝番 IS NULL THEN '' ELSE p.製作枝番 END AS 製作枝番,"
						+ "p.発行年月日 AS 製作年月日,"
						+ "p.契約金額,"
						+ "p.出荷年月日,"
						+ "p.検収年月日"
						+ " from "
				);
				//////////////////////////////////////////////////////////////////////////
				String col = "名称";
				if (type == 3)
					col = "品名";
				StringBuilder sb = new StringBuilder("");
				str = str.replaceAll("　", " ");
				String[] words = str.split(" ");
				int count = words.length;
				for (int j = 0; j < count; j++) {
					if (j > 0) {
						sb.append(" " + conjunction + " ");
					}
					sb.append(col + " like ?");
				}
				//////////////////////////////////////////////////////////////////
				switch (type) {
					case 1:// 見積
						query.append(
							"(select 見積親ID from T_見積_子 where " + sb.toString() + " group by 見積親ID) a"
								+ " left outer join T_製作_親 p on a.見積親ID=p.見積親ID"
								+ " left outer join T_見積_親 e on a.見積親ID=e.見積親ID"
								+ " left outer join T_製作_親 p2 on e.元製作親ID=p2.製作親ID"
								+ " left outer join (select min(売上親ID) as 売上親ID,製作親ID from T_売上_子 group by 製作親ID) sc on p.製作親ID=sc.製作親ID"
								+ " left outer join T_売上_親 s on s.売上親ID=sc.売上親ID"
								+ " left outer join M_法人 c on e.得意先CD=c.得意先CD"
								+ " where e.得意先CD is not null"
						);
						break;
					case 2:// 製作
						query.append(
							"(select 製作親ID from T_製作_子 where " + sb.toString() + " group by 製作親ID) a"
								+ " left outer join T_製作_親 p on a.製作親ID=p.製作親ID"
								+ " left outer join T_見積_親 e on p.見積親ID=e.見積親ID"
								+ " left outer join T_製作_親 p2 on e.元製作親ID=p2.製作親ID"
								+ " left outer join (select min(売上親ID) as 売上親ID,製作親ID from T_売上_子 group by 製作親ID) sc on p.製作親ID=sc.製作親ID"
								+ " left outer join T_売上_親 s on s.売上親ID=sc.売上親ID"
								+ " left outer join M_法人 c on p.得意先CD=c.得意先CD"
								+ " where p.得意先CD is not null"
						);
						break;
					case 3:// 売上
						query.append(
							"(select 製作親ID,売上親ID from T_売上_子 where " + sb.toString() + " group by 製作親ID,売上親ID) a"
								+ " left outer join T_製作_親 p on a.製作親ID=p.製作親ID"
								+ " left outer join T_見積_親 e on p.見積親ID=e.見積親ID"
								+ " left outer join T_製作_親 p2 on e.元製作親ID=p2.製作親ID"
								+ " left outer join T_売上_親 s on s.売上親ID=a.売上親ID"
								+ " left outer join M_法人 c on p.得意先CD=c.得意先CD"
								+ " where p.得意先CD is not null"
						);
						break;
				}
				ps = c.prepareStatement(query.toString());
				for (int i = 0; i < count; i++) {
					ps.setString(i + 1, "%" + words[i] + "%");
				}
				rs = ps.executeQuery();
				while (rs.next()) {
					Vector<Object> v2 = new Vector<Object>();
					v2.add(new IDDTO(rs.getInt("見積親ID"), rs.getInt("製作親ID"), rs.getInt("売上親ID")));
					if (rs.getInt("見積番号") != 0) {
						v2.add(rs.getString("見積期") + "-" + rs.getString("見積番号") + " " + rs.getString("見積枝番"));
					} else {
						v2.add("");
					}
					if (rs.getInt("製作番号") != 0) {
						v2.add(rs.getString("製作期") + "-" + rs.getInt("製作番号") + " " + rs.getString("製作枝番"));
					} else {
						v2.add("");
					}
					// v2.add(rs.getInt("種類"));
					if (rs.getInt("誕生番号") != 0) {
						v2.add(rs.getString("誕生期") + "-" + rs.getInt("誕生番号") + " " + rs.getString("誕生枝番"));
					} else {
						v2.add("");
					}

					if (rs.getInt("得意先CD") != 0) {
						v2.add(/* rs.getInt("得意先CD") + "：" + */rs.getString("社名"));
					} else {
						v2.add("");
					}
					v2.add(rs.getString("納入先名"));
					v2.add(rs.getString("案件名"));

					v2.add(rs.getDate("見積年月日"));
					v2.add(rs.getInt("見積金額"));
					v2.add(rs.getString("受注番号"));
					v2.add(rs.getDate("受注年月日"));
					v2.add(rs.getDate("製作年月日"));
					v2.add(rs.getDate("納期"));
					v2.add(rs.getInt("契約金額"));
					v2.add(rs.getDate("出荷年月日"));
					v2.add(rs.getDate("検収年月日"));

					v.add(v2);
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
			out.writeObject(v);
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
