package fukaisystem;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

import javax.servlet.GenericServlet;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import org.apache.log4j.Logger;

import fukaisystem.dto.DispatchingDTO;
import fukaisystem.sql.DBConnection;
import fukaisystem.util.Logging;

public class DispatchingSearch extends GenericServlet {

	private static final long serialVersionUID = 1L;
	private static final Logger lg = Logger.getLogger("A1");
	private static final String className = "DispatchingSearch\n";

	@Override
	public void service(ServletRequest request, ServletResponse response) {
		DBConnection dbc = new DBConnection();
		Connection c = dbc.getConnection();
		PreparedStatement ps = null;
		ResultSet rs = null;
		DispatchingDTO dispatchingDTO = null;
		StringBuilder err = new StringBuilder();
		List<Integer> strIndex = new ArrayList<Integer>();
		List<Integer> intIndex = new ArrayList<Integer>();

		Vector<Vector<Object>> v = new Vector<Vector<Object>>();

		String[] constStrs = {
			"製作枝番 like ?",
			"SUBSTRING(CONVERT(VARCHAR, 出庫年月日),1,4) like ?",
			"SUBSTRING(CONVERT(VARCHAR, 出庫年月日),6,2) like ?",
			"SUBSTRING(CONVERT(VARCHAR, 出庫年月日),9,2) like ?",
			"用途 like ?",
			"摘要 like ?",
			"注文枝番 like ?",
			"品名 like ?"
		};
		String[] constInts = { "製作期=?", "製作番号=?", "大分類CD=?", "中分類CD=?", "小分類CD=?", "注文期=?", "注文番号=?" };
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
				if (obj instanceof DispatchingDTO) {
					dispatchingDTO = (DispatchingDTO) obj;
				} else {
					err.append(className + "readObjectがProjectSearchDTO型ではありません\n");
					lg.error(className + "readObjectがProjectSearchDTO型ではありません");
				}
			}
			try {
				StringBuilder query = new StringBuilder(
					"SELECT top 30000 出庫親ID,製作期,製作番号,製作枝番,出庫年月日,用途,摘要 FROM ("
						+ " SELECT p.出庫親ID,p.製作期,p.製作番号,p.製作枝番,出庫年月日,用途,p.摘要,注文期,注文番号,注文枝番 FROM T_出庫_親 p"
						+ " LEFT OUTER JOIN T_出庫_子 c ON p.出庫親ID=c.出庫親ID"
						+ " LEFT OUTER JOIN ("
						+ "  SELECT 在庫親ID,注文期,注文番号,注文枝番 FROM T_在庫_親 zp"
						+ "  UNION"
						+ "  SELECT 製作親ID,製作期,製作番号,製作枝番 FROM T_製作_親 pp"
						+ " ) z on z.在庫親ID=c.在庫親ID"
				);
				boolean isFirst = true;
				// 文字列の検索条件は、searchDTO.getStr(0～7)
				for (int i = 0; i < 8; i++) {
					if (!dispatchingDTO.getStr(i).equals("")) { // 検索条件が入っていれば
						strIndex.add(i);
						if (isFirst) {
							query.append(" WHERE ");
							isFirst = false;
						} else {
							if (dispatchingDTO.isAnd())
								query.append(" AND ");
							else
								query.append(" OR ");
						}
						query.append(constStrs[i]);
					}
				}

				// 数値の検索条件は、searchDTO.getInt(0～6)
				for (int i = 0; i < 7; i++) {
					if (dispatchingDTO.getInt(i) != 0) { // 検索条件が入っていれば
						intIndex.add(i);
						if (isFirst) {
							query.append(" WHERE " + constInts[i]);
							isFirst = false;
						} else {
							if (dispatchingDTO.isAnd())
								query.append(" AND " + constInts[i]);
							else
								query.append(" OR " + constInts[i]);
						}
					}
				}
				query.append(") a GROUP BY 出庫親ID,製作期,製作番号,製作枝番,出庫年月日,用途,摘要");
				ps = c.prepareStatement(query.toString());
				int j = 1;
				for (int i : strIndex) {
					ps.setString(j, dispatchingDTO.getStr(i));
					j++;
				}
				for (int i : intIndex) {
					ps.setInt(j, dispatchingDTO.getInt(i));
					j++;
				}
				rs = ps.executeQuery();
				while (rs.next()) {
					Vector<Object> v2 = new Vector<Object>();
					v2.add(rs.getInt("出庫親ID"));
					if (rs.getInt("製作期") != 0 && rs.getInt("製作番号") != 0) {
						v2.add(rs.getInt("製作期") + "-" + rs.getInt("製作番号") + " " + rs.getString("製作枝番"));
					} else {
						v2.add("");
					}
					v2.add(rs.getDate("出庫年月日"));
					v2.add(rs.getString("用途"));
					v2.add(rs.getString("摘要"));
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

	public String partialDateStr(String target, String ymd, int begin, int count) {
		switch (begin) {
			case 1:
			return "substring(convert(varchar(" + count + "), " + target + ", 120), " + begin + ", " + count + ")='" + ymd + "'";
			case 6:
			return "substring(convert(varchar(" + (count + 5) + "), " + target + ", 120), " + begin + ", " + count + ")='" + ymd + "'";
			default:
			return "substring(convert(varchar(10), " + target + ", 120), " + begin + ", " + count + ")='" + ymd + "'";
		}
	}
}
