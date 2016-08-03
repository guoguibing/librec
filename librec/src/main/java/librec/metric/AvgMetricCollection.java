package librec.metric;

import librec.intf.Recommender;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by rdburke on 8/2/16.
 */
public class AvgMetricCollection {

    MetricDict<AverageMetric> m_avgMetrics;

    public AvgMetricCollection (Recommender rec) {
        m_avgMetrics = new MetricDict<AverageMetric>();
        List<String> names = new ArrayList<String>();
        names.addAll(rec.measures.getRatingMetricNames());
        names.addAll(rec.measures.getRankingMetricNames());
        names.addAll(rec.measures.getTimeMetricNames());
        names.addAll(rec.measures.getDiversityMetricNames());

        for (String name : names) {
            m_avgMetrics.addMetric(name, new AverageMetric(name));
        }
    }

    public AverageMetric getMetric(String name) {
        return m_avgMetrics.getMetric(name);
    }

    public void initAll(Recommender rec) {
        for (AverageMetric metric : m_avgMetrics.getMetrics()) {
            metric.init(rec);
        }
    }

    public void updateFromMeasures (MetricCollection measures) {
        for (IRatingMetric metric : measures.getRatingMetrics()) {
            AverageMetric avgMetric = m_avgMetrics.getMetric(metric.getName());
            avgMetric.update(metric.getValue());
        }
        for (IRankingMetric metric : measures.getRankingMetrics()) {
            AverageMetric avgMetric = m_avgMetrics.getMetric(metric.getName());
            avgMetric.update(metric.getValue());
        }
        for (ITimeMetric metric : measures.getTimeMetrics()) {
            AverageMetric avgMetric = m_avgMetrics.getMetric(metric.getName());
            avgMetric.update(metric.getValue());
        }
        for (IDiversityMetric metric : measures.getDiversityMetrics()) {
            AverageMetric avgMetric = m_avgMetrics.getMetric(metric.getName());
            avgMetric.update(metric.getValue());
        }
    }

    public void compute (int count) {
        for (AverageMetric metric : m_avgMetrics.getMetrics()) {
            metric.compute(count);
        }
    }

    public String getEvalResultString () {
        return m_avgMetrics.getResultString();
    }
}
