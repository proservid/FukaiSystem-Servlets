package fukaisystem.print;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import javax.servlet.ServletResponse;

import com.proservid.print.dao.GetFormatDAO;

import fukaisystem.ServiceFoundation;

/**
 * 帳票名とフォーマット名の対応表をdataVectorで取得する
 */
public class GetLinks extends ServiceFoundation {
	protected static final String className = "GetLinks";

	@Override
    public Object access(Connection c, ServletResponse response, Object o) throws IOException, SQLException {
        return GetFormatDAO.getLinks(c);
    }

}
