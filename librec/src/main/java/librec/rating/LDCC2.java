package librec.rating;

import static happy.coding.math.Gamma.digamma;
import happy.coding.io.Logs;
import happy.coding.io.Strings;
import librec.data.Configuration;
import librec.data.DenseMatrix;
import librec.data.DenseVector;
import librec.data.MatrixEntry;
import librec.data.SparseMatrix;
import librec.intf.GraphicRecommender;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;

/**
 * LDCC: Bayesian Co-clustering (BCC) with Gibbs sampling <br>
 * 
 * Wang et al., <strong>Latent Dirichlet Bayesian Co-Clustering</strong>, Machine Learning and Knowledge Discovery in
 * Databases, 2009.
 * 
 * @author Guo Guibing
 *
 */
@Configuration("Ku, Kv, au, av, beta, numIters, burnIn, sampleLag")
public class LDCC2 extends GraphicRecommender {

	private Table<Integer, Integer, Integer> Zu, Zv;

	private DenseMatrix Nui, Nvj;
	private DenseVector Nv;

	private int[][][] Nijl;
	private DenseMatrix Nij;

	private int Ku, Kv;

	private float au, av, bl;
	private DenseVector auVec, avVec, blVec;

	// parameters
	private DenseMatrix PIu, PIv, PIuSum, PIvSum;
	private double[][][] theta, thetaSum;

	public LDCC2(SparseMatrix trainMatrix, SparseMatrix testMatrix, int fold) {
		super(trainMatrix, testMatrix, fold);
	}

	@Override
	protected void initModel() throws Exception {
		Ku = algoOptions.getInt("-ku", numFactors);
		Kv = algoOptions.getInt("-kv", numFactors);

		Nui = new DenseMatrix(numUsers, Ku);
		Nu = new DenseVector(numUsers);

		Nvj = new DenseMatrix(numItems, Kv);
		Nv = new DenseVector(numItems);

		Nijl = new int[Ku][Kv][numLevels];
		Nij = new DenseMatrix(Ku, Kv);

		auVec = new DenseVector(Ku);
		au = algoOptions.getFloat("-au", 1.0f / Ku); // alpha for user
		auVec.setAll(au);

		avVec = new DenseVector(Kv);
		av = algoOptions.getFloat("-av", 1.0f / Kv); // alpha for item
		avVec.setAll(av);

		blVec = new DenseVector(numLevels);
		bl = algoOptions.getFloat("-beta", 1.0f / numLevels); // beta for rating levels
		blVec.setAll(bl);

		Zu = HashBasedTable.create();
		Zv = HashBasedTable.create();

		for (MatrixEntry me : trainMatrix) {
			int u = me.row();
			int v = me.column();
			double rate = me.get();
			int l = scales.indexOf(rate);

			int i = (int) (Ku * Math.random());
			int j = (int) (Kv * Math.random());

			Nui.add(u, i, 1);
			Nu.add(u, 1);

			Nvj.add(v, j, 1);
			Nv.add(v, 1);

			Nijl[i][j][l]++;
			Nij.add(i, j, 1);

			Zu.put(u, v, i);
			Zv.put(u, v, j);
		}

		// parameters
		PIuSum = new DenseMatrix(numUsers, Ku);
		PIvSum = new DenseMatrix(numItems, Kv);
		theta = new double[Ku][Kv][numLevels];
		thetaSum = new double[Ku][Kv][numLevels];
	}

	@Override
	protected void eStep() {
		double sumAu = auVec.sum(), sumAv = avVec.sum(), sumBl = blVec.sum();

		for (MatrixEntry me : trainMatrix) {
			int u = me.row();
			int v = me.column();
			double rate = me.get();
			int l = scales.indexOf(rate);

			// user and item's factors
			int i = Zu.get(u, v);
			int j = Zv.get(u, v);

			// remove this observation
			Nui.add(u, i, -1);
			Nu.add(u, -1);

			Nvj.add(v, j, -1);
			Nv.add(v, -1);

			Nijl[i][j][l]--;
			Nij.add(i, j, -1);

			// compute P(i, j)
			DenseMatrix probs = new DenseMatrix(Ku, Kv);
			double sum = 0;
			for (int m = 0; m < Ku; m++) {
				for (int n = 0; n < Kv; n++) {
					// compute Pmn
					double v1 = (Nui.get(u, m) + au) / (Nu.get(u) + sumAu);
					double v2 = (Nvj.get(v, n) + av) / (Nv.get(v) + sumAv);
					double v3 = (Nijl[m][n][l] + bl) / (Nij.get(m, n) + sumBl);

					double prob = v1 * v2 * v3;
					probs.set(m, n, prob);
					sum += prob;
				}
			}

			probs = probs.scale(1.0 / sum);

			// re-sample user factor
			double[] Pu = new double[Ku];
			for (int m = 0; m < Ku; m++) {
				Pu[m] = probs.sumOfRow(m);
			}
			for (int m = 1; m < Ku; m++) {
				Pu[m] += Pu[m - 1];
			}

			double rand = Math.random();
			for (i = 0; i < Ku; i++) {
				if (rand < Pu[i])
					break;
			}

			// re-sample item factor
			double[] Pv = new double[Kv];
			for (int n = 0; n < Kv; n++) {
				Pv[n] = probs.sumOfColumn(n);
			}
			for (int n = 1; n < Kv; n++) {
				Pv[n] += Pv[n - 1];
			}

			rand = Math.random();
			for (j = 0; j < Kv; j++) {
				if (rand < Pv[j])
					break;
			}

			// add statistics
			Nui.add(u, i, 1);
			Nu.add(u, 1);

			Nvj.add(v, j, 1);
			Nv.add(v, 1);

			Nijl[i][j][l]++;
			Nij.add(i, j, 1);

			Zu.put(u, v, i);
			Zv.put(u, v, j);
		}
	}

