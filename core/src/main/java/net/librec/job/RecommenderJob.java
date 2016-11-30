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
package net.librec.job;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import net.librec.common.LibrecException;
import net.librec.conf.Configuration;
import net.librec.data.DataModel;
import net.librec.eval.RecommenderEvaluator;
import net.librec.eval.Measure.MeasureValue;
import net.librec.filter.RecommendedFilter;
import net.librec.recommender.Recommender;
import net.librec.recommender.RecommenderContext;
import net.librec.recommender.item.RecommendedItem;
import net.librec.similarity.RecommenderSimilarity;
import net.librec.util.DriverClassUtil;
import net.librec.util.FileUtil;
import net.librec.util.JobUtil;
import net.librec.util.ReflectionUtil;

/**
 * RecommenderJob
 * 
 * @author WangYuFeng
 */
public class RecommenderJob {
	/**
	 * LOG
	 */
	protected final Log LOG = LogFactory.getLog(this.getClass());

	private Configuration conf;

	public RecommenderJob(Configuration conf) {
		this.conf = conf;
		setJobId(JobUtil.generateNewJobId());
	}

	private void setJobId(String jobId) {
		conf.set("rec.job.id", jobId);
	}

	public void setRecommenderClass(String jobClass) {
		conf.set("rec.recommender.class", jobClass);
	}

	public void setRecommenderClass(Class<Recommender> jobClass) {
		conf.set("rec.recommender.class", jobClass.getName());
	}

	public Class<?> getConvertorClass() throws ClassNotFoundException, IOException {
		return DriverClassUtil.getClass(conf.get("data.convertor.format"));
	}
	
	public Class<?> getDataModelClass() throws ClassNotFoundException, IOException {
		return DriverClassUtil.getClass(conf.get("data.model.format"));
	}

	@SuppressWarnings("unchecked")
	public Class<RecommenderSimilarity> getSimilarityClass() {
		try {
			return (Class<RecommenderSimilarity>) DriverClassUtil.getClass(conf.get("rec.similarity.class"));
		} catch (ClassNotFoundException e) {
			return null;
		}
	}

	public Class<?> getRecommenderClass() throws ClassNotFoundException, IOException {
		return DriverClassUtil.getClass(conf.get("rec.recommender.class"));
	}

	public Class<?> getEvaluatorClass() throws ClassNotFoundException, IOException {
		return DriverClassUtil.getClass(conf.get("rec.eval.class"));
	}

	public Class<?> getFilterClass() throws ClassNotFoundException, IOException {
		return DriverClassUtil.getClass(conf.get("rec.filter.class"));
	}

	public void runJob() throws LibrecException, ClassNotFoundException, IOException {
		String modelSplit = conf.get("data.model.splitter");
		switch (modelSplit) {
		case "kcv": {
			int cvNumber = conf.getInt("data.splitter.cv.number", 1);
			for (int i = 0; i < cvNumber; i++) {
				conf.set("data.splitter.cv.index", String.valueOf(i));
				executeRecommenderJob();
			}
			break;
		}
		case "loocv": {
			int cvNumber = conf.getInt("data.splitter.cv.number", 1);
			for (int i = 0; i < cvNumber; i++) {
				conf.set("data.splitter.cv.index", String.valueOf(i));
				executeRecommenderJob();
			}
			break;
		}
		case "given": {
			executeRecommenderJob();
			break;
		}
		case "ratio": {
			executeRecommenderJob();
			break;
		}
		}
	}

	@SuppressWarnings("unchecked")
	private void executeRecommenderJob() throws ClassNotFoundException, LibrecException, IOException {
		DataModel dataModel = ReflectionUtil.newInstance((Class<DataModel>) this.getDataModelClass(), conf);
		dataModel.buildDataModel();
		RecommenderContext context = new RecommenderContext(conf, dataModel);
		if (getSimilarityClass() != null) {
			RecommenderSimilarity similarity = (RecommenderSimilarity) ReflectionUtil.newInstance(getSimilarityClass(), null);
			similarity.buildSimilarityMatrix(dataModel, conf.getBoolean("rec.similarity.category"));
			context.setSimilarity(similarity);
		}
		Recommender recommender = (Recommender) ReflectionUtil.newInstance((Class<Recommender>) getRecommenderClass(), conf);
		recommender.recommend(context);
		if (conf.getBoolean("rec.eval.enable")) {
			if (getEvaluatorClass() != null) {// Run the evaluator which is designated.
				RecommenderEvaluator evaluator = (RecommenderEvaluator) ReflectionUtil.newInstance(getEvaluatorClass(), null);
				double evalValue = recommender.evaluate(evaluator);
				LOG.info("Evaluator info:"+evaluator.getClass().getSimpleName() + " is " + evalValue);
			} else {// Run all evaluators
				Map<MeasureValue, Double> evalValueMap = recommender.evaluateMap();
				if (evalValueMap != null && evalValueMap.size() > 0) {
					for (Map.Entry<MeasureValue, Double> entry : evalValueMap.entrySet()) {
						if (entry != null && entry.getKey() != null) {
							if (entry.getKey().getTopN() != null && entry.getKey().getTopN() > 0) {
								LOG.info("Evaluator value:"+entry.getKey().getMeasure() + "topn " + entry.getKey().getTopN() + " is " + entry.getValue());
							} else {
								LOG.info("Evaluator value:"+entry.getKey().getMeasure() + " is " + entry.getValue());
							}
						}
					}
				}
			}
		}
		List<RecommendedItem> recommendedList = recommender.getRecommendedList();
		if (getFilterClass() != null) {
			RecommendedFilter filter = (RecommendedFilter) ReflectionUtil.newInstance(getFilterClass(), null);
			recommendedList = filter.filter(recommendedList);
		}
		saveResult(recommendedList);
	}

	public void saveResult(List<RecommendedItem> recommendedList) throws LibrecException, IOException, ClassNotFoundException{
        if (recommendedList != null && recommendedList.size() > 0) {
            //make output path
            String algoSimpleName = getRecommenderClass().getSimpleName();
            String outputPath = conf.get("dfs.result.dir")+"/"+conf.get("data.input.path") + "-" +algoSimpleName + "-output/"+algoSimpleName;
            LOG.info("Result path is " + outputPath);
            //convert itemList to string
            StringBuilder sb = new StringBuilder();
            for (RecommendedItem recItem : recommendedList){
                String userId = recItem.getUserId();
                String itemId = recItem.getItemId();
                String value = String.valueOf(recItem.getValue());
                sb.append(userId).append(",").append(itemId).append(",").append(value).append("\n");
            }
            String resultData = sb.toString();
            //save resultData
            try{
                FileUtil.writeString(outputPath, resultData);
            }catch (Exception e){
                e.printStackTrace();
            }
        }
	}
}
