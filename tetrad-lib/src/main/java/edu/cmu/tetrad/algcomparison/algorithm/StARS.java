package edu.cmu.tetrad.algcomparison.algorithm;

import edu.cmu.tetrad.data.BootstrapSampler;
import edu.cmu.tetrad.data.DataModel;
import edu.cmu.tetrad.data.DataSet;
import edu.cmu.tetrad.data.DataType;
import edu.cmu.tetrad.graph.Graph;
import edu.cmu.tetrad.graph.GraphUtils;
import edu.cmu.tetrad.graph.Node;
import edu.cmu.tetrad.util.ForkJoinPoolInstance;
import edu.cmu.tetrad.util.Parameters;
import org.apache.commons.math3.util.FastMath;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.RecursiveAction;

/**
 * StARS
 *
 * @author josephramsey
 */
public class StARS implements Algorithm {

    static final long serialVersionUID = 23L;
    private final double low;
    private final double high;
    private final String parameter;
    private final Algorithm algorithm;

    public StARS(Algorithm algorithm, String parameter, double low, double high) {
        if (low >= high) {
            throw new IllegalArgumentException("Must have low < high");
        }
        this.algorithm = algorithm;
        this.low = low;
        this.high = high;
        this.parameter = parameter;
    }

    private static double getD(Parameters params, String paramName, double paramValue, List<DataSet> samples,
                               Algorithm algorithm) {
        params.set(paramName, paramValue);

        List<Graph> graphs = new ArrayList<>();

        ForkJoinPool pool = ForkJoinPoolInstance.getInstance().getPool();

        class StabilityAction extends RecursiveAction {

            private final int chunk;
            private final int from;
            private final int to;

            private StabilityAction(int chunk, int from, int to) {
                this.chunk = chunk;
                this.from = from;
                this.to = to;
            }

            @Override
            protected void compute() {
                if (this.to - this.from <= this.chunk) {
                    for (int s = this.from; s < this.to; s++) {
                        Graph e = algorithm.search(samples.get(s), params);
                        e = GraphUtils.replaceNodes(e, samples.get(0).getVariables());
                        graphs.add(e);
                    }
                } else {
                    int mid = (this.to + this.from) / 2;

                    StabilityAction left = new StabilityAction(this.chunk, this.from, mid);
                    StabilityAction right = new StabilityAction(this.chunk, mid, this.to);

                    left.fork();
                    right.compute();
                    left.join();
                }
            }
        }

        final int chunk = 1;

        pool.invoke(new StabilityAction(chunk, 0, samples.size()));

        int p = samples.get(0).getNumColumns();
        List<Node> nodes = graphs.get(0).getNodes();

        double D = 0.0;
        int count = 0;

        for (int i = 0; i < p; i++) {
            for (int j = i + 1; j < p; j++) {
                double theta = 0.0;
                Node x = nodes.get(i);
                Node y = nodes.get(j);

                for (Graph graph : graphs) {
                    if (graph.isAdjacentTo(x, y)) {
                        theta += 1.0;
                    }
                }

                theta /= graphs.size();
                double xsi = 2 * theta * (1.0 - theta);

//                if (xsi != 0){
                D += xsi;
                count++;
//                }
            }
        }

        D /= count;
        return D;
    }

    private static double getValue(double value, Parameters parameters) {
        if (parameters.getBoolean("logScale")) {
            return FastMath.round(FastMath.pow(10.0, value) * 1000000000.0) / 1000000000.0;
        } else {
            return FastMath.round(value * 1000000000.0) / 1000000000.0;
        }
    }

    @Override
    public Graph search(DataModel dataSet, Parameters parameters) {
        DataSet _dataSet;

        _dataSet = (DataSet) dataSet;//.subsetColumns(cols);

        double percentageB = parameters.getDouble("percentSubsampleSize");
        double beta = parameters.getDouble("StARS.cutoff");
        int numSubsamples = parameters.getInt("numSubsamples");

        Parameters _parameters = new Parameters(parameters);

        List<DataSet> samples = new ArrayList<>();

        for (int i = 0; i < numSubsamples; i++) {
            BootstrapSampler sampler = new BootstrapSampler();
            sampler.setWithoutReplacements(true);
            samples.add(sampler.sample(_dataSet, (int) (percentageB * _dataSet.getNumRows())));
        }

        double maxD = Double.NEGATIVE_INFINITY;
        double _lambda = Double.NaN;

        for (double lambda = this.low; lambda <= this.high; lambda += 0.5) {
            double D = StARS.getD(parameters, this.parameter, lambda, samples, this.algorithm);
            System.out.println("lambda = " + lambda + " D = " + D);

            if (D > maxD && D < beta) {
                maxD = D;
                _lambda = lambda;
            }
        }

        System.out.println("FINAL: lambda = " + _lambda + " D = " + maxD);

        System.out.println(this.parameter + " = " + StARS.getValue(_lambda, parameters));
        _parameters.set(this.parameter, StARS.getValue(_lambda, parameters));

        return this.algorithm.search(dataSet, _parameters);
    }

    @Override
    public Graph getComparisonGraph(Graph graph) {
        return this.algorithm.getComparisonGraph(graph);
    }

    @Override
    public String getDescription() {
        return "StARS for " + this.algorithm.getDescription() + " parameter = " + this.parameter;
    }

    @Override
    public DataType getDataType() {
        return this.algorithm.getDataType();
    }

    @Override
    public List<String> getParameters() {
        List<String> parameters = this.algorithm.getParameters();
        parameters.add("depth");
        parameters.add("verbose");
        parameters.add("StARS.percentageB");
        parameters.add("StARS.tolerance");
        parameters.add("StARS.cutoff");
        parameters.add("numSubsamples");

        return parameters;
    }
}
