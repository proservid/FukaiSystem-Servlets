package fukaisystem.dto;

import java.io.Serializable;
import java.sql.Types;

/**
 *
 * @author kameura
 */
public class ColInfoDTO implements  Serializable {

	private static final long serialVersionUID = 1L;
	String colName, colTypeName;
	int colType, colSize;
	String colSizeTxt = "";

	public ColInfoDTO(String colName, String colTypeName, int colType, int colSize) {
		this.colName = colName;
		this.colTypeName = colTypeName;
		this.colType = colType;
		this.colSize = colSize;

		if(colType == Types.CHAR || colType == Types.VARCHAR || colType == Types.NCHAR || colType == Types.NVARCHAR) {
			colSizeTxt = "(" + colSize + ")";
		} else if(colType == Types.LONGVARCHAR || colType == Types.LONGNVARCHAR) {
			colSizeTxt = "(max)";
		}

	}

	public String getColName() {
		return colName;
	}
	public String getColTypeName(boolean isSizeAdd) {
		if(isSizeAdd) {
			return colTypeName + colSizeTxt;
		} else {
			return colTypeName;
		}
	}
	public int getColType() {
		return colType;
	}

	public int getColSize() {
		return colSize;
	}
}
