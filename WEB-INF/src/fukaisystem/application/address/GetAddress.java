package fukaisystem.application.address;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Vector;

import javax.servlet.ServletResponse;

import fukaisystem.foundation.ServiceFoundation;

public class GetAddress extends ServiceFoundation {

	@Override
	public Object access(Connection c, ServletResponse response, Object o) throws IOException, SQLException {

		String input = cast(response, o, String.class);
		Vector<Vector<String>> output = new Vector<Vector<String>>();

		try (
			PreparedStatement ps = c.prepareStatement(
				"select 都道府県,市区町村,"
					+ "case when 町域 is null then 町域補足 else 町域 end as 町域,"
					+ "case when 京都通り名 is null then '' else 京都通り名 end as 京都通り名,"
					+ "case when 字丁目 is null then '' else 字丁目 end as 字丁目,"
					+ "case when 補足 is null then '' else 補足 end as 補足,"
					+ "case when 事業所名 is null then '' else 事業所名 end as 事業所名,"
					+ "case when 事業所住所 is null then '' else 事業所住所 end as 事業所住所,"
					+ "郵便枝番"
					+ " from V_郵便番号 pc"
					+ " left outer join M_都道府県 p on pc.都道府県CD=p.CD"
					+ " left outer join M_市区町村 c on pc.都道府県CD=c.都道府県CD and pc.市区町村CD=c.CD" +
					(input.length() == 7 ? " where 郵便番号=?" : " where 郵便番号+郵便枝番=?")
			);
		) {
			ps.setString(1, input);
			ResultSet rs = ps.executeQuery();
			while (rs.next()) {
				Vector<String> record = new Vector<String>();
				record.add(rs.getString("都道府県"));
				record.add(rs.getString("市区町村"));
				record.add(rs.getString("町域") + rs.getString("京都通り名"));
				record.add(rs.getString("字丁目") + rs.getString("補足"));
				record.add(rs.getString("事業所名"));
				record.add(rs.getString("事業所住所"));
				record.add(rs.getString("郵便枝番"));
				output.add(record);
			}
			if (output.size() == 0) {
				Vector<String> record = new Vector<String>();
				for (int i = 0; i < 7; i++) {
					record.add("");
				}
				output.add(record);
			}
		}
		return output;
	}
}
