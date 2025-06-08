package fukaisystem;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

import javax.servlet.GenericServlet;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;



import org.apache.log4j.Logger;

import fukaisystem.dto.ProjectSummaryDTO;
import fukaisystem.sql.DBConnection;
import fukaisystem.util.Logging;


public class EstimateRegistration extends GenericServlet {


	private static final long serialVersionUID = 1L;
    private static final Logger lg = Logger.getLogger("A1");
	private static final String className = "EstimateRegistration\n";

	public void service(ServletRequest request, ServletResponse response) {

		DBConnection dbc = new DBConnection();
		Connection c = dbc.getConnection();
		Statement st = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		boolean isError = false;
		ProjectSummaryDTO summaryDTO = null;
		StringBuilder err = new StringBuilder();
		List<Integer> keys = new ArrayList<Integer>();
		int estimateID = 0;
		int productID = 0;
		int estimatePer = 0;
		int estimateNum = 0;

		try {

			/**
			 * クライアントデータ受け取り
			 */
			ObjectInputStream in = new ObjectInputStream(request.getInputStream());
			Object obj = in.readObject();
			in.close();

			if(obj == null) {
				isError = true;
				err.append(className + "readObjectがnullです\n");
				lg.error(className + "readObjectがnullです");
			} else {
				if(obj instanceof ProjectSummaryDTO) {
					summaryDTO = (ProjectSummaryDTO)obj;
				} else {
					isError = true;
					err.append(className + "readObjectがProjectSummaryDTO型ではありません\n");
					lg.error(className + "readObjectがProjectSummaryDTO型ではありません");
				}
			}
			try {
				//コミット後でありさえすればよい。
				//コミット後ということは、子・孫ともども更新されているはずで、問題は生じない
				//親→子→孫（加工→材料）の順が守られている限り、デッドロックは発生しないはず
				st = c.createStatement();
				st.executeUpdate("SET TRANSACTION ISOLATION LEVEL READ COMMITTED");
				st.executeUpdate("BEGIN TRANSACTION");
			} catch(SQLException ex) {
				isError = true;
				err.append(className + "トランザクションの開始に失敗しました\n");
				Logging.logStackTrace(ex, lg, className);
			}

			estimatePer = summaryDTO.getInt(3);
			estimateNum = summaryDTO.getInt(4);
			estimateID = summaryDTO.getInt(16);
			productID = summaryDTO.getInt(17);

			if(estimatePer == 0 && estimateNum == 0 && estimateID != 0) {
				//見積IDがあるデータの見積期と見積番号を0に変更したということは、消去せよということ
				try {
					ps = c.prepareStatement("" +
							"DELETE FROM T_見積_親 WHERE 見積親ID=?;" +
							"DELETE FROM T_見積_子 WHERE 見積親ID=?;" +
							"DELETE FROM T_見積_材料 WHERE 見積親ID=?;" +
							"DELETE FROM T_見積_加工 WHERE 見積親ID=?;" +
							"UPDATE T_製作_親 SET 見積親ID=0 WHERE 見積親ID=?;" +
							"DELETE FROM T_見積製作 WHERE 見積親ID=?");
					ps.setInt(1, estimateID);
					ps.setInt(2, estimateID);
					ps.setInt(3, estimateID);
					ps.setInt(4, estimateID);
					ps.setInt(5, estimateID);
					ps.setInt(6, estimateID);
					ps.executeUpdate();
					estimateID = 0;
				} catch(SQLException ex) {
					isError = true;
					err.append(className + "見積テーブルの削除に失敗しました\n");
					Logging.logStackTrace(ex, lg, className);
				}
			} else {
				//納期
				int deadline = 0;
				int place = 0;
				int terms = 0;
				int validity = 0;
				try {
					ps = c.prepareStatement(
					"MERGE INTO M_有効期間 AS v" +
					" USING (SELECT ? AS 有効期間) AS w" +
					" ON replace(replace(v.有効期間,' ',''),'　','')=replace(replace(w.有効期間,' ',''),'　','')" +
					" WHEN MATCHED THEN" +
					"	UPDATE SET v.有効期間=v.有効期間" +
					" WHEN NOT MATCHED THEN" +
					"	INSERT VALUES(w.有効期間)" +
					" OUTPUT deleted.CD as oldId, inserted.CD as newId;");
					ps.setString(1, summaryDTO.getStr(8));
					boolean isResultSet = ps.execute();
					int   updateCount = 0;
					while (true) {
					   if (isResultSet) {
					         rs = ps.getResultSet();
					         while (rs.next()) {
					        	 validity = rs.getInt(2);
					         }
					         rs.close();
					   }
					   else {
					         updateCount = ps.getUpdateCount();
					         if (updateCount == -1) {
					            break;
					         }
					   }
					   isResultSet = ps.getMoreResults();
					}
					ps = c.prepareStatement(
					"MERGE INTO M_納期 AS v" +
					" USING (SELECT ? AS 納期) AS w" +
					" ON replace(replace(v.納期,' ',''),'　','')=replace(replace(w.納期,' ',''),'　','')" +
					" WHEN MATCHED THEN" +
					"	UPDATE SET v.納期=v.納期" +
					" WHEN NOT MATCHED THEN" +
					"	INSERT VALUES(w.納期)" +
					" OUTPUT deleted.CD as oldId, inserted.CD as newId;");
					ps.setString(1, summaryDTO.getStr(5));
					isResultSet = ps.execute();
					updateCount = 0;
					while (true) {
					   if (isResultSet) {
					         rs = ps.getResultSet();
					         while (rs.next()) {
					        	 deadline = rs.getInt(2);
					         }
					         rs.close();
					   }
					   else {
					         updateCount = ps.getUpdateCount();
					         if (updateCount == -1) {
					            break;
					         }
					   }
					   isResultSet = ps.getMoreResults();
					}
					ps = c.prepareStatement(
					"MERGE INTO M_受渡場所 AS v" +
					" USING (SELECT ? AS 受渡場所) AS w" +
					" ON replace(replace(v.受渡場所,' ',''),'　','')=replace(replace(w.受渡場所,' ',''),'　','')" +
					" WHEN MATCHED THEN" +
					"	UPDATE SET v.受渡場所=v.受渡場所" +
					" WHEN NOT MATCHED THEN" +
					"	INSERT VALUES(w.受渡場所)" +
					" OUTPUT deleted.CD as oldId, inserted.CD as newId;");
					ps.setString(1, summaryDTO.getStr(6));
					isResultSet = ps.execute();
					updateCount = 0;
					while (true) {
					   if (isResultSet) {
					         rs = ps.getResultSet();
					         while (rs.next()) {
					        	 place = rs.getInt(2);
					         }
					         rs.close();
					   }
					   else {
					         updateCount = ps.getUpdateCount();
					         if (updateCount == -1) {
					            break;
					         }
					   }
					   isResultSet = ps.getMoreResults();
					}
					ps = c.prepareStatement(
					"MERGE INTO M_取引条件 AS v" +
					" USING (SELECT ? AS 取引条件) AS w" +
					" ON replace(replace(v.取引条件,' ',''),'　','')=replace(replace(w.取引条件,' ',''),'　','')" +
					" WHEN MATCHED THEN" +
					"	UPDATE SET v.取引条件=v.取引条件" +
					" WHEN NOT MATCHED THEN" +
					"	INSERT VALUES(w.取引条件)" +
					" OUTPUT deleted.CD as oldId, inserted.CD as newId;");
					ps.setString(1, summaryDTO.getStr(7));
					isResultSet = ps.execute();
					updateCount = 0;
					while (true) {
					   if (isResultSet) {
					         rs = ps.getResultSet();
					         while (rs.next()) {
					        	 terms = rs.getInt(2);
					         }
					         rs.close();
					   }
					   else {
					         updateCount = ps.getUpdateCount();
					         if (updateCount == -1) {
					            break;
					         }
					   }
					   isResultSet = ps.getMoreResults();
					}
				} catch(SQLException ex) {
					isError = true;
					ex.printStackTrace();
					err.append(className + "テーブル「T_見積_親」の更新に失敗しました\n");
					Logging.logStackTrace(ex, lg, className);
				}
				if(estimateNum == 0) {
					try {
						ps = c.prepareStatement("SELECT MAX(見積番号) AS 最終見積番号 FROM T_見積_親 WHERE 見積期=?");
						ps.setInt(1, estimatePer);//見積期
						rs = ps.executeQuery();
						if(rs.next()) {
							estimateNum = rs.getInt("最終見積番号") + 1;
						}
					} catch(SQLException ex) {
						ex.printStackTrace();
						isError = true;
						err.append(className + "見積番号の読み込みに失敗しました\n");
						Logging.logStackTrace(ex, lg, className);
					}
				} else {
					try {
						ps = c.prepareStatement("SELECT 見積親ID FROM T_見積_親 WHERE 見積期=? AND 見積番号=? AND 見積枝番=?");
						int i = 1;
						ps.setInt(i, estimatePer); i++;//見積期
						ps.setInt(i, estimateNum); i++;//見積番号
						ps.setString(i, summaryDTO.getStr(4)); i++;//見積枝番
						rs = ps.executeQuery();
						if(rs.next()) {
							if(rs.getInt("見積親ID") != 0) {
								estimateID = rs.getInt("見積親ID");
							}
						}
					} catch(SQLException ex) {
						ex.printStackTrace();
						isError = true;
						err.append(className + "見積親IDの読み込みに失敗しました\n");
						Logging.logStackTrace(ex, lg, className);
					}
				}
				if(estimateID == 0) {//見積書新規作成
					try {
						ps = c.prepareStatement(
								"INSERT INTO T_見積_親" +
								" (見積期, 見積番号, 見積枝番, 元製作親ID, 案件名, 案内文, 得意先CD, 得意先表示名," +
								" 納期CD, 受渡場所CD, 取引条件CD, 有効期間CD, 提出済CD, 見積年月日, 提出年月日," +
								" 通貨CD, 見積金額, 摘要, 更新日, 更新者CD)" +
								" OUTPUT inserted.見積親ID as newId, inserted.更新日" +
								" SELECT" +
								" ?, ?, ?," +
								" CASE WHEN (?=0 AND ?=0 AND ?='' AND ?=0)" +
								"  THEN 0" +//種類、誕生製番がすべて空なら自身が新機となるため、親なしとして登録
								"  ELSE (SELECT CASE WHEN MIN(製作親ID) IS NULL THEN 0 ELSE MIN(製作親ID) END FROM T_製作_親" +
								" WHERE (得意先CD=? AND 機械番号=?) OR (製作期=? AND 製作番号=? AND 製作枝番=?)) END," +
								" ?, ?, ?, ?, ?, ?, ?, ?, ?, ?," +
								" ?, ?, ?, ?, ?, ?");
						int i = 1;
						ps.setInt(i, estimatePer); i++;//見積期
						ps.setInt(i, estimateNum); i++;//見積番号
						ps.setString(i, summaryDTO.getStr(4)); i++;//見積枝番
						//元製作親IDサブクエリ---------------------------------
						ps.setInt(i, summaryDTO.getInt(18)); i++;//誕生期
						ps.setInt(i, summaryDTO.getInt(19)); i++;//誕生番号
						ps.setString(i, summaryDTO.getStr(14)); i++;//誕生枝番
						ps.setInt(i, summaryDTO.getInt(2)); i++;//納入機・・・これらが入力されていなければ0、入力されていればそれ（入力に一致するデータがなければ0)
						ps.setInt(i, summaryDTO.getInt(22)); i++;//購入者CD
						ps.setInt(i, summaryDTO.getInt(2) == 0 ? -1 : summaryDTO.getInt(2)); i++;//納入機・・・これが0だとヒットしてしまうので、-1にする
						ps.setInt(i, summaryDTO.getInt(18)); i++;//誕生期
						ps.setInt(i, summaryDTO.getInt(19)); i++;//誕生番号
						ps.setString(i, summaryDTO.getStr(14)); i++;//誕生枝番
						//-----------------------------------------------------
						ps.setString(i, summaryDTO.getStr(2)); i++;//案件名
						ps.setString(i, summaryDTO.getStr(15)); i++;//案内文
						ps.setInt(i, summaryDTO.getInt(0)); i++;//得意先CD
						ps.setString(i, summaryDTO.getStr(0)); i++;//得意先表示名
//						ps.setInt(i, summaryDTO.getInt(5)); i++;//個人CD
//						ps.setInt(i, summaryDTO.getInt(6)); i++;//依頼手段CD
						ps.setInt(i, deadline); i++;//納期CD
						ps.setInt(i, place); i++;//受渡場所CD
						ps.setInt(i, terms); i++;//取引条件CD
						ps.setInt(i, validity); i++;//有効期間CD
						ps.setInt(i, summaryDTO.getInt(7)); i++;//提出済CD
//						ps.setDate(i, summaryDTO.getDate(0)); i++;//依頼年月日
						ps.setDate(i, summaryDTO.getDate(1)); i++;//見積年月日
						ps.setDate(i, summaryDTO.getDate(2)); i++;//提出年月日
						ps.setInt(i, summaryDTO.getInt(8)); i++;//通貨CD
						ps.setInt(i, summaryDTO.getInt(9)); i++;//見積金額
						ps.setString(i, summaryDTO.getStr(9)); i++;//摘要
						ps.setTimestamp(i, new Timestamp(new java.util.Date().getTime())); i++;//更新日
						ps.setInt(i, 0); i++;//更新者CD
						boolean isResultSet = ps.execute();
						int   updateCount = 0;
						while (true) {
						   if (isResultSet) {
						         rs = ps.getResultSet();
						         while (rs.next()) {
						        	estimateID = rs.getInt(1);
						         }
						         rs.close();
						   }
						   else {
						         updateCount = ps.getUpdateCount();
						         if (updateCount == -1) {
						            break;
						         }
						   }
						   isResultSet = ps.getMoreResults();
						}
						//製作伝票が作成されていたら、T_製作_親テーブルの見積親IDを更新する（旧仕様）
						if(productID != 0) {
							ps = c.prepareStatement("UPDATE T_製作_親 SET 見積親ID=? WHERE 製作親ID=?");
							ps.setInt(1, estimateID);//見積親ID
							ps.setInt(2, productID);
							ps.executeUpdate();
							//新仕様
							ps = c.prepareStatement("INSERT INTO T_見積製作 VALUES(?,?)");
							ps.setInt(1, estimateID);
							ps.setInt(2, productID);
							ps.executeUpdate();
						}
					} catch(SQLException ex) {
						isError = true;
						err.append(className + "テーブル「T_見積_親」の更新に失敗しました\n");
						Logging.logStackTrace(ex, lg, className);
					}

				} else {//既存見積書更新
					try {
						ps = c.prepareStatement("UPDATE T_見積_親 SET" +
								" 見積期=?, 見積番号=?, 見積枝番=?," +
								" 元製作親ID=" +

								" CASE WHEN (?=0 AND ?=0 AND ?='' AND ?=0)" +
								"  THEN 0" +//種類、誕生製番がすべて空なら自身が新機となるため、親なしとして登録
								"  ELSE (SELECT CASE WHEN MIN(製作親ID) IS NULL THEN 0 ELSE MIN(製作親ID) END FROM T_製作_親" +
								" WHERE (得意先CD=? AND 機械番号=?) OR (製作期=? AND 製作番号=? AND 製作枝番=?)) END," +

								" 案件名=?, 案内文=?, 得意先CD=?, 得意先表示名=?," +
								" 納期CD=?, 受渡場所CD=?, 取引条件CD=?, 有効期間CD=?, 提出済CD=?, 見積年月日=?, 提出年月日=?," +
								" 通貨CD=?, 見積金額=?, 摘要=?, 更新日=?, 更新者CD=? WHERE 見積親ID=?");
						int i = 1;
						for(int g = 0; g < 18; g++)
						ps.setInt(i, estimatePer); i++;//見積期
						ps.setInt(i, estimateNum); i++;//見積番号
						ps.setString(i, summaryDTO.getStr(4)); i++;//見積枝番
						//元製作親IDサブクエリ---------------------------------
						ps.setInt(i, summaryDTO.getInt(18)); i++;//誕生期
						ps.setInt(i, summaryDTO.getInt(19)); i++;//誕生番号
						ps.setString(i, summaryDTO.getStr(14)); i++;//誕生枝番
						ps.setInt(i, summaryDTO.getInt(2)); i++;//納入機・・・これらが入力されていなければ0、入力されていればそれ（入力に一致するデータがなければ0)
						ps.setInt(i, summaryDTO.getInt(22)); i++;//購入者CD
						ps.setInt(i, summaryDTO.getInt(2) == 0 ? -1 : summaryDTO.getInt(2)); i++;//納入機・・・これが0だとヒットしてしまうので、-1にする
						ps.setInt(i, summaryDTO.getInt(18)); i++;//誕生期
						ps.setInt(i, summaryDTO.getInt(19)); i++;//誕生番号
						ps.setString(i, summaryDTO.getStr(14)); i++;//誕生枝番
						//-----------------------------------------------------
						ps.setString(i, summaryDTO.getStr(2)); i++;//案件名
						ps.setString(i, summaryDTO.getStr(15)); i++;//案内文
						ps.setInt(i, summaryDTO.getInt(0)); i++;//得意先CD
						ps.setString(i, summaryDTO.getStr(0)); i++;//得意先表示名
//						ps.setInt(i, summaryDTO.getInt(5)); i++;//個人CD
//						ps.setInt(i, summaryDTO.getInt(6)); i++;//依頼手段CD
						ps.setInt(i, deadline); i++;//納期CD
						ps.setInt(i, place); i++;//受渡場所CD
						ps.setInt(i, terms); i++;//取引条件CD
						ps.setInt(i, validity); i++;//有効期間CD
						ps.setInt(i, summaryDTO.getInt(7)); i++;//提出済CD
//						ps.setDate(i, summaryDTO.getDate(0)); i++;//依頼年月日
						ps.setDate(i, summaryDTO.getDate(1)); i++;//見積年月日
						ps.setDate(i, summaryDTO.getDate(2)); i++;//提出年月日
						ps.setInt(i, summaryDTO.getInt(8)); i++;//通貨CD
						ps.setInt(i, summaryDTO.getInt(9)); i++;//見積金額
						ps.setString(i, summaryDTO.getStr(9)); i++;//摘要
						ps.setTimestamp(i, new Timestamp(new java.util.Date().getTime())); i++;//更新日
						ps.setInt(i, 0); i++;//更新者CD
						ps.setInt(i, estimateID);//ID
						ps.executeUpdate();
						//子孫のデータ更新は、削除→追加にて
						ps = c.prepareStatement("DELETE FROM T_見積_子 WHERE 見積親ID=?");
						ps.setInt(1, estimateID);
						ps.executeUpdate();
						ps = c.prepareStatement("DELETE FROM T_見積_材料 WHERE 見積親ID=?");
						ps.setInt(1, estimateID);
						ps.executeUpdate();
						ps = c.prepareStatement("DELETE FROM T_見積_加工 WHERE 見積親ID=?");
						ps.setInt(1, estimateID);
						ps.executeUpdate();
					} catch(SQLException ex) {
						isError = true;
						ex.printStackTrace();
						err.append(className + "見積テーブルの削除に失敗しました\n");
						Logging.logStackTrace(ex, lg, className);
					}
				}
				//mainTable
				int k = 1;
				try {
					ps = c.prepareStatement(
						"INSERT INTO T_見積_子 VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)");
					for(Vector<Object> v : summaryDTO.getVector(0)) {
						int tag = (Integer)v.get(1);
						if(tag != 0) {
							keys.add((Integer)v.get(0));
							int i = 1;
							int j = 2;
							ps.setInt(i, k); i++;//ID
							ps.setInt(i, estimateID); i++;//見積親ID
							ps.setInt(i, tag); i++;//表示CD
							ps.setString(i, (String)v.get(j)); i++; j++;//名称
							ps.setBoolean(i, (Boolean)v.get(j)); i++; j++;//各FLG
							ps.setInt(i, (Integer)v.get(j)); i++; j++;//数量
							ps.setInt(i, (Integer)v.get(j)); i++; j++;//数量単位CD
							ps.setInt(i, (Integer)v.get(j)); i++; j++;//単価
							ps.setInt(i, (Integer)v.get(j)); i++; j++;//提示額
							ps.setString(i, (String)v.get(j)); i++; j++;//図番
							ps.setString(i, (String)v.get(j));//備考
							ps.addBatch();
							k++;
						}
					}
					int[] updateCounts = ps.executeBatch();
					lg.info("T_見積_子は" + updateCounts.length + "件処理されました。");
				} catch(SQLException ex) {
					isError = true;
					ex.printStackTrace();
					err.append(className + "テーブル「T_見積_子」の更新に失敗しました\n");
					Logging.logStackTrace(ex, lg, className);
				}

				//subTable
				int l = 1;
				try {
					PreparedStatement ps1 = c.prepareStatement(
						"INSERT INTO T_見積_加工 VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)");
					PreparedStatement ps2 = c.prepareStatement(
						"INSERT INTO T_見積_材料 VALUES(" +
						" ?, ?, ?, ?, ?, ?, ?, ?, ?, ?," +
						" ?, ?, ?, ?, ?, ?)");
					for(int key : keys) {

						if(summaryDTO.getMap(0).containsKey(key)) {
							int m = 1;
							for(Vector<Object> v : summaryDTO.getMap(0).get(key)) {

								if((Integer)v.get(1) != 0 || !((String)v.get(4)).equals("")) {
									if(((Integer)v.get(1)).intValue() > 100) {
										//加工等
										int i = 1;

										ps1.setInt(i, m); i++;//ID
										ps1.setInt(i, l); i++;//子ID
										ps1.setInt(i, estimateID); i++;//親ID
										ps1.setInt(i, (Integer)v.get(1)); i++;//大分類
										ps1.setInt(i, (v.get(2) == null) ? 0 : (Integer)v.get(2)); i++;//加工CD
										ps1.setInt(i, (v.get(3) == null) ? 0 : (Integer)v.get(3)); i++;//加工CD
										ps1.setString(i, (String)v.get(4)); i++;//名称
										ps1.setInt(i, (Integer)v.get(5)); i++;//単価
										ps1.setDouble(i, (Double)v.get(6)); i++;//数量
										ps1.setDouble(i, (Double)v.get(8)); i++;//掛率
										ps1.setString(i, (String)v.get(15));//備考
										ps1.addBatch();

									} else {
										//材料
										int i = 1;
										ps2.setInt(i, m); i++;//孫ID
										ps2.setInt(i, l); i++;//子ID
										ps2.setInt(i, estimateID); i++;//親ID
										ps2.setInt(i, (v.get(1) == null) ? 0 : (Integer)v.get(1)); i++;//大分類
										ps2.setInt(i, (v.get(2) == null) ? 0 : (Integer)v.get(2)); i++;//中分類
										ps2.setInt(i, (v.get(3) == null) ? 0 : (Integer)v.get(3)); i++;//小分類
										ps2.setString(i, (String)v.get(4)); i++;//名称
										ps2.setInt(i, (Integer)v.get(5)); i++;//単価
										ps2.setDouble(i, (Double)v.get(6)); i++;//数量
										ps2.setDouble(i, (Double)v.get(8)); i++;//掛率
										ps2.setInt(i, (Integer)v.get(10)); i++;//品番
										ps2.setDouble(i, (Double)v.get(11)); i++;//重量
										ps2.setInt(i, (Integer)v.get(12)); i++;//仕入先CD
//										ps2.setInt(i, 0); i++;//仕入先CD
										ps2.setBoolean(i, (Boolean)v.get(13)); i++;//仕入見積FLG
										ps2.setString(i, (String)v.get(14)); i++;//仕入納期
										ps2.setString(i, (String)v.get(15));//備考
										ps2.addBatch();

									}
									m++;
								}
							}
						}
						l++;
					}
					int[] updateCounts1 = ps1.executeBatch();
					int[] updateCounts2 = ps2.executeBatch();


					lg.info("T_見積_加工は" + updateCounts1.length + "件処理されました。");
					lg.info("T_見積_材料は" + updateCounts2.length + "件処理されました。");
				} catch(SQLException ex) {
					ex.printStackTrace();
					err.append(className + "テーブル「T_見積_孫」の更新に失敗しました\n");
					Logging.logStackTrace(ex, lg, className);
				}
			}



			if(!isError) {
				try {
					st = c.createStatement();
					st.executeUpdate("COMMIT");
				} catch(SQLException ex) {
					isError = true;
					ex.printStackTrace();
					err.append(className + "コミットに失敗しました\n");
					Logging.logStackTrace(ex, lg, className);
				}
			}
		}catch(Exception ex) {
			ex.printStackTrace();
			isError = true;lg.debug("error");
			lg.error(ex);
		} finally {
			if(isError) {lg.debug("rollback");
				try{
					st = c.createStatement();
					st.executeUpdate("ROLLBACK");
				} catch(SQLException ex) {
					err.append(className + "ロールバックに失敗しました\n");
					Logging.logStackTrace(ex, lg, className);
				}
			}
		}
		/**
		 * クライアントに送信
		 */
		try {
			response.setContentType("application/octet-stream");
			ObjectOutputStream out = new ObjectOutputStream(response.getOutputStream());
			out.writeObject(estimateID);
			out.writeUTF(err.toString());
			out.flush();
			out.close();
		}catch(Exception ex) {
			Logging.logStackTrace(ex, lg, className);
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

}
