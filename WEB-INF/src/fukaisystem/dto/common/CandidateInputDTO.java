/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package fukaisystem.dto.common;

import java.io.Serializable;

/**
 *
 * @author kameura
 */
public class CandidateInputDTO implements Serializable {

	private String input;
	private int key;
	private boolean isValidOnly;

	public CandidateInputDTO(String input, int key, boolean isValidOnly) {
		this.input = input;
		this.key = key;
		this.isValidOnly = isValidOnly;
	}

	public String getInput() {
		return input;
	}

	public int getKey() {
		return key;
	}

	public boolean isValidOnly() {
		return isValidOnly;
	}
}
