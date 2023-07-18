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

package edu.cmu.tetrad.search.score;

import edu.cmu.tetrad.data.DataModel;
import edu.cmu.tetrad.data.DataSet;
import edu.cmu.tetrad.data.ICovarianceMatrix;
import edu.cmu.tetrad.data.SimpleDataLoader;
import edu.cmu.tetrad.graph.Node;
import edu.cmu.tetrad.search.utils.LogUtilsSearch;
import edu.cmu.tetrad.util.Matrix;
import edu.cmu.tetrad.util.StatUtils;
import edu.cmu.tetrad.util.TetradLogger;
import org.apache.commons.math3.linear.SingularMatrixException;
import org.apache.commons.math3.util.FastMath;
import org.jetbrains.annotations.NotNull;

import java.util.*;

import static edu.cmu.tetrad.util.MatrixUtils.convertCovToCorr;
import static java.lang.Double.NaN;
import static org.apache.commons.math3.util.FastMath.abs;
import static org.apache.commons.math3.util.FastMath.log;

/**
 * <p>Implements the linear, Gaussian BIC score, with a 'penalty discount' multiplier
 * on the BIC penalty. The formula used for the score is BIC = 2L - ck ln n, where c is the penalty discount and L is
 * the linear, Gaussian log likelihood--that is, the sum of the log likelihoods of the individual records, which are
 * assumed to be i.i.d.</p>
 *
 * <p>For FGES, Chickering uses the standard linear, Gaussian BIC score, so we will
 * for lack of a better reference give his paper:</p>
 *
 * <p>Chickering (2002) "Optimal structure identification with greedy search"
 * Journal of Machine Learning Research.</p>
 *
 * <p>The version of the score due to Nandy et al. is given in this reference:</p>
 *
 * <p>Nandy, P., Hauser, A., & Maathuis, M. H. (2018). High-dimensional consistency
 * in score-based and hybrid structure learning. The Annals of Statistics, 46(6A), 3151-3183.</p>
 *
 * <p>This score may be used anywhere though where a linear, Gaussian score is needed.
 * Anectodally, the score is fairly robust to non-Gaussianity, though with some additional unfaithfulness over and above
 * waht the score would give for Guassian data, a detriment that can be overcome to an extent by use a permutation
 * algorithm such as SP, GRaSP, or BOSS</p>
 *
 * <p>As for all scores in Tetrad, higher scores mean more dependence, and negative
 * scores indicate independence.</p>
 *
 * @author josephramsey
 * @see edu.cmu.tetrad.search.Fges
 * @see edu.cmu.tetrad.search.Sp
 * @see edu.cmu.tetrad.search.Grasp
 * @see edu.cmu.tetrad.search.Boss
 */
public class SemBicScore implements Score {

    // The sample size of the covariance matrix.
    private final int sampleSize;
    // A  map from variable names to their indices.
    private final Map<Node, Integer> indexMap;
    private final double logN;
    private boolean calculateRowSubsets;
    // The dataset.
    private DataModel dataModel;
    // .. as matrix
    private Matrix data;
    // The correlation matrix.
    private ICovarianceMatrix covariances;
    // The variables of the covariance matrix.
    private List<Node> variables;
    // True if verbose output should be sent to out.
    private boolean verbose;
    // The penalty penaltyDiscount, 1 for standard BIC.
    private double penaltyDiscount = 1.0;

    // The structure prior, 0 for standard BIC.
    private double structurePrior;

    // Equivalent sample size
    private Matrix matrix;

    // The rule type to use.
    private RuleType ruleType = RuleType.CHICKERING;

    /**
     * Constructs the score using a covariance matrix.
     */
    public SemBicScore(ICovarianceMatrix covariances) {
        if (covariances == null) {
            throw new NullPointerException();
        }

        setCovariances(covariances);
        this.variables = covariances.getVariables();
        this.sampleSize = covariances.getSampleSize();
        this.indexMap = indexMap(this.variables);
        this.logN = log(sampleSize);
    }

