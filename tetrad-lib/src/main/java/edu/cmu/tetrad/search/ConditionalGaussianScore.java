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

package edu.cmu.tetrad.search;

import edu.cmu.tetrad.data.ContinuousVariable;
import edu.cmu.tetrad.data.DataSet;
import edu.cmu.tetrad.data.DiscreteVariable;
import edu.cmu.tetrad.graph.Node;
import edu.cmu.tetrad.util.CombinationIterator;
import edu.cmu.tetrad.util.TetradMatrix;
import org.apache.commons.math3.stat.correlation.Covariance;

import java.util.*;

/**
 * Implements a conditional Gaussian BIC score for FGS.
 *
 * @author Joseph Ramsey
 */
public class ConditionalGaussianScore implements Score {

    private DataSet dataSet;

    // The variables of the continuousData set.
    private List<Node> variables;

    // Continuous data only.
    private double[][] continuousData;

    // Discrete data only.
    private int[][] discreteData;

    // Indices of variables.
    private Map<Node, Integer> nodesHash;

    /**
     * Constructs the score using a covariance matrix.
     */
    public ConditionalGaussianScore(DataSet dataSet) {
        if (dataSet == null) {
            throw new NullPointerException();
        }

        this.dataSet = dataSet;
        this.variables = dataSet.getVariables();

        continuousData = new double[dataSet.getNumColumns()][];
        discreteData = new int[dataSet.getNumColumns()][];

        for (int j = 0; j < dataSet.getNumColumns(); j++) {
            Node v = dataSet.getVariable(j);

            if (v instanceof ContinuousVariable) {
                double[] col = new double[dataSet.getNumRows()];

                for (int i = 0; i < dataSet.getNumRows(); i++) {
                    col[i] = dataSet.getDouble(i, j);
                }

                continuousData[j] = col;
            } else if (v instanceof DiscreteVariable) {
                int[] col = new int[dataSet.getNumRows()];

                for (int i = 0; i < dataSet.getNumRows(); i++) {
                    col[i] = dataSet.getInt(i, j);
                }

                discreteData[j] = col;
            }
        }

        nodesHash = new HashMap<>();

        for (int j = 0; j < dataSet.getNumColumns(); j++) {
            Node v = dataSet.getVariable(j);
            nodesHash.put(v, j);
        }
    }

    /**
     * Calculates the sample likelihood and BIC score for i given its parents in a simple SEM model
     */
    public double localScore(int i, int... parents) {
        Node target = variables.get(i);

        List<ContinuousVariable> denominatorContinuous = new ArrayList<>();
        List<DiscreteVariable> denominatorDiscrete = new ArrayList<>();

        for (int parent1 : parents) {
            Node parent = variables.get(parent1);

            if (parent instanceof ContinuousVariable) {
                denominatorContinuous.add((ContinuousVariable) parent);
            } else {
                denominatorDiscrete.add((DiscreteVariable) parent);
            }
        }

        List<ContinuousVariable> numeratorContinuous = new ArrayList<>(denominatorContinuous);
        List<DiscreteVariable> numeratorDiscrete = new ArrayList<>(denominatorDiscrete);

        if (target instanceof ContinuousVariable) {
            numeratorContinuous.add((ContinuousVariable) target);
        } else if (target instanceof DiscreteVariable) {
            numeratorDiscrete.add((DiscreteVariable) target);
        } else {
            throw new IllegalStateException();
        }

        int N = dataSet.getNumRows();

        double lik;
        double dof;

        if (numeratorContinuous.isEmpty()) {

            // Discrete target, discrete predictors.
            if (!(target instanceof DiscreteVariable)) throw new IllegalStateException();
            Ret ret = getLikelihood((DiscreteVariable) target, denominatorDiscrete);
            lik = ret.getLik();
            dof = ret.getDof();
        } else if (denominatorContinuous.isEmpty()) {

            // Continuous target, all discrete predictors.
            Ret ret1 = getLikelihood(numeratorContinuous, numeratorDiscrete);
            dof = ret1.getDof();
            lik = ret1.getLik();
        } else if (numeratorContinuous.size() == denominatorContinuous.size()) {

            // Discrete target, mixed predictors.
            Ret ret1 = getLikelihood(numeratorContinuous, numeratorDiscrete);
            Ret ret2 = getLikelihood(numeratorContinuous, denominatorDiscrete);
            dof = ret1.getDof() - ret2.getDof();
            lik = ret1.getLik() - ret2.getLik();
        } else {

            // Continuous target, mixed predictors.
            Ret ret1 = getLikelihood(numeratorContinuous, numeratorDiscrete);
            Ret ret2 = getLikelihood(denominatorContinuous, numeratorDiscrete);
            dof = ret1.getDof() - ret2.getDof();
            lik = ret1.getLik() - ret2.getLik();
        }

        return lik - dof * Math.log(N);
    }

