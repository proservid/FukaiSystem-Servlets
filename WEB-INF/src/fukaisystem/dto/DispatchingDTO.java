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
	int dispatchingID, period1, period2, number1, number2, l, m, s;
	String branch1, branch2, year, month, day, name, purpose, purpose2;
	Vector<Vector<Object>> vector;
	Date date;
	boolean isAndSearch;

	public DispatchingDTO(int dispatchingID, int period1, int number1, String branch1,
			int period2, int number2, String branch2, String year, String month, String day,
			String name, String purpose, String purpose2, Vector<Vector<Object>> vector,
			Date date, int l, int m, int s,	boolean isAndSearch) {
		this.dispatchingID = dispatchingID;
		this.period1 = period1;
		this.number1 = number1;
		this.branch1 = branch1;
		this.period2 = period2;
		this.number2 = number2;
		this.branch2 = branch2;
		this.name = name;
		this.purpose = purpose;
		this.purpose2 = purpose2;
		this.year = year;
		this.month = month;
		this.day = day;
		this.vector = vector;
		this.date = date;
		this.l = l;
		this.m = m;
		this.s = s;
		this.isAndSearch = isAndSearch;
	}

	@Override
	public String getStr(int order) {
		String str = "";
		switch(order) {
			case 0:
				str = branch2; break;
			case 1:
				str = year; break;
			case 2:
				str = month; break;
			case 3:
				str = day; break;
			case 4:
				str = purpose; break;
			case 5:
				str = purpose2; break;
			case 6:
				str = branch1; break;
			case 7:
				str = name; break;
		}
		return str;
	}

	@Override
	public int getInt(int order) {
		int i = 0;
		switch(order) {
			case 0:
				i = period2; break;
			case 1:
				i = number2; break;
			case 2:
				i = l; break;
			case 3:
				i = m; break;
			case 4:
				i = s; break;
			case 5:
				i = period1; break;
			case 6:
				i = number1; break;
			case 7://5Å®7
				i = dispatchingID; break;
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
	public List<String> getEsts() {
		return null;
	}

	@Override
	public List<Integer> getParents() {
		return null;
	}

}
