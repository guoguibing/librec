package librec.metric;

import java.util.ArrayList;
import java.util.List;
import java.util.Collection;

import librec.intf.Recommender;
import librec.util.Logs;

/**
 * Collects all the metrics to be calculated. There are four separate collections:
 * one for ratings, one for rankings, one for time, and one for diversity
 * Created by rdburke on 8/1/16.
 */
public class MetricCollection {

    public static String ValueFormatString = "%s: %.6f";
    public static String[] DefaultMetrics =
                    /* prediction-based measures */
            {"MetricMAE", "MetricRMSE", "MetricNMAE", "MetricRMAE", "MetricRRMSE", "MetricMPE", "Perplexity",
                    /* ranking-based measures */
                    "MetricPre5", "MetricPre10", "MetricRec5", "MetricRec10", "MetricMAP", "MetricMRR",
                    "MetricNDCG", "MetricAUC",
                    /* execution time */
                    "TrainTime", "TestTime"};

    private MetricDict<IRatingMetric> m_ratingMetrics;
    private MetricDict<IRankingMetric<Integer>> m_rankingMetrics;
    private MetricDict<ITimeMetric> m_timeMetrics;
    private MetricDict<IDiversityMetric<Integer>> m_diversityMetrics;

    public MetricCollection() {
        m_ratingMetrics = new MetricDict<IRatingMetric>();
        m_rankingMetrics = new MetricDict<IRankingMetric<Integer>>();
        m_timeMetrics = new MetricDict<ITimeMetric>();
        m_diversityMetrics = new MetricDict<IDiversityMetric<Integer>>();
    }

    public MetricCollection(List<String> classNames) throws ClassNotFoundException, IllegalAccessException,
            InstantiationException

    {
        m_ratingMetrics = new MetricDict<IRatingMetric>();
        m_rankingMetrics = new MetricDict<IRankingMetric<Integer>>();
        m_timeMetrics = new MetricDict<ITimeMetric>();
        m_diversityMetrics = new MetricDict<IDiversityMetric<Integer>>();

        Class ratingMetricIntf = Class.forName("librec.metric.IRatingMetric");
        Class rankingMetricIntf = Class.forName("librec.metric.IRankingMetric");
        Class timeMetricIntf = Class.forName("librec.metric.ITimeMetric");
        Class diversityMetricIntf = Class.forName("librec.metric.IDiversityMetric");

        for (String className : classNames) {
            Class metricClass = Class.forName(className);

            if (implementsInterface(metricClass, ratingMetricIntf)) {
                IRatingMetric metric = (IRatingMetric) metricClass.newInstance();
                m_ratingMetrics.addMetric(metric.getName(), metric);
            } else if (implementsInterface(metricClass, rankingMetricIntf)) {
                IRankingMetric<Integer> metric = (IRankingMetric<Integer>) metricClass.newInstance();
                m_rankingMetrics.addMetric(metric.getName(), metric);
            } else if (implementsInterface(metricClass, timeMetricIntf)) {
                ITimeMetric metric = (ITimeMetric) metricClass.newInstance();
                m_timeMetrics.addMetric(metric.getName(), metric);
            } else if (implementsInterface(metricClass, diversityMetricIntf)) {
                IDiversityMetric metric = (IDiversityMetric) metricClass.newInstance();
                m_diversityMetrics.addMetric(metric.getName(), metric);
            } else {
                Logs.debug("Unknown metric: " + className);
            }
        }
    }

    /**
     * Helper function
     * @param classObj
     * @param intf
     * @return True if the class implements the interface
     */
    private boolean implementsInterface(Class classObj, Class intf) {
        Class[] interfaces = classObj.getInterfaces();

        for (Class i : interfaces) {
            if (i.toString().equals(intf.toString())) {
                return true;
                // if this is true, the class implements the interface you're looking for
            }
        }
        return false;
    }

    public boolean hasRatingMetrics () { return !m_ratingMetrics.isEmpty(); }
    public boolean hasRankingMetrics () { return !m_rankingMetrics.isEmpty(); }
    public boolean hasTimeMetrics () { return !m_timeMetrics.isEmpty(); }
    public boolean hasDiversityMetrics () { return !m_diversityMetrics.isEmpty(); }

