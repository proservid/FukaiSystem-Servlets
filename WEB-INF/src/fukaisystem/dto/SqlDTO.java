package fukaisystem.dto;

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */


import java.io.Serializable;

/**
 *
 * @author kameura
 */
public class SqlDTO implements Serializable {

	private String str;
	private boolean isQuery;

	public SqlDTO(String str, boolean isQuery) {
		this.str = str;
		this.isQuery = isQuery;
	}

	public String getString() {
		return str;
	}

	public boolean isQuery() {
		return isQuery;
	}

}

