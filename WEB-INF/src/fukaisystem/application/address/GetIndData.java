package fukaisystem.application.address;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import javax.servlet.ServletResponse;

import fukaisystem.dto.address.CorpDTO;
import fukaisystem.dto.address.IndDTO;
import fukaisystem.foundation.ServiceFoundation;

public class GetIndData extends ServiceFoundation {

	@Override
	public Object access(Connection c, ServletResponse response, Object o) throws IOException, SQLException {

		String input = cast(response, o, String.class);

		try (
			PreparedStatement ps = c.prepareStatement(
				"select "
					+ "ind.法人CD as i法人CD,ind.部署名 as i部署名,ind.役職名 as i役職名,ind.氏名 as i氏名,ind.シメイ as iシメイ,ind.敬称 as i敬称,"
					+ "ind.TEL1 as iTEL1,ind.TEL2 as iTEL2,ind.TEL3 as iTEL3,ind.FAX1 as iFAX1,ind.FAX2 as iFAX2,ind.FAX3 as iFAX3,ind.メール as iメール,ind.備考 as i備考,"
					+ "ind.住所FLG as i住所FLG,ind.有効FLG as i有効FLG,ind.贈答FLG as i贈答FLG,ind.喪FLG as i喪FLG,ind.年賀状CD as i年賀状CD,"
					+ "indad.alpha_2 as ialpha_2,indad.郵便番号 as i郵便番号,indad.郵便枝番 as i郵便枝番,indp.都道府県 as i都道府県,indc.市区町村 as i市区町村,indpc.町域 as i町域,indad.番地 as i番地,indad.建物等 as i建物等,indad.自宅FLG as i自宅FLG,"
					+ "co.仕入先CD as c仕入先CD,co.得意先CD as c得意先CD,co.種別CD as c種別CD,co.会社名 as c会社名,co.カイシャメイ as cカイシャメイ,co.支店名 as c支店名,co.シテンメイ as cシテンメイ,co.表示名 as c表示名,co.アルファベット as cアルファベット,"
					+ "co.alpha_2 as calpha_2,co.郵便番号 as c郵便番号,co.郵便枝番 as c郵便枝番,cop.都道府県 as c都道府県,coc.市区町村 as c市区町村,copc.町域 as c町域,co.番地 as c番地,co.建物等 as c建物等,"
					+ "co.TEL1 as cTEL1,co.TEL2 as cTEL2,co.TEL3 as cTEL3,co.FAX1 as cFAX1,co.FAX2 as cFAX2,co.FAX3 as cFAX3,co.メール as cメール,co.URL as cURL,co.備考 as c備考,co.有効FLG as c有効FLG,co.贈答FLG as c贈答FLG,co.年賀状CD as c年賀状CD,"
					+ "CASE"
					+ " WHEN 種別CD = 1 THEN '㈱'+会社名 + CASE WHEN 支店名 IS NULL THEN '' ELSE ' ' + 支店名 END"
					+ " WHEN 種別CD = 2 THEN 会社名+'㈱' + CASE WHEN 支店名 IS NULL THEN '' ELSE ' ' + 支店名 END"
					+ " WHEN 種別CD = 3 THEN '㈲'+会社名 + CASE WHEN 支店名 IS NULL THEN '' ELSE ' ' + 支店名 END"
					+ " WHEN 種別CD = 4 THEN 会社名+'㈲' + CASE WHEN 支店名 IS NULL THEN '' ELSE ' ' + 支店名 END"
					+ " ELSE 会社名 + CASE WHEN 支店名 IS NULL THEN '' ELSE ' ' + 支店名 END"
					+ " END AS 社名"
					+ " from M_個人 ind"
					+ " left outer join M_個人住所 indad on ind.CD=indad.個人CD"
					+ " left outer join V_郵便番号 indpc on replace(indad.郵便番号,'-','')=indpc.郵便番号 and indad.郵便枝番=indpc.郵便枝番"
					+ " left outer join M_都道府県 indp on indpc.都道府県CD=indp.CD"
					+ " left outer join M_市区町村 indc on indpc.都道府県CD=indc.都道府県CD and indpc.市区町村CD=indc.CD"
					+ " left outer join M_法人 co on ind.法人CD=co.CD"
					+ " left outer join V_郵便番号 copc on replace(co.郵便番号,'-','')=copc.郵便番号 and co.郵便枝番=copc.郵便枝番"
					+ " left outer join M_都道府県 cop on copc.都道府県CD=cop.CD"
					+ " left outer join M_市区町村 coc on copc.都道府県CD=coc.都道府県CD and copc.市区町村CD=coc.CD"
					+ " where ind.CD=?"
			);
		) {
			ps.setString(1, input);
			ResultSet rs = ps.executeQuery();
			if (rs.next()) {
				CorpDTO corpDTO = null;
				if (rs.getInt("i法人CD") > 0) {
					corpDTO = new CorpDTO(
						rs.getString("c会社名"), rs.getString("cカイシャメイ"), rs.getString("c支店名"), rs.getString("cシテンメイ"),
						rs.getString("c表示名"),
						rs.getString("calpha_2"), rs.getString("c郵便番号"), rs.getString("c郵便枝番"),
						rs.getString("c都道府県"), rs.getString("c市区町村"),
						rs.getString("c町域"), rs.getString("c番地"), rs.getString("c建物等"), rs.getString("cTEL1"),
						rs.getString("cTEL2"), rs.getString("cTEL3"),
						rs.getString("cFAX1"), rs.getString("cFAX2"), rs.getString("cFAX3"), rs.getString("cメール"),
						rs.getString("cURL"), rs.getString("c備考"),
						rs.getString("i法人CD"), rs.getString("cアルファベット"), "",
						rs.getInt("c仕入先CD"), rs.getInt("c得意先CD"), rs.getInt("c種別CD"), rs.getInt("c年賀状CD"),
						rs.getBoolean("c有効FLG"), rs.getBoolean("c贈答FLG")
					);
				}
				return new IndDTO(
					rs.getString("i氏名"), rs.getString("iシメイ"), rs.getString("i部署名"), rs.getString("i役職名"),
					rs.getString("i法人CD"),
					rs.getString("ialpha_2"), rs.getString("i郵便番号"), rs.getString("i郵便枝番"), rs.getString("i都道府県"),
					rs.getString("i市区町村"), rs.getString("i町域"), rs.getString("i番地"),
					rs.getString("i建物等"), rs.getString("iTEL1"), rs.getString("iTEL2"), rs.getString("iTEL3"),
					rs.getString("iFAX1"), rs.getString("iFAX2"), rs.getString("iFAX3"), rs.getString("iメール"), "",
					rs.getString("i備考"), input, rs.getString("社名"),
					rs.getInt("i敬称"), rs.getInt("i年賀状CD"),
					rs.getBoolean("i有効FLG"), rs.getBoolean("i贈答FLG"), rs.getBoolean("i喪FLG"),
					rs.getBoolean("i自宅FLG"), rs.getBoolean("i住所FLG"), corpDTO
				);
			}
		}
		return null;
	}
}
