package fukaisystem.dto;


import java.io.Serializable;
import java.util.List;

/**
 * DBのテーブルとJTableとのアダプタークラス
 * @author kameura
 */
public class TableAdapter implements Serializable {
	private static final long serialVersionUID = 1L;
	Object obj2;
	List<String> keys;
	List<ColInfoDTO> colInfos;
	List<List<Object>> contents;
	int keySize;
	int titleSize;
	int contentSize;

	public TableAdapter(List<String> keys, List<ColInfoDTO> colInfos,  List<List<Object>> contents) {
		this.keys = keys;
		keySize = keys.size();
		this.colInfos = colInfos;
		titleSize = colInfos.size();
		this.contents = contents;
		contentSize = contents.size();
	}

	public List<String> getKeys() {
		return keys;
	}

	public List<ColInfoDTO> getColInfos() {
		return colInfos;
	}

	public List<List<Object>> getContents() {
		return contents;
	}

	/**
	 * プライマリキーの列名
	 * @return
	 */
	public String[] getKeyNames() {
		String[] primaryKeys = new String[keySize];
		for(int i = 0; i < keySize; i++) {
			primaryKeys[i] = String.valueOf(keys.get(i));
		}
			return primaryKeys;
	}

	public String[] getDataTitles() {
		String[] colNames = null;
		if(colInfos != null) {
			colNames = new String[titleSize];
			for(int i = 0; i < titleSize; i++) {
				ColInfoDTO ci = colInfos.get(i);
				colNames[i] = (ci.getColName());
			}
		}
		return colNames;
	}

	public int[] getDataTypes() {
		int[] colTypes = null;
		if(colInfos != null) {
			colTypes = new int[titleSize];
			for(int i = 0; i < titleSize; i++) {
				ColInfoDTO ci = colInfos.get(i);
				colTypes[i] = (ci.getColType());
			}
		}
		return colTypes;
	}

	public String[] getDataTypeNames(boolean isSizeAdd) {
		String[] colTypeNames = null;
		if(colInfos != null) {
			colTypeNames = new String[titleSize];
			for(int i = 0; i < titleSize; i++) {
				ColInfoDTO ci = colInfos.get(i);
				colTypeNames[i] = (ci.getColTypeName(isSizeAdd));
			}
		}
		return colTypeNames;
	}

	public String[] getDataTitlesAndTypeNames(boolean isSizeAdd) {
		String[] colTitlesTypeNames = null;
		if(colInfos != null) {
			colTitlesTypeNames = new String[titleSize];
			for(int i = 0; i < titleSize; i++) {
				ColInfoDTO ci = colInfos.get(i);
				colTitlesTypeNames[i] = ci.getColName() + " [" + ci.getColTypeName(isSizeAdd) + "]";
			}
		}
		return colTitlesTypeNames;
	}

	public int[] getDataSizes() {
		int[] colSizes = null;
		if(colInfos != null) {
			colSizes = new int[titleSize];
			for(int i = 0; i < titleSize; i++) {
				ColInfoDTO ci = colInfos.get(i);
				colSizes[i] = (ci.getColSize());
			}
		}
		return colSizes;
	}

	/**
	 * テーブルデータ
	 * @return
	 */
	public Object[][] getArrayData() {

		Object[][] rowData = new Object[contentSize][titleSize];
		for(int i = 0; i < contentSize; i++) {
			List<Object> row = contents.get(i);
			for(int j = 0; j < row.size(); j++) {
				rowData[i][j] = row.get(j);
			}
		}
		return rowData;
	}

	/**
	 * テーブルデータの最終行に空白を追加したデータ
	 * @return
	 */
	public Object[][] getEditData() {
		Object[][] rowData = new Object[contentSize + 1][titleSize];
		for(int i = 0; i < contentSize; i++) {
			List<Object> row = contents.get(i);
			for(int j = 0; j < row.size(); j++) {
				rowData[i][j] = row.get(j);
			}
		}
		return rowData;
	}

	/**
	 * 用途不明、最初の列のみの配列を返す？
	 * @return
	 */
	public String[] getComboData() {
		String[] comboData = {""};
		comboData = new String[contentSize];
		for(int i = 0; i < contentSize; i++) {
			comboData[i] = String.valueOf(contents.get(i).get(0));
		}
		return comboData;
	}

	public int getKeySize() {
		return keySize;
	}
	public int getTitleSize() {
		return titleSize;
	}
	public int getContentSize() {
		return contentSize;
	}
}