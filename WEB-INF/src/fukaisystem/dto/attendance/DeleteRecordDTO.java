package fukaisystem.dto.attendance;

import java.io.Serializable;
import java.util.Date;

public class DeleteRecordDTO implements Serializable {

	private static final long serialVersionUID = 1L;

	Date date;
	int memberCD;

	public DeleteRecordDTO(Date date, int memberCD) {
		this.date = date;
		this.memberCD = memberCD;
	}

	public Date getDate() {
		return date;
	}

	public int getMemberCD() {
		return memberCD;
	}
}
