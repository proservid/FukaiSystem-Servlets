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
 * 工数管理のマスター表（対象月の製番・加工ごとの工数）を取得するためのクラス
 * <p>
 * 列見出しはクライアントが固定で持っているため、列の順序を変更してはならない。
 * 順序は 製番, 加工CD, 工程名, 工数 とする。
 *
 * @author kameura
 *
 */
public class TbGetMonthly extends ServiceFoundation {

	@Override
	public Object access(Connection c, ServletResponse response, Object o) throws IOException, SQLException {

		TbMonthDTO condition = cast(response, o, TbMonthDTO.class);
		if (condition == null) {
			return null;
		}

		try (
			PreparedStatement ps = c.prepareStatement(
				"SELECT RIGHT('00' + CAST(w.製作期 AS varchar), 2)+'-'"
					+ "+RIGHT('0000' + CAST(w.製作番号 AS varchar), 4)+w.製作枝番 AS 製番,"
					+ " w.加工CD,"
					+ " k.小分類名 AS 工程名,"
					+ " w.工数"
					+ " FROM (SELECT 加工CD,製作期,製作番号,製作枝番,CONVERT(float, SUM(時間))/100 AS 工数"
					+ " FROM T_加工実績"
					+ " WHERE 着手日時>=? AND 着手日時<?"
					+ " GROUP BY 製作期,製作番号,製作枝番,加工CD) w"
					+ " LEFT OUTER JOIN M_加工_子 k ON w.加工CD=k.CD"
					+ " ORDER BY 製番,加工CD"
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
