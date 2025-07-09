package fukaisystem.application.business;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import javax.servlet.GenericServlet;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import org.apache.log4j.Logger;

import fukaisystem.dto.IDDTO;
import fukaisystem.dto.ProjectSummaryDTO;
import fukaisystem.sql.DBConnection;
import fukaisystem.util.Logging;

public class GetSummary extends GenericServlet {

	private static final long serialVersionUID = 1L;
	private static final Logger lg = Logger.getLogger("A1");
	private static final String className = "GetSummary\n";

	@Override
	public void service(ServletRequest request, ServletResponse response) {
		DBConnection dbc = new DBConnection();
		Connection c = dbc.getConnection();
		PreparedStatement ps = null;
		ResultSet rs = null;
		ProjectSummaryDTO summaryDTO = null;
		StringBuilder err = new StringBuilder();

		int quotationID = 0, productionID = 0, salesID = 0;
		int tax = 0, discount = 0;

		Vector<Vector<Object>> quotationData = new Vector<Vector<Object>>();
		Vector<Vector<Object>> productionData = new Vector<Vector<Object>>();
		Vector<Vector<Object>> salesData = new Vector<Vector<Object>>();
		Vector<Vector<Object>> salesSlips = new Vector<Vector<Object>>();
		Map<Integer, Vector<Vector<Object>>> quotationBasisDataMap = new HashMap<Integer, Vector<Vector<Object>>>();

		try {

			/**
			 * クライアントデータ受け取り
			 */
			ObjectInputStream in = new ObjectInputStream(request.getInputStream());
			Object obj = in.readObject();
			in.close();

			if (obj == null) {
				quotationID = 0;
				productionID = 0;
				salesID = 0;
			} else {
				if (obj instanceof IDDTO) {
					quotationID = ((IDDTO) obj).getQuotationID();
					productionID = ((IDDTO) obj).getProductionID();
					salesID = ((IDDTO) obj).getSalesID();
				} else {
					err.append(className + "readObjectがIDDTO型ではありません\n");
					lg.error(className + "readObjectがIDDTO型ではありません");
				}
			}
		} catch (Exception ex) {
			Logging.logStackTrace(ex, lg, className);
		}

		try { // TODO:やや遅い
			StringBuilder query = new StringBuilder(
				"SELECT TOP 1"
					+ " e.案件名 AS 案件名e, p.案件名 AS 案件名p,"
					+ " CASE WHEN p.得意先CD=0 OR p.得意先CD IS NULL THEN e.親機番号 ELSE p.親機番号 END AS 親機番号,"
					+ " CASE WHEN p.得意先CD=0 OR p.得意先CD IS NULL THEN e.誕生期 ELSE p.誕生期 END AS 誕生期,"
					+ " CASE WHEN p.得意先CD=0 OR p.得意先CD IS NULL THEN e.誕生番号 ELSE p.誕生番号 END AS 誕生番号,"
					+ " CASE WHEN p.得意先CD=0 OR p.得意先CD IS NULL THEN e.誕生枝番 ELSE p.誕生枝番 END AS 誕生枝番,"
					+ " e.得意先CD AS 得意先CDe, p.得意先CD AS 得意先CDp,"
					+ " CASE WHEN p.得意先CD=0 OR p.得意先CD IS NULL THEN e.購入者CD ELSE p.購入者CD END AS 購入者CD,"
					/*
					 * " CASE" +
					 * " WHEN ce.種別CD=1 THEN '㈱'+ce.会社名 + CASE WHEN ce.支店名 IS NULL THEN '' ELSE ' ' + ce.支店名 END" +
					 * " WHEN ce.種別CD=2 THEN ce.会社名+'㈱' + CASE WHEN ce.支店名 IS NULL THEN '' ELSE ' ' + ce.支店名 END" +
					 * " WHEN ce.種別CD=3 THEN '㈲'+ce.会社名 + CASE WHEN ce.支店名 IS NULL THEN '' ELSE ' ' + ce.支店名 END" +
					 * " WHEN ce.種別CD=4 THEN ce.会社名+'㈲' + CASE WHEN ce.支店名 IS NULL THEN '' ELSE ' ' + ce.支店名 END"
					 * " ELSE ce.会社名 END AS 社名e," +
					 */
					+ "得意先表示名 AS 社名e,"
					+ " CASE"
					+ " WHEN cp.種別CD=1 THEN '㈱'+cp.会社名 + CASE WHEN cp.支店名 IS NULL THEN '' ELSE ' ' + cp.支店名 END"
					+ " WHEN cp.種別CD=2 THEN cp.会社名+'㈱' + CASE WHEN cp.支店名 IS NULL THEN '' ELSE ' ' + cp.支店名 END"
					+ " WHEN cp.種別CD=3 THEN '㈲'+cp.会社名 + CASE WHEN cp.支店名 IS NULL THEN '' ELSE ' ' + cp.支店名 END"
					+ " WHEN cp.種別CD=4 THEN cp.会社名+'㈲' + CASE WHEN cp.支店名 IS NULL THEN '' ELSE ' ' + cp.支店名 END"
					+ " ELSE cp.会社名 END AS 社名p,"
					+ " e.見積親ID, 見積期, 見積番号, 見積枝番, 案内文,"
					+ " de.納期 AS 納期de, 受渡場所, 取引条件, 有効期間, e.摘要 AS 摘要e,"
					+ " 提出済CD, e.通貨CD AS 通貨e, 見積金額,"
					+ " 見積年月日, 提出年月日, 受注番号, 納入先名, p.摘要 AS 摘要p, p.製作親ID, 製作期, 製作番号, 製作枝番, p.通貨CD AS 通貨p, 契約金額,"
					+ " 受注年月日, p.納期 AS 納期p, p.発行年月日,"
					+ " CASE WHEN (p.機械番号 IS NULL OR p.機械番号=0) THEN 0 ELSE 1 END AS 新機FLG,"
					+ " 出図FLG, 手配FLG,"
					+ " 出荷年月日, 検収年月日,"
					+ "s.得意先CD AS 得意先CDd, s.売上年月日, s.納品区分CD, s.納品手段CD, s.消費税, s.値引き, s.摘要 AS 摘要s"
					+ " FROM (SELECT "
					+ "ee.見積親ID, 見積期, 見積番号, 見積枝番, ee.案件名, ee.案内文, ee.得意先CD, ee.得意先表示名, eo.得意先CD AS 購入者CD, 納期CD, 受渡場所CD, 取引条件CD, "
					+ "有効期間CD, 提出済CD, 見積年月日, 提出年月日, ee.通貨CD, 見積金額, ee.摘要, ee.更新日, ee.更新者CD, "
					+ "製作期 AS 誕生期, 製作番号 AS 誕生番号, 製作枝番 AS 誕生枝番, 機械番号 AS 親機番号"
					+ " FROM T_見積_親 ee LEFT OUTER JOIN T_製作_親 eo ON ee.元製作親ID=eo.製作親ID) e"
					+ " LEFT OUTER JOIN M_納期 de ON e.納期CD=de.CD"
					+ " LEFT OUTER JOIN M_受渡場所 pl ON e.受渡場所CD=pl.CD"
					+ " LEFT OUTER JOIN M_取引条件 te ON e.取引条件CD=te.CD"
					+ " LEFT OUTER JOIN M_有効期間 va ON e.有効期間CD=va.CD"
					+ " LEFT OUTER JOIN T_見積製作 map ON e.見積親ID=map.見積親ID"
					+ " FULL OUTER JOIN (SELECT "
					+ "pp.製作親ID, pp.製作期, pp.製作番号, pp.製作枝番, pp.受注番号, pp.案件名, pp.見積親ID, pp.得意先CD,"
					+ " po.得意先CD AS 購入者CD, pp.機械番号, pp.納入先名, pp.納期, pp.受注年月日, pp.発行年月日, pp.出荷年月日, pp.検収年月日,"
					+ " pp.通貨CD, pp.契約金額, pp.摘要, pp.出図FLG, pp.手配FLG, pp.更新日, pp.更新者CD,"
					+ " po.製作期 AS 誕生期, po.製作番号 AS 誕生番号, po.製作枝番 AS 誕生枝番, po.機械番号 AS 親機番号"
					+ " FROM T_製作_親 pp"
					+ " LEFT OUTER JOIN T_カルテ履歴 h ON h.製作親ID=pp.製作親ID"
					+ " LEFT OUTER JOIN T_製作_親 po ON h.元製作親ID=po.製作親ID) p"
					+ " ON map.製作親ID=p.製作親ID"
					+ " LEFT OUTER JOIN ("
					+ "	SELECT sp.売上親ID, 製作親ID, 得意先CD, 売上年月日, 納品区分CD, 納品手段CD, 消費税, 値引き, 摘要 FROM T_売上_親 sp"
					+ "	 LEFT OUTER JOIN T_売上_子 sc ON sp.売上親ID=sc.売上親ID) s"
					+ " ON p.製作親ID=s.製作親ID"
					+ " LEFT OUTER JOIN M_法人 ce"
					+ " ON e.得意先CD=ce.得意先CD"
					+ " LEFT OUTER JOIN M_法人 cp"
					+ " ON p.得意先CD=cp.得意先CD"
					+ " WHERE "
			);
			if (quotationID != 0) {
				query.append("e.見積親ID=? AND ");
			}
			if (productionID == 0) {
				query.append("p.製作親ID IS NULL");
			} else {
				query.append("p.製作親ID=?");
			}
			ps = c.prepareStatement(query.toString());
			int i = 1;
			if (quotationID != 0) {
				ps.setInt(i, quotationID);
				i++;
			}
			if (productionID != 0) {
				ps.setInt(i, productionID);
			}
			rs = ps.executeQuery();
			while (rs.next()) {
				summaryDTO = new ProjectSummaryDTO(
					// 共通
					rs.getString("社名e"), rs.getString("社名p"), rs.getString("案件名e"), rs.getString("案件名p"),
					rs.getInt("得意先CDe"), rs.getInt("得意先CDp"), rs.getInt("親機番号"), // 今すぐセットされるのは見積の親機番号、製作のはあとでsetList
					rs.getInt("購入者CD"),
					null, // あとでsetList
					// 見積
					rs.getString("見積枝番"), rs.getString("納期de"), rs.getString("受渡場所"), rs.getString("取引条件"),
					rs.getString("有効期間"), rs.getString("摘要e"), rs.getString("案内文"), rs.getString("誕生枝番"),
					quotationID, rs.getInt("見積期"), rs.getInt("見積番号"), 0, 0, // rs.getInt("個人CD"), rs.getInt("依頼手段CD"),
					rs.getInt("提出済CD"), rs.getInt("通貨e"), rs.getInt("見積金額"), rs.getInt("誕生期"), rs.getInt("誕生番号"),
					/* rs.getDate("依頼年月日") */null, rs.getDate("見積年月日"), rs.getDate("提出年月日"),
					null, // あとでsetVectorする。Vector<Vector<Object>> mainTable_e,
					null, // あとでsetMapする。Map<Integer, Vector<Vector<Object>>> subTable_e,
					// 製作
					rs.getString("受注番号"), rs.getString("製作枝番"), rs.getString("納入先名"), rs.getString("摘要p"),
					productionID, rs.getInt("製作期"), rs.getInt("製作番号"), rs.getInt("通貨p"),
					rs.getInt("契約金額"), rs.getDate("受注年月日"), rs.getDate("納期p"),
					rs.getDate("発行年月日"), rs.getDate("出荷年月日"), rs.getDate("検収年月日"),
					rs.getBoolean("新機FLG"), false, rs.getBoolean("出図FLG"), rs.getBoolean("手配FLG"),
					null, // あとでsetVectorする。Vector<Vector<Object>> mainTable_p,
					// 出荷
					rs.getString("摘要s"),
					salesID,
					rs.getInt("納品区分CD"), rs.getInt("納品手段CD"), rs.getInt("得意先CDd"),
					rs.getDate("売上年月日"),
					null, // あとでsetVectorする。Vector<Vector<Object>> mainTable_d
					null// あとでsetVectorする。Vector<Vector<Object>> slipList
				);
				if (rs.getString("消費税") == null) {
					tax = -1;
				} else {
					tax = rs.getInt("消費税");
				}
				discount = rs.getInt("値引き");
			}

			ps = c.prepareStatement("SELECT * FROM T_見積_子 WHERE 見積親ID=?");
			ps.setInt(1, quotationID);
			rs = ps.executeQuery();
			while (rs.next()) {
				int serial = rs.getInt("ID");
				Vector<Object> record = new Vector<Object>();
				record.add(serial);
				record.add(rs.getInt("表示CD"));
				record.add(rs.getString("名称"));
				record.add(rs.getBoolean("各FLG"));
				record.add(rs.getInt("数量"));
				record.add(rs.getInt("数量単位CD"));
				record.add(rs.getInt("単価"));
				record.add(rs.getInt("提示額"));
				record.add(rs.getString("図番"));
				record.add(rs.getString("備考"));
				quotationData.add(record);
			}

			// 見積明細
			ps = c.prepareStatement(
				"SELECT ID,見積子ID,見積親ID,大分類CD,中分類CD,小分類CD,名称,単価,数量,ROUND(単価*数量,0) AS 原価,掛率,ROUND(単価*数量*掛率,0) AS 小計,品番,重量,仕入先CD,仕入見積FLG,仕入納期,備考"
					+ " FROM T_見積_材料 WHERE 見積親ID=?"
					+ " UNION ALL"
					+ " SELECT ID,見積子ID,見積親ID,大分類CD,中分類CD,小分類CD,名称,単価,時間,ROUND(単価*時間,0) AS 原価,掛率,ROUND(単価*時間*掛率,0) AS 小計,0,0,0,'false','',備考"
					+ " FROM T_見積_加工"
					+ "  WHERE 見積親ID=? ORDER BY ID"
			);
			ps.setInt(1, quotationID);
			ps.setInt(2, quotationID);
			rs = ps.executeQuery();
			while (rs.next()) {
				Vector<Object> record = new Vector<Object>();
				record.add(rs.getInt("ID"));
				int id = rs.getInt("見積子ID");
				record.add(rs.getInt("大分類CD"));
				record.add(rs.getInt("中分類CD"));
				record.add(rs.getInt("小分類CD"));
				record.add(rs.getString("名称"));
				record.add(rs.getInt("単価"));
				record.add(rs.getDouble("数量"));
				record.add(rs.getInt("原価")); // 計算
				record.add(rs.getDouble("掛率"));
				record.add(rs.getInt("小計")); // 計算
				record.add(rs.getInt("品番"));
				record.add(rs.getDouble("重量"));
				record.add(rs.getInt("仕入先CD"));
				record.add(rs.getBoolean("仕入見積FLG"));
				record.add(rs.getString("仕入納期"));
				record.add(rs.getString("備考"));
				if (quotationBasisDataMap.containsKey(id)) {
					quotationBasisDataMap.get(id).add(record);
				} else {
					Vector<Vector<Object>> data = new Vector<Vector<Object>>();
					data.add(record);
					quotationBasisDataMap.put(id, data);
				}
			}
			if (summaryDTO != null) {
				summaryDTO.setQuotationVector(quotationData);
				summaryDTO.setMap(quotationBasisDataMap);
			}

			// 製作明細おそい
			ps = c.prepareStatement("SELECT * FROM T_製作_子 WHERE 製作親ID=?");
			ps.setInt(1, productionID);
			rs = ps.executeQuery();
			while (rs.next()) {
				Vector<Object> record = new Vector<Object>();
				record.add(rs.getInt("ID"));
				record.add(rs.getInt("表示CD"));
				record.add(rs.getString("名称"));
				record.add(rs.getBoolean("各FLG"));
				record.add(rs.getInt("数量"));
				record.add(rs.getInt("数量単位CD"));
				record.add(rs.getInt("単価"));
				record.add(rs.getInt("金額"));
				record.add(rs.getString("図番"));
				record.add(rs.getString("備考"));
				record.add(rs.getDate("完成年月日"));
				record.add(rs.getDate("完成年月日") != null);
				record.add(rs.getInt("表示CD") == 2 ? rs.getDate("納品年月日") : null);
				record.add(rs.getDate("納品年月日") != null);
				productionData.add(record);
			}
			if (summaryDTO != null)
				summaryDTO.setProductionVector(productionData);

			// 売伝一覧
			ps = c.prepareStatement(
				"SELECT sp.売上親ID,得意先CD,売上年月日,売上FLG,請求FLG,納品区分CD,納品手段CD,摘要 FROM T_売上_親 sp"
					+ " LEFT OUTER JOIN (SELECT DISTINCT 売上親ID,製作親ID FROM T_売上_子) sc ON sp.売上親ID=sc.売上親ID"
					+ " WHERE 製作親ID=?"
			);
			ps.setInt(1, productionID);
			rs = ps.executeQuery();
			while (rs.next()) {
				int type = 0;
				if (!rs.getBoolean("売上FLG")) {
					type = 2;
				} else if (!rs.getBoolean("請求FLG")) {
					type = 1;
				}
				Vector<Object> record = new Vector<Object>();
				record.add(rs.getInt("売上親ID"));
				record.add(type);
				record.add(rs.getDate("売上年月日"));
				record.add(rs.getInt("納品区分CD"));
				record.add(rs.getInt("納品手段CD"));
				record.add(rs.getString("摘要"));
				salesSlips.add(record);
			}
			if (summaryDTO != null)
				summaryDTO.setSalesSlips(salesSlips);

			if (productionID > 0) {
				ps = c.prepareStatement(
					"SELECT convert(varchar,見積期)+'-'+right('000' + convert(varchar, 見積番号), 3)+見積枝番 AS 見積番号 FROM T_見積製作 ep"
						+ " LEFT OUTER JOIN T_見積_親 e ON ep.見積親ID=e.見積親ID"
						+ " WHERE 製作親ID=?"
				);
				ps.setInt(1, productionID);
				rs = ps.executeQuery();
				List<String> quoteNumbers = new ArrayList<String>();
				while (rs.next()) {
					quoteNumbers.add(rs.getString("見積番号"));
				}
				if (summaryDTO != null)
					summaryDTO.setQuotationNumbers(quoteNumbers);

				// カルテ履歴
				// 見積の履歴はすでに入っており、製作データがあるときのみ、より詳細なデータを取得
				ps = c.prepareStatement(
					"SELECT 機械番号 FROM T_カルテ履歴 h LEFT OUTER JOIN T_製作_親 p ON h.元製作親ID=p.製作親ID"
						+ " WHERE h.製作親ID=? AND p.製作親ID > 0"
				);
				ps.setInt(1, productionID);
				rs = ps.executeQuery();
				List<Integer> parents = new ArrayList<Integer>();
				while (rs.next()) {
					parents.add(rs.getInt("機械番号"));
				}
				if (summaryDTO != null)
					summaryDTO.setParents(parents);
			}
		} catch (SQLException ex) {
			err.append(ex.toString());
			Logging.logStackTrace(ex, lg, className);
		}

		/**
		 * クライアントに送信
		 */
		try {
			response.setContentType("application/octet-stream");
			ObjectOutputStream out = new ObjectOutputStream(response.getOutputStream());
			out.writeObject(summaryDTO);
			out.writeUTF(err.toString());
			out.flush();
			out.close();
		} catch (Exception ex) {
			Logging.logStackTrace(ex, lg, className);
		} finally {
			try {
				if (c != null && !c.isClosed())
					c.close();
			} catch (SQLException ex) {
				Logging.logStackTrace(ex, lg, className);
			}
			// The following processes requires JDBC4.0.
			try {
				if (ps != null && !ps.isClosed()) {
					ps.close();
					lg.debug(className + "ps is closed by jdbc4.0");
				}
			} catch (SQLException ex) {
				Logging.logStackTrace(ex, lg, className);
			}
			try {
				if (rs != null && !rs.isClosed()) {
					rs.close();
					lg.debug(className + "rs is closed by jdbc4.0");
				}
			} catch (SQLException ex) {
				Logging.logStackTrace(ex, lg, className);
			}
		}
	}

}
