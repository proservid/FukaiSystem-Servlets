package fukaisystem;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.sql.Connection;
import java.sql.Date;
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


public class Registration extends GenericServlet {


	private static final long serialVersionUID = 1L;
    private static final Logger lg = Logger.getLogger("A1");
	private static final String className = "Registration\n";

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
		int estimateNum = 0;
		int productID = 0;
		int productNum = 0;

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
//				st.executeQuery("SELECT COUNT(*) FROM T_見積_親 WITH(TABLOCKX)");
//				st.executeQuery("SELECT COUNT(*) FROM T_見積_子 WITH(TABLOCKX)");
//				st.executeQuery("SELECT COUNT(*) FROM T_見積_材料 WITH(TABLOCKX)");
//				st.executeQuery("SELECT COUNT(*) FROM T_見積_加工 WITH(TABLOCKX)");
			} catch(SQLException ex) {
				isError = true;
				err.append(className + "トランザクションの開始に失敗しました\n");
				Logging.logStackTrace(ex, lg, className);
			}

/*
MERGE INTO triple AS t
 USING (SELECT 101 AS fir, 2 AS sec, 11 AS thi, 'ggg' AS txt) AS w
  ON t.fir=w.fir
  AND t.sec=w.sec
  AND t.thi=w.thi
 WHEN MATCHED THEN
 	UPDATE SET
 	t.sec = w.sec,
 	t.thi = w.thi,
	t.txt = w.txt
 WHEN NOT MATCHED THEN
 	INSERT VALUES(w.fir, w.sec, w.thi, w.txt);
*/
			estimateNum = summaryDTO.getInt(3);
			estimateID = summaryDTO.getInt(14);lg.debug("estimateID;"+summaryDTO.getInt(14));
			if(summaryDTO.getInt(2) * estimateNum == 0) {
				//見積期または見積番号を0に変更したということは、消去せよということ
				if(estimateID != 0) {
					try {
						ps = c.prepareStatement("DELETE FROM T_見積_親 WHERE 見積親ID=?");
						ps.setInt(1, estimateID);
						ps.executeUpdate();
						ps = c.prepareStatement("DELETE FROM T_見積_子 WHERE 見積親ID=?");
						ps.setInt(1, estimateID);
						ps.executeUpdate();
						ps = c.prepareStatement("DELETE FROM T_見積_材料 WHERE 見積親ID=?");
						ps.setInt(1, estimateID);
						ps.executeUpdate();
						ps = c.prepareStatement("DELETE FROM T_見積_加工 WHERE 見積親ID=?");
						ps.setInt(1, estimateID);
						ps.executeUpdate();
						estimateID = 0;
					} catch(SQLException ex) {
						isError = true;
						err.append(className + "見積テーブルの削除に失敗しました\n");
						Logging.logStackTrace(ex, lg, className);
					}
				}
			} else {
				if(estimateID == 0) {
					try {
/* 2012.8.28 MERGEのストアド→MERGE+OUTPUTに変更
						cs = c.prepareCall("{call estimateProc" +
								"(?, ?, ?, ?, ?, ?, ?, ?, ?, ?," +
								" ?, ?, ?, ?, ?, ?, ?, ?, ?, ?," +
								" ?, ?, ?, ?)}");
						cs.registerOutParameter(1, java.sql.Types.INTEGER);
						int i = 2;
						cs.setInt(i, summaryDTO.getInt(2)); i++;//見積期
						cs.setInt(i, summaryDTO.getInt(3)); i++;//見積番号
						cs.setString(i, summaryDTO.getStr(3)); i++;//見積枝番
						cs.setString(i, summaryDTO.getStr(1)); i++;//案件名
						cs.setString(i, summaryDTO.getStr(2)); i++;//数量等
						cs.setInt(i, summaryDTO.getInt(1)); i++;//種類
						cs.setInt(i, summaryDTO.getInt(0)); i++;//得意先CD
						cs.setInt(i, summaryDTO.getInt(4)); i++;//個人CD
						cs.setDate(i, summaryDTO.getDate(0)); i++;//依頼年月日
						cs.setInt(i, summaryDTO.getInt(5)); i++;//依頼手段CD
						cs.setString(i, summaryDTO.getStr(4)); i++;//納期
						cs.setString(i, summaryDTO.getStr(5)); i++;//受渡場所
						cs.setString(i, summaryDTO.getStr(6)); i++;//取引条件
						cs.setString(i, summaryDTO.getStr(7)); i++;//有効期間
						cs.setDate(i, summaryDTO.getDate(1)); i++;//見積年月日
						cs.setInt(i, summaryDTO.getInt(6)); i++;//提出済CD
						cs.setDate(i, summaryDTO.getDate(2)); i++;//提出年月日
						cs.setInt(i, summaryDTO.getInt(7)); i++;//通貨CD
						cs.setInt(i, summaryDTO.getInt(8)); i++;//通貨CD
						cs.setTimestamp(i, new Timestamp(new java.util.Date().getTime())); i++;//更新日
						cs.setInt(i, 0); i++;//更新者CD
						cs.setString(i, summaryDTO.getStr(8)); i++;//備考
						cs.setString(i, "");//納品備考??
						cs.execute();

						estimateID = cs.getInt(1);lg.debug("get;"+estimateID);
*/
/* 2012.8.29 MERGE→INSERTのみに変更
						ps = c.prepareStatement(
						"MERGE INTO T_見積_親 AS t" +
						" USING (SELECT ? AS 見積期, ? AS 見積番号, ? AS 見積枝番, ? AS 案件名, ? AS 数量等, ? AS 種類, ? AS 得意先CD," +
						"  ? AS 個人CD, ? AS 依頼年月日, ? AS 依頼手段CD, ? AS 納期, ? AS 受渡場所, ? AS 取引条件," +
						"  ? AS 有効期間, ? AS 見積年月日, ? AS 提出済CD, ? AS 提出年月日, ? AS 通貨CD, ? AS 見積金額," +
						"  ? AS 更新日, ? AS 更新者CD, ? AS 備考, ? AS 納品備考) AS w" +
						" ON t.見積期=w.見積期 AND t.見積番号=w.見積番号 AND t.見積枝番=w.見積枝番" +
						" WHEN MATCHED THEN" +
						"	UPDATE SET t.案件名=w.案件名, t.数量等=w.数量等," +
						"	 t.種類=w.種類, t.得意先CD=w.得意先CD, t.個人CD=w.個人CD, t.依頼年月日=w.依頼年月日, t.依頼手段CD=w.依頼手段CD," +
						"	 t.納期=w.納期, t.受渡場所=w.受渡場所, t.取引条件=w.取引条件, t.有効期間=w.有効期間," +
						"	 t.見積年月日=w.見積年月日, t.提出済CD=w.提出済CD, t.提出年月日=w.提出年月日, t.通貨CD=w.通貨CD, t.見積金額=w.見積金額," +
						"	 t.更新日=w.更新日, t.更新者CD=w.更新者CD, t.備考=w.備考, t.納品備考=w.納品備考" +
						" WHEN NOT MATCHED THEN" +
						"	INSERT VALUES(w.見積期, w.見積番号, w.見積枝番, w.案件名," +
						"	 w.数量等, w.種類, w.得意先CD, w.個人CD, w.依頼年月日, w.依頼手段CD," +
						"	 w.納期, w.受渡場所, w.取引条件, w.有効期間, w.見積年月日, w.提出済CD," +
						"	 w.提出年月日, w.通貨CD, w.見積金額, w.更新日, w.更新者CD, w.備考, w.納品備考)" +
						" OUTPUT deleted.ID as oldId, inserted.ID as newId, inserted.更新日;");
						int i = 1;
						ps.setInt(i, summaryDTO.getInt(2)); i++;//見積期
						ps.setInt(i, summaryDTO.getInt(3)); i++;//見積番号
						ps.setString(i, summaryDTO.getStr(3)); i++;//見積枝番
						ps.setString(i, summaryDTO.getStr(1)); i++;//案件名
						ps.setString(i, summaryDTO.getStr(2)); i++;//数量等
						ps.setInt(i, summaryDTO.getInt(1)); i++;//種類
						ps.setInt(i, summaryDTO.getInt(0)); i++;//得意先CD
						ps.setInt(i, summaryDTO.getInt(4)); i++;//個人CD
						ps.setDate(i, summaryDTO.getDate(0)); i++;//依頼年月日
						ps.setInt(i, summaryDTO.getInt(5)); i++;//依頼手段CD
						ps.setString(i, summaryDTO.getStr(4)); i++;//納期
						ps.setString(i, summaryDTO.getStr(5)); i++;//受渡場所
						ps.setString(i, summaryDTO.getStr(6)); i++;//取引条件
						ps.setString(i, summaryDTO.getStr(7)); i++;//有効期間
						ps.setDate(i, summaryDTO.getDate(1)); i++;//見積年月日
						ps.setInt(i, summaryDTO.getInt(6)); i++;//提出済CD
						ps.setDate(i, summaryDTO.getDate(2)); i++;//提出年月日
						ps.setInt(i, summaryDTO.getInt(7)); i++;//通貨CD
						ps.setInt(i, summaryDTO.getInt(8)); i++;//見積金額
						ps.setTimestamp(i, new Timestamp(new java.util.Date().getTime())); i++;//更新日
						ps.setInt(i, 0); i++;//更新者CD
						ps.setString(i, summaryDTO.getStr(8)); i++;//備考
						ps.setString(i, "");//納品備考??
						boolean isResultSet = ps.execute();
						int   updateCount = 0;
						while (true) {
						   if (isResultSet) {
						         rs = ps.getResultSet();
						         while (rs.next()) {
						        	estimateID = rs.getInt(2);
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
						err.append("テーブル「T_見積_親」の更新に失敗しました\n");
						lg.error("テーブル「T_見積_親」の更新に失敗しました：" + ex);
					}
*/
						ps = c.prepareStatement(
								"INSERT INTO T_見積_親" +
								" OUTPUT inserted.ID as newId, inserted.更新日" +
								" VALUES(" +
								" ?, ?, ?, ?, ?, ?, ?, ?, ?, ?," +
								" ?, ?, ?, ?, ?, ?, ?, ?, ?, ?," +
								" ?, ?, ?)");
						int i = 1;
						ps.setInt(i, summaryDTO.getInt(2)); i++;//見積期
						ps.setInt(i, summaryDTO.getInt(3)); i++;//見積番号
						ps.setString(i, summaryDTO.getStr(3)); i++;//見積枝番
						ps.setString(i, summaryDTO.getStr(1)); i++;//案件名
						ps.setString(i, summaryDTO.getStr(2)); i++;//数量等
						ps.setInt(i, summaryDTO.getInt(1)); i++;//種類
						ps.setInt(i, summaryDTO.getInt(0)); i++;//得意先CD
						ps.setInt(i, summaryDTO.getInt(4)); i++;//個人CD
						ps.setDate(i, summaryDTO.getDate(0)); i++;//依頼年月日
						ps.setInt(i, summaryDTO.getInt(5)); i++;//依頼手段CD
						ps.setString(i, summaryDTO.getStr(4)); i++;//納期
						ps.setString(i, summaryDTO.getStr(5)); i++;//受渡場所
						ps.setString(i, summaryDTO.getStr(6)); i++;//取引条件
						ps.setString(i, summaryDTO.getStr(7)); i++;//有効期間
						ps.setDate(i, summaryDTO.getDate(1)); i++;//見積年月日
						ps.setInt(i, summaryDTO.getInt(6)); i++;//提出済CD
						ps.setDate(i, summaryDTO.getDate(2)); i++;//提出年月日
						ps.setInt(i, summaryDTO.getInt(7)); i++;//通貨CD
						ps.setInt(i, summaryDTO.getInt(8)); i++;//見積金額
						ps.setTimestamp(i, new Timestamp(new java.util.Date().getTime())); i++;//更新日
						ps.setInt(i, 0); i++;//更新者CD
						ps.setString(i, summaryDTO.getStr(8)); i++;//備考
						ps.setString(i, "");//納品備考
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
					} catch(SQLException ex) {
						isError = true;
						err.append(className + "テーブル「T_見積_親」の更新に失敗しました\n");
						Logging.logStackTrace(ex, lg, className);
					}

				} else {
					try {
						ps = c.prepareStatement("UPDATE T_見積_親 SET" +
								" 見積期=?, 見積番号=?, 見積枝番=?, 案件名=?, 数量等=?, 種類=?, 得意先CD=?, 個人CD=?, 依頼年月日=?," +
								" 依頼手段CD=?, 納期=?, 受渡場所=?, 取引条件CD=?, 有効期間=?, 見積年月日=?, 提出済CD=?, 提出年月日=?," +
								" 通貨CD=?, 見積金額=?, 更新日=?, 更新者CD=?, 備考=?, 納品備考=? WHERE 見積親ID=?");
						int i = 1;
						ps.setInt(i, summaryDTO.getInt(2)); i++;//見積期
						ps.setInt(i, summaryDTO.getInt(3)); i++;//見積番号
						ps.setString(i, summaryDTO.getStr(3)); i++;//見積枝番
						ps.setString(i, summaryDTO.getStr(1)); i++;//案件名
						ps.setString(i, summaryDTO.getStr(2)); i++;//数量等
						ps.setInt(i, summaryDTO.getInt(1)); i++;//種類
						ps.setInt(i, summaryDTO.getInt(0)); i++;//得意先CD
						ps.setInt(i, summaryDTO.getInt(4)); i++;//個人CD
						ps.setDate(i, summaryDTO.getDate(0)); i++;//依頼年月日
						ps.setInt(i, summaryDTO.getInt(5)); i++;//依頼手段CD
						ps.setString(i, summaryDTO.getStr(4)); i++;//納期
						ps.setString(i, summaryDTO.getStr(5)); i++;//受渡場所
						ps.setString(i, summaryDTO.getStr(6)); i++;//取引条件CD
						ps.setString(i, summaryDTO.getStr(7)); i++;//有効期間
						ps.setDate(i, summaryDTO.getDate(1)); i++;//見積年月日
						ps.setInt(i, summaryDTO.getInt(6)); i++;//提出済CD
						ps.setDate(i, summaryDTO.getDate(2)); i++;//提出年月日
						ps.setInt(i, summaryDTO.getInt(7)); i++;//通貨CD
						ps.setInt(i, summaryDTO.getInt(8)); i++;//見積金額
						ps.setTimestamp(i, new Timestamp(new java.util.Date().getTime())); i++;//更新日
						ps.setInt(i, 0); i++;//更新者CD
						ps.setString(i, summaryDTO.getStr(8)); i++;//備考
						ps.setString(i, ""); i++;//納品備考
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
						err.append(className + "見積テーブルの削除に失敗しました\n");
						Logging.logStackTrace(ex, lg, className);
					}
				}
				//mainTable
				int k = 1;
				try {
					ps = c.prepareStatement(
						"INSERT INTO T_見積_子 VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?, ?)");
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
							ps.setInt(i, (Integer)v.get(j)); i++; j++;//金額
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
					err.append(className + "テーブル「T_見積_子」の更新に失敗しました\n");
					Logging.logStackTrace(ex, lg, className);
				}

				//subTable
				int l = 1;
				try {
					PreparedStatement ps1 = c.prepareStatement(
						"INSERT INTO T_見積_加工 VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?)");
					PreparedStatement ps2 = c.prepareStatement(
						"INSERT INTO T_見積_材料 VALUES(" +
						" ?, ?, ?, ?, ?, ?, ?, ?, ?, ?," +
						" ?, ?, ?, ?, ?, ?)");
					for(int key : keys) {

						if(summaryDTO.getMap(0).containsKey(key)) {
							int m = 1;
							for(Vector<Object> v : summaryDTO.getMap(0).get(key)) {

								if((Integer)v.get(1) != 0 || !((String)v.get(4)).equals("")) {
									if(((Integer)v.get(1)).intValue() > 200) {
										//加工等
										int i = 1;

										ps1.setInt(i, m); i++;//ID
										ps1.setInt(i, l); i++;//子ID
										ps1.setInt(i, estimateID); i++;//親ID
										ps1.setInt(i, (Integer)v.get(1)); i++;//大分類
										ps1.setInt(i, (v.get(2) == null) ? 0 : (Integer)v.get(2)); i++;//加工CD
										ps1.setInt(i, (Integer)v.get(5)); i++;//単価
										ps1.setFloat(i, (Float)v.get(6)); i++;//数量
										ps1.setFloat(i, (Float)v.get(8)); i++;//掛率
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
										ps2.setFloat(i, (Float)v.get(6)); i++;//数量
										ps2.setFloat(i, (Float)v.get(8)); i++;//掛率
										ps2.setInt(i, (Integer)v.get(10)); i++;//品番
										ps2.setInt(i, (int)(new Float(v.get(11).toString())*1000)); i++;//重量
										ps2.setInt(i, (Integer)v.get(12)); i++;//仕入先CD
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
					err.append(className + "テーブル「T_見積_孫」の更新に失敗しました\n");
					Logging.logStackTrace(ex, lg, className);
				}
			}







			productNum = summaryDTO.getInt(10);
			productID = summaryDTO.getInt(15);
			if(summaryDTO.getInt(9) * productNum == 0) {
				//製作期または製作番号が0なら
				if(productID != 0) {
					try {
						ps = c.prepareStatement("DELETE FROM T_製作_親 WHERE 製作親ID=0");
						ps.executeUpdate();
						ps = c.prepareStatement("DELETE FROM T_製作_子 WHERE 製作親ID=0");
						ps.executeUpdate();
						productID = 0;
					} catch(SQLException ex) {
						isError = true;
						err.append(className + "製作テーブルの削除に失敗しました\n");
						Logging.logStackTrace(ex, lg, className);
					}
				}

			} else {
				if(productID == 0) {
					try {
/* 2012.8.29 MERGEのストアド→INSERT+OUTPUTに変更
						cs = c.prepareCall("{call productProc" +
								"(?, ?, ?, ?, ?, ?, ?, ?, ?, ?," +
								" ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)}");
						cs.registerOutParameter(1, java.sql.Types.INTEGER);
						int i = 2;
						cs.setInt(i, summaryDTO.getInt(9)); i++;//製作期
						cs.setInt(i, summaryDTO.getInt(10)); i++;//製作番号
						cs.setString(i, summaryDTO.getStr(10)); i++;//製作枝番
						cs.setString(i, summaryDTO.getStr(9)); i++;//受注番号
						cs.setString(i, summaryDTO.getStr(1)); i++;//案件名
						cs.setInt(i, estimateID); i++;//見積親ID
						cs.setBoolean(i, summaryDTO.getBool(0)); i++;//出図FLG
						cs.setBoolean(i, summaryDTO.getBool(1)); i++;//手配FLG
						cs.setDate(i, summaryDTO.getDate(3)); i++;//受注年月日
						cs.setDate(i, summaryDTO.getDate(4)); i++;//納期
						cs.setDate(i, summaryDTO.getDate(5)); i++;//発行年月日
						cs.setDate(i, summaryDTO.getDate(6)); i++;//出荷年月日
						cs.setInt(i, summaryDTO.getInt(7)); i++;//通貨
						cs.setInt(i, summaryDTO.getInt(11)); i++;//契約金額
						cs.setInt(i, summaryDTO.getInt(12)); i++;//納品状況CD
						cs.setDate(i, summaryDTO.getDate(7)); i++;//検収年月日
						cs.setInt(i, summaryDTO.getInt(13)); i++;//納品手段CD
						cs.setString(i, summaryDTO.getStr(11)); i++;//備考
						cs.setInt(i, summaryDTO.getInt(0)); //得意先CD
						cs.execute();
						productID = cs.getInt(1);lg.debug("get;"+productID);
 */

						ps = c.prepareStatement("INSERT INTO T_製作_親" +
								" OUTPUT inserted.ID as newId, inserted.更新日" +
								" VALUES(" +
								" ?, ?, ?, ?, ?, ?, ?, ?, ?, ?," +
								" ?, ?, ?, ?, ?, ?, ?, ?, ?, ?," +
								" ?, ?)");
						int i = 1;
						ps.setInt(i, summaryDTO.getInt(9)); i++;//製作期
						ps.setInt(i, summaryDTO.getInt(10)); i++;//製作番号
						ps.setString(i, summaryDTO.getStr(10)); i++;//製作枝番
						ps.setString(i, summaryDTO.getStr(9)); i++;//受注番号
						ps.setString(i, summaryDTO.getStr(1)); i++;//案件名
						ps.setInt(i, estimateID); i++;//見積親ID
						ps.setBoolean(i, summaryDTO.getBool(0)); i++;//出図FLG
						ps.setBoolean(i, summaryDTO.getBool(1)); i++;//手配FLG
						ps.setDate(i, summaryDTO.getDate(3)); i++;//受注年月日
						ps.setDate(i, summaryDTO.getDate(4)); i++;//納期
						ps.setDate(i, summaryDTO.getDate(5)); i++;//発行年月日
						ps.setDate(i, null); i++;//完成年月日
						ps.setDate(i, summaryDTO.getDate(6)); i++;//出荷年月日
						ps.setInt(i, summaryDTO.getInt(7)); i++;//通貨
						ps.setInt(i, summaryDTO.getInt(11)); i++;//契約金額
						ps.setInt(i, summaryDTO.getInt(12)); i++;//納品状況CD
						ps.setDate(i, summaryDTO.getDate(7)); i++;//検収年月日
						ps.setInt(i, summaryDTO.getInt(13)); i++;//納品手段CD
						ps.setTimestamp(i, new Timestamp(new java.util.Date().getTime())); i++;//更新日
						ps.setInt(i, 0); i++;//更新者CD
						ps.setString(i, summaryDTO.getStr(11)); i++;//備考
						ps.setInt(i, summaryDTO.getInt(0)); //得意先CD
						boolean isResultSet = ps.execute();
						int   updateCount = 0;
						while (true) {
						   if (isResultSet) {
						         rs = ps.getResultSet();
						         while (rs.next()) {
						        	productID = rs.getInt(1);
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
						err.append(className + "テーブル「T_製作_親」の更新に失敗しました\n");
						Logging.logStackTrace(ex, lg, className);
					}


				} else {
					try {
						ps = c.prepareStatement("UPDATE T_製作_親 SET 製作期=?, 製作番号=?, 製作枝番=?, 受注番号=?, 案件名=?, 見積親ID=?, 出図FLG=?, 手配FLG=?," +
							" 受注年月日=?, 納期=?, 発行年月日=?, 出荷年月日=?, 通貨=?, 契約金額=?, 納品状況CD=?, 検収年月日=?, 納品手段CD=?, 更新日=?, 更新者CD=?, 備考=?, 得意先CD=? WHERE 製作親ID=?");
						int i = 1;
						ps.setInt(i, summaryDTO.getInt(9)); i++;//製作期
						ps.setInt(i, summaryDTO.getInt(10)); i++;//製作番号
						ps.setString(i, summaryDTO.getStr(10)); i++;//製作枝番
						ps.setString(i, summaryDTO.getStr(9)); i++;//受注番号
						ps.setString(i, summaryDTO.getStr(1)); i++;//案件名
						ps.setInt(i, estimateID); i++;//見積親ID
						ps.setBoolean(i, summaryDTO.getBool(0)); i++;//出図FLG
						ps.setBoolean(i, summaryDTO.getBool(1)); i++;//手配FLG
						ps.setDate(i, summaryDTO.getDate(3)); i++;//受注年月日
						ps.setDate(i, summaryDTO.getDate(4)); i++;//納期
						ps.setDate(i, summaryDTO.getDate(5)); i++;//発行年月日
						ps.setDate(i, summaryDTO.getDate(6)); i++;//出荷年月日
						ps.setInt(i, summaryDTO.getInt(7)); i++;//通貨
						ps.setInt(i, summaryDTO.getInt(11)); i++;//契約金額
						ps.setInt(i, summaryDTO.getInt(12)); i++;//納品状況CD
						ps.setDate(i, summaryDTO.getDate(7)); i++;//検収年月日
						ps.setInt(i, summaryDTO.getInt(13)); i++;//納品手段CD
						ps.setTimestamp(i, new Timestamp(new java.util.Date().getTime())); i++;//更新日
						ps.setInt(i, 0); i++;//更新者CD
						ps.setString(i, summaryDTO.getStr(11)); i++;//備考
						ps.setInt(i, summaryDTO.getInt(0)); i++;//得意先CD
						ps.setInt(i, productID); //製作親ID
						ps.executeUpdate();
						ps = c.prepareStatement("DELETE FROM T_製作_子 WHERE 製作親ID=?");
						ps.setInt(1, productID);
						ps.executeUpdate();
					} catch(SQLException ex) {
						isError = true;
						err.append(className + "製作テーブルの削除に失敗しました\n");
						Logging.logStackTrace(ex, lg, className);
					}
				}

				int k = 1;
				try {
					ps = c.prepareStatement(
						"INSERT INTO T_製作_子 VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)");
					for(Vector<Object> v : summaryDTO.getVector(1)) {
						int tag = (Integer)v.get(1);
						if(tag != 0) {
							int i = 1;
							int j = 2;
							ps.setInt(i, k); i++;//ID
							ps.setInt(i, productID); i++;//見積親ID
							ps.setInt(i, tag); i++;//表示CD
							ps.setString(i, (String)v.get(j)); i++; j++;//名称
							ps.setBoolean(i, (Boolean)v.get(j)); i++; j++;//各FLG
							ps.setInt(i, (Integer)v.get(j)); i++; j++;//数量
							ps.setInt(i, (Integer)v.get(j)); i++; j++;//数量単位CD
							ps.setInt(i, (Integer)v.get(j)); i++; j++;//提示額
							ps.setString(i, (String)v.get(j)); i++; j++;//図番
							ps.setString(i, (String)v.get(j)); i++;//記事
							ps.setDate(i, v.get(j + 1) == null ? null : new Date(((java.util.Date)v.get(j + 1)).getTime())); i++;
							ps.setDate(i, v.get(j + 3) == null ? null : new Date(((java.util.Date)v.get(j + 3)).getTime()));
							ps.addBatch();
							k++;
						}
					}
					int[] updateCounts = ps.executeBatch();
					lg.info("T_製作_子は" + updateCounts.length + "件処理されました。");
				} catch(SQLException ex) {
					isError = true;
					err.append(className + "テーブル「T_製作_子」の更新に失敗しました\n");
					Logging.logStackTrace(ex, lg, className);
				}
			}



			if(!isError) {
				try {
					st = c.createStatement();
					st.executeUpdate("COMMIT");
				} catch(SQLException ex) {
					isError = true;
					err.append(className + "コミットに失敗しました\n");
					Logging.logStackTrace(ex, lg, className);
				}
			}
		}catch(Exception ex) {
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
