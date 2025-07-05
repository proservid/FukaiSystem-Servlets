/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package fukaisystem.dto;

import java.io.Serializable;
import java.sql.Date;
import java.util.List;
import java.util.Map;
import java.util.Vector;

/**
 *
 * @author kameura
 */
public class ProjectSummaryDTO implements Serializable {

	String quotationAccountName, productionAccountName, quotationProjectName, productionProjectName, placeName,
		quotationNum3, due, place, terms, validity, quotationNote,
		acceptID, productionNum3, productionNote,
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
	Vector<Vector<Object>> quotationVector, productionVector, dispatchingVector, salesSlips;
	Map<Integer, Vector<Vector<Object>>> quotationBasisDataMap;

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
		Vector<Vector<Object>> quotationVector,
		Map<Integer, Vector<Vector<Object>>> quotationBasisDataMap,
		// 製作
		String acceptID,
		String productionNum3,
		String placeName,
		String productionNote,
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
		Vector<Vector<Object>> productionVector,
		// 売上
		String salesNote,
		int salesID,
		int deliveryStateCode,
		int deliveryCode,
		int dispatchingAccountID,
		Date salesSlipPublishDate,
		Vector<Vector<Object>> dispatchingVector,
		Vector<Vector<Object>> salesSlips
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
		this.quotationVector = quotationVector;
		this.quotationBasisDataMap = quotationBasisDataMap;

		this.acceptID = acceptID;
		this.productionNum3 = productionNum3;
		this.placeName = placeName;
		this.productionNote = productionNote;
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
		this.productionVector = productionVector;

		this.salesNote = salesNote;
		this.salesID = salesID;
		this.deliveryStateCode = deliveryStateCode;
		this.deliveryCode = deliveryCode;
		this.dispatchingAccountID = dispatchingAccountID;
		this.salesSlipPublishDate = salesSlipPublishDate;
		this.dispatchingVector = dispatchingVector;
		this.salesSlips = salesSlips;
		this.buyerCode = buyerCode;
		this.isAdd = isAdd;
	}

	public String quotationAccountName() {
		return quotationAccountName;
	}

	public String productionAccountName() {
		return productionAccountName;
	}

	public String quotationProjectName() {
		return quotationProjectName;
	}

	public String placeName() {
		return placeName;
	}

	public String quotationNum3() {
		return quotationNum3;
	}

	public String due() {
		return due;
	} // 納期

	public String place() {
		return place;
	} // 受渡場所

	public String terms() {
		return terms;
	} // 取引条件

	public String validity() {
		return validity;
	} // 有効期間

	public String quotationNote() {
		return quotationNote;
	}

	public String acceptID() {
		return acceptID;
	}

	public String productionNum3() {
		return productionNum3;
	}

	public String productionNote() {
		return productionNote;
	}

	public String salesNote() {
		return salesNote;
	}

	public String birthNum3() {
		return birthNum3;
	}

	public String announcement() {
		return announcement;
	}

	public String productionProjectName() {
		return productionProjectName;
	}

	public int quotationAccountID() {
		return quotationAccountID;
	}

	public int productionAccountID() {
		return productionAccountID;
	}

	public int projectCode() {
		return projectCode;
	}

	public int quotationNum1() {
		return quotationNum1;
	}

	public int quotationNum2() {
		return quotationNum2;
	}

	public int contactCode() {
		return contactCode;
	}

	public int inquiryCode() {
		return inquiryCode;
	}

	public int submitCD() {
		return submitCD;
	}

	public int quotationCurrencyCD() {
		return quotationCurrencyCD;
	}

	public int quotationAmount() {
		return quotationAmount;
	}

	public int productionNum1() {
		return productionNum1;
	}

	public int productionNum2() {
		return productionNum2;
	}

	public int productionCurrencyCD() {
		return productionCurrencyCD;
	}

	public int productionAmount() {
		return productionAmount;
	}

	public int deliveryStateCode() {
		return deliveryStateCode;
	}

	public int deliveryCode() {
		return deliveryCode;
	}

	public int quotationID() {
		return quotationID;
	}

	public int productionID() {
		return productionID;
	}

	public int birthNum1() {
		return birthNum1;
	}

	public int birthNum2() {
		return birthNum2;
	}

	public int salesID() {
		return salesID;
	}

	public int dispatchingAccountID() {
		return dispatchingAccountID;
	}

	public int buyerCode() {
		return buyerCode;
	}

	public Date inquiryDate() {
		return inquiryDate;
	}

	public Date quotationDate() {
		return quotationDate;
	}

	public Date submitDate() {
		return submitDate;
	}

	public Date acceptDate() {
		return acceptDate;
	} // 受注年月日

	public Date deadlineDate() {
		return deadlineDate;
	} // 納期

	public Date productionSlipPublishDate() {
		return productionSlipPublishDate;
	} // 発行年月日

	public Date shippingDate() {
		return shippingDate;
	} // 出荷年月日

	public Date inspectionDate() {
		return inspectionDate;
	} // 検収年月日

	public Date salesSlipPublishDate() {
		return salesSlipPublishDate;
	} // 売上伝票発行年月日

	public boolean isRelease() {
		return isRelease;
	}

	public boolean isOrder() {
		return isOrder;
	}

	public boolean isNew() {
		return isNew;
	}

	public boolean isAdd() {
		return isAdd;
	}

	public Vector<Vector<Object>> quotationVector() {
		return quotationVector;
	}

	public Vector<Vector<Object>> productionVector() {
		return productionVector;
	}

	public Vector<Vector<Object>> salesSlips() {
		return salesSlips;
	}

	public void setQuotationVector(Vector<Vector<Object>> vector) {
		quotationVector = vector;
	}

	public void setProductionVector(Vector<Vector<Object>> vector) {
		productionVector = vector;
	}

	public void setSalesSlips(Vector<Vector<Object>> vector) {
		salesSlips = vector;
	}

	public Map<Integer, Vector<Vector<Object>>> getMap() {
		return quotationBasisDataMap;
	}

	public void setMap(Map<Integer, Vector<Vector<Object>>> map) {
		quotationBasisDataMap = map;
	}

	public void setQuotationNumbers(List<String> quotationNumbers) {
		this.quotationNumbers = quotationNumbers;
	}

	public void setParents(List<Integer> parents) {
		this.parents = parents;
		if (parents.size() > 0)
			this.projectCode = parents.get(0);
	}

	public List<String> getQuotationNumbers() {
		return quotationNumbers;
	}

	public List<Integer> getParents() {
		return parents;
	}
}
