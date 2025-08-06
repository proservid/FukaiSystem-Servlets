package fukaisystem.application.business;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import java.util.Vector;

import javax.servlet.ServletResponse;

import fukaisystem.dto.InitialDTO;
import fukaisystem.foundation.ServiceFoundation;

/**
 * 見積書の選択候補を更新する
 */
public class UpdateCandidate extends ServiceFoundation {

	@Override
	public Object access(Connection c, ServletResponse response, Object o) throws IOException, SQLException {

		Vector<String> dues = new Vector<String>();
		Vector<String> places = new Vector<String>();
		Vector<String> terms = new Vector<String>();
		Vector<String> validities = new Vector<String>();

		try (PreparedStatement ps = c.prepareStatement("SELECT * FROM M_納期 WHERE CD<100");) {
			try (ResultSet rs = ps.executeQuery();) {
				while (rs.next()) {
					dues.add(rs.getString("納期"));
				}
			}
		}
		try (PreparedStatement ps = c.prepareStatement("SELECT * FROM M_受渡場所");) {
			try (ResultSet rs = ps.executeQuery();) {
				while (rs.next()) {
					places.add(rs.getString("受渡場所"));
				}
			}
		}
		try (PreparedStatement ps = c.prepareStatement("SELECT * FROM M_取引条件");) {
			try (ResultSet rs = ps.executeQuery();) {
				while (rs.next()) {
					terms.add(rs.getString("取引条件"));
				}
			}
		}
		try (PreparedStatement ps = c.prepareStatement("SELECT * FROM M_有効期間 WHERE CD<18");) {
			try (ResultSet rs = ps.executeQuery();) {
				while (rs.next()) {
					validities.add(rs.getString("有効期間"));
				}
			}
		}

		return new InitialDTO(
			null, null, null, dues, places, terms,
			validities, null, null, null, null, null, null, null, null, null,
			null, null, null, null, null, null
		);

	}

}
