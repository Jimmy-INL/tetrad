///////////////////////////////////////////////////////////////////////////////
// For information as to what this class does, see the Javadoc, below.       //
// Copyright (C) 1998, 1999, 2000, 2001, 2002, 2003, 2004, 2005, 2006,       //
// 2007, 2008, 2009, 2010, 2014, 2015, 2022 by Peter Spirtes, Richard        //
// Scheines, Joseph Ramsey, and Clark Glymour.                               //
//                                                                           //
// This program is free software; you can redistribute it and/or modify      //
// it under the terms of the GNU General Public License as published by      //
// the Free Software Foundation; either version 2 of the License, or         //
// (at your option) any later version.                                       //
//                                                                           //
// This program is distributed in the hope that it will be useful,           //
// but WITHOUT ANY WARRANTY; without even the implied warranty of            //
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the             //
// GNU General Public License for more details.                              //
//                                                                           //
// You should have received a copy of the GNU General Public License         //
// along with this program; if not, write to the Free Software               //
// Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA //
///////////////////////////////////////////////////////////////////////////////

package edu.cmu.tetrad.search;

import edu.cmu.tetrad.data.AndersonDarlingTest;
import edu.cmu.tetrad.data.DataSet;
import edu.cmu.tetrad.data.DataUtils;
import edu.cmu.tetrad.graph.EdgeListGraph;
import edu.cmu.tetrad.graph.Graph;
import edu.cmu.tetrad.graph.Node;
import edu.cmu.tetrad.search.utils.HungarianAlgorithm;
import edu.cmu.tetrad.search.utils.NRooks;
import edu.cmu.tetrad.search.utils.PermutationMatrixPair;
import edu.cmu.tetrad.util.Matrix;
import edu.cmu.tetrad.util.TetradLogger;
import org.apache.commons.math3.linear.BlockRealMatrix;
import org.apache.commons.math3.linear.EigenDecomposition;
import org.jetbrains.annotations.NotNull;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;

import static org.apache.commons.math3.util.FastMath.*;

/**
 * <p>Implements the LiNG-D algorithm as well as a number of ancillary
 * methods for LiNG-D and LiNGAM. The reference is here:</p>
 *
 * <p>Lacerda, G., Spirtes, P. L., Ramsey, J., &amp; Hoyer, P. O. (2012). Discovering
 * cyclic causal models by independent components analysis. arXiv preprint
 * arXiv:1206.3273.</p>
 *
 * <p>The focus for this implementation was making super-simple code, not so much
 * because the method was trivial (it's not) but out of an attempt to compartmentalize.
 * Bootstrapping and other forms of improving the estimate of BHat were not addressed,
 * and no attempt was made here to ensure that LiNGAM outputs a DAG. For high sample sizes
 * for an acyclic model it does tend to. No attempt was made to implement DirectLiNGAM
 * since it was tangential to the effort to get LiNG-D to work. Also, only a passing effort
 * to get either of these algorithms to handle real data. There are two tuning parameters--a
 * threshold for finding a strong diagonal and a threshold on the B matrix for finding edges
 * in the final graph; these are finicky. So there's more work to do, and the implementation may
 * improve in the future.</p>
 *
 * <p>Both N Rooks and Hungarian Algorithm were tested for finding the best strong diagonal;
 * these were not compared head to head, though the initial impression was that N Rooks was better,
 * so this version uses it.</p>
 *
 * <p>This implementation has two parameters, a threshold (for N Rooks) on the minimum values
 * in absolute value for including entries in a possible strong diagonal for W, and a threshold
 * for BHat for including edges in the final graph.</p>
 *
 * <p>This class is not configured to respect knowledge of forbidden and required
 * edges.</p>
 *
 * @author peterspirtes
 * @author gustavolacerda
 * @author patrickhoyer
 * @author josephramsey
 */
public class LingD {
    private double spineThreshold = 0.5;
    private double bThreshold = 0.1;

