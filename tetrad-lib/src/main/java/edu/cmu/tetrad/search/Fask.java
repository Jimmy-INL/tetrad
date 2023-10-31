///////////////////////////////////////////////////////////////////////////////
// For information as to what this class does, see the Javadoc, below.       //
// Copyright (C) 1998, 1999, 2000, 2001, 2002, 2003, 2004, 2005, 2006,       //
// 2007, 2008, 2009, 2010, 2014, 2015, 2022 by Peter Spirtes, Richard        //
// Scheines, Joseph Ramsey, and Clark Glymour.                               //
//                                                                           //
// This program is free software; you can redistribute it and/or modify      //
// it under the terms of the GNU General Public License as published by      //
// the Free Software Foundation; either version 2 of the License, or         //
// (at your option) any later version.                                       //
//                                                                           //
// This program is distributed in the hope that it will be useful,           //
// but WITHOUT ANY WARRANTY; without even the implied warranty of            //
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the             //
// GNU General Public License for more details.                              //
//                                                                           //
// You should have received a copy of the GNU General Public License         //
// along with this program; if not, write to the Free Software               //
// Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA //
///////////////////////////////////////////////////////////////////////////////

package edu.cmu.tetrad.search;

import edu.cmu.tetrad.data.DataSet;
import edu.cmu.tetrad.data.DataTransforms;
import edu.cmu.tetrad.data.Knowledge;
import edu.cmu.tetrad.graph.*;
import edu.cmu.tetrad.regression.RegressionDataset;
import edu.cmu.tetrad.regression.RegressionResult;
import edu.cmu.tetrad.search.score.Score;
import edu.cmu.tetrad.search.utils.GraphSearchUtils;
import edu.cmu.tetrad.util.*;
import org.apache.commons.math3.linear.SingularMatrixException;
import org.apache.commons.math3.util.FastMath;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static edu.cmu.tetrad.util.StatUtils.*;
import static org.apache.commons.math3.util.FastMath.*;

