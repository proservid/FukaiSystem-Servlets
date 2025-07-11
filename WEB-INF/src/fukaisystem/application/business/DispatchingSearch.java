package fukaisystem.application.business;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

import javax.servlet.ServletResponse;

import fukaisystem.dto.DispatchingDTO;
import fukaisystem.foundation.ServiceFoundation;

/**
 * 出庫データを検索する
 */
public class DispatchingSearch extends ServiceFoundation {

	@Override
	public Object access(Connection c, ServletResponse response, Object o) throws IOException, SQLException {

		String[] constStrs = {
			"製作枝番 like ?",
			"SUBSTRING(CONVERT(VARCHAR, 出庫年月日),1,4) like ?",
			"SUBSTRING(CONVERT(VARCHAR, 出庫年月日),6,2) like ?",
			"SUBSTRING(CONVERT(VARCHAR, 出庫年月日),9,2) like ?",
			"用途 like ?",
			"摘要 like ?",
			"注文枝番 like ?",
			"品名 like ?"
		};
		String[] constInts = { "製作期=?", "製作番号=?", "大分類CD=?", "中分類CD=?", "小分類CD=?", "注文期=?", "注文番号=?" };
		DispatchingDTO dto = cast(response, o, DispatchingDTO.class);
		StringBuilder query = new StringBuilder(
			"SELECT top 30000 出庫親ID,製作期,製作番号,製作枝番,出庫年月日,用途,摘要 FROM ("
				+ " SELECT p.出庫親ID,p.製作期,p.製作番号,p.製作枝番,出庫年月日,用途,p.摘要,注文期,注文番号,注文枝番 FROM T_出庫_親 p"
				+ " LEFT OUTER JOIN T_出庫_子 c ON p.出庫親ID=c.出庫親ID"
				+ " LEFT OUTER JOIN ("
				+ "  SELECT 在庫親ID,注文期,注文番号,注文枝番 FROM T_在庫_親 zp"
				+ "  UNION"
				+ "  SELECT 製作親ID,製作期,製作番号,製作枝番 FROM T_製作_親 pp"
				+ " ) z on z.在庫親ID=c.在庫親ID"
		);
		boolean isFirst = true;
		List<Integer> stringConditionIndex = new ArrayList<Integer>();
		// 文字列の検索条件は、searchDTO.getStr(0～7)
		for (int i = 0; i < 8; i++) {
			if (!dto.getStr(i).equals("")) { // 検索条件が入っていれば
				stringConditionIndex.add(i);
				if (isFirst) {
					query.append(" WHERE ");
					isFirst = false;
				} else {
					if (dto.isAndSearch())
						query.append(" AND ");
					else
						query.append(" OR ");
				}
				query.append(constStrs[i]);
			}
		}

		List<Integer> intConditionIndex = new ArrayList<Integer>();
		// 数値の検索条件は、searchDTO.getInt(0～6)
		for (int i = 0; i < 7; i++) {
			if (dto.getInt(i) != 0) { // 検索条件が入っていれば
				intConditionIndex.add(i);
				if (isFirst) {
					query.append(" WHERE " + constInts[i]);
					isFirst = false;
				} else {
					if (dto.isAndSearch())
						query.append(" AND " + constInts[i]);
					else
						query.append(" OR " + constInts[i]);
				}
			}
		}
		query.append(") a GROUP BY 出庫親ID,製作期,製作番号,製作枝番,出庫年月日,用途,摘要");
		Vector<Vector<Object>> data = new Vector<Vector<Object>>();
		try (PreparedStatement ps = c.prepareStatement(query.toString());) {
			int j = 1;
			for (int i : stringConditionIndex) {
				ps.setString(j, dto.getStr(i));
				j++;
			}
			for (int i : intConditionIndex) {
				ps.setInt(j, dto.getInt(i));
				j++;
			}
			try (ResultSet rs = ps.executeQuery();) {
				while (rs.next()) {
					Vector<Object> record = new Vector<Object>();
					record.add(rs.getInt("出庫親ID"));
					if (rs.getInt("製作期") != 0 && rs.getInt("製作番号") != 0) {
						record.add(rs.getInt("製作期") + "-" + rs.getInt("製作番号") + " " + rs.getString("製作枝番"));
					} else {
						record.add("");
					}
					record.add(rs.getDate("出庫年月日"));
					record.add(rs.getString("用途"));
					record.add(rs.getString("摘要"));
					data.add(record);
				}
			}
		}
		return data;
	}
}
