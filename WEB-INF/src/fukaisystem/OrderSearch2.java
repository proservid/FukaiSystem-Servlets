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

import fukaisystem.dto.OrderSearchDTO;
import fukaisystem.sql.DBConnection;
import fukaisystem.util.Logging;

public class OrderSearch2 extends GenericServlet {

	private static final long serialVersionUID = 1L;
	private static final Logger lg = Logger.getLogger("A1");
	private static final String className = "Search\n";

	@Override
	public void service(ServletRequest request, ServletResponse response) {
		DBConnection dbc = new DBConnection();
		Connection c = dbc.getConnection();
		PreparedStatement ps = null;
		ResultSet rs = null;
		OrderSearchDTO dto = null;
		StringBuilder err = new StringBuilder();
		List<Integer> stringConditonIndex = new ArrayList<Integer>();
		List<Integer> intConditionIndex = new ArrayList<Integer>();

		Vector<Vector<Object>> data = new Vector<Vector<Object>>();

		String[] constStrs = { "注文枝番 like ?",
			"SUBSTRING(CONVERT(VARCHAR, 注文年月日),1,4) like ?",
			"SUBSTRING(CONVERT(VARCHAR, 注文年月日),6,2) like ?",
			"SUBSTRING(CONVERT(VARCHAR, 注文年月日),9,2) like ?",
			"SUBSTRING(CONVERT(VARCHAR, 指定納期),1,4) like ?",
			"SUBSTRING(CONVERT(VARCHAR, 指定納期),6,2) like ?",
			"SUBSTRING(CONVERT(VARCHAR, 指定納期),9,2) like ?",
			"SUBSTRING(CONVERT(VARCHAR, 入庫年月日),1,4) like ?",
			"SUBSTRING(CONVERT(VARCHAR, 入庫年月日),6,2) like ?",
			"SUBSTRING(CONVERT(VARCHAR, 入庫年月日),9,2) like ?",
			"SUBSTRING(CONVERT(VARCHAR, 納品書日),1,4) like ?",
			"SUBSTRING(CONVERT(VARCHAR, 納品書日),6,2) like ?",
			"SUBSTRING(CONVERT(VARCHAR, 納品書日),9,2) like ?" };
		String[] constInts = { "c.仕入先CD=?", "注文期=?", "注文番号=?", "伝票番号=?", "納品書番号=?" };
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
				if (obj instanceof OrderSearchDTO) {
					dto = (OrderSearchDTO) obj;
				} else {
					err.append(className + "readObjectがOrderSearchDTO型ではありません\n");
					lg.error(className + "readObjectがOrderSearchDTO型ではありません");
				}
			}
			try {
				StringBuilder query = new StringBuilder(
					"SELECT top 30000 s.在庫親ID,注文期,注文番号,注文枝番,"
						+ " 伝票番号,s.仕入先CD,注文年月日,指定納期,入庫年月日,"
						+ " 摘要,納入先指定,納品書番号,納品書日,"
						+ " CASE"
						+ " WHEN 種別CD = 1 THEN '㈱'+会社名"
						+ " WHEN 種別CD = 2 THEN 会社名+'㈱'"
						+ " WHEN 種別CD = 3 THEN '㈲'+会社名"
						+ " WHEN 種別CD = 4 THEN 会社名+'㈲'"
						+ " ELSE 会社名 END AS 社名"
						+ " FROM ("
						+ "	SELECT sp.在庫親ID,入庫年月日,納品書番号,納品書日 FROM T_在庫_親 sp"
						+ "	 LEFT OUTER JOIN (SELECT * FROM T_在庫_子 WHERE 表示CD=2) sc ON sp.在庫親ID=sc.在庫親ID"
						+ "	 LEFT OUTER JOIN T_指定納品書 d on d.ID=sc.納品書番号"
				);
				boolean isFirst = true;
				for (int i = 1; i < 13; i++) {
					if (!dto.getStr(i).equals("")) { // 検索条件が入っていれば
						if (isFirst) {
							query.append(" WHERE (");
							isFirst = false;
						} else {
							if (dto.isAnd())
								query.append(" AND ");
							else
								query.append(" OR ");
						}
						stringConditonIndex.add(i);
						if (dto.getStr(i).equals("未")) {
							switch (i) {
								case 1:
									query.append("注文年月日 IS NULL");
									break;
								case 4:
									query.append("指定納期 IS NULL");
									break;
								case 7:
									query.append("入庫年月日 IS NULL");
									break;
								case 10:
									query.append("納品書日 IS NULL");
									break;
							}
						} else {
							query.append(constStrs[i]);
						}
					}
				}
				if (!isFirst)
					query.append(")");
				query.append(
					" GROUP BY sp.在庫親ID,入庫年月日,納品書番号,納品書日) spc"
						+ " LEFT OUTER JOIN T_在庫_親 s ON s.在庫親ID=spc.在庫親ID"
						+ " LEFT OUTER JOIN M_法人 c ON s.仕入先CD=c.仕入先CD"
				);

				isFirst = true;
				if (!dto.getStr(0).equals("")) { // 検索条件が入っていれば
					stringConditonIndex.add(0);
					if (isFirst) {
						query.append(" WHERE ");
						query.append(constStrs[0]);
						isFirst = false;
					}
				}

				// 数値の検索条件は、searchDTO.getInt(0～4)
				for (int i = 0; i < 5; i++) {
					if (dto.getInt(i) != 0) { // 検索条件が入っていれば
						intConditionIndex.add(i);
						if (isFirst) {
							query.append(" WHERE " + constInts[i]);
							isFirst = false;
						} else {
							if (dto.isAnd())
								query.append(" AND " + constInts[i]);
							else
								query.append(" OR " + constInts[i]);
						}
					}
				}
				ps = c.prepareStatement(query.toString());
				int j = 1;
				for (int i : stringConditonIndex) {
					if (!dto.getStr(i).equals("未")) {
						ps.setString(j, dto.getStr(i));
						j++;
					}
				}
				for (int i : intConditionIndex) {
					ps.setInt(j, dto.getInt(i));
					j++;
				}
				rs = ps.executeQuery();
				while (rs.next()) {
					Vector<Object> record = new Vector<Object>();
					record.add(rs.getInt("在庫親ID"));
					if (rs.getInt("注文期") != 0 && rs.getInt("注文番号") != 0) {
						record.add(rs.getInt("注文期") + "-" + rs.getInt("注文番号") + " " + rs.getString("注文枝番"));
					} else {
						record.add("");
					}
					record.add(rs.getInt("伝票番号"));
					if (rs.getInt("仕入先CD") != 0) {
						record.add(/* rs.getInt("仕入先CD") + "：" + */rs.getString("社名"));
					} else {
						record.add("");
					}
					record.add(rs.getDate("注文年月日"));
					record.add(rs.getDate("指定納期"));
					record.add(rs.getDate("入庫年月日"));
					record.add(rs.getString("摘要"));
					record.add(rs.getString("納入先指定"));
					record.add(rs.getInt("納品書番号"));
					record.add(rs.getDate("納品書日"));
					data.add(record);
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
			out.writeObject(data);
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
