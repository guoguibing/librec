// Copyright (C) 2014 Guibing Guo
//
// This file is part of LibRec.
//
// LibRec is free software: you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation, either version 3 of the License, or
// (at your option) any later version.
//
// LibRec is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with LibRec. If not, see <http://www.gnu.org/licenses/>.
//

package librec.main;

import happy.coding.io.FileIO;
import happy.coding.io.Logs;
import happy.coding.io.Strings;
import happy.coding.system.Systems;

import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

/**
 * A demo created for the UMAP'15 demo session, could be useful for other users.
 * 
 * @author Guo Guibing
 *
 */
public class Demo {

	public static void main(String[] args) {
		try {
			new Demo().execute(args);

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void execute(String[] args) throws Exception {

		// config logger
		String dirPath = FileIO.makeDirPath("demo");
		Logs.config(dirPath + "log4j.xml", true);

		// set the folder path for configuration files
		String configDirPath = FileIO.makeDirPath(dirPath, "config");

		// prepare candidate options
		List<String> candOptions = new ArrayList<>();
		candOptions.add("General Usage:");
		candOptions.add(" 0: the format of rating prediction results;");
		candOptions.add(" 1: the format of item recommendation results;");
		candOptions.add(" 2: run an algorithm by name [Input: 2 algoName];");
		candOptions.add(" 3: help & about this demo;");
		candOptions.add("-1: quit the demo!");
		candOptions.add("");
		candOptions.add("Part I: baselines");
		candOptions.add("10: Global Average;   11: User Average;  12: Item Average;");
		candOptions.add("13: Most Popularity;  14: User Cluster;  15: Item Cluster;");
		candOptions.add("16: Association Rule; 17: Non-neg MF;    18: Slope One;");
		candOptions.add("");
		candOptions.add("Part II: rating prediction");
		candOptions.add("20: UserKNN;\t 21: ItemKNN; \t 22: TrustSVD; ");
		candOptions.add("23: RegSVD; \t 24: BiasedMF;\t 25: SVD++; ");
		candOptions.add("");
		candOptions.add("Part III: item recommendation");
		candOptions.add("30: LDA;    \t 31: BPR;     \t 32: FISM; ");
		candOptions.add("33: WRMF;   \t 34: SLIM;    \t 35: RankALS. ");

		int option = 0;
		boolean flag = false;
		Scanner reader = new Scanner(System.in);
		String configFile = "librec.conf";
		do {
			Logs.debug(Strings.toSection(candOptions));
			System.out.print("Please choose your command id: ");
			option = reader.nextInt();

			// print an empty line
			Logs.debug();
			flag = false;

			// get algorithm-specific configuration file
			switch (option) {
			case 10:
				configFile = "GlobalAvg.conf";
				break;
			case 11:
				configFile = "UserAvg.conf";
				break;
			case 12:
				configFile = "ItemAvg.conf";
				break;
			case 13:
				configFile = "MostPop.conf";
				break;
			case 14:
				configFile = "UserCluster.conf";
				break;
			case 15:
				configFile = "ItemCluster.conf";
				break;
			case 16:
				configFile = "AR.conf";
				break;
			case 17:
				configFile = "NMF.conf";
				break;
			case 18:
				configFile = "SlopeOne.conf";
				break;
			case 20:
				configFile = "UserKNN.conf";
				break;
			case 21:
				configFile = "ItemKNN.conf";
				break;
			case 22:
				configFile = "TrustSVD.conf";
				break;
			case 23:
				configFile = "RegSVD.conf";
				break;
			case 24:
				configFile = "BiasedMF.conf";
				break;
			case 25:
				configFile = "SVD++.conf";
				break;
			case 30:
				configFile = "LDA.conf";
				break;
			case 31:
				configFile = "BPR.conf";
				break;
			case 32:
				configFile = "FISM.conf";
				break;
			case 33:
				configFile = "WRMF.conf";
				break;
			case 34:
				configFile = "SLIM.conf";
				break;
			case 35:
				configFile = "RankALS.conf";
				break;
			case -1:
				flag = true;
				break;
			case 0:
				Logs.debug("Prediction results: MAE, RMSE, NMAE, rMAE, rRMSE, MPE, <configuration>, training time, test time\n");
				Systems.pause();
				continue;
			case 1:
				Logs.debug("Ranking results: Prec@5, Prec@10, Recall@5, Recall@10, AUC, MAP, NDCG, MRR, <configuration>, training time, test time\n");
				Systems.pause();
				continue;
			case 2:
				// System.out.print("Please input the method name: ");
				String algoName = reader.next().trim();
				configFile = algoName + ".conf";
				break;
			case 3:
				StringBuilder about = new StringBuilder();
				about.append("About. This demo was created by Guo Guibing, the author of the LibRec library.\n")
						.append("It is based on LibRec-v1.3 (http://www.librec.net/). Although initially designed\n")
						.append("for a demo session at UMAP'15, it may be useful for those who want to take a \n")
						.append("quick trial of LibRec. Source code: https://github.com/guoguibing/librec.\n\n")
						.append("Usage. To run a predefined recommender, simply choose a recommender id.\n")
						.append("To run a customized recommender, give the input '2 algoName' (e.g., '2 RegSVD').\n")
						.append("For case 2, make sure you have a configuration file named by 'algoName.conf'\n");

				Logs.debug(about.toString());
				Systems.pause();
				continue;
			default:
				Logs.error("Wrong input id!\n");
				Systems.pause();
				continue;
			}

			if (flag)
				break;

			// run algorithm
			LibRec librec = new LibRec();
			librec.setConfigFiles(configDirPath + configFile);
			librec.execute(args);

			// await next command
			Logs.debug();
			Systems.pause();

		} while (option != -1);
		reader.close();

		Logs.debug("Thanks for trying out LibRec! See you again!");
	}
}
