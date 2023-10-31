package edu.cmu.tetrad.algcomparison.score;

import edu.cmu.tetrad.annotation.Experimental;
import edu.cmu.tetrad.data.DataModel;
import edu.cmu.tetrad.data.DataType;
import edu.cmu.tetrad.data.SimpleDataLoader;
import edu.cmu.tetrad.graph.Node;
import edu.cmu.tetrad.search.score.MvpScore;
import edu.cmu.tetrad.search.score.Score;
import edu.cmu.tetrad.util.Parameters;
import edu.cmu.tetrad.util.Params;

import java.util.ArrayList;
import java.util.List;

/**
 * Wrapper for MVP BIC Score.
 *
 * @author Bryan Andrews
 */
@Experimental
@edu.cmu.tetrad.annotation.Score(
        name = "Mixed Variable Polynomial BIC Score",
        command = "mvp-bic-score",
        dataType = DataType.Mixed
)
public class MVPBicScore implements ScoreWrapper {

    private static final long serialVersionUID = 23L;

    @Override
    public Score getScore(DataModel dataSet, Parameters parameters) {
        return new MvpScore(SimpleDataLoader.getMixedDataSet(dataSet),
                parameters.getDouble("structurePrior", 0),
                parameters.getInt("fDegree", -1),
                parameters.getInt("discretize", 0) > 0);
    }

    @Override
    public String getDescription() {
        return "Mixed Variable Polynomial BIC Score";
    }

    @Override
    public DataType getDataType() {
        return DataType.Mixed;
    }

    @Override
    public List<String> getParameters() {
        List<String> parameters = new ArrayList<>();
        parameters.add(Params.STRUCTURE_PRIOR);
        parameters.add("fDegree");
        parameters.add(Params.DISCRETIZE);
        return parameters;
    }

    @Override
    public Node getVariable(String name) {
        return null;
    }
}
