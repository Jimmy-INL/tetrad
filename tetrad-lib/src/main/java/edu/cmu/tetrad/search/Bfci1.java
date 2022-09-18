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

import edu.cmu.tetrad.data.ICovarianceMatrix;
import edu.cmu.tetrad.graph.*;
import edu.cmu.tetrad.util.ChoiceGenerator;
import edu.cmu.tetrad.util.DepthChoiceGenerator;
import edu.cmu.tetrad.util.TetradLogger;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;

/**
 * Does an FCI-style latent variable search using permutation-based reasoning. Follows GFCI to
 * an extent; the GFCI reference is this:
 * <p>
 * J.M. Ogarrio and P. Spirtes and J. Ramsey, "A Hybrid Causal Search Algorithm
 * for Latent Variable Models," JMLR 2016.
 *
 * @author jdramsey
 */
public final class Bfci1 implements GraphSearch {

    // The score used, if GS is used to build DAGs.
    private final Score score;

    // The logger to use.
    private final TetradLogger logger = TetradLogger.getInstance();

    // The covariance matrix being searched over, if continuous data is supplied. This is
    // no used by the algorithm but can be retrieved by another method if desired
    ICovarianceMatrix covarianceMatrix;

    // The test used if Pearl's method is used ot build DAGs
    private IndependenceTest test;

    // Flag for complete rule set, true if you should use complete rule set, false otherwise.
    private boolean completeRuleSetUsed = true;

    // The maximum length for any discriminating path. -1 if unlimited; otherwise, a positive integer.
    private int maxPathLength = -1;

    // True iff verbose output should be printed.
    private boolean verbose;

    // The print stream that output is directed to.
    private PrintStream out = System.out;

    // GRaSP parameters
    private int numStarts = 1;
    private int depth = 4;
    private boolean useRaskuttiUhler;
    private boolean useDataOrder = true;
    private boolean useScore = true;
    private Graph graph;
    private boolean doDiscriminatingPathRule = true;
    private boolean possibleDsepSearchDone = true;

    //============================CONSTRUCTORS============================//
    public Bfci1(IndependenceTest test, Score score) {
        this.test = test;
        this.score = score;
    }

    //========================PUBLIC METHODS==========================//
    public Graph search() {
        this.logger.log("info", "Starting FCI algorithm.");
        this.logger.log("info", "Independence test = " + getTest() + ".");

        // Run BOSS-tuck to get a CPDAG (like GFCI with FGES)...
        Boss boss = new Boss(test, score);
        boss.setAlgType(Boss.AlgType.BOSS);
        boss.setUseScore(useScore);
        boss.setUseRaskuttiUhler(useRaskuttiUhler);
        boss.setUseDataOrder(useDataOrder);
        boss.setDepth(depth);
        boss.setNumStarts(numStarts);
        boss.setVerbose(false);

        List<Node> variables = this.score.getVariables();
        assert variables != null;

        List<Node> pi = boss.bestOrder(variables);
        this.graph = boss.getGraph(true);

        // Keep a copy of this CPDAG.
        Graph cpdag = new EdgeListGraph(this.graph);

        // Orient the CPDAG with all circle endpoints...
        SepsetProducer sepsets = new SepsetsGreedy(this.graph, test, null, depth);

        // Copy the colliders from the copy of the CPDAG into the o-o graph.
        this.graph.reorientAllWith(Endpoint.CIRCLE);
        copyColliders(cpdag);

        // Remove edges by conditioning on subsets of variables in triangles, orienting the
        // corresponding bidiricted edges.
        TeyssierScorer scorer = new TeyssierScorer(test, score);
        scorer.score(pi);
        triangleReduce(scorer);

//        if (true) return this.graph;


        if (this.possibleDsepSearchDone) {
            // Remove edges using the possible dsep rule.
            removeByPossibleDsep(graph, test);
        }

        // Retain only the unshielded colliders.
        retainUnshieldedColliders();

        // Do final FCI orientation rules app
        FciOrient fciOrient = new FciOrient(sepsets);
        fciOrient.setCompleteRuleSetUsed(this.completeRuleSetUsed);
        fciOrient.setDoDiscriminatingPathRule(this.doDiscriminatingPathRule);
        fciOrient.setMaxPathLength(this.maxPathLength);
        fciOrient.doFinalOrientation(graph);

        this.graph.setPag(true);

        return this.graph;
    }