    /**
     * Constructor. The W matrix needs to be estimated separately (e.g., using
     * the Lingam.estimateW(.) method using the ICA method in Tetrad, or some
     * method in Python or R) and passed into the search(W) method.
     */
    public LingD() {
    }

    /**
     * Fits a LiNG-D model to the given dataset using a default method for estimting
     * W.
     * @param D A continuous dataset.
     * @return The BHat matrix, where B[i][j] gives the coefficient of j->i if nonzero.
     */
    public List<Matrix> fit(DataSet D) {
        Matrix W = LingD.estimateW(D, 5000, 1e-6, 1.2);
        return fitW(W);
    }

    /**
     * Performs the LiNG-D algorithm given a W matrix, which needs to be discovered
     * elsewhere. The local algorithm is assumed--in fact, the W matrix is simply
     * thresholded without bootstrapping.
     *
     * @param W The W matrix to be used.
     * @return A list of estimated B Hat matrices generated by LiNG-D.
     */
    public List<Matrix> fitW(Matrix W) {
        List<PermutationMatrixPair> pairs = nRooks(W.transpose(), spineThreshold);

        if (pairs.isEmpty()) {
            throw new IllegalArgumentException("Could not find an N Rooks solution with that threshold.");
        }

        List<Matrix> results = new ArrayList<>();

        for (PermutationMatrixPair pair : pairs) {
            Matrix bHat = LingD.getScaledBHat(pair, bThreshold);
            results.add(bHat);
        }

        return results;
    }

    /**
     * Sets the threshold used to prune the B matrix for the local algorithms.
     *
     * @param bThreshold The threshold, a non-negative number.
     */
    public void setBThreshold(double bThreshold) {
        if (bThreshold < 0) throw new IllegalArgumentException("Expecting a non-negative number: " + bThreshold);
        this.bThreshold = bThreshold;
    }

    /**
     * Sets the threshold used to prune the matrix for purpose of searching for alterantive strong dia=gonals..
     *
     * @param spineThreshold The threshold, a non-negative number.
     */
    public void setSpineThreshold(double spineThreshold) {
        if (spineThreshold < 0)
            throw new IllegalArgumentException("Expecting a non-negative number: " + spineThreshold);
        this.spineThreshold = spineThreshold;
    }

    /**
     * Estimates the W matrix using FastICA. Assumes the "parallel" option, using
     * the "exp" function.
     *
     * @param data             The dataset to estimate W for.
     * @param fastIcaMaxIter   Maximum number of iterations of ICA.
     * @param fastIcaTolerance Tolerance for ICA.
     * @param fastIcaA         Alpha for ICA.
     * @return The estimated W matrix.
     */
    public static Matrix estimateW(DataSet data, int fastIcaMaxIter, double fastIcaTolerance,
                                   double fastIcaA) {
        data = data.copy();

        double[][] _data = data.getDoubleData().transpose().toArray();
        TetradLogger.getInstance().forceLogMessage("Anderson Darling P-values Per Variables (p < alpha means Non-Guassian)");
        TetradLogger.getInstance().forceLogMessage("");

        for (int i = 0; i < _data.length; i++) {
            Node node = data.getVariable(i);
            AndersonDarlingTest test = new AndersonDarlingTest(_data[i]);
            double p = test.getP();
            NumberFormat nf = new DecimalFormat("0.000");
            TetradLogger.getInstance().forceLogMessage(node.getName() + ": p = " + nf.format(p));
        }

        TetradLogger.getInstance().forceLogMessage("");

        Matrix X = data.getDoubleData();
        X = DataUtils.centerData(X).transpose();
        FastIca fastIca = new FastIca(X, X.rows());
        fastIca.setVerbose(false);
        fastIca.setMaxIterations(fastIcaMaxIter);
        fastIca.setAlgorithmType(FastIca.PARALLEL);
        fastIca.setTolerance(fastIcaTolerance);
        fastIca.setFunction(FastIca.LOGCOSH);
        fastIca.setRowNorm(false);
        fastIca.setAlpha(fastIcaA);
        FastIca.IcaResult result11 = fastIca.findComponents();
        return result11.getW();
    }

