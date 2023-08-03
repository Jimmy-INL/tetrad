package edu.cmu.tetrad.algcomparison.statistic;

import edu.cmu.tetrad.data.DataModel;
import edu.cmu.tetrad.data.DataSet;
import edu.cmu.tetrad.graph.Graph;
import edu.cmu.tetrad.search.score.SemBicScorer;
import edu.cmu.tetrad.search.utils.GraphSearchUtils;

import static org.apache.commons.math3.util.FastMath.abs;
import static org.apache.commons.math3.util.FastMath.tanh;

/**
 * Difference between the true and estiamted BIC scores.
 *
 * @author josephramsey
 */
public class BicDiffPerRecord implements Statistic {
    static final long serialVersionUID = 23L;
    private boolean precomputeCovariances = true;

    @Override
    public String getAbbreviation() {
        return "BicDiffPerRecord";
    }

    @Override
    public String getDescription() {
        return "Difference between the true and estimated BIC scores, " +
                "divided by the sample size";
    }

    @Override
    public double getValue(Graph trueGraph, Graph estGraph, DataModel dataModel) {
        double _true = SemBicScorer.scoreDag(GraphSearchUtils.dagFromCPDAG(trueGraph), dataModel, precomputeCovariances);
        double est = SemBicScorer.scoreDag(GraphSearchUtils.dagFromCPDAG(estGraph), dataModel, precomputeCovariances);
        if (abs(_true) < 0.0001) _true = 0.0;
        if (abs(est) < 0.0001) est = 0.0;
        return (_true - est) / ((DataSet) dataModel).getNumRows();
    }

    @Override
    public double getNormValue(double value) {
        return tanh(value / 1e6);
    }

    public void setPrecomputeCovariances(boolean precomputeCovariances) {
        this.precomputeCovariances = precomputeCovariances;
    }
}

