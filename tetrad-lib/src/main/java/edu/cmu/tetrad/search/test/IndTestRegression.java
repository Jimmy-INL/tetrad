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

package edu.cmu.tetrad.search.test;

import cern.colt.matrix.DoubleMatrix1D;
import cern.colt.matrix.DoubleMatrix2D;
import cern.colt.matrix.impl.DenseDoubleMatrix2D;
import cern.colt.matrix.linalg.Algebra;
import cern.jet.math.Functions;
import edu.cmu.tetrad.data.DataSet;
import edu.cmu.tetrad.graph.IndependenceFact;
import edu.cmu.tetrad.graph.Node;
import edu.cmu.tetrad.regression.Regression;
import edu.cmu.tetrad.regression.RegressionDataset;
import edu.cmu.tetrad.regression.RegressionResult;
import edu.cmu.tetrad.search.IndependenceTest;
import edu.cmu.tetrad.search.utils.LogUtilsSearch;
import edu.cmu.tetrad.util.Matrix;
import edu.cmu.tetrad.util.NumberFormatUtil;
import edu.cmu.tetrad.util.TetradLogger;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * Checks independence of X _||_ Y | Z for variables X and Y and list Z of variables by regressing X on {Y} U Z and
 * testing whether the coefficient for Y is zero.
 *
 * @author josephramsey
 * @author Frank Wimberly
 */
public final class IndTestRegression implements IndependenceTest {

    /**
     * The correlation matrix.
     */
    private final DoubleMatrix2D data;

    /**
     * The variables of the correlation matrix, in order. (Unmodifiable list.)
     */
    private final List<Node> variables;

    /**
     * The significance level of the independence tests.
     */
    private double alpha;

    /**
     * The value of the Fisher's Z statistic associated with the las calculated partial correlation.
     */
    private double fishersZ;

    /**
     * The standard number formatter for Tetrad.
     */
    private static final NumberFormat nf = NumberFormatUtil.getInstance().getNumberFormat();
    private final DataSet dataSet;
    private boolean verbose;

    //==========================CONSTRUCTORS=============================//

    /**
     * Constructs a new Independence test which checks independence facts based on the correlation matrix implied by the
     * given data set (must be continuous). The given significance level is used.
     *
     * @param dataSet A data set containing only continuous columns.
     * @param alpha   The alpha level of the test.
     */
    public IndTestRegression(DataSet dataSet, double alpha) {
        if (!(alpha >= 0 && alpha <= 1)) {
            throw new IllegalArgumentException("Alpha mut be in [0, 1]");
        }

        this.dataSet = dataSet;
        this.data = new DenseDoubleMatrix2D(dataSet.getDoubleData().toArray());
        this.variables = Collections.unmodifiableList(dataSet.getVariables());
        setAlpha(alpha);
    }

    //==========================PUBLIC METHODS=============================//

    /**
     * Creates a new IndTestCramerT instance for a subset of the variables.
     */
    public IndependenceTest indTestSubset(List<Node> vars) {
        return null;
    }

    /**
     * Determines whether variable x is independent of variable y given a list of conditioning variables z.
     *
     * @param xVar  the one variable being compared.
     * @param yVar  the second variable being compared.
     * @param zList the list of conditioning variables.
     * @return true iff x _||_ y | z.
     * @throws RuntimeException if a matrix singularity is encountered.
     */
    public IndependenceResult checkIndependence(Node xVar, Node yVar, Set<Node> zList) {
        if (zList == null) {
            throw new NullPointerException();
        }

        for (Node node : zList) {
            if (node == null) {
                throw new NullPointerException();
            }
        }

        List<Node> regressors = new ArrayList<>();
        regressors.add(this.dataSet.getVariable(yVar.getName()));

        for (Node zVar : zList) {
            regressors.add(this.dataSet.getVariable(zVar.getName()));
        }

        Regression regression = new RegressionDataset(this.dataSet);
        RegressionResult result;

        try {
            result = regression.regress(xVar, regressors);
        } catch (Exception e) {
            return new IndependenceResult(new IndependenceFact(xVar, yVar, zList),
                    false, Double.NaN, Double.NaN);
        }

        double p = result.getP()[1];

        boolean independent = p > this.alpha;

        if (this.verbose) {
            if (independent) {
                TetradLogger.getInstance().log("independencies", LogUtilsSearch.independenceFactMsg(xVar, yVar, zList, p));
            } else {
                TetradLogger.getInstance().log("dependencies", LogUtilsSearch.dependenceFactMsg(xVar, yVar, zList, p));
            }
        }

        if (this.verbose) {
            if (independent) {
                TetradLogger.getInstance().forceLogMessage(
                        LogUtilsSearch.independenceFactMsg(xVar, yVar, zList, p));
            }
        }

        return new IndependenceResult(new IndependenceFact(xVar, yVar, zList),
                independent, p, getAlpha() - p);
    }

