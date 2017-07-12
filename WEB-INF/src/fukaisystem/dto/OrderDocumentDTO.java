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
	 String accountName, number_ob, note1, note2;
	 int accountID, period_o, number_o, slipNum, orderID;
	 Date issueDate, dueDate, enteringDate, dispatchingDate;

	public OrderDocumentDTO(Vector<Vector<Object>> vector,
	 String accountName, String number_ob, String note1, String note2,
	 int accountID, int period_o, int number_o, int slipNum, int orderID,
	 Date issueDate, Date dueDate, Date enteringDate, Date dispatchingDate) {
		this.vector = vector;
		this.accountName = accountName;
		this.number_ob = number_ob;
		this.note1 = note1;
		this.note2 = note2;
		this.accountID = accountID;
		this.period_o = period_o;
		this.number_o = number_o;
		this.slipNum = slipNum;
		this.orderID = orderID;
		this.issueDate = issueDate;
		this.dueDate = dueDate;
		this.enteringDate = enteringDate;
		this.dispatchingDate = dispatchingDate;
	}

	@Override
	public Vector<Vector<Object>> getVector(int order) {
		return vector;
	}

	@Override
	public String getStr(int order) {
		String s = "";
		switch(order) {
			case  0: s = accountName; break;
			case  1: s = number_ob; break;
			case  2: s = note1; break;
			case  3: s = note2; break;
		}
		return s;
	}

	@Override
	public int getInt(int order) {
		int i = 0;
		switch(order) {
			case 0:	i = accountID; break;
			case 1:	i = period_o; break;
			case 2:	i = number_o; break;
			case 3: i = slipNum; break;
			case 4: i = orderID; break;
		}
		return i;
	}

	@Override
	public Date getDate(int order) {
		Date d = null;
		switch(order) {
			case 0:	d = issueDate; break;
			case 1:	d = dueDate; break;
			case 2:	d = enteringDate; break;
			case 3:	d = dispatchingDate; break;
		}
		return d;
	}

	@Override
	public boolean getBool(int order) {
		return false;//ïségóp
	}

	@Override
	public List<String> getEsts() {
		return null;
	}

	@Override
	public List<Integer> getParents() {
		return null;
	}

}
