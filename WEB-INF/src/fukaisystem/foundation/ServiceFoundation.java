package fukaisystem.foundation;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.sql.Connection;
import java.sql.SQLException;

import javax.servlet.GenericServlet;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import org.apache.log4j.Logger;

import fukaisystem.sql.DBConnection;
import fukaisystem.util.Logging;

public abstract class ServiceFoundation extends GenericServlet {

	protected static final Logger logger = Logger.getLogger("A1");
	protected static final int isolationLevel = Connection.TRANSACTION_READ_COMMITTED;
	private StringBuilder err;
	private boolean isSent = false;

	/**
	 * すべてのサーブレットで共通となる処理の骨格
	 */
	@Override
	public void service(ServletRequest request, ServletResponse response) {
		err = new StringBuilder();
		try {

			ObjectInputStream in = new ObjectInputStream(request.getInputStream());
			Object obj = in.readObject();
			in.close();
			Object result = core(response, obj);
			if (!isSent) { // reportException で送信済みの場合は isSent が true
				send(response, result);
			}

		} catch (Exception e) {
			handleException(e);
		}
	}

	/**
	 * 中心となる処理
	 * DBへのアクセスが不要な場合はこのメソッドをオーバーライドする
	 * 
	 * @param obj 未キャストの入力DTO
	 * 
	 * @return クライアントに返す出力DTO
	 * @throws IOException
	 */
	protected Object core(ServletResponse response, Object obj) throws IOException {
		try (Connection c = getConnection();) {
			return access(c, response, obj);
		} catch (Exception e) {
			reportException(response, e);
		}
		return null;
	}

	/**
	 * DBへの処理
	 * トランザクションが不要な場合はこのメソッドをオーバーライドする
	 * 
	 * @param c Connectionオブジェクト
	 * @param response ServletResponseオブジェクト
	 * @param obj 未キャストの入力DTO
	 * 
	 * @return クライアントに返す出力DTO
	 * @throws Exception
	 */
	protected Object access(Connection c, ServletResponse response, Object obj) throws Exception {
		c.setTransactionIsolation(isolationLevel);
		c.setAutoCommit(false); // begin();
		try {
			return transaction(c, response, obj);
		} catch (SQLException e) {
			c.rollback();
			throw e; // core で処理
		}
	}

	/**
	 * トランザクション処理
	 * トランザクションが必要な場合はこのメソッドをオーバーライドする
	 * 
	 * @param c Connectionオブジェクト
	 * @param response ServletResponseオブジェクト
	 * @param obj 未キャストの入力DTO
	 * 
	 * @return クライアントに返す出力DTO
	 * @throws Exception
	 */
	protected Object transaction(Connection c, ServletResponse response, Object obj) throws Exception {
		return null;
	}

	/**
	 * コネクションを取得する
	 * 
	 * @return Connectionオブジェクト
	 */
	protected Connection getConnection() {
		return new DBConnection().getConnection();
	}

	/**
	 * オブジェクトが指定クラスのインスタンスである場合にはキャストして返し、そうでない場合には {@code null} を返す
	 * 
	 * @param <T> 期待される型
	 * @param response ServletResponseオブジェクト
	 * @param obj 対象オブジェクト
	 * @param clazz 判定対象の型クラス
	 * 
	 * @return オブジェクトが指定クラスのインスタンスである場合にはその型にキャストされたオブジェクト、そうでなければ {@code null}
	 * @throws IOException
	 */
	@SuppressWarnings("unchecked")
	public <T> T cast(ServletResponse response, Object obj, Class<T> clazz) throws IOException {
		if (clazz.isInstance(obj)) {
			return (T) obj;
		}
		send(response, new Exception(String.format("[%s] DTO が %s 型ではありません\n", getClass().getName(), clazz.getSimpleName())));
		return null;
	}

	/**
	 * クライアントにオブジェクトとエラーを送信する
	 * エラーは getErrors() の内容が送信される（指定不可）
	 * 
	 * @param response ServletResponseオブジェクト
	 * @param obj 送信するオブジェクト
	 * 
	 * @throws IOException
	 */
	protected void send(ServletResponse response, Object obj) throws IOException {
		String message;
		if (obj instanceof Exception) {
			message = ((Exception) obj).getMessage(); // extendedMessage はシリアル化できないので
		} else {
			message = getErrors();
			if (!message.isEmpty()) {
				obj = new Exception();
			}
		}
		response.setContentType("application/octet-stream");
		ObjectOutputStream out = new ObjectOutputStream(response.getOutputStream());
		out.writeObject(obj);
		out.writeUTF(message);
		out.flush();
		out.close();
	}

	/**
	 * 発生した例外をクライアントに送信する
	 * 
	 * @param response ServletResponseオブジェクト
	 * @param e Exceptionオブジェクト
	 * 
	 * @throws IOException
	 */
	protected void reportException(ServletResponse response, Exception e) throws IOException {
		send(response, e);
		isSent = true;
	}

	/**
	 * 例外の最終処理（クライアントに送信できないのでログに記録するのみ）
	 * 
	 * @param e Exceptionオブジェクト
	 */
	protected void handleException(Exception e) {
		Logging.logStackTrace(e, logger, getClass().getName());
	}

	/**
	 * エラーを追加する
	 * 
	 * @param e 追加するエラー
	 */
	protected void addError(String e) {
		err.append(e + " ");
		logger.error(e + " ");
	}

	/**
	 * それまでに発生したすべてのエラーを、エラーごとに改行コードで区切られた文字列で取得する
	 * 
	 * @return 発生したすべてのエラーの文字列
	 */
	protected String getErrors() {
		return err.toString();
	}
}
