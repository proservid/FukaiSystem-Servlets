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

	private int id;
	private String titleTableName, dataTableName;
	private List<List<String>> titleTable, dataTable;

	public PrintSlipDTO(int id, String titleTableName, String dataTableName) {
		this.id = id;
		this.titleTableName = titleTableName;
		this.dataTableName = dataTableName;
	}

	public void setTable(List<List<String>> titleTable, List<List<String>> dataTable) {
		this.titleTable = titleTable;
		this.dataTable = dataTable;
	}

	public String titleTableName() {
		return titleTableName;
	}

	public String dataTableName() {
		return dataTableName;
	}

	public List<List<String>> titleTable() {
		return titleTable;
	}

	public List<List<String>> dataTable() {
		return dataTable;
	}

	public int id() {
		return id;
	}

}
