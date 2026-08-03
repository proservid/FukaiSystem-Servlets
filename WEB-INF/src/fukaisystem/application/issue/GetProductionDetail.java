package fukaisystem.application.issue;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Vector;

import javax.servlet.ServletResponse;

import fukaisystem.foundation.ServiceFoundation;

public class GetProductionDetail extends ServiceFoundation {

	@Override
	public Object access(Connection c, ServletResponse response, Object o) throws IOException, SQLException {

		int productionID = cast(response, o, Integer.class);

		Vector<Vector<Object>> productionData = new Vector<>();

		// 製作明細
		try (PreparedStatement ps = c.prepareStatement("SELECT ID,表示CD,名称,数量 FROM T_製作_子 WHERE 製作親ID=?");) {
			ps.setInt(1, productionID);
			try (ResultSet rs = ps.executeQuery();) {
				while (rs.next()) {
					Vector<Object> record = new Vector<Object>();
					record.add(rs.getInt("ID")); // 使用しないがテーブル共通化のため
					record.add(rs.getInt("表示CD")); // 使用しないがテーブル共通化のため
					record.add(rs.getString("名称"));
					// record.add(rs.getBoolean("各FLG"));
					record.add(rs.getInt("数量"));
					// record.add(rs.getInt("数量単位CD"));
					// record.add(rs.getInt("単価"));
					// record.add(rs.getInt("金額"));
					// record.add(rs.getString("図番"));
					// record.add(rs.getString("備考"));
					// record.add(rs.getDate("完成年月日"));
					// record.add(rs.getDate("完成年月日") != null);
					// record.add(rs.getInt("表示CD") == 2 ? rs.getDate("納品年月日") : null);
					// record.add(rs.getDate("納品年月日") != null);
					productionData.add(record);
				}
			}
		}
		return productionData;
	}
}
