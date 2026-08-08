package fukaisystem.application.aggregate;

import java.io.IOException;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.DecimalFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;

import javax.servlet.ServletResponse;

import fukaisystem.foundation.ServiceFoundation;

/**
 * 得意先元帳を取得するためのクラス
 *
 * @author kameura
 *
 */
public class GetLedger extends ServiceFoundation {

	@Override
	public Object access(Connection c, ServletResponse response, Object o) throws IOException, SQLException {

		Map<String, Amount> m = new HashMap<String, Amount>();
		DecimalFormat df = new DecimalFormat("#,###");

		Date from = cast(response, o, Date.class);
		if (from == null) {
			return null;
		}
		Calendar target = Calendar.getInstance();
		target.setTime(from);
		int month = target.get(Calendar.MONTH) + 1;
		target.add(Calendar.MONTH, 1);
		Date to = new Date(target.getTimeInMillis());
		target.add(Calendar.DATE, -1);
		Date last = new Date(target.getTimeInMillis());

		Vector<Vector<Object>> contents = new Vector<Vector<Object>>();
		try (
			PreparedStatement ps = c.prepareStatement(
				"SELECT"
					+ " 得意先CD,"
					+ " 合計額,"
					+ " ROUND(CAST(税合計 AS DECIMAL(18,9)),2) AS 税額"
					+ " FROM ("
					+ "SELECT"
					+ " 得意先CD,"
					+ " SUM(金額) AS 合計額,"
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
				m.put(rs.getString("得意先CD"), new Amount(rs.getInt("合計額"), rs.getInt("税額")));
			}
		}

		try (
			PreparedStatement ps = c.prepareStatement(
				"select sp.得意先CD,"
					// "sc.売上親ID,ID,"
					+ "CASE WHEN 種別CD = 1 THEN '㈱' + 会社名 + CASE WHEN 支店名 IS NULL THEN '' ELSE ' ' + 支店名 END WHEN 種別CD = 2 THEN 会社名 + '㈱' + CASE WHEN 支店名 IS NULL"
					+ "                     THEN '' ELSE ' ' + 支店名 END WHEN 種別CD = 3 THEN '㈲' + 会社名 + CASE WHEN 支店名 IS NULL "
					+ "                     THEN '' ELSE ' ' + 支店名 END WHEN 種別CD = 4 THEN 会社名 + '㈲' + CASE WHEN 支店名 IS NULL THEN '' ELSE ' ' + 支店名 END ELSE 会社名 + CASE WHEN 支店名 IS NULL"
					+ "                     THEN '' ELSE ' ' + 支店名 END END AS 得意先名,"
					+ "売上年月日 as 納入月日,"
					+ "品名,"
					+ "case when 数量=0 then ''"
					+ "     when 各FLG = 1 then '各' + convert(varchar,数量) + 数量単位 else convert(varchar,数量) + 数量単位 end as 数量,"
					+ "金額,"
					+ "convert(varchar,製作期)+'-'+convert(varchar,製作番号) + 製作枝番 as 受注番号,"
					+ "受注番号 as 注文書番号"
					+ " from T_売上_子 sc"
					+ " left outer join T_売上_親 sp on sp.売上親ID=sc.売上親ID"
					+ " left outer join M_数量単位 u on sc.数量単位CD=u.CD"
					+ " left outer join M_法人 co on sp.得意先CD=co.得意先CD"
					+ " left outer join T_製作_親 pp on sc.製作親ID=pp.製作親ID"
					+ " where 売上年月日>=? and 売上年月日<? and 売上FLG='true' and 製作番号<9000 and 納品区分CD<5"
					+ " order by 得意先CD,売上年月日,受注番号,ID"
			);
		) {
			ps.setDate(1, from);
			ps.setDate(2, to);
			ResultSet rs = ps.executeQuery();

			String acNum = "";
			String acName = "";
			String accept = "";
			int subtotal = 0;
			int total = 0;
			int inclusive = 0;
			while (rs.next()) {
				// 特殊データの追加
				if (rs.getString("受注番号") != null) {
					if (!accept.equals(rs.getString("受注番号")) && !accept.equals("")) { // 次の受注番号へ変わるタイミングで小計を追加
						Vector<Object> record = new Vector<Object>();
						record.add(acName);
						record.add("");
						record.add("小計");
						record.add(""); // 数量
						record.add(subtotal); // 金額
						record.add(""); // 受注番号
						record.add(""); // 注文書番号
						contents.add(record);
						subtotal = 0;
						if (!acNum.equals(rs.getString("得意先CD")) && !acNum.equals("")) { // さらに次の得意先CDへ変わるタイミングで（消費税別途の得意先の消費税と）合計を追加
							if (m.containsKey(acNum)) { // 消費税を別途計算していた得意先については、追加
								record = new Vector<Object>();
								record.add(acName);
								record.add(last);
								record.add(month + "月度納入額(\\" + df.format(m.get(acNum).getPrice()) + ")");
								record.add(""); // 数量
								record.add(""); // 金額
								record.add(""); // 受注番号
								record.add(""); // 注文書番号
								contents.add(record);

								record = new Vector<Object>();
								record.add(acName);
								record.add(last);
								record.add("* 消費税");
								record.add(""); // 数量
								record.add(m.get(acNum).getTax()); // 金額
								record.add(""); // 受注番号
								record.add(""); // 注文書番号
								contents.add(record);

								record = new Vector<Object>();
								record.add(acName);
								record.add("");
								record.add("小計");
								record.add(""); // 数量
								record.add(m.get(acNum).getTax()); // 金額
								record.add(""); // 受注番号
								record.add(""); // 注文書番号
								contents.add(record);
								total += m.get(acNum).getTax();
								inclusive += m.get(acNum).getTax();
							}
							record = new Vector<Object>();
							record.add(acName);
							record.add("");
							record.add("合計");
							record.add(""); // 数量
							record.add(total); // 金額
							record.add(""); // 受注番号
							record.add(""); // 注文書番号
							contents.add(record);
							total = 0;
						}
					}
					// 通常データの追加
					Vector<Object> record = new Vector<Object>();
					record.add(rs.getString("得意先名"));
					record.add(rs.getString("納入月日"));
					record.add(rs.getString("品名"));
					record.add(rs.getString("数量"));
					record.add(rs.getInt("金額"));
					record.add(rs.getString("受注番号"));
					record.add(rs.getString("注文書番号"));
					contents.add(record);
					acNum = rs.getString("得意先CD");
					acName = rs.getString("得意先名");
					accept = rs.getString("受注番号");
					subtotal += rs.getInt("金額");
					total += rs.getInt("金額");
					inclusive += rs.getInt("金額");
				}
			}
			rs.close();

			// 最終データ分の小計合計そして総合計
			Vector<Object> record = new Vector<Object>();
			record.add(acName);
			record.add("");
			record.add("小計");
			record.add(""); // 数量
			record.add(subtotal); // 金額
			record.add(""); // 受注番号
			record.add(""); // 注文書番号
			contents.add(record);
			subtotal = 0;

			if (m.containsKey(acNum)) { // 消費税を別途計算していた得意先については、追加
				record = new Vector<Object>();
				record.add(acName);
				record.add(last);
				record.add(month + "月度納入額(\\" + df.format(m.get(acNum).getPrice()) + ")");
				record.add(""); // 数量
				record.add(""); // 金額
				record.add(""); // 受注番号
				record.add(""); // 注文書番号
				contents.add(record);

				record = new Vector<Object>();
				record.add(acName);
				record.add(last);
				record.add("* 消費税");
				record.add(""); // 数量
				record.add(m.get(acNum).getTax()); // 金額
				record.add(""); // 受注番号
				record.add(""); // 注文書番号
				contents.add(record);

				record = new Vector<Object>();
				record.add(acName);
				record.add("");
				record.add("小計");
				record.add(""); // 数量
				record.add(m.get(acNum).getTax()); // 金額
				record.add(""); // 受注番号
				record.add(""); // 注文書番号
				contents.add(record);
				total += m.get(acNum).getTax();
				inclusive += m.get(acNum).getTax();
			}
			record = new Vector<Object>();
			record.add(acName);
			record.add("");
			record.add("合計");
			record.add(""); // 数量
			record.add(total); // 金額
			record.add(""); // 受注番号
			record.add(""); // 注文書番号
			contents.add(record);
			record = new Vector<Object>();
			record.add(acName);
			record.add("");
			record.add("総合計");
			record.add(""); // 数量
			record.add(inclusive); // 金額
			record.add(""); // 受注番号
			record.add(""); // 注文書番号
			contents.add(record);
		}

		return contents;
	}

	private class Amount {
		int price;
		int tax;

		private Amount(int price, int tax) {
			this.price = price;
			this.tax = tax;
		}

		private int getPrice() {
			return price;
		}

		private int getTax() {
			return tax;
		}
	}
}
