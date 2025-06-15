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
public class DispatchingDTO extends BasicDTO {
	int dispatchingID, orderNum1, dispatchNum1, orderNum2, dispatchNum2, coarseCategory, middleCategory, fineCategory;
	String orderNum3, dispatchNum3, year, month, day, name, use, remark;
	Vector<Vector<Object>> vector;
	Date date;
	boolean isAndSearch;

	public DispatchingDTO(
		int dispatchingID,
		int orderNum1,
		int orderNum2,
		String orderNum3,
		int dispatchNum1,
		int dispatchNum2,
		String dispatchNum3,
		String year,
		String month,
		String day,
		String name,
		String use,
		String remark,
		Vector<Vector<Object>> vector,
		Date date,
		int coarseCategory,
		int middleCategory,
		int fineCategory,
		boolean isAndSearch
	) {
		this.dispatchingID = dispatchingID;
		this.orderNum1 = orderNum1;
		this.orderNum2 = orderNum2;
		this.orderNum3 = orderNum3;
		this.dispatchNum1 = dispatchNum1;
		this.dispatchNum2 = dispatchNum2;
		this.dispatchNum3 = dispatchNum3;
		this.name = name;
		this.use = use;
		this.remark = remark;
		this.year = year;
		this.month = month;
		this.day = day;
		this.vector = vector;
		this.date = date;
		this.coarseCategory = coarseCategory;
		this.middleCategory = middleCategory;
		this.fineCategory = fineCategory;
		this.isAndSearch = isAndSearch;
	}

	@Override
	public String getStr(int order) {
		String str = "";
		switch (order) {
			case 0:
				str = dispatchNum3;
				break;
			case 1:
				str = year;
				break;
			case 2:
				str = month;
				break;
			case 3:
				str = day;
				break;
			case 4:
				str = use;
				break;
			case 5:
				str = remark;
				break;
			case 6:
				str = orderNum3;
				break;
			case 7:
				str = name;
				break;
		}
		return str;
	}

	@Override
	public int getInt(int order) {
		int i = 0;
		switch (order) {
			case 0:
				i = dispatchNum1;
				break;
			case 1:
				i = dispatchNum2;
				break;
			case 2:
				i = coarseCategory;
				break;
			case 3:
				i = middleCategory;
				break;
			case 4:
				i = fineCategory;
				break;
			case 5:
				i = orderNum1;
				break;
			case 6:
				i = orderNum2;
				break;
			case 7: // 5→7
				i = dispatchingID;
				break;
		}
		return i;
	}

	@Override
	public Vector<Vector<Object>> getVector(int order) {
		return vector;
	}

	@Override
	public Date getDate(int order) {
		return date;
	}

	public boolean isAnd() {
		return isAndSearch;
	}

	@Override
	public boolean getBool(int order) {
		return false;
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
 