    private void triangleReduce(TeyssierScorer scorer) {
        for (Edge edge : graph.getEdges()) {
            Node a = edge.getNode1();
            Node b = edge.getNode2();
            reduceVisit(scorer, a, b);
        }
    }

    private void reduceVisit(TeyssierScorer scorer, Node a, Node b) {
        TeyssierScorer scorer2 = new TeyssierScorer(scorer);
        List<Node> inTriangle = graph.getAdjacentNodes(a);
        inTriangle.retainAll(graph.getAdjacentNodes(b));

        if (graph.isAdjacentTo(a, b)) {
            DepthChoiceGenerator gen = new DepthChoiceGenerator(inTriangle.size(), inTriangle.size());
            int[] choice;

            while ((choice = gen.next()) != null) {
                List<Node> before = GraphUtils.asList(choice, inTriangle);
                List<Node> after = new ArrayList<>(inTriangle);
                after.removeAll(before);

                LinkedList<Node> perm = new LinkedList<>(inTriangle);

                for (Node d : graph.getAdjacentNodes(a)) {
                    if (graph.getEdge(a, d).pointsTowards(a)) {
                        if (!perm.contains(d)) {
                            perm.addFirst(d);
                        }
                    }
                }

                perm.remove(a);
                perm.add(a);
                perm.remove(b);
                perm.add(b);

                for (Node node : after) {
                    perm.remove(node);
                    perm.add(node);
                }

                scorer2.score(perm);

                if (!scorer2.adjacent(a, b)) {
                    for (Node x : perm) {
                        if (x == a || x == b) continue;
                        if (scorer2.collider(a, x, b)) {
                            graph.setEndpoint(a, x, Endpoint.ARROW);
                            graph.setEndpoint(b, x, Endpoint.ARROW);
                            graph.removeEdge(a, b);
                        }
                    }

//                    break;
                }
            }
        }
    }

    private void removeByPossibleDsep(Graph graph, IndependenceTest test) {
        for (Edge edge : graph.getEdges()) {
            Node a = edge.getNode1();
            Node b = edge.getNode2();

            {
                List<Node> possibleDsep = GraphUtils.possibleDsep(a, b, graph, -1);

                DepthChoiceGenerator gen = new DepthChoiceGenerator(possibleDsep.size(), possibleDsep.size());
                int[] choice;

                while ((choice = gen.next()) != null) {
                    if (choice.length < 2) continue;
                    List<Node> sepset = GraphUtils.asList(choice, possibleDsep);
                    if (new HashSet<>(graph.getAdjacentNodes(a)).containsAll(sepset)) continue;
                    if (new HashSet<>(graph.getAdjacentNodes(b)).containsAll(sepset)) continue;
                    if (test.checkIndependence(a, b, sepset).independent()) {
                        graph.removeEdge(edge);
                        break;
                    }
                }
            }

            if (graph.containsEdge(edge)) {
                {
                    List<Node> possibleDsep = GraphUtils.possibleDsep(b, a, graph, -1);

                    DepthChoiceGenerator gen = new DepthChoiceGenerator(possibleDsep.size(), possibleDsep.size());
                    int[] choice;

                    while ((choice = gen.next()) != null) {
                        if (choice.length < 2) continue;
                        List<Node> sepset = GraphUtils.asList(choice, possibleDsep);
                        if (new HashSet<>(graph.getAdjacentNodes(a)).containsAll(sepset)) continue;
                        if (new HashSet<>(graph.getAdjacentNodes(b)).containsAll(sepset)) continue;
                        if (test.checkIndependence(a, b, sepset).independent()) {
                            graph.removeEdge(edge);
                            break;
                        }
                    }
                }
            }
        }
    }

