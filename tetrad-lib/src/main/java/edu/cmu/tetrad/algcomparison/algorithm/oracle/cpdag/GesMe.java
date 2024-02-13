package edu.cmu.tetrad.algcomparison.algorithm.oracle.cpdag;

import edu.cmu.tetrad.algcomparison.algorithm.Algorithm;
import edu.cmu.tetrad.algcomparison.algorithm.ReturnsBootstrapGraphs;
import edu.cmu.tetrad.algcomparison.score.ScoreWrapper;
import edu.cmu.tetrad.algcomparison.score.SemBicScoreDeterministic;
import edu.cmu.tetrad.annotation.Bootstrapping;
import edu.cmu.tetrad.annotation.Experimental;
import edu.cmu.tetrad.data.*;
import edu.cmu.tetrad.graph.EdgeListGraph;
import edu.cmu.tetrad.graph.Graph;
import edu.cmu.tetrad.graph.GraphTransforms;
import edu.cmu.tetrad.graph.Node;
import edu.cmu.tetrad.search.score.Score;
import edu.cmu.tetrad.util.*;
import edu.pitt.dbmi.algo.resampling.GeneralResamplingTest;
import org.apache.commons.math3.util.FastMath;

import java.io.PrintStream;
import java.io.Serial;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;

import static org.apache.commons.math3.util.FastMath.sqrt;

/**
 * FGES (the heuristic version).
 *
 * @author josephramsey
 * @version $Id: $Id
 */
@Bootstrapping
@Experimental
public class GesMe implements Algorithm, ReturnsBootstrapGraphs {

    @Serial
    private static final long serialVersionUID = 23L;

    /**
     * The score to use.
     */
    private final ScoreWrapper score = new SemBicScoreDeterministic();

    /**
     * The bootstrap graphs.
     */
    private boolean compareToTrue;

    /**
     * The bootstrap graphs.
     */
    private List<Graph> bootstrapGraphs = new ArrayList<>();


    /**
     * <p>Constructor for GesMe.</p>
     */
    public GesMe() {
        setCompareToTrue(false);
    }

