package fukaisystem.dto;

import java.io.Serializable;
import java.sql.Date;
import java.util.List;

public class Daily implements Serializable {
	String myCD;
	Date date;
	String text;
	List<Integer> ids;
	List<Banner> banners;
	List<Repeat> repeats;
	public Daily(String myCD, Date date, String text, List<Integer> ids, List<Banner> banners, List<Repeat> repeats) {
		this.myCD = myCD;
		this.date = date;
		this.text = text;
		this.ids = ids;
		this.banners = banners;
		this.repeats = repeats;
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

}
