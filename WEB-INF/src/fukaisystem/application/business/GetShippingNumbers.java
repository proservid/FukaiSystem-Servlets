package fukaisystem.application.business;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Vector;

import javax.servlet.ServletResponse;

import fukaisystem.foundation.ServiceFoundation;

/**
 * 製作伝票に対応する出荷案内書番号のリストを取得する
 */
public class GetShippingNumbers extends ServiceFoundation {

	@Override
	public Object access(Connection c, ServletResponse response, Object o) throws IOException, SQLException {

		Integer productionID = cast(response, o, Integer.class); // 製作伝票ID
		Vector<String> shippingNumbers = new Vector<>(); // "xx-yyyy" 形式

		try (
			PreparedStatement ps = c.prepareStatement(
				"SELECT 出荷伝票番号1,出荷伝票番号2 FROM T_出荷_親 WHERE 製作親ID=?"
			);
		) {
			ps.setInt(1, productionID);
			try (ResultSet rs = ps.executeQuery();) {
				while (rs.next()) {
					shippingNumbers.add(rs.getString("出荷伝票番号1") + "-" + String.format("%03d", rs.getInt("出荷伝票番号2")));
				}
			}
		}
		return shippingNumbers;
	}
}
