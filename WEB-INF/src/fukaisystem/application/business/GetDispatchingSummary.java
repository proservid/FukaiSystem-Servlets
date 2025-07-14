package fukaisystem.application.business;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Vector;

import javax.servlet.ServletResponse;

import fukaisystem.dto.DispatchingDTO;
import fukaisystem.foundation.ServiceFoundation;

/**
 * 指定した出庫データの詳細を取得する
 */
public class GetDispatchingSummary extends ServiceFoundation {

	@Override
	public Object access(Connection c, ServletResponse response, Object o) throws IOException, SQLException {
		DispatchingDTO dto = null;

		Vector<Vector<Object>> data = new Vector<Vector<Object>>();

		int id = cast(response, o, Integer.class);

		try (
			PreparedStatement ps = c.prepareStatement(
				"SELECT 大分類CD,中分類CD,小分類CD,品名,各FLG,数量,数量単位CD,重量長さ,"
					+ "単価,金額,備考,c.在庫親ID,在庫子ID,注文期,注文番号,注文枝番 FROM T_出庫_子 c"
					+ " LEFT OUTER JOIN ("
					+ "  SELECT 在庫親ID,注文期,注文番号,注文枝番 FROM T_在庫_親"
					+ "  UNION"
					+ "  SELECT 製作親ID,製作期,製作番号,製作枝番 FROM T_製作_親"
					+ " ) p ON c.在庫親ID=p.在庫親ID"
					+ " WHERE 出庫親ID=?"
			);
		) {
			ps.setInt(1, id);
			try (ResultSet rs = ps.executeQuery();) {
				while (rs.next()) {
					Vector<Object> record = new Vector<Object>();
					record.add(rs.getInt("大分類CD"));
					record.add(rs.getInt("中分類CD"));
					record.add(rs.getInt("小分類CD"));
					record.add(rs.getString("品名"));
					record.add(rs.getBoolean("各FLG"));
					record.add(rs.getDouble("数量"));
					record.add(rs.getInt("数量単位CD"));
					record.add(rs.getDouble("重量長さ"));
					record.add(rs.getInt("単価"));
					record.add(rs.getInt("金額"));
					record.add(rs.getString("備考"));
					record.add(rs.getInt("在庫親ID"));
					record.add(rs.getInt("在庫子ID"));
					record.add(
						rs.getString("注文期") == null
							? ""
							: rs.getString("注文期") + "-" + rs.getString("注文番号") + " " + rs.getString("注文枝番")
					);
					data.add(record);
				}
			}
		}

		try (PreparedStatement ps = c.prepareStatement("SELECT * FROM T_出庫_親 p WHERE 出庫親ID=?");) {
			ps.setInt(1, id);
			try (ResultSet rs = ps.executeQuery();) {
				while (rs.next()) {
					dto = new DispatchingDTO(
						rs.getInt("出庫親ID"),
						0,
						0,
						"",
						rs.getInt("製作期"),
						rs.getInt("製作番号"),
						rs.getString("製作枝番"),
						"",
						"",
						"",
						"",
						rs.getString("用途"),
						rs.getString("摘要"),
						data,
						rs.getDate("出庫年月日"),
						0,
						0,
						0,
						false
					);
				}
			}
		}
		return dto;
	}
}
