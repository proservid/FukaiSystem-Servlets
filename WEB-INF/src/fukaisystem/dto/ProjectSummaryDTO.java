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

	String quotationAccountName, productionAccountName, quotationProjectName, productionProjectName, placeName,
		quotationNum3, due, place, terms, validity, quotationNote,
		acceptID, productionNum3, prosuctionNote,
		salesNote, birthNum3, announcement;
	int quotationAccountID, productionAccountID, projectCode, buyerCode,
		quotationID, quotationNum1, quotationNum2, contactCode, inquiryCode,
		submitCD, quotationCurrencyCD, quotationAmount,
		productionID, productionNum1, productionNum2, productionCurrencyCD, productionAmount,
		deliveryStateCode, deliveryCode, birthNum1, birthNum2, salesID, dispatchingAccountID;
	Date inquiryDate, quotationDate, submitDate,
		acceptDate, deadlineDate, productionSlipPublishDate,
		shippingDate, inspectionDate, salesSlipPublishDate;
	boolean isNew, isAdd, isRelease, isOrder;
	List<String> quotationNumbers;
	List<Integer> parents;
	Vector<Vector<Object>> quotationTable, productionTable, dispatchingTable, slipList;
	Map<Integer, Vector<Vector<Object>>> quotationBasisTable;

	public ProjectSummaryDTO(
		// 共通
		String quotationAccountName,
		String productionAccountName,
		String quotationProjectName,
		String productionProjectName,
		int quotationAccountID,
		int productionAccountID,
		int projectCode,
		int buyerCode,
		List<Integer> parents,
		// 見積
		String quotationNum3,
		String due,
		String place,
		String terms,
		String validity,
		String quotationNote,
		String announcement,
		String birthNum3,
		int quotationID,
		int quotationNum1,
		int quotationNum2,
		int contactCode,
		int inquiryCode,
		int submitCD,
		int quotationCurrencyCD,
		int quotationAmount,
		int birthNum1,
		int birthNum2,
		Date inquiryDate,
		Date quotationDate,
		Date submitDate,
		Vector<Vector<Object>> quotationTable,
		Map<Integer, Vector<Vector<Object>>> quotationBasisTable,
		// 製作
		String acceptID,
		String productionNum3,
		String placeName,
		String prosuctionNote,
		int productionID,
		int productionNum1,
		int productionNum2,
		int productionCurrencyCD,
		int productionAmount,
		Date acceptDate,
		Date deadlineDate,
		Date productionSlipPublishDate,
		Date shippingDate,
		Date inspectionDate,
		boolean isNew,
		boolean isAdd,
		boolean isRelease,
		boolean isOrder,
		Vector<Vector<Object>> productionTable,
		// 売上
		String salesNote,
		int salesID,
		int deliveryStateCode,
		int deliveryCode,
		int dispatchingAccountID,
		Date salesSlipPublishDate,
		Vector<Vector<Object>> dispatchingTable,
		Vector<Vector<Object>> slipList
	) {

		this.quotationAccountName = quotationAccountName;
		this.productionAccountName = productionAccountName;
		this.quotationProjectName = quotationProjectName;
		this.productionProjectName = productionProjectName;
		this.quotationAccountID = quotationAccountID;
		this.productionAccountID = productionAccountID;
		this.projectCode = projectCode;
		this.parents = parents;
		this.quotationNum3 = quotationNum3;
		this.due = due;
		this.place = place;
		this.terms = terms;
		this.validity = validity;
		this.quotationNote = quotationNote;
		this.announcement = announcement;
		this.birthNum3 = birthNum3;
		this.quotationID = quotationID;
		this.quotationNum1 = quotationNum1;
		this.quotationNum2 = quotationNum2;
		this.contactCode = contactCode;
		this.inquiryCode = inquiryCode;
		this.submitCD = submitCD;
		this.quotationCurrencyCD = quotationCurrencyCD;
		this.quotationAmount = quotationAmount;
		this.birthNum1 = birthNum1;
		this.birthNum2 = birthNum2;
		this.inquiryDate = inquiryDate;
		this.quotationDate = quotationDate;
		this.submitDate = submitDate;
		this.quotationTable = quotationTable;
		this.quotationBasisTable = quotationBasisTable;

		this.acceptID = acceptID;
		this.productionNum3 = productionNum3;
		this.placeName = placeName;
		this.prosuctionNote = prosuctionNote;
		this.productionID = productionID;
		this.productionNum1 = productionNum1;
		this.productionNum2 = productionNum2;
		this.productionCurrencyCD = productionCurrencyCD;
		this.productionAmount = productionAmount;
		this.acceptDate = acceptDate;
		this.deadlineDate = deadlineDate;
		this.productionSlipPublishDate = productionSlipPublishDate;
		this.shippingDate = shippingDate;
		this.inspectionDate = inspectionDate;
		this.isNew = isNew;
		this.isRelease = isRelease;
		this.isOrder = isOrder;
		this.productionTable = productionTable;

		this.salesNote = salesNote;
		this.salesID = salesID;
		this.deliveryStateCode = deliveryStateCode;
		this.deliveryCode = deliveryCode;
		this.dispatchingAccountID = dispatchingAccountID;
		this.salesSlipPublishDate = salesSlipPublishDate;
		this.dispatchingTable = dispatchingTable;
		this.slipList = slipList;
		this.buyerCode = buyerCode;
		this.isAdd = isAdd;
	}

	@Override
	public String getStr(int order) {
		String s = "";
		switch (order) {
			case 0:
				s = quotationAccountName;
				break;
			case 1:
				s = productionAccountName;
				break;
			case 2:
				s = quotationProjectName;
				break;
			case 3:
				s = placeName;
				break;
			case 4:
				s = quotationNum3;
				break;
			case 5:
				s = due;
				break; // 納期
			case 6:
				s = place;
				break; // 受渡場所
			case 7:
				s = terms;
				break; // 取引条件
			case 8:
				s = validity;
				break; // 有効期間
			case 9:
				s = quotationNote;
				break;
			case 10:
				s = acceptID;
				break;
			case 11:
				s = productionNum3;
				break;
			case 12:
				s = prosuctionNote;
				break;
			case 13:
				s = salesNote;
				break;
			case 14:
				s = birthNum3;
				break;
			case 15:
				s = announcement;
				break;
			case 16:
				s = productionProjectName;
				break;
		}
		return s;
	}

	@Override
	public int getInt(int order) {
		int i = 0;
		switch (order) {
			case 0:
				i = quotationAccountID;
				break;
			case 1:
				i = productionAccountID;
				break;
			case 2:
				i = projectCode;
				break;
			case 3:
				i = quotationNum1;
				break;
			case 4:
				i = quotationNum2;
				break;
			case 5:
				i = contactCode;
				break;
			case 6:
				i = inquiryCode;
				break;
			case 7:
				i = submitCD;
				break;
			case 8:
				i = quotationCurrencyCD;
				break;
			case 9:
				i = quotationAmount;
				break;
			case 10:
				i = productionNum1;
				break;
			case 11:
				i = productionNum2;
				break;
			case 12:
				i = productionCurrencyCD;
				break;
			case 13:
				i = productionAmount;
				break;
			case 14:
				i = deliveryStateCode;
				break;
			case 15:
				i = deliveryCode;
				break;
			case 16:
				i = quotationID;
				break;
			case 17:
				i = productionID;
				break;
			case 18:
				i = birthNum1;
				break;
			case 19:
				i = birthNum2;
				break;
			case 20:
				i = salesID;
				break;
			case 21:
				i = dispatchingAccountID;
				break;
			case 22:
				i = buyerCode;
				break;
		}
		return i;
	}

	@Override
	public Date getDate(int order) {
		Date d = null;
		switch (order) {
			case 0:
				d = inquiryDate;
				break;
			case 1:
				d = quotationDate;
				break;
			case 2:
				d = submitDate;
				break;
			case 3:
				d = acceptDate;
				break; // 受注年月日
			case 4:
				d = deadlineDate;
				break; // 納期
			case 5:
				d = productionSlipPublishDate;
				break; // 発行年月日
			case 6:
				d = shippingDate;
				break; // 出荷年月日
			case 7:
				d = inspectionDate;
				break; // 検収年月日
			case 8:
				d = salesSlipPublishDate;
				break; // 売上伝票発行年月日
		}
		return d;
	}

	@Override
	public boolean getBool(int order) {
		boolean b = false;
		switch (order) {
			case 0:
				b = isRelease;
				break;
			case 1:
				b = isOrder;
				break;
			case 2:
				b = isNew;
				break;
			case 3:
				b = isAdd;
				break;
		}
		return b;
	}

	@Override
	public Vector<Vector<Object>> getVector(int order) {
		Vector<Vector<Object>> v = null;
		switch (order) {
			case 0:
				v = quotationTable;
				break;
			case 1:
				v = productionTable;
				break;
			case 2:
				v = dispatchingTable;
				break;
			case 3:
				v = slipList;
				break;
		}
		return v;
	}

	public void setVector(int order, Vector<Vector<Object>> vector) {
		switch (order) {
			case 0:
				quotationTable = vector;
				break;
			case 1:
				productionTable = vector;
				break;
			case 2:
				dispatchingTable = vector;
				break;
			case 3:
				slipList = vector;
				break;
		}
	}

	public Map<Integer, Vector<Vector<Object>>> getMap() {
		return quotationBasisTable;
	}

	public void setMap(Map<Integer, Vector<Vector<Object>>> map) {
		quotationBasisTable = map;
	}

	public void setQuotationNumbers(List<String> quotationNumbers) {
		this.quotationNumbers = quotationNumbers;
	}

	public void setParents(List<Integer> parents) {
		this.parents = parents;
		if (parents.size() > 0)
			this.projectCode = parents.get(0);
	}

	@Override
	public List<String> getQuotationNumbers() {
		return quotationNumbers;
	}

	@Override
	public List<Integer> getParents() {
		return parents;
	}
}
 