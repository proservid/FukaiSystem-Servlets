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
import java.util.Vector;

import javax.servlet.GenericServlet;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;



import org.apache.log4j.Logger;

import fukaisystem.dto.ProjectSummaryDTO;
import fukaisystem.sql.DBConnection;
import fukaisystem.util.Logging;


public class ProductRegistration extends GenericServlet {


	private static final long serialVersionUID = 1L;
    private static final Logger lg = Logger.getLogger("A1");
	private static final String className = "ProductRegistration\n";

	public void service(ServletRequest request, ServletResponse response) {

		DBConnection dbc = new DBConnection();
		Connection c = dbc.getConnection();
		Statement st = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		boolean isError = false;
		ProjectSummaryDTO summaryDTO = null;
		StringBuilder err = new StringBuilder();
		int estimateID = 0;
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
			} catch(SQLException ex) {
				ex.printStackTrace();
				isError = true;
				err.append(className + "トランザクションの開始に失敗しました\n");
				Logging.logStackTrace(ex, lg, className);
			}

			estimateID = summaryDTO.getInt(16);
			productID = summaryDTO.getInt(17);

			productNum = summaryDTO.getInt(11);
			if(productNum == 0 && productID != 0) {
				//製作期または製作番号が0なら
				try {
					ps = c.prepareStatement(
							"DELETE FROM T_売上_親" +
							" WHERE 売上親ID IN (SELECT MIN(売上親ID) FROM T_売上_子 WHERE 製作親ID=? group by 売上親ID);" +
							"DELETE FROM T_製作_親 WHERE 製作親ID=?;" +
							"DELETE FROM T_製作_子 WHERE 製作親ID=?;" +
							"DELETE FROM T_売上_子 WHERE 製作親ID=?;" +
							"DELETE FROM T_見積製作 WHERE 製作親ID=?");//売上親はリレーションで消える
					ps.setInt(1, productID);
					ps.setInt(2, productID);
					ps.setInt(3, productID);
					ps.setInt(4, productID);
					ps.setInt(5, productID);
					ps.executeUpdate();
					productID = 0;
				} catch(SQLException ex) {
					ex.printStackTrace();
					isError = true;
					err.append(className + "製作テーブルの削除に失敗しました\n");
					Logging.logStackTrace(ex, lg, className);
				}
			} else {
				/*
			}
				try {
					ps = c.prepareStatement("SELECT 製作親ID FROM T_製作_親 WHERE 製作期=? AND 製作番号=? AND 製作枝番=?");
					int i = 1;
					ps.setInt(i, summaryDTO.getInt(10)); i++;//製作期
					ps.setInt(i, summaryDTO.getInt(11)); i++;//製作番号
					ps.setString(i, summaryDTO.getStr(11)); i++;//製作枝番
					rs = ps.executeQuery();
					if(rs.next()) {
						if(rs.getInt("製作親ID") != 0) {
							productID = rs.getInt("製作親ID");
						}
					}
				} catch(SQLException ex) {
					ex.printStackTrace();
					isError = true;
					err.append(className + "製作親IDの読み込みに失敗しました\n");
					Logging.logStackTrace(ex, lg, className);
				}
				*/
				if(productID == 0) {//製作伝票新規作成
					try {
						ps = c.prepareStatement("INSERT INTO T_製作_親" +
								" (製作親ID, 製作期, 製作番号, 製作枝番, 受注番号, 案件名, 見積親ID, 得意先CD, 機械番号, 納入先名, 納期," +
								"受注年月日, 発行年月日, 出荷年月日, 検収年月日, 通貨CD, 契約金額, 摘要, 出図FLG, 手配FLG, 更新日, 更新者CD)" +
								" OUTPUT inserted.製作親ID as newId, inserted.更新日" +
								" SELECT CASE WHEN MAX(製作親ID) IS NULL THEN 1 ELSE MAX(製作親ID)+1 END," +
								"?, ?, ?, ?, ?, ?, ?," +
								"CASE WHEN ?=1 THEN" +//新機なら、
								" (SELECT CASE WHEN MAX(機械番号) IS NULL THEN 1 ELSE MAX(機械番号)+1 END FROM T_製作_親 WHERE 得意先CD=?)" +//機種番号を登録
								" ELSE 0 END," +//それ以外の場合はすべて0
								"?, ?, ?, ?, ?, ?, ?, ?, ?, ?," +
								"?, ?, ? FROM T_製作_親");
						int i = 1;
						ps.setInt(i, summaryDTO.getInt(10)); i++;//製作期
						ps.setInt(i, summaryDTO.getInt(11)); i++;//製作番号
						ps.setString(i, summaryDTO.getStr(11)); i++;//製作枝番
						ps.setString(i, summaryDTO.getStr(10)); i++;//受注番号
						ps.setString(i, summaryDTO.getStr(16)); i++;//案件名
						ps.setInt(i, estimateID); i++;//見積親ID
						ps.setInt(i, summaryDTO.getInt(1)); i++;//得意先CD
						//機械番号サブクエリ-----------------------------------
						ps.setBoolean(i, summaryDTO.getBool(2)); i++;//新機FLG
						ps.setInt(i, summaryDTO.getInt(1)); i++;//得意先CD
						//-----------------------------------------------------
						ps.setString(i, summaryDTO.getStr(3)); i++;//納入先名
						ps.setDate(i, summaryDTO.getDate(4)); i++;//納期
						ps.setDate(i, summaryDTO.getDate(3)); i++;//受注年月日
						ps.setDate(i, summaryDTO.getDate(5)); i++;//発行年月日
						ps.setDate(i, summaryDTO.getDate(6)); i++;//出荷年月日
						ps.setDate(i, summaryDTO.getDate(7)); i++;//検収年月日
						ps.setInt(i, summaryDTO.getInt(12)); i++;//通貨CD
						ps.setInt(i, summaryDTO.getInt(13)); i++;//契約金額
						ps.setString(i, summaryDTO.getStr(12)); i++;//摘要
						ps.setBoolean(i, summaryDTO.getBool(0)); i++;//出図FLG
						ps.setBoolean(i, summaryDTO.getBool(1)); i++;//手配FLG
						ps.setTimestamp(i, new Timestamp(new java.util.Date().getTime())); i++;//更新日
						ps.setInt(i, 0); i++;//更新者CD
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
						ex.printStackTrace();
						isError = true;
						err.append(className + "テーブル「T_製作_親」の更新に失敗しました\n");
						Logging.logStackTrace(ex, lg, className);
					}


				} else {//既存製作伝票の更新
					try {
						ps = c.prepareStatement("UPDATE T_製作_親 SET 製作期=?, 製作番号=?, 製作枝番=?," +
							"受注番号=?, 案件名=?, 見積親ID=?, 得意先CD=?," +//これは受注者、カルテ履歴の得意先（購入者）と異なる可能性あり
							"機械番号=" +
							"CASE WHEN ?=1 THEN" +//新機なら、
							" (SELECT CASE WHEN MAX(機械番号) IS NULL THEN 1 ELSE MAX(機械番号)+1 END FROM T_製作_親 WHERE 得意先CD=?)" +//純粋な新機だから機種番号をセット
							" ELSE 0 END," +//それ以外の場合はすべて0
							"納入先名=?, 納期=?, 受注年月日=?, 発行年月日=?, 出荷年月日=?, 検収年月日=?, 通貨CD=?, 契約金額=?, 摘要=?, 出図FLG=?, 手配FLG=?, 更新日=?, 更新者CD=? WHERE 製作親ID=?");
						int i = 1;
						ps.setInt(i, summaryDTO.getInt(10)); i++;//製作期
						ps.setInt(i, summaryDTO.getInt(11)); i++;//製作番号
						ps.setString(i, summaryDTO.getStr(11)); i++;//製作枝番
						ps.setString(i, summaryDTO.getStr(10)); i++;//受注番号
						ps.setString(i, summaryDTO.getStr(16)); i++;//案件名
						ps.setInt(i, estimateID); i++;//見積親ID
						ps.setInt(i, summaryDTO.getInt(1)); i++;//得意先CD
						//機械番号サブクエリ-----------------------------------
						ps.setBoolean(i, summaryDTO.getBool(2)); i++;//新機FLG
						ps.setInt(i, summaryDTO.getInt(1)); i++;//得意先CD
						//-----------------------------------------------------
						ps.setString(i, summaryDTO.getStr(3)); i++;//納入先名
						ps.setDate(i, summaryDTO.getDate(4)); i++;//納期
						ps.setDate(i, summaryDTO.getDate(3)); i++;//受注年月日
						ps.setDate(i, summaryDTO.getDate(5)); i++;//発行年月日
						ps.setDate(i, summaryDTO.getDate(6)); i++;//出荷年月日
						ps.setDate(i, summaryDTO.getDate(7)); i++;//検収年月日
						ps.setInt(i, summaryDTO.getInt(12)); i++;//通貨CD
						ps.setInt(i, summaryDTO.getInt(13)); i++;//契約金額
						ps.setString(i, summaryDTO.getStr(12)); i++;//摘要
						ps.setBoolean(i, summaryDTO.getBool(0)); i++;//出図FLG
						ps.setBoolean(i, summaryDTO.getBool(1)); i++;//手配FLG
						ps.setTimestamp(i, new Timestamp(new java.util.Date().getTime())); i++;//更新日
						ps.setInt(i, 0); i++;//更新者CD
						ps.setInt(i, productID); //製作親ID
						ps.executeUpdate();
						ps = c.prepareStatement("DELETE FROM T_製作_子 WHERE 製作親ID=?");
						ps.setInt(1, productID);
						ps.executeUpdate();
					} catch(SQLException ex) {
						ex.printStackTrace();
						isError = true;
						err.append(className + "製作テーブルの削除に失敗しました\n");
						Logging.logStackTrace(ex, lg, className);
					}
				}

				try {
//					if(summaryDTO.getBool(3)) {//カルテ追加
						ps = c.prepareStatement("DELETE FROM T_カルテ履歴 WHERE 製作親ID=?");
						ps.setInt(1, productID);
						ps.executeUpdate();

						ps = c.prepareStatement(
								"INSERT INTO T_カルテ履歴 (製作親ID,元製作親ID) " +//部品の製作ID,機械の製作ID
								" SELECT ?, CASE WHEN ?=1 THEN" +//新しい機種で
									" CASE WHEN MAX(製作親ID) IS NULL THEN ? ELSE MAX(製作親ID) END" +//製作履歴がなければその製作親ID、あれば直近の製作親ID
								" ELSE" +//修理部品等で
									" CASE WHEN MAX(製作親ID) IS NULL THEN 0 ELSE MAX(製作親ID) END" +//製作履歴がなければ0、あれば直近の製作親ID
								" END FROM T_製作_親 WHERE 得意先CD=? AND 機械番号=? AND 機械番号<>0");
						for(Integer parentCode : summaryDTO.getParents()) {
							int i = 1;
							ps.setInt(i, productID); i++;//製作親ID
							ps.setBoolean(i, summaryDTO.getBool(2)); i++;//新機FLG
							ps.setInt(i, productID); i++;//製作親ID
							ps.setInt(i, summaryDTO.getInt(22)); i++;//購入者CD
							ps.setInt(i, parentCode); i++;//納入機
							ps.addBatch();
						}
						int[] updateCounts = ps.executeBatch();
				} catch(SQLException ex) {
					ex.printStackTrace();
					isError = true;
					err.append(className + "テーブル「T_カルテ履歴」の更新に失敗しました\n");
					Logging.logStackTrace(ex, lg, className);
				}
if(productID != 0) {
				int k = 1;
				try {
					ps = c.prepareStatement(
						"INSERT INTO T_製作_子 VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)");
					for(Vector<Object> v : summaryDTO.getVector(1)) {
						int tag = (Integer)v.get(1);
						if(tag != 0) {
							int i = 1;
							int j = 2;
							ps.setInt(i, k); i++;//ID
							ps.setInt(i, productID); i++;//製作親ID
							ps.setInt(i, tag); i++;//表示CD
							ps.setString(i, (String)v.get(j)); i++; j++;//名称
							ps.setBoolean(i, (Boolean)v.get(j)); i++; j++;//各FLG
							ps.setInt(i, (Integer)v.get(j)); i++; j++;//数量
							ps.setInt(i, (Integer)v.get(j)); i++; j++;//数量単位CD
							ps.setInt(i, (Integer)v.get(j)); i++; j++;//単価
							ps.setInt(i, (Integer)v.get(j)); i++; j++;//金額
							ps.setString(i, (String)v.get(j)); i++; j++;//図番
							ps.setString(i, (String)v.get(j)); i++; j++;//記事
							ps.setDate(i, (v.get(j) == null || v.get(j).equals("")) ? null : new Date(((java.util.Date)v.get(j)).getTime())); i++; j += 2;
							ps.setDate(i, (v.get(j) == null || v.get(j).equals("")) ? null : new Date(((java.util.Date)v.get(j)).getTime()));
							ps.addBatch();
							k++;
						}
					}
					int[] updateCounts = ps.executeBatch();
					lg.info("T_製作_子は" + updateCounts.length + "件処理されました。");
				} catch(SQLException ex) {
					ex.printStackTrace();
					isError = true;
					err.append(className + "テーブル「T_製作_子」の更新に失敗しました\n");
					Logging.logStackTrace(ex, lg, className);
				}

				try {
					ps = c.prepareStatement("DELETE FROM T_見積製作 WHERE 製作親ID=?");
					ps.setInt(1, productID);
					ps.executeUpdate();
					ps = c.prepareStatement("INSERT INTO T_見積製作" +
						" select 見積親ID,? from (" +
						" select 見積親ID,convert(varchar,見積期)+'-'+right('000' + convert(varchar, 見積番号), 3)+見積枝番 as 見積番 from T_見積_親) a" +
						" where 見積番 like ?");
					for(String s : summaryDTO.getEsts()) {
						ps.setInt(1, productID);
						ps.setString(2, s);
						ps.addBatch();
					}
					int[] updateCounts = ps.executeBatch();
					lg.info("T_見積製作は" + updateCounts.length + "件処理されました。");
				} catch(SQLException ex) {
					ex.printStackTrace();
					isError = true;
					err.append(className + "テーブル「T_見積製作」の更新に失敗しました\n");
					Logging.logStackTrace(ex, lg, className);
				}
}
			}



			if(!isError) {
				try {
					st = c.createStatement();
					st.executeUpdate("COMMIT");
				} catch(SQLException ex) {
					ex.printStackTrace();
					isError = true;
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
			out.writeObject(productID);
			out.writeUTF(err.toString());
			out.flush();
			out.close();
		}catch(Exception ex) {
			ex.printStackTrace();
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