/**
 * <p>Implements the FASK (Fast Adjacency Skewness) algorithm, which makes decisions for adjacency
 * and orientation using a combination of conditional independence testing, judgments of nonlinear adjacency, and
 * pairwise orientation due to non-Gaussianity. The reference is this:</p>
 *
 * <p>Sanchez-Romero, R., Ramsey, J. D., Zhang, K., Glymour, M. R., Huang, B., and Glymour, C.
 * (2019). Estimating feedforward and feedback effective connections from fMRI time series: Assessments of statistical
 * methods. Network Neuroscience, 3(2), 274-30</p>
 *
 * <p>Some adjustments have been made in some ways from that version, and some additional pairwise options
 * have been added from this reference:</p>
 *
 * <p>Hyvärinen, A., and Smith, S. M. (2013). Pairwise likelihood ratios for estimation of non-Gaussian structural
 * equation models. Journal of Machine Learning Research, 14(Jan), 111-152. </p>
 *
 * <p>This method (and the Hyvarinen and Smith methods) make the assumption that the data are generated by
 * a linear, non-Gaussian causal process and attempts to recover the causal graph for that process. They do not attempt
 * to recover the parametrization of this graph; for this a separate estimation algorithm would be needed, such as
 * linear regression regressing each node onto its parents. A further assumption is made, that there are no latent
 * common causes of the algorithm. This is not a constraint on the pairwise orientation methods, since they orient with
 * respect only to the two variables at the endpoints of an edge and so are happy with all other variables being
 * considered latent with respect to that single edge. However, if the built-in adjacency search is used (FAS-Stable),
 * the existence of latents will throw this method off.</p>
 *
 * <p>
 * As was shown in the Hyvarinen and Smith paper above, FASK works quite well even if the graph contains feedback loops
 * in most configurations, including 2-cycles. 2-cycles can be detected fairly well if the FASK left-right rule is
 * selected and the 2-cycle threshold set to about 0.1--more will be detected (or hallucinated) if the threshold is set
 * higher. As shown in the Sanchez-Romero reference above, 2-cycle detection of the FASK algorithm using this rule is
 * quite good.
 * <p>
 * Some edges may be undiscoverable by FAS-Stable; to recover more of these edges, a test related to the FASK left-right
 * rule is used, and there is a threshold for this test. A good default for this threshold (the "skew edge threshold")
 * is 0.3. For more of these edges, set this threshold to a lower number.
 * <p>
 * It is assumed that the data are arranged so the each variable forms a column and that there are no missing values.
 * The data matrix is assumed to be rectangular. To this end, the Tetrad DataSet class is used, which enforces this.
 * <p>
 * Note that orienting a DAG for a linear, non-Gaussian model using the Hyvarinen and Smith pairwise rules is
 * alternatively known in the literature as Pairwise LiNGAM--see Hyvärinen, A., and Smith, S. M. (2013). Pairwise
 * likelihood ratios for estimation of non-Gaussian structural equation models. Journal of Machine Learning Research,
 * 14(Jan), 111-152. We include some of these methods here for comparison.
 * <p>
 * Parameters:
 * <p>
 * faskAdjacencyMethod: 1 # this run FAS-Stable (the one used in the paper). See Algorithm 2.
 * <p>
 * depth: -1. # control the size of the conditional set in the independence tests, setting this to a small integer may
 * reduce the running time, but can also result in false positives. -1 means that it will check "all" possible sizes.
 * <p>
 * test: sem-bic-test # test for FAS adjacency
 * <p>
 * score: sem-bic-score
 * <p>
 * semBicRule: 1 # to set the Chickering Rule, used in the original Fask
 * <p>
 * penaltyDiscount: 2 # if using sem-bic as independence test (as in the paper). In the paper this is referred as c.
 * Check step 1 and 10 in Algorithm 2 FAS stable.
 * <p>
 * skewEdgeThreshold: 0.3 # See description of Fask algorithm, and step 11 in Algorithm 1 FASK. Threshold to add edges
 * that may have been non-inferred because there was a positive/negative cycle that result in a non-zero observed
 * relation.
 * <p>
 * faskLeftRightRule: 1 # this run FASK v1, the original FASK from the paper
 * <p>
 * faskDelta: -0.3 # See step 1 and 11 in Algorithm 4 (this is the value set in the paper)
 * <p>
 * twoCycleScreeningThreshold: 0 # not used in the original paper implementation. Added afterwards. You can set it to
 * 0.3, for example, to use it as a filter to run Algorithm 3 2-cycle detection, which may take some time to run.
 * <p>
 * orientationAlpha: 0.1 # this was referred in the paper as TwoCycle Alpha or just alpha, the lower it is, the lower
 * the chance of inferring a two cycle. Check steps 17 to 28 in Algorithm 3: 2 Cycle Detection Rule.
 * <p>
 * structurePrior: 0 # prior on the number of parents. Not used in the paper implementation.
 * <p>
 * So a run of command line would look like this:
 * <p>
 * java -jar -Xmx10G causal-cmd-1.4.1-jar-with-dependencies.jar --delimiter tab --data-type continuous --dataset
 * concat_BOLDfslfilter_60_FullMacaque.txt --prefix Fask_Test_MacaqueFull --algorithm fask --faskAdjacencyMethod 1
 * --depth -1 --test sem-bic-test --score sem-bic-score --semBicRule 1 --penaltyDiscount 2 --skewEdgeThreshold 0.3
 * --faskLeftRightRule 1 --faskDelta -0.3 --twoCycleScreeningThreshold 0 --orientationAlpha 0.1 -structurePrior 0
 * </p>
 *
 * <p>This class is configured to respect knowledge of forbidden and required
 * edges, including knowledge of temporal tiers.</p>
 *
 * @author josephramsey
 * @author rubensanchez
 * @see Knowledge
 * @see Lofs
 */
public final class Fask implements IGraphSearch {


