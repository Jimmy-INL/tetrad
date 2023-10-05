package edu.cmu.tetrad.algcomparison.statistic;

import edu.cmu.tetrad.data.DataModel;
import edu.cmu.tetrad.graph.Graph;
import edu.cmu.tetrad.graph.Node;
import edu.cmu.tetrad.search.utils.GraphSearchUtils;

import java.util.List;

/**
 * The bidirected true positives.
 *
 * @author josephramsey
 */
public class NoSemidirectedPrecision implements Statistic {
    private static final long serialVersionUID = 23L;

    @Override
    public String getAbbreviation() {
        return "NoSemidirected-Prec";
    }

    @Override
    public String getDescription() {
        return "Proportion of (X, Y) where if no semidirected path in est then also not in true";
    }

    @Override
    public double getValue(Graph trueGraph, Graph estGraph, DataModel dataModel) {
        int tp = 0, fp = 0;

        Graph cpdag = GraphSearchUtils.cpdagForDag(trueGraph);

        List<Node> nodes = estGraph.getNodes();

        for (Node x : nodes) {
            for (Node y : nodes) {
                if (x == y) continue;

                if (!estGraph.paths().existsSemiDirectedPath(x, y)) {
                    if (!cpdag.paths().existsSemiDirectedPath(x, y)) {
                        tp++;
                    } else {
                        fp++;
                    }
                }
            }
        }

        return tp / (double) (tp + fp);
    }

    @Override
    public double getNormValue(double value) {
        return value;
    }
}
