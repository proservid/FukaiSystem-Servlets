package fukaisystem;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.Vector;

import javax.servlet.GenericServlet;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;



import org.apache.log4j.Logger;

import fukaisystem.dto.DeliveryDTO;
import fukaisystem.sql.DBConnection;
import fukaisystem.util.Logging;


public class DeliveryRegistration extends GenericServlet {


	private static final long serialVersionUID = 1L;
    private static final Logger lg = Logger.getLogger("A1");
	private static final String className = "DeliveryRegistration\n";

	public void service(ServletRequest request, ServletResponse response) {
		DBConnection dbc = new DBConnection();
		Connection c = dbc.getConnection();
		Statement st = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		boolean isError = false;
		DeliveryDTO deliveryDTO = null;
		StringBuilder err = new StringBuilder();


		int deliveryID = 0;
		int productID = 0;


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
				if(obj instanceof DeliveryDTO) {
					deliveryDTO = (DeliveryDTO)obj;
				} else {
					isError = true;
					err.append(className + "readObjectがShippingDTO型ではありません\n");
					lg.error(className + "readObjectがShippingDTO型ではありません");
				}
			}


			deliveryID = deliveryDTO.getInt(0);
			productID = deliveryDTO.getInt(1);

			boolean isEmpty = true;
			for(Vector<Object> v : deliveryDTO.getVector()) {
				if((Integer)v.get(2) != 0) {
					isEmpty = false;
				}
			}
			
