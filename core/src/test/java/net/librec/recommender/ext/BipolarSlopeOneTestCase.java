package net.librec.recommender.ext;

import net.librec.BaseTestCase;
import net.librec.common.LibrecException;
import net.librec.conf.Configuration;
import net.librec.job.RecommenderJob;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;

/**
 * BipolarSlopeOne Test Case corresponds to BipolarSlopeOneRedommender
 * {@link net.librec.recommender.ext.BipolarSlopeOneRecommender}
 *
 * @author Qian Shaofeng
 *
 */
public class BipolarSlopeOneTestCase extends BaseTestCase {
    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();
    }

    /**
     * test the whole process of BipolarSlopeOneRecommender recommendation
     *
     * @throws ClassNotFoundException
     * @throws LibrecException
     * @throws IOException
     */
    @Test
    public void testRecommender() throws ClassNotFoundException, LibrecException, IOException {
        Configuration.Resource resource = new Configuration.Resource("rec/ext/bipolarslopeone-test.properties");
        conf.addResource(resource);
        RecommenderJob job = new RecommenderJob(conf);
        job.runJob();
    }
}
