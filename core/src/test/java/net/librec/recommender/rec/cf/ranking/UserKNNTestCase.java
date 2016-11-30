package net.librec.recommender.rec.cf.ranking;

import java.io.IOException;

import org.junit.Before;
import org.junit.Test;

import net.librec.BaseTestCase;
import net.librec.common.LibrecException;
import net.librec.conf.Configuration.Resource;
import net.librec.job.RecommenderJob;

public class UserKNNTestCase extends BaseTestCase {

	@Override
	@Before
	public void setUp() throws Exception {
		super.setUp();
	}

	@Test
	public void test() throws ClassNotFoundException, LibrecException, IOException {
		Resource resource = new Resource("rec/cf/userknn-testranking.properties");
		conf.addResource(resource);
		RecommenderJob job = new RecommenderJob(conf);
		job.runJob();
	}
}
