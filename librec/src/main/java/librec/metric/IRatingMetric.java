package librec.metric;

import librec.intf.Recommender;
import librec.util.Logs;

import java.util.List;

/**
 * Rating metrics are based on the difference between the user's rating and
 * that returned by the recommender.
 * Created by rdburke on 8/1/16.
 */
public interface IRatingMetric extends IMetric {
    public void updatePredicted(int user, int item,
                                double predicted, double actual,
                                Recommender rec);
}

// DEFAULT RATING METRICS
//	xMAE, xRMSE, xNMAE, xrMAE, xrRMSE, xMPE, xPerplexity,

class MetricMAE implements IRatingMetric {
    private double m_totalErr;
    private double m_mae;
    public String getName () { return "MAE";}
    public void init(Recommender rec) {
        m_totalErr = 0.0;
        m_mae = -1;
    }
    public void updatePredicted(int user, int item,
                                double predicted, double actual,
                                Recommender rec) {
        double err = Math.abs(actual - predicted);
        m_totalErr += err;
    }

    public void compute(int count) {
        m_mae = m_totalErr / count;
    }

    public double getValue() { return m_mae;}
    public String getValueAsString () {
        return MetricCollection.ValueFormatString.format(this.getName(), this.getValue());
    }
}

class MetricRMSE implements IRatingMetric {
    private double m_totalSqErr;
    private double m_rmse;
    public String getName () { return "RMSE";}
    public void init(Recommender rec) {
        m_totalSqErr = 0.0;
        m_rmse = -1;
    }
    public void updatePredicted(int user, int item,
                                double predicted, double actual,
                                Recommender rec) {
        double err = Math.abs(actual - predicted);
        m_totalSqErr += err * err;
    }

    public void compute(int count) {
        m_rmse = Math.sqrt(m_totalSqErr / count);
    }

    public double getValue() { return m_rmse;}
    public String getValueAsString () {
        return MetricCollection.ValueFormatString.format(this.getName(), this.getValue());
    }
}

class MetricRMAE implements IRatingMetric {
    private double m_totalErr;
    private double m_rmae;
    private double m_minRate;
    public String getName () { return "R_MAE";}
    public void init(Recommender rec) {
        m_totalErr = 0.0;
        m_rmae = -1;
        List<Double> ratingScale = rec.rateDao.getRatingScale();
        m_minRate = ratingScale.get(0);
    }

    public void updatePredicted(int user, int item,
                                double predicted, double actual,
                                Recommender rec) {
        // rounding prediction to the closest rating level
        double rPred = Math.round(predicted / m_minRate) * m_minRate;
        double err = Math.abs(actual - rPred);

        m_totalErr += err;
    }

    public void compute(int count) {
        m_rmae = Math.sqrt(m_totalErr / count);
    }

    public double getValue() { return m_rmae;}
    public String getValueAsString () {
        return MetricCollection.ValueFormatString.format(this.getName(), this.getValue());
    }
}

class MetricRRMSE implements IRatingMetric {
    private double m_totalSqErr;
    private double m_rrmse;
    private double m_minRate;
    public String getName () { return "R_RMSE";}
    public void init(Recommender rec) {
        m_totalSqErr = 0.0;
        m_rrmse = -1;
        List<Double>ratingScale = rec.rateDao.getRatingScale();
        m_minRate = ratingScale.get(0);
    }

    public void updatePredicted(int user, int item,
                                double predicted, double actual,
                                Recommender rec) {
        // rounding prediction to the closest rating level
        double rPred = Math.round(predicted / m_minRate) * m_minRate;
        double err = Math.abs(actual - rPred);
        m_totalSqErr += err * err;
    }

    public void compute(int count) {
        m_rrmse = Math.sqrt(m_totalSqErr / count);
    }

    public double getValue() { return m_rrmse;}
    public String getValueAsString () {
        return MetricCollection.ValueFormatString.format(this.getName(), this.getValue());
    }
}

class MetricNMAE implements IRatingMetric {
    private double m_totalErr;
    private double m_nmae;
    private double m_minRate, m_maxRate;
    public String getName () { return "NMAE";}
    public void init(Recommender rec) {
        m_totalErr = 0.0;
        m_nmae = -1;
        List<Double>ratingScale = rec.rateDao.getRatingScale();
        m_minRate = ratingScale.get(0);
        m_maxRate = ratingScale.get(ratingScale.size() - 1);
    }
    public void updatePredicted(int user, int item,
                                double predicted, double actual,
                                Recommender rec) {
        double err = Math.abs(actual - predicted);
        m_totalErr += err;
    }

    public void compute(int count) {
        double mae = m_totalErr / count;
        m_nmae = mae / (m_maxRate - m_minRate);
    }

    public double getValue() { return m_nmae;}
    public String getValueAsString () {
        return MetricCollection.ValueFormatString.format(this.getName(), this.getValue());
    }
}

class MetricMPE implements IRatingMetric {
    private int m_errCount;
    private static double s_threshold = 1e-5;
    private double m_mpe;
    private double m_minRate;
    public String getName () { return "MPE";}
    public void init(Recommender rec) {
        m_errCount = 0;
        m_mpe = -1;
        List<Double>ratingScale = rec.rateDao.getRatingScale();
        m_minRate = ratingScale.get(0);
    }

    public void updatePredicted(int user, int item,
                                double predicted, double actual,
                                Recommender rec) {
        // rounding prediction to the closest rating level
        double rPred = Math.round(predicted / m_minRate) * m_minRate;
        double err = Math.abs(actual - rPred);

        if (err > s_threshold) m_errCount++;
    }

    public void compute(int count) {
        m_mpe = (double)(m_errCount) / count;
    }

    public double getValue() { return m_mpe;}
    public String getValueAsString () {
        return MetricCollection.ValueFormatString.format(this.getName(), this.getValue());
    }
}

class Perplexity implements IRatingMetric {
    private double m_sumPerps;
    private double m_perp;
    public String getName () { return "Perplexity";}
    public void init(Recommender rec) {
        m_sumPerps = 0.0;
        m_perp = -1;
    }

    public void updatePredicted(int user, int item,
                                double predicted, double actual,
                                Recommender rec) {
        try {
            double perp = rec.perplexity(user, item, predicted);
            m_sumPerps += perp;
        } catch (Exception e) {
            Logs.debug("Error computing perplexity: " + e.toString());
        }
    }

    public void compute (int count) {
        if (m_sumPerps > 0) {
            m_perp = Math.exp(m_sumPerps / count);
        }
    }

    public double getValue() { return m_perp;}
    public String getValueAsString () {
        return MetricCollection.ValueFormatString.format(this.getName(), this.getValue());
    }
}
