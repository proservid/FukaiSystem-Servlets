package fukaisystem.application.aggregate;

import java.io.IOException;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;

import javax.servlet.ServletResponse;

import fukaisystem.foundation.ServiceFoundation;

/**
 * 得意先別製番別売上集計を取得するためのクラス
 *
 * @author kameura
 *
 */
public class GetSalesData extends ServiceFoundation {

	@Override
	public Object access(Connection c, ServletResponse response, Object o) throws IOException, SQLException {

		Map<String, Integer> m = new HashMap<String, Integer>();
		Date from = cast(response, o, Date.class);
		if (from == null) {
			return null;
		}
		Calendar target = Calendar.getInstance();
		target.setTime(from);
		target.add(Calendar.MONTH, 1);
		Date to = new Date(target.getTimeInMillis());

		Vector<Vector<Object>> contents = new Vector<Vector<Object>>();

		try (
			PreparedStatement ps = c.prepareStatement(
				"SELECT"
					+ " 得意先CD,"
					+ " ROUND(CAST(税合計 AS DECIMAL(18,9)),2) AS 税額"
					+ " FROM ("
					+ "SELECT"
					+ " 得意先CD,"
					+ " ROUND(SUM(金額) * ("
					+ "SELECT 税率"
					+ " FROM M_消費税 t"
					+ " WHERE 適用開始日<? AND NOT EXISTS ("
					+ "SELECT 1 FROM M_消費税 t2"
					+ " WHERE t.適用開始日<t2.適用開始日 AND 適用開始日<?"
					+ ")"
					+ "),0) AS 税合計"
					+ " FROM V_売上集計ヘッダ "
					+ " WHERE 売上年月日>=? AND 売上年月日<?"
					+ " GROUP BY 得意先CD"
					+ ") a"
			);
		) {
			ps.setDate(1, to);
			ps.setDate(2, to);
			ps.setDate(3, from);
			ps.setDate(4, to);
			ResultSet rs = ps.executeQuery();
			while (rs.next()) {
				m.put(rs.getString("得意先CD"), rs.getInt("税額"));
			}
		}

		try (
			PreparedStatement ps = c.prepareStatement(
				"select 得意先CD,得意先名,受注番号,日付,sum(金額) as 金額 from ("
					+ " select "
					+ " sp.得意先CD,"
					+ " CASE WHEN 種別CD=1 THEN '㈱'+会社名 + CASE WHEN 支店名 IS NULL THEN '' ELSE ' ' + 支店名 END"
					+ "      WHEN 種別CD=2 THEN 会社名+'㈱' + CASE WHEN 支店名 IS NULL THEN '' ELSE ' ' + 支店名 END"
					+ "      WHEN 種別CD=3 THEN '㈲'+会社名 + CASE WHEN 支店名 IS NULL THEN '' ELSE ' ' + 支店名 END"
					+ "      WHEN 種別CD=4 THEN 会社名+'㈲' + CASE WHEN 支店名 IS NULL THEN '' ELSE ' ' + 支店名 END"
					+ "      ELSE 会社名 END AS 得意先名,"
					+ " convert(varchar, pp.製作期)+'-'+convert(varchar, pp.製作番号)+pp.製作枝番 as 受注番号,"
					+ " sp.売上年月日 as 日付,"
					+ " sc.金額 "
					+ " FROM T_売上_子 sc"
					+ " left outer join T_売上_親 sp on sc.売上親ID=sp.売上親ID"
					+ " left outer join M_法人 c on sp.得意先CD=c.得意先CD"
					+ " left outer join T_製作_親 pp on sc.製作親ID=pp.製作親ID"
					+ " WHERE 売上年月日>=? AND 売上年月日<? and 売上FLG='true' and 製作番号<9000 and 納品区分CD<5"
					+ ") a group by 得意先CD,得意先名,受注番号,日付"
			);
		) {
			ps.setDate(1, from);
			ps.setDate(2, to);
			ResultSet rs = ps.executeQuery();

			String accountCode = "";
			String accountName = "";
			int subtotal = 0;
			int total = 0;
			while (rs.next()) {
				if (!accountCode.equals(rs.getString("得意先CD")) && !accountCode.equals("")) {
					if (m.containsKey(accountCode)) {
						Vector<Object> record = new Vector<Object>();
						record.add(accountCode);
						record.add(accountName);
						record.add("消費税");
						record.add("");
						record.add(m.get(accountCode));
						contents.add(record);
						subtotal += m.get(accountCode);
						total += m.get(accountCode);
					}
					Vector<Object> record = new Vector<Object>();
					record.add(accountCode);
					record.add(accountName);
					record.add("");
					record.add("");
					record.add(subtotal);
					contents.add(record);
					record = new Vector<Object>();
					record.add("");
					record.add("");
					record.add("");
					record.add("");
					record.add(null);
					contents.add(record);
					subtotal = 0;
				}
				Vector<Object> record = new Vector<Object>();
				record.add(rs.getString("得意先CD"));
				record.add(rs.getString("得意先名"));
				record.add(rs.getString("受注番号"));
				record.add(rs.getString("日付"));
				record.add(rs.getInt("金額"));
				contents.add(record);
				accountCode = rs.getString("得意先CD");
				accountName = rs.getString("得意先名");
				subtotal += rs.getInt("金額");
				total += rs.getInt("金額");
			}
			rs.close();
			if (m.containsKey(accountCode)) {
				Vector<Object> record = new Vector<Object>();
				record.add(accountCode);
				record.add(accountName);
				record.add("消費税");
				record.add("");
				record.add(m.get(accountCode));
				contents.add(record);
				subtotal += m.get(accountCode);
				total += m.get(accountCode);
			}
			Vector<Object> record = new Vector<Object>();
			record.add(accountCode);
			record.add(accountName);
			record.add("");
			record.add("");
			record.add(subtotal);
			contents.add(record);
			record = new Vector<Object>();
			record.add("");
			record.add("");
			record.add("");
			record.add("");
			record.add(null);
			contents.add(record);
			record = new Vector<Object>();
			record.add("");
			record.add("");
			record.add("");
			record.add("合計");
			record.add(total);
			contents.add(record);
		}

		return contents;
	}
}
