package research.core.vo;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 行为组。由一个用户的一个session的一组行为组成。
 * 
 * @author liweigu714@163.com
 *
 */
public class ActionGroup extends BaseVo {
	private static final long serialVersionUID = -4088454789190267585L;
	private List<Action> actions = new ArrayList<Action>();
	private String userId;
	private String sessionId;
	private int step = 0;
	// 最新时间
	private String lastTime;
	// 扩展属性
	private Map<String, List<Double>> ext = new HashMap<String, List<Double>>();

	public ActionGroup() {
	}

	public ActionGroup(String userId, String sessionId) {
		this.userId = userId;
		this.sessionId = sessionId;
	}

	public List<Action> getActions() {
		return actions;
	}

	public void setActions(List<Action> actions) {
		this.actions = actions;
	}

	public String getUserId() {
		return userId;
	}

	public void setUserId(String userId) {
		this.userId = userId;
	}

	public String getSessionId() {
		return sessionId;
	}

	public void setSessionId(String sessionId) {
		this.sessionId = sessionId;
	}

	public String getLastTime() {
		return lastTime;
	}

	public void setLastTime(String lastTime) {
		this.lastTime = lastTime;
	}

	public int getStep() {
		return step;
	}

	public void setStep(int step) {
		this.step = step;
	}

	public Map<String, List<Double>> getExt() {
		return ext;
	}

	public void setExt(Map<String, List<Double>> ext) {
		this.ext = ext;
	}

}