    // The score to be used for the FAS adjacency search.
    private final IndependenceTest test;
    private final Score score;
    // The data sets being analyzed. They must all have the same variables and the same
    // number of records.
    private final DataSet dataSet;
    // Used for calculating coefficient values.
    private final RegressionDataset regressionDataset;
    private double[][] D;
    // An initial graph to constrain the adjacency step.
    private Graph externalGraph;
    // Elapsed time of the search, in milliseconds.
    private long elapsed;
    // For the Fast Adjacency Search, the maximum number of edges in a conditioning set.
    private int depth = -1;
    // Knowledge the search will obey, of forbidden and required edges.
    private Knowledge knowledge = new Knowledge();
    // A threshold for including extra adjacencies due to skewness. Default is 0.3. For more edges, lower
    // this threshold.
    private double skewEdgeThreshold;
    // A threshold for making 2-cycles. Default is 0 (no 2-cycles.) Note that the 2-cycle rule will only work
    // with the FASK left-right rule. Default is 0; a good value for finding a decent set of 2-cycles is 0.1.
    private double twoCycleScreeningCutoff;
    // At the end of the procedure, two cycles marked in the graph (for having small LR differences) are then
    // tested statistically to see if they are two-cycles, using this cutoff. To adjust this cutoff, set the
    // two-cycle alpha to a number in [0, 1]. The default alpha  is 0.01.
    private double orientationCutoff;
    // The corresponding alpha.
    private double orientationAlpha;
    // Bias for orienting with negative coefficients.
    private double delta;
    // Whether X and Y should be adjusted for skewness. (Otherwise, they are assumed to have positive skewness.)
    private boolean empirical = true;
    // By default, FAS Stable will be used for adjacencies, though this can be set.
    private AdjacencyMethod adjacencyMethod = AdjacencyMethod.GRASP;
    // The left right rule to use, default FASK.
    private LeftRight leftRight = LeftRight.RSKEW;
    // The graph resulting from search.
    private Graph graph;

    /**
     * Constructor.
     *
     * @param dataSet A continuous dataset over variables V.
     * @param test    An independence test over variables V. (Used for FAS.)
     */
    public Fask(DataSet dataSet, Score score, IndependenceTest test) {
        if (dataSet == null) {
            throw new NullPointerException("Data set not provided.");
        }

        if (!dataSet.isContinuous()) {
            throw new IllegalArgumentException("For FASK, the dataset must be entirely continuous");
        }

        this.dataSet = dataSet;
        this.test = test;
        this.score = score;

        this.regressionDataset = new RegressionDataset(dataSet);
        this.orientationCutoff = getZForAlpha(0.01);
        this.orientationAlpha = 0.01;
    }

    private static double cu(double[] x, double[] y, double[] condition) {
        double exy = 0.0;

        int n = 0;

        for (int k = 0; k < x.length; k++) {
            if (condition[k] > 0) {
                exy += x[k] * y[k];
                n++;
            }
        }

        return exy / n;
    }

    // Returns E(XY | Z > 0); Z is typically either X or Y.
    private static double E(double[] x, double[] y, double[] z) {
        double exy = 0.0;
        int n = 0;

        for (int k = 0; k < x.length; k++) {
            if (z[k] > 0) {
                exy += x[k] * y[k];
                n++;
            }
        }

        return exy / n;
    }


    // Returns E(XY | Z > 0) / sqrt(E(XX | Z > 0) * E(YY | Z > 0)). Z is typically either X or Y.
    private static double correxp(double[] x, double[] y, double[] z) {
        return Fask.E(x, y, z) / sqrt(Fask.E(x, x, z) * Fask.E(y, y, z));
    }

