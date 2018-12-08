/**
 * Copyright (C) 2016 LibRec
 * <p>
 * This file is part of LibRec.
 * LibRec is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * <p>
 * LibRec is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * <p>
 * You should have received a copy of the GNU General Public License
 * along with LibRec. If not, see <http://www.gnu.org/licenses/>.
 */
package net.librec.recommender.cf.ranking;

import net.librec.common.LibrecException;
import net.librec.math.algorithm.Gamma;
import net.librec.math.algorithm.Randoms;
import net.librec.math.structure.DenseMatrix;
import net.librec.math.structure.DenseVector;
import net.librec.math.structure.MatrixEntry;
import net.librec.math.structure.VectorBasedDenseVector;
import net.librec.recommender.MatrixFactorizationRecommender;
import org.apache.commons.math3.distribution.GammaDistribution;

/**
 * Prem Gopalan, et al. <strong>Scalable Recommendation with Hierarchical Poisson Factorization</strong>, UAI 2015.
 *
 * @author Haidong Zhang and Yatong Sun
 */

public class BPoissMFRecommender extends MatrixFactorizationRecommender {

    private double a;
    private double aPrime;
    private double bPrime;
    private double c;
    private double cPrime;
    private double dPrime;

    @Override
    protected void setup() throws LibrecException {
        super.setup();

        // get hyper-parameters
        a = conf.getDouble("rec.a", 0.3);
        aPrime = conf.getDouble("rec.a.prime", 0.3);
        bPrime = conf.getDouble("rec.b.prime", 1.0);
        c = conf.getDouble("rec.c", 0.3);
        cPrime = conf.getDouble("rec.c.prime", 0.3);
        dPrime = conf.getDouble("rec.d.prime", 1.0);

        // init user factors
        GammaDistribution userGammaDis = new GammaDistribution(a, 1/bPrime);
        for (int u=0; u<numUsers; u++) {
            for (int k=0; k<numFactors; k++)
                userFactors.set(u, k, userGammaDis.sample());
        }

        // init item factors
        GammaDistribution itemGammaDis = new GammaDistribution(c, 1/dPrime);
        for (int i=0; i<numItems; i++) {
            for (int k=0; k<numFactors; k++)
                itemFactors.set(i, k, itemGammaDis.sample());
        }

    }

