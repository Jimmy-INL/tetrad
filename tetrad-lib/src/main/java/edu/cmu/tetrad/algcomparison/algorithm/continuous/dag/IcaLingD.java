package edu.cmu.tetrad.algcomparison.algorithm.continuous.dag;

import edu.cmu.tetrad.algcomparison.algorithm.AbstractBootstrapAlgorithm;
import edu.cmu.tetrad.algcomparison.algorithm.Algorithm;
import edu.cmu.tetrad.algcomparison.algorithm.ReturnsBootstrapGraphs;
import edu.cmu.tetrad.annotation.AlgType;
import edu.cmu.tetrad.annotation.Bootstrapping;
import edu.cmu.tetrad.data.DataModel;
import edu.cmu.tetrad.data.DataSet;
import edu.cmu.tetrad.data.DataType;
import edu.cmu.tetrad.data.SimpleDataLoader;
import edu.cmu.tetrad.graph.EdgeListGraph;
import edu.cmu.tetrad.graph.Graph;
import edu.cmu.tetrad.util.Matrix;
import edu.cmu.tetrad.util.Parameters;
import edu.cmu.tetrad.util.Params;
import edu.cmu.tetrad.util.TetradLogger;

import java.io.Serial;
import java.util.*;

/**
 * IcaLingD is an implementation of the Algorithm interface that performs the ICA-LiNG-D algorithm for discovering
 * causal models for the linear non-Gaussian case where the underlying model might be cyclic.
 *
 * @see edu.cmu.tetrad.search.IcaLingD
 */
@edu.cmu.tetrad.annotation.Algorithm(
        name = "ICA-LiNG-D",
        command = "ica-ling-d",
        algoType = AlgType.forbid_latent_common_causes,
        dataType = DataType.Continuous
)
@Bootstrapping
public class IcaLingD extends AbstractBootstrapAlgorithm implements Algorithm, ReturnsBootstrapGraphs {

    @Serial
    private static final long serialVersionUID = 23L;

    /**
     * Constructs a new instance of the IcaLingD algorithm.
     */
    public IcaLingD() {

    }

    /**
     * Runs a search on the provided data set using the given parameters. If verbose is set to true, all stable and
     * unstable graphs are printed to the console along with their B matrices.
     *
     * @param dataSet    The data set to run the search on.
     * @param parameters The parameters of the search.
     * @return One of the stable graphs, otherwise and empty graph.
     */
    public Graph runSearch(DataModel dataSet, Parameters parameters) {
        DataSet data = SimpleDataLoader.getContinuousDataSet(dataSet);

        int maxIter = parameters.getInt(Params.FAST_ICA_MAX_ITER);
        double alpha = parameters.getDouble(Params.FAST_ICA_A);
        double tol = parameters.getDouble(Params.FAST_ICA_TOLERANCE);
        double bThreshold = parameters.getDouble(Params.THRESHOLD_B);
        double wThreshold = parameters.getDouble(Params.THRESHOLD_W);

        Matrix W = edu.cmu.tetrad.search.IcaLingD.estimateW(data, maxIter, tol, alpha, parameters.getBoolean(Params.VERBOSE));

        edu.cmu.tetrad.search.IcaLingD icaLingD = new edu.cmu.tetrad.search.IcaLingD();
        icaLingD.setBThreshold(bThreshold);
        icaLingD.setWThreshold(wThreshold);
        List<Matrix> bHats = icaLingD.getScaledBHats(W);
        Set<Graph> _unstableGraphs = new HashSet<>();
        Map<Graph, Matrix> _bHats = new HashMap<>();
        Set<Graph> _stableGraphs = new HashSet<>();

        for (Matrix bHat : bHats) {
            Graph graph = edu.cmu.tetrad.search.IcaLingD.makeGraph(bHat, dataSet.getVariables());
            _unstableGraphs.add(graph);
            _bHats.put(graph, bHat);

            if (edu.cmu.tetrad.search.IcaLingD.isStable(bHat)) {
                _stableGraphs.add(graph);
            } else {
                _unstableGraphs.add(graph);
            }
        }

        List<Graph> unstableGraphs = new ArrayList<>(_unstableGraphs);
        List<Graph> stableGraphs = new ArrayList<>(_stableGraphs);
        unstableGraphs.sort(Comparator.comparingInt(Graph::getNumEdges));
        stableGraphs.sort(Comparator.comparingDouble(Graph::getNumEdges));

        int count = 0;

        if (parameters.getBoolean(Params.VERBOSE)) {
            for (Graph graph : unstableGraphs) {
                TetradLogger.getInstance().forceLogMessage("LiNG-D Model #" + (++count) + " Stable = False");
                TetradLogger.getInstance().forceLogMessage(_bHats.get(graph).toString());
                TetradLogger.getInstance().forceLogMessage(graph.toString());
            }
        } else if (!unstableGraphs.isEmpty()) {
            TetradLogger.getInstance().forceLogMessage("To see unstable models and and their B matrices, set the verbose flag to true");
        }

        for (Graph graph : stableGraphs) {
            TetradLogger.getInstance().forceLogMessage("LiNG-D Model #" + (++count) + " Stable = True");
            TetradLogger.getInstance().forceLogMessage(_bHats.get(graph).toString());
            TetradLogger.getInstance().forceLogMessage(graph.toString());
        }

        if (stableGraphs.isEmpty()) {
            TetradLogger.getInstance().forceLogMessage("## There were no stable models. ##");
        }

        return stableGraphs.isEmpty() ? new EdgeListGraph() : stableGraphs.get(0);
    }

    /**
     * Retrieves the comparison graph of the provided true directed graph.
     *
     * @param graph The true directed graph, if there is one.
     * @return The comparison graph.
     */
    @Override
    public Graph getComparisonGraph(Graph graph) {
        return new EdgeListGraph(graph);
    }

    /**
     * Retrieves the description of the algorithm.
     *
     * @return The description of the algorithm.
     */
    public String getDescription() {
        return "LiNG-D (Linear Non-Gaussian Discovery)";
    }

    /**
     * Retrieves the data type of the algorithm.
     *
     * @return The data type of the algorithm.
     */
    @Override
    public DataType getDataType() {
        return DataType.Continuous;
    }

    /**
     * Retrieves the list of parameters used by this method.
     *
     * @return A list of strings representing the parameters used by this method.
     */
    @Override
    public List<String> getParameters() {
        List<String> parameters = new ArrayList<>();
        parameters.add(Params.VERBOSE);
        parameters.add(Params.FAST_ICA_A);
        parameters.add(Params.FAST_ICA_MAX_ITER);
        parameters.add(Params.FAST_ICA_TOLERANCE);
        parameters.add(Params.THRESHOLD_B);
        parameters.add(Params.THRESHOLD_W);
        return parameters;
    }
}