    /**
     * Runs the search on the concatenated data, returning a graph, possibly cyclic, possibly with two-cycles. Runs the
     * fast adjacency search (FAS, Spirtes et al., 2000) followed by a modification of the robust skew rule (Pairwise
     * Likelihood Ratios for Estimation of Non-Gaussian Structural Equation Models, Smith and Hyvarinen), together with
     * some heuristics for orienting two-cycles.
     *
     * @return the graph. Some edges may be undirected (though it shouldn't be many in most cases) and some adjacencies
     * may be two-cycles.
     */
    public Graph search() {
        long start = MillisecondTimes.timeMillis();
        NumberFormat nf = new DecimalFormat("0.000");

        DataSet dataSet = DataTransforms.standardizeData(this.dataSet);

        List<Node> variables = dataSet.getVariables();
        double[][] lrs = getLrScores(); // Sets D.

        for (int i = 0; i < variables.size(); i++) {
            System.out.println("Skewness of " + variables.get(i) + " = " + skewness(this.D[i]));
        }

        TetradLogger.getInstance().forceLogMessage("FASK v. 2.0");
        TetradLogger.getInstance().forceLogMessage("");
        TetradLogger.getInstance().forceLogMessage("# variables = " + dataSet.getNumColumns());
        TetradLogger.getInstance().forceLogMessage("N = " + dataSet.getNumRows());
        TetradLogger.getInstance().forceLogMessage("Skewness edge threshold = " + this.skewEdgeThreshold);
        TetradLogger.getInstance().forceLogMessage("Orientation Alpha = " + this.orientationAlpha);
        TetradLogger.getInstance().forceLogMessage("2-cycle threshold = " + this.twoCycleScreeningCutoff);
        TetradLogger.getInstance().forceLogMessage("");

        Graph G;

        if (this.adjacencyMethod == AdjacencyMethod.BOSS) {
            PermutationSearch fas = new PermutationSearch(new Boss(this.score));
            fas.setKnowledge(this.knowledge);
            G = fas.search();
        } else if (this.adjacencyMethod == AdjacencyMethod.GRASP) {
            Grasp fas = new Grasp(this.score);
            fas.setDepth(5);
            fas.setNonSingularDepth(1);
            fas.setUncoveredDepth(1);
            fas.setNumStarts(5);
            fas.setAllowInternalRandomness(true);
            fas.setUseDataOrder(false);
            fas.setKnowledge(this.knowledge);
            fas.bestOrder(dataSet.getVariables());
            G = fas.getGraph(true);
        } else if (this.adjacencyMethod == AdjacencyMethod.FAS_STABLE) {
            Fas fas = new Fas(this.test);
            fas.setStable(true);
            fas.setVerbose(false);
            fas.setKnowledge(this.knowledge);
            G = fas.search();
        } else if (this.adjacencyMethod == AdjacencyMethod.FGES) {
            Fges fas = new Fges(this.score);
            fas.setVerbose(false);
            fas.setKnowledge(this.knowledge);
            G = fas.search();
        } else if (this.adjacencyMethod == AdjacencyMethod.EXTERNAL_GRAPH) {
            if (this.externalGraph == null) throw new IllegalStateException("An external graph was not supplied.");

            Graph g1 = new EdgeListGraph(this.externalGraph.getNodes());

            for (Edge edge : this.externalGraph.getEdges()) {
                Node x = edge.getNode1();
                Node y = edge.getNode2();

                if (!g1.isAdjacentTo(x, y)) g1.addUndirectedEdge(x, y);
            }

            g1 = GraphUtils.replaceNodes(g1, dataSet.getVariables());

            G = g1;
        } else if (this.adjacencyMethod == AdjacencyMethod.NONE) {
            G = new EdgeListGraph(variables);
        } else {
            throw new IllegalStateException("That method was not configured: " + this.adjacencyMethod);
        }

        G = GraphUtils.replaceNodes(G, dataSet.getVariables());

        TetradLogger.getInstance().forceLogMessage("");

        GraphSearchUtils.pcOrientbk(this.knowledge, G, G.getNodes());

        Graph graph = new EdgeListGraph(G.getNodes());

        TetradLogger.getInstance().forceLogMessage("X\tY\tMethod\tLR\tEdge");

        int V = variables.size();

        List<NodePair> twoCycles = new ArrayList<>();

        for (int i = 0; i < V; i++) {
            for (int j = i + 1; j < V; j++) {
                Node X = variables.get(i);
                Node Y = variables.get(j);

                // Centered
                double[] x = this.D[i];
                double[] y = this.D[j];

                double cx = Fask.correxp(x, y, x);
                double cy = Fask.correxp(x, y, y);

                if (G.isAdjacentTo(X, Y) || (abs(cx - cy) > this.skewEdgeThreshold)) {
                    double lr = lrs[i][j];// leftRight(x, y);

                    if (edgeForbiddenByKnowledge(X, Y) && edgeForbiddenByKnowledge(Y, X)) {
                        TetradLogger.getInstance().forceLogMessage(X + "\t" + Y + "\tknowledge_forbidden"
                                + "\t" + nf.format(lr)
                                + "\t" + X + "<->" + Y
                        );
                        continue;
                    }

                    if (knowledgeOrients(X, Y)) {
                        TetradLogger.getInstance().forceLogMessage(X + "\t" + Y + "\tknowledge"
                                + "\t" + nf.format(lr)
                                + "\t" + X + "-->" + Y
                        );
                        graph.addDirectedEdge(X, Y);
                    } else if (knowledgeOrients(Y, X)) {
                        TetradLogger.getInstance().forceLogMessage(X + "\t" + Y + "\tknowledge"
                                + "\t" + nf.format(lr)
                                + "\t" + X + "<--" + Y
                        );
                        graph.addDirectedEdge(Y, X);
                    } else {
                        if (zeroDiff(i, j, this.D)) {
                            TetradLogger.getInstance().forceLogMessage(X + "\t" + Y + "\t2-cycle Prescreen"
                                    + "\t" + nf.format(lr)
                                    + "\t" + X + "...TC?..." + Y
                            );

                            System.out.println(X + " " + Y + " lr = " + lr + " zero");
                            continue;
                        }

                        if (this.twoCycleScreeningCutoff > 0 && abs(faskLeftRightV2(x, y, empirical, delta)) < this.twoCycleScreeningCutoff) {
                            TetradLogger.getInstance().forceLogMessage(X + "\t" + Y + "\t2-cycle Prescreen"
                                    + "\t" + nf.format(lr)
                                    + "\t" + X + "...TC?..." + Y
                            );

                            twoCycles.add(new NodePair(X, Y));
                            System.out.println(X + " " + Y + " lr = " + lr + " zero");
                        }

                        if (lr > 0) {
                            TetradLogger.getInstance().forceLogMessage(X + "\t" + Y + "\tleft-right"
                                    + "\t" + nf.format(lr)
                                    + "\t" + X + "-->" + Y
                            );
                            graph.addDirectedEdge(X, Y);
                        } else if (lr < 0) {
                            TetradLogger.getInstance().forceLogMessage(Y + "\t" + X + "\tleft-right"
                                    + "\t" + nf.format(lr)
                                    + "\t" + Y + "-->" + X
                            );
                            graph.addDirectedEdge(Y, X);
                        }
                    }
                }
            }
        }

        if (this.twoCycleScreeningCutoff > 0 && this.orientationAlpha == 0) {
            for (NodePair edge : twoCycles) {
                Node X = edge.getFirst();
                Node Y = edge.getSecond();

                graph.removeEdges(X, Y);
                graph.addDirectedEdge(X, Y);
                graph.addDirectedEdge(Y, X);
                logTwoCycle(nf, variables, this.D, X, Y, "2-cycle Pre-screen");
            }
        } else if (this.twoCycleScreeningCutoff > 0 && this.orientationAlpha > 0) {
            for (NodePair edge : twoCycles) {
                Node X = edge.getFirst();
                Node Y = edge.getSecond();

                int i = variables.indexOf(X);
                int j = variables.indexOf(Y);

                if (twoCycleTest(i, j, this.D, graph, variables)) {
                    graph.removeEdges(X, Y);
                    graph.addDirectedEdge(X, Y);
                    graph.addDirectedEdge(Y, X);
                    logTwoCycle(nf, variables, this.D, X, Y, "2-cycle Screened then Tested");
                }
            }
        }

        long stop = MillisecondTimes.timeMillis();
        this.elapsed = stop - start;

        this.graph = graph;

        return graph;
    }

