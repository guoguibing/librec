package librec.metric;

import librec.intf.Recommender;
import librec.util.Measures;
import librec.util.Stats;

import java.util.List;
import java.util.ArrayList;

/**
 * Metrics that are based on the rank of the test items.
 * Created by rdburke on 8/1/16.
 *
 * 2016/8/2 RB It seems inefficient to store a bunch of copies of the results list. We might be
 * able to solve this by consolidating at the MetricDict level somehow.
 * @author rburke
 */

public interface IRankingMetric<T>  extends IMetric {

    /**
     * Updates the metric with a new set of results.
     * @param results
     * @param test
     * @param numDropped Needed by AUC measure
     */
    public void updateWithList(List<T> results, List<T> test, int numDropped);
}

		/* ranking-based measures */
//		xPre5, xPre10, xRec5, xRec10, xMAP, xMRR, xNDCG, xAUC,

class MetricPre5 implements IRankingMetric<Integer> {
    private double m_sumPrec;
    private double m_prec;
    public String getName () { return "Pre5";}

    public void init(Recommender rec) {
        m_sumPrec = 0.0;
        m_prec = -1;
    }

    public void updateWithList(List<Integer> results, List<Integer> test, int numDropped) {
        double prec = Measures.PrecAt(results, test, 5);
        m_sumPrec += prec;
    }

    public void compute(int count) {
        m_prec = m_sumPrec / count;
    }

    public double getValue() { return m_prec;}
    public String getValueAsString () {
        return MetricCollection.ValueFormatString.format(this.getName(), this.getValue());
    }
}

class MetricPre10 implements IRankingMetric<Integer> {
    private double m_sumPrec;
    private double m_prec;
    public String getName () { return "Pre10";}

    public void init(Recommender rec) {
        m_sumPrec = 0.0;
        m_prec = -1;
    }

    public void updateWithList(List<Integer> results, List<Integer> test, int numDropped) {
        double prec = Measures.PrecAt(results, test, 10);
        m_sumPrec += prec;
    }

    public void compute(int count) {
        m_prec = m_sumPrec / count;
    }

    public double getValue() { return m_prec;}
    public String getValueAsString () {
        return MetricCollection.ValueFormatString.format(this.getName(), this.getValue());
    }
}

class MetricRec5 implements IRankingMetric<Integer> {
    private double m_sumRec;
    private double m_rec;
    public String getName () { return "Rec5";}

    public void init(Recommender rec) {
        m_sumRec = 0.0;
        m_rec = -1;
    }

    public void updateWithList(List<Integer> results, List<Integer> test, int numDropped) {
        double rec = Measures.RecallAt(results, test, 5);
        m_sumRec += rec;
    }

    public void compute(int count) {
        m_rec = m_sumRec / count;
    }

    public double getValue() { return m_rec;}
    public String getValueAsString () {
        return MetricCollection.ValueFormatString.format(this.getName(), this.getValue());
    }
}

class MetricRec10 implements IRankingMetric<Integer> {
    private double m_sumRec;
    private double m_rec;
    public String getName () { return "Rec5";}

    public void init(Recommender rec) {
        m_sumRec = 0.0;
        m_rec = -1;
    }

    public void updateWithList(List<Integer> results, List<Integer> test, int numDropped) {
        double rec = Measures.RecallAt(results, test, 10);
        m_sumRec += rec;
    }

    public void compute(int count) {
        m_rec = m_sumRec / count;
    }

    public double getValue() { return m_rec;}
    public String getValueAsString () {
        return MetricCollection.ValueFormatString.format(this.getName(), this.getValue());
    }
}


class MetricMAP implements IRankingMetric<Integer> {
    private double m_sumAP;
    private double m_map;
    public String getName () { return "MAP";}

    public void init(Recommender rec) {
        m_sumAP = 0.0;
        m_map = -1;
    }

    public void updateWithList(List<Integer> results, List<Integer> test, int numDropped) {
        double ap = Measures.AP(results, test);
        m_sumAP += ap;
    }

    public void compute(int count) {
        m_map = m_sumAP / count;
    }

    public double getValue() { return m_map;}
    public String getValueAsString () {
        return MetricCollection.ValueFormatString.format(this.getName(), this.getValue());
    }
}


class MetricMRR implements IRankingMetric<Integer> {
    private double m_sumRR;
    private double m_mrr;
    public String getName () { return "MRR";}

    public void init(Recommender rec) {
        m_sumRR = 0.0;
        m_mrr = -1;
    }

    public void updateWithList(List<Integer> results, List<Integer> test, int numDropped) {
        double rr = Measures.RR(results, test);
        m_sumRR += rr;
    }

    public void compute(int count) {
        m_mrr = m_sumRR / count;
    }

    public double getValue() { return m_mrr;}
    public String getValueAsString () {
        return MetricCollection.ValueFormatString.format(this.getName(), this.getValue());
    }
}

class MetricAUC implements IRankingMetric<Integer> {
    private double m_sumAUC;
    private double m_auc;
    public String getName () { return "AUC";}

    public void init(Recommender rec) {
        m_sumAUC = 0.0;
        m_auc = -1;
    }

    public void updateWithList(List<Integer> results, List<Integer> test, int numDropped) {
        double auc = Measures.AUC(results, test, numDropped);
        m_sumAUC += auc;
    }

    public void compute(int count) {
        m_auc = m_sumAUC / count;
    }

    public double getValue() { return m_auc;}
    public String getValueAsString () {
        return MetricCollection.ValueFormatString.format(this.getName(), this.getValue());
    }
}

class MetricNDCG implements IRankingMetric<Integer> {
    private double m_sumNDCG;
    private double m_ndcg;
    public String getName () { return "NDCG";}

    public void init(Recommender rec) {
        m_sumNDCG = 0.0;
        m_ndcg = -1;
    }

    public void updateWithList(List<Integer> results, List<Integer> test, int numDropped) {
        double ndcg = Measures.RR(results, test);
        m_sumNDCG += ndcg;
    }

    public void compute(int count) {
        m_ndcg = m_sumNDCG / count;
    }

    public double getValue() { return m_ndcg;}
    public String getValueAsString () {
        return MetricCollection.ValueFormatString.format(this.getName(), this.getValue());
    }
}


