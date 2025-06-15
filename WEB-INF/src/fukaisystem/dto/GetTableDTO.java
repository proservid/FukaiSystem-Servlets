/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package fukaisystem.dto;

import java.io.Serializable;

/**
 *
 * @author kameura
 */
public class GetTableDTO implements Serializable {
	String tableName;
	String columnName;
	String[] condition;
	String[] signs;
	String[] id;
	String order;

	public GetTableDTO(
		String tableName,
		String columnName,
		String[] condition,
		String[] signs,
		String[] id,
		String order
	) {
		this.tableName = tableName;
		this.columnName = columnName;
		this.condition = condition;
		this.signs = signs;
		this.id = id;
		this.order = order;
	}

	public String getString(int i) {
		String s = "";
		switch (i) {
			case 0:
				s = tableName;
				break;
			case 1:
				s = columnName;
				break;
		}
		return s;
	}

	public String[] getKeys() {
		return condition;
	}

	public String[] getIDs() {
		return id;
	}

	public String[] getSigns() {
		return signs;
	}

	public String getOrder() {
		return order;
	}
}
