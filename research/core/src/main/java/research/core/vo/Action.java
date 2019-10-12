package research.core.vo;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 行为
 * 
 * @author liweigu714@163.com
 *
 */
public class Action extends BaseVo {
	private static final long serialVersionUID = 1101595706067070966L;

	// session id
	private int sid;
	// profile id
	private int pid;
	private String time;
	// 扩展属性
	private Map<String, List<Double>> ext = new HashMap<String, List<Double>>();

	public Action() {
		this.values = new ArrayList<Double>();
	}

	public Action(List<Double> values) {
		this.values = values;
	}

	public int getSid() {
		return this.sid;
	}

	public void setSid(int sid) {
		this.sid = sid;
	}

	public int getPid() {
		return pid;
	}

	public void setPid(int pid) {
		this.pid = pid;
	}

	public String getTime() {
		return time;
	}

	public void setTime(String time) {
		this.time = time;
	}

	public Map<String, List<Double>> getExt() {
		return ext;
	}

	public void setExt(Map<String, List<Double>> ext) {
		this.ext = ext;
	}

}