    /**
     * Constructs the score using a covariance matrix.
     */
    public SemBicScore(DataSet dataSet, boolean precomputeCovariances) {

        if (dataSet == null) {
            throw new NullPointerException();
        }

        this.dataModel = dataSet;
        this.data = dataSet.getDoubleData();

        if (!dataSet.existsMissingValue()) {
            setCovariances(getCovarianceMatrix(dataSet, precomputeCovariances));

            this.variables = this.covariances.getVariables();
            this.sampleSize = this.covariances.getSampleSize();
            this.indexMap = indexMap(this.variables);
            this.calculateRowSubsets = false;
            this.logN = log(sampleSize);
            return;
        }

        this.variables = dataSet.getVariables();
        this.sampleSize = dataSet.getNumRows();
        this.indexMap = indexMap(this.variables);
        this.calculateRowSubsets = true;
        this.logN = log(sampleSize);
    }

    public static double getVarRy(int i, int[] parents, Matrix data, ICovarianceMatrix covariances, boolean calculateRowSubsets)
            throws SingularMatrixException {
        int[] all = SemBicScore.concat(i, parents);
        Matrix cov = SemBicScore.getCov(SemBicScore.getRows(i, parents, data, calculateRowSubsets), all, all, data, covariances);
        int[] pp = SemBicScore.indexedParents(parents);
        Matrix covxx = cov.getSelection(pp, pp);
        Matrix covxy = cov.getSelection(pp, new int[]{0});
        Matrix b = (covxx.inverse().times(covxy));
        Matrix bStar = bStar(b);
        return (bStar.transpose().times(cov).times(bStar).get(0, 0));
    }

    @NotNull
    public static Matrix bStar(Matrix b) {
        Matrix byx = new Matrix(b.getNumRows() + 1, 1);
        byx.set(0, 0, 1);
        for (int j = 0; j < b.getNumRows(); j++) byx.set(j + 1, 0, -b.get(j, 0));
        return byx;
    }

    private static int[] indexedParents(int[] parents) {
        int[] pp = new int[parents.length];
        for (int j = 0; j < pp.length; j++) pp[j] = j + 1;
        return pp;
    }

    private static int[] concat(int i, int[] parents) {
        int[] all = new int[parents.length + 1];
        all[0] = i;
        System.arraycopy(parents, 0, all, 1, parents.length);
        return all;
    }

    private static Matrix getCov(List<Integer> rows, int[] _rows, int[] cols, Matrix data, ICovarianceMatrix covarianceMatrix) {
        if (rows == null) {
            return covarianceMatrix.getSelection(_rows, cols);
        }

        Matrix cov = new Matrix(_rows.length, cols.length);

        for (int i = 0; i < _rows.length; i++) {
            for (int j = 0; j < cols.length; j++) {
                double mui = 0.0;
                double muj = 0.0;

                for (int k : rows) {
                    mui += data.get(k, _rows[i]);
                    muj += data.get(k, cols[j]);
                }

                mui /= rows.size() - 1;
                muj /= rows.size() - 1;

                double _cov = 0.0;

                for (int k : rows) {
                    _cov += (data.get(k, _rows[i]) - mui) * (data.get(k, cols[j]) - muj);
                }

                double mean = _cov / (rows.size());
                cov.set(i, j, mean);
            }
        }

        return cov;
    }

    private static List<Integer> getRows(int i, int[] parents, Matrix data, boolean calculateRowSubsets) {
        if (!calculateRowSubsets) {
            return null;
        }

        List<Integer> rows = new ArrayList<>();

        K:
        for (int k = 0; k < data.getNumRows(); k++) {
            if (Double.isNaN(data.get(k, i))) continue;

            for (int p : parents) {
                if (Double.isNaN(data.get(k, p))) continue K;
            }

            rows.add(k);
        }

        return rows;
    }

    @NotNull
    private ICovarianceMatrix getCovarianceMatrix(DataSet dataSet, boolean precomputeCovariances) {
        return SimpleDataLoader.getCovarianceMatrix(dataSet, precomputeCovariances);
    }

    @Override
    public double localScoreDiff(int x, int y, int[] z) {
        if (this.ruleType == RuleType.NANDY) {
            return nandyBic(x, y, z);
        } else {
            return localScore(y, append(z, x)) - localScore(y, z);
        }
    }

