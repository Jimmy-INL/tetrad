package edu.cmu.tetrad.algcomparison.algorithm.multi;

import edu.cmu.tetrad.algcomparison.algorithm.Algorithm;
import edu.cmu.tetrad.algcomparison.algorithm.MultiDataSetAlgorithm;
import edu.cmu.tetrad.algcomparison.independence.IndependenceWrapper;
import edu.cmu.tetrad.algcomparison.score.ScoreWrapper;
import edu.cmu.tetrad.algcomparison.utils.HasKnowledge;
import edu.cmu.tetrad.annotation.Bootstrapping;
import edu.cmu.tetrad.data.*;
import edu.cmu.tetrad.graph.EdgeListGraph;
import edu.cmu.tetrad.graph.Graph;
import edu.cmu.tetrad.search.utils.GraphSearchUtils;
import edu.cmu.tetrad.util.Parameters;
import edu.cmu.tetrad.util.Params;
import edu.pitt.dbmi.algo.resampling.GeneralResamplingTest;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Requires that the parameter 'randomSelectionSize' be set to indicate how many datasets should be taken at a time
 * (randomly). This cannot given multiple values.
 *
 * @author josephramsey
 */
@Bootstrapping
public class FgesConcatenated implements MultiDataSetAlgorithm, HasKnowledge {
    static final long serialVersionUID = 23L;
    private final ScoreWrapper score;
    private Knowledge knowledge = new Knowledge();
    private Algorithm externalGraph;
    private boolean compareToTrue;

    public FgesConcatenated(ScoreWrapper score) {
        this.score = score;
    }

    public FgesConcatenated(ScoreWrapper score, boolean compareToTrue) {
        this.score = score;
        this.compareToTrue = compareToTrue;
    }

    public FgesConcatenated(ScoreWrapper score, Algorithm externalGraph) {
        this.score = score;
        this.externalGraph = externalGraph;
    }

    @Override
    public Graph search(List<DataModel> dataModels, Parameters parameters) {
        if (parameters.getInt(Params.NUMBER_RESAMPLING) < 1) {
            List<DataSet> dataSets = new ArrayList<>();

            for (DataModel dataModel : dataModels) {
                dataSets.add((DataSet) dataModel);
            }

            DataSet dataSet = DataUtils.concatenate(dataSets);

            Graph initial = null;
            if (this.externalGraph != null) {

                initial = this.externalGraph.search(dataSet, parameters);
            }

            edu.cmu.tetrad.search.Fges search = new edu.cmu.tetrad.search.Fges(this.score.getScore(dataSet, parameters));
            search.setKnowledge(this.knowledge);
            search.setVerbose(parameters.getBoolean(Params.VERBOSE));
            search.setMaxDegree(parameters.getInt(Params.MAX_DEGREE));

            Object obj = parameters.get("printStedu.cmream");
            if (obj instanceof PrintStream) {
                search.setOut((PrintStream) obj);
            }

            if (initial != null) {
                search.setBoundGraph(initial);
            }

            return search.search();
        } else {
            FgesConcatenated fgesConcatenated = new FgesConcatenated(this.score, this.externalGraph);
            fgesConcatenated.setCompareToTrue(this.compareToTrue);

            List<DataSet> datasets = new ArrayList<>();

            for (DataModel dataModel : dataModels) {
                datasets.add((DataSet) dataModel);
            }
            GeneralResamplingTest search = new GeneralResamplingTest(datasets,
                    fgesConcatenated,
                    parameters.getInt(Params.NUMBER_RESAMPLING),
                    parameters.getDouble(Params.PERCENT_RESAMPLE_SIZE),
                    parameters.getBoolean(Params.RESAMPLING_WITH_REPLACEMENT), parameters.getInt(Params.RESAMPLING_ENSEMBLE), parameters.getBoolean(Params.ADD_ORIGINAL_DATASET));
            search.setKnowledge(this.knowledge);
            search.setScoreWrapper(score);

            search.setParameters(parameters);
            search.setVerbose(parameters.getBoolean(Params.VERBOSE));
            return search.search();
        }
    }

    @Override
    public void setScoreWrapper(ScoreWrapper score) {
        // Not used.
    }

    @Override
    public void setIndTestWrapper(IndependenceWrapper test) {
        // Not used.
    }

    @Override
    public Graph search(DataModel dataSet, Parameters parameters) {
        if (parameters.getInt(Params.NUMBER_RESAMPLING) < 1) {
            return search(Collections.singletonList(SimpleDataLoader.getContinuousDataSet(dataSet)), parameters);
        } else {
            FgesConcatenated fgesConcatenated = new FgesConcatenated(this.score, this.externalGraph);
            fgesConcatenated.setCompareToTrue(this.compareToTrue);

            List<DataSet> dataSets = Collections.singletonList(SimpleDataLoader.getContinuousDataSet(dataSet));
            GeneralResamplingTest search = new GeneralResamplingTest(dataSets,
                    fgesConcatenated,
                    parameters.getInt(Params.NUMBER_RESAMPLING),
                    parameters.getDouble(Params.PERCENT_RESAMPLE_SIZE),
                    parameters.getBoolean(Params.RESAMPLING_WITH_REPLACEMENT), parameters.getInt(Params.RESAMPLING_ENSEMBLE), parameters.getBoolean(Params.ADD_ORIGINAL_DATASET));
            search.setKnowledge(this.knowledge);
            search.setScoreWrapper(score);

            search.setParameters(parameters);
            search.setVerbose(parameters.getBoolean(Params.VERBOSE));
            return search.search();
        }
    }

    @Override
    public Graph getComparisonGraph(Graph graph) {
        if (this.compareToTrue) {
            return new EdgeListGraph(graph);
        } else {
            return GraphSearchUtils.cpdagForDag(new EdgeListGraph(graph));
        }
    }

    @Override
    public String getDescription() {
        return "FGES (Fast Greedy Equivalence Search) on concatenated data using " + this.score.getDescription();
    }

    @Override
    public DataType getDataType() {
        return DataType.Continuous;
    }

    @Override
    public List<String> getParameters() {
        List<String> parameters = new ArrayList<>();
        parameters.add(Params.FAITHFULNESS_ASSUMED);
        parameters.add(Params.MAX_DEGREE);

        parameters.add(Params.NUM_RUNS);
        parameters.add(Params.RANDOM_SELECTION_SIZE);

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

    /**
     * @param compareToTrue true if the result should be compared to the true graph, false if to the CPDAG of the true
     *                      graph.
     */
    public void setCompareToTrue(boolean compareToTrue) {
        this.compareToTrue = compareToTrue;
    }
}
