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

	String number_ob, order_y, order_m, order_d,
	 due_y, due_m, due_d,
	 delivery_y, delivery_m, delivery_d,
	 accept_y, accept_m, accept_d;
	int supplierID, period_o, number_o, slipNum, deliveryNum;
	boolean isAndSearch;

	public OrderSearchDTO(
	 String number_ob,
	 String order_y, String order_m, String order_d,
	 String due_y, String due_m, String due_d,
	 String accept_y, String accept_m, String accept_d,
	 String delivery_y, String delivery_m, String delivery_d,
	 int supplierID, int period_o, int number_o, int slipNum,
	 int deliveryNum,
	 boolean isAndSearch) {
		this.number_ob = number_ob;
		this.order_y = order_y;
		this.order_m = order_m;
		this.order_d = order_d;
		this.due_y = due_y;
		this.due_m = due_m;
		this.due_d = due_d;
		this.accept_y = accept_y;
		this.accept_m = accept_m;
		this.accept_d = accept_d;
		this.delivery_y = delivery_y;
		this.delivery_m = delivery_m;
		this.delivery_d = delivery_d;
		this.supplierID = supplierID;
		this.period_o = period_o;
		this.number_o = number_o;
		this.slipNum = slipNum;
		this.deliveryNum = deliveryNum;
		this.isAndSearch = isAndSearch;
	}

	public String getStr(int order) {
		String s = "";
		switch(order) {
			case 0: s = number_ob; break;
			case 1: s = order_y; break;
			case 2: s = order_m; break;
			case 3: s = order_d; break;
			case 4: s = due_y; break;
			case 5: s = due_m; break;
			case 6: s = due_d; break;
			case 7: s = accept_y; break;
			case 8: s = accept_m; break;
			case 9: s = accept_d; break;
			case 10: s = delivery_y; break;
			case 11: s = delivery_m; break;
			case 12: s = delivery_d; break;
		}
		return s;
	}

	public int getInt(int order) {
		int i = 0;
		switch(order) {
			case 0:	i = supplierID; break;
			case 1:	i = period_o; break;
			case 2:	i = number_o; break;
			case 3:	i = slipNum; break;
			case 4: i = deliveryNum; break;
		}
		return i;
	}

	public boolean isAnd() {
		return isAndSearch;
	}

}
