package fukaisystem.print.table;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.text.ParseException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.servlet.ServletResponse;

import com.proservid.print.dao.TableDAO;

import fukaisystem.dto.GetTableDTO;
import fukaisystem.foundation.ServiceFoundation;

public class GetData extends ServiceFoundation {
	protected static final String className = "GetData";

	@Override
	public Object access(Connection c, ServletResponse response, Object o)
		throws IOException, SQLException, ParseException {

		GetTableDTO dto = cast(response, o, GetTableDTO.class);
		Map<String, Map<String, List<String>>> data = new HashMap<>();
		Map<String, List<String>> tables = dto.getTables();
		for (Map.Entry<String, List<String>> table : tables.entrySet()) {
			String tableName = table.getKey();
			if (tableName.equals("V_見積原簿")) {
				// 見積原簿は特別なクエリを使用
				StringBuilder query = new StringBuilder(
					"select "
						+ " z.ID,見積子ID,"
						+ " min(大分類CD) as 大分類CD,"
						+ " min(中分類CD) as 中分類CD,"
						+ " min(小分類CD) as 小分類CD,"
						+ " case when z.ID is null then case when 見積子ID is null then '総合計' else min(c.名称) end else min(z.名称) end as 名称,"
						+ " case when z.ID is null then REPLACE(CONVERT(VARCHAR, CAST(sum(case when z.ID=1 then 提示額 else 0 end) AS MONEY), 1), '.00', '') else '' end as 提示額,"
						+ " case when z.ID is null then '' else REPLACE(CONVERT(VARCHAR, CAST(min(z.単価) AS MONEY), 1), '.00', '') end as 単価,"
						+ " case when z.ID is null then '' else CONVERT(VARCHAR,CONVERT(FLOAT,min(z.数量))) end as 数量,"
						+ " REPLACE(CONVERT(VARCHAR, CAST(sum(原価) AS MONEY), 1), '.00', '') as 原価,"
						+ " case when z.ID is null then '' else CONVERT(VARCHAR,min(掛率)) end as 掛率,"
						+ " REPLACE(CONVERT(VARCHAR, CAST(sum(小計) AS MONEY), 1), '.00', '') as 小計,"
						+ " case when z.ID is null then case when 見積子ID is null then '' else min(c.備考) end else min(z.備考) end as 備考"
						+ "   from ("
						+ "	SELECT em.ID,em.見積親ID,見積子ID,大分類CD,中分類CD,小分類CD,em.名称,"
						+ "	重量,em.単価,em.数量,round(em.数量*em.単価,0) as 原価,掛率,round(em.数量*em.単価*掛率,0) as 小計,em.備考,"
						+ "	CASE WHEN 種別CD=1 THEN '㈱'+会社名"
						+ "		WHEN 種別CD=2 THEN 会社名+'㈱'"
						+ "		WHEN 種別CD=3 THEN '㈲'+会社名"
						+ "		WHEN 種別CD=4 THEN 会社名+'㈲'"
						+ "		ELSE 会社名 END AS 仕入先名"
						+ "	FROM T_見積_材料 em"
						+ "	LEFT OUTER JOIN T_見積_子 ec ON em.見積子ID=ec.ID and em.見積親ID=ec.見積親ID"
						+ "	LEFT OUTER JOIN T_見積_親 ep ON ec.見積親ID=ep.見積親ID"
						+ "	LEFT OUTER JOIN T_製作_親 pp ON ec.見積親ID=pp.見積親ID"
						+ "	LEFT OUTER JOIN M_法人 c on em.仕入先CD=c.仕入先CD"
						+ "	union all"
						+ "	select ID,見積親ID,見積子ID,大分類CD,中分類CD,小分類CD,ew.名称,"
						+ "	0,単価,時間,round(単価*時間,0) as 原価,掛率,round(単価*時間*掛率,0) as 小計,ew.備考,'' from T_見積_加工 ew"
						+ ") z "
						+ " left outer join T_見積_子 c on c.見積親ID=z.見積親ID and c.ID=z.見積子ID"
						+ " left outer join T_見積_親 p on c.見積親ID=p.見積親ID"
				);
				data.put(
					tableName,
					TableDAO.getTableData(
						c,
						query,
						dto.getConditions(),
						" group by rollup(見積子ID,z.ID) order by 見積子ID,z.ID",
						true // z.見積親ID=?
					)
				);
			} else {
				// それ以外は通常処理
				StringBuilder query = new StringBuilder("SELECT ")
					.append(String.join(",", table.getValue()))
					.append(" FROM ")
					.append(tableName);
				data.put(tableName, TableDAO.getTableData(c, query, dto.getConditions(), dto.getOrder(), false));
			}
		}
		return data;
	}
}
