///////////////////////////////////////////////////////////////////////////////
// For information as to what this class does, see the Javadoc, below.       //
// Copyright (C) 1998, 1999, 2000, 2001, 2002, 2003, 2004, 2005, 2006,       //
// 2007, 2008, 2009, 2010, 2014, 2015 by Peter Spirtes, Richard Scheines, Joseph   //
// Ramsey, and Clark Glymour.                                                //
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

package edu.cmu.tetradapp.editor;

import edu.cmu.tetrad.data.ContinuousVariable;
import edu.cmu.tetrad.data.DataSet;
import edu.cmu.tetrad.graph.Node;
import edu.cmu.tetrad.regression.Regression;
import edu.cmu.tetrad.regression.RegressionDataset;
import edu.cmu.tetrad.regression.RegressionResult;
import edu.cmu.tetrad.util.Matrix;
import edu.cmu.tetrad.util.RandomUtil;
import edu.cmu.tetrad.util.StatUtils;

import java.awt.geom.Point2D;
import java.util.*;

import static java.lang.Math.abs;
import static java.lang.Math.log;

/**
 * This is the scatterplot model class holding the necessary information to
 * create a scatterplot. It uses Point2D to hold the pair of values need to
 * create the scatterplot.
 *
 * @author Adrian Tang
 * @author Joseph Ramsey
 */
public class ScatterPlot {
    private final String x;
    private final String y;
    private final boolean includeLine;
    private final DataSet dataSet;
    private Map<Node, double[]> continuousIntervals;

    /**
     * Constructor.
     *
     * @param includeLine whether or not to include the regression line in the
     *                    plot.
     * @param x           y-axis variable name.
     * @param y           x-axis variable name.
     */
    public ScatterPlot(
            final DataSet dataSet,
            final boolean includeLine,
            final String x,
            final String y) {
        this.dataSet = dataSet;
        this.x = x;
        this.y = y;
        this.includeLine = includeLine;
        this.continuousIntervals = new HashMap<>();
    }

    private RegressionResult getRegressionResult() {
        final List<Node> regressors = new ArrayList<>();
        regressors.add(this.dataSet.getVariable(this.x));
        final Node target = this.dataSet.getVariable(this.y);
        final Regression regression = new RegressionDataset(this.dataSet);
        final RegressionResult result = regression.regress(target, regressors);
        System.out.println(result);
        return result;
    }

    public double getCorrelationCoeff() {
        final DataSet dataSet = getDataSet();
        final Matrix data = dataSet.getDoubleData();

        final int _x = dataSet.getColumn(dataSet.getVariable(this.x));
        final int _y = dataSet.getColumn(dataSet.getVariable(this.y));

        final double[] xdata = data.getColumn(_x).toArray();
        final double[] ydata = data.getColumn(_y).toArray();

        double correlation = StatUtils.correlation(xdata, ydata);

        if (correlation > 1) correlation = 1;
        else if (correlation < -1) correlation = -1;

        return correlation;
    }

    /**
     * @return the p-value of the correlation coefficient statistics.
     */
    public double getCorrelationPValue() {
        final double r = getCorrelationCoeff();
        final double fisherZ = fisherz(r);
        final double pValue;

        if (Double.isInfinite(fisherZ)) {
            pValue = 0;
        } else {
            pValue = 2.0 * (1.0 - RandomUtil.getInstance().normalCdf(0, 1, abs(fisherZ)));
        }

        return pValue;
    }

    private double fisherz(final double r) {
        return 0.5 * Math.sqrt(getSampleSize() - 3.0) * (log(1.0 + r) - log(1.0 - r));
    }

    /**
     * @return the minimum x-axis value from the set of sample values.
     */
    public double getXmin() {
        double min = Double.POSITIVE_INFINITY;
        final Vector<Point2D.Double> cleanedSampleValues = getSievedValues();
        for (final Point2D.Double cleanedSampleValue : cleanedSampleValues) {
            min = Math.min(min, cleanedSampleValue.getX());
        }
        return min;
    }

    /**
     * @return the minimum y-axis value from the set of sample values.
     */
    public double getYmin() {
        double min = Double.POSITIVE_INFINITY;
        final Vector<Point2D.Double> cleanedSampleValues = getSievedValues();
        for (final Point2D.Double cleanedSampleValue : cleanedSampleValues) {
            min = Math.min(min, cleanedSampleValue.getY());
        }
        return min;
    }

    /**
     * @return the maximum x-axis value from the set of sample values.
     */
    public double getXmax() {
        double max = Double.NEGATIVE_INFINITY;
        final Vector<Point2D.Double> cleanedSampleValues = getSievedValues();
        for (final Point2D.Double cleanedSampleValue : cleanedSampleValues) {
            max = Math.max(max, cleanedSampleValue.getX());
        }
        return max;
    }

    /**
     * @return the maximum y-axis value from the set of sample values.
     */
    public double getYmax() {
        double max = Double.NEGATIVE_INFINITY;
        final Vector<Point2D.Double> cleanedSampleValues = getSievedValues();
        for (final Point2D.Double cleanedSampleValue : cleanedSampleValues) {
            max = Math.max(max, cleanedSampleValue.getY());
        }
        return max;
    }

