/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package fukaisystem.dto;

import java.io.Serializable;
import java.util.Vector;

/**
 *
 * @author kameura
 */
public class AccountNamesDTO implements Serializable {

	private int type; // 0=M_得意先名, 1=M_納入先
	private int accountCD;
	private Vector<Vector<Object>> accountNames;

	public AccountNamesDTO(int type, int accountCD, Vector<Vector<Object>> accountNames) {
		this.type = type;
		this.accountCD = accountCD;
		this.accountNames = accountNames;
	}

	public int getType() {
		return type;
	}

	public int getAccountCD() {
		return accountCD;
	}

	public Vector<Vector<Object>> getAccountNames() {
		return accountNames;
	}
}
