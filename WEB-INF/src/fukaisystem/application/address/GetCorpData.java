package fukaisystem.application.address;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import javax.servlet.ServletResponse;

import fukaisystem.dto.address.CorpDTO;
import fukaisystem.foundation.ServiceFoundation;

public class GetCorpData extends ServiceFoundation {

	@Override
	public Object access(Connection c, ServletResponse response, Object o) throws IOException, SQLException {

		String input = cast(response, o, String.class);

		try (
			PreparedStatement ps = c.prepareStatement(
				"select "
					+ "会社名, カイシャメイ, 支店名, シテンメイ, 表示名,"
					+ "alpha_2, co.郵便番号, co.郵便枝番, p.都道府県,市区町村,町域,"
					+ "番地,建物等,TEL1,TEL2,TEL3,FAX1,FAX2,FAX3,メール,URL,"
					+ "備考,アルファベット,登録番号,仕入先CD,得意先CD,種別CD,有効FLG,贈答FLG,年賀状CD"
					+ " from M_法人 co"
					+ " left outer join V_郵便番号 pc on replace(co.郵便番号,'-','')=pc.郵便番号 and co.郵便枝番=pc.郵便枝番"
					+ " left outer join M_都道府県 p on pc.都道府県CD=p.CD"
					+ " left outer join M_市区町村 c on pc.都道府県CD=c.都道府県CD and pc.市区町村CD=c.CD"
					+ " where co.CD=?"
			);
		) {
			ps.setString(1, input);
			ResultSet rs = ps.executeQuery();
			if (rs.next()) {
				return new CorpDTO(
					rs.getString("会社名"), rs.getString("カイシャメイ"), rs.getString("支店名"), rs.getString("シテンメイ"),
					rs.getString("表示名"),
					rs.getString("alpha_2"), rs.getString("郵便番号"), rs.getString("郵便枝番"), rs.getString("都道府県"),
					rs.getString("市区町村"),
					rs.getString("町域"), rs.getString("番地"), rs.getString("建物等"), rs.getString("TEL1"),
					rs.getString("TEL2"), rs.getString("TEL3"),
					rs.getString("FAX1"), rs.getString("FAX2"), rs.getString("FAX3"), rs.getString("メール"),
					rs.getString("URL"), rs.getString("備考"),
					input, rs.getString("アルファベット"), rs.getString("登録番号"),
					rs.getInt("仕入先CD"), rs.getInt("得意先CD"), rs.getInt("種別CD"), rs.getInt("年賀状CD"),
					rs.getBoolean("有効FLG"), rs.getBoolean("贈答FLG")
				);
			}
		}

		return null;
	}
}
