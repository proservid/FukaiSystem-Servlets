package fukaisystem.application.aggregate;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import javax.servlet.ServletResponse;

import fukaisystem.dto.ProductSheetDTO;
import fukaisystem.foundation.ServiceFoundation;
import fukaisystem.sql.ResultSetConverter;

/**
 * 製作伝票の台帳を取得するためのクラス
 *
 * @author kameura
 *
 */
public class GetProductSheet extends ServiceFoundation {

	@Override
	public Object access(Connection c, ServletResponse response, Object o) throws IOException, SQLException {
		ProductSheetDTO condition = cast(response, o, ProductSheetDTO.class);
		if (condition == null) {
			return null;
		}
		Integer period = condition.getPeriod();

		String sql = "select top 30000 " +
			"pp.製作親ID,ID,表示CD," +
			"発行年月日," +
			"pp.得意先CD," +
			"名称," +
			"case when 数量=0 then ''" +
			"     when 各FLG = 1 then '各' + convert(varchar,数量) + 数量単位 else convert(varchar,数量) + 数量単位 end as 数量," +
			"単価," +
			"金額," +
			"納期," +
			"convert(varchar,製作期)+'-'+convert(varchar,製作番号) + 製作枝番 as 番号," +
			"出荷年月日 as 納入月日," +
			"convert(varchar,見積期)+'-'+convert(varchar,見積番号) + 見積枝番 as 見積原簿," +
			"受注番号 as 記事" +
			" from T_製作_子 sc" +
			"  left outer join T_製作_親 pp on sc.製作親ID=pp.製作親ID" +
			" left outer join M_数量単位 u on sc.数量単位CD=u.CD" +
			" left outer join M_法人 co on pp.得意先CD=co.得意先CD" +
			" left outer join (select min(見積親ID) as 見積親ID,製作親ID from T_見積製作 group by 製作親ID) z on pp.製作親ID=z.製作親ID" +
			" left outer join T_見積_親 ep on z.見積親ID=ep.見積親ID" +
			" where" +
			(period == null ? "" : " 製作期=? and") +
			"  substring(convert(varchar,製作番号),1,1)=?" +
			" order by 製作期,製作番号,製作枝番,ID";

		try (PreparedStatement ps = c.prepareStatement(sql);) {
			int n = 1;
			if (period != null) {
				ps.setInt(n++, period.intValue());
			}
			ps.setString(n++, String.valueOf(condition.getType()));
			try (ResultSet rs = ps.executeQuery();) {
				return ResultSetConverter.toTable(rs);
			}
		}
	}
}
