package fukaisystem;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.sql.Connection;
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

import fukaisystem.dto.DispatchingDTO;
import fukaisystem.sql.DBConnection;
import fukaisystem.util.Logging;

public class DispatchingRegister extends GenericServlet {

	private static final long serialVersionUID = 1L;
	private static final Logger lg = Logger.getLogger("A1");
	private static final String className = "DispatchingRegister\n";

	public void service(ServletRequest request, ServletResponse response) {
		DBConnection dbc = new DBConnection();
		Connection c = dbc.getConnection();
		Statement st = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		boolean isError = false;
		DispatchingDTO dto = null;
		StringBuilder err = new StringBuilder();

		int dispatchingID = 0;

		try {
			/**
			 * クライアントデータ受け取り
			 */
			ObjectInputStream in = new ObjectInputStream(request.getInputStream());
			Object obj = in.readObject();
			in.close();
			if (obj == null) {
				isError = true;
				err.append(className + "readObjectがnullです\n");
				lg.error(className + "readObjectがnullです");
			} else {
				if (obj instanceof DispatchingDTO) {
					dto = (DispatchingDTO) obj;
				} else {
					isError = true;
					err.append(className + "readObjectがDispatchDTO型ではありません\n");
					lg.error(className + "readObjectがDispatchDTO型ではありません");
				}
			}

			try {
				st = c.createStatement();
				st.executeUpdate("SET TRANSACTION ISOLATION LEVEL READ COMMITTED");
				st.executeUpdate("BEGIN TRANSACTION");
			} catch (SQLException ex) {
				isError = true;
				err.append(className + "トランザクションの開始に失敗しました\n");
				Logging.logStackTrace(ex, lg, className);
			}

			dispatchingID = dto.dispatchingID();
			if (dto.dispatchNum1() * dto.dispatchNum2() == 0) {
				// 注文期または注文番号を0に変更したということは、消去せよということ
				if (dispatchingID != 0) {
					try {
						ps = c.prepareStatement("DELETE FROM T_出庫_親 WHERE ID=?");
						ps.setInt(1, dispatchingID);
						ps.executeUpdate();
						ps = c.prepareStatement("DELETE FROM T_出庫_子 WHERE ID=?");
						ps.setInt(1, dispatchingID);
						ps.executeUpdate();
						dispatchingID = 0;
					} catch (SQLException ex) {
						isError = true;
						err.append(className + "出庫テーブルの削除に失敗しました\n");
						Logging.logStackTrace(ex, lg, className);
					}
				}
			} else {
				if (dispatchingID == 0) {
					try {
						ps = c.prepareStatement(
							"INSERT INTO T_出庫_親"
								+ " OUTPUT inserted.出庫親ID as newId, inserted.更新日"
								+ " VALUES(?, ?, ?, ?, ?, ?, ?, ?)"
						);
						int i = 1;
						ps.setInt(i++, dto.dispatchNum1()); // 製作期
						ps.setInt(i++, dto.dispatchNum2()); // 製作番号
						ps.setString(i++, dto.dispatchNum3()); // 製作枝番
						ps.setDate(i++, dto.date()); // 出庫年月日
						ps.setString(i++, dto.use()); // 用途
						ps.setString(i++, dto.remark()); // 用途2
						ps.setTimestamp(i++, new Timestamp(new java.util.Date().getTime())); // 更新日
						ps.setInt(i, 0); // 更新者CD
						boolean isResultSet = ps.execute();
						int updateCount = 0;
						while (true) {
							if (isResultSet) {
								rs = ps.getResultSet();
								while (rs.next()) {
									dispatchingID = rs.getInt(1);
								}
								rs.close();
							} else {
								updateCount = ps.getUpdateCount();
								if (updateCount == -1) {
									break;
								}
							}
							isResultSet = ps.getMoreResults();
						}
					} catch (SQLException ex) {
						isError = true;
						err.append(className + "テーブル「T_出庫_親」の更新に失敗しました\n");
						Logging.logStackTrace(ex, lg, className);
					}

				} else {
					try {
						ps = c.prepareStatement(
							"UPDATE T_出庫_親 SET 製作期=?, 製作番号=?, 製作枝番=?, 出庫年月日=?, 用途=?, 摘要=?, 更新日=?, 更新者CD=?"
								+ " WHERE 出庫親ID=?"
						);
						int i = 1;
						ps.setInt(i++, dto.dispatchNum1()); // 製作期
						ps.setInt(i++, dto.dispatchNum2()); // 製作番号
						ps.setString(i++, dto.dispatchNum3()); // 製作枝番
						ps.setDate(i++, dto.date()); // 出庫年月日
						ps.setString(i++, dto.use()); // 用途
						ps.setString(i++, dto.remark()); // 用途2
						ps.setTimestamp(i++, new Timestamp(new java.util.Date().getTime())); // 更新日
						ps.setInt(i++, 0); // 更新者CD
						ps.setInt(i, dispatchingID);
						ps.executeUpdate();
						ps = c.prepareStatement("DELETE FROM T_出庫_子 WHERE 出庫親ID=?");
						ps.setInt(1, dispatchingID);
						ps.executeUpdate();
					} catch (SQLException ex) {
						isError = true;
						err.append(className + "出庫テーブルの削除に失敗しました\n");
						Logging.logStackTrace(ex, lg, className);
					}
				}
				// UPDATE失敗したらINSERTさせない
				int k = 1;
				try {
					ps = c.prepareStatement("INSERT INTO T_出庫_子 VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?);"); // +
					// "UPDATE T_製作_子 SET 納品年月日=? WHERE ID=? AND 製作親ID=?");
					for (Vector<Object> record : dto.getVector()) {
						int i = 1;
						int j = 0;
						if (record.get(0) != null && (Integer) record.get(0) != 0) {
							ps.setInt(i++, k); // ID
							ps.setInt(i++, dispatchingID); // 出庫親ID
							ps.setInt(i++, (record.get(j) == null) ? 0 : (Integer) record.get(j)); j++; // 大分類CD
							ps.setInt(i++, (record.get(j) == null) ? 0 : (Integer) record.get(j)); j++; // 中分類CD
							ps.setInt(i++, (record.get(j) == null) ? 0 : (Integer) record.get(j)); j++; // 小分類CD
							ps.setString(i++, (String) record.get(j++)); // 名称
							ps.setBoolean(i++, (Boolean) record.get(j++)); // 各FLG
							ps.setDouble(i++, (Double) record.get(j++)); // 数量
							ps.setInt(i++, (Integer) record.get(j++)); // 数量単位CD
							ps.setDouble(i++, (Double) record.get(j++)); // 重量長さ（単位を要検討のこと）
							ps.setInt(i++, (Integer) record.get(j++)); // 単価
							ps.setInt(i++, (Integer) record.get(j++)); // 金額
							ps.setString(i++, (String) record.get(j++)); // 備考
							ps.setInt(i++, (Integer) record.get(j++)); // 在庫親ID
							ps.setInt(i, (Integer) record.get(j++)); // 在庫子ID
							ps.addBatch();
							k++;
						}
					}

					int[] updateCounts = ps.executeBatch();
					lg.info("T_出庫_子は" + updateCounts.length + "件処理されました。");
				} catch (SQLException ex) {
					isError = true;
					err.append(className + "テーブル「T_出庫_子」の更新に失敗しました\n");
					Logging.logStackTrace(ex, lg, className);
				}
			}

			if (!isError) {
				try {
					st = c.createStatement();
					st.executeUpdate("COMMIT");
				} catch (SQLException ex) {
					isError = true;
					err.append(className + "コミットに失敗しました\n");
					Logging.logStackTrace(ex, lg, className);
				}
			}
		} catch (Exception ex) {
			ex.printStackTrace();
			isError = true;
			lg.debug("error");
			lg.error(ex);
		} finally {
			if (isError) {
				lg.debug("rollback");
				try {
					st = c.createStatement();
					st.executeUpdate("ROLLBACK");
				} catch (SQLException ex) {
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
			out.writeObject(dispatchingID);
			out.writeUTF(err.toString());
			out.flush();
			out.close();
		} catch (Exception ex) {
			ex.printStackTrace();
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