    /**
     * Returns the coefficient matrix for the search. If the search has not yet run, runs it, then estimates
     * coefficients of each node given its parents using linear regression and forms the B matrix of coefficients from
     * these estimates. B[i][j] != 0 means i-&gt;j with that coefficient.
     *
     * @return This matrix as a double[][] array.
     */
    public double[][] getB() {
        if (this.graph == null) search();

        List<Node> nodes = this.dataSet.getVariables();
        double[][] B = new double[nodes.size()][nodes.size()];

        for (int j = 0; j < nodes.size(); j++) {
            Node y = nodes.get(j);

            List<Node> pary = new ArrayList<>(this.graph.getParents(y));
            RegressionResult result = this.regressionDataset.regress(y, pary);
            double[] coef = result.getCoef();

            for (int i = 0; i < pary.size(); i++) {
                B[nodes.indexOf(pary.get(i))][j] = coef[i + 1];
            }
        }

        return B;
    }

    /**
     * Returns a matrix of left-right scores for the search. If lr = getLrScores(), then lr[i][j]
     * is the left right scores leftRight(data[i], data[j]);
     *
     * @return This matrix as a double[][] array.
     */
    public double[][] getLrScores() {
        List<Node> variables = this.dataSet.getVariables();
        double[][] D = DataTransforms.standardizeData(this.dataSet).getDoubleData().transpose().toArray();

        double[][] lr = new double[variables.size()][variables.size()];

        for (int i = 0; i < variables.size(); i++) {
            for (int j = 0; j < variables.size(); j++) {
                lr[i][j] = leftRight(D[i], D[j]);
            }
        }

        this.D = D;

        return lr;
    }

