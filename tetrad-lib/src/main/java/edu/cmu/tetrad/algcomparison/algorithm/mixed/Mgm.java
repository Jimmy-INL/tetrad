package edu.cmu.tetrad.algcomparison.algorithm.mixed;

import edu.cmu.tetrad.algcomparison.algorithm.Algorithm;
import edu.cmu.tetrad.annotation.AlgType;
import edu.cmu.tetrad.annotation.Bootstrapping;
import edu.cmu.tetrad.data.*;
import edu.cmu.tetrad.graph.Graph;
import edu.cmu.tetrad.graph.GraphUtils;
import edu.cmu.tetrad.graph.Node;
import edu.cmu.tetrad.util.Parameters;
import edu.cmu.tetrad.util.Params;
import edu.pitt.dbmi.algo.resampling.GeneralResamplingTest;

import java.util.ArrayList;
import java.util.List;

/**
 * MGM.
 *
 * @author josephramsey
 * @version $Id: $Id
 */
@edu.cmu.tetrad.annotation.Algorithm(
        name = "MGM",
        command = "mgm",
        algoType = AlgType.produce_undirected_graphs
)
@Bootstrapping
public class Mgm implements Algorithm {

    private static final long serialVersionUID = 23L;

    /**
     * <p>Constructor for Mgm.</p>
     */
    public Mgm() {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Graph search(DataModel ds, Parameters parameters) {
        if (!(ds instanceof DataSet _data)) {
            throw new IllegalArgumentException("Expecting tabular data for MGM.");
        }

        for (int j = 0; j < _data.getNumColumns(); j++) {
            for (int i = 0; i < _data.getNumRows(); i++) {
                if (ds.getVariables().get(j) instanceof ContinuousVariable) {
                    if (Double.isNaN(_data.getDouble(i, j))) {
                        throw new IllegalArgumentException("Please remove or impute missing values.");
                    }
                } else if (ds.getVariables().get(j) instanceof DiscreteVariable) {
                    if (_data.getDouble(i, j) == -99) {
                        throw new IllegalArgumentException("Please remove or impute missing values.");
                    }
                }
            }
        }

        // Notify the user that you need at least one continuous and one discrete variable to run MGM
        List<Node> variables = ds.getVariables();
        boolean hasContinuous = false;
        boolean hasDiscrete = false;

        for (Node node : variables) {
            if (node instanceof ContinuousVariable) {
                hasContinuous = true;
            }

            if (node instanceof DiscreteVariable) {
                hasDiscrete = true;
            }
        }

        if (!hasContinuous || !hasDiscrete) {
            throw new IllegalArgumentException("You need at least one continuous and one discrete variable to run MGM.");
        }

        if (parameters.getInt(Params.NUMBER_RESAMPLING) < 1) {
            DataSet _ds = SimpleDataLoader.getMixedDataSet(ds);

            double mgmParam1 = parameters.getDouble(Params.MGM_PARAM1);
            double mgmParam2 = parameters.getDouble(Params.MGM_PARAM2);
            double mgmParam3 = parameters.getDouble(Params.MGM_PARAM3);

            double[] lambda = {
                    mgmParam1,
                    mgmParam2,
                    mgmParam3
            };

            edu.pitt.csb.mgm.Mgm m = new edu.pitt.csb.mgm.Mgm(_ds, lambda);

            return m.search();
        } else {
            Mgm algorithm = new Mgm();

            DataSet data = (DataSet) ds;
            GeneralResamplingTest search = new GeneralResamplingTest(data, algorithm, parameters.getInt(Params.NUMBER_RESAMPLING), parameters.getDouble(Params.PERCENT_RESAMPLE_SIZE), parameters.getBoolean(Params.RESAMPLING_WITH_REPLACEMENT), parameters.getInt(Params.RESAMPLING_ENSEMBLE), parameters.getBoolean(Params.ADD_ORIGINAL_DATASET));

            search.setParameters(parameters);
            search.setVerbose(parameters.getBoolean(Params.VERBOSE));
            return search.search();
        }
    }

    // Need to marry the parents on this.

    /**
     * {@inheritDoc}
     */
    @Override
    public Graph getComparisonGraph(Graph graph) {
        return GraphUtils.undirectedGraph(graph);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getDescription() {
        return "Returns the output of the MGM (Mixed Graphical Model) algorithm (a Markov random field)";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public DataType getDataType() {
        return DataType.Mixed;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<String> getParameters() {
        List<String> parameters = new ArrayList<>();
        parameters.add(Params.MGM_PARAM1);
        parameters.add(Params.MGM_PARAM2);
        parameters.add(Params.MGM_PARAM3);
        parameters.add(Params.VERBOSE);
        return parameters;
    }
}
