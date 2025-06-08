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
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import javax.servlet.GenericServlet;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import org.apache.log4j.Logger;

import fukaisystem.dto.OrderDocumentDTO;
import fukaisystem.sql.DBConnection;
import fukaisystem.util.Logging;


public class OrderRegistration extends GenericServlet {

	/**
	 *
	 */
	private static final long serialVersionUID = 1L;
	static final Logger lg = Logger.getLogger("A1");
	private static final String className = "OrderRegistration\n";

	public void service(ServletRequest request, ServletResponse response) {

		DBConnection dbc = new DBConnection();
		Connection c = dbc.getConnection();
		boolean isError = false;
		OrderDocumentDTO odd = null;
		StringBuilder err = new StringBuilder("");
		int orderID = -1;
		Map<Integer, DeliverySlip> taxMap = null;
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
				if(obj instanceof OrderDocumentDTO) {
					odd = (OrderDocumentDTO)obj;
				} else {
					err.append(className + "readObjectがString型ではありません\n");
					lg.error(className + "readObjectがString型ではありません");
				}
			}

			try {
				//コミット後でありさえすればよい。
				//コミット後ということは、子・孫ともども更新されているはずで、問題は生じない
				//親→子→孫（加工→材料）の順が守られている限り、デッドロックは発生しないはず
				Statement st = c.createStatement();
				st.executeUpdate("SET TRANSACTION ISOLATION LEVEL READ COMMITTED");
				st.executeUpdate("BEGIN TRANSACTION");
//				st.executeQuery("SELECT COUNT(*) FROM T_在庫_親 WITH(TABLOCKX)");
//				st.executeQuery("SELECT COUNT(*) FROM T_在庫_子 WITH(TABLOCKX)");
			} catch(SQLException ex) {
				ex.printStackTrace();
				isError = true;
				err.append(className + "トランザクションの開始に失敗しました\n");
				Logging.logStackTrace(ex, lg, className);
			}

