package fukaisystem.foundation;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
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
	private boolean isSent;

	/**
	 * すべてのサーブレットで共通となる処理の骨格
	 */
	@Override
	public void service(ServletRequest request, ServletResponse response) {
		err = new StringBuilder();
		isSent = false;
		try {

			ObjectInputStream in = new ObjectInputStream(request.getInputStream());
			// まずエラーオブジェクトが送信されていれば正規のアクセス
			Object error = in.readObject();
			// 不正アクセスへは "ok" を返す
			Object result = "ok";
			if (error instanceof Error) {
				result = core(response, in.readObject());
			}
			in.close();
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
			if (c == null) {
				reportException(response, new Exception("データベースに接続できません"));
				return null;
			}
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
			Object result = transaction(c, response, obj);
			if (result == null) {
				c.rollback();
				return null;
			}
			c.commit();
			return result;
		} catch (Exception e) { // SQLException 以外（NullPointerException 等）でも必ず巻き戻す
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
	 * 更新対象の行に更新ロックを掛ける
	 *
	 * <p>{@code UPDLOCK} で更新ロックを、{@code HOLDLOCK} で該当行がない場合の範囲ロックを取り、
	 * トランザクションが終わるまで保持する。読み取りから更新までの間に別のトランザクションが
	 * 割り込むのを防ぐため、読み取り・更新・削除を行う前に呼び出す。
	 * 更新ロックは互いに非互換のため、後から来たトランザクションは待たされる。
	 *
	 * <p>明細は必ず親を経由してしか操作されないため、親子にまたがる更新では親の行だけをロックすればよい。
	 * それ以外の理由で複数の行をロックする場合は、デッドロックを避けるため必ず親テーブルから順に呼び出すこと。
	 *
	 * <p>テーブル名・列名はプレースホルダで指定できないため文字列連結で組み立てる。
	 * いずれも呼び出し元が定数で与えるものに限り、入力値を渡してはならない。
	 *
	 * @param c Connectionオブジェクト
	 * @param table ロック対象のテーブル名
	 * @param condition ロック対象を絞り込む条件（WHERE 以降。プレースホルダを含んでよい）
	 * @param keys condition のプレースホルダに設定する値
	 *
	 * @throws SQLException
	 */
	protected void lock(Connection c, String table, String condition, Object... keys) throws SQLException {
		try (
			PreparedStatement ps = c.prepareStatement(
				"SELECT 1 FROM " + table + " WITH (UPDLOCK, HOLDLOCK) WHERE " + condition
			);
		) {
			for (int i = 0; i < keys.length; i++) {
				ps.setObject(i + 1, keys[i]);
			}
			try (ResultSet rs = ps.executeQuery();) {
				rs.next(); // ロックの取得が目的のため結果は使用しない
			}
		}
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
