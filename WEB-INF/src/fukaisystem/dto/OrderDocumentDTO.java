/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package fukaisystem.dto;

import java.io.Serializable;
import java.sql.Date;
import java.util.Vector;

/**
 *
 * @author kameura
 */
public class OrderDocumentDTO implements Serializable {

	private Vector<Vector<Object>> dataVector;
	private String accountName, orderNum3, note1, note2;
	private int accountID, orderNum1, orderNum2, orderSlipNum, orderID;
	private Date publishDate, dueDate;

	public OrderDocumentDTO(
		Vector<Vector<Object>> dataVector,
		String accountName,
		String orderNum3,
		String note1,
		String note2,
		int accountID,
		int orderNum1,
		int orderNum2,
		int orderSlipNum,
		int orderID,
		Date publishDate,
		Date dueDate
	) {
		this.dataVector = dataVector;
		this.accountName = accountName;
		this.orderNum3 = orderNum3;
		this.note1 = note1;
		this.note2 = note2;
		this.accountID = accountID;
		this.orderNum1 = orderNum1;
		this.orderNum2 = orderNum2;
		this.orderSlipNum = orderSlipNum;
		this.orderID = orderID;
		this.publishDate = publishDate;
		this.dueDate = dueDate;
	}

	public Vector<Vector<Object>> getDataVector() {
		return dataVector;
	}

	public String accountName() {
		return accountName;
	}

	public String orderNum3() {
		return orderNum3;
	}

	public String note1() {
		return note1;
	}

	public String note2() {
		return note2;
	}

	public int accountID() {
		return accountID;
	}

	public int orderNum1() {
		return orderNum1;
	}

	public int orderNum2() {
		return orderNum2;
	}

	public int orderSlipNum() {
		return orderSlipNum;
	}

	public int orderID() {
		return orderID;
	}

	public Date publishDate() {
		return publishDate;
	}

	public Date dueDate() {
		return dueDate;
	}

}
