/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package fukaisystem.dto.schedule;

import java.io.Serializable;

/**
 *
 * @author kameura
 */
public class Repeat implements Serializable {
	private boolean isWeekly;
	private int value;
	private String content;
	public Repeat(boolean isWeekly, int value, String content) {
		this.isWeekly = isWeekly;
		this.value = value;
		this.content = content;
	}
	public boolean isWeekly() {
		return isWeekly;
	}
	public int getValue() {
		return value;
	}
	public String getContent() {
		return content;
	}
}