    /**
     * @return The depth of search for the Fast Adjacency Search (FAS).
     */
    public int getDepth() {
        return this.depth;
    }

    /**
     * @param depth The depth of search for the Fast Adjacency Search (S). The default is -1. Unlimited. Making this too
     *              high may result in statistical errors.
     */
    public void setDepth(int depth) {
        this.depth = depth;
    }

    /**
     * @return The elapsed time in milliseconds.
     */
    public long getElapsedTime() {
        return this.elapsed;
    }

    /**
     * @return the current knowledge.
     */
    public Knowledge getKnowledge() {
        return this.knowledge;
    }

    /**
     * @param knowledge Knowledge of forbidden and required edges.
     */
    public void setKnowledge(Knowledge knowledge) {
        this.knowledge = new Knowledge(knowledge);
    }

    /**
     * Sets the external graph to use. This graph will be used as a set of adjacencies to be included in the graph is
     * the "external graph" options is selected. It doesn't matter what the orientations of the graph are; the graph
     * will be reoriented using the left-right rule selected.
     *
     * @param externalGraph This graph.
     */
    public void setExternalGraph(Graph externalGraph) {
        this.externalGraph = externalGraph;
    }

    /**
     * Sets the skew-edge threshold.
     *
     * @param skewEdgeThreshold This threshold.
     */
    public void setSkewEdgeThreshold(double skewEdgeThreshold) {
        this.skewEdgeThreshold = skewEdgeThreshold;
    }

    /**
     * Sets the cutoff for two-cycle screening.
     *
     * @param twoCycleScreeningCutoff This cutoff.
     */
    public void setTwoCycleScreeningCutoff(double twoCycleScreeningCutoff) {
        if (twoCycleScreeningCutoff < 0)
            throw new IllegalStateException("Two cycle screening threshold must be >= 0");
        this.twoCycleScreeningCutoff = twoCycleScreeningCutoff;
    }

    /**
     * Sets the orientation alpha.
     *
     * @param orientationAlpha This alpha.
     */
    public void setOrientationAlpha(double orientationAlpha) {
        if (orientationAlpha < 0 || orientationAlpha > 1)
            throw new IllegalArgumentException("Two cycle testing alpha should be in [0, 1].");
        this.orientationCutoff = getZForAlpha(orientationAlpha);
        this.orientationAlpha = orientationAlpha;
    }

    /**
     * Sets the left-right rule used
     *
     * @param leftRight This rule.
     * @see LeftRight
     */
    public void setLeftRight(LeftRight leftRight) {
        this.leftRight = leftRight;
    }

    /**
     * Sets the adjacency method used.
     *
     * @param adjacencyMethod This method.
     * @see AdjacencyMethod
     */
    public void setAdjacencyMethod(AdjacencyMethod adjacencyMethod) {
        this.adjacencyMethod = adjacencyMethod;
    }

    /**
     * Sets the delta to use.
     *
     * @param delta This delta.
     */
    public void setDelta(double delta) {
        this.delta = delta;
    }

    /**
     * Sets whether the empirical option is selected.
     *
     * @param empirical True, if so.
     */
    public void setEmpirical(boolean empirical) {
        this.empirical = empirical;
    }

    /**
     * A left/right judgment for double[] arrays (data) as input.
     *
     * @param x The data for the first variable.
     * @param y The data for the second variable.
     * @return The left-right judgment, which is negative if X&lt;-Y, positive if X-&gt;Y, and 0 if indeterminate.
     */
    public double leftRight(double[] x, double[] y) {
        if (this.leftRight == LeftRight.FASK1) {
            return faskLeftRightV1(x, y, empirical, delta);
        } else if (this.leftRight == LeftRight.FASK2) {
            return faskLeftRightV2(x, y, empirical, delta);
        } else if (this.leftRight == LeftRight.RSKEW) {
            return robustSkew(x, y, empirical);
        } else if (this.leftRight == LeftRight.SKEW) {
            return skew(x, y, empirical);
        } else if (this.leftRight == LeftRight.TANH) {
            return tanh(x, y, empirical);
        }

        throw new IllegalStateException("Left right rule not configured: " + this.leftRight);
    }