    public double nandyBic(int x, int y, int[] z) {
        double sp1 = getStructurePrior(z.length + 1);
        double sp2 = getStructurePrior(z.length);

        Node _x = this.variables.get(x);
        Node _y = this.variables.get(y);
        List<Node> _z = getVariableList(z);

        List<Integer> rows = getRows(x, z);

        if (rows != null) {
            rows.retainAll(Objects.requireNonNull(getRows(y, z)));
        }

        double r = partialCorrelation(_x, _y, _z, rows);

        double c = getPenaltyDiscount();

        return -this.sampleSize * log(1.0 - r * r) - c * log(this.sampleSize)
                - 2.0 * (sp1 - sp2);
    }

    /**
     * @param i       The index of the node.
     * @param parents The indices of the node's parents.
     * @return The score, or NaN if the score cannot be calculated.
     */
    public double localScore(int i, int... parents) {
        int k = parents.length;
        double lik;

        Arrays.sort(parents);

        try {
            double varey = SemBicScore.getVarRy(i, parents, this.data, this.covariances, this.calculateRowSubsets);
            lik = -(double) (this.sampleSize / 2.0) * log(varey);
        } catch (SingularMatrixException e) {
            throw new RuntimeException("Singularity encountered when scoring " +
                    LogUtilsSearch.getScoreFact(i, parents, variables));
        }


        double c = getPenaltyDiscount();

        if (this.ruleType == RuleType.CHICKERING || this.ruleType == RuleType.NANDY) {

            // Standard BIC, with penalty discount and structure prior.
            double _score = lik - c * (k / 2.0) * logN - getStructurePrior(k);

            if (Double.isNaN(_score) || Double.isInfinite(_score)) {
                return Double.NaN;
            } else {
                return _score;
            }
        } else {
            throw new IllegalStateException("That rule type is not implemented: " + this.ruleType);
        }
    }


//    private final Map<List<Integer>, Double> cache = new ConcurrentHashMap<>();

    /**
     * Specialized scoring method for a single parent. Used to speed up the effect edges search.
     */


    public double getPenaltyDiscount() {
        return this.penaltyDiscount;
    }

    public void setPenaltyDiscount(double penaltyDiscount) {
        this.penaltyDiscount = penaltyDiscount;
    }

    public double getStructurePrior() {
        return this.structurePrior;
    }

    public void setStructurePrior(double structurePrior) {
        this.structurePrior = structurePrior;
    }

    public ICovarianceMatrix getCovariances() {
        return this.covariances;
    }

    private void setCovariances(ICovarianceMatrix covariances) {
        this.covariances = covariances;
        this.matrix = this.covariances.getMatrix();

        this.dataModel = covariances;

    }

    public int getSampleSize() {
        return this.sampleSize;
    }

    @Override
    public boolean isEffectEdge(double bump) {
        return bump > 0;
    }

    public DataModel getDataModel() {
        return this.dataModel;
    }

    public boolean isVerbose() {
        return this.verbose;
    }

    public void setVerbose(boolean verbose) {
        this.verbose = verbose;
    }

    @Override
    public List<Node> getVariables() {
        return new ArrayList<>(this.variables);
    }

    public void setVariables(List<Node> variables) {
        if (this.covariances != null) {
            this.covariances.setVariables(variables);
        }

        this.variables = variables;
    }

    @Override
    public int getMaxDegree() {
        return (int) FastMath.ceil(log(this.sampleSize));
    }

    @Override
    public boolean determines(List<Node> z, Node y) {
        int i = this.variables.indexOf(y);

        int[] k = new int[z.size()];

        for (int t = 0; t < z.size(); t++) {
            k[t] = this.variables.indexOf(z.get(t));
        }

        try {
            localScore(i, k);
        } catch (RuntimeException e) {
            TetradLogger.getInstance().forceLogMessage(e.getMessage());
            return true;
        }

        return false;
    }

    //    @Override
    public DataModel getData() {
        return this.dataModel;
    }

