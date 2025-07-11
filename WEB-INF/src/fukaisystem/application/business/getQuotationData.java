package fukaisystem.application.business;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Vector;

import javax.servlet.ServletResponse;

import fukaisystem.foundation.ServiceFoundation;

/**
 * 製作伝票にコピーするための見積書データを取得する
 */
public class getQuotationData extends ServiceFoundation {

	@Override
	@SuppressWarnings("unchecked")
	public Object access(Connection c, ServletResponse response, Object o) throws IOException, SQLException {
		List<String> nums = cast(response, o, List.class);
		Vector<Vector<Object>> data = new Vector<Vector<Object>>();

		StringBuilder query = new StringBuilder(
			"select * from T_見積_子 where 見積親ID IN (select 見積親ID from ("
				+ "select 見積親ID,convert(varchar,見積期)+'-'+right('000' + convert(varchar, 見積番号), 3)+見積枝番 as 見積番 from T_見積_親) a"
				+ " where "
		);
		boolean isFirst = true;
		for (String s : nums) {
			if (isFirst) {
				query.append("見積番 like '" + s + "'");
				isFirst = false;
			} else {
				query.append(" OR 見積番 like '" + s + "'");
			}
		}
		query.append(") order by 見積親ID,ID");
		try (PreparedStatement ps = c.prepareStatement(query.toString());) {
			try (ResultSet rs = ps.executeQuery();) {
				while (rs.next()) {
					Vector<Object> record = new Vector<Object>();
					record.add(rs.getInt("ID"));
					record.add(rs.getInt("表示CD"));
					record.add(rs.getString("名称"));
					record.add(rs.getBoolean("各FLG"));
					record.add(rs.getInt("数量"));
					record.add(rs.getInt("数量単位CD"));
					record.add(rs.getInt("単価"));
					record.add(rs.getInt("提示額"));
					record.add(rs.getString("図番"));
					record.add(rs.getString("備考"));
					data.add(record);
				}
			}
		}
		return data;
	}
}