    public static double faskLeftRightV2(double[] x, double[] y, boolean empirical, double delta) {
        double sx = skewness(x);
        double sy = skewness(y);
        double r = correlation(x, y);
        double lr = Fask.correxp(x, y, x) - Fask.correxp(x, y, y);

        if (empirical) {
            lr *= signum(sx) * signum(sy);
        }

        if (r < delta) {
            lr *= -1;
        }

        return lr;
    }

    public static double faskLeftRightV1(double[] x, double[] y, boolean empirical, double delta) {
        double left = Fask.cu(x, y, x) / (sqrt(Fask.cu(x, x, x) * Fask.cu(y, y, x)));
        double right = Fask.cu(x, y, y) / (sqrt(Fask.cu(x, x, y) * Fask.cu(y, y, y)));
        double lr = left - right;

        double r = correlation(x, y);
        double sx = skewness(x);
        double sy = skewness(y);

        if (empirical) {
            r *= signum(sx) * signum(sy);
        }

        lr *= signum(r);
        if (r < delta) lr *= -1;

        return lr;
    }

    public static double robustSkew(double[] x, double[] y, boolean empirical) {

        if (empirical) {
            x = correctSkewness(x, skewness(x));
            y = correctSkewness(y, skewness(y));
        }

        double[] lr = new double[x.length];

        for (int i = 0; i < x.length; i++) {
            lr[i] = g(x[i]) * y[i] - x[i] * g(y[i]);
        }

        return correlation(x, y) * mean(lr);
    }

    public static double skew(double[] x, double[] y, boolean empirical) {

        if (empirical) {
            x = correctSkewness(x, skewness(x));
            y = correctSkewness(y, skewness(y));
        }

        double[] lr = new double[x.length];

        for (int i = 0; i < x.length; i++) {
            lr[i] = x[i] * x[i] * y[i] - x[i] * y[i] * y[i];
        }

        return correlation(x, y) * mean(lr);
    }

    private double tanh(double[] x, double[] y, boolean empirical) {

        if (empirical) {
            x = correctSkewness(x, skewness(x));
            y = correctSkewness(y, skewness(y));
        }

        double[] lr = new double[x.length];

        for (int i = 0; i < x.length; i++) {
            lr[i] = x[i] * FastMath.tanh(y[i]) - FastMath.tanh(x[i]) * y[i];
        }

        return correlation(x, y) * mean(lr);
    }

    public static double g(double x) {
        return log(cosh(FastMath.max(x, 0)));
    }

    private boolean knowledgeOrients(Node X, Node Y) {
        return this.knowledge.isForbidden(Y.getName(), X.getName()) || this.knowledge.isRequired(X.getName(), Y.getName());
    }

    private boolean edgeForbiddenByKnowledge(Node X, Node Y) {
        return this.knowledge.isForbidden(Y.getName(), X.getName()) && this.knowledge.isForbidden(X.getName(), Y.getName());
    }

    public static double[] correctSkewness(double[] data, double sk) {
        double[] data2 = new double[data.length];
        for (int i = 0; i < data.length; i++) data2[i] = data[i] * signum(sk);
        return data2;
    }