    private double getStructurePrior(int parents) {
        if (abs(getStructurePrior()) <= 0) {
            return 0;
        } else {
            double p = (getStructurePrior()) / (this.variables.size());
            return -((parents) * log(p) + (this.variables.size() - (parents)) * log(1.0 - p));
        }
    }

    private List<Node> getVariableList(int[] indices) {
        List<Node> variables = new ArrayList<>();
        for (int i : indices) {
            variables.add(this.variables.get(i));
        }
        return variables;
    }

    private Map<Node, Integer> indexMap(List<Node> variables) {
        Map<Node, Integer> indexMap = new HashMap<>();

        for (int i = 0; variables.size() > i; i++) {
            indexMap.put(variables.get(i), i);
        }

        return indexMap;
    }

    private List<Integer> getRows(int i, int[] parents) {
        if (this.dataModel == null) {
            return null;
        }

        List<Integer> rows = new ArrayList<>();

        DataSet dataSet = (DataSet) this.dataModel;

        K:
        for (int k = 0; k < dataSet.getNumRows(); k++) {
            if (Double.isNaN(dataSet.getDouble(k, i))) continue;

            for (int p : parents) {
                if (Double.isNaN(dataSet.getDouble(k, p))) continue K;
            }

            rows.add(k);
        }

        return rows;
    }

    private double partialCorrelation(Node x, Node y, List<Node> z, List<Integer> rows) {
        try {
            return StatUtils.partialCorrelation(convertCovToCorr(getCov(rows, indices(x, y, z))));
        } catch (Exception e) {
            return NaN;
        }
    }

    private int[] indices(Node x, Node y, List<Node> z) {
        int[] indices = new int[z.size() + 2];
        indices[0] = this.indexMap.get(x);
        indices[1] = this.indexMap.get(y);
        for (int i = 0; i < z.size(); i++) indices[i + 2] = this.indexMap.get(z.get(i));
        return indices;
    }

    private Matrix getCov(List<Integer> rows, int[] cols) {
        if (this.dataModel == null) {
            return this.matrix.getSelection(cols, cols);
        }

        DataSet dataSet = (DataSet) this.dataModel;

        Matrix cov = new Matrix(cols.length, cols.length);

        for (int i = 0; i < cols.length; i++) {
            for (int j = i + 1; j < cols.length; j++) {
                double mui = 0.0;
                double muj = 0.0;

                for (int k : rows) {
                    mui += dataSet.getDouble(k, cols[i]);
                    muj += dataSet.getDouble(k, cols[j]);
                }

                mui /= rows.size() - 1;
                muj /= rows.size() - 1;

                double _cov = 0.0;

                for (int k : rows) {
                    _cov += (dataSet.getDouble(k, cols[i]) - mui) * (dataSet.getDouble(k, cols[j]) - muj);
                }

                double mean = _cov / (rows.size());
                cov.set(i, j, mean);
                cov.set(j, i, mean);
            }
        }

        for (int i = 0; i < cols.length; i++) {
            double mui = 0.0;

            for (int k : rows) {
                mui += dataSet.getDouble(k, cols[i]);
            }

            mui /= rows.size();

            double _cov = 0.0;

            for (int k : rows) {
                _cov += (dataSet.getDouble(k, cols[i]) - mui) * (dataSet.getDouble(k, cols[i]) - mui);
            }

            double mean = _cov / (rows.size());
            cov.set(i, i, mean);
        }

        return cov;
    }

    public void setRuleType(RuleType ruleType) {
        this.ruleType = ruleType;
    }

    public SemBicScore subset(List<Node> pi2) {
        int[] cols = new int[pi2.size()];
        for (int i = 0; i < cols.length; i++) {
            cols[i] = variables.indexOf(pi2.get(i));
        }
        ICovarianceMatrix cov = getCovariances().getSubmatrix(cols);
        return new SemBicScore(cov);
    }

    public String toString() {
        return "SEM BIC Score";
    }

    /**
     * Gives two options for calculating the BIC score, one describe by Chickering and the other due to Nandy et al.
     */
    public enum RuleType {CHICKERING, NANDY}
}


