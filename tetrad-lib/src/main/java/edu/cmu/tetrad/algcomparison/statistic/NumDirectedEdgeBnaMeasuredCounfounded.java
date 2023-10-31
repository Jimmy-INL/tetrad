package edu.cmu.tetrad.algcomparison.statistic;

import edu.cmu.tetrad.data.DataModel;
import edu.cmu.tetrad.graph.Edge;
import edu.cmu.tetrad.graph.Edges;
import edu.cmu.tetrad.graph.Graph;
import edu.cmu.tetrad.graph.Node;

import static edu.cmu.tetrad.algcomparison.statistic.LatentCommonAncestorTruePositiveBidirected.existsLatentCommonAncestor;
import static edu.cmu.tetrad.algcomparison.statistic.NumCommonMeasuredAncestorBidirected.existsCommonAncestor;

/**
 * The bidirected true positives.
 *
 * @author josephramsey
 */
public class NumDirectedEdgeBnaMeasuredCounfounded implements Statistic {
    private static final long serialVersionUID = 23L;

    @Override
    public String getAbbreviation() {
        return "#X->Y-Shouldbe-<->";
    }

    @Override
    public String getDescription() {
        return "Number X-->Y for which both not X~~>Y and not Y~~>X but X<-M->Y (should be X<->Y) ";
    }

    @Override
    public double getValue(Graph trueGraph, Graph estGraph, DataModel dataModel) {
        int tp = 0;

        for (Edge edge : estGraph.getEdges()) {
            if (Edges.isDirectedEdge(edge)) {
                Node x = Edges.getDirectedEdgeTail(edge);
                Node y = Edges.getDirectedEdgeHead(edge);

                if (!trueGraph.paths().isAncestorOf(x, y) && !trueGraph.paths().isAncestorOf(y, x) &&
                        (existsCommonAncestor(trueGraph, edge) && !existsLatentCommonAncestor(trueGraph, edge))) {
                    tp++;
                }
            }
        }

        return tp;
    }

    @Override
    public double getNormValue(double value) {
        return value;
    }
}
