/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package fukaisystem.dto;

import java.sql.Date;
import java.util.List;
import java.util.Map;
import java.util.Vector;

/**
 *
 * @author kameura
 */
public class ProjectSummaryDTO extends BasicDTO {

	 String accountName_e, accountName_p, projectNameE, projectNameP, placeName,
	  number_eb, deadline, place, terms, validity, note_e,
	  acceptID, number_pb, note_p,
	  note_s, number_bb, announcement;
	 int accountID_e, accountID_p, projectCode, buyer,
	  estimateID, period_e, number_e, contactCode, inquiryCode,
	  submitCD, currencyCD_e, amount_e,
	  productID, period_p, number_p, currencyCD_p, amount_p,
	  deliveryState, deliveryCode, period_b, number_b, deliveryID, accountID_d;
	 Date inquiryDate, estimateDate, submitDate,
	  acceptDate, dueDate, publishDate,
	  shippingDate, inspectionDate, salesDate;
	 boolean isNew, isAdd, isRelease, isOrder;
	 List<String> ests;
	 List<Integer> parents;
	 Vector<Vector<Object>> mainTable_e, mainTable_p, mainTable_d, slipList;
	 Map<Integer, Vector<Vector<Object>>> subTable_e, subTable_p;

	public ProjectSummaryDTO(
	 //共通
	 String accountName_e, String accountName_p, String projectNameE, String projectNameP,
	 int accountID_e, int accountID_p, int projectCode, int buyer, List<Integer> parents,
	 //見積
	 String number_eb, String deadline, String place, String terms, String validity, String note_e,
	 String announcement, String number_bb,
	 int estimateID, int period_e, int number_e, int contactCode, int inquiryCode,
	 int submitCD, int currencyCD_e, int amount_e, int period_b, int number_b,
	 Date inquiryDate, Date estimateDate, Date submitDate,
	 Vector<Vector<Object>> mainTable_e,
	 Map<Integer, Vector<Vector<Object>>> subTable_e,
	 //製作
	 String acceptID, String number_pb, String placeName, String note_p,
	 int productID, int period_p, int number_p, int currencyCD_p, int amount_p,
	 Date acceptDate, Date dueDate, Date publishDate, Date shippingDate, Date inspectionDate,
	 boolean isNew, boolean isAdd, boolean isRelease, boolean isOrder,
	 Vector<Vector<Object>> mainTable_p,
	 Map<Integer, Vector<Vector<Object>>> subTable_p,
	 //売上
	 String note_s,
	 int deliveryID,
	 int deliveryState, int deliveryCode, int accountID_d,
	 Date salesDate,
	 Vector<Vector<Object>> mainTable_d, Vector<Vector<Object>> slipList) {
		this.accountName_e = accountName_e;	this.accountName_p = accountName_p; this.projectNameE = projectNameE; this.projectNameP = projectNameP;
		this.accountID_e = accountID_e;	this.accountID_p = accountID_p;	this.projectCode = projectCode; this.parents = parents;
		this.number_eb = number_eb; this.deadline = deadline;	this.place = place;	this.terms = terms;	this.validity = validity; this.note_e = note_e;
		this.announcement = announcement; this.number_bb = number_bb;
		this.estimateID = estimateID; this.period_e = period_e; this.number_e = number_e; this.contactCode = contactCode;	this.inquiryCode = inquiryCode;
		this.submitCD = submitCD; this.currencyCD_e = currencyCD_e; this.amount_e = amount_e; this.period_b = period_b; this.number_b = number_b;
		this.inquiryDate = inquiryDate;	this.estimateDate = estimateDate; this.submitDate = submitDate;
		this.mainTable_e = mainTable_e;
		this.subTable_e = subTable_e;

		this.acceptID = acceptID; this.number_pb = number_pb; this.placeName = placeName; this.note_p = note_p;
		this.productID = productID; this.period_p = period_p; this.number_p = number_p; this.currencyCD_p = currencyCD_p; this.amount_p = amount_p;
		this.acceptDate = acceptDate; this.dueDate = dueDate; this.publishDate = publishDate;
		this.shippingDate = shippingDate;
		this.inspectionDate = inspectionDate;
		this.isNew = isNew; this.isRelease = isRelease;	this.isOrder = isOrder;
		this.mainTable_p = mainTable_p;
		this.subTable_p = subTable_p;

		this.note_s = note_s;
		this.deliveryID = deliveryID;
		this.deliveryState = deliveryState;	this.deliveryCode = deliveryCode;
		this.accountID_d = accountID_d;
		this.salesDate = salesDate;
		this.mainTable_d = mainTable_d;
		this.slipList = slipList;
		this.buyer = buyer;
		this.isAdd = isAdd;
	}

