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
public class OrderSearchDTO implements Serializable {

	String orderNum3, orderY, orderM, orderD,
		dueY, dueM, dueD,
		deliveryY, deliveryM, deliveryD,
		acceptY, acceptM, acceptD;
	int supplierID, orderNum1, orderNum2, slipNum, deliveryNum;
	boolean isAndSearch;

	public OrderSearchDTO(
		String orderNum3,
		String orderY,
		String orderM,
		String orderD,
		String dueY,
		String dueM,
		String dueD,
		String acceptY,
		String acceptM,
		String acceptD,
		String deliveryY,
		String deliveryM,
		String deliveryD,
		int supplierID,
		int orderNum1,
		int orderNum2,
		int slipNum,
		int deliveryNum,
		boolean isAndSearch
	) {
		this.orderNum3 = orderNum3;
		this.orderY = orderY;
		this.orderM = orderM;
		this.orderD = orderD;
		this.dueY = dueY;
		this.dueM = dueM;
		this.dueD = dueD;
		this.acceptY = acceptY;
		this.acceptM = acceptM;
		this.acceptD = acceptD;
		this.deliveryY = deliveryY;
		this.deliveryM = deliveryM;
		this.deliveryD = deliveryD;
		this.supplierID = supplierID;
		this.orderNum1 = orderNum1;
		this.orderNum2 = orderNum2;
		this.slipNum = slipNum;
		this.deliveryNum = deliveryNum;
		this.isAndSearch = isAndSearch;
	}

	public String getStr(int order) {
		String s = "";
		switch (order) {
			case 0:
				s = orderNum3;
				break;
			case 1:
				s = orderY;
				break;
			case 2:
				s = orderM;
				break;
			case 3:
				s = orderD;
				break;
			case 4:
				s = dueY;
				break;
			case 5:
				s = dueM;
				break;
			case 6:
				s = dueD;
				break;
			case 7:
				s = acceptY;
				break;
			case 8:
				s = acceptM;
				break;
			case 9:
				s = acceptD;
				break;
			case 10:
				s = deliveryY;
				break;
			case 11:
				s = deliveryM;
				break;
			case 12:
				s = deliveryD;
				break;
		}
		return s;
	}

	public int getInt(int order) {
		int i = 0;
		switch (order) {
			case 0:
				i = supplierID;
				break;
			case 1:
				i = orderNum1;
				break;
			case 2:
				i = orderNum2;
				break;
			case 3:
				i = slipNum;
				break;
			case 4:
				i = deliveryNum;
				break;
		}
		return i;
	}

	public boolean isAnd() {
		return isAndSearch;
	}

}
