package fukaisystem.application.business;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import javax.servlet.ServletResponse;

import fukaisystem.foundation.ServiceFoundation;

/**
 * 入力された出荷案内書番号が登録済みか調べる（更新時は除外）
 */
public class ExistsShippingNumber extends ServiceFoundation {

	@Override
	public Object access(Connection c, ServletResponse response, Object o) throws IOException, SQLException {

		int[] numbers = cast(response, o, int[].class);

		try (
			PreparedStatement ps = c.prepareStatement(
				"SELECT 1 FROM T_出荷_親 WHERE 出荷親ID<>? AND 出荷伝票番号1=? AND 出荷伝票番号2=?"
			);
		) {
			ps.setInt(1, numbers[0]); // 出荷親IDが一致したら更新なので登録済みで良い
			ps.setInt(2, numbers[1]);
			ps.setInt(3, numbers[2]);
			try (ResultSet rs = ps.executeQuery();) {
				if (rs.next()) {
					return true;
				}
			}
		}
		return false;
	}
}