	@Override
	public String getStr(int order) {
		String s = "";
		switch(order) {
			case  0: s = accountName_e; break;
			case  1: s = accountName_p; break;
			case  2: s = projectNameE; break;
			case  3: s = placeName; break;
			case  4: s = number_eb; break;
			case  5: s = deadline; break;//納期
			case  6: s = place; break;//受渡場所
			case  7: s = terms; break;//取引条件
			case  8: s = validity; break;//有効期間
			case  9: s = note_e; break;
			case 10: s = acceptID; break;
			case 11: s = number_pb; break;
			case 12: s = note_p; break;
			case 13: s = note_s; break;
			case 14: s = number_bb; break;
			case 15: s = announcement; break;
			case 16: s = projectNameP; break;
		}
		return s;
	}

	@Override
	public int getInt(int order) {
		int i = 0;
		switch(order) {
			case  0: i = accountID_e; break;
			case  1: i = accountID_p; break;
			case  2: i = projectCode; break;
			case  3: i = period_e; break;
			case  4: i = number_e; break;
			case  5: i = contactCode; break;
			case  6: i = inquiryCode; break;
			case  7: i = submitCD; break;
			case  8: i = currencyCD_e; break;
			case  9: i = amount_e; break;
			case 10: i = period_p; break;
			case 11: i = number_p; break;
			case 12: i = currencyCD_p; break;
			case 13: i = amount_p; break;
			case 14: i = deliveryState; break;
			case 15: i = deliveryCode; break;
			case 16: i = estimateID; break;
			case 17: i = productID; break;
			case 18: i = period_b; break;
			case 19: i = number_b; break;
			case 20: i = deliveryID; break;
			case 21: i = accountID_d; break;
			case 22: i = buyer; break;
		}
		return i;
	}

	@Override
	public Date getDate(int order) {
		Date d = null;
		switch(order) {
			case 0: d = inquiryDate; break;
			case 1: d = estimateDate; break;
			case 2: d = submitDate; break;
			case 3: d = acceptDate; break;//受注年月日
			case 4: d = dueDate; break;//納期
			case 5: d = publishDate; break;//発行年月日
			case 6: d = shippingDate; break;//出荷年月日
			case 7: d = inspectionDate; break;//検収年月日
			case 8: d = salesDate; break;//売上年月日
		}
		return d;
	}

	@Override
	public boolean getBool(int order) {
		boolean b = false;
		switch(order) {
			case 0: b = isRelease; break;
			case 1: b = isOrder; break;
			case 2: b = isNew; break;
			case 3: b = isAdd; break;
		}
		return b;
	}

	@Override
	public Vector<Vector<Object>> getVector(int order) {
		Vector<Vector<Object>> v = null;
		switch(order) {
			case 0: v = mainTable_e; break;
			case 1: v = mainTable_p; break;
			case 2: v = mainTable_d; break;
			case 3: v = slipList; break;
		}
		return v;
	}
	public void setVector(int order, Vector<Vector<Object>> vector) {
		switch(order) {
			case 0: mainTable_e = vector; break;
			case 1: mainTable_p = vector; break;
			case 2: mainTable_d = vector; break;
			case 3: slipList = vector; break;
		}
	}

	public Map<Integer, Vector<Vector<Object>>> getMap(int order) {
		Map<Integer, Vector<Vector<Object>>> m = null;
		switch(order) {
			case 0: m = subTable_e; break;
			case 1: m = subTable_p; break;
		}
		return m;
	}
	public void setMap(int order, Map<Integer, Vector<Vector<Object>>> map) {
		switch(order) {
			case 0: subTable_e = map; break;
			case 1: subTable_p = map; break;
		}
	}

	public void setEsts(List<String> ests) {
		this.ests = ests;
	}
	public void setParents(List<Integer> parents) {
		this.parents = parents;
		if(parents.size() > 0) this.projectCode = parents.get(0);
	}
	@Override
	public List<String> getEsts() {
		return ests;
	}
	@Override
	public List<Integer> getParents() {
		return parents;
	}
}
