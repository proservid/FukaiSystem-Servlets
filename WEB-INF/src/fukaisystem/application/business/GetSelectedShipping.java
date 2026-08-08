package fukaisystem.application.business;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Vector;

import javax.servlet.ServletResponse;

import fukaisystem.dto.business.ShippingDTO;
import fukaisystem.foundation.ServiceFoundation;

/**
 * 製作伝票に対応する出荷案内書番号のリストを取得する
 */
public class GetSelectedShipping extends ServiceFoundation {

	@Override
	public Object access(Connection c, ServletResponse response, Object o) throws IOException, SQLException {

		String shippingNumber = cast(response, o, String.class); // 出荷伝票番号
		String[] numParts = shippingNumber.split("-");
		int shippingID = 0;
		ShippingDTO dto = null;

		try (
			PreparedStatement ps = c.prepareStatement(
				"SELECT 出荷親ID, 得意先CD, 納入先名, 出荷伝票番号1, 出荷伝票番号2, 発行年月日, 出荷月, 出荷日, 納品手段CD, 製作親ID FROM T_出荷_親 WHERE 出荷伝票番号1=? AND 出荷伝票番号2=?"
			);
		) {
			ps.setString(1, numParts[0]);
			ps.setString(2, numParts[1]);
			try (ResultSet rs = ps.executeQuery();) {
				if (rs.next()) {
					shippingID = rs.getInt("出荷親iD");
					dto = new ShippingDTO(
						shippingID,
						rs.getInt("得意先CD"),
						rs.getString("納入先名"),
						rs.getInt("出荷伝票番号1"),
						rs.getInt("出荷伝票番号2"),
						rs.getDate("発行年月日"),
						rs.getInt("出荷月"),
						rs.getInt("出荷日"),
						rs.getInt("納品手段CD"),
						rs.getInt("製作親ID"),
						null
					);
				}
			}
		}
		try (
			PreparedStatement ps = c.prepareStatement(
				"SELECT 表示CD, 注文年月日, 注番, 品名, 各FLG, 数量, 数量単位CD, 分納FLG, 備考, 製作子ID FROM T_出荷_子 WHERE 出荷親ID=?"
			);
		) {
			ps.setInt(1, shippingID);
			try (ResultSet rs = ps.executeQuery();) {
				Vector<Vector<Object>> dataVector = new Vector<>();
				while (rs.next()) {
					Vector<Object> v = new Vector<>();
					v.add(rs.getInt("表示CD"));
					v.add(rs.getDate("注文年月日"));
					v.add(rs.getString("注番"));
					v.add(rs.getString("品名"));
					v.add(rs.getBoolean("各FLG"));
					v.add(rs.getInt("数量"));
					v.add(rs.getInt("数量単位CD"));
					v.add(rs.getBoolean("分納FLG"));
					v.add(rs.getString("備考"));
					v.add(rs.getInt("製作子ID"));
					dataVector.add(v);
				}
				dto.setVector(dataVector);
			}
		}
		return dto;
	}
}
