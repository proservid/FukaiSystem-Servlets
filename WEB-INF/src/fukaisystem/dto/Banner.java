/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package fukaisystem.dto;

import java.io.Serializable;
import java.sql.Date;

/**
 *
 * @author kameura
 */
public class Banner implements Serializable {
	private int id;
	private Date from;
	private Date to;
	private String content;
	public Banner(int id, Date from, Date to, String content) {
		this.id = id;
		this.from = from;
		this.to = to;
		this.content = content;
	}
	public int getID() {
		return id;
	}
	public Date getFrom() {
		return from;
	}
	public Date getTo() {
		return to;
	}
	public String getContent() {
		return content;
	}
}
