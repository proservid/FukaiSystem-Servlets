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
public class SlipDTO implements Serializable {

	private boolean isOverwrite;
	private String name;
	private byte[] byteSlip;

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
