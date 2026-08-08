package fukaisystem.application.business;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Vector;

import javax.servlet.ServletResponse;

import fukaisystem.dto.business.SalesDTO;
import fukaisystem.foundation.ServiceFoundation;

/**
 * 選択された売上伝票のデータを取得する
 */
public class GetSelectedSales extends ServiceFoundation {

	@Override
	public Object access(Connection c, ServletResponse response, Object o) throws IOException, SQLException {
		SalesDTO dto = null;

		int salesID = cast(response, o, Integer.class);
		int tax = 0, discount = 0;

		Vector<Vector<Object>> deliveryData = new Vector<Vector<Object>>();

		try (
			PreparedStatement ps = c.prepareStatement(
				"SELECT 納品区分CD,納品手段CD,得意先CD,売上年月日,売上FLG,請求FLG,消費税,値引き,摘要 FROM T_売上_親"
					+ " WHERE 売上親ID=?"
			);
		) {
			ps.setInt(1, salesID);
			try (ResultSet rs = ps.executeQuery();) {
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
					dto = new SalesDTO(
						salesID,
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
			}
		}

		// 売上明細
		try (
			PreparedStatement ps = c.prepareStatement(
				"SELECT sc.製作親ID,sc.製作子ID,sc.表示CD,出荷伝票番号,pp.受注年月日,sp.売上年月日,pp.受注番号,sc.品名,sc.各FLG,sc.数量,sc.数量単位CD,sc.単価,sc.金額,sc.備考,pc.完成年月日 FROM T_売上_子 sc"
					+ " LEFT OUTER JOIN T_製作_親 pp ON sc.製作親ID=pp.製作親ID"
					+ " LEFT OUTER JOIN T_製作_子 pc ON sc.製作親ID=pc.製作親ID AND sc.製作子ID=pc.ID"
					+ " LEFT OUTER JOIN T_売上_親 sp ON sc.売上親ID=sp.売上親ID"
					+ " WHERE sc.売上親ID=? ORDER BY sc.ID"
			);
		) {
			ps.setInt(1, salesID);
			try (ResultSet rs = ps.executeQuery();) {
				while (rs.next()) {
					Vector<Object> record = new Vector<Object>();
					int price = rs.getInt("金額");
					if (rs.getInt("表示CD") == 5) {
						price = tax;
					} else if (rs.getInt("表示CD") == 6) {
						price = discount;
					}
					record.add(rs.getInt("製作親ID"));
					record.add(rs.getInt("製作子ID"));
					record.add(rs.getInt("表示CD"));
					record.add(rs.getString("出荷伝票番号"));
					record.add(rs.getDate("受注年月日"));
					record.add(rs.getString("受注番号"));
					record.add(rs.getString("品名"));
					record.add(rs.getBoolean("各FLG"));
					record.add(rs.getInt("数量"));
					record.add(rs.getInt("数量単位CD"));
					record.add(rs.getInt("単価"));
					record.add(price);
					record.add(rs.getString("備考"));
					record.add(rs.getDate("完成年月日"));
					record.add(rs.getDate("売上年月日"));
					deliveryData.add(record);
				}
				if (dto != null)
					dto.setVector(deliveryData);
			}
		}
		return dto;
	}

}
