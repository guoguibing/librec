package librec.metric;

import librec.intf.Recommender;

/**
 * General interface for metrics that evaluate recommender systems.
 * Created by rdburke on 8/1/16.
 */
public interface IMetric {
    /**
     *
     * @return the name of the metric, used for lookup
     */
    public String getName ();

    /**
     * Initializes the metric
     * @param rec
     */
    public void init(Recommender rec);

    /**
     * After the values have been accumulated, this function does the final computation.
     * @param count
     */
    public void compute(int count);

    /**
     * Returns the value as a double
     * @return
     */
    public double getValue ();

    /**
     * Returns the value in string form.
     * @return
     */
    public String getValueAsString ();

    /**
     * Return the value annotated with the metric name, as in "Prec5: (0.375)"
     * @return
     */
    public String toString();
}

