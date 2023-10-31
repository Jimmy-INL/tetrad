package edu.cmu.tetrad.algcomparison.simulation;

import edu.cmu.tetrad.algcomparison.graph.RandomGraph;
import edu.cmu.tetrad.algcomparison.graph.SingleGraph;
import edu.cmu.tetrad.algcomparison.utils.HasKnowledge;
import edu.cmu.tetrad.data.*;
import edu.cmu.tetrad.graph.Graph;
import edu.cmu.tetrad.graph.Node;
import edu.cmu.tetrad.graph.TimeLagGraph;
import edu.cmu.tetrad.search.utils.TsUtils;
import edu.cmu.tetrad.sem.SemIm;
import edu.cmu.tetrad.sem.SemPm;
import edu.cmu.tetrad.util.Parameters;
import edu.cmu.tetrad.util.Params;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Time series SEM simulation.
 *
 * @author josephramsey
 * @author danielmalinsky
 */
public class TimeSeriesSemSimulation implements Simulation, HasKnowledge {

    private static final long serialVersionUID = 23L;
    private final RandomGraph randomGraph;
    private List<Graph> graphs = new ArrayList<>();
    private List<DataSet> dataSets = new ArrayList<>();
    private Knowledge knowledge;

    public TimeSeriesSemSimulation(RandomGraph randomGraph) {
        if (randomGraph == null) {
            throw new NullPointerException();
        }
        this.randomGraph = randomGraph;
    }

    public static void topToBottomLayout(TimeLagGraph graph) {

        final int xStart = 65;
        final int yStart = 50;
        final int xSpace = 100;
        final int ySpace = 100;
        List<Node> lag0Nodes = graph.getLag0Nodes();

        lag0Nodes.sort(Comparator.comparingInt(Node::getCenterX));

        int x = xStart - xSpace;

        for (Node node : lag0Nodes) {
            x += xSpace;
            int y = yStart - ySpace;
            TimeLagGraph.NodeId id = graph.getNodeId(node);

            for (int lag = graph.getMaxLag(); lag >= 0; lag--) {
                y += ySpace;
                Node _node = graph.getNode(id.getName(), lag);

                if (_node == null) {
                    System.out.println("Couldn't find " + _node);
                    continue;
                }

                _node.setCenterX(x);
                _node.setCenterY(y);
            }
        }
    }

    @Override
    public void createData(Parameters parameters, boolean newModel) {
//        if (parameters.getLong(Params.SEED) != -1L) {
//            RandomUtil.getInstance().setSeed(parameters.getLong(Params.SEED));
//        }

        this.dataSets = new ArrayList<>();
        this.graphs = new ArrayList<>();

        Graph graph = this.randomGraph.createGraph(parameters);
        graph = TsUtils.graphToLagGraph(graph, parameters.getInt(Params.NUM_LAGS));
        TimeSeriesSemSimulation.topToBottomLayout((TimeLagGraph) graph);
        this.knowledge = TsUtils.getKnowledge(graph);

        for (int i = 0; i < parameters.getInt(Params.NUM_RUNS); i++) {
            if (parameters.getBoolean(Params.DIFFERENT_GRAPHS) && i > 0) {
                graph = this.randomGraph.createGraph(parameters);
                graph = TsUtils.graphToLagGraph(graph, 2);
                TimeSeriesSemSimulation.topToBottomLayout((TimeLagGraph) graph);
            }

            this.graphs.add(graph);

            SemPm pm = new SemPm(graph);
            SemIm im = new SemIm(pm, parameters);

            int sampleSize = parameters.getInt(Params.SAMPLE_SIZE);

            boolean saveLatentVars = parameters.getBoolean(Params.SAVE_LATENT_VARS);
            DataSet dataSet = im.simulateData(sampleSize, saveLatentVars);

            int numLags = ((TimeLagGraph) graph).getMaxLag();

            dataSet = TsUtils.createLagData(dataSet, numLags);

            if (parameters.getDouble(Params.PROB_REMOVE_COLUMN) > 0) {
                double aDouble = parameters.getDouble(Params.PROB_REMOVE_COLUMN);
                dataSet = DataTransforms.removeRandomColumns(dataSet, aDouble);
            }

            dataSet.setName("" + (i + 1));
            dataSet.setKnowledge(this.knowledge.copy());
            this.dataSets.add(dataSet);

        }
    }

    @Override
    public DataModel getDataModel(int index) {
        return this.dataSets.get(index);
    }

    @Override
    public Graph getTrueGraph(int index) {
        return this.graphs.get(index);
    }

    @Override
    public String getDescription() {
        return "Linear, Gaussian Dynamic SEM (1-lag SVAR) simulation";
    }

    @Override
    public List<String> getParameters() {
        List<String> parameters = new ArrayList<>();

        parameters.add(Params.NUM_LAGS);

        if (!(this.randomGraph instanceof SingleGraph)) {
            parameters.addAll(this.randomGraph.getParameters());
        }

        parameters.addAll(SemIm.getParameterNames());

        parameters.add(Params.STANDARDIZE);
        parameters.add(Params.MEASUREMENT_VARIANCE);
        parameters.add(Params.NUM_RUNS);
        parameters.add(Params.PROB_REMOVE_COLUMN);
        parameters.add(Params.DIFFERENT_GRAPHS);
        parameters.add(Params.SAMPLE_SIZE);
        parameters.add(Params.SAVE_LATENT_VARS);
//        parameters.add(Params.SEED);

        return parameters;

    }

    @Override
    public int getNumDataModels() {
        return this.dataSets.size();
    }

    @Override
    public DataType getDataType() {
        return DataType.Continuous;
    }

    @Override
    public Knowledge getKnowledge() {
        return this.knowledge;
    }

    @Override
    public void setKnowledge(Knowledge knowledge) {
        this.knowledge = new Knowledge((Knowledge) knowledge);
    }

}