    @Override
    protected void trainModel() throws LibrecException {
        // cdef float k_shp = a_prime + k*a
        double kShp = aPrime + numFactors * a;
        // cdef float t_shp = c_prime + k*c
        double tShp = cPrime + numFactors * c;
        // cdef np.ndarray[float, ndim=2] k_rte = b_prime + Theta.sum(axis=1, keepdims=True)
        DenseVector kRte = new VectorBasedDenseVector(numUsers);
        for (int u=0; u<numUsers; u++) {
            kRte.set(u, userFactors.row(u).sum() + bPrime);
        }
        // cdef np.ndarray[float, ndim=2] t_rte = d_prime + Beta.sum(axis=1, keepdims=True)
        DenseVector tRte = new VectorBasedDenseVector(numItems);
        for (int i=0; i<numItems; i++) {
            tRte.set(i, itemFactors.row(i).sum() + dPrime);
        }
        // cdef np.ndarray[float, ndim=2] Gamma_rte = np.random.gamma(a_prime, b_prime/a_prime, size=(nU, 1)).astype('float32') + \
        // Beta.sum(axis=0, keepdims=True)
        GammaDistribution gammaPre = new GammaDistribution(aPrime, bPrime/aPrime);
        DenseMatrix gammaRte = new DenseMatrix(numUsers, numFactors);
        for (int u=0; u<numUsers; u++) {
            double userSample = gammaPre.sample();
            for (int k=0; k<numFactors; k++) {
                gammaRte.set(u, k, userSample + itemFactors.column(k).sum());
            }
        }
        // cdef np.ndarray[float, ndim=2] Lambda_rte = np.random.gamma(c_prime, d_prime/c_prime, size=(nI, 1)).astype('float32') + \
        // Theta.sum(axis=0, keepdims=True)
        GammaDistribution lambdaPre = new GammaDistribution(cPrime, dPrime/cPrime);
        DenseMatrix lambdaRte = new DenseMatrix(numItems, numFactors);
        for (int i=0; i<numItems; i++) {
            double itemSample = lambdaPre.sample();
            for (int k=0; k<numFactors; k++) {
                lambdaRte.set(i, k, itemSample + userFactors.column(k).sum());
            }
        }

        // cdef np.ndarray[float, ndim=2] Gamma_shp = Gamma_rte * Theta * np.random.uniform(low=.85, high=1.15, size=(nU, k)).astype('float32')
        DenseMatrix gammaShp = new DenseMatrix(numUsers, numFactors);
        for (int u=0; u<numUsers; u++) {
            for (int k=0; k<numFactors; k++) {
                gammaShp.set(u, k, gammaRte.get(u, k) * userFactors.get(u, k) * Randoms.uniform(0.85, 1.15));
            }
        }
        // cdef np.ndarray[float, ndim=2] Lambda_shp = Lambda_rte * Beta * np.random.uniform(low=.85, high=1.15, size=(nI, k)).astype('float32')
        DenseMatrix lambdaShp = new DenseMatrix(numItems, numFactors);
        for (int i=0; i<numItems; i++) {
            for (int k=0; k<numFactors; k++) {
                lambdaShp.set(i, k, lambdaRte.get(i, k) * itemFactors.get(i, k) * Randoms.uniform(0.85, 1.15));
            }
        }

        // def np.ndarray[float, ndim=2] phi = np.empty((nY, k), dtype='float32')
        DenseMatrix phi = new DenseMatrix(numRates, numFactors);

        // cdef float add_k_rte = a_prime/b_prime
        double addKRate = aPrime / bPrime;
        // cdef float add_t_rte = c_prime/d_prime
        double addTRate = cPrime / dPrime;

        // Main loop
        for (int iter = 1; iter <= numIterations; iter++) {
            // update_phi(&Gamma_shp[0,0], &Gamma_rte[0,0], &Lambda_shp[0,0], &Lambda_rte[0,0], &phi[0,0], &Y[0], k, &ix_u[0], &ix_i[0], nY, nthreads)
            updatePhi(gammaShp, gammaRte, lambdaShp, lambdaRte, phi);

            // Gamma_rte = k_shp/k_rte + Beta.sum(axis=0, keepdims=True)
            for (int u=0; u<numUsers; u++) {
                double rowNumner = kShp / kRte.get(u);
                for (int k=0; k<numFactors; k++) {
                    double colNumner = itemFactors.column(k).sum();
                    gammaRte.set(u, k, rowNumner + colNumner);
                }
            }

            //don't put this part before the update for Gamma rate
            // Gamma_shp = np.zeros((Gamma_shp.shape[0], Gamma_shp.shape[1]), dtype='float32')
            for (int u=0; u<numUsers; u++) {
                for (int k=0; k<numFactors; k++) {
                    gammaShp.set(u, k, 0.0);
                }
            }
            // Lambda_shp = np.zeros((Lambda_shp.shape[0], Lambda_shp.shape[1]), dtype='float32')
            for (int i=0; i<numItems; i++) {
                for (int k=0; k<numFactors; k++) {
                    lambdaShp.set(i, k, 0.0);
                }
            }
            // update_G_n_L_sh(&Gamma_shp[0,0], &Lambda_shp[0,0], &phi[0,0], k, &ix_u[0], &ix_i[0], nY)
            // Gamma_shp += a
            // Lambda_shp += c
            update_G_n_L_sh(gammaShp, lambdaShp, phi, a, c);

            // Theta[:,:] = Gamma_shp/Gamma_rte
            for (int u=0; u<numUsers; u++) {
                for (int k=0; k<numFactors; k++) {
                    userFactors.set(u, k, gammaShp.get(u, k) / gammaRte.get(u, k));
                }
            }
            // Lambda_rte = t_shp/t_rte + Theta.sum(axis=0, keepdims=True)
            for (int i=0; i<numItems; i++) {
                double rowNumner = tShp / tRte.get(i);
                for (int k=0; k<numFactors; k++) {
                    double colNumner = userFactors.column(k).sum();
                    lambdaRte.set(i, k, rowNumner + colNumner);
                }
            }
            // Beta[:,:] = Lambda_shp/Lambda_rte
            for (int i=0; i<numItems; i++) {
                for (int k=0; k<numFactors; k++) {
                    itemFactors.set(i, k, lambdaShp.get(i, k) / lambdaRte.get(i, k));
                }
            }
            // k_rte = add_k_rte + Theta.sum(axis=1, keepdims=True)
            for (int u=0; u<numUsers; u++) {
                double newValue = addKRate + userFactors.row(u).sum();
                kRte.set(u, newValue);
            }
            // t_rte = add_t_rte + Beta.sum(axis=1, keepdims=True)
            for (int i=0; i<numItems; i++) {
                double newValue = addTRate + itemFactors.row(i).sum();
                tRte.set(i, newValue);
            }

        }
    }

