package fukaisystem;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

import javax.servlet.GenericServlet;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import org.apache.log4j.Logger;

import fukaisystem.dto.IDDTO;
import fukaisystem.dto.ProjectSearchDTO;
import fukaisystem.sql.DBConnection;
import fukaisystem.util.Logging;



public class Search extends GenericServlet {

	private static final long serialVersionUID = 1L;
	private static final Logger lg = Logger.getLogger("A1");
	private static final String className = "Search\n";

	@Override
	public void service(ServletRequest request, ServletResponse response) {
		DBConnection dbc = new DBConnection();
		Connection c = dbc.getConnection();
		PreparedStatement ps = null;
		ResultSet rs = null;
		ProjectSearchDTO searchDTO = null;
		StringBuilder err = new StringBuilder();
		List<Integer> strIndex = new ArrayList<Integer>();
		List<Integer> intIndex = new ArrayList<Integer>();

		Vector<Vector<Object>> v = new Vector<Vector<Object>>();

		String[] constStrs = {"(e.案件名 LIKE ? OR p.案件名 LIKE ?)", "(e.誕生枝番=? OR p.誕生枝番=?)", "見積枝番=?",
				"SUBSTRING(CONVERT(VARCHAR, 見積年月日),1,4) like ?", "SUBSTRING(CONVERT(VARCHAR, 見積年月日),6,2) like ?", "SUBSTRING(CONVERT(VARCHAR, 見積年月日),9,2) like ?",
				 "製作枝番=?", "受注番号=?",
				"SUBSTRING(CONVERT(VARCHAR, 受注年月日),1,4) like ?", "SUBSTRING(CONVERT(VARCHAR, 受注年月日),6,2) like ?", "SUBSTRING(CONVERT(VARCHAR, 受注年月日),9,2) like ?",
				"SUBSTRING(CONVERT(VARCHAR, 発行年月日),1,4) like ?", "SUBSTRING(CONVERT(VARCHAR, 発行年月日),6,2) like ?", "SUBSTRING(CONVERT(VARCHAR, 発行年月日),9,2) like ?",
				"SUBSTRING(CONVERT(VARCHAR, p.納期),1,4) like ?", "SUBSTRING(CONVERT(VARCHAR, p.納期),6,2) like ?", "SUBSTRING(CONVERT(VARCHAR, p.納期),9,2) like ?",
				"SUBSTRING(CONVERT(VARCHAR, 出荷年月日),1,4) like ?", "SUBSTRING(CONVERT(VARCHAR, 出荷年月日),6,2) like ?", "SUBSTRING(CONVERT(VARCHAR, 出荷年月日),9,2) like ?",
				"SUBSTRING(CONVERT(VARCHAR, 検収年月日),1,4) like ?", "SUBSTRING(CONVERT(VARCHAR, 検収年月日),6,2) like ?", "SUBSTRING(CONVERT(VARCHAR, 検収年月日),9,2) like ?",
				"SUBSTRING(CONVERT(VARCHAR, 売上年月日),1,4) like ?", "SUBSTRING(CONVERT(VARCHAR, 売上年月日),6,2) like ?", "SUBSTRING(CONVERT(VARCHAR, 売上年月日),9,2) like ?",
				};
		String[] constInts = {"製作期=?", "製作番号=?", "c.得意先CD=?", "見積期=?", "見積番号=?",
				"((e.購入者CD=? AND e.種類=?) OR (p.購入者CD=? AND p.種類=?))", "(e.通貨CD=? OR p.通貨CD=?)", "(見積金額=? OR 契約金額=?)", "(e.誕生期=? OR p.誕生期=?)", "(e.誕生番号=? OR p.誕生番号=?)"};
		try {

			/**
			 * クライアントデータ受け取り
			 */
			ObjectInputStream in = new ObjectInputStream(request.getInputStream());
			Object obj = in.readObject();
			in.close();

			if(obj == null) {
				err.append(className + "readObjectがnullです\n");
				lg.error(className + "readObjectがnullです");
			} else {
				if(obj instanceof ProjectSearchDTO) {
					searchDTO = (ProjectSearchDTO)obj;
				} else {
					err.append(className + "readObjectがProjectSearchDTO型ではありません\n");
					lg.error(className + "readObjectがProjectSearchDTO型ではありません");
				}
			}
			try {
				int deliveryCode = searchDTO.getInt(10);
				boolean isFull = true;
				boolean isFirst = true;
				StringBuilder condition = new StringBuilder("");

				if(!searchDTO.getStr(0).equals("")) {//納入先名に検索条件が入っていれば
					isFirst = false;
					condition.append("p.納入先名 LIKE ?");
					strIndex.add(0);
				}
				//文字列の検索条件は、searchDTO.getStr(1～27)
				for(int i = 2; i < 28; i++) {
					if(!searchDTO.getStr(i).equals("")) {//検索条件が入っていれば
						if(i > 7) isFull = false;//製作関連の検索条件があれば、無条件で見積もりデータを結合しない
						strIndex.add(i);
						if(isFirst) {
							isFirst = false;
						} else {
							if(searchDTO.isAnd()) condition.append(" AND ");
							else condition.append(" OR ");
						}
						if(searchDTO.getStr(i).equals("未")) {
							switch(i) {
								case 5 :
									condition.append("(提出年月日 IS NULL AND 見積年月日 IS NOT NULL)"); break;
								case 10 :
									condition.append("(受注年月日 IS NULL AND (提出年月日 IS NOT NULL OR 発行年月日 IS NOT NULL))"); break;
								case 13 :
									condition.append("(発行年月日 IS NULL AND 提出年月日 IS NOT NULL)"); break;
								case 16 :
									condition.append("(納期 IS NULL AND 発行年月日 IS NOT NULL)"); break;
								case 19 :
									condition.append("(出荷年月日 IS NULL AND 発行年月日 IS NOT NULL)"); break;
								case 22 :
									condition.append("(検収年月日 IS NULL AND 発行年月日 IS NOT NULL)"); break;
								case 25 :
									condition.append("(売上年月日 IS NULL AND 売上年月日 IS NOT NULL)"); break;
							}
						} else {
							condition.append(constStrs[i - 2]);
						}
					}
				}
/*
				String completeStr = "";
				if(searchDTO.getStr(25).equals("未")) {//完成年月日が未
					completeStr = " AND 完成年月日 IS NULL";
					isFull = false;

				} else {//年月日いづれか
					if(!searchDTO.getStr(25).equals("")) {
						completeStr = " AND SUBSTRING(CONVERT(VARCHAR, 完成年月日),1,4) like ?";
						isFull = false;
					}
					if(!searchDTO.getStr(26).equals("")) {
						completeStr = " AND SUBSTRING(CONVERT(VARCHAR, 完成年月日),6,2) like ?";
						isFull = false;
					}
					if(!searchDTO.getStr(27).equals("")) {
						completeStr = " AND SUBSTRING(CONVERT(VARCHAR, 完成年月日),9,2) like ?";
						isFull = false;
					}
				}
*/
				//数値の検索条件は、searchDTO.getInt(0～10)
				for(int i = 0; i < 10; i++) {//10:deliveryCodeを除く
					if(searchDTO.getInt(i) != 0) {//検索条件が入っていれば
						if(i < 2) isFull = false;//製作関連の検索条件があれば、無条件で見積もりデータを結合しない
						intIndex.add(i);
						if(isFirst) {
							isFirst = false;
						} else {
							if(searchDTO.isAnd()) condition.append(" AND ");
							else condition.append(" OR ");
						}
						//金額欄が0以外の場合
						if(i == 7 && searchDTO.getStr(1).equals("以上")) {
							condition.append("(見積金額>=? OR 契約金額>=? OR 売上合計>=?)");
						} else if(i == 7 && searchDTO.getStr(1).equals("以下")) {
							condition.append("((見積金額>0 AND 見積金額<=?) OR (契約金額>0 AND 契約金額<=?) OR (売上合計>0 AND 売上合計<=?))");
						} else if(searchDTO.getInt(1) % 1000 == 0 && i == 1) {//製番で検索
							condition.append("FLOOR(製作番号/1000)*1000=?");
						} else {
							//製番が0で終わる→○○台　を追加
							condition.append(constInts[i]);
						}
					}
				}
				if(deliveryCode == 1) {//納品が未のデータの検索は、製伝が発行されたものに限る
					if(isFirst) {
						isFirst = false;
					} else {
						condition.append(" AND ");
					}
					condition.append("発行年月日 IS NOT NULL");
				}

				StringBuilder query = new StringBuilder(
				 "SELECT top 30000 " + "\n" +
					"e.見積親ID," + "\n" +
					"main.製作親ID," + "\n" +
					"s.売上ID," + "\n" +
					"convert(varchar, 見積期) AS 見積期," + "\n" +
					"見積番号," + "\n" +
					"CASE WHEN e.見積枝番 IS NULL THEN '' ELSE e.見積枝番 END AS 見積枝番," + "\n" +
					"CASE WHEN p.得意先CD=0 OR p.得意先CD IS NULL THEN e.案件名 ELSE p.案件名 END AS 案件名," + "\n" +
					"CASE WHEN p.得意先CD=0 OR p.得意先CD IS NULL THEN e.種類 ELSE p.種類 END AS 種類," + "\n" +
					"CASE WHEN p.得意先CD=0 OR p.得意先CD IS NULL THEN" + "\n" +
						" right('00' + convert(varchar, e.誕生期), 2)" + "\n" +
						" ELSE" + "\n" +
						" right('00' + convert(varchar, p.誕生期), 2)" + "\n" +
						" END AS 誕生期," + "\n" +
					"CASE WHEN p.得意先CD=0 OR p.得意先CD IS NULL THEN e.誕生番号 ELSE p.誕生番号 END AS 誕生番号," + "\n" +
					"CASE WHEN p.得意先CD=0 OR p.得意先CD IS NULL THEN" + "\n" +
						" CASE WHEN e.誕生枝番 IS NULL THEN '' ELSE e.誕生枝番 END" + "\n" +
						" ELSE p.誕生枝番 END AS 誕生枝番," + "\n" +
					"CASE WHEN p.得意先CD=0 OR p.得意先CD IS NULL THEN e.得意先CD ELSE p.得意先CD END AS 得意先CD," + "\n" +
					"CASE" + "\n" +
						" WHEN 種別CD = 1 THEN '㈱'+会社名 + CASE WHEN 支店名 IS NULL THEN '' ELSE ' ' + 支店名 END" + "\n" +
						" WHEN 種別CD = 2 THEN 会社名+'㈱' + CASE WHEN 支店名 IS NULL THEN '' ELSE ' ' + 支店名 END" + "\n" +
						" WHEN 種別CD = 3 THEN '㈲'+会社名 + CASE WHEN 支店名 IS NULL THEN '' ELSE ' ' + 支店名 END" + "\n" +
						" WHEN 種別CD = 4 THEN 会社名+'㈲' + CASE WHEN 支店名 IS NULL THEN '' ELSE ' ' + 支店名 END" + "\n" +
						" ELSE 会社名 END AS 社名," + "\n" +
					"e.見積年月日," + "\n" +
					"e.提出年月日," + "\n" +
					"e.見積金額," + "\n" +
					"p.受注番号," + "\n" +
					"p.受注年月日," + "\n" +
					"p.納期," + "\n" +
					"p.納入先名," + "\n" +
					"right('00' + convert(varchar, p.製作期), 2) AS 製作期," + "\n" +
					"p.製作番号," + "\n" +
					"CASE WHEN p.製作枝番 IS NULL THEN '' ELSE p.製作枝番 END AS 製作枝番," + "\n" +
					"p.発行年月日 AS 製作年月日," + "\n" +
					"p.契約金額," + "\n" +
					"p.出荷年月日," + "\n" +
					"p.検収年月日," + "\n" +
					"売上合計" + "\n" +
				" FROM (SELECT 見積親ID,sub.製作親ID from (" +
				"SELECT MIN(mp.製作親ID) AS 製作親ID" + "\n" +
				" FROM T_製作_親 mp" + "\n");

				switch(deliveryCode) {
					case 0 ://指定しない
						query.append(
						" GROUP BY mp.製作親ID) sub" + "\n" +
						" left outer join T_見積製作 mapping on sub.製作親ID=mapping.製作親ID) main" + "\n" +
						((isFull || !searchDTO.isAnd()) ? " FULL" : "LEFT")); break;
					case 1 ://未
						query.append(
						" RIGHT OUTER JOIN (" + "\n" +
						"	SELECT 製作親ID FROM T_製作_親 m where not exists(" + "\n" +
						"	select * from T_売上_親 s1 left outer join T_売上_子 s2 on s2.売上親ID=s1.売上親ID where m.製作親ID=s2.製作親ID and (納品区分CD=2 or 納品区分CD=4 or 納品区分CD=5))" + "\n" +
						" ) smp on mp.製作親ID=smp.製作親ID group by mp.製作親ID) sub" + "\n" +
						" left outer join T_見積製作 mapping on sub.製作親ID=mapping.製作親ID) main" + "\n" +
						" LEFT"); break;
					default ://何らかの納品
						query.append(
						" RIGHT OUTER JOIN (SELECT * FROM T_製作_子 WHERE 表示CD=2) mc ON mp.製作親ID=mc.製作親ID" + "\n" +
						" WHERE EXISTS(" + "\n" +
						"  SELECT * FROM T_売上_子 msc" + "\n" +
						"  LEFT OUTER JOIN T_売上_親 msp ON msc.売上親ID=msp.売上親ID" + "\n" +
						"  WHERE mc.ID=msc.製作子ID AND mc.製作親ID=msc.製作親ID AND 納品区分CD=?" + "\n" +
						" )" + "\n" +
						" GROUP BY mp.製作親ID) sub" + "\n" +
						" left outer join T_見積製作 mapping on sub.製作親ID=mapping.製作親ID) main" + "\n" +
						" LEFT"); break;
				}

				query.append(
				" OUTER JOIN (" + "\n" +
					"SELECT " + "\n" +
						"ee.見積親ID, ee.見積期, ee.見積番号, ee.見積枝番, ee.案件名, ee.得意先CD, ee.納期CD, ee.受渡場所CD, ee.取引条件CD, " + "\n" +
						"ee.有効期間CD, ee.提出済CD, ee.見積年月日, ee.提出年月日, ee.通貨CD, ee.見積金額, ee.摘要, ee.更新日, ee.更新者CD, " + "\n" +
						"eo.製作親ID AS 元製作親ID, eo.製作期 AS 誕生期, eo.製作番号 AS 誕生番号, eo.製作枝番 AS 誕生枝番, eo.得意先CD AS 購入者CD, eo.機械番号 AS 種類" + "\n" +
					" FROM T_見積_親 ee" + "\n" +
					" LEFT OUTER JOIN T_製作_親 eo" + "\n" +
					" ON ee.元製作親ID=eo.製作親ID" + "\n" +
				") e ON main.見積親ID=e.見積親ID" + "\n" +
				 " LEFT OUTER JOIN (" + "\n" +
				 	"SELECT" + "\n" +
				 		" pp.製作親ID, pp.製作期, pp.製作番号, pp.製作枝番, pp.受注番号, pp.案件名, pp.見積親ID, pp.得意先CD," + "\n" +
				 		" pp.機械番号, pp.納入先名, pp.納期, pp.受注年月日, pp.発行年月日, pp.出荷年月日, pp.検収年月日," + "\n" +
				 		" pp.通貨CD, pp.契約金額, pp.摘要, pp.出図FLG, pp.手配FLG, pp.更新日, pp.更新者CD," + "\n" +
				 		" po.製作期 AS 誕生期, po.製作番号 AS 誕生番号, po.製作枝番 AS 誕生枝番, po.得意先CD AS 購入者CD, po.機械番号 AS 種類" + "\n" +
					" FROM T_製作_親 pp" + "\n" +
					" LEFT OUTER JOIN T_カルテ履歴 h" + "\n" +
					" ON pp.製作親ID=h.製作親ID" + "\n" +
					" LEFT OUTER JOIN T_製作_親 po" + "\n" +
					" ON h.元製作親ID=po.製作親ID" + "\n" +
				") p ON main.製作親ID=p.製作親ID" + "\n" +
				" LEFT OUTER JOIN (" + "\n" +
					"SELECT" + "\n" +
						" MIN(sp.売上親ID) AS 売上ID,製作親ID,SUM(金額) AS 売上合計,売上年月日" + "\n" +
					" FROM T_売上_子 sc" +
					" LEFT OUTER JOIN T_売上_親 sp ON sc.売上親ID=sp.売上親ID" + "\n" +
					" GROUP BY 製作親ID,売上年月日" + "\n" +
				") s ON p.製作親ID=s.製作親ID" + "\n" +
				" LEFT OUTER JOIN M_法人 c" + "\n" +
				 " ON (CASE WHEN p.得意先CD=0 OR p.得意先CD IS NULL THEN e.得意先CD ELSE p.得意先CD END) =c.得意先CD" + "\n");

				if(searchDTO.getInt(3) != 0 || searchDTO.getInt(4) != 0 || !searchDTO.getStr(4).equals("")) {
					//見積番号（の一部）を指定
					condition.append(" ORDER BY 見積期 DESC,見積番号 DESC,見積枝番 DESC");
				} else if(searchDTO.getInt(0) != 0 || searchDTO.getInt(1) != 0 || !searchDTO.getStr(8).equals("")) {
					//製作番号（の一部）を指定
					condition.append(" ORDER BY 製作期 DESC,製作番号 DESC,製作枝番 DESC");
				} else if(searchDTO.getInt(8) != 0 || searchDTO.getInt(9) != 0 || !searchDTO.getStr(3).equals("")) {
					//誕生番号（の一部）を指定
					condition.append(" ORDER BY 誕生期 DESC,誕生番号 DESC,誕生枝番 DESC");
				}
				String conditionStr = condition.toString();
				if(!conditionStr.equals("")) query.append(" WHERE " + conditionStr);
				ps = c.prepareStatement(query.toString());

				//////////////////////条件のセット
				int j = 1;
				/*
				if(!searchDTO.getStr(25).equals("未")) {//完成年月日が未
					if(!searchDTO.getStr(25).equals("")) {//完成年が入力されていれば
						ps.setString(j, searchDTO.getStr(25)); j++;
					}
					if(!searchDTO.getStr(26).equals("")) {//完成月が入力されていれば
						ps.setString(j, searchDTO.getStr(26)); j++;
					}
					if(!searchDTO.getStr(27).equals("")) {//完成日が入力されていれば
						ps.setString(j, searchDTO.getStr(27)); j++;
					}
				}
				*/
				if(deliveryCode > 1) {//何らかの納品がされていれば
					ps.setInt(j, deliveryCode);
					j++;
				}
				for(int i : strIndex) {
					if(i == 0) {
						//案件名は1つの検索条件でe.案件名とp.案件名を対象にする
						ps.setString(j, "%" + searchDTO.getStr(i) + "%"); j++;					
					} else if(i == 2) {
						//案件名は1つの検索条件でe.案件名とp.案件名を対象にする
						ps.setString(j, "%" + searchDTO.getStr(i) + "%"); j++;
						ps.setString(j, "%" + searchDTO.getStr(i) + "%"); j++;
					} else if(i == 3) {
						//誕生枝番はeとp両方の誕生
						ps.setString(j, searchDTO.getStr(i)); j++;
						ps.setString(j, searchDTO.getStr(i)); j++;
					} else if(!searchDTO.getStr(i).equals("未")) {
						ps.setString(j, searchDTO.getStr(i)); j++;
					}
				}
				for(int i : intIndex) {
					if(i == 5) {//2条件×eとpの2本立て＝4つ
						ps.setInt(j, searchDTO.getInt(11)); j++;//購入者CD
						ps.setInt(j, searchDTO.getInt(5)); j++;//機種
						ps.setInt(j, searchDTO.getInt(11)); j++;
						ps.setInt(j, searchDTO.getInt(5)); j++;
					} else if(i == 7) {//eとpとsの3本立て
						ps.setInt(j, searchDTO.getInt(i)); j++;
						ps.setInt(j, searchDTO.getInt(i)); j++;
						ps.setInt(j, searchDTO.getInt(i)); j++;
					} else if(i > 5) {//eとpの2本立て
						ps.setInt(j, searchDTO.getInt(i)); j++;
						ps.setInt(j, searchDTO.getInt(i)); j++;
					} else {
						ps.setInt(j, searchDTO.getInt(i)); j++;
					}
				}
				rs = ps.executeQuery();
				while(rs.next()) {
					Vector<Object> v2 = new Vector<Object>();
					v2.add(new IDDTO(rs.getInt("見積親ID"), rs.getInt("製作親ID"), rs.getInt("売上ID")));
					int estNum = rs.getInt("見積番号");
					if(estNum != 0) {
						if(estNum < 10) {
							v2.add(rs.getString("見積期") + "-00" + rs.getString("見積番号") + " " + rs.getString("見積枝番"));
						} else if(estNum < 100) {
							v2.add(rs.getString("見積期") + "-0" + rs.getString("見積番号") + " " + rs.getString("見積枝番"));
						} else {
							v2.add(rs.getString("見積期") + "-" + rs.getString("見積番号") + " " + rs.getString("見積枝番"));
						}
					} else {
						v2.add("");
					}
					if(rs.getInt("製作番号") != 0) {
						v2.add(rs.getString("製作期") + "-" + rs.getInt("製作番号") + " " + rs.getString("製作枝番"));
					} else {
						v2.add("");
					}
					//v2.add(rs.getInt("種類"));
					if(rs.getInt("誕生番号") != 0) {
						v2.add(rs.getString("誕生期") + "-" + rs.getInt("誕生番号") + " " + rs.getString("誕生枝番"));
					} else {
						v2.add("");
					}

					if(rs.getInt("得意先CD") != 0) {
						v2.add(/*rs.getInt("得意先CD") + "：" + */rs.getString("社名"));
					} else {
						v2.add("");
					}
					v2.add(rs.getString("納入先名"));
					v2.add(rs.getString("案件名"));

					v2.add(rs.getDate("見積年月日"));
					v2.add(rs.getInt("見積金額"));
					v2.add(rs.getString("受注番号"));
					v2.add(rs.getDate("受注年月日"));
					v2.add(rs.getDate("製作年月日"));
					v2.add(rs.getDate("納期"));
					v2.add(rs.getInt("売上合計"));
					v2.add(rs.getDate("出荷年月日"));
					v2.add(rs.getDate("検収年月日"));

					v.add(v2);
				}

			} catch(SQLException ex) {
				err.append(className + "テーブル「T_見積_親」の読み出しに失敗しました\n");
				Logging.logStackTrace(ex, lg, className);
			}
		}catch(Exception ex) {
			Logging.logStackTrace(ex, lg, className);
		}

		/**
		 * クライアントに送信
		 */
		try {
			response.setContentType("application/octet-stream");
			ObjectOutputStream out = new ObjectOutputStream(response.getOutputStream());
			out.writeObject(v);
			out.writeUTF(err.toString());
			out.flush();
			out.close();
		}catch(Exception ex) {
			lg.error(ex);
		} finally {
			try {
				if(c != null && !c.isClosed()) c.close();
			} catch(SQLException ex) {
				Logging.logStackTrace(ex, lg, className);
			}
// The following processes requires JDBC4.0.
			try {
				if(ps != null && !ps.isClosed()) {
					ps.close();
					lg.debug(className + "ps is closed by jdbc4.0");
				}
			} catch(SQLException ex) {
				Logging.logStackTrace(ex, lg, className);
			}
			try {
				if(rs != null && !rs.isClosed()) {
					rs.close();
					lg.debug(className + "rs is closed by jdbc4.0");
				}
			} catch(SQLException ex) {
				Logging.logStackTrace(ex, lg, className);
			}
		}
	}


	public String partialDateStr(String target, String ymd, int begin, int count) {
		switch(begin) {
			case 1 : return "substring(convert(varchar(" + count + "), " + target + ", 120), " + begin + ", " + count + ")='" + ymd + "'";
			case 6 : return "substring(convert(varchar(" + (count + 5) + "), " + target + ", 120), " + begin + ", " + count + ")='" + ymd + "'";
			default : return "substring(convert(varchar(10), " + target + ", 120), " + begin + ", " + count + ")='" + ymd + "'";
		}
	}
}
