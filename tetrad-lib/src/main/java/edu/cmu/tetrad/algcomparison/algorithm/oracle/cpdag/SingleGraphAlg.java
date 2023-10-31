package edu.cmu.tetrad.algcomparison.algorithm.oracle.cpdag;

import edu.cmu.tetrad.algcomparison.algorithm.Algorithm;
import edu.cmu.tetrad.algcomparison.utils.HasKnowledge;
import edu.cmu.tetrad.data.DataModel;
import edu.cmu.tetrad.data.DataType;
import edu.cmu.tetrad.data.Knowledge;
import edu.cmu.tetrad.graph.EdgeListGraph;
import edu.cmu.tetrad.graph.Graph;
import edu.cmu.tetrad.util.Parameters;

import java.util.ArrayList;
import java.util.List;

/**
 * PC.
 *
 * @author josephramsey
 */
public class SingleGraphAlg implements Algorithm, HasKnowledge {

    private static final long serialVersionUID = 23L;
    private final Graph graph;

    public SingleGraphAlg(Graph graph) {
        this.graph = graph;
    }

    @Override
    public Graph search(DataModel dataSet, Parameters parameters) {
        return this.graph;
    }

    @Override
    public Graph getComparisonGraph(Graph graph) {
        return new EdgeListGraph(graph);
    }

    @Override
    public String getDescription() {
        return "Given graph";
    }

    @Override
    public DataType getDataType() {
        return DataType.Mixed;
    }

    @Override
    public List<String> getParameters() {
        return new ArrayList<>();
    }

    @Override
    public Knowledge getKnowledge() {
        return new Knowledge();
    }

    @Override
    public void setKnowledge(Knowledge knowledge) {
    }

}
