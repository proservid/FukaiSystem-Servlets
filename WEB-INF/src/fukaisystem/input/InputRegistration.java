package fukaisystem.input;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Calendar;

import javax.servlet.GenericServlet;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;



import org.apache.log4j.Logger;

import fukaisystem.dto.InputDTO;
import fukaisystem.sql.DBConnection;
import fukaisystem.util.Logging;


public class InputRegistration extends GenericServlet {


	private static final long serialVersionUID = 1L;
    private static final Logger lg = Logger.getLogger("A1");
	private static final String className = "InputRegistration\n";
	private Date from, to, t0820, t1200, t1245, t1700, t1715;
	private int time;

	public void service(ServletRequest request, ServletResponse response) {

		DBConnection dbc = new DBConnection();
		Connection c = dbc.getConnection();
		PreparedStatement ps = null;
		ResultSet rs = null;
		InputDTO inputDTO = null;
		StringBuilder err = new StringBuilder();

		/**
		 * クライアントデータ受け取り
		 */
		ObjectInputStream in;
		try {
			in = new ObjectInputStream(request.getInputStream());
			Object obj = in.readObject();
			in.close();
			if(obj == null) {
				err.append(className + "readObjectがnullです\n");
				lg.error(className + "readObjectがnullです");
			} else {
				if(obj instanceof InputDTO) {
					inputDTO = (InputDTO)obj;
				} else {
					err.append(className + "readObjectがShippingDTO型ではありません\n");
					lg.error(className + "readObjectがShippingDTO型ではありません");
				}
			}
		} catch (Exception e) {
			// TODO 自動生成された catch ブロック
			e.printStackTrace();
		}

		int year = inputDTO.getInt(2);
		int month = inputDTO.getInt(3) - 1;
		int day = inputDTO.getInt(4);
		int fromT = inputDTO.getInt(5);
		int fromM = inputDTO.getInt(6);
		int toT = inputDTO.getInt(7);
		int toM = inputDTO.getInt(8);
		int rest = inputDTO.getInt(9);
		int id = inputDTO.getInt(10);
		Calendar cal = Calendar.getInstance();
		cal.set(year, month, day, fromT, fromM, 0);
		cal.set(Calendar.MILLISECOND, 0);
		from = new Date(cal.getTimeInMillis());
		cal.set(year, month, day, toT, toM, 0);
		cal.set(Calendar.MILLISECOND, 0);
		to = new Date(cal.getTimeInMillis());

		try {
			ps = c.prepareStatement("SELECT * FROM M_就業時間");
			rs = ps.executeQuery();
			if(rs.next()) {
				int w0 = rs.getInt("始業時");
				int w1 = rs.getInt("始業分");
				int w2 = rs.getInt("終業時");
				int w3 = rs.getInt("終業分");
				int w4 = rs.getInt("休憩始時");
				int w5 = rs.getInt("休憩始分");
				int w6 = rs.getInt("休憩終時");
				int w7 = rs.getInt("休憩終分");
				int w8 = rs.getInt("残業始時");
				int w9 = rs.getInt("残業始分");
				//基本情報
				cal.set(year, month, day, w0, w1, -1);
				t0820 = new Date(cal.getTimeInMillis());
				cal.set(year, month, day, w4, w5, -1);
				t1200 = new Date(cal.getTimeInMillis());
				cal.set(year, month, day, w6, w7, -1);
				t1245 = new Date(cal.getTimeInMillis());
				cal.set(year, month, day, w2, w3, -1);
				t1700 = new Date(cal.getTimeInMillis());
				cal.set(year, month, day, w8, w9, -1);
				t1715 = new Date(cal.getTimeInMillis());

				int br1 = (int)((t1245.getTime() - t1200.getTime()) / (1000 * 60));//45分
				int br2 = (int)((t1715.getTime() - t1700.getTime()) / (1000 * 60));//15分

				if(inputDTO.getString(2).equals("17")) {//出張の場合
					cal.setTime(to);
					cal.add(Calendar.MINUTE, rest*(-1));
					setTime(cal.getTimeInMillis() - from.getTime());
				} else {
					if(from.before(t0820)) {
						//開始を0820に
						cal.set(year, month, day, w0, w1, 0);
						long adjustedFrom = cal.getTimeInMillis();
						if(to.after(t1715)){
							//終了から45分+15分=1時間引く
							cal.setTime(to);
							cal.add(Calendar.MINUTE, -br1-br2);
						} else if(to.after(t1700)) {
							//45分引き、終りを1700で計算
							cal.set(year, month, day, w2, w3, 0);
							cal.add(Calendar.MINUTE, -br1);
						} else if(to.after(t1245)) {
							//45分引くのみ
							cal.setTime(to);
							cal.add(Calendar.MINUTE, -br1);
						} else if(to.before(t1200)) {
							//そのまま
							cal.setTime(to);
						} else {
							//終了を1200に
							cal.set(year, month, day, w4, w5, 0);
						}
						setTime(cal.getTimeInMillis() - adjustedFrom);
					} else if(from.before(t1200)) {
						if(to.after(t1715)){
							//終了から45分+15分=1時間引く
							cal.setTime(to);
							cal.add(Calendar.MINUTE, -br1-br2);
						} else if(to.after(t1700)) {//TODO 1659
							//終了を1700とし、45分引く
							cal.set(year, month, day, w2, w3, 0);
							cal.add(Calendar.MINUTE, -br1);
						} else if(to.after(t1245)) {
							//終了から45分引く
							cal.setTime(to);
							cal.add(Calendar.MINUTE, -br1);
						} else if(to.after(t1200)) {
							//終了を1200に
							cal.set(year, month, day, w4, w5, 0);
						} else {
							//終了が12時前ならそのまま
							cal.setTime(to);
						}
						setTime(cal.getTimeInMillis() - from.getTime());
					} else if(from.after(t1715)) {
						//一切調整不要
						setTime(to.getTime() - from.getTime());
					} else if(from.after(t1700)) {
						//開始を1715にするのみ
						cal.set(year, month, day, w8, w9, 0);
						setTime(to.getTime() - cal.getTimeInMillis());
					} else if(from.after(t1245)) {
						//開始時刻は調整不要
						if(to.after(t1715)) {
							cal.setTime(to);
							cal.add(Calendar.MINUTE, -br2);//終了時間から15分引く
							setTime(cal.getTimeInMillis() - from.getTime());//
						} else if(to.after(t1700)) {
							//終了を1700に
							cal.set(year, month, day, w2, w3);
							setTime(cal.getTimeInMillis() - from.getTime());
						} else {
							setTime(to.getTime() - from.getTime());
						}
					} else {//12時から12時45分の間に始まっていたら
						//開始を1245に
						cal.set(year, month, day, w6, w7, 0);
						long adjustedFrom = cal.getTimeInMillis();
						if(to.after(t1715)) {
							cal.setTime(to);
							cal.add(Calendar.MINUTE, -br2);//15分引く
							setTime(cal.getTimeInMillis() - adjustedFrom);
						} else if(to.after(t1700)) {
							//終了を1700に
							cal.set(year, month, day, w2, w3, 0);
							setTime(cal.getTimeInMillis() - adjustedFrom);
						} else {
							setTime(to.getTime() - adjustedFrom);
						}
					}
				}
			}
		} catch(SQLException ex) {
			ex.printStackTrace();
			err.append(className + "テーブル「M_就業時間」の読み出しに失敗しました\n");
			Logging.logStackTrace(ex, lg, className);
		}


		if(id == 0) {
			try {
				ps = c.prepareStatement("INSERT INTO T_加工実績 (製作期,製作番号,製作枝番,加工CD,時間,着手日時,終了日時,単価,担当者CD,備考,入力日時)"
						+ " VALUES(?,?,?,?,?,?,?,"
						+ "(select 単価 from M_加工_単価 wp1 where wp1.CD=? and 適用開始日<? AND NOT EXISTS ("
						+ "	SELECT 1 FROM M_加工_単価 wp2"
						+ "	WHERE wp1.適用開始日<wp2.適用開始日 AND wp1.CD=wp2.CD AND 適用開始日<?)),"
						+ "?,?,?)");
				/*
				"MERGE INTO T_加工実績2 AS t"+//(製作期,製作番号,製作枝番,加工CD,時間,着手日時,終了日時,担当者CD,備考)" +
				" USING (SELECT ? AS 製作期, ? AS 製作番号, ? AS 製作枝番, ? AS 加工CD, ? AS 時間, ? AS 着手日時, ? AS 終了日時, ? AS 担当者CD, ? AS 備考) AS w" +
				" ON t.着手日時=w.着手日時 AND t.担当者CD=w.担当者CD" +
				" WHEN MATCHED THEN" +
				"   UPDATE SET t.製作期=w.製作期, t.製作番号=w.製作番号, t.製作枝番=w.製作枝番, t.加工CD=w.加工CD, t.時間=w.時間, t.着手日時=w.着手日時, t.終了日時=w.終了日時, t.担当者CD=w.担当者CD, t.備考=w.備考, t.入力年月日=?" +
				" WHEN NOT MATCHED THEN" +
				"   INSERT VALUES(w.製作期, w.製作番号, w.製作枝番, w.加工CD, w.時間, w.着手日時, w.終了日時, w.担当者CD, w.備考, ?)" +
				" OUTPUT deleted.ID as oldId, inserted.着手日時 as newId;");
	*/
				int i = 1;
				ps.setInt(i++, inputDTO.getInt(1) == 0 ? 0 : inputDTO.getInt(0)); //番号が0なら期も0
				ps.setInt(i++, inputDTO.getInt(1));
				ps.setString(i++, inputDTO.getInt(1) == 0 ? "" : inputDTO.getString(3)); //番号が0なら枝番なし
				ps.setString(i++, inputDTO.getString(2)); //加工CD
				ps.setInt(i++, time); //時間
				ps.setTimestamp(i++, new Timestamp(from.getTime())); //着手日時
				ps.setTimestamp(i++, new Timestamp(to.getTime())); //終了日時

				ps.setString(i++, inputDTO.getString(2)); //加工CD
				ps.setTimestamp(i++, new Timestamp(from.getTime())); //着手日時
				ps.setTimestamp(i++, new Timestamp(from.getTime())); //着手日時

				ps.setString(i++, inputDTO.getString(1)); //担当者CD
				ps.setString(i++, inputDTO.getString(2).equals("17") ? "休憩"+rest+"分" : ""); //備考
				ps.setTimestamp(i, new Timestamp(new java.util.Date().getTime()));
				ps.executeUpdate();
	 		} catch(SQLException ex) {
				ex.printStackTrace();
				err.append(className + "テーブル「T_加工実績」の更新に失敗しました\n");
				Logging.logStackTrace(ex, lg, className);
			}
		} else {
			try {
				ps = c.prepareStatement("UPDATE T_加工実績  SET 製作期=?,製作番号=?,製作枝番=?,加工CD=?,時間=?,着手日時=?,終了日時=?,"
						+ "単価=(select 単価 from M_加工_単価 p where p.CD=加工CD and 適用開始日<? AND NOT EXISTS ("
						+ "	SELECT 1 FROM M_加工_単価 wp2"
						+ "	WHERE wp1.適用開始日<wp2.適用開始日 AND wp1.CD=wp2.CD AND 適用開始日<?)),"
						+ "担当者CD=?,備考=?,入力日時=? WHERE ID=?");
				int i = 1;
				ps.setInt(i++, inputDTO.getInt(1) == 0 ? 0 : inputDTO.getInt(0)); //番号が0なら期も0
				ps.setInt(i++, inputDTO.getInt(1));
				ps.setString(i++, inputDTO.getInt(1) == 0 ? "" : inputDTO.getString(3)); //番号が0なら枝番なし
				ps.setString(i++, inputDTO.getString(2)); //加工CD
				ps.setInt(i++, time); //時間
				ps.setTimestamp(i++, new Timestamp(from.getTime())); //着手日時
				ps.setTimestamp(i++, new Timestamp(to.getTime())); //終了日時
				ps.setTimestamp(i++, new Timestamp(from.getTime())); //着手日時
				ps.setTimestamp(i++, new Timestamp(from.getTime())); //着手日時
				ps.setString(i++, inputDTO.getString(1)); //担当者CD
				ps.setString(i++, inputDTO.getString(2).equals("17") ? "休憩"+rest+"分" : ""); //備考
				ps.setTimestamp(i++, new Timestamp(new java.util.Date().getTime()));
				ps.setInt(i, id);
				ps.executeUpdate();
	 		} catch(SQLException ex) {
				ex.printStackTrace();
				err.append(className + "テーブル「T_加工実績」の更新に失敗しました\n");
				Logging.logStackTrace(ex, lg, className);
			}
		}


		/**
		 * クライアントに送信
		 */
		try {
			response.setContentType("application/octet-stream");
			ObjectOutputStream out = new ObjectOutputStream(response.getOutputStream());
			//未入力の製番があるので当面製番が見つかりませんエラーを出さない
			//out.writeObject(inputDTO.getInt(1) == 0 ? -1 : update);//製作番号が0なら、該当する製作データが登録されていなくてもOKにする
			out.writeObject(-1);//製作番号が0なら、該当する製作データが登録されていなくてもOKにする
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

	private void setTime(long value) {
		long remainder = value % (1000 * 60 * 15);//ミリ秒を100分の1秒にして分にして15分単位にする
		//３捨４入
		time = ((float)remainder / (1000 * 60 * 15) >= 0.4) ? ((int)(value / (1000 * 60 * 15)) + 1) * 25 : ((int)(value / (1000 * 60 * 15))) * 25;
	}

}
