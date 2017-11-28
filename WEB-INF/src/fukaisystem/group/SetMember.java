package fukaisystem.group;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;
import java.util.Vector;

import javax.servlet.GenericServlet;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import org.apache.log4j.Logger;

import fukaisystem.dto.DMDTO;
import fukaisystem.sql.DBConnection;
import fukaisystem.util.Logging;

public class SetMember extends GenericServlet {

	private static final long serialVersionUID = 1L;
    private static final Logger lg = Logger.getLogger("A1");
	private static final String className = "SetMember\n";
	private DMDTO dmd;
	private String dept;
	private Vector<Vector> data;
	private int[] updateCounts;

	public void service(ServletRequest request, ServletResponse response) {

		DBConnection dbc = new DBConnection();
		Connection c = dbc.getConnection();
		PreparedStatement ps = null;
		ResultSet rs = null;
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
				if(obj instanceof DMDTO) {
					dmd = (DMDTO)obj;
					dept = dmd.getName().keySet().iterator().next();
					data = dmd.getName().get(dept);
				} else {
					err.append("型が一致しません\n");
					lg.error(className + "型が一致しません");
				}
			}

			try {
				ps = c.prepareStatement( "MERGE INTO M_人員 AS m" +
						 " USING (SELECT ? AS 管理ID, ? AS CD, ? AS 姓, ? AS 名, ? AS 所属部署CD, ? AS 表示CD, ? AS 在籍FLG) AS temp" +
						 "  ON m.管理ID=temp.管理ID and temp.CD<>0" +
						 " WHEN MATCHED THEN" +
						 " UPDATE SET m.CD=temp.CD, m.姓=temp.姓, m.名=temp.名, m.所属部署CD=temp.所属部署CD," +
						 " m.表示CD=temp.表示CD, m.在籍FLG=temp.在籍FLG" +
						 " WHEN NOT MATCHED THEN" +
						 " 	INSERT VALUES(temp.CD, temp.姓, temp.名, temp.所属部署CD, temp.表示CD, 0, temp.在籍FLG, '00000', '00000');");
				int i = 1;
				for(Vector v : data) {
					if(v.get(1).toString().equals("0") || (v.get(2).equals("") && v.get(3).equals(""))) {
					} else {
						ps.setInt(1, (Integer)v.get(0)); //管理ID
						ps.setInt(2, (Integer)v.get(1)); //CD
						ps.setString(3, (String)v.get(2)); //姓
						ps.setString(4, (String)v.get(3)); //名
						ps.setString(5, dept); //所属部署CD
						ps.setInt(6, i++); //表示CD
						ps.setBoolean(7, (Boolean)v.get(4)); //在籍FLG
						ps.addBatch();
					}
				}
				updateCounts = ps.executeBatch();

	 		} catch(SQLException ex) {
				ex.printStackTrace();
				err.append(className + "更新に失敗しました\n");
				Logging.logStackTrace(ex, lg, className);
			}

		} catch (Exception e) {
			// TODO 自動生成された catch ブロック
			e.printStackTrace();
		}

		/**
		 * クライアントに送信
		 */
		try {
			response.setContentType("application/octet-stream");
			ObjectOutputStream out = new ObjectOutputStream(response.getOutputStream());

			out.writeObject(updateCounts.length);//製作番号が0なら、該当する製作データが登録されていなくてもOKにする
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
