package edu.cmu.tetrad.algcomparison.graph;

import edu.cmu.tetrad.graph.Graph;
import edu.cmu.tetrad.util.Parameters;

import java.util.ArrayList;
import java.util.List;

/**
 * Returns a cyclic graph build up from small cyclic graph components.
 *
 * @author josephramsey
 */
public class Cyclic implements RandomGraph {
    private static final long serialVersionUID = 23L;

    @Override
    public Graph createGraph(Parameters parameters) {
        return edu.cmu.tetrad.graph.RandomGraph.randomCyclicGraph3(parameters.getInt("numMeasures"),
                parameters.getInt("avgDegree") * parameters.getInt("numMeasures") / 2,
                parameters.getInt("maxDegree"), parameters.getDouble("probCycle"),
                parameters.getInt("probTwoCycle"));
    }

    @Override
    public String getDescription() {
        return "Cyclic graph built from small cyclic components";
    }

    @Override
    public List<String> getParameters() {
        List<String> paramDescriptions = new ArrayList<>();
        paramDescriptions.add("numMeasures");
        paramDescriptions.add("avgDegree");
        paramDescriptions.add("maxDegree");
        paramDescriptions.add("probCycle");
        paramDescriptions.add("probTwoCycle");
        return paramDescriptions;
    }
}
