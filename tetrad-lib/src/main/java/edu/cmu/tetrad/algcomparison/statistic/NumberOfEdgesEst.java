package edu.cmu.tetrad.algcomparison.statistic;

import edu.cmu.tetrad.data.DataModel;
import edu.cmu.tetrad.graph.Graph;
import org.apache.commons.math3.util.FastMath;

/**
 * Prints the number of edges in the estimated graph.
 *
 * @author josephramsey
 */
public class NumberOfEdgesEst implements Statistic {
    private static final long serialVersionUID = 23L;

    @Override
    public String getAbbreviation() {
        return "EdgesEst";
    }

    @Override
    public String getDescription() {
        return "Number of Edges in the Estimated Graph";
    }

    @Override
    public double getValue(Graph trueGraph, Graph estGraph, DataModel dataModel) {
        return estGraph.getNumEdges();
    }

    @Override
    public double getNormValue(double value) {
        return FastMath.tanh(value);
    }
}
