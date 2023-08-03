package edu.cmu.tetrad.algcomparison.score;

import edu.cmu.tetrad.data.DataModel;
import edu.cmu.tetrad.data.DataType;
import edu.cmu.tetrad.data.SimpleDataLoader;
import edu.cmu.tetrad.graph.Node;
import edu.cmu.tetrad.search.score.IndTestScore;
import edu.cmu.tetrad.search.score.Score;
import edu.cmu.tetrad.search.test.ConditionalCorrelationIndependence;
import edu.cmu.tetrad.search.test.IndTestConditionalCorrelation;
import edu.cmu.tetrad.util.Parameters;
import edu.cmu.tetrad.util.Params;

import java.util.ArrayList;
import java.util.List;

///**
// * Wrapper for CCI Score.
// *
// * @author josephramsey
// */
//@edu.cmu.tetrad.annotation.Score(name = "CCI-Score (Conditional Correlation Independence Score)", command = "cci-score", dataType = DataType.Continuous)
//@General
//@Experimental
public class CciScore implements ScoreWrapper {

    static final long serialVersionUID = 23L;
    private DataModel dataSet;

    @Override
    public Score getScore(DataModel dataSet, Parameters parameters) {
        this.dataSet = dataSet;
        IndTestConditionalCorrelation cci = new IndTestConditionalCorrelation(SimpleDataLoader.getContinuousDataSet(dataSet),
                parameters.getDouble(Params.CCI_SCORE_ALPHA));
        if (parameters.getInt(Params.KERNEL_TYPE) == 1) {
            cci.setKernel(ConditionalCorrelationIndependence.Kernel.Gaussian);
        } else if (parameters.getInt(Params.KERNEL_TYPE) == 2) {
            cci.setKernel(ConditionalCorrelationIndependence.Kernel.Epinechnikov);
        } else {
            throw new IllegalStateException("Kernel not configured.");
        }
        cci.setNumFunctions(parameters.getInt(Params.NUM_BASIS_FUNCTIONS));
        cci.setKernelMultiplier(parameters.getDouble(Params.KERNEL_MULTIPLIER));
        cci.setKernelRegressionSampleSize(parameters.getInt(Params.KERNEL_REGRESSION_SAMPLE_SIZE));

        if (parameters.getInt(Params.BASIS_TYPE) == 1) {
            cci.setBasis(ConditionalCorrelationIndependence.Basis.Polynomial);
        } else if (parameters.getInt(Params.BASIS_TYPE) == 2) {
            cci.setBasis(ConditionalCorrelationIndependence.Basis.Cosine);
        } else {
            throw new IllegalStateException("Basis not configured.");
        }

        return new IndTestScore(cci);
    }

    @Override
    public String getDescription() {
        return "CCI Score";
    }

    @Override
    public DataType getDataType() {
        return DataType.Continuous;
    }

    @Override
    public List<String> getParameters() {
        List<String> parameters = new ArrayList<>();
        parameters.add(Params.CCI_SCORE_ALPHA);
        parameters.add(Params.NUM_BASIS_FUNCTIONS);
        parameters.add(Params.KERNEL_TYPE);
        parameters.add(Params.KERNEL_MULTIPLIER);
        parameters.add(Params.BASIS_TYPE);
//        parameters.add("fastFDR");
        parameters.add(Params.KERNEL_REGRESSION_SAMPLE_SIZE);
//        parameters.add("numDependenceSpotChecks");
        return parameters;
    }

    @Override
    public Node getVariable(String name) {
        return this.dataSet.getVariable(name);
    }

}