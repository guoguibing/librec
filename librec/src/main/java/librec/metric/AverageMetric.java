package librec.metric;

import librec.intf.Recommender;

/**
 * Created by rdburke on 8/2/16.
 */
public class AverageMetric implements IMetric {
    String m_name;
    double m_sum;
    double m_avg;

    public AverageMetric (String name) {
        m_name = name;
    }

    public String getName () { return m_name; }

    public void init(Recommender rec) {
        m_sum = 0.0;
        m_avg = -1;
    }

    public void update(double value) {
        m_sum += value;
    }

    public void compute(int count) {
        m_avg = m_sum / count;
    }

    /**
     * Returns the value as a double
     * @return
     */
    public double getValue () { return m_avg; }

    /**
     * Returns the value in string form.
     * @return
     */
    public String getValueAsString () {
        return MetricCollection.ValueFormatString.format(this.getName(), this.getValue());
    }

}
