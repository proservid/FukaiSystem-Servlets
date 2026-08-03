package fukaisystem.application.address;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.ServletResponse;

import fukaisystem.foundation.ServiceFoundation;

public class GetCountry extends ServiceFoundation {

	@Override
	public Object access(Connection c, ServletResponse response, Object o) throws IOException, SQLException {

		String input = cast(response, o, String.class);
		List<String> output = new ArrayList<String>();

		try (
			PreparedStatement ps = c.prepareStatement(
				"select ccTLD,国際電話国番号,国名,英語名,"
					+ "case when 郵便番号 is null then '#' else 郵便番号 end as 郵便番号"
					+ " from M_国 where alpha_2=?"
			);
		) {
			ps.setString(1, input);
			ResultSet rs = ps.executeQuery();
			if (rs.next()) {
				output.add(rs.getString("ccTLD"));
				output.add(rs.getString("国際電話国番号"));
				output.add(rs.getString("国名"));
				output.add(rs.getString("英語名"));
				output.add(rs.getString("郵便番号"));
			}
		}
		return output;
	}
}
