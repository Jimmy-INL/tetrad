package edu.cmu.tetrad.algcomparison.algorithm.oracle.cpdag;

import edu.cmu.tetrad.algcomparison.algorithm.Algorithm;
import edu.cmu.tetrad.algcomparison.algorithm.ReturnsBootstrapGraphs;
import edu.cmu.tetrad.algcomparison.score.ScoreWrapper;
import edu.cmu.tetrad.algcomparison.utils.HasKnowledge;
import edu.cmu.tetrad.algcomparison.utils.UsesScoreWrapper;
import edu.cmu.tetrad.annotation.AlgType;
import edu.cmu.tetrad.annotation.Bootstrapping;
import edu.cmu.tetrad.data.DataModel;
import edu.cmu.tetrad.data.DataSet;
import edu.cmu.tetrad.data.DataType;
import edu.cmu.tetrad.data.Knowledge;
import edu.cmu.tetrad.graph.EdgeListGraph;
import edu.cmu.tetrad.graph.Graph;
import edu.cmu.tetrad.search.PermutationSearch;
import edu.cmu.tetrad.search.score.Score;
import edu.cmu.tetrad.search.utils.TsUtils;
import edu.cmu.tetrad.util.Parameters;
import edu.cmu.tetrad.util.Params;
import edu.pitt.dbmi.algo.resampling.GeneralResamplingTest;

import java.util.ArrayList;
import java.util.List;

import static edu.cmu.tetrad.search.utils.LogUtilsSearch.stampWithBic;

/**
 * BOSS-LiNGAM algorithm. This runs the BOSS algorithm to find the CPDAG and then orients the undirected edges using the
 * LiNGAM algorithm.
 *
 * @author josephramsey
 */
@edu.cmu.tetrad.annotation.Algorithm(name = "BOSS-LiNGAM", command = "boss-lingam", algoType = AlgType.forbid_latent_common_causes)
@Bootstrapping
public class BossLingam implements Algorithm, HasKnowledge, UsesScoreWrapper, ReturnsBootstrapGraphs {
    private static final long serialVersionUID = 23L;
    private ScoreWrapper score;
    private Knowledge knowledge = new Knowledge();
    private List<Graph> bootstrapGraphs = new ArrayList<>();

    /**
     * Constructs a new BOSS-LiNGAM algorithm.
     */
    public BossLingam() {
    }

    /**
     * Constructs a new BOSS-LiNGAM algorithm with the given score.
     *
     * @param scoreWrapper the score to use
     */
    public BossLingam(ScoreWrapper scoreWrapper) {
        this.score = scoreWrapper;
    }

    /**
     * Runs the BOSS-LiNGAM algorithm.
     *
     * @param dataModel  The data set to run to the search on.
     * @param parameters The paramters of the search.
     * @return The graph.
     */
    @Override
    public Graph search(DataModel dataModel, Parameters parameters) {
        if (parameters.getInt(Params.NUMBER_RESAMPLING) < 1) {
            if (parameters.getInt(Params.TIME_LAG) > 0) {
                DataSet dataSet = (DataSet) dataModel;
                DataSet timeSeries = TsUtils.createLagData(dataSet, parameters.getInt(Params.TIME_LAG));
                if (dataSet.getName() != null) {
                    timeSeries.setName(dataSet.getName());
                }
                dataModel = timeSeries;
                knowledge = timeSeries.getKnowledge();
            }

            Score score = this.score.getScore(dataModel, parameters);

            edu.cmu.tetrad.search.Boss boss = new edu.cmu.tetrad.search.Boss(score);
            boss.setUseBes(parameters.getBoolean(Params.USE_BES));
            boss.setNumStarts(parameters.getInt(Params.NUM_STARTS));
            boss.setNumThreads(parameters.getInt(Params.NUM_THREADS));
            boss.setUseDataOrder(parameters.getBoolean(Params.USE_DATA_ORDER));
            boss.setVerbose(parameters.getBoolean(Params.VERBOSE));
            PermutationSearch permutationSearch = new PermutationSearch(boss);
            permutationSearch.setSeed(parameters.getLong(Params.SEED));
            permutationSearch.setKnowledge(this.knowledge);

            Graph cpdag = permutationSearch.search();

            edu.cmu.tetrad.search.BossLingam bossLingam = new edu.cmu.tetrad.search.BossLingam(cpdag, (DataSet) dataModel);
            Graph graph = bossLingam.search();

            stampWithBic(graph, dataModel);
            return graph;
        } else {
            BossLingam pcAll = new BossLingam(this.score);

            DataSet data = (DataSet) dataModel;
            GeneralResamplingTest search = new GeneralResamplingTest(data, pcAll, parameters.getInt(Params.NUMBER_RESAMPLING), parameters.getDouble(Params.PERCENT_RESAMPLE_SIZE), parameters.getBoolean(Params.RESAMPLING_WITH_REPLACEMENT), parameters.getInt(Params.RESAMPLING_ENSEMBLE), parameters.getBoolean(Params.ADD_ORIGINAL_DATASET));
            search.setKnowledge(this.knowledge);

            search.setParameters(parameters);
            search.setVerbose(parameters.getBoolean(Params.VERBOSE));
            Graph graph = search.search();
            if (parameters.getBoolean(Params.SAVE_BOOTSTRAP_GRAPHS)) this.bootstrapGraphs = search.getGraphs();
            return graph;
        }
    }

    /**
     * Returns the comparison graph.
     *
     * @param graph The true directed graph, if there is one.
     * @return The comparison graph.
     */
    @Override
    public Graph getComparisonGraph(Graph graph) {
        return new EdgeListGraph(graph);
    }

    /**
     * Returns the description of the algorithm.
     *
     * @return The description of the algorithm.
     */
    @Override
    public String getDescription() {
        return "BOSS-LiNGAM using " + this.score.getDescription();
    }

    /**
     * Returns the data type that the algorithm can handle.
     *
     * @return The data type that the algorithm can handle.
     */
    @Override
    public DataType getDataType() {
        return this.score.getDataType();
    }

    /**
     * Returns the parameters for the algorithm.
     *
     * @return The parameters for the algorithm.
     */
    @Override
    public List<String> getParameters() {
        List<String> parameters = new ArrayList<>();
        parameters.add(Params.USE_BES);
        parameters.add(Params.NUM_STARTS);
        parameters.add(Params.TIME_LAG);
        parameters.add(Params.NUM_THREADS);
        parameters.add(Params.USE_DATA_ORDER);
        parameters.add(Params.SEED);
        parameters.add(Params.VERBOSE);
        return parameters;
    }

    /**
     * Returns the knowledge.
     *
     * @return The knowledge.
     */
    @Override
    public Knowledge getKnowledge() {
        return this.knowledge;
    }

    /**
     * Sets the knowledge.
     *
     * @param knowledge a knowledge object.
     */
    @Override
    public void setKnowledge(Knowledge knowledge) {
        this.knowledge = new Knowledge(knowledge);
    }

    /**
     * Returns the score wrapper.
     *
     * @return The score wrapper.
     */
    @Override
    public ScoreWrapper getScoreWrapper() {
        return this.score;
    }

    /**
     * Sets the score wrapper.
     *
     * @param score the score wrapper.
     */
    @Override
    public void setScoreWrapper(ScoreWrapper score) {
        this.score = score;
    }

    /**
     * Returns the bootstrap graphs.
     *
     * @return The bootstrap graphs.
     */
    @Override
    public List<Graph> getBootstrapGraphs() {
        return this.bootstrapGraphs;
    }
}