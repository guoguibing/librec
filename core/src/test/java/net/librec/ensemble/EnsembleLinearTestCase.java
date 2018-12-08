package net.librec.ensemble;

import net.librec.BaseTestCase;
import net.librec.common.LibrecException;
import net.librec.conf.Configuration.Resource;
import net.librec.job.RecommenderJob;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;

import net.librec.ensemble.Ensemble;
import net.librec.ensemble.EnsembleLinear;
import java.util.List;

/**
 * @author logicxin
 */

public class EnsembleLinearTestCase extends BaseTestCase {

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();
    }

    /**
     *
     * @throws ClassNotFoundException
     * @throws LibrecException
     * @throws IOException
     */
    @Test
    public void testEnsemble() throws ClassNotFoundException, LibrecException, IOException, Exception{


         // Creat an ensemble object
        String configFilePath = "rec/ensemble/ensemble-linear.properties";
        EnsembleLinear ensembleJob = new EnsembleLinear(configFilePath);
        ensembleJob.trainModel();

        // Get ensemble weight
        // List<Double> weightList = ensembleJob.ensembelWeight();

        // Get ensemble result
        //List ensenbleResult = ensembleJob.recommendedResult();

        // Save ensembel result
        //Boolean ensenbleResultSave = ensembleJob.saveRecommendResult("rec/ensemble/ensemble-linear.properties");

    }
}
