package fukaisystem.application.business;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import javax.servlet.ServletResponse;

import fukaisystem.dto.ProductNumber;
import fukaisystem.foundation.ServiceFoundation;

/**
 * 新規登録で既存の番号が指定されていないかチェックする
 * 見積もりにヒット=1, 製作にヒット=2, 見積と政策にヒット=3, ヒットなし=0, エラー=-1
 */
public class NumCheck extends ServiceFoundation {

	@Override
	public Object access(Connection c, ServletResponse response, Object o) throws IOException, SQLException {

		ProductNumber input = cast(response, o, ProductNumber.class);
		int result = -1;
		try (PreparedStatement ps = c.prepareStatement("SELECT 1 FROM T_見積_親 WHERE 見積期=? AND 見積番号=? AND 見積枝番=?");) {
			ps.setInt(1, input.getPeriod());
			ps.setInt(2, input.getNumber());
			ps.setString(3, input.getBranch());
			try (ResultSet rs = ps.executeQuery();) {
				if (rs.next()) {
					result += 1;
				}
			}
		}
		try (PreparedStatement ps = c.prepareStatement("SELECT 1 FROM T_製作_親 WHERE 製作期=? AND 製作番号=? AND 製作枝番=?");) {
			ps.setInt(1, input.getPeriod());
			ps.setInt(2, input.getNumber());
			ps.setString(3, input.getBranch());
			try (ResultSet rs = ps.executeQuery();) {
				if (rs.next()) {
					result += 2;
				}
			}
			result++;
		}
		return result;
	}
}
