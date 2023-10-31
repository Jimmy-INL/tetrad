package edu.cmu.tetrad.algcomparison.score;

import edu.cmu.tetrad.data.DataModel;
import edu.cmu.tetrad.data.DataSet;
import edu.cmu.tetrad.data.DataType;
import edu.cmu.tetrad.graph.Node;
import edu.cmu.tetrad.search.score.IndTestScore;
import edu.cmu.tetrad.search.score.Score;
import edu.cmu.tetrad.search.test.IndTestFisherZ;
import edu.cmu.tetrad.util.Parameters;
import edu.cmu.tetrad.util.Params;

import java.util.ArrayList;
import java.util.List;

/**
 * Wrapper for Fisher Z test.
 *
 * @author josephramsey
 */
//@edu.cmu.tetrad.annotation.Score(
//        name = "Fisher Z Score",
//        command = "fisher-z",
//        dataType = DataType.Continuous
//)
public class FisherZScore implements ScoreWrapper {

    private static final long serialVersionUID = 23L;
    double alpha = 0.001;
    private DataModel dataSet;

    @Override
    public Score getScore(DataModel dataSet, Parameters parameters) {
        this.dataSet = dataSet;
        double alpha = parameters.getDouble(Params.ALPHA);
        this.alpha = alpha;
        IndTestFisherZ test = new IndTestFisherZ((DataSet) dataSet, alpha);
        return new IndTestScore(test);
    }

    @Override
    public String getDescription() {
        return "Fisher Z Score";
    }

    @Override
    public DataType getDataType() {
        return DataType.Continuous;
    }

    @Override
    public List<String> getParameters() {
        List<String> parameters = new ArrayList<>();
        parameters.add(Params.ALPHA);
        return parameters;
    }

    @Override
    public Node getVariable(String name) {
        return this.dataSet.getVariable(name);
    }
}
