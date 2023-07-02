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

package edu.cmu.tetrad.search.work_in_progress;

import edu.cmu.tetrad.data.CovarianceMatrix;
import edu.cmu.tetrad.data.DataSet;
import edu.cmu.tetrad.data.DataUtils;
import edu.cmu.tetrad.data.ICovarianceMatrix;
import edu.cmu.tetrad.graph.IndependenceFact;
import edu.cmu.tetrad.graph.Node;
import edu.cmu.tetrad.search.IndependenceTest;
import edu.cmu.tetrad.search.test.IndependenceResult;
import edu.cmu.tetrad.search.utils.LogUtilsSearch;
import edu.cmu.tetrad.util.Matrix;
import edu.cmu.tetrad.util.RandomUtil;
import edu.cmu.tetrad.util.StatUtils;
import edu.cmu.tetrad.util.TetradLogger;
import org.apache.commons.math3.linear.SingularMatrixException;

import java.util.*;

import static org.apache.commons.math3.util.FastMath.*;

/**
 * Calculates independence from pooled residuals.
 *
 * @author josephramsey
 */
public final class IndTestFisherZPercentIndependent implements IndependenceTest {
    private final List<Node> variables;
    private final List<DataSet> dataSets;
    private double alpha;
    private final int[] rows;
    private final List<Matrix> data;
    private final List<Matrix> ncov;
    private final Map<Node, Integer> variablesMap;
    private double percent = .75;
    private boolean fdr = true;
    private boolean verbose;

    //==========================CONSTRUCTORS=============================//

    public IndTestFisherZPercentIndependent(List<DataSet> dataSets, double alpha) {
        this.dataSets = dataSets;
        this.variables = dataSets.get(0).getVariables();

        if (!(alpha >= 0 && alpha <= 1)) {
            throw new IllegalArgumentException("Alpha mut be in [0, 1]");
        }

        this.data = new ArrayList<>();

        for (DataSet dataSet : dataSets) {
            dataSet = DataUtils.center(dataSet);
            Matrix _data = dataSet.getDoubleData();
            this.data.add(_data);
        }

        this.ncov = new ArrayList<>();
        for (Matrix d : this.data) this.ncov.add(d.transpose().times(d).scalarMult(1.0 / d.getNumRows()));

        setAlpha(alpha);
        this.rows = new int[dataSets.get(0).getNumRows()];
        for (int i = 0; i < getRows().length; i++) getRows()[i] = i;

        this.variablesMap = new HashMap<>();
        for (int i = 0; i < this.variables.size(); i++) {
            this.variablesMap.put(this.variables.get(i), i);
        }
    }

    //==========================PUBLIC METHODS=============================//

    public IndependenceTest indTestSubset(List<Node> vars) {
        throw new UnsupportedOperationException();
    }

    public IndependenceResult checkIndependence(Node x, Node y, Set<Node> _z) {
        try {
            List<Node> z = new ArrayList<>(_z);
            Collections.sort(z);

            int[] all = new int[z.size() + 2];
            all[0] = this.variablesMap.get(x);
            all[1] = this.variablesMap.get(y);
            for (int i = 0; i < z.size(); i++) {
                all[i + 2] = this.variablesMap.get(z.get(i));
            }

            int sampleSize = this.data.get(0).getNumRows();
            List<Double> pValues = new ArrayList<>();

            for (Matrix matrix : this.ncov) {
                Matrix _ncov = matrix.getSelection(all, all);
                Matrix inv = _ncov.inverse();
                double r = -inv.get(0, 1) / sqrt(inv.get(0, 0) * inv.get(1, 1));

                double fisherZ = sqrt(sampleSize - z.size() - 3.0) * 0.5 * (log(1.0 + r) - log(1.0 - r));
                double pValue;

                if (Double.isInfinite(fisherZ)) {
                    pValue = 0;
                } else {
                    pValue = 2.0 * (1.0 - RandomUtil.getInstance().normalCdf(0, 1, abs(fisherZ)));
                }

                pValues.add(pValue);
            }

            double _cutoff = this.alpha;

            if (this.fdr) {
                _cutoff = StatUtils.fdrCutoff(this.alpha, pValues, false);
            }

            Collections.sort(pValues);
            int index = (int) round((1.0 - this.percent) * pValues.size());
            double pValue = pValues.get(index);

            boolean independent = pValue > _cutoff;

            if (this.verbose) {
                if (independent) {
                    TetradLogger.getInstance().forceLogMessage(
                            LogUtilsSearch.independenceFactMsg(x, y, _z, pValue));
                }
            }

            return new IndependenceResult(new IndependenceFact(x, y, _z), independent, pValue, getAlpha() - pValue);
        } catch (SingularMatrixException e) {
            throw new RuntimeException("Singularity encountered when testing " +
                    LogUtilsSearch.independenceFact(x, y, _z));
        }
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

    /**
     * @return the variable with the given name.
     */


    /**
     * @throws UnsupportedOperationException
     */
    public boolean determines(List z, Node x) throws UnsupportedOperationException {
        throw new UnsupportedOperationException();
    }

    /**
     * @throws UnsupportedOperationException
     */
    public DataSet getData() {
        return DataUtils.concatenate(this.dataSets);
    }

    public ICovarianceMatrix getCov() {
        List<DataSet> _dataSets = new ArrayList<>();

        for (DataSet d : this.dataSets) {
            _dataSets.add(DataUtils.standardizeData(d));
        }

        return new CovarianceMatrix(DataUtils.concatenate(this.dataSets));
    }

    @Override
    public List<DataSet> getDataSets() {
        return this.dataSets;
    }

    @Override
    public int getSampleSize() {
        return this.dataSets.get(0).getNumRows();
    }

    /**
     * @return a string representation of this test.
     */
    public String toString() {
        return "Fisher Z, Percent Independent";
    }

    public int[] getRows() {
        return this.rows;
    }

    public double getPercent() {
        return this.percent;
    }

    public void setPercent(double percent) {
        if (percent < 0.0 || percent > 1.0) throw new IllegalArgumentException();
        this.percent = percent;
    }

    public void setFdr(boolean fdr) {
        this.fdr = fdr;
    }

    public boolean isVerbose() {
        return this.verbose;
    }

    public void setVerbose(boolean verbose) {
        this.verbose = verbose;
    }
}


