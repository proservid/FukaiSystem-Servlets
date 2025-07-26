package fukaisystem.application.business;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import javax.servlet.ServletResponse;

import fukaisystem.dto.InitialDTO;
import fukaisystem.foundation.ServiceFoundation;

/**
 * システム初期化用の基礎データを取得する
 */
public class Init extends ServiceFoundation {

	@Override
	public Object access(Connection c, ServletResponse response, Object o) throws IOException, SQLException {

		Map<Integer, String> typeMap = new LinkedHashMap<Integer, String>();
		Map<Integer, String> routeMap = new HashMap<Integer, String>();
		Vector<String> dueVector = new Vector<String>();
		Vector<String> placeVector = new Vector<String>();
		Vector<String> termVector = new Vector<String>();
		Vector<String> validityVector = new Vector<String>();
		Map<Integer, String> submitMap = new HashMap<Integer, String>();
		Map<Integer, String> currencieMap = new HashMap<Integer, String>();
		Map<Integer, String> stateMap = new HashMap<Integer, String>();
		Map<Integer, String> wayMap = new HashMap<Integer, String>();
		Map<Integer, String> partialMap = new HashMap<Integer, String>();
		Map<Integer, String> indicationMap = new HashMap<Integer, String>();
		Map<Integer, String> unitMap = new HashMap<Integer, String>();
		Map<Integer, String> supplierMap = new HashMap<Integer, String>();
		// Map<Integer, Integer> processingCostMap = new HashMap<Integer, Integer>();
		Map<Integer, String> materialCostMap = new LinkedHashMap<Integer, String>();
		Map<Integer, String> coarseCategoryMap = new LinkedHashMap<Integer, String>();
		Map<List<Integer>, Map<Integer, String>> middleCategoryMap = new HashMap<List<Integer>, Map<Integer, String>>();
		Map<List<Integer>, Map<Integer, String>> fineCategoryMap = new HashMap<List<Integer>, Map<Integer, String>>();
		Map<Integer, Map<Integer, Map<Integer, Integer>>> costMap = new HashMap<Integer, Map<Integer, Map<Integer, Integer>>>();
		Map<Integer, Double> sgMap = new HashMap<Integer, Double>();

		try (PreparedStatement ps = c.prepareStatement("SELECT * FROM M_製品種別");) {
			try (ResultSet rs = ps.executeQuery();) {
				while (rs.next()) {
					typeMap.put(rs.getInt("CD"), rs.getString("製品種別"));
				}
			}
		}

		try (PreparedStatement ps = c.prepareStatement("SELECT * FROM M_依頼手段");) {
			try (ResultSet rs = ps.executeQuery();) {
				while (rs.next()) {
					routeMap.put(rs.getInt("CD"), rs.getString("依頼手段"));
				}
			}
		}

		try (PreparedStatement ps = c.prepareStatement("SELECT * FROM M_納期 WHERE CD<100");) {
			try (ResultSet rs = ps.executeQuery();) {
				while (rs.next()) {
					dueVector.add(rs.getString("納期"));
				}
			}
		}
		try (PreparedStatement ps = c.prepareStatement("SELECT * FROM M_受渡場所");) {
			try (ResultSet rs = ps.executeQuery();) {
				while (rs.next()) {
					placeVector.add(rs.getString("受渡場所"));
				}
			}
		}
		try (PreparedStatement ps = c.prepareStatement("SELECT * FROM M_取引条件");) {
			try (ResultSet rs = ps.executeQuery();) {
				while (rs.next()) {
					termVector.add(rs.getString("取引条件"));
				}
			}
		}
		try (PreparedStatement ps = c.prepareStatement("SELECT * FROM M_有効期間");) {
			try (ResultSet rs = ps.executeQuery();) {
				while (rs.next()) {
					validityVector.add(rs.getString("有効期間"));
				}
			}
		}

		try (PreparedStatement ps = c.prepareStatement("SELECT * FROM M_見積提出");) {
			try (ResultSet rs = ps.executeQuery();) {
				while (rs.next()) {
					submitMap.put(rs.getInt("CD"), rs.getString("見積提出"));
				}
			}
		}

		try (PreparedStatement ps = c.prepareStatement("SELECT * FROM M_使用通貨");) {
			try (ResultSet rs = ps.executeQuery();) {
				while (rs.next()) {
					currencieMap.put(rs.getInt("CD"), rs.getString("通貨記号"));
				}
			}
		}

		try (PreparedStatement ps = c.prepareStatement("SELECT * FROM M_納品区分");) {
			try (ResultSet rs = ps.executeQuery();) {
				while (rs.next()) {
					stateMap.put(rs.getInt("CD"), rs.getString("納品区分"));
				}
			}
		}

		try (PreparedStatement ps = c.prepareStatement("SELECT * FROM M_納入手段");) {
			try (ResultSet rs = ps.executeQuery();) {
				while (rs.next()) {
					wayMap.put(rs.getInt("CD"), rs.getString("納入手段"));
				}
			}
		}

		try (PreparedStatement ps = c.prepareStatement("SELECT * FROM M_納品区分");) {
			try (ResultSet rs = ps.executeQuery();) {
				while (rs.next()) {
					partialMap.put(rs.getInt("CD"), rs.getString("納品区分"));
				}
			}
		}

		try (PreparedStatement ps = c.prepareStatement("SELECT * FROM M_見積表示");) {
			try (ResultSet rs = ps.executeQuery();) {
				while (rs.next()) {
					indicationMap.put(rs.getInt("CD"), rs.getString("表示種別"));
				}
			}
		}

		try (PreparedStatement ps = c.prepareStatement("SELECT * FROM M_数量単位");) {
			try (ResultSet rs = ps.executeQuery();) {
				while (rs.next()) {
					unitMap.put(rs.getInt("CD"), rs.getString("数量単位"));
				}
			}
		}

		try (PreparedStatement ps = c.prepareStatement("SELECT 仕入先CD,会社名 FROM M_法人 WHERE 仕入先CD IS NOT NULL");) {
			try (ResultSet rs = ps.executeQuery();) {
				while (rs.next()) {
					supplierMap.put(rs.getInt("仕入先CD"), rs.getString("会社名"));
				}
			}
		}
		/*
		 * Calendar cal = Calendar.getInstance();
		 * try (PreparedStatement ps = c.prepareStatement("SELECT wc.CD,wp.単価 FROM M_加工_子 wc"){
		 * + " left outer join (select CD,単価 from M_加工_単価 wp1"
		 * + " where 適用開始日<? AND NOT EXISTS ("
		 * + "		SELECT 1 FROM M_加工_単価 wp2"
		 * + "		WHERE wp1.適用開始日<wp2.適用開始日 AND wp1.CD=wp2.CD AND 適用開始日<?)"
		 * + "	) wp on wp.CD=wc.CD");
		 * ps.setDate(1, new java.sql.Date(cal.getTimeInMillis()));
		 * ps.setDate(2, new java.sql.Date(cal.getTimeInMillis()));
		 * try (ResultSet rs = ps.executeQuery();){
		 * while(rs.next()) {
		 * processingCostMap.put(rs.getInt("CD"), rs.getInt("単価"));
		 * }}}
		 */
		try (PreparedStatement ps = c.prepareStatement("SELECT * FROM M_原価 WHERE CD<101");) {
			try (ResultSet rs = ps.executeQuery();) {
				while (rs.next()) {
					materialCostMap.put(rs.getInt("CD"), rs.getString("大分類名"));
				}
			}
		}

		try (PreparedStatement ps = c.prepareStatement("SELECT * FROM M_原価");) {
			try (ResultSet rs = ps.executeQuery();) {
				while (rs.next()) {
					coarseCategoryMap.put(rs.getInt("CD"), rs.getString("大分類名"));
				}
			}
		}

		try (PreparedStatement ps = c.prepareStatement("SELECT * FROM M_材料_親");) {
			try (ResultSet rs = ps.executeQuery();) {
				while (rs.next()) {
					List<Integer> subKey = new ArrayList<Integer>();
					subKey.add(rs.getInt("大分類CD"));
					if (!middleCategoryMap.containsKey(subKey)) {
						middleCategoryMap.put(subKey, new LinkedHashMap<Integer, String>());
					}
					middleCategoryMap.get(subKey).put(rs.getInt("CD"), rs.getString("中分類名"));
				}
			}
		}
		try (PreparedStatement ps = c.prepareStatement("SELECT * FROM M_加工_親");) {
			try (ResultSet rs = ps.executeQuery();) {
				while (rs.next()) {
					List<Integer> subKey = new ArrayList<Integer>();
					subKey.add(rs.getInt("大分類CD"));
					if (!middleCategoryMap.containsKey(subKey)) {
						middleCategoryMap.put(subKey, new LinkedHashMap<Integer, String>());
					}
					middleCategoryMap.get(subKey).put(rs.getInt("CD"), rs.getString("中分類名"));
				}
			}
		}

		try (PreparedStatement ps = c.prepareStatement("SELECT * FROM M_比重");) {
			try (ResultSet rs = ps.executeQuery();) {
				while (rs.next()) {
					sgMap.put(rs.getInt("種類"), rs.getDouble("比重"));
				}
			}
		}

		try (PreparedStatement ps = c.prepareStatement("SELECT * FROM M_材料_子");) {
			try (ResultSet rs = ps.executeQuery();) {
				while (rs.next()) {
					List<Integer> subKey = new ArrayList<Integer>();
					subKey.add(rs.getInt("大分類CD"));
					subKey.add(rs.getInt("中分類CD"));
					if (!fineCategoryMap.containsKey(subKey)) {
						fineCategoryMap.put(subKey, new LinkedHashMap<Integer, String>());
					}
					fineCategoryMap.get(subKey).put(rs.getInt("CD"), rs.getString("小分類名"));
				}
			}
		}
		Calendar cal = Calendar.getInstance();
		try (
			PreparedStatement ps = c.prepareStatement(
				"SELECT wc.CD,中分類CD,大分類CD,小分類名,wp.単価 FROM M_加工_子 wc"
					+ " left outer join (select CD,単価 from M_加工_単価 wp1"
					+ " where 適用開始日<? AND NOT EXISTS ("
					+ "		SELECT 1 FROM M_加工_単価 wp2"
					+ "		WHERE wp1.適用開始日<wp2.適用開始日 AND wp1.CD=wp2.CD AND 適用開始日<?)"
					+ "	) wp on wp.CD=wc.CD"
					+ " WHERE 使用FLG='true'"
			);
		) {
			ps.setDate(1, new java.sql.Date(cal.getTimeInMillis()));
			ps.setDate(2, new java.sql.Date(cal.getTimeInMillis()));
			try (ResultSet rs = ps.executeQuery();) {
				while (rs.next()) {
					int coarseCD = rs.getInt("大分類CD");
					int middleCD = rs.getInt("中分類CD");
					int fineCD = rs.getInt("CD");
					int cost = rs.getInt("単価");
					String name = rs.getString("小分類名");
					if (costMap.containsKey(coarseCD)) {
						Map<Integer, Map<Integer, Integer>> middleMap = costMap.get(coarseCD);
						if (middleMap.containsKey(middleCD)) {
							Map<Integer, Integer> fineMap = middleMap.get(middleCD);
							fineMap.put(fineCD, cost);
						} else {
							Map<Integer, Integer> fineMap = new HashMap<Integer, Integer>();
							fineMap.put(fineCD, cost);
							middleMap.put(middleCD, fineMap);
						}
					} else {
						Map<Integer, Map<Integer, Integer>> middleMap = new HashMap<Integer, Map<Integer, Integer>>();
						Map<Integer, Integer> fineMap = new HashMap<Integer, Integer>();
						fineMap.put(fineCD, cost);
						middleMap.put(middleCD, fineMap);
						costMap.put(coarseCD, middleMap);
					}
					List<Integer> subKey = new ArrayList<Integer>();
					subKey.add(coarseCD);
					subKey.add(middleCD);
					if (!fineCategoryMap.containsKey(subKey)) {
						fineCategoryMap.put(subKey, new LinkedHashMap<Integer, String>());
					}
					if (name != null)
						fineCategoryMap.get(subKey).put(fineCD, name);
				}
			}
		}

		return new InitialDTO(
			null,
			typeMap,
			routeMap,
			dueVector,
			placeVector,
			termVector,
			validityVector,
			submitMap,
			currencieMap,
			stateMap,
			wayMap,
			partialMap,
			indicationMap,
			unitMap,
			supplierMap,
			null,
			materialCostMap,
			coarseCategoryMap,
			middleCategoryMap,
			fineCategoryMap,
			costMap,
			sgMap
		);
	}

}