    private boolean twoCycleTest(int i, int j, double[][] D, Graph G0, List<Node> V) {
        Node X = V.get(i);
        Node Y = V.get(j);

        double[] x = D[i];
        double[] y = D[j];

        Set<Node> adjSet = new HashSet<>(G0.getAdjacentNodes(X));
        adjSet.addAll(G0.getAdjacentNodes(Y));
        List<Node> adj = new ArrayList<>(adjSet);
        adj.remove(X);
        adj.remove(Y);

        SublistGenerator gen = new SublistGenerator(adj.size(), FastMath.min(this.depth, adj.size()));
        int[] choice;

        while ((choice = gen.next()) != null) {
            List<Node> _adj = GraphUtils.asList(choice, adj);
            double[][] _Z = new double[_adj.size()][];

            for (int f = 0; f < _adj.size(); f++) {
                Node _z = _adj.get(f);
                int column = this.dataSet.getColumn(_z);
                _Z[f] = D[column];
            }

            double pc;
            double pc1;
            double pc2;

            try {
                pc = partialCorrelation(x, y, _Z, x, Double.NEGATIVE_INFINITY);
                pc1 = partialCorrelation(x, y, _Z, x, 0);
                pc2 = partialCorrelation(x, y, _Z, y, 0);
            } catch (SingularMatrixException e) {
                System.out.println("Singularity X = " + X + " Y = " + Y + " adj = " + adj);
                TetradLogger.getInstance().forceLogMessage("Singularity X = " + X + " Y = " + Y + " adj = " + adj);
                continue;
            }

            int nc = getRows(x, x, 0, Double.NEGATIVE_INFINITY).size();
            int nc1 = getRows(x, x, 0, +1).size();
            int nc2 = getRows(y, y, 0, +1).size();

            double z = 0.5 * (log(1.0 + pc) - log(1.0 - pc));
            double z1 = 0.5 * (log(1.0 + pc1) - log(1.0 - pc1));
            double z2 = 0.5 * (log(1.0 + pc2) - log(1.0 - pc2));

            double zv1 = (z - z1) / sqrt((1.0 / ((double) nc - 3) + 1.0 / ((double) nc1 - 3)));
            double zv2 = (z - z2) / sqrt((1.0 / ((double) nc - 3) + 1.0 / ((double) nc2 - 3)));

            boolean rejected1 = abs(zv1) > this.orientationCutoff;
            boolean rejected2 = abs(zv2) > this.orientationCutoff;

            boolean possibleTwoCycle = false;

            if (zv1 < 0 && zv2 > 0 && rejected1) {
                possibleTwoCycle = true;
            } else if (zv1 > 0 && zv2 < 0 && rejected2) {
                possibleTwoCycle = true;
            } else if (rejected1 && rejected2) {
                possibleTwoCycle = true;
            }

            if (!possibleTwoCycle) {
                return false;
            }
        }

        return true;
    }

    private boolean zeroDiff(int i, int j, double[][] D) {
        double[] x = D[i];
        double[] y = D[j];

        double pc1;
        double pc2;

        try {
            pc1 = partialCorrelation(x, y, new double[0][], x, 0);
            pc2 = partialCorrelation(x, y, new double[0][], y, 0);
        } catch (SingularMatrixException e) {
            List<Node> nodes = dataSet.getVariables();
            throw new RuntimeException("Singularity encountered (conditioning on X > 0, Y > 0) for variables "
                    + nodes.get(i) + ", " + nodes.get(j));
        }

        int nc1 = getRows(x, x, 0, +1).size();
        int nc2 = getRows(y, y, 0, +1).size();

        double z1 = 0.5 * (log(1.0 + pc1) - log(1.0 - pc1));
        double z2 = 0.5 * (log(1.0 + pc2) - log(1.0 - pc2));

        double zv = (z1 - z2) / sqrt((1.0 / ((double) nc1 - 3) + 1.0 / ((double) nc2 - 3)));

        return abs(zv) <= this.twoCycleScreeningCutoff;
    }

    private double partialCorrelation(double[] x, double[] y, double[][] z, double[] condition, double threshold) throws SingularMatrixException {
        double[][] cv = covMatrix(x, y, z, condition, threshold, 1);
        Matrix m = new Matrix(cv).transpose();
        return StatUtils.partialCorrelation(m);
    }

    private void logTwoCycle(NumberFormat nf, List<Node> variables, double[][] d, Node X, Node Y, String type) {
        int i = variables.indexOf(X);
        int j = variables.indexOf(Y);

        double[] x = d[i];
        double[] y = d[j];

        double lr = leftRight(x, y);

        TetradLogger.getInstance().forceLogMessage(X + "\t" + Y + "\t" + type
                + "\t" + nf.format(lr)
                + "\t" + X + "<=>" + Y
        );
    }

    /**
     * Enumerates the options left-right rules to use for FASK. Options include the FASK left-right rule and three
     * left-right rules from the Hyvarinen and Smith pairwise orientation paper: Robust Skew, Skew, and Tanh. In that
     * paper, "empirical" versions were given in which the variables are multiplied through by the signs of the
     * skewnesses; we follow this advice here (with good results). These others are provided for those who prefer them.
     */
    public enum LeftRight {FASK1, FASK2, RSKEW, SKEW, TANH}

    /**
     * Enumerates the alternatives to use for finding the initial adjacencies for FASK.
     */
    public enum AdjacencyMethod {FAS_STABLE, FGES, BOSS, GRASP, EXTERNAL_GRAPH, NONE}
}






