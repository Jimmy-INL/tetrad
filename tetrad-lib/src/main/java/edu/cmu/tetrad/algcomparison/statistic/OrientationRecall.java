package edu.cmu.tetrad.algcomparison.statistic;

import edu.cmu.tetrad.algcomparison.statistic.utils.OrientationConfusion;
import edu.cmu.tetrad.data.DataModel;
import edu.cmu.tetrad.graph.Graph;

/**
 * The orientation recall.
 *
 * @author bryanandrews, josephramsey
 */
public class OrientationRecall implements Statistic {
    static final long serialVersionUID = 23L;

    @Override
    public String getAbbreviation() {
        return "OR";
    }

    @Override
    public String getDescription() {
        return "Orientation Recall";
    }

    @Override
    public double getValue(Graph trueGraph, Graph estGraph, DataModel dataModel) {
        OrientationConfusion oriConfusion = new OrientationConfusion(trueGraph, estGraph);
        int oriTp = oriConfusion.getTp();
        int oriFn = oriConfusion.getFn();
        return oriTp / (double) (oriTp + oriFn);
    }

    @Override
    public double getNormValue(double value) {
        return value;
    }
}
