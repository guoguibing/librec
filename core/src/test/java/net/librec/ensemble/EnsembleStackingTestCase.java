package net.librec.ensemble;

import net.librec.BaseTestCase;
import net.librec.common.LibrecException;
import net.librec.math.algorithm.Randoms;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;

import net.librec.ensemble.EnsembleStacking;
/**
 * @author logicxin
 */

public class EnsembleStackingTestCase extends BaseTestCase {

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
         //Ensemble ensembleJob = new EnsembleStacking("rec/ensemble/ensemble-linear.properties");
    }


    @Test
    public  void testKFold() throws ClassNotFoundException, LibrecException, IOException, Exception {
        // testCase  core code, resources, data
        System.out.println(Randoms.uniform());
        EnsembleStacking ensembleJob = new EnsembleStacking("rec/ensemble/ensemble-stacking.properties");
        ensembleJob.dataKFlodSpliter(5, 0.2);
        //System.out.println(Randoms.uniform());
    }
}
