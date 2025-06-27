/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package fukaisystem.dto;

import java.io.Serializable;

import com.proservid.print.slip.Slip;

/**
 *
 * @author kameura
 */
public class FormatDTO implements Serializable {

	boolean isOverwrite;
	String name;
	Slip format;

	public FormatDTO(boolean isOverwrite, String name, Slip format) {
		this.isOverwrite = isOverwrite;
		this.name = name;
		this.format = format;
	}

	public boolean isOverwrite() {
		return isOverwrite;
	}

	public String getName() {
		return name;
	}

	public Slip getformat() {
		return format;
	}

}
