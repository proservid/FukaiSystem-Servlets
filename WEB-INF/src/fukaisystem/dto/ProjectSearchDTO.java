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
public class ProjectSearchDTO implements Serializable {

	String accountName, projectName, range,
	 number_eb, number_bb, number_pb, acceptID,
	 submit_y, submit_m, submit_d,
	 accept_y, accept_m, accept_d,
	 publish_y, publish_m, publish_d,
	 due_y, due_m, due_d,
	 complete_y, complete_m, complete_d,
	 delivery_y, delivery_m, delivery_d,
	 inspection_y, inspection_m, inspection_d;
	int accountID, projectCode,
	 period_e, number_e,
	 period_b, number_b,
	 period_p, number_p,
	 currency,
	 price,
	 deliveryCode,
	 buyer;
	boolean isAnd;

	public ProjectSearchDTO(
	 int accountID, String accountName, String projectName, int projectCode,
	 int period_b, int number_b, String number_bb,
	 int period_e, int number_e, String number_eb,
	 String submit_y, String submit_m, String submit_d,
	 String acceptID,
	 String accept_y, String accept_m, String accept_d,
	 int period_p, int number_p, String number_pb,
	 String publish_y, String publish_m, String publish_d,
	 String due_y, String due_m, String due_d,
	 String complete_y, String complete_m, String complete_d,
	 String delivery_y, String delivery_m, String delivery_d,
	 String inspection_y, String inspection_m, String inspection_d,
	 int currency, int price, String range,
	 int deliveryCode, int buyer,
	 boolean isAnd) {
		this.accountID = accountID;
		this.accountName = accountName;
		this.projectName = projectName;
		this.projectCode = projectCode;
		this.period_b = period_b;
		this.number_b = number_b;
		this.number_bb = number_bb;
		this.period_e = period_e;
		this.number_e = number_e;
		this.number_eb = number_eb;
		this.submit_y = submit_y;
		this.submit_m = submit_m;
		this.submit_d = submit_d;
		this.acceptID = acceptID;
		this.accept_y = accept_y;
		this.accept_m = accept_m;
		this.accept_d = accept_d;
		this.period_p = period_p;
		this.number_p = number_p;
		this.number_pb = number_pb;
		this.publish_y = publish_y;
		this.publish_m = publish_m;
		this.publish_d = publish_d;
		this.due_y = due_y;
		this.due_m = due_m;
		this.due_d = due_d;
		this.complete_y = complete_y;
		this.complete_m = complete_m;
		this.complete_d = complete_d;
		this.delivery_y = delivery_y;
		this.delivery_m = delivery_m;
		this.delivery_d = delivery_d;
		this.inspection_y = inspection_y;
		this.inspection_m = inspection_m;
		this.inspection_d = inspection_d;
		this.currency = currency;
		this.price = price;
		this.range = range;
		this.deliveryCode = deliveryCode;
		this.buyer = buyer;
		this.isAnd = isAnd;
	}

	public String getStr(int order) {
		String s = "";
		switch(order) {
			case  0: s = accountName; break;
			case  1: s = range; break;

			case  2: s = projectName; break;
			case  3: s = number_bb; break;
			case  4: s = number_eb; break;
			case  5: s = submit_y; break;
			case  6: s = submit_m; break;
			case  7: s = submit_d; break;

			case  8: s = number_pb; break;
			case  9: s = acceptID; break;
			case 10: s = accept_y; break;
			case 11: s = accept_m; break;
			case 12: s = accept_d; break;
			case 13: s = publish_y; break;
			case 14: s = publish_m; break;
			case 15: s = publish_d; break;
			case 16: s = due_y; break;
			case 17: s = due_m; break;
			case 18: s = due_d; break;
			case 19: s = delivery_y; break;
			case 20: s = delivery_m; break;
			case 21: s = delivery_d; break;
			case 22: s = inspection_y; break;
			case 23: s = inspection_m; break;
			case 24: s = inspection_d; break;
			case 25: s = complete_y; break;
			case 26: s = complete_m; break;
			case 27: s = complete_d; break;
		}
		return s;
	}

	public int getInt(int order) {
		int i = 0;
		switch(order) {
			case  0: i = period_p; break;
			case  1: i = number_p; break;

			case  2: i = accountID; break;
			case  3: i = period_e; break;
			case  4: i = number_e; break;
			case  5: i = projectCode; break;
			case  6: i = currency; break;
			case  7: i = price; break;
			case  8: i = period_b; break;
			case  9: i = number_b; break;
			case 10: i = deliveryCode; break;
			case 11: i = buyer; break;
		}
		return i;
	}

	public boolean isAnd() {
		return isAnd;
	}

}
