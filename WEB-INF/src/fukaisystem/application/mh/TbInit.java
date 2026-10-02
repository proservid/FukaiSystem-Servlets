package fukaisystem.application.mh;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletResponse;

import fukaisystem.foundation.ServiceFoundation;

public class TbInit extends ServiceFoundation {

	@Override
	public Object access(Connection c, ServletResponse response, Object o) throws IOException, SQLException {

		Map<String, String> validMembers = new LinkedHashMap<String, String>();
		Map<String, String> allMembers = new LinkedHashMap<String, String>();
		Map<String, String> validWorks = new LinkedHashMap<String, String>();
		Map<String, String> allWorks = new LinkedHashMap<String, String>();
		List<Map<String, String>> list = new ArrayList<Map<String, String>>();

		try (
			PreparedStatement ps = c.prepareStatement(
				"select distinct 担当者CD,姓+名 as 氏名 from T_加工実績 h"
					+ " left outer join M_人員 m on h.担当者CD=m.CD"
					+ " where 在籍FLG='true'"
					+ " order by 担当者CD"
			);
		) {
			ResultSet rs = ps.executeQuery();
			while (rs.next()) {
				validMembers.put(rs.getString("担当者CD"), rs.getString("氏名"));
			}
			list.add(validMembers);
		}

		try (
			PreparedStatement ps = c.prepareStatement(
				"select distinct 担当者CD,姓+名 as 氏名 from T_加工実績 h"
					+ " left outer join M_人員 m on h.担当者CD=m.CD"
					+ " order by 担当者CD"
			);
		) {
			ResultSet rs = ps.executeQuery();
			while (rs.next()) {
				allMembers.put(rs.getString("担当者CD"), rs.getString("氏名"));
			}
			list.add(allMembers);
		}

		try (
			PreparedStatement ps = c.prepareStatement(
				"select distinct 加工CD,小分類名 from T_加工実績 h"
					+ " left outer join M_加工_子 w on h.加工CD=w.CD"
					+ " where 使用FLG='true'"
					+ " order by 加工CD"
			);
		) {
			ResultSet rs = ps.executeQuery();
			while (rs.next()) {
				validWorks.put(rs.getString("加工CD"), rs.getString("小分類名"));
			}
			list.add(validWorks);
		}

		try (
			PreparedStatement ps = c.prepareStatement(
				"select distinct 加工CD,小分類名 from T_加工実績 h"
					+ " left outer join M_加工_子 w on h.加工CD=w.CD"
					+ " order by 加工CD"
			);
		) {
			ResultSet rs = ps.executeQuery();
			while (rs.next()) {
				allWorks.put(rs.getString("加工CD"), rs.getString("小分類名"));
			}
			list.add(allWorks);
		}

		return list;
	}

}
