package edu.cmu.tetrad.algcomparison.algorithm.oracle.cpdag;

import edu.cmu.tetrad.algcomparison.algorithm.Algorithm;
import edu.cmu.tetrad.algcomparison.algorithm.ReturnsBootstrapGraphs;
import edu.cmu.tetrad.algcomparison.independence.IndependenceWrapper;
import edu.cmu.tetrad.algcomparison.utils.HasKnowledge;
import edu.cmu.tetrad.algcomparison.utils.TakesIndependenceWrapper;
import edu.cmu.tetrad.annotation.AlgType;
import edu.cmu.tetrad.annotation.Bootstrapping;
import edu.cmu.tetrad.data.DataModel;
import edu.cmu.tetrad.data.DataSet;
import edu.cmu.tetrad.data.DataType;
import edu.cmu.tetrad.data.Knowledge;
import edu.cmu.tetrad.graph.EdgeListGraph;
import edu.cmu.tetrad.graph.Graph;
import edu.cmu.tetrad.graph.GraphUtils;
import edu.cmu.tetrad.graph.Node;
import edu.cmu.tetrad.search.IndependenceTest;
import edu.cmu.tetrad.util.Parameters;
import edu.cmu.tetrad.util.Params;
import edu.pitt.dbmi.algo.resampling.GeneralResamplingTest;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * PC.
 *
 * @author josephramsey
 */
@edu.cmu.tetrad.annotation.Algorithm(
        name = "PC-MB",
        command = "pc-mb",
        algoType = AlgType.search_for_Markov_blankets
)
@Bootstrapping
public class PcMb implements Algorithm, HasKnowledge, TakesIndependenceWrapper,
        ReturnsBootstrapGraphs {

    static final long serialVersionUID = 23L;
    private IndependenceWrapper test;
    private Knowledge knowledge = new Knowledge();
    private List<Node> targets;
    private List<Graph> bootstrapGraphs = new ArrayList<>();

    public PcMb() {
    }

    public PcMb(IndependenceWrapper type) {
        this.test = type;
    }

    @Override
    public Graph search(DataModel dataSet, Parameters parameters) {
        if (parameters.getInt(Params.NUMBER_RESAMPLING) < 1) {
            IndependenceTest test = this.test.getTest(dataSet, parameters);
            edu.cmu.tetrad.search.PcMb search = new edu.cmu.tetrad.search.PcMb(test, parameters.getInt(Params.DEPTH));
            List<Node> targets = targets(test, parameters.getString(Params.TARGETS));
            this.targets = targets;
            search.setDepth(parameters.getInt(Params.DEPTH));
            search.setKnowledge(this.knowledge);
            search.setFindMb(parameters.getBoolean(Params.MB));
            return search.search(targets);
        } else {
            PcMb algorithm = new PcMb(this.test);

            DataSet data = (DataSet) dataSet;
            GeneralResamplingTest search = new GeneralResamplingTest(data, algorithm, parameters.getInt(Params.NUMBER_RESAMPLING), parameters.getDouble(Params.PERCENT_RESAMPLE_SIZE), parameters.getBoolean(Params.RESAMPLING_WITH_REPLACEMENT), parameters.getInt(Params.RESAMPLING_ENSEMBLE), parameters.getBoolean(Params.ADD_ORIGINAL_DATASET));
            search.setKnowledge(this.knowledge);

            search.setParameters(parameters);
            search.setVerbose(parameters.getBoolean(Params.VERBOSE));
            Graph graph = search.search();
            if (parameters.getBoolean(Params.SAVE_BOOTSTRAP_GRAPHS)) this.bootstrapGraphs = search.getGraphs();
            return graph;
        }
    }

    @NotNull
    private List<Node> targets(IndependenceTest test, String targetString) {
        String[] tokens = targetString.split(",");
        List<Node> targets = new ArrayList<>();

        for (String t : tokens) {
            String name = t.trim();
            targets.add(test.getVariable(name));
        }
        return targets;
    }

    @Override
    public Graph getComparisonGraph(Graph graph) {
        return GraphUtils.markovBlanketSubgraph(targets.get(0), new EdgeListGraph(graph));
    }

    @Override
    public String getDescription() {
        return "PC-MB (Markov blanket search using PC) using " + this.test.getDescription();
    }

    @Override
    public DataType getDataType() {
        return this.test.getDataType();
    }

    @Override
    public List<String> getParameters() {
        List<String> parameters = new ArrayList<>();
        parameters.add(Params.TARGETS);
        parameters.add(Params.MB);
        parameters.add(Params.DEPTH);

        parameters.add(Params.VERBOSE);
        return parameters;
    }

    @Override
    public Knowledge getKnowledge() {
        return this.knowledge;
    }

    @Override
    public void setKnowledge(Knowledge knowledge) {
        this.knowledge = new Knowledge((Knowledge) knowledge);
    }

    @Override
    public void setIndependenceWrapper(IndependenceWrapper test) {
        this.test = test;
    }

    @Override
    public IndependenceWrapper getIndependenceWrapper() {
        return this.test;
    }

    @Override
    public List<Graph> getBootstrapGraphs() {
        return this.bootstrapGraphs;
    }
}
