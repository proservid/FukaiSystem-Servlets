package fukaisystem.application.staff;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Vector;

import javax.servlet.ServletResponse;

import fukaisystem.dto.staff.DMDTO;
import fukaisystem.foundation.ServiceFoundation;

/**
 * 指定した部署の人員データを登録・変更する
 *
 * <p>複数件をひとつのトランザクションで処理する。自動コミットのままでは
 * 途中で失敗したときに一部だけ登録された状態が確定してしまう。
 *
 * <p>MERGE の対象には {@code WITH (HOLDLOCK)} を指定する。既定の READ COMMITTED では
 * 一致判定のために取った共有ロックが判定直後に解放されるため、判定と登録の間に
 * 同じ人員CDの登録が割り込み、主キー違反になることがある。
 */
public class SetMember extends ServiceFoundation {

	private DMDTO dmd;
	private String dept;
	private Vector<Vector<Object>> data;

	@Override
	public Object transaction(Connection c, ServletResponse response, Object o) throws IOException, SQLException {

		dmd = cast(response, o, DMDTO.class);
		if (dmd == null) {
			return null; // cast() がクライアントに送信済み
		}
		dept = dmd.getName().keySet().iterator().next();
		data = dmd.getName().get(dept);

		try (
			PreparedStatement ps = c.prepareStatement(
				"MERGE INTO M_人員 WITH (HOLDLOCK) AS m"
					+ " USING (SELECT ? AS CD, ? AS 姓, ? AS 名, ? AS 所属部署CD, ? AS 表示CD, ? AS 在籍FLG) AS temp"
					+ "  ON m.CD=temp.CD AND temp.CD<>0 "
					+ " WHEN MATCHED THEN"
					+ " UPDATE SET m.姓=temp.姓, m.名=temp.名, m.所属部署CD=temp.所属部署CD,"
					+ " m.表示CD=temp.表示CD, m.在籍FLG=temp.在籍FLG"
					+ " WHEN NOT MATCHED THEN"
					+ " 	INSERT VALUES(temp.CD, temp.姓, temp.名, temp.所属部署CD, temp.表示CD, 0, temp.在籍FLG, '00000', '00000');"
			);
		) {
			int i = 1;
			for (Vector<Object> record : data) {
				if (record.get(0).toString().equals("0") || (record.get(1).equals("") && record.get(2).equals(""))) {
				} else {
					ps.setInt(1, (Integer)record.get(0)); // CD
					ps.setString(2, (String)record.get(1)); // 姓
					ps.setString(3, (String)record.get(2)); // 名
					ps.setString(4, dept); // 所属部署CD
					ps.setInt(5, i++); // 表示CD
					ps.setBoolean(6, (Boolean)record.get(3)); // 在籍FLG
					ps.addBatch();
				}
			}
			return ps.executeBatch().length;
		}
	}

}