    private Ret getLikelihood1(DiscreteVariable target, List<DiscreteVariable> parents) {

        if (parents.contains(target)) throw new IllegalArgumentException();
        int p = target.getNumCategories();
        int targetCol = nodesHash.get(target);

        int d = parents.size();
        int[] dims = new int[d];

        for (int i = 0; i < d; i++) {
            dims[i] = parents.get(i).getNumCategories();
        }

        CombinationIterator iterator = new CombinationIterator(dims);
        int[] comb;

        int[] parentCols = new int[d];
        for (int i = 0; i < d; i++) parentCols[i] = dataSet.getColumn(parents.get(i));

        double lik = 0;
        int s = 0;
        int N = dataSet.getNumRows();

        while (iterator.hasNext()) {
            comb = iterator.next();
            List<Integer> rows = new ArrayList<>();

            for (int i = 0; i < dataSet.getNumRows(); i++) {
                boolean addRow = true;

                for (int c = 0; c < comb.length; c++) {
                    if (comb[c] != discreteData[parentCols[c]][i]) {
                        addRow = false;
                        break;
                    }
                }

                if (addRow) {
                    rows.add(i);
                }
            }

            double[] counts = new double[p];
            int r = 0;

            for (int row : rows) {
                int value = discreteData[targetCol][row];
                counts[value]++;
                r++;
            }

            double l = 0;

            for (int c = 0; c < p; c++) {
                double count = counts[c];

                if (count > 0) {
                    l += count * (Math.log(count) - Math.log(r));
                    l += Math.log(r) - Math.log(N);
                }
            }

            lik += l;
            s++;
        }

        int t = p - 1;
        int dof = s * t;

        return new Ret(lik, dof);
    }

    private Ret getLikelihood(DiscreteVariable target, List<DiscreteVariable> parents) {
        if (parents.contains(target)) throw new IllegalArgumentException();

        int p = target.getNumCategories();
        int targetCol = nodesHash.get(target);

        int d = parents.size();
        int[] dims = new int[d];
        for (int i = 0; i < d; i++) dims[i] = parents.get(i).getNumCategories();

        int[] parentCols = new int[d];
        for (int i = 0; i < d; i++) parentCols[i] = nodesHash.get(parents.get(i));

        int numRows = 1;
        for (int dim : dims) numRows *= dim;
        List<List<Integer>> rows = new ArrayList<>();
        for (int i = 0; i < numRows; i++) rows.add(new ArrayList<Integer>());

        int[] values = new int[dims.length];

        for (int i = 0; i < dataSet.getNumRows(); i++) {
            for (int j = 0; j < dims.length; j++) {
                values[j] = discreteData[parentCols[j]][i];
            }

            int rowIndex = getRowIndex(values, dims);
            rows.get(rowIndex).add(i);
        }

        double lik = 0;
        int N = dataSet.getNumRows();

        for (int k = 0; k < rows.size(); k++) {
            if (rows.get(k).isEmpty()) continue;

            double[] counts = new double[p];

            for (int row : rows.get(k)) {
                int value = discreteData[targetCol][row];
                counts[value]++;
            }

            int r = rows.get(k).size();

            for (int c = 0; c < p; c++) {
                double count = counts[c];

                if (count > 0) {
                    lik += count * (Math.log(count) - Math.log(r));// + Math.log(r) - Math.log(N);
                }
            }
        }

        int s = rows.size();
        int t = p;

        double dof = s * t;

        return new Ret(lik, dof);
    }

