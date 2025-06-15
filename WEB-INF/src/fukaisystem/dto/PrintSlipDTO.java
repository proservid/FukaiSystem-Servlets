/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package fukaisystem.dto;

import java.io.Serializable;
import java.util.List;

/**
 *
 * @author kameura
 */
public class PrintSlipDTO implements Serializable {
	int id;
	String titleTableName, dataTableName;
	List<List<String>> titleTable, dataTable;

	public PrintSlipDTO(int id, String titleTableName, String dataTableName) {
		this.id = id;
		this.titleTableName = titleTableName;
		this.dataTableName = dataTableName;
	}

	public void setTable(List<List<String>> titleTable, List<List<String>> dataTable) {
		this.titleTable = titleTable;
		this.dataTable = dataTable;
	}

	public String getTableName(int order) {
		String s = "";
		switch (order) {
			case 0:
				s = titleTableName;
				break;
			case 1:
				s = dataTableName;
				break;
		}
		return s;
	}

	public List<List<String>> getTable(int order) {
		List<List<String>> table = null;
		switch (order) {
			case 0:
				table = titleTable;
				break;
			case 1:
				table = dataTable;
				break;
		}
		return table;
	}

	public int getID() {
		return id;
	}

}
