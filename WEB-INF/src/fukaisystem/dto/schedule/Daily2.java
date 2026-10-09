package fukaisystem.dto.schedule;

import java.io.Serializable;
import java.sql.Date;
import java.util.List;

public class Daily2 implements Serializable {
	public static final int DAILY = 1;
	public static final int REPEAT = 2;
	public static final int BANNER = 4;

	String myCD;
	Date date;
	String text;
	List<Integer> ids;
	List<Banner> banners;
	List<Repeat> repeats;
	Object[] members;
	int withBit;

	public Daily2(String myCD, Date date, String text, List<Integer> ids, List<Banner> banners, List<Repeat> repeats, Object[] members, int withBit) {
		this.myCD = myCD;
		this.date = date;
		this.text = text;
		this.ids = ids;
		this.banners = banners;
		this.repeats = repeats;
		this.members = members;
		this.withBit = withBit;
	}
	public String getCD() {
		return myCD;
	}
	public Date getDate() {
		return date;
	}
	public String getText() {
		return text;
	}
	public List<Integer> getIds() {
		return ids;
	}
	public List<Banner> getBanners() {
		return banners;
	}
	public List<Repeat> getRepeats() {
		return repeats;
	}
	public Object[] getMembers() {
		return members;
	}
	public int getWithBit() {
		return withBit;
	}

}
