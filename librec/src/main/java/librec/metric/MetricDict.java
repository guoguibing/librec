package librec.metric;

import librec.intf.Recommender;

import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.Collection;

/**
 * Utility class to associate metric names with the metric objects.
 * Created by rdburke on 8/1/16.
 */
public class MetricDict<T extends IMetric> {
    Map<String, T> m_map;

    public MetricDict() {
        m_map = new HashMap<String, T>();
    }

    public boolean isEmpty() {
        return m_map.isEmpty();
    }

    public void addMetric(String name, T metric) {
        m_map.put(name, metric);
    }

    public T getMetric(String name) {
        return m_map.get(name);
    }

    public Collection<T> getMetrics () {
        return m_map.values();
    }

    public boolean hasMetric(String name) {
        return m_map.containsKey(name);
    }

    public void initAll(Recommender rec) {
        for (T metric : m_map.values()) {
            metric.init(rec);
        }
    }

    public void computeAll(int count) {
        for (T metric : m_map.values()) {
            metric.compute(count);
        }
    }

    public String getResultString () {
        StringBuffer buf = new StringBuffer();
        List<String> names = getNames();
        for (String name : names) {
            T metric = m_map.get(name);
            String result = String.format("%.6f,", metric.getValue());
            buf.append(result);
        }
        // Remove final comma
        buf.deleteCharAt(buf.length()-1);
        return buf.toString();
    }

    public List<String> getNames () {
        List<String> names = new ArrayList(m_map.keySet());
        java.util.Collections.sort(names);
        return names;
    }

    public String getNamesString () {
        StringBuffer buf = new StringBuffer();
        List<String> names = getNames();
        for (String name : names) {
            buf.append(name);
            buf.append(",");
        }
        // Remove final comma
        buf.deleteCharAt(buf.length());
        return buf.toString();
    }
 }
