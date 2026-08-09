package fukaisystem.application.mh;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import javax.servlet.ServletResponse;

import fukaisystem.dto.mh.TbMonthDTO;
import fukaisystem.foundation.ServiceFoundation;
import fukaisystem.sql.ResultSetConverter;

/**
 * 工数管理の集計（対象月の台ごとの工数）を取得するためのクラス
 * <p>
 * 列見出しはクライアントが固定で持っているため、列の順序を変更してはならない。
 * 順序は 台, 工数 とする。
 *
 * @author kameura
 *
 */
public class TbGetSummary extends ServiceFoundation {

	@Override
	public Object access(Connection c, ServletResponse response, Object o) throws IOException, SQLException {

		TbMonthDTO condition = cast(response, o, TbMonthDTO.class);
		if (condition == null) {
			return null;
		}

		try (
			PreparedStatement ps = c.prepareStatement(
				"SELECT CASE WHEN 台 IS NULL THEN '計' ELSE 台+'0' END AS 台,"
					+ " CONVERT(float, SUM(時間))/100 AS 工数"
					+ " FROM (SELECT 製作期,LEFT(製作番号, 1) AS 台,時間 FROM T_加工実績"
					+ " WHERE 着手日時>=? AND 着手日時<?) w"
					+ " GROUP BY ROLLUP(台)"
			);
		) {
			ps.setDate(1, condition.getFrom());
			ps.setDate(2, condition.getTo());

			try (ResultSet rs = ps.executeQuery();) {
				return ResultSetConverter.toTable(rs);
			}
		}
	}
}