	@Override
	protected void mStep() {
		double sumAu = auVec.sum(), sumAv = avVec.sum(), sumBl = blVec.sum();
		double ai, aj, bl;

		// update user hyperparameter vector
		for (int i = 0; i < Ku; i++) {

			ai = auVec.get(i);
			double numerator = 0, denominator = 0;
			for (int u = 0; u < numUsers; u++) {
				numerator += digamma(Nui.get(u, i) + ai) - digamma(ai);
				denominator += digamma(Nu.get(u) + sumAu) - digamma(sumAu);
			}
			auVec.set(i, ai * (numerator / denominator));
		}

		// update item hyperparameter vector
		for (int j = 0; j < Kv; j++) {

			aj = avVec.get(j);
			double numerator = 0, denominator = 0;
			for (int v = 0; v < numItems; v++) {
				numerator += digamma(Nvj.get(v, j) + aj) - digamma(aj);
				denominator += digamma(Nv.get(v) + sumAv) - digamma(sumAv);
			}
			avVec.set(j, aj * (numerator / denominator));
		}

		// update beta_k
		for (int l = 0; l < numLevels; l++) {
			bl = blVec.get(l);
			double numerator = 0, denominator = 0;
			for (int i = 0; i < Ku; i++) {
				for (int j = 0; j < Kv; j++) {
					numerator += digamma(Nijl[i][j][l] + bl) - digamma(bl);
					denominator += digamma(Nij.get(i, j) + sumBl) - digamma(sumBl);
				}
			}
			blVec.set(l, bl * (numerator / denominator));
		}

	}

	@Override
	protected void readoutParams() {
		double sumAu = auVec.sum(), sumAv = avVec.sum(), sumBl = blVec.sum();
		
		for (int u = 0; u < numUsers; u++) {
			for (int i = 0; i < Ku; i++) {
				PIuSum.add(u, i, (Nui.get(u, i) + au) / (Nu.get(u) + sumAu));
			}
		}

		for (int v = 0; v < numItems; v++) {
			for (int j = 0; j < Kv; j++) {
				PIvSum.add(v, j, (Nvj.get(v, j) + av) / (Nv.get(v) + sumAv));
			}
		}

		for (int i = 0; i < Ku; i++) {
			for (int j = 0; j < Kv; j++) {
				for (int l = 0; l < numLevels; l++) {
					thetaSum[i][j][l] += (Nijl[i][j][l] + bl) / (Nij.get(i, j) + sumBl);
				}
			}
		}

		numStats++;
	}

	@Override
	protected void postProbDistr() {
		PIu = PIuSum.scale(1.0 / numStats);
		PIv = PIvSum.scale(1.0 / numStats);

		for (int i = 0; i < Ku; i++) {
			for (int j = 0; j < Kv; j++) {
				for (int l = 0; l < numLevels; l++) {
					theta[i][j][l] = thetaSum[i][j][l] / numStats;
				}
			}
		}
	}

	@Override
	protected boolean isConverged(int iter) throws Exception {

		// get the parameters
		postProbDistr();

		// compute the perplexity
		int N = 0;
		double sum = 0;
		for (MatrixEntry me : trainMatrix) {
			int u = me.row();
			int v = me.column();
			double ruv = me.get();

			sum += perplexity(u, v, ruv);
			N++;
		}

		double perp = Math.exp(sum / N);
		double delta = perp - loss; // perplexity should get smaller and smaller --> delta<0

		Logs.debug("{}{} iter {} achieves perplexity = {}, delta_perp = {}", algoName, foldInfo, iter, perp, delta);

		if (numStats > 1 && delta > 0)
			return true;

		loss = perp;
		return false;
	}

	@Override
	protected double perplexity(int u, int v, double pred) throws Exception {
		int l = (int) (pred / minRate - 1);

		// compute p(r|u,v)
		double prob = 0;
		for (int i = 0; i < Ku; i++) {
			for (int j = 0; j < Kv; j++) {
				prob += theta[i][j][l] * PIu.get(u, i) * PIv.get(v, j);
			}
		}
		return -Math.log(prob);
	}

	@Override
	protected double predict(int u, int v) throws Exception {
		double pred = 0;

		for (int l = 0; l < numLevels; l++) {
			double rate = scales.get(l);

			double prob = 0; // P(r|u,v)=\sum_{i,j} P(r|i,j)P(i|u)P(j|v)
			for (int i = 0; i < Ku; i++) {
				for (int j = 0; j < Kv; j++) {
					prob += theta[i][j][l] * PIu.get(u, i) * PIv.get(v, j);
				}
			}

			pred += rate * prob;
		}

		return pred;
	}

	@Override
	public String toString() {
		return Strings.toString(new Object[] { Ku, Kv, au, av, bl, numIters, burnIn, sampleLag }, ", ");
	}

}