    public List<String> getRatingMetricNames () { return m_ratingMetrics.getNames(); }
    public List<String> getRankingMetricNames () { return m_rankingMetrics.getNames(); }
    public List<String> getTimeMetricNames () { return m_timeMetrics.getNames(); }
    public List<String> getDiversityMetricNames () { return m_ratingMetrics.getNames(); }

    public void setRatingMetrics (List<IRatingMetric> metrics) {
        for (IRatingMetric metric : metrics) {
            m_ratingMetrics.addMetric(metric.getName(), metric);
        }
    }

    public void setRankingMetrics (List<IRankingMetric> metrics) {
        for (IRankingMetric metric : metrics) {
            m_rankingMetrics.addMetric(metric.getName(), metric);
        }
    }

    public void setTimeMetrics (List<ITimeMetric> metrics) {
        for (ITimeMetric metric : metrics) {
            m_timeMetrics.addMetric(metric.getName(), metric);
        }
    }


    public IRatingMetric getRatingMetric (String name) {
        return m_ratingMetrics.getMetric(name);
    }

    public ITimeMetric getTimeMetric (String name) {
        return m_timeMetrics.getMetric(name);
    }

    public IRankingMetric getRankingMetric (String name) {
        return m_rankingMetrics.getMetric(name);
    }

    public IDiversityMetric getDiversityMetric (String name) {
        return m_diversityMetrics.getMetric(name);
    }

    public Collection<IRatingMetric> getRatingMetrics () { return m_ratingMetrics.getMetrics(); }

    public Collection<IRankingMetric<Integer>> getRankingMetrics () { return m_rankingMetrics.getMetrics(); }

    public Collection<ITimeMetric> getTimeMetrics () { return m_timeMetrics.getMetrics(); }

    public Collection<IDiversityMetric<Integer>> getDiversityMetrics () { return m_diversityMetrics.getMetrics(); }



    // Time metrics are really just storage locations, so they don't support
    // update or compute.
    public void init(Recommender rec) {
        m_ratingMetrics.initAll(rec);
        m_rankingMetrics.initAll(rec);
        m_timeMetrics.initAll(rec);
        m_diversityMetrics.initAll(rec);
    }

    public void updateRatingMetrics (int user, int item,
                                     double predicted, double actual, Recommender rec) {
        for (IRatingMetric metric : m_ratingMetrics.getMetrics()) {
            metric.updatePredicted(user, item, predicted, actual, rec);
        }
    }

    public void updateRankingMetrics (List<Integer> pred, List<Integer> correct, int numDropped,
                                      Recommender rec) {
        for (IRankingMetric<Integer> metric : m_rankingMetrics.getMetrics()) {
            metric.updateWithList(pred, correct, numDropped);
        }
    }

    public void updateDiversityMetrics (List<Integer> pred, Recommender rec) {
        for (IDiversityMetric<Integer> metric : m_diversityMetrics.getMetrics()) {
            metric.updateDiversity(pred, rec);
        }
    }

    public void computeRatingMetrics(int count) {
        m_ratingMetrics.computeAll(count);
    }

    public void computeRankingMetrics(int count) {
        m_rankingMetrics.computeAll(count);
    }

    public String getMetricNamesString () {
        if (m_rankingMetrics.isEmpty()) {
            return m_ratingMetrics.getResultString();
        } else if (m_ratingMetrics.isEmpty()) {
            return m_rankingMetrics.getResultString();
        } else {
            String ratingResults = m_ratingMetrics.getResultString();
            String rankingResults = m_rankingMetrics.getResultString();
            return ratingResults + "," + rankingResults;
        }
    }

    /**
     * For backward compatibility, does not return time information here.
     * @return
     */
    public String getEvalResultString () {
        StringBuffer buf = new StringBuffer();
        if (!m_ratingMetrics.isEmpty()) {
            buf.append(m_ratingMetrics.getResultString());
        }
        if (!m_rankingMetrics.isEmpty()) {
            if (buf.length() != 0) {
                buf.append(",");
            }
            buf.append(m_rankingMetrics.getResultString());
        }
        if (!m_diversityMetrics.isEmpty()) {
            if (buf.length() != 0) {
                buf.append(",");
            }
            buf.append(m_diversityMetrics.getResultString());
        }

        return buf.toString();
    }
}

