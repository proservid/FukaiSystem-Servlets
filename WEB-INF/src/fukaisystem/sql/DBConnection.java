package fukaisystem.sql;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;

import org.apache.log4j.Logger;
import org.apache.tomcat.dbcp.dbcp2.BasicDataSource;

import fukaisystem.util.Logging;

public class DBConnection {
	Connection c = null;
	DataSource ds = null;
	BasicDataSource bds = null;
	private static final Logger lg = Logger.getLogger("A1");
	private static final String className = "DBConnection\n";

	public DBConnection() {
		/**
		 * DB接続
		 */
		// *
		try {
			Context ctx = new InitialContext();
			ds = (DataSource) ctx.lookup("java:comp/env/jdbc/FukaiSystem");
			c = ds.getConnection();
		} catch (NamingException | SQLException ex) { // JNDI 未定義・接続失敗のどちらでも直接接続にフォールバックする
			Logging.logStackTrace(ex, lg, className);
			// */
			try {
				Class.forName("com.microsoft.sqlserver.jdbc.SQLServerDriver");
			} catch (ClassNotFoundException ex2) {
				Logging.logStackTrace(ex2, lg, className);
			}
			try {
				c = DriverManager.getConnection(
					"jdbc:sqlserver://localhost:1433;databaseName=FukaiSystem;user=sa;password=sqlexpress_2014"
				);
			} catch (SQLException ex2) {
				Logging.logStackTrace(ex2, lg, className);
			}
			// *
		}
		if (ds instanceof BasicDataSource) {
			bds = (BasicDataSource) ds;
			lg.debug("idle:" + bds.getNumIdle());
			lg.debug("active:" + bds.getNumActive());
		}
		// */
	}

	public Connection getConnection() {
		return c;
	}

	public int getNumActive() {
		return (bds != null) ? bds.getNumActive() : -1;
	}

	public int getNumIdle() {
		return (bds != null) ? bds.getNumIdle() : -1;
	}

}