    /**
     * <p>Constructor for GesMe.</p>
     *
     * @param compareToTrueGraph a boolean
     */
    public GesMe(boolean compareToTrueGraph) {
        setCompareToTrue(compareToTrueGraph);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Graph search(DataModel dataSet, Parameters parameters) {
        if (parameters.getInt(Params.NUMBER_RESAMPLING) < 1) {
//          dataSet = DataUtils.center((DataSet) dataSet);
            CovarianceMatrix covarianceMatrix = new CovarianceMatrix((DataSet) dataSet);

            edu.cmu.tetrad.search.FactorAnalysis analysis = new edu.cmu.tetrad.search.FactorAnalysis(covarianceMatrix);
            analysis.setThreshold(parameters.getDouble("convergenceThreshold"));
            analysis.setNumFactors(parameters.getInt("numFactors"));
//            analysis.setNumFactors(((DataSet) dataSet).getNumColumns());

            Matrix unrotated = analysis.successiveResidual();
            Matrix rotated = analysis.successiveFactorVarimax(unrotated);

            if (parameters.getBoolean(Params.VERBOSE)) {
                NumberFormat nf = NumberFormatUtil.getInstance().getNumberFormat();

                String output = "Unrotated Factor Loading Matrix:\n";

                output += tableString(unrotated, nf, Double.POSITIVE_INFINITY);

                if (unrotated.getNumColumns() != 1) {
                    output += "\n\nRotated Matrix (using sequential varimax):\n";
                    output += tableString(rotated, nf, parameters.getDouble("fa_threshold"));
                }

                System.out.println(output);
                TetradLogger.getInstance().forceLogMessage(output);
            }

            Matrix L;

            if (parameters.getBoolean("useVarimax")) {
                L = rotated;
            } else {
                L = unrotated;
            }


            Matrix residual = analysis.getResidual();

            ICovarianceMatrix covFa = new CovarianceMatrix(covarianceMatrix.getVariables(), L.times(L.transpose()),
                    covarianceMatrix.getSampleSize());

            System.out.println(covFa);

            double[] vars = covarianceMatrix.getMatrix().diag().toArray();
            List<Integer> indices = new ArrayList<>();
            for (int i = 0; i < vars.length; i++) {
                indices.add(i);
            }

            indices.sort((o1, o2) -> -Double.compare(vars[o1], vars[o2]));

            NumberFormat nf = new DecimalFormat("0.000");

            for (Integer index : indices) {
                System.out.println(nf.format(vars[index]) + " ");
            }

            System.out.println();

            int n = vars.length;

            int cutoff = (int) (n * ((sqrt(8 * n + 1) - 1) / (2 * n)));

            List<Node> nodes = covarianceMatrix.getVariables();

            List<Node> leaves = new ArrayList<>();

            for (int i = 0; i < cutoff; i++) {
                leaves.add(nodes.get(indices.get(i)));
            }

            Knowledge knowledge2 = new Knowledge();

            for (Node v : nodes) {
                if (leaves.contains(v)) {
                    knowledge2.addToTier(2, v.getName());
                } else {
                    knowledge2.addToTier(1, v.getName());
                }
            }

            knowledge2.setTierForbiddenWithin(2, true);

            System.out.println("knowledge2 = " + knowledge2);

            Score score = this.score.getScore(covFa, parameters);

            edu.cmu.tetrad.search.Fges search = new edu.cmu.tetrad.search.Fges(score);
            search.setFaithfulnessAssumed(parameters.getBoolean(Params.FAITHFULNESS_ASSUMED));

            if (parameters.getBoolean("enforceMinimumLeafNodes")) {
                search.setKnowledge(knowledge2);
            }

            search.setVerbose(parameters.getBoolean(Params.VERBOSE));
            search.setMaxDegree(parameters.getInt(Params.MAX_DEGREE));
            search.setSymmetricFirstStep(parameters.getBoolean(Params.SYMMETRIC_FIRST_STEP));

            Object obj = parameters.get(Params.PRINT_STREAM);
            if (obj instanceof PrintStream) {
                search.setOut((PrintStream) obj);
            }

            if (parameters.getBoolean(Params.VERBOSE)) {
//                NumberFormat nf = NumberFormatUtil.getInstance().getNumberFormat();
                String output = "Unrotated Factor Loading Matrix:\n";
                double threshold = parameters.getDouble("fa_threshold");

                output += tableString(L, nf, Double.POSITIVE_INFINITY);

                if (L.getNumColumns() != 1) {
                    output += "\n\nL:\n";
                    output += tableString(L, nf, threshold);
                }

                System.out.println(output);
                TetradLogger.getInstance().forceLogMessage(output);
            }

            System.out.println("residual = " + residual);

            return search.search();
        } else {
            GesMe algorithm = new GesMe(this.compareToTrue);

            DataSet data = (DataSet) dataSet;
            GeneralResamplingTest search = new GeneralResamplingTest(data, algorithm, parameters.getInt(Params.NUMBER_RESAMPLING), parameters.getDouble(Params.PERCENT_RESAMPLE_SIZE), parameters.getBoolean(Params.RESAMPLING_WITH_REPLACEMENT), parameters.getInt(Params.RESAMPLING_ENSEMBLE), parameters.getBoolean(Params.ADD_ORIGINAL_DATASET), parameters.getInt(Params.BOOTSTRAPPING_NUM_THEADS));

            search.setParameters(parameters);
            search.setVerbose(parameters.getBoolean(Params.VERBOSE));
            Graph graph = search.search();
            if (parameters.getBoolean(Params.SAVE_BOOTSTRAP_GRAPHS)) this.bootstrapGraphs = search.getGraphs();
            return graph;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Graph getComparisonGraph(Graph graph) {
        if (this.compareToTrue) {
            return new EdgeListGraph(graph);
        } else {
            Graph dag = new EdgeListGraph(graph);
            return GraphTransforms.cpdagForDag(dag);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getDescription() {
        return "FGES (Fast Greedy Equivalence Search) using " + this.score.getDescription();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public DataType getDataType() {
        return this.score.getDataType();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<String> getParameters() {
        List<String> parameters = new ArrayList<>();
        parameters.add(Params.SYMMETRIC_FIRST_STEP);
        parameters.add(Params.FAITHFULNESS_ASSUMED);
        parameters.add(Params.MAX_DEGREE);
        parameters.add(Params.DETERMINISM_THRESHOLD);
        parameters.add("convergenceThreshold");
        parameters.add("fa_threshold");
        parameters.add("numFactors");
        parameters.add("useVarimax");
        parameters.add("enforceMinimumLeafNodes");

        parameters.add(Params.VERBOSE);

        return parameters;
    }

    /**
     * <p>Setter for the field <code>compareToTrue</code>.</p>
     *
     * @param compareToTrue a boolean
     */
    public void setCompareToTrue(boolean compareToTrue) {
        this.compareToTrue = compareToTrue;
    }

    private String tableString(Matrix matrix, NumberFormat nf, double threshold) {
        TextTable table = new TextTable(matrix.getNumRows() + 1, matrix.getNumColumns() + 1);

        for (int i = 0; i < matrix.getNumRows() + 1; i++) {
            for (int j = 0; j < matrix.getNumColumns() + 1; j++) {
                if (i > 0 && j == 0) {
                    table.setToken(i, 0, "X" + i);
                } else if (i == 0 && j > 0) {
                    table.setToken(0, j, "Factor " + j);
                } else if (i > 0) {
                    double coefficient = matrix.get(i - 1, j - 1);
                    String token = !Double.isNaN(coefficient) ? nf.format(coefficient) : "Undefined";
                    token += FastMath.abs(coefficient) > threshold ? "*" : " ";
                    table.setToken(i, j, token);
                }
            }
        }

        return "\n" + table;

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<Graph> getBootstrapGraphs() {
        return this.bootstrapGraphs;
    }
}