    private void copyColliders(Graph cpdag) {
        List<Node> nodes = this.graph.getNodes();

        for (Node b : nodes) {
            List<Node> adjacentNodes = this.graph.getAdjacentNodes(b);

            if (adjacentNodes.size() < 2) {
                continue;
            }

            ChoiceGenerator cg = new ChoiceGenerator(adjacentNodes.size(), 2);
            int[] combination;

            while ((combination = cg.next()) != null) {
                Node a = adjacentNodes.get(combination[0]);
                Node c = adjacentNodes.get(combination[1]);

                if (cpdag.isDefCollider(a, b, c)) {
                    this.graph.setEndpoint(a, b, Endpoint.ARROW);
                    this.graph.setEndpoint(c, b, Endpoint.ARROW);
                }
            }
        }
    }

    public void retainUnshieldedColliders() {
        Graph orig = new EdgeListGraph(graph);
        this.graph.reorientAllWith(Endpoint.CIRCLE);
        List<Node> nodes = this.graph.getNodes();

        for (Node b : nodes) {
            List<Node> adjacentNodes = this.graph.getAdjacentNodes(b);

            if (adjacentNodes.size() < 2) {
                continue;
            }

            ChoiceGenerator cg = new ChoiceGenerator(adjacentNodes.size(), 2);
            int[] combination;

            while ((combination = cg.next()) != null) {
                Node a = adjacentNodes.get(combination[0]);
                Node c = adjacentNodes.get(combination[1]);

                if (orig.isDefCollider(a, b, c) && !orig.isAdjacentTo(a, c)) {
                    this.graph.setEndpoint(a, b, Endpoint.ARROW);
                    this.graph.setEndpoint(c, b, Endpoint.ARROW);
                }
            }
        }
    }

    /**
     * @return true if Zhang's complete rule set should be used, false if only
     * R1-R4 (the rule set of the original FCI) should be used. False by
     * default.
     */
    public boolean isCompleteRuleSetUsed() {
        return this.completeRuleSetUsed;
    }

    /**
     * @param completeRuleSetUsed set to true if Zhang's complete rule set
     *                            should be used, false if only R1-R4 (the rule set of the original FCI)
     *                            should be used. False by default.
     */
    public void setCompleteRuleSetUsed(boolean completeRuleSetUsed) {
        this.completeRuleSetUsed = completeRuleSetUsed;
    }

    /**
     * @return the maximum length of any discriminating path, or -1 of
     * unlimited.
     */
    public int getMaxPathLength() {
        return this.maxPathLength;
    }

    /**
     * @param maxPathLength the maximum length of any discriminating path, or -1
     *                      if unlimited.
     */
    public void setMaxPathLength(int maxPathLength) {
        if (maxPathLength < -1) {
            throw new IllegalArgumentException("Max path length must be -1 (unlimited) or >= 0: " + maxPathLength);
        }

        this.maxPathLength = maxPathLength;
    }

    /**
     * True iff verbose output should be printed.
     */
    public boolean isVerbose() {
        return this.verbose;
    }

    public void setVerbose(boolean verbose) {
        this.verbose = verbose;
    }

    /**
     * The independence test.
     */
    public IndependenceTest getTest() {
        return this.test;
    }

    public void setTest(IndependenceTest test) {
        this.test = test;
    }

    public ICovarianceMatrix getCovMatrix() {
        return this.covarianceMatrix;
    }

    public ICovarianceMatrix getCovarianceMatrix() {
        return this.covarianceMatrix;
    }

    public void setCovarianceMatrix(ICovarianceMatrix covarianceMatrix) {
        this.covarianceMatrix = covarianceMatrix;
    }

    public PrintStream getOut() {
        return this.out;
    }

    public void setOut(PrintStream out) {
        this.out = out;
    }

    public void setNumStarts(int numStarts) {
        this.numStarts = numStarts;
    }

    public void setDepth(int depth) {
        this.depth = depth;
    }

    public void setUseRaskuttiUhler(boolean useRaskuttiUhler) {
        this.useRaskuttiUhler = useRaskuttiUhler;
    }

    public void setUseScore(boolean useScore) {
        this.useScore = useScore;
    }

    public void setUseDataOrder(boolean useDataOrder) {
        this.useDataOrder = useDataOrder;
    }

    public void setDoDiscriminatingPathRule(boolean doDiscriminatingPathRule) {
        this.doDiscriminatingPathRule = doDiscriminatingPathRule;
    }

    public void setPossibleDsepSearchDone(boolean possibleDsepSearchDone) {
        this.possibleDsepSearchDone = possibleDsepSearchDone;
    }
}
