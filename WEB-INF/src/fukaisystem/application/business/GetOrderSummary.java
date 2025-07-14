package fukaisystem.application.business;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Vector;

import javax.servlet.ServletResponse;

import fukaisystem.dto.OrderDocumentDTO;
import fukaisystem.foundation.ServiceFoundation;

/**
 * 指定した注文データの詳細を取得する
 */
public class GetOrderSummary extends ServiceFoundation {

	@Override
	public Object access(Connection c, ServletResponse response, Object o) throws IOException, SQLException {
		OrderDocumentDTO dto = null;
		int id = cast(response, o, Integer.class);

		Vector<Vector<Object>> data = new Vector<Vector<Object>>();

		try (
			PreparedStatement ps = c.prepareStatement(
				"SELECT * FROM T_在庫_子 s LEFT OUTER JOIN T_指定納品書 slip "
					+ "ON s.納品書番号=slip.ID WHERE 在庫親ID=? ORDER BY s.ID"
			);
		) {
			ps.setInt(1, id);
			try (ResultSet rs = ps.executeQuery();) {
				while (rs.next()) {
					Vector<Object> record = new Vector<Object>();
					record.add(rs.getInt("表示CD"));
					record.add(rs.getInt("大分類CD"));
					record.add(rs.getInt("中分類CD"));
					record.add(rs.getInt("小分類CD"));
					record.add(rs.getString("材料品名"));
					record.add(rs.getBoolean("各FLG"));
					record.add(rs.getInt("数量"));
					record.add(rs.getInt("数量単位CD"));
					record.add(rs.getDouble("重量長さ"));
					record.add(rs.getInt("単価"));
					record.add(rs.getInt("金額"));
					record.add(rs.getString("備考"));
					record.add(rs.getDate("入庫年月日"));
					record.add(rs.getDate("入庫年月日") != null);
					record.add(rs.getInt("納品書番号"));
					record.add(rs.getDate("納品書日"));
					record.add(rs.getInt("消費税"));
					record.add(rs.getInt("納品書番号") != 0);
					record.add(rs.getBoolean("〆FLG"));
					data.add(record);
				}
			}
		}

		try (
			PreparedStatement ps = c.prepareStatement(
				"SELECT 在庫親ID,注文期,注文番号,注文枝番,"
					+ " 伝票番号,s.仕入先CD,注文年月日,指定納期,"
					+ " 摘要,納入先指定,"
					+ " CASE"
					+ " WHEN 種別CD = 1 THEN '㈱'+会社名 + CASE WHEN 支店名 IS NULL THEN '' ELSE ' ' + 支店名 END"
					+ " WHEN 種別CD = 2 THEN 会社名+'㈱' + CASE WHEN 支店名 IS NULL THEN '' ELSE ' ' + 支店名 END"
					+ " WHEN 種別CD = 3 THEN '㈲'+会社名 + CASE WHEN 支店名 IS NULL THEN '' ELSE ' ' + 支店名 END"
					+ " WHEN 種別CD = 4 THEN 会社名+'㈲' + CASE WHEN 支店名 IS NULL THEN '' ELSE ' ' + 支店名 END"
					+ " ELSE 会社名 END AS 社名"
					+ " FROM T_在庫_親 s"
					+ " LEFT OUTER JOIN M_法人 c"
					+ " ON s.仕入先CD=c.仕入先CD"
					+ " WHERE 在庫親ID=?"
			);
		) {
			ps.setInt(1, id);
			try (ResultSet rs = ps.executeQuery();) {
				while (rs.next()) {
					dto = new OrderDocumentDTO(
						data,
						rs.getString("社名"),
						rs.getString("注文枝番"),
						rs.getString("摘要"),
						rs.getString("納入先指定"),
						rs.getInt("仕入先CD"),
						rs.getInt("注文期"),
						rs.getInt("注文番号"),
						rs.getInt("伝票番号"),
						rs.getInt("在庫親ID"),
						rs.getDate("注文年月日"),
						rs.getDate("指定納期")
					);
				}

			}
		}
		return dto;
	}
}
