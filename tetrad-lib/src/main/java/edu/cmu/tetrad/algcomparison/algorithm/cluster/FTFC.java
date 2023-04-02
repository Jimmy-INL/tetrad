package edu.cmu.tetrad.algcomparison.algorithm.cluster;

import edu.cmu.tetrad.algcomparison.algorithm.Algorithm;
import edu.cmu.tetrad.algcomparison.utils.HasKnowledge;
import edu.cmu.tetrad.annotation.AlgType;
import edu.cmu.tetrad.annotation.Bootstrapping;
import edu.cmu.tetrad.data.*;
import edu.cmu.tetrad.graph.EdgeListGraph;
import edu.cmu.tetrad.graph.Graph;
import edu.cmu.tetrad.search.FindTwoFactorClusters;
import edu.cmu.tetrad.search.SearchGraphUtils;
import edu.cmu.tetrad.util.Parameters;
import edu.cmu.tetrad.util.Params;
import edu.pitt.dbmi.algo.resampling.GeneralResamplingTest;

import java.util.ArrayList;
import java.util.List;

/**
 * FTFC.
 *
 * @author jdramsey
 */
@edu.cmu.tetrad.annotation.Algorithm(
        name = "FTFC",
        command = "ftfc",
        algoType = AlgType.search_for_structure_over_latents
)
@Bootstrapping
public class FTFC implements Algorithm, HasKnowledge, CLUSTER_ALGORITHM {

    static final long serialVersionUID = 23L;
    private Knowledge knowledge = new Knowledge();

    public FTFC() {
    }

    @Override
    public Graph search(DataModel dataSet, Parameters parameters) {
        if (parameters.getInt(Params.NUMBER_RESAMPLING) < 1) {
            ICovarianceMatrix cov;

            if (dataSet instanceof DataSet) {
                cov = SimpleDataLoader.getCovarianceMatrix(dataSet);
            } else if (dataSet instanceof ICovarianceMatrix) {
                cov = (ICovarianceMatrix) dataSet;
            } else {
                throw new IllegalArgumentException("Expected a dataset or a covariance matrix.");
            }

            double alpha = parameters.getDouble(Params.ALPHA);

            boolean gap = parameters.getBoolean(Params.USE_GAP, true);
            FindTwoFactorClusters.Algorithm algorithm;

            if (gap) {
                algorithm = FindTwoFactorClusters.Algorithm.GAP;
            } else {
                algorithm = FindTwoFactorClusters.Algorithm.SAG;
            }

            FindTwoFactorClusters search
                    = new FindTwoFactorClusters(cov, algorithm, alpha);
            search.setVerbose(parameters.getBoolean(Params.VERBOSE));

            return search.search();
        } else {
            FTFC algorithm = new FTFC();

            DataSet data = (DataSet) dataSet;
            GeneralResamplingTest search = new GeneralResamplingTest(data, algorithm, parameters.getInt(Params.NUMBER_RESAMPLING), parameters.getDouble(Params.PERCENT_RESAMPLE_SIZE), parameters.getBoolean(Params.RESAMPLING_WITH_REPLACEMENT), parameters.getInt(Params.RESAMPLING_ENSEMBLE), parameters.getBoolean(Params.ADD_ORIGINAL_DATASET));
            search.setKnowledge(this.knowledge);

            search.setParameters(parameters);
            search.setVerbose(parameters.getBoolean(Params.VERBOSE));
            return search.search();
        }
    }

    @Override
    public Graph getComparisonGraph(Graph graph) {
        return SearchGraphUtils.cpdagForDag(new EdgeListGraph(graph));
    }

    @Override
    public String getDescription() {
        return "FTFC (Find Two Factor Clusters)";
    }

    @Override
    public DataType getDataType() {
        return DataType.Continuous;
    }

    @Override
    public List<String> getParameters() {
        List<String> parameters = new ArrayList<>();
        parameters.add(Params.ALPHA);
        parameters.add(Params.USE_WISHART);
        parameters.add(Params.USE_GAP);
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

}
