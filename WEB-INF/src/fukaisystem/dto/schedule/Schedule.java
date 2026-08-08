package fukaisystem.dto.schedule;

import java.io.Serializable;
import java.sql.Date;
import java.util.List;
import java.util.Map;

public class Schedule implements Serializable {
	Map<Date, String> text;
	List<Banner> banners;
	List<Repeat> repeats;
	public Schedule(Map<Date, String> text, List<Banner> banners, List<Repeat> repeats) {
		this.text = text;
		this.banners = banners;
		this.repeats = repeats;
	}
	public Map<Date, String> getText() {
		return text;
	}
	public List<Banner> getBanners() {
		return banners;
	}
	public void setBanners(List<Banner> banners) {
		this.banners = banners;
	}
	public List<Repeat> getRepeats() {
		return repeats;
	}
	public void setRepeats(List<Repeat> repeats) {
		this.repeats = repeats;
	}

}
