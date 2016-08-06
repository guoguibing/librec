package librec.metric;

import librec.intf.Recommender;

import java.util.List;

/**
 * Created by rdburke on 8/2/16.
 */
public interface IDiversityMetric<T> extends IMetric {
    public void updateDiversity(List<T> results, Recommender rec);
}

// diversity measures xD5, xD10,

class MetricDiv5 implements IDiversityMetric<Integer> {
    private double m_sumDiv5;
    private double m_div;
    public String getName () { return "Div5";}

    public void init(Recommender rec) {
        m_sumDiv5 = 0.0;
        m_div = -1;
    }

    public void updateDiversity(List<Integer> results, Recommender rec) {
        double div = rec.diverseAt(results, 5);
        m_sumDiv5 += div;
    }

    public void compute(int count) {
        m_div = m_sumDiv5 / count;
    }

    public double getValue() { return m_div;}
    public String getValueAsString () {
        return MetricCollection.ValueFormatString.format(this.getName(), this.getValue());
    }
}


class MetricDiv10 implements IDiversityMetric<Integer> {
    private double m_sumDiv10;
    private double m_div;
    public String getName () { return "Div10";}

    public void init(Recommender rec) {
        m_sumDiv10 = 0.0;
        m_div = -1;
    }

    public void updateDiversity(List<Integer> results, Recommender rec) {
        double div = rec.diverseAt(results, 10);
        m_sumDiv10 += div;
    }

    public void compute(int count) {
        m_div = m_sumDiv10 / count;
    }

    public double getValue() { return m_div;}
    public String getValueAsString () {
        return MetricCollection.ValueFormatString.format(this.getName(), this.getValue());
    }
}