			//////////////////////////////////////////////////////////////////////////////////////////////////////
			boolean isParentEditable = true; //納品書番号が入ったデータが１つでもあるか
			taxMap = new HashMap<Integer, DeliverySlip>(); //指定納品書のデータ
			List<Vector<Object>> regVector = new ArrayList<Vector<Object>>();
			for(Vector<Object> v : odd.getVector(0)) {
				boolean closeFlg = false;
				boolean matchFlg = false;
				if(v.get(14) == null || v.get(15) == null || !(Boolean)v.get(17)) {//納品書番号か納品日がnullまたは納品書チェックなし
					//納品書入力をクリアし、登録用データに追加
					v.set(14, 0);
					v.set(15, null);
					v.set(16, 0);
					v.set(17, false);
					regVector.add(v);
				} else {
					//〆後の日付の指定納品書を追加させない
					PreparedStatement ps = c.prepareStatement("select * from T_指定納品書 WHERE 納品書日>=?　and 納品書日<? and 〆FLG='true'");
					java.util.Date d = (java.util.Date)v.get(15);
					Calendar cal = Calendar.getInstance();
					cal.setTime(d);
					cal.set(Calendar.DATE, 1);//その月の１日
					ps.setDate(1, new Date(cal.getTimeInMillis()));
					cal.add(Calendar.MONTH, 1);//翌月１日
					ps.setDate(2, new Date(cal.getTimeInMillis()));
					ResultSet rs = ps.executeQuery();
					while(rs.next()) {
						//一致する納品書データがあるか
						if(rs.getInt("ID") == (Integer)v.get(14)) {
							if((Boolean)v.get(18)) {
								matchFlg = true;
								//1行でも指定納品書が〆られていたら親データの編集を不可に
								isParentEditable = false;
								//〆後なので変更されてはいないはずだが念のため登録データをセットしておく(IDは一致確認済)
								v.set(15, rs.getDate("納品書日"));
								v.set(16, rs.getInt("消費税"));
								v.set(17, true);
								v.set(18, true);
								regVector.add(v);
							}
						}
						//一致する納品書データがなくてもその月が〆られてさえいれば(その行の)closeFlgをtrueに
						closeFlg = true;
					}
					if(!closeFlg) {//その行の納品書日が〆後の日付でなければ
						//データはそのまま使用
						regVector.add(v);
						//納品書番号、納品書日、消費税、〆FLGをMapにセット（後でT_指定納品書にmergeするため）
						if(!taxMap.containsKey(v.get(14)) || !(Boolean)v.get(18)) {
							//納品書番号が同じデータ又は〆後データは省く
							taxMap.put((Integer)v.get(14), new DeliverySlip(new Date(((java.util.Date)v.get(15)).getTime()), v.get(16) == null ? 0 : (Integer)v.get(16)));
						}
					} else {
						//〆後の日付で
						if(!matchFlg) {
							//登録されていない番号　または
							//登録はあるが〆Flgがfalse(=番号入力ミス)
							//クリアし、登録用データに追加
							v.set(14, 0);
							v.set(15, null);
							v.set(16, 0);
							v.set(17, false);
							regVector.add(v);
							err.append("〆後の納品書日では登録できません");
						}
					}
				}
			}
//			if(closeFlg) err.append("〆後の納品書日が入力されている行があります\n納品書データは〆後の日付で登録できません");
			//////////////////////////////////////////////////////////////////////////////////////////////////////
			orderID = odd.getInt(4);
			if(odd.getInt(1) + odd.getInt(2) == 0) {
				//注文期と注文番号を0に変更したということは、消去せよということ
				if(isParentEditable) {
					//削除
					if(orderID != 0) {
						try {
							PreparedStatement ps = c.prepareStatement("DELETE FROM T_在庫_親 WHERE 在庫親ID=?");
							ps.setInt(1, orderID);
							ps.executeUpdate();
							ps = c.prepareStatement("DELETE FROM T_在庫_子 WHERE 在庫親ID=?");
							ps.setInt(1, orderID);
							ps.executeUpdate();
							orderID = 0;
						} catch(SQLException ex) {
							ex.printStackTrace();
							isError = true;
							err.append("データの削除に失敗しました\n");
							Logging.logStackTrace(ex, lg, className);
						}
					}
				}
			} else {
				int orderNum = odd.getInt(2);
				//在庫用注文番号（≠注文書番号）自動採番
				if(orderNum == 0) {
					try {
						PreparedStatement ps = c.prepareStatement("SELECT CASE WHEN MAX(注文番号) IS NULL THEN 1 ELSE MAX(注文番号)+1 END AS 新注文番号 FROM T_在庫_親 WHERE 注文期=? AND 注文番号<9999");
						ps.setInt(1, odd.getInt(1));//注文期
						ResultSet rs = ps.executeQuery();
						if(rs.next()) {
							orderNum = rs.getInt("新注文番号");
						}
					} catch(SQLException ex) {
						ex.printStackTrace();
						isError = true;
						err.append("注文番号の読み込みに失敗しました\n");
						Logging.logStackTrace(ex, lg, className);
					}
				}

				if(orderID == 0 || odd.getInt(3) == 0) {//ID又は注文書番号が0 → 新規

					try {
						PreparedStatement ps = c.prepareStatement(
								"INSERT INTO T_在庫_親" +
								" OUTPUT inserted.在庫親ID as newId, inserted.更新日" +
								" SELECT ?, ?, ?, MAX(伝票番号)+1, ?, ?, ?, ?, ?, ?, ? FROM T_在庫_親");
						int i = 1;
						ps.setInt(i, odd.getInt(1)); i++;//注文期
						ps.setInt(i, orderNum); i++;//注文番号
						ps.setString(i, odd.getStr(1)); i++;//注文枝番
//自動採番
//						ps.setInt(i, odd.getInt(3)); i++;//伝票番号
						ps.setInt(i, odd.getInt(0)); i++;//仕入先CD
						ps.setDate(i, odd.getDate(0)); i++;//注文年月日
						ps.setDate(i, odd.getDate(1)); i++;//指定納期
						ps.setString(i, odd.getStr(2)); i++;//摘要
						ps.setString(i, odd.getStr(3)); i++;//納入先指定
						ps.setTimestamp(i, new Timestamp(new java.util.Date().getTime())); i++;//更新日
						ps.setInt(i, 0); i++;//更新者CD
						boolean isResultSet = ps.execute();
						int   updateCount = 0;
						while (true) {
						   if (isResultSet) {
							   ResultSet rs = ps.getResultSet();
						         while (rs.next()) {
						        	orderID = rs.getInt(1);
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
						err.append("基礎データの更新に失敗しました\n仕入先コード、日付フォーマット等を見直してみてください");
						Logging.logStackTrace(ex, lg, className);
					}

				} else {
					//更新
					try {
						if(isParentEditable) {
							PreparedStatement ps = c.prepareStatement("UPDATE T_在庫_親 SET" +
								" 注文期=?, 注文番号=?, 注文枝番=?, 仕入先CD=?, 注文年月日=?, 指定納期=?," +
								" 摘要=?, 納入先指定=?, 更新日=?, 更新者CD=?" +
								" WHERE 在庫親ID=?");
							int i = 1;
							ps.setInt(i, odd.getInt(1)); i++;//注文期
							ps.setInt(i, orderNum); i++;//注文番号
							ps.setString(i, odd.getStr(1)); i++;//注文枝番
	//自動採番
	//						ps.setInt(i, odd.getInt(3)); i++;//伝票番号
							ps.setInt(i, odd.getInt(0)); i++;//仕入先CD
							ps.setDate(i, odd.getDate(0)); i++;//注文年月日
							ps.setDate(i, odd.getDate(1)); i++;//指定納期
							ps.setString(i, odd.getStr(2)); i++;//摘要
							ps.setString(i, odd.getStr(3)); i++;//納入先指定
							ps.setTimestamp(i, new Timestamp(new java.util.Date().getTime())); i++;//更新日
							ps.setInt(i, 0); i++;//更新者CD
							ps.setInt(i, orderID); i++;//在庫親ID
							ps.executeUpdate();
							ps.close();
						}
					} catch(SQLException ex) {
						ex.printStackTrace();
						isError = true;
						err.append("基礎データの更新に失敗しました\n仕入先コード、日付フォーマット等を見直してみてください");
						Logging.logStackTrace(ex, lg, className);
					}
					try {
						//子孫のデータ更新は、削除→追加にて
						//T_在庫_子とT_指定納品書のinnerjoinにおいて、それぞれの〆FLGに不一致のものがあったときは
						//〆後に〆前の画面から登録しようとした→エラー
						PreparedStatement ps = c.prepareStatement("DELETE FROM T_在庫_子 WHERE 在庫親ID=?");
						//where以降
						ps.setInt(1, orderID);//在庫親ID
						ps.executeUpdate();
					} catch(SQLException ex) {
						ex.printStackTrace();
						isError = true;
						err.append("明細データの更新に失敗しました\n");
						Logging.logStackTrace(ex, lg, className);
					}
				}

				int k = 1;
				try {
					PreparedStatement ps = c.prepareStatement(
						"INSERT INTO T_在庫_子 VALUES(" +
						"?, ?, ?, ?, ?, ?, ?, ?, ?, ?," +
						"?, ?, ?, ?, ?, ?)");
					for(Vector<Object> v : regVector) {
						int tag = (Integer)v.get(0);
						if(tag != 0) {
							int i = 1;
							int j = 0;
							ps.setInt(i, k); i++;//ID
							ps.setInt(i, orderID); i++;//親ID
							ps.setInt(i, (Integer)v.get(j)); i++; j++;//表示CD
							ps.setInt(i, (v.get(j) == null) ? 0 : (Integer)v.get(j)); i++; j++;//大分類CD
							ps.setInt(i, (v.get(j) == null) ? 0 : (Integer)v.get(j)); i++; j++;//中分類CD
							ps.setInt(i, (v.get(j) == null) ? 0 : (Integer)v.get(j)); i++; j++;//小分類CD
							ps.setString(i, (String)v.get(j)); i++; j++;//名称
							ps.setBoolean(i, (Boolean)v.get(j)); i++; j++;//各FLG
							ps.setInt(i, (Integer)v.get(j)); i++; j++;//数量
							ps.setInt(i, (Integer)v.get(j)); i++; j++;//数量単位CD
							ps.setDouble(i, (Double)v.get(j)); i++; j++;//重量長さ（単位を要検討のこと）
							ps.setInt(i, (Integer)v.get(j)); i++; j++;//単価
							ps.setInt(i, (Integer)v.get(j)); i++; j++;//金額
							ps.setString(i, (String)v.get(j)); i++; j++;//備考
							ps.setDate(i, v.get(j) == null ? null : new java.sql.Date(((java.util.Date)v.get(j)).getTime())); i++; j += 2;//入庫年月日（チェックボックスを飛ばすためj+=2）
							ps.setInt(i, v.get(j) == null ? 0 : (Integer)v.get(j));//納品書番号
							ps.addBatch();
							k++;
						}
					}
					int[] updateCounts = ps.executeBatch();
					lg.info("T_在庫_子は" + updateCounts.length + "件処理されました。");
				} catch(SQLException ex) {
					ex.printStackTrace();
					isError = true;
					err.append("明細データの登録に失敗しました\n");
					Logging.logStackTrace(ex, lg, className);
				}

				try {
					PreparedStatement ps = c.prepareStatement(
					 "MERGE INTO T_指定納品書 AS t" +
					 " USING (SELECT ? AS ID, ? AS 納品書日, ? AS 消費税) AS w" +
					 "  ON t.ID=w.ID AND t.〆FLG='false'" +
					 " WHEN MATCHED THEN" +
					 "  UPDATE SET" +
					 "   t.ID = w.ID," +
					 "   t.納品書日 = w.納品書日," +
					 "   t.消費税 = w.消費税" +
					 " WHEN NOT MATCHED THEN" +
					 "  INSERT VALUES(w.ID, w.納品書日, w.消費税, 'false');");
					for(Map.Entry<Integer, DeliverySlip> e : taxMap.entrySet()) {
						ps.setInt(1, e.getKey());//ID
						ps.setDate(2, e.getValue().getDate());
						ps.setInt(3, e.getValue().getTax());//消費税
						ps.addBatch();
					}
					int[] updateCounts = ps.executeBatch();
					lg.info("T_指定納品書は" + updateCounts.length + "件処理されました。");
				} catch(SQLException ex) {
					ex.printStackTrace();
					isError = true;
					err.append("〆後の納品書番号では登録できません");
					Logging.logStackTrace(ex, lg, className);
				}

				try {
					//注文書データで使用していない納品書番号は削除する
					PreparedStatement ps = c.prepareStatement(
							"delete from T_指定納品書 where ID IN (" +
							"select ID from T_指定納品書 f WHERE NOT EXISTS(" +
							"SELECT 1 FROM T_在庫_子 c WHERE c.納品書番号=f.ID))");
					ps.executeUpdate();
				} catch(SQLException ex) {
					ex.printStackTrace();
					isError = true;
					err.append(className + "指定納品書の掃除に失敗しました\n");
					Logging.logStackTrace(ex, lg, className);
				}

			}

			if(!isError) {
				try {
					Statement st = c.createStatement();
					st.executeUpdate("COMMIT");
					st.close();
				} catch(SQLException ex) {
					ex.printStackTrace();
					isError = true;
					err.append(className + "コミットに失敗しました\n");
					Logging.logStackTrace(ex, lg, className);
				}
			}
		} catch(Exception ex) {
			ex.printStackTrace();
			isError = true;lg.debug("error");
			lg.error(ex);
		} finally {
			if(isError) {lg.debug("rollback");
				try{
					Statement st = c.createStatement();
					st.executeUpdate("ROLLBACK");
				} catch(SQLException ex) {
					ex.printStackTrace();
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
			out.writeObject(orderID);
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
				ex.printStackTrace();
				Logging.logStackTrace(ex, lg, className);
			}
// The following processes requires JDBC4.0.
//		https://www.ibm.com/developerworks/jp/java/library/j-jtp03216/index.html
			/*
			try {
				if(ps != null && !ps.isClosed()) {
					ps.close();
					lg.debug(className + "ps is closed by jdbc4.0");
				}
			} catch(SQLException ex) {
				ex.printStackTrace();
				Logging.logStackTrace(ex, lg, className);
			}
			try {
				if(rs != null && !rs.isClosed()) {
					rs.close();
					lg.debug(className + "rs is closed by jdbc4.0");
				}
			} catch(SQLException ex) {
				ex.printStackTrace();
				Logging.logStackTrace(ex, lg, className);
			}
			*/
		}
	}
	private class DeliverySlip {
		Date date;
		int tax;

		DeliverySlip(Date date, int tax) {
			this.date = date;
			this.tax = tax;
		}

		Date getDate() {
			return date;
		}
		int getTax() {
			return tax;
		}
	}
}