    /**
     * Seives through the sample values and grabs only the values for the
     * response and predictor variables.
     *
     * @return a vector containing the filtered values.
     */
    public Vector<Point2D.Double> getSievedValues() {
        final Vector<Point2D.Double> pairs = pairs(this.x, this.y);
        return pairs;
    }

    /**
     * @return size of the sample.
     */
    private int getSampleSize() {
        return getSievedValues().size();
    }

    /**
     * @return the name of the predictor variable.
     */
    public String getXvar() {
        return this.x;
    }

    /**
     * @return the name of the response variable.
     */
    public String getYvar() {
        return this.y;
    }

    /**
     * @return whether or not to include the regression line.
     */
    public boolean isIncludeLine() {
        return this.includeLine;
    }

    /**
     * Calculates the regression coefficient for the variables
     * return a regression coeff
     */
    public double getRegressionCoeff() {
        return getRegressionResult().getCoef()[1];
    }

    /**
     * @return the zero intercept of the regression equation.
     */
    public double getRegressionIntercept() {
        return getRegressionResult().getCoef()[0];
    }

    public DataSet getDataSet() {
        return this.dataSet;
    }


    //========================================PUBLIC METHODS=================================//

    /**
     * Adds a continuous conditioning variables, conditioning on a range of values.
     *
     * @param variable The name of the variable in the data set.
     * @param low      The low end of the conditioning range.
     * @param high     The high end of the conditioning range.
     */
    public void addConditioningVariable(final String variable, final double low, final double high) {
        if (!(low < high)) throw new IllegalArgumentException("Low must be less than high: " + low + " >= " + high);

        final Node node = this.dataSet.getVariable(variable);
        if (!(node instanceof ContinuousVariable)) throw new IllegalArgumentException("Variable must be continuous.");
        if (this.continuousIntervals.containsKey(node))
            throw new IllegalArgumentException("Please remove conditioning variable first.");

        this.continuousIntervals.put(node, new double[]{low, high});
    }

    /**
     * Removes a conditioning variable.
     *
     * @param variable The name of the conditioning variable to remove.
     */
    public void removeConditioningVariable(final String variable) {
        final Node node = this.dataSet.getVariable(variable);
        if (!(this.continuousIntervals.containsKey(node))) {
            throw new IllegalArgumentException("Not a conditioning node: " + variable);
        }
        this.continuousIntervals.remove(node);
    }

    public void removeConditioningVariables() {
        this.continuousIntervals = new HashMap<>();
    }

    /**
     * For a continuous target, returns the number of values histogrammed. This may be
     * less than the sample size of the data set because of conditioning.
     */
    public int getN(final String target) {
        final List<Double> conditionedDataContinuous = getConditionedDataContinuous(target);
        return conditionedDataContinuous.size();
    }

    /**
     * A convenience method to return the data for a particular named continuous
     * variable.
     *
     * @param variable The name of the variable.
     */
    public double[] getContinuousData(final String variable) {
        final int index = this.dataSet.getColumn(this.dataSet.getVariable(variable));
        final List<Double> _data = new ArrayList<>();

        for (int i = 0; i < this.dataSet.getNumRows(); i++) {
            _data.add(this.dataSet.getDouble(i, index));
        }

        return asDoubleArray(_data);
    }

    //======================================PRIVATE METHODS=======================================//

    private double[] asDoubleArray(final List<Double> data) {
        final double[] _data = new double[data.size()];
        for (int i = 0; i < data.size(); i++) _data[i] = data.get(i);
        return _data;
    }

    private List<Double> getUnconditionedDataContinuous(final String target) {
        final int index = this.dataSet.getColumn(this.dataSet.getVariable(target));

        final List<Double> _data = new ArrayList<>();

        for (int i = 0; i < this.dataSet.getNumRows(); i++) {
            _data.add(this.dataSet.getDouble(i, index));
        }

        return _data;
    }

    private List<Double> getConditionedDataContinuous(final String target) {
        if (this.continuousIntervals == null) return getUnconditionedDataContinuous(target);

        final List<Integer> rows = getConditionedRows();

        final int index = this.dataSet.getColumn(this.dataSet.getVariable(target));

        final List<Double> _data = new ArrayList<>();

        for (final Integer row : rows) {
            _data.add(this.dataSet.getDouble(row, index));
        }

        return _data;
    }

    // Returns the rows in the data that satisfy the conditioning constraints.
    private List<Integer> getConditionedRows() {
        final List<Integer> rows = new ArrayList<>();

        I:
        for (int i = 0; i < this.dataSet.getNumRows(); i++) {
            for (final Node node : this.continuousIntervals.keySet()) {
                final double[] range = this.continuousIntervals.get(node);
                final int index = this.dataSet.getColumn(node);
                final double value = this.dataSet.getDouble(i, index);
                if (!(value > range[0] && value < range[1])) {
                    continue I;
                }
            }

            rows.add(i);
        }

        return rows;
    }

    private Vector<Point2D.Double> pairs(final String x, final String y) {
        Point2D.Double pt;
        final Vector<Point2D.Double> cleanedVals = new Vector<>();

        final List<Double> _x = getConditionedDataContinuous(x);
        final List<Double> _y = getConditionedDataContinuous(y);

        for (int row = 0; row < _x.size(); row++) {
            pt = new Point2D.Double();
            pt.setLocation(_x.get(row), _y.get(row));
            cleanedVals.add(pt);
        }

        return cleanedVals;
    }

}



