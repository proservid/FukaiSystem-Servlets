package fukaisystem.table;

import java.io.IOException;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import javax.servlet.ServletResponse;

import fukaisystem.ServiceFoundation;
import fukaisystem.dto.GetTableDTO;

public class GetSalesSummary extends ServiceFoundation {
	protected static final String className = "GetSalesSummary";

	@Override
	public Object access(Connection c, ServletResponse response, Object o)
		throws IOException, SQLException, ParseException {
		GetTableDTO dto = cast(response, o, GetTableDTO.class);
		Map<String, Map<String, List<String>>> data = new HashMap<>();
		Map<String, List<String>> tables = dto.getTables();
		for (Map.Entry<String, List<String>> m : tables.entrySet()) {
			String table = m.getKey();
			String columns = String.join(",", m.getValue());
				// 売上集計ヘッダは特別な処理（例外扱い）
				data.put(table, getSalesSummary(c, table, columns, dto.getConditions(), dto.getOrder()));
		}
		return data;
	}

	/**
	 * 売上集計ヘッダを取得する
	 * 
	 * @param c Connectionオブジェクト
	 * @param dto GetTableDTOオブジェクト
	 * @return 売上集計ヘッダ
	 * @throws SQLException
	 * @throws ParseException
	 */
	private Map<String, List<String>> getSalesSummary(
		Connection c, String table, String columns, Map<String, Map<String, Object>> conditions, String order
	) throws SQLException, ParseException {
		SimpleDateFormat format = new SimpleDateFormat("yyyy/MM/dd");
		Calendar cal = Calendar.getInstance();
		cal.setTimeInMillis(format.parse(conditions.get("").get("0").toString()).getTime()); // TODO:
		Date current = new Date(cal.getTimeInMillis());
		cal.add(Calendar.MONTH, 1);
		Date next = new Date(cal.getTimeInMillis());
		cal.add(Calendar.DATE, -1);
		Date last = new Date(cal.getTimeInMillis());

		StringBuilder query = new StringBuilder(
			"SELECT "
				+ "得意先CD,"
				+ "出荷伝票番号,"
				+ "社名,"
				+ "見出し + ' ( \\' + REPLACE(CONVERT(VARCHAR, CAST(納入合計 AS MONEY), 1), '.00', '') + ' )\n' + 消費税表示 AS 納入見出し1,"
				+ "見出し + '\n' + 消費税表示 AS 納入見出し2,"
				+ "REPLACE(CONVERT(VARCHAR, CAST(納入合計 AS MONEY), 1), '.00', '') AS 納入額,"
				+ "数量,"
				+ "REPLACE(CONVERT(VARCHAR, CAST(ROUND(CAST(ROUND(納入合計 * 適用税率, 0) AS DECIMAL(18, 9)), 2) AS MONEY), 1), '.00', '') AS 税額,"
				+ "REPLACE(CONVERT(VARCHAR, CAST(納入合計 + ROUND(納入合計 * 適用税率, 0) AS MONEY), 1), '.00', '') AS 合計額,"
				+ "売上日"
				+ " FROM ("
				+ "SELECT "
				+ "得意先CD,"
				+ "'9-' + CONVERT(VARCHAR, ROW_NUMBER() OVER (" + order + ")) AS 出荷伝票番号,"
				+ "MIN(社名) AS 社名,"
				+ "CONVERT(VARCHAR, MONTH(?)) + '月度納入額' AS 見出し,"
				+ "SUM(金額) AS 納入合計,"
				+ "'* 消費税（税率' + REPLACE(CONVERT(VARCHAR, CAST(適用税率 * 100 AS MONEY)), '.00', '') + '%）' AS 消費税表示,"
				+ "適用税率,"
				+ "'1式' AS 数量,"
				+ "dbo.F_和暦表示(?) AS 売上日"
				+ " FROM ("
				+ "SELECT 得意先CD, 社名, 金額, 売上年月日,"
				+ "(SELECT 税率 FROM M_消費税 t WHERE 適用開始日 <= ? AND NOT EXISTS ("
				+ "SELECT 1 FROM M_消費税 t2 WHERE t.適用開始日 < t2.適用開始日 AND 適用開始日 <= ?"
				+ ")) AS 適用税率"
				+ " FROM V_売上集計ヘッダ"
				+ ") h"
				+ " WHERE 売上年月日 >= ? AND 売上年月日 < ?"
				+ " GROUP BY 得意先CD, 適用税率"
				+ ") a"
		);
		if (conditions.get("").get("2").toString().equals("0")) {// TODO: その月の対象社一覧を表示する場合
			try (PreparedStatement ps = c.prepareStatement(query.toString());) {
				ps.setDate(1, current); // 月度納入額
				ps.setDate(2, last); // 売上日
				ps.setDate(3, last); // 消費税
				ps.setDate(4, last); // 消費税
				ps.setDate(5, current); // 抽出(from)
				ps.setDate(6, next); // 抽出(to)
				Vector<Vector<Object>> dataVector = new Vector<>();
				try (ResultSet rs = ps.executeQuery();) {
					while (rs.next()) {
						Vector<Object> record = new Vector<>();
						record.add(rs.getString("得意先CD"));
						record.add(rs.getString("出荷伝票番号"));
						record.add(rs.getString("社名"));
						record.add(rs.getString("納入額"));
						record.add(rs.getString("税額"));
						dataVector.add(record);
					}
				}
				// return dataVector;
				return null; // TODO: 専用のサーブレットに分離のこと
			}
		} else { // 対象社のうちの1社を選択した場合
			query.append(" WHERE 得意先CD=" + conditions.get("").get("2").toString()); // TODO:
			try (PreparedStatement ps = c.prepareStatement(query.toString());) {
				ps.setDate(1, current); // 月度納入額
				ps.setDate(2, last); // 消費税
				ps.setDate(3, last); // 消費税
				ps.setDate(4, last); // 売上日
				ps.setDate(5, current); // 抽出(from)
				ps.setDate(6, next); // 抽出(to)
				try (ResultSet rs = ps.executeQuery();) {
					ResultSetMetaData metaData = rs.getMetaData();
					int columnCount = metaData.getColumnCount();
					Map<String, List<String>> dataMap = new HashMap<>();
					while (rs.next()) {
						for (int k = 0; k < columnCount; k++) {
							String columnName = metaData.getColumnName(k + 1);
							if (dataMap.containsKey(columnName))
								dataMap.get(columnName).add(rs.getString(columnName));
							else {
								List<String> columnData = new ArrayList<>();
								columnData.add(rs.getString(columnName));
								dataMap.put(columnName, columnData);
							}
						}
						return dataMap;
					}
				}
			}
		}
		return null;
	}

}
