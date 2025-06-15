/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package fukaisystem.dto;

import java.sql.Date;
import java.util.List;
import java.util.Vector;

/**
 *
 * @author kameura
 */
public class OrderDocumentDTO extends BasicDTO {
	Vector<Vector<Object>> vector;
	String accountName, orderNum3, note1, note2;
	int accountID, orderNum1, orderNum2, orderSlipNum, orderID;
	Date publishDate, dueDate;

	public OrderDocumentDTO(
		Vector<Vector<Object>> vector,
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
		this.vector = vector;
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

	@Override
	public Vector<Vector<Object>> getVector(int order) {
		return vector;
	}

	@Override
	public String getStr(int order) {
		String s = "";
		switch (order) {
			case 0:
				s = accountName;
				break;
			case 1:
				s = orderNum3;
				break;
			case 2:
				s = note1;
				break;
			case 3:
				s = note2;
				break;
		}
		return s;
	}

	@Override
	public int getInt(int order) {
		int i = 0;
		switch (order) {
			case 0:
				i = accountID;
				break;
			case 1:
				i = orderNum1;
				break;
			case 2:
				i = orderNum2;
				break;
			case 3:
				i = orderSlipNum;
				break;
			case 4:
				i = orderID;
				break;
		}
		return i;
	}

	@Override
	public Date getDate(int order) {
		Date d = null;
		switch (order) {
			case 0:
				d = publishDate;
				break;
			case 1:
				d = dueDate;
				break;
		}
		return d;
	}

	@Override
	public boolean getBool(int order) {
		return false; // 不使用
	}

	@Override
	public List<String> getQuotationNumbers() {
		return null;
	}

	@Override
	public List<Integer> getParents() {
		return null;
	}

}
