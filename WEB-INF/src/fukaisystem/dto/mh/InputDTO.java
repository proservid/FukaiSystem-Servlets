/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package fukaisystem.dto.mh;

import java.io.Serializable;

/**
 *
 * @author kameura
 */
public class InputDTO implements Serializable {
	int id, period, number, year, month, day, fromT, fromM, toT, toM, rest;
	String dept, name, process, branch;
	public InputDTO(int id, String dept, String name, String process, int period, int number, String branch,
	 int year, int month, int day, int fromT, int fromM, int toT, int toM, int rest) throws Exception {
		if(name.equals("") || process.equals("")) throw new Exception();
		this.id = id;
		this.dept = dept;
		this.name = name;
		this.process = process;
		this.period = period;
		this.number = number;
		this.branch = branch;
		this.year = year;
		this.month = month;
		this.day = day;
		this.fromT = fromT;
		this.fromM = fromM;
		this.toT = toT;
		this.toM = toM;
		this.rest = rest;

	}

	public int getInt(int order) {
		int i = 0;
		switch(order) {
			case 0:	i = period; break;
			case 1:	i = number; break;
			case 2:	i = year; break;
			case 3:	i = month; break;
			case 4:	i = day; break;
			case 5:	i = fromT; break;
			case 6:	i = fromM; break;
			case 7:	i = toT; break;
			case 8:	i = toM; break;
			case 9:	i = rest; break;
			case 10: i = id; break;
		}
		return i;

	}
	public String getString(int order) {
		String s = "";
		switch(order) {
			case  0: s = dept; break;
			case  1: s = name; break;
			case  2: s = process; break;
			case  3: s = branch; break;
		}
		return s;

	}

}