    protected void updatePhi(DenseMatrix gammaShp, DenseMatrix gammaRte, DenseMatrix lambdaShp, DenseMatrix lambdaRte, DenseMatrix phi) {
        //    cdef int uid, iid
        //    cdef int uid_st, iid_st, phi_st
        //    cdef float sumphi
        //    cdef int i, j
        //	 for i in prange(nY, schedule='static', num_threads=nthreads):
        //      uid = ix_u[i]
        //      iid = ix_i[i]
        //      sumphi = 0
        //      uid_st = k*uid
        //      iid_st = k*iid
        //      phi_st = i*k
        //      for j in range(k):
        //          phi[phi_st + j] = exp(  psi(G_sh[uid_st + j]) - log(G_rt[uid_st + j]) +psi(L_sh[iid_st + j]) - log(L_rt[iid_st + j])  )
        //          sumphi += phi[phi_st + j]
        //      for j in range(k):
        //          phi[phi_st + j] *= Y[i]/sumphi
        int ratingCount = 0;
        for (MatrixEntry me : trainMatrix) {
            int userIdx = me.row();
            int itemIdx = me.column();
            double rating = me.get();
            double sumphi = 0;

            for (int k=0; k<numFactors; k++) {
                double newPhiValue = Math.exp(Gamma.digamma(gammaShp.get(userIdx, k)) - Math.log(gammaRte.get(userIdx, k)) + Gamma.digamma(lambdaShp.get(itemIdx, k)) - Math.log(lambdaRte.get(itemIdx, k)));
                phi.set(ratingCount, k, newPhiValue);
                sumphi += newPhiValue;
            }

            for (int k=0; k<numFactors; k++) {
                double newPhiValue = phi.get(ratingCount, k) * rating / sumphi ;
                phi.set(ratingCount, k, newPhiValue);
            }

            ratingCount++;
        }

    }

    protected void update_G_n_L_sh(DenseMatrix gammaShp, DenseMatrix lambdaShp, DenseMatrix phi, double a, double c) {
        //    cdef int i, j
        //	 for i in range(nY):
        //      for j in range(k):
        //          G_sh[ix_u[i]*k + j] += phi[i*k + j]
        //          L_sh[ix_i[i]*k + j] += phi[i*k + j]
        int ratingCount = 0;
        for (MatrixEntry me : trainMatrix) {
            int userIdx = me.row();
            int itemIdx = me.column();
            for (int k=0; k<numFactors; k++) {
                double phiValue = phi.get(ratingCount, k);
                double newGammaValue = gammaShp.get(userIdx, k) + phiValue + a;
                gammaShp.set(userIdx, k, newGammaValue);

                double newLambdaValue = lambdaShp.get(itemIdx, k) + phiValue + c;
                lambdaShp.set(itemIdx, k, newLambdaValue);
            }
            ratingCount++;
        }
    }

}
