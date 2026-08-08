package fukaisystem.application.address;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.Vector;

import javax.servlet.ServletResponse;

import fukaisystem.foundation.ServiceFoundation;

/**
 * 年賀状の宛名一覧を取得するためのクラス
 *
 * 法人（M_法人）と個人（M_個人）を UNION ALL で連結し、1 行 17 列の宛名データを返す。
 * 列の並びはクライアント側で固定している列見出しと一致させること。
 *
 * @author kameura
 *
 */
public class GetNengaList extends ServiceFoundation {

	@Override
	public Object access(Connection c, ServletResponse response, Object o) throws IOException, SQLException {

		Integer nengaCD = cast(response, o, Integer.class);
		if (nengaCD == null) {
			return null;
		}

		String sql = "SELECT " +
			"'' AS 表印刷済," +
			"'' AS 裏印刷済,'勤務先' AS 宛先,'' AS 氏名,'' AS ﾌﾘｶﾞﾅ,'御中' AS 敬称,'' AS 関係," +
			"'' AS 自宅郵便番号," +
			"'' AS 自宅住所1," +
			"'' AS 自宅住所2," +
			"CASE" +
			" WHEN 種別CD = 1 THEN '㈱'+会社名" +
			" WHEN 種別CD = 2 THEN 会社名+'㈱'" +
			" WHEN 種別CD = 3 THEN '㈲'+会社名" +
			" WHEN 種別CD = 4 THEN 会社名+'㈲'" +
			" ELSE 会社名" +
			" END AS 会社名," +
			" 支店名 AS 部署名1,'' AS 部署名2,'' AS 役職名," +
			" co.郵便番号 AS 会社郵便番号," +
			" case when pc.郵便番号 is null then" +
			"  case when 町域 is null then 都道府県+市区町村+番地 else 都道府県+市区町村+町域+番地 end" +
			" else " +
			"  case when 町域 is null then 都道府県+市区町村+番地 else 都道府県+市区町村+町域+番地 end" +
			" end AS 会社住所1," +
			" 建物等 AS 会社住所2" +
			" from M_法人 co" +
			" left outer join V_郵便番号 pc on replace(co.郵便番号,'-','')=pc.郵便番号 and co.郵便枝番=pc.郵便枝番" +
			" left outer join M_都道府県 p on pc.都道府県CD=p.CD" +
			" left outer join M_市区町村 c on pc.都道府県CD=c.都道府県CD and pc.市区町村CD=c.CD" +
			" where co.有効FLG='true' AND 年賀状CD=?" +
			" union all" +
			" select " +
			"CASE" +
			" WHEN 喪FLG='true' THEN '×'" +
			" ELSE '' END AS 表印刷済," +
			" '' AS 裏印刷済," +
			" case when 住所FLG='true' and 自宅FLG='true' then '自宅' else '勤務先' end AS 宛先," +
			" 氏名,シメイ AS ﾌﾘｶﾞﾅ,kei.敬称,'' AS 関係," +
			" case when 住所FLG='true' and 自宅FLG='true' then " +
			"	case when inadd.郵便番号 is null then '' else inadd.郵便番号 end" +
			" else ''" +
			" end as 自宅郵便番号," +
			" case when 住所FLG='true' and 自宅FLG='true' then" +
			"	case when 町域 is null then 都道府県+市区町村+inadd.番地 else 都道府県+市区町村+町域+inadd.番地 end" +
			" else ''" +
			" end AS 自宅住所1," +
			" case when 住所FLG='true' and 自宅FLG='true' then" +
			"	inadd.建物等" +
			" else ''" +
			" end as 自宅住所2," +
			" CASE" +
			" WHEN 会社名 IS NULL THEN '' ELSE" +
			"  CASE" +
			"  WHEN 種別CD = 1 THEN '㈱'+会社名 + CASE WHEN 支店名 IS NULL THEN '' ELSE ' ' + 支店名 END" +
			"  WHEN 種別CD = 2 THEN 会社名+'㈱' + CASE WHEN 支店名 IS NULL THEN '' ELSE ' ' + 支店名 END" +
			"  WHEN 種別CD = 3 THEN '㈲'+会社名 + CASE WHEN 支店名 IS NULL THEN '' ELSE ' ' + 支店名 END" +
			"  WHEN 種別CD = 4 THEN 会社名+'㈲' + CASE WHEN 支店名 IS NULL THEN '' ELSE ' ' + 支店名 END" +
			"  ELSE 会社名 + CASE WHEN 支店名 IS NULL THEN '' ELSE ' ' + 支店名 END" +
			"  END" +
			" END AS 会社名," +
			" 部署名 AS 部署名1,'' AS 部署名2,役職名," +
			" case when 住所FLG='true' then" +
			"	case when 自宅FLG='true' then ''" +
			"	else inadd.郵便番号" +
			"	end" +
			" else co.郵便番号" +
			" end as 会社郵便番号," +
			" case when 住所FLG='true' then" +
			"	case when 自宅FLG='true' then ''" +
			"	else" +
			"		case when 町域 is null then 都道府県+市区町村+inadd.番地" +
			"		else 都道府県+市区町村+町域+inadd.番地 end" +
			"	end" +
			" else" +
			"	case when 町域 is null then 都道府県+市区町村+co.番地" +
			"	else 都道府県+市区町村+町域+co.番地 end" +
			" end AS 会社住所1," +
			" case when 住所FLG='true' then" +
			"	case when 自宅FLG='true' then ''" +
			"	else inadd.建物等" +
			"	end" +
			" else co.建物等" +
			" end as 会社住所2" +
			" from M_個人 ind" +
			" left outer join M_敬称 kei on ind.敬称=kei.CD" +
			" left outer join M_個人住所 inadd on ind.CD=inadd.個人CD" +
			" left outer join M_法人 co on ind.法人CD=co.CD" +
			" left outer join V_郵便番号 pc" +
			"  on replace(case when 住所FLG='true' then inadd.郵便番号 else co.郵便番号 end,'-','')=pc.郵便番号" +
			"  and (case when 住所FLG='true' then inadd.郵便枝番 else co.郵便枝番 end)=pc.郵便枝番" +
			" left outer join M_都道府県 p on pc.都道府県CD=p.CD" +
			" left outer join M_市区町村 c on pc.都道府県CD=c.都道府県CD and pc.市区町村CD=c.CD" +
			" where ind.有効FLG='true' AND ind.年賀状CD=?";

		Vector<Vector<String>> list = new Vector<Vector<String>>();
		try (
			PreparedStatement ps = c.prepareStatement(sql);
		) {
			ps.setInt(1, nengaCD.intValue());
			ps.setInt(2, nengaCD.intValue());
			try (ResultSet rs = ps.executeQuery();) {
				ResultSetMetaData rsmd = rs.getMetaData();
				int colCount = rsmd.getColumnCount();
				while (rs.next()) {
					Vector<String> record = new Vector<String>();
					for (int i = 1; i <= colCount; i++) {
						String value = rs.getString(i);
						record.add(value == null ? "" : value.trim());
					}
					list.add(record);
				}
			}
		}
		return list;
	}
}