    /**
     * Sets the significance level at which independence judgments should be made.  Affects the cutoff for partial
     * correlations to be considered statistically equal to zero.
     */
    public void setAlpha(double alpha) {
        if (alpha < 0.0 || alpha > 1.0) {
            throw new IllegalArgumentException("Significance out of range.");
        }

        this.alpha = alpha;
    }

    /**
     * Gets the getModel significance level.
     */
    public double getAlpha() {
        return this.alpha;
    }

    /**
     * @return the list of variables over which this independence checker is capable of determinine independence
     * relations-- that is, all the variables in the given graph or the given data set.
     */
    public List<Node> getVariables() {
        return this.variables;
    }


    public String toString() {
        return "Linear Regression Test, alpha = " + IndTestRegression.nf.format(getAlpha());
    }

    //==========================PRIVATE METHODS============================//

    public boolean determines(List<Node> zList, Node xVar) {
        if (zList == null) {
            throw new NullPointerException();
        }

        for (Node node : zList) {
            if (node == null) {
                throw new NullPointerException();
            }
        }

        int size = zList.size();
        int[] zCols = new int[size];

        int xIndex = getVariables().indexOf(xVar);

        for (int i = 0; i < zList.size(); i++) {
            zCols[i] = getVariables().indexOf(zList.get(i));
        }

        int[] zRows = new int[this.data.rows()];
        for (int i = 0; i < this.data.rows(); i++) {
            zRows[i] = i;
        }

        DoubleMatrix2D Z = this.data.viewSelection(zRows, zCols);
        DoubleMatrix1D x = this.data.viewColumn(xIndex);
        DoubleMatrix2D Zt = new Algebra().transpose(Z);
        DoubleMatrix2D ZtZ = new Algebra().mult(Zt, Z);
        DoubleMatrix2D G = new DenseDoubleMatrix2D(new Matrix(ZtZ.toArray()).inverse().toArray());

        // Bug in Colt? Need to make a copy before multiplying to avoid
        // a ClassCastException.
        DoubleMatrix2D Zt2 = Zt.like();
        Zt2.assign(Zt);
        DoubleMatrix2D GZt = new Algebra().mult(G, Zt2);

        DoubleMatrix1D b_x = new Algebra().mult(GZt, x);

        DoubleMatrix1D xPred = new Algebra().mult(Z, b_x);

        DoubleMatrix1D xRes = xPred.copy().assign(x, Functions.minus);

        double SSE = xRes.aggregate(Functions.plus, Functions.square);
        boolean determined = SSE < 0.0001;

        if (determined) {
            StringBuilder sb = new StringBuilder();
            sb.append("Determination found: ").append(xVar).append(
                    " is determined by {");

            for (int i = 0; i < zList.size(); i++) {
                sb.append(zList.get(i));

                if (i < zList.size() - 1) {
                    sb.append(", ");
                }
            }

            sb.append("}");

            TetradLogger.getInstance().log("independencies", sb.toString());
        }

        return determined;
    }

    public DataSet getData() {
        return this.dataSet;
    }

    public boolean isVerbose() {
        return this.verbose;
    }

    public void setVerbose(boolean verbose) {
        this.verbose = verbose;
    }
}




