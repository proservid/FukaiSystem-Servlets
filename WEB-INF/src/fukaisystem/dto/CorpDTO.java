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
public class CorpDTO implements Serializable {

	String name, name2, branch, branch2, disp,
			country, zip, zip2, pref, city, area, st, bldg, tel1, tel2, tel3, fax1, fax2, fax3, mail, url, rem, id, alphabet;
	int supNum, acNum, type, nenga;
	boolean isValid, isZou;

	public CorpDTO(
		String name, String name2, String branch, String branch2, String disp,
		String country, String zip, String zip2, String pref, String city, String area, String st, String bldg,
		String tel1, String tel2, String tel3, String fax1, String fax2, String fax3, String mail, String url,
		String rem, String id, String alphabet,
		int supNum, int acNum, int type, int nenga,
		boolean isValid, boolean isZou) {
		this.name = name;
		this.name2 = name2;
		this.branch = branch;
		this.branch2 = branch2;
		this.disp = disp;
		this.country = country;
		this.zip = zip;
		this.zip2 = zip2;
		this.pref = pref;
		this.city = city;
		this.area = area;
		this.st = st;
		this.bldg = bldg;
		this.mail = mail;
		this.url = url;
		this.rem = rem;
		this.id = id;
		this.alphabet = alphabet;
		this.supNum = supNum;
		this.acNum = acNum;
		this.type = type;
		this.tel1 = tel1;
		this.tel2 = tel2;
		this.tel3 = tel3;
		this.fax1 = fax1;
		this.fax2 = fax2;
		this.fax3 = fax3;
		this.isValid = isValid;
		this.isZou = isZou;
		this.nenga = nenga;
	}

	public String getStr(int order) {
		String s = "";
		switch(order) {
			case  0: s = name; break; //会社名/氏名
			case  1: s = name2; break; //カイシャメイ/シメイ
			case  2: s = branch; break; //支店名/所属部署
			case  3: s = branch2; break; //シテンメイ/役職名
			case  4: s = disp; break; //表示/会社コード
			case  5: s = country; break; //
			case  6: s = zip; break; //
			case  7: s = zip2; break; //
			case  8: s = pref; break; //
			case  9: s = city; break; //
			case 10: s = area; break; //
			case 11: s = st; break; //
			case 12: s = bldg; break; //
			case 13: s = tel1; break; //
			case 14: s = tel2; break; //
			case 15: s = tel3; break; //
			case 16: s = fax1; break; //
			case 17: s = fax2; break; //
			case 18: s = fax3; break; //
			case 19: s = mail; break; //
			case 20: s = url; break; //
			case 21: s = rem; break; //
			case 22: s = id; break; //
			case 23: s = alphabet; break; //
		}
		return s;
	}

	public int getInt(int order) {
		int i = 0;
		switch(order) {
			case  0: i = supNum; break;
			case  1: i = acNum; break;
			case  2: i = type; break;
			case  3: i = nenga; break;
		}
		return i;
	}

	public boolean getBool(int order) {
		boolean b = false;
		switch(order) {
			case 0:	b = isValid; break;
			case 1:	b = isZou; break;
		}
		return b; //不使用
	}
}
