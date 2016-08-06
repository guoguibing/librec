package librec.metric;

import librec.intf.Recommender;
import librec.util.Dates;

/**
 * Really just a placeholder for storing timing information. No computation required.
 * Created by rdburke on 8/1/16.
 */
public interface ITimeMetric extends IMetric {
    public void setTime(double time);
}


/**
 * Created by rdburke on 8/2/16.
 */
class TestTime implements ITimeMetric {
    private Double m_time;
    private String m_dateString;

    public TestTime () {
        m_time = -1.0;
        m_dateString = "<unknown>";
    }

    public void init (Recommender rec) { }

    public void compute (int count) { }

    public String getName() { return "TestTime"; }

    public void setTime (double time) {
        m_time = time;
        m_dateString = Dates.parse(m_time.longValue());
    }

    public double getValue () { return m_time; }

    public String getValueAsString() { return m_dateString; }
}

class TrainTime implements ITimeMetric {
    private Double m_time;
    private String m_dateString;

    public TrainTime () {
        m_time = -1.0;
        m_dateString = "<unknown>";
    }

    public void init (Recommender rec) { }

    public void compute (int count) { }

    public String getName() { return "TrainTime"; }

    public void setTime (double time) {
        m_time = time;
        m_dateString = Dates.parse(m_time.longValue());
    }

    public double getValue () { return m_time; }

    public String getValueAsString() { return m_dateString; }
}
