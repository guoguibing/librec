package recommend.sample.movielens;

import java.io.IOException;

import junit.framework.TestCase;

public class RecommendMovieTest extends TestCase{
	public void testRun() throws IOException{
		String basePath = "/data/dl4j/data/movielens/ml-1m/";
		RecommendMovie.run(basePath);
	}
}
