/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package print.dto;

import java.io.Serializable;

/**
 *
 * @author kameura
 */
public class SlipDTO implements Serializable {

	boolean isOverwrite;
	String name;
	byte[] byteSlip;

	public SlipDTO(boolean isOverwrite, String name, byte[] byteSlip) {
		this.isOverwrite = isOverwrite;
		this.name = name;
		this.byteSlip = byteSlip;
	}

	public boolean isOverwrite() {
		return isOverwrite;
	}

	public String getName() {
		return name;
	}

	public byte[] getByteSlip() {
		return byteSlip;
	}

}