    private Ret getLikelihood(List<ContinuousVariable> continuous, List<DiscreteVariable> discrete) {
        if (continuous.isEmpty()) throw new IllegalArgumentException();
        int p = continuous.size();
        int d = discrete.size();

        // For each combination of values for the discrete guys extract a subset of the data.
        int[] discreteCols = new int[d];
        int[] continuousCols = new int[p];
        int[] dims = new int[d];

        for (int i = 0; i < d; i++) discreteCols[i] = nodesHash.get(discrete.get(i));
        for (int j = 0; j < p; j++) continuousCols[j] = nodesHash.get(continuous.get(j));
        for (int i = 0; i < d; i++) dims[i] = discrete.get(i).getNumCategories();

        int numRows = 1;
        for (int dim : dims) numRows *= dim;
        List<List<Integer>> rows = new ArrayList<>();
        for (int i = 0; i < numRows; i++) {
            rows.add(new ArrayList<Integer>());
        }

        int[] values = new int[dims.length];

        for (int i = 0; i < dataSet.getNumRows(); i++) {
            for (int j = 0; j < dims.length; j++) {
                values[j] = discreteData[discreteCols[j]][i];
            }

            int rowIndex = getRowIndex(values, dims);
            rows.get(rowIndex).add(i);
        }

        double lik = 0;
        int s = 0;
        double C = 0.5 * p * (.5 + Math.log(2 * Math.PI));

        for (int k = 0; k < rows.size(); k++) {
            if (rows.get(k).isEmpty()) continue;

            int n = rows.get(k).size();

            TetradMatrix subset = new TetradMatrix(rows.get(k).size(), continuousCols.length);

            for (int i = 0; i < n; i++) {
                for (int j = 0; j < p; j++) {
                    subset.set(i, j, continuousData[continuousCols[j]][rows.get(k).get(i)] );
                }
            }

            if (n > p) {
                TetradMatrix Sigma = new TetradMatrix(new Covariance(subset.getRealMatrix(),
                        false).getCovarianceMatrix());
                lik -= 0.5 * n * Math.log(Sigma.det());
                lik -= n * C;
                s++;
            } else {
                lik -= 0.5 * n * Math.log(p + 2); // guestimate--not enough data.
                lik -= n * C;
                s++;
            }
        }

        int t = p * (p + 1) / 2;
        double dof = s * t;

        List<DiscreteVariable> condDiscrete = new ArrayList<>(discrete);

        for (DiscreteVariable f : new ArrayList<>(condDiscrete)) {
            condDiscrete.remove(f);
            Ret ret = getLikelihood(f, condDiscrete);
            lik += ret.getLik();
            dof += ret.getDof();
        }

        return new Ret(lik, dof);
    }

    private class Ret {
        private double lik;
        private double dof;

        public Ret(double lik, double dof) {
            this.lik = lik;
            this.dof = dof;
        }

        public double getLik() {
            return lik;
        }

        public double getDof() {
            return dof;
        }
    }

    public double localScoreDiff(int x, int y, int[] z) {
        return localScore(y, append(z, x)) - localScore(y, z);
    }

    @Override
    public double localScoreDiff(int x, int y) {
        return localScore(y, x) - localScore(y);
    }

    private int[] append(int[] parents, int extra) {
        int[] all = new int[parents.length + 1];
        System.arraycopy(parents, 0, all, 0, parents.length);
        all[parents.length] = extra;
        return all;
    }

    /**
     * Specialized scoring method for a single parent. Used to speed up the effect edges search.
     */
    public double localScore(int i, int parent) {
        return localScore(i, new int[]{parent});
    }

    /**
     * Specialized scoring method for no parents. Used to speed up the effect edges search.
     */
    public double localScore(int i) {
        return localScore(i, new int[0]);
    }

    public int getSampleSize() {
        return dataSet.getNumRows();
    }

    @Override
    public boolean isEffectEdge(double bump) {
        return bump > -100;
    }

    @Override
    public List<Node> getVariables() {
        return variables;
    }

    @Override
    public double getParameter1() {
        return 0;
    }

    @Override
    public void setParameter1(double alpha) {

    }

    @Override
    public Node getVariable(String targetName) {
        for (Node node : variables) {
            if (node.getName().equals(targetName)) {
                return node;
            }
        }

        return null;
    }

    @Override
    public int getMaxIndegree() {
        return (int) Math.ceil(Math.log(dataSet.getNumRows()));
    }

    public int getRowIndex(int[] values, int[] dims) {
        int rowIndex = 0;

        for (int i = 0; i < dims.length; i++) {
            rowIndex *= dims[i];
            rowIndex += values[i];
        }

        return rowIndex;
    }
}