			if(isEmpty) {
				err.append("明細データがありません。\n");
			} else {
				try {
					st = c.createStatement();
					st.executeUpdate("SET TRANSACTION ISOLATION LEVEL READ COMMITTED");
					st.executeUpdate("BEGIN TRANSACTION");
				} catch(SQLException ex) {
					isError = true;
					err.append(className + "トランザクションの開始に失敗しました\n");
					Logging.logStackTrace(ex, lg, className);
				}

				if(deliveryID == 0) {
					try {
						ps = c.prepareStatement(
						"INSERT INTO T_売上_親" +
							" OUTPUT inserted.売上親ID as newId, inserted.更新日" +
							" VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)");
						int i = 1;
						ps.setInt(i, deliveryDTO.getInt(2)); i++;//得意先CD
						ps.setDate(i, deliveryDTO.getDate()); i++;//売上年月日
						ps.setBoolean(i, (deliveryDTO.getInt(7) != 2)); i++;//売上FLG
						ps.setBoolean(i, (deliveryDTO.getInt(7) != 1)); i++;//請求FLG
						ps.setInt(i, deliveryDTO.getInt(3)); i++;//納品区分
						ps.setInt(i, deliveryDTO.getInt(4)); i++;//納品手段
						if(deliveryDTO.getInt(5) < 0) {
							ps.setNull(i, Types.INTEGER); i++;
						} else {
							ps.setInt(i, deliveryDTO.getInt(5)); i++;//消費税
						}
						ps.setInt(i, deliveryDTO.getInt(6)); i++;//値引き
						ps.setString(i, deliveryDTO.getString()); i++;//摘要
						ps.setTimestamp(i, new Timestamp(new java.util.Date().getTime())); i++;//更新日
						ps.setInt(i, 0);//更新者CD
						boolean isResultSet = ps.execute();
						int   updateCount = 0;
						while (true) {
						   if (isResultSet) {
						         rs = ps.getResultSet();
						         while (rs.next()) {
						        	deliveryID = rs.getInt(1);
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
						err.append(className + "テーブル「T_売上_親」の更新に失敗しました\n");
						Logging.logStackTrace(ex, lg, className);
					}
	
	
				} else {
	
					try {
						ps = c.prepareStatement("UPDATE T_売上_親 SET 得意先CD=?, 売上年月日=?, 売上FLG=?, 請求FLG=?, 納品区分CD=?, 納品手段CD=?, 消費税=?, 値引き=?, 摘要=?, 更新日=?, 更新者CD=?" +
								" WHERE 売上親ID=?");
						int i = 1;
						ps.setInt(i, deliveryDTO.getInt(2)); i++;//得意先CD
						ps.setDate(i, deliveryDTO.getDate()); i++;//売上年月日
						ps.setBoolean(i, (deliveryDTO.getInt(7) != 2)); i++;//売上FLG
						ps.setBoolean(i, (deliveryDTO.getInt(7) != 1)); i++;//請求FLG
						ps.setInt(i, deliveryDTO.getInt(3)); i++;//納品区分
						ps.setInt(i, deliveryDTO.getInt(4)); i++;//納品手段
						if(deliveryDTO.getInt(5) < 0) {
							ps.setNull(i, Types.INTEGER); i++;
						} else {
							ps.setInt(i, deliveryDTO.getInt(5)); i++;//消費税
						}
						ps.setInt(i, deliveryDTO.getInt(6)); i++;//値引き
						ps.setString(i, deliveryDTO.getString()); i++;//摘要
						ps.setTimestamp(i, new Timestamp(new java.util.Date().getTime())); i++;//更新日
						ps.setInt(i, 0); i++;//更新者CD
						ps.setInt(i, deliveryID);
						ps.executeUpdate();
						ps = c.prepareStatement("DELETE FROM T_売上_子 WHERE 売上親ID=?");
						ps.setInt(1, deliveryID);
						ps.executeUpdate();
					} catch(SQLException ex) {
						isError = true;
						err.append(className + "売上テーブルの削除に失敗しました\n");
						Logging.logStackTrace(ex, lg, className);
					}
				}
	//UPDATE失敗したらINSERTさせない
				int k = 1;
				try {
					ps = c.prepareStatement(
						"INSERT INTO T_売上_子 VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?);");// +
						//"UPDATE T_製作_子 SET 納品年月日=? WHERE ID=? AND 製作親ID=?");
					for(Vector<Object> v : deliveryDTO.getVector()) {
						int tag = (Integer)v.get(2);
						if(tag != 0) {
							int i = 1;
							int j = 0;
							ps.setInt(i, k); i++;//ID
							ps.setInt(i, deliveryID); i++;//売上親ID
							ps.setInt(i, productID); i++; j++;//製作親ID
							ps.setInt(i, (Integer)v.get(j)); i++; j++;//製作子ID
							ps.setInt(i, tag); i++; j++;//表示CD
							ps.setString(i, (String)v.get(j)); i++; j += 3;//出荷伝票番号
							ps.setString(i, (String)v.get(j)); i++; j ++;//品名
							ps.setBoolean(i, (Boolean)v.get(j)); i++; j++;//各
							ps.setInt(i, (Integer)v.get(j)); i++; j++;//数量
							ps.setInt(i, (Integer)v.get(j)); i++; j++;//単位
							ps.setInt(i, (Integer)v.get(j)); i++; j++;//単価
							ps.setInt(i, (Integer)v.get(j)); i++; j++;//金額
							ps.setString(i, (String)v.get(j)); i++; //備考
							//ps.setDate(i, deliveryDTO.getDate()); i++; j = 1;//売上年月日
							//ps.setInt(i, (Integer)v.get(j)); i++; j = 0;//製作子ID
							//ps.setInt(i, (Integer)v.get(j));//製作親ID
							ps.addBatch();
							k++;
						}
					}
					int[] updateCounts = ps.executeBatch();
					lg.info("T_売上_子は" + updateCounts.length + "件処理されました。");
				} catch(SQLException ex) {
					isError = true;
					err.append(className + "テーブル「T_売上_子」の更新に失敗しました\n");
					Logging.logStackTrace(ex, lg, className);
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
			}
		}catch(Exception ex) {
			ex.printStackTrace();
			isError = true;lg.debug("error");
			lg.error(ex);
		} finally {
			if(isError) {
				lg.debug("rollback");
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
			out.writeObject(deliveryID);
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
