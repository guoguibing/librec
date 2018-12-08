package net.librec.ensemble;

import net.librec.BaseTestCase;
import net.librec.common.LibrecException;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;

/**
 * @author logicxin
 */

public class EnsembleWaterfallTestCase extends BaseTestCase {

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

         Ensemble ensembleJob = new EnsembleWaterfall("rec/ensemble/ensemble-linear.properties");

    }
}