    /**
     * Returns a graph given a coefficient matrix and a list of variables. It is
     * assumed that any non-zero entry in B corresponds to a directed edges, so
     * that Bij != 0 implies that j->i in the graph.
     *
     * @param B         The coefficient matrix.
     * @param variables The list of variables.
     * @return The built graph.
     */
    @NotNull
    public static Graph makeGraph(Matrix B, List<Node> variables) {
        Graph g = new EdgeListGraph(variables);

        for (int j = 0; j < B.columns(); j++) {
            for (int i = 0; i < B.rows(); i++) {
                if (B.get(i, j) != 0) {
                    g.addDirectedEdge(variables.get(j), variables.get(i));
                }
            }
        }

        return g;
    }

    /**
     * Finds a column permutation of the W matrix that maximizes the sum
     * of 1 / |Wii| for diagonal elements Wii in W. This will be speeded up
     * if W is a thresholded matrix.
     *
     * @param W             The W matrix, WX = e.
     * @param spineThrehold The threshold used in NRooks to search for alternative strong diagonals.
     * @return The model with the strongest diagonal, as a permutation matrix pair.
     * @see PermutationMatrixPair
     */
    public static PermutationMatrixPair strongestDiagonal(Matrix W, double spineThrehold) {
        List<PermutationMatrixPair> pairs = nRooks(W.transpose(), spineThrehold);

        if (pairs.isEmpty()) {
            throw new IllegalArgumentException("Could not find an N Rooks solution with that threshold.");
        }

        PermutationMatrixPair bestPair = null;
        double sum1 = Double.NEGATIVE_INFINITY;

        for (PermutationMatrixPair pair : pairs) {
            Matrix permutedMatrix = pair.getPermutedMatrix();

            double sum = 0.0;
            for (int j = 0; j < permutedMatrix.rows(); j++) {
                double a = permutedMatrix.get(j, j);
                sum += abs(a);
            }

            if (sum > sum1) {
                sum1 = sum;
                bestPair = pair;
            }
        }

        if (bestPair == null) {
            throw new IllegalArgumentException("Could not find a best N Rooks solution with that threshold.");
        }

        return bestPair;
    }

    /**
     * Whether the BHat matrix represents a stable model. The eigenvalues are
     * checked ot make sure they are all less than 1 in modulus.
     *
     * @param bHat The bHat matrix.
     * @return True iff the model is stable.
     */
    public static boolean isStable(Matrix bHat) {
        EigenDecomposition eigen = new EigenDecomposition(new BlockRealMatrix(bHat.toArray()));
        double[] realEigenvalues = eigen.getRealEigenvalues();
        double[] imagEigenvalues = eigen.getImagEigenvalues();

        for (int i = 0; i < realEigenvalues.length; i++) {
            double realEigenvalue = realEigenvalues[i];
            double imagEigenvalue = imagEigenvalues[i];
            double modulus = sqrt(pow(realEigenvalue, 2) + pow(imagEigenvalue, 2));

            System.out.println("Modulus for eigenvalue " + (i + 1) + " = " + modulus);

            if (modulus >= 1.0) {
                return false;
            }
        }

        return true;
    }

    /**
     * Scares the given matrix M by diving each entry (i, j) by M(j, j)
     *
     * @param M The matrix to scale.
     * @return The scaled matrix.
     */
    public static Matrix scale(Matrix M) {
        Matrix _M = M.like();

        for (int i = 0; i < _M.rows(); i++) {
            for (int j = 0; j < _M.columns(); j++) {
                _M.set(i, j, M.get(i, j) / M.get(j, j));
            }
        }

        return _M;
    }

