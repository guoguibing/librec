/**
 * Copyright (C) 2016 LibRec
 *
 * This file is part of LibRec.
 * LibRec is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * LibRec is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with LibRec. If not, see <http://www.gnu.org/licenses/>.
 */
package net.librec.recommender.cf;

import net.librec.BaseTestCase;
import net.librec.common.LibrecException;
import net.librec.conf.Configuration;
import net.librec.conf.Configuration.Resource;
import net.librec.data.DataModel;
import net.librec.data.model.TextDataModel;
import net.librec.filter.GenericRecommendedFilter;
import net.librec.filter.RecommendedFilter;
import net.librec.job.RecommenderJob;
import net.librec.recommender.Recommender;
import net.librec.recommender.RecommenderContext;
import net.librec.recommender.item.RecommendedItem;
import net.librec.similarity.PCCSimilarity;
import net.librec.similarity.RecommenderSimilarity;
import net.librec.util.DriverClassUtil;
import org.junit.Before;
import org.junit.FixMethodOrder;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runners.MethodSorters;

import java.io.IOException;
import java.util.List;

/**
 * UserKnn Test Case corresponds to UserKNNRecommender
 * {@link net.librec.recommender.cf.UserKNNRecommender}
 *
 * @author liuxz
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class UserKNNTestCase extends BaseTestCase {

	@Override
	@Before
	public void setUp() throws Exception {
		super.setUp();
	}

	/**
	 * test the whole rating process of UserKNN recommendation
	 *
	 * @throws ClassNotFoundException
	 * @throws LibrecException
	 * @throws IOException
	 */
	// @Ignore
	@Test
	public void testRecommenderRating() throws ClassNotFoundException, LibrecException, IOException {
		Resource resource = new Resource("rec/cf/userknn-test.properties");
		conf.addResource(resource);
		RecommenderJob job = new RecommenderJob(conf);
		job.runJob();
	}

	/**
	 * test the whole ranking process of UserKNN recommendation
	 *
	 * @throws ClassNotFoundException
	 * @throws LibrecException
	 * @throws IOException
	 */
	// @Ignore
	@Test
	public void testRecommenderRanking() throws ClassNotFoundException, LibrecException, IOException {
		Resource resource = new Resource("rec/cf/userknn-testranking.properties");
		conf.addResource(resource);
		RecommenderJob job = new RecommenderJob(conf);
		job.runJob();
	}

	/**
	 * test the whole process of UserKNN recommendation
	 *
	 * @throws ClassNotFoundException
	 * @throws LibrecException
	 * @throws IOException
	 */
	@Ignore
	@Test
	public void test01SaveModel() throws ClassNotFoundException, LibrecException, IOException {
		Configuration conf = new Configuration();
		Configuration.Resource resource = new Resource("rec/cf/userknn-test.properties");
		conf.addResource(resource);
		DataModel dataModel = new TextDataModel(conf);
		dataModel.buildDataModel();
		RecommenderContext context = new RecommenderContext(conf, dataModel);
		RecommenderSimilarity similarity = new PCCSimilarity();
		// similarity.setConf(conf);
		// similarity.buildSimilarityMatrix(dataModel, true);
		context.setSimilarity(similarity);
		Recommender recommender = new UserKNNRecommender();
		recommender.recommend(context);
		String filePath = conf.get("dfs.result.dir") + "/model-"
				+ DriverClassUtil.getDriverName(UserKNNRecommender.class);
		recommender.saveModel(filePath);
	}

	/**
	 * test the whole process of UserKNN recommendation
	 *
	 * @throws ClassNotFoundException
	 * @throws LibrecException
	 * @throws IOException
	 */
	@Ignore
	@Test
	public void test02LoadModel() throws ClassNotFoundException, LibrecException, IOException {
		Configuration conf = new Configuration();
		Configuration.Resource resource = new Resource("rec/cf/userknn-test.properties");
		conf.addResource(resource);
		// DataModel dataModel = new TextDataModel(conf);
		// dataModel.buildDataModel();
		RecommenderContext context = new RecommenderContext(conf);
		// RecommenderSimilarity similarity = new PCCSimilarity();
		// similarity.setConf(conf);
		// similarity.buildSimilarityMatrix(dataModel, true);
		// context.setSimilarity(similarity);
		Recommender recommender = new UserKNNRecommender();
		recommender.setContext(context);
		String filePath = conf.get("dfs.result.dir") + "/model-"
				+ DriverClassUtil.getDriverName(UserKNNRecommender.class);
		recommender.loadModel(filePath);
		recommender.recommend(context);
		List<RecommendedItem> recommendedItemList = recommender.getRecommendedList();
		RecommendedFilter filter = new GenericRecommendedFilter();
		recommendedItemList = filter.filter(recommendedItemList);
	}

	/**
	 * Test RatioDataSplitter with rating ratio, evaluating enabled.
	 *
	 * @throws ClassNotFoundException
	 * @throws LibrecException
	 * @throws IOException
	 */
	@Test
	public void test03SplitterRatioRating() throws ClassNotFoundException, LibrecException, IOException {
		Resource resource = new Resource("rec/cf/userknn-test.properties");
		conf.addResource(resource);
		conf.set("rec.eval.enable", "true");
		conf.set("data.model.splitter", "ratio");
		conf.set("data.splitter.ratio", "rating");
		RecommenderJob job = new RecommenderJob(conf);
		job.runJob();
	}

	/**
	 * Test RatioDataSplitter with user ratio, evaluating enabled.
	 *
	 * @throws ClassNotFoundException
	 * @throws LibrecException
	 * @throws IOException
	 */
	@Test
	public void test04SplitterRatioUser() throws ClassNotFoundException, LibrecException, IOException {
		Resource resource = new Resource("rec/cf/userknn-test.properties");
		conf.addResource(resource);
		conf.set("rec.eval.enable", "true");
		conf.set("data.model.splitter", "ratio");
		conf.set("data.splitter.ratio", "user");
		RecommenderJob job = new RecommenderJob(conf);
		job.runJob();
	}

	/**
	 * Test RatioDataSplitter with user fixed ratio, evaluating enabled.
	 *
	 * @throws ClassNotFoundException
	 * @throws LibrecException
	 * @throws IOException
	 */
	@Test
	public void test05SplitterRatioUserFixed() throws ClassNotFoundException, LibrecException, IOException {
		Resource resource = new Resource("rec/cf/userknn-test.properties");
		conf.addResource(resource);
		conf.set("rec.eval.enable", "true");
		conf.set("data.model.splitter", "ratio");
		conf.set("data.splitter.ratio", "userfixed");
		RecommenderJob job = new RecommenderJob(conf);
		job.runJob();
	}

	/**
	 * Test RatioDataSplitter with item ratio, evaluating enabled.
	 *
	 * @throws ClassNotFoundException
	 * @throws LibrecException
	 * @throws IOException
	 */
	@Test
	public void test06SplitterRatioItem() throws ClassNotFoundException, LibrecException, IOException {
		Resource resource = new Resource("rec/cf/userknn-test.properties");
		conf.addResource(resource);
		conf.set("rec.eval.enable", "true");
		conf.set("data.model.splitter", "ratio");
		conf.set("data.splitter.ratio", "item");
		RecommenderJob job = new RecommenderJob(conf);
		job.runJob();
	}

	/**
	 * Test RatioDataSplitter with valid ratio, evaluating enabled.
	 *
	 * @throws ClassNotFoundException
	 * @throws LibrecException
	 * @throws IOException
	 */
	@Test
	public void test07SplitterRatioValid() throws ClassNotFoundException, LibrecException, IOException {
		Resource resource = new Resource("rec/cf/userknn-test.properties");
		conf.addResource(resource);
		conf.set("rec.eval.enable", "true");
		conf.set("data.model.splitter", "ratio");
		conf.set("data.splitter.ratio", "valid");
		conf.set("data.splitter.trainset.ratio", "0.5");
		conf.set("data.splitter.validset.ratio", "0.2");
		RecommenderJob job = new RecommenderJob(conf);
		job.runJob();
	}

	/**
	 * Test RatioDataSplitter with rating date ratio, evaluating enabled.
	 *
	 * @throws ClassNotFoundException
	 * @throws LibrecException
	 * @throws IOException
	 */
	@Test
	public void test08SplitterRatioRatingDate() throws ClassNotFoundException, LibrecException, IOException {
		Resource resource = new Resource("rec/cf/userknn-test.properties");
		conf.addResource(resource);
		conf.set("rec.eval.enable", "true");
		conf.set("data.model.splitter", "ratio");
		conf.set("data.input.path", "test/datamodeltest/ratings-date.txt");
		conf.set("data.column.format", "UIRT");
		conf.set("data.splitter.ratio", "ratingdate");
		RecommenderJob job = new RecommenderJob(conf);
		job.runJob();
	}

	/**
	 * Test RatioDataSplitter with user date ratio, evaluating enabled.
	 *
	 * @throws ClassNotFoundException
	 * @throws LibrecException
	 * @throws IOException
	 */
	@Test
	public void test09SplitterRatioUserDate() throws ClassNotFoundException, LibrecException, IOException {
		Resource resource = new Resource("rec/cf/userknn-test.properties");
		conf.addResource(resource);
		conf.set("rec.eval.enable", "true");
		conf.set("data.model.splitter", "ratio");
		conf.set("data.input.path", "test/datamodeltest/ratings-date.txt");
		conf.set("data.column.format", "UIRT");
		conf.set("data.splitter.ratio", "userdate");
		RecommenderJob job = new RecommenderJob(conf);
		job.runJob();
	}

	/**
	 * Test RatioDataSplitter with item date ratio, evaluating enabled.
	 *
	 * @throws ClassNotFoundException
	 * @throws LibrecException
	 * @throws IOException
	 */
	@Test
	public void test10SplitterRatioItemDate() throws ClassNotFoundException, LibrecException, IOException {
		Resource resource = new Resource("rec/cf/userknn-test.properties");
		conf.addResource(resource);
		conf.set("rec.eval.enable", "true");
		conf.set("data.model.splitter", "ratio");
		conf.set("data.input.path", "test/datamodeltest/ratings-date.txt");
		conf.set("data.column.format", "UIRT");
		conf.set("data.splitter.ratio", "itemdate");
		RecommenderJob job = new RecommenderJob(conf);
		job.runJob();
	}

	/**
	 * Test GivenNDataSplitter with user, evaluating enabled.
	 *
	 * @throws ClassNotFoundException
	 * @throws LibrecException
	 * @throws IOException
	 */
	@Test
	public void test11SplitterGivenNUser() throws ClassNotFoundException, LibrecException, IOException {
		Resource resource = new Resource("rec/cf/userknn-test.properties");
		conf.addResource(resource);
		conf.set("rec.eval.enable", "true");
		conf.set("data.model.splitter", "givenn");
		conf.set("data.splitter.givenn.n", "5");
		conf.set("data.splitter.givenn", "user");
		RecommenderJob job = new RecommenderJob(conf);
		job.runJob();
	}

	/**
	 * Test GivenNDataSplitter with item, evaluating enabled.
	 *
	 * @throws ClassNotFoundException
	 * @throws LibrecException
	 * @throws IOException
	 */
	@Test
	public void test12SplitterGivenNItem() throws ClassNotFoundException, LibrecException, IOException {
		Resource resource = new Resource("rec/cf/userknn-test.properties");
		conf.addResource(resource);
		conf.set("rec.eval.enable", "true");
		conf.set("data.model.splitter", "givenn");
		conf.set("data.splitter.givenn.n", "5");
		conf.set("data.splitter.givenn", "item");
		RecommenderJob job = new RecommenderJob(conf);
		job.runJob();
	}

	/**
	 * Test GivenNDataSplitter with userdate, evaluating enabled.
	 *
	 * @throws ClassNotFoundException
	 * @throws LibrecException
	 * @throws IOException
	 */
	@Test
	public void test13SplitterGivenNUserDate() throws ClassNotFoundException, LibrecException, IOException {
		Resource resource = new Resource("rec/cf/userknn-test.properties");
		conf.addResource(resource);
		conf.set("rec.eval.enable", "true");
		conf.set("data.model.splitter", "givenn");
		conf.set("data.splitter.givenn.n", "5");
		conf.set("data.input.path", "test/datamodeltest/ratings-date.txt");
		conf.set("data.column.format", "UIRT");
		conf.set("data.splitter.givenn", "userdate");
		RecommenderJob job = new RecommenderJob(conf);
		job.runJob();
	}

	/**
	 * Test GivenNDataSplitter with itemdate, evaluating enabled.
	 *
	 * @throws ClassNotFoundException
	 * @throws LibrecException
	 * @throws IOException
	 */
	@Test
	public void test14SplitterGivenNItemDate() throws ClassNotFoundException, LibrecException, IOException {
		Resource resource = new Resource("rec/cf/userknn-test.properties");
		conf.addResource(resource);
		conf.set("rec.eval.enable", "true");
		conf.set("data.model.splitter", "givenn");
		conf.set("data.splitter.givenn.n", "5");
		conf.set("data.input.path", "test/datamodeltest/ratings-date.txt");
		conf.set("data.column.format", "UIRT");
		conf.set("data.splitter.givenn", "itemdate");
		RecommenderJob job = new RecommenderJob(conf);
		job.runJob();
	}

	/**
	 * Test KCVDataSplitter, evaluating enabled.
	 *
	 * @throws ClassNotFoundException
	 * @throws LibrecException
	 * @throws IOException
	 */
	@Test
	public void test15SplitterKCV() throws ClassNotFoundException, LibrecException, IOException {
		Resource resource = new Resource("rec/cf/userknn-test.properties");
		conf.addResource(resource);
		conf.set("rec.eval.enable", "true");
		conf.set("data.model.splitter", "kcv");
		conf.set("data.splitter.cv.number", "5");
		RecommenderJob job = new RecommenderJob(conf);
		job.runJob();
	}

	/**
	 * Test LOOCVDataSplitter with user, evaluating enabled.
	 *
	 * @throws ClassNotFoundException
	 * @throws LibrecException
	 * @throws IOException
	 */
	@Test
	public void test16SplitterLOOCVUser() throws ClassNotFoundException, LibrecException, IOException {
		Resource resource = new Resource("rec/cf/userknn-test.properties");
		conf.addResource(resource);
		conf.set("data.model.splitter", "loocv");
		conf.set("data.splitter.loocv", "user");
		RecommenderJob job = new RecommenderJob(conf);
		job.runJob();
	}

	/**
	 * Test LOOCVDataSplitter with item, evaluating enabled.
	 *
	 * @throws ClassNotFoundException
	 * @throws LibrecException
	 * @throws IOException
	 */
	@Test
	public void test17SplitterLOOCVItem() throws ClassNotFoundException, LibrecException, IOException {
		Resource resource = new Resource("rec/cf/userknn-test.properties");
		conf.addResource(resource);
		conf.set("data.model.splitter", "loocv");
		conf.set("data.splitter.loocv", "item");
		RecommenderJob job = new RecommenderJob(conf);
		job.runJob();
	}

	/**
	 * Test LOOCVDataSplitter with user date, evaluating enabled.
	 *
	 * @throws ClassNotFoundException
	 * @throws LibrecException
	 * @throws IOException
	 */
	@Test
	public void test18SplitterLOOCVUserDate() throws ClassNotFoundException, LibrecException, IOException {
		Resource resource = new Resource("rec/cf/userknn-test.properties");
		conf.addResource(resource);
		conf.set("data.model.splitter", "loocv");
		conf.set("data.input.path", "test/datamodeltest/ratings-date.txt");
		conf.set("data.column.format", "UIRT");
		conf.set("data.splitter.loocv", "userdate");
		RecommenderJob job = new RecommenderJob(conf);
		job.runJob();
	}

	/**
	 * Test LOOCVDataSplitter with item date, evaluating enabled.
	 *
	 * @throws ClassNotFoundException
	 * @throws LibrecException
	 * @throws IOException
	 */
	@Test
	public void test19SplitterLOOCVItemDate() throws ClassNotFoundException, LibrecException, IOException {
		Resource resource = new Resource("rec/cf/userknn-test.properties");
		conf.addResource(resource);
		conf.set("data.model.splitter", "loocv");
		conf.set("data.input.path", "test/datamodeltest/ratings-date.txt");
		conf.set("data.column.format", "UIRT");
		conf.set("data.splitter.loocv", "itemdate");
		RecommenderJob job = new RecommenderJob(conf);
		job.runJob();
	}
	
	/**
	 * Test LOOCVDataSplitter with rate.
	 *
	 * @throws ClassNotFoundException
	 * @throws LibrecException
	 * @throws IOException
	 */
	@Test
	public void test191SplitterLOOCVRate() throws ClassNotFoundException, LibrecException, IOException {
		Resource resource = new Resource("rec/cf/userknn-test.properties");
		conf.addResource(resource);
		conf.set("data.model.splitter", "loocv");
		conf.set("data.input.path", "test/datamodeltest/matrix4by4A.txt");
		conf.set("data.splitter.loocv", "rate");
		RecommenderJob job = new RecommenderJob(conf);
		job.runJob();
	}

	/**
	 * Test RatioDataSplitter with rate.
	 *
	 * @throws ClassNotFoundException
	 * @throws LibrecException
	 * @throws IOException
	 */
	@Test
	public void test192SplitterTestSet() throws ClassNotFoundException, LibrecException, IOException {
		Resource resource = new Resource("rec/cf/userknn-test.properties");
		conf.addResource(resource);
		conf.set("data.model.splitter", "testset");
		conf.set("data.input.path", "test/given-testset");
		conf.set("data.testset.path", "test/given-testset/test");
		RecommenderJob job = new RecommenderJob(conf);
		job.runJob();
	}
	/**
	 * Test RatioDataSplitter with rating ratio, filter enabled.
	 *
	 * @throws ClassNotFoundException
	 * @throws LibrecException
	 * @throws IOException
	 */
	@Test
	public void test20SplitterRatioRating() throws ClassNotFoundException, LibrecException, IOException {
		Resource resource = new Resource("rec/cf/userknn-test.properties");
		conf.addResource(resource);
		conf.set("rec.eval.enable", "false");
		conf.set("rec.filter.class", "generic");
		conf.set("data.model.splitter", "ratio");
		conf.set("data.splitter.ratio", "rating");
		RecommenderJob job = new RecommenderJob(conf);
		job.runJob();
	}

	/**
	 * Test RatioDataSplitter with user ratio, filter enabled.
	 *
	 * @throws ClassNotFoundException
	 * @throws LibrecException
	 * @throws IOException
	 */
	@Test
	public void test21SplitterRatioUser() throws ClassNotFoundException, LibrecException, IOException {
		Resource resource = new Resource("rec/cf/userknn-test.properties");
		conf.addResource(resource);
		conf.set("rec.eval.enable", "false");
		conf.set("rec.filter.class", "generic");
		conf.set("data.model.splitter", "ratio");
		conf.set("data.splitter.ratio", "user");
		RecommenderJob job = new RecommenderJob(conf);
		job.runJob();
	}

	/**
	 * Test RatioDataSplitter with user fixed ratio, filter enabled.
	 *
	 * @throws ClassNotFoundException
	 * @throws LibrecException
	 * @throws IOException
	 */
	@Test
	public void test22SplitterRatioUserFixed() throws ClassNotFoundException, LibrecException, IOException {
		Resource resource = new Resource("rec/cf/userknn-test.properties");
		conf.addResource(resource);
		conf.set("rec.eval.enable", "false");
		conf.set("rec.filter.class", "generic");
		conf.set("data.model.splitter", "ratio");
		conf.set("data.splitter.ratio", "userfixed");
		RecommenderJob job = new RecommenderJob(conf);
		job.runJob();
	}

	/**
	 * Test RatioDataSplitter with item ratio, filter enabled.
	 *
	 * @throws ClassNotFoundException
	 * @throws LibrecException
	 * @throws IOException
	 */
	@Test
	public void test23SplitterRatioItem() throws ClassNotFoundException, LibrecException, IOException {
		Resource resource = new Resource("rec/cf/userknn-test.properties");
		conf.addResource(resource);
		conf.set("rec.eval.enable", "false");
		conf.set("rec.filter.class", "generic");
		conf.set("data.model.splitter", "ratio");
		conf.set("data.splitter.ratio", "item");
		RecommenderJob job = new RecommenderJob(conf);
		job.runJob();
	}

	/**
	 * Test RatioDataSplitter with valid ratio, filter enabled.
	 *
	 * @throws ClassNotFoundException
	 * @throws LibrecException
	 * @throws IOException
	 */
	@Test
	public void test24SplitterRatioValid() throws ClassNotFoundException, LibrecException, IOException {
		Resource resource = new Resource("rec/cf/userknn-test.properties");
		conf.addResource(resource);
		conf.set("rec.eval.enable", "false");
		conf.set("rec.filter.class", "generic");
		conf.set("data.model.splitter", "ratio");
		conf.set("data.splitter.ratio", "valid");
		conf.set("data.splitter.trainset.ratio", "0.5");
		conf.set("data.splitter.validset.ratio", "0.2");
		RecommenderJob job = new RecommenderJob(conf);
		job.runJob();
	}

	/**
	 * Test RatioDataSplitter with rating date ratio, filter enabled.
	 *
	 * @throws ClassNotFoundException
	 * @throws LibrecException
	 * @throws IOException
	 */
	@Test
	public void test25SplitterRatioRatingDate() throws ClassNotFoundException, LibrecException, IOException {
		Resource resource = new Resource("rec/cf/userknn-test.properties");
		conf.addResource(resource);
		conf.set("rec.eval.enable", "false");
		conf.set("rec.filter.class", "generic");
		conf.set("data.model.splitter", "ratio");
		conf.set("data.input.path", "test/datamodeltest/ratings-date.txt");
		conf.set("data.column.format", "UIRT");
		conf.set("data.splitter.ratio", "ratingdate");
		RecommenderJob job = new RecommenderJob(conf);
		job.runJob();
	}

	/**
	 * Test RatioDataSplitter with user date ratio, filter enabled.
	 *
	 * @throws ClassNotFoundException
	 * @throws LibrecException
	 * @throws IOException
	 */
	@Test
	public void test26SplitterRatioUserDate() throws ClassNotFoundException, LibrecException, IOException {
		Resource resource = new Resource("rec/cf/userknn-test.properties");
		conf.addResource(resource);
		conf.set("rec.eval.enable", "false");
		conf.set("rec.filter.class", "generic");
		conf.set("data.model.splitter", "ratio");
		conf.set("data.input.path", "test/datamodeltest/ratings-date.txt");
		conf.set("data.column.format", "UIRT");
		conf.set("data.splitter.ratio", "userdate");
		RecommenderJob job = new RecommenderJob(conf);
		job.runJob();
	}

	/**
	 * Test RatioDataSplitter with item date ratio, filter enabled.
	 *
	 * @throws ClassNotFoundException
	 * @throws LibrecException
	 * @throws IOException
	 */
	@Test
	public void test27SplitterRatioItemDate() throws ClassNotFoundException, LibrecException, IOException {
		Resource resource = new Resource("rec/cf/userknn-test.properties");
		conf.addResource(resource);
		conf.set("rec.eval.enable", "false");
		conf.set("rec.filter.class", "generic");
		conf.set("data.model.splitter", "ratio");
		conf.set("data.input.path", "test/datamodeltest/ratings-date.txt");
		conf.set("data.column.format", "UIRT");
		conf.set("data.splitter.ratio", "itemdate");
		RecommenderJob job = new RecommenderJob(conf);
		job.runJob();
	}

	/**
	 * Test GivenNDataSplitter with user, filter enabled.
	 *
	 * @throws ClassNotFoundException
	 * @throws LibrecException
	 * @throws IOException
	 */
	@Test
	public void test28SplitterGivenNUser() throws ClassNotFoundException, LibrecException, IOException {
		Resource resource = new Resource("rec/cf/userknn-test.properties");
		conf.addResource(resource);
		conf.set("rec.eval.enable", "false");
		conf.set("rec.filter.class", "generic");
		conf.set("data.model.splitter", "givenn");
		conf.set("data.splitter.givenn.n", "5");
		conf.set("data.splitter.givenn", "user");
		RecommenderJob job = new RecommenderJob(conf);
		job.runJob();
	}

	/**
	 * Test GivenNDataSplitter with item, filter enabled.
	 *
	 * @throws ClassNotFoundException
	 * @throws LibrecException
	 * @throws IOException
	 */
	@Test
	public void test29SplitterGivenNItem() throws ClassNotFoundException, LibrecException, IOException {
		Resource resource = new Resource("rec/cf/userknn-test.properties");
		conf.addResource(resource);
		conf.set("rec.eval.enable", "false");
		conf.set("rec.filter.class", "generic");
		conf.set("data.model.splitter", "givenn");
		conf.set("data.splitter.givenn.n", "5");
		conf.set("data.splitter.givenn", "item");
		RecommenderJob job = new RecommenderJob(conf);
		job.runJob();
	}

	/**
	 * Test GivenNDataSplitter with user date, filter enabled.
	 *
	 * @throws ClassNotFoundException
	 * @throws LibrecException
	 * @throws IOException
	 */
	@Test
	public void test30SplitterGivenNUserDate() throws ClassNotFoundException, LibrecException, IOException {
		Resource resource = new Resource("rec/cf/userknn-test.properties");
		conf.addResource(resource);
		conf.set("rec.eval.enable", "false");
		conf.set("rec.filter.class", "generic");
		conf.set("data.model.splitter", "givenn");
		conf.set("data.splitter.givenn.n", "5");
		conf.set("data.input.path", "test/datamodeltest/ratings-date.txt");
		conf.set("data.column.format", "UIRT");
		conf.set("data.splitter.givenn", "userdate");
		RecommenderJob job = new RecommenderJob(conf);
		job.runJob();
	}

	/**
	 * Test GivenNDataSplitter with item date, filter enabled.
	 *
	 * @throws ClassNotFoundException
	 * @throws LibrecException
	 * @throws IOException
	 */
	@Test
	public void test31SplitterGivenNItemDate() throws ClassNotFoundException, LibrecException, IOException {
		Resource resource = new Resource("rec/cf/userknn-test.properties");
		conf.addResource(resource);
		conf.set("rec.eval.enable", "false");
		conf.set("rec.filter.class", "generic");
		conf.set("data.model.splitter", "givenn");
		conf.set("data.splitter.givenn.n", "5");
		conf.set("data.input.path", "test/datamodeltest/ratings-date.txt");
		conf.set("data.column.format", "UIRT");
		conf.set("data.splitter.givenn", "itemdate");
		RecommenderJob job = new RecommenderJob(conf);
		job.runJob();
	}

	/**
	 * Test KCVDataSplitter, filter enabled.
	 *
	 * @throws ClassNotFoundException
	 * @throws LibrecException
	 * @throws IOException
	 */
	@Test
	public void test32SplitterKCV() throws ClassNotFoundException, LibrecException, IOException {
		Resource resource = new Resource("rec/cf/userknn-test.properties");
		conf.addResource(resource);
		conf.set("rec.eval.enable", "false");
		conf.set("rec.filter.class", "generic");
		conf.set("data.model.splitter", "kcv");
		conf.set("data.splitter.cv.number", "5");
		RecommenderJob job = new RecommenderJob(conf);
		job.runJob();
	}

	/**
	 * Test LOOCVDataSplitter with user, filter enabled.
	 *
	 * @throws ClassNotFoundException
	 * @throws LibrecException
	 * @throws IOException
	 */
	@Test
	public void test33SplitterLOOCVUser() throws ClassNotFoundException, LibrecException, IOException {
		Resource resource = new Resource("rec/cf/userknn-test.properties");
		conf.addResource(resource);
		conf.set("rec.eval.enable", "false");
		conf.set("rec.filter.class", "generic");
		conf.set("data.model.splitter", "loocv");
		conf.set("data.splitter.loocv", "user");
		RecommenderJob job = new RecommenderJob(conf);
		job.runJob();
	}

	/**
	 * Test LOOCVDataSplitter with item, filter enabled.
	 *
	 * @throws ClassNotFoundException
	 * @throws LibrecException
	 * @throws IOException
	 */
	@Test
	public void test34SplitterLOOCVItem() throws ClassNotFoundException, LibrecException, IOException {
		Resource resource = new Resource("rec/cf/userknn-test.properties");
		conf.addResource(resource);
		conf.set("rec.eval.enable", "false");
		conf.set("rec.filter.class", "generic");
		conf.set("data.model.splitter", "loocv");
		conf.set("data.splitter.loocv", "item");
		RecommenderJob job = new RecommenderJob(conf);
		job.runJob();
	}

	/**
	 * Test LOOCVDataSplitter with user date, filter enabled.
	 *
	 * @throws ClassNotFoundException
	 * @throws LibrecException
	 * @throws IOException
	 */
	@Test
	public void test35SplitterLOOCVUserDate() throws ClassNotFoundException, LibrecException, IOException {
		Resource resource = new Resource("rec/cf/userknn-test.properties");
		conf.addResource(resource);
		conf.set("rec.eval.enable", "false");
		conf.set("rec.filter.class", "generic");
		conf.set("data.model.splitter", "loocv");
		conf.set("data.input.path", "test/datamodeltest/ratings-date.txt");
		conf.set("data.column.format", "UIRT");
		conf.set("data.splitter.loocv", "userdate");
		RecommenderJob job = new RecommenderJob(conf);
		job.runJob();
	}

	/**
	 * Test LOOCVDataSplitter with item date, filter enabled.
	 *
	 * @throws ClassNotFoundException
	 * @throws LibrecException
	 * @throws IOException
	 */
	@Test
	public void test36SplitterLOOCVItemDate() throws ClassNotFoundException, LibrecException, IOException {
		Resource resource = new Resource("rec/cf/userknn-test.properties");
		conf.addResource(resource);
		conf.set("rec.eval.enable", "false");
		conf.set("rec.filter.class", "generic");
		conf.set("data.model.splitter", "loocv");
		conf.set("data.input.path", "test/datamodeltest/ratings-date.txt");
		conf.set("data.column.format", "UIRT");
		conf.set("data.splitter.loocv", "itemdate");
		RecommenderJob job = new RecommenderJob(conf);
		job.runJob();
	}
}
