package net.librec.recommender.cf.ranking;

import net.librec.BaseTestCase;
import net.librec.common.LibrecException;
import net.librec.conf.Configuration;
import net.librec.job.RecommenderJob;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;

/**
 * ListRankMF Test Case corresponds to ListRankMFRecommender
 * {@link net.librec.recommender.cf.ranking.ListRankMFRecommender}
 *
 * @author Jinyuanyuan
 */
public class ListRankMFRecommenderTestCase extends BaseTestCase{

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();
    }

    /**
     * test the whole process of ListwiseFM recommendation
     *
     * @throws ClassNotFoundException
     * @throws LibrecException
     * @throws IOException
     */
    @Test
    public void testRecommender() throws ClassNotFoundException, LibrecException, IOException {
        Configuration.Resource resource = new Configuration.Resource("rec/cf/ranking/listrankmf-test.properties");
        conf.addResource(resource);
        RecommenderJob job = new RecommenderJob(conf);
        job.runJob();
    }
}