    /**
     * Thresholds the givem matrix, sending any small entries to zero.
     *
     * @param M         The matrix to threshold.
     * @param threshold The value such that M(i, j) is set to zero if |M(i, j)| < threshold.
     *                  Should be non-negative.
     * @return The thresholded matrix.
     */
    public static Matrix threshold(Matrix M, double threshold) {
        if (threshold < 0) throw new IllegalArgumentException("Expecting a non-negative number: " + threshold);

        Matrix _M = M.copy();

        for (int i = 0; i < M.rows(); i++) {
            for (int j = 0; j < M.columns(); j++) {
                if (abs(M.get(i, j)) < threshold) _M.set(i, j, 0.0);
            }
        }

        return _M;
    }

    /**
     * Returns the BHat matrix, permuted to causal order (lower triangle) and
     * scaled so that the diagonal consists only of 1's.
     *
     * @param pair       The (column permutation, thresholded, column permuted W matrix)
     *                   pair.
     * @param bThreshold Valued in the BHat matrix less than this in absolute
     *                   value are set to 0.
     * @return The estimated B Hat matrix for this pair.
     * @see PermutationMatrixPair
     */
    public static Matrix getScaledBHat(PermutationMatrixPair pair, double bThreshold) {
        Matrix WTilde = pair.getPermutedMatrix().transpose();
        WTilde = LingD.scale(WTilde);
        Matrix BHat = Matrix.identity(WTilde.columns()).minus(WTilde);
        BHat = threshold(BHat, bThreshold);
        int[] perm = pair.getRowPerm();
        int[] inverse = LingD.inversePermutation(perm);
        PermutationMatrixPair inversePair = new PermutationMatrixPair(BHat, inverse, inverse);
        return inversePair.getPermutedMatrix();
    }

    @NotNull
    public static List<PermutationMatrixPair> nRooks(Matrix W, double spineThreshold) {
        return pairsNRook(W, spineThreshold);
//        return pairsHungarian(W);
    }

    @NotNull
    private static List<PermutationMatrixPair> pairsHungarian(Matrix W) {
        double[][] costMatrix = new double[W.rows()][W.columns()];

        for (int i = 0; i < W.rows(); i++) {
            for (int j = 0; j < W.columns(); j++) {
                if (W.get(i, j) != 0) {
                    costMatrix[i][j] = 1.0 / abs(W.get(i, j));
                } else {
                    costMatrix[i][j] = 1000.0;
                }
            }
        }

        HungarianAlgorithm alg = new HungarianAlgorithm(costMatrix);
        int[][] assignment = alg.findOptimalAssignment();

        List<PermutationMatrixPair> pairs = new ArrayList<>();

        int[] perm = new int[assignment.length];
        for (int i = 0; i < perm.length; i++) perm[i] = assignment[i][1];

        PermutationMatrixPair pair = new PermutationMatrixPair(W, perm, null);
        pairs.add(pair);
        return pairs;
    }

    @NotNull
    private static List<PermutationMatrixPair> pairsNRook(Matrix W, double spineThreshold) {
        boolean[][] allowablePositions = new boolean[W.rows()][W.columns()];

        for (int i = 0; i < W.rows(); i++) {
            for (int j = 0; j < W.columns(); j++) {
                allowablePositions[i][j] = abs(W.get(i, j)) > spineThreshold;
            }
        }

        List<PermutationMatrixPair> pairs = new ArrayList<>();
        List<int[]> colPermutations = NRooks.nRooks(allowablePositions);

        for (int[] colPermutation : colPermutations) {
            pairs.add(new PermutationMatrixPair(W, null, colPermutation));
        }

        return pairs;
    }

    static int[] inversePermutation(int[] perm) {
        int[] inverse = new int[perm.length];

        for (int i = 0; i < perm.length; i++) {
            inverse[perm[i]] = i;
        }

        return inverse;
    }
}


