package librec.metric;

import librec.intf.Recommender;
import librec.util.Dates;

/**
 * Created by rdburke on 8/2/16.
 */
public class TimeMetric implements ITimeMetric {
    private Double m_time;
    private String m_dateString;
    private String m_name;

    public TimeMetric (String name) {
        m_name = name;
        m_time = -1.0;
        m_dateString = "<unknown>";
    }

    public void init (Recommender rec) { }

    public void compute (int count) { }

    public String getName() { return m_name; }

    public void setTime (double time) {
        m_time = time;
        m_dateString = Dates.parse(m_time.longValue());
    }

    public double getValue () { return m_time; }

    public String getValueAsString() { return m_dateString; }
}
