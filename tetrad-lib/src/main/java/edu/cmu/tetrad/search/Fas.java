///////////////////////////////////////////////////////////////////////////////
// For information as to what this class does, see the Javadoc, below.       //
// Copyright (C) 1998, 1999, 2000, 2001, 2002, 2003, 2004, 2005, 2006,       //
// 2007, 2008, 2009, 2010, 2014 by Peter Spirtes, Richard Scheines, Joseph   //
// Ramsey, and Clark Glymour.                                                //
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

import edu.cmu.tetrad.data.Knowledge;
import edu.cmu.tetrad.graph.*;
import edu.cmu.tetrad.search.test.IndependenceTest;
import edu.cmu.tetrad.search.utils.SepsetMap;
import edu.cmu.tetrad.util.ChoiceGenerator;
import edu.cmu.tetrad.util.MillisecondTimes;
import edu.cmu.tetrad.util.TetradLogger;

import java.io.PrintStream;
import java.util.*;

/**
 * <p>Implements the adjacency search of the PC algorithm (see), which is a useful algorithm
 * in many contexts, including as the first step of FCI (see). Se we call it the "Fast
 * Adjacency Search" (FAS), to give it a name.</p>
 *
 * <p>The idea of FAS is that at a given stage of the search, an edge X*-*Y is removed from the
 * graph if X _||_ Y | S, where S is a subset of size d either of adj(X) or of adj(Y), where d
 * is the depth of the search. The fast adjacency search performs this procedure for each pair
 * of adjacent edges in the graph and for each depth d = 0, 1, 2, ..., d1, where d1 is either
 * the maximum depth or else the first such depth at which no edges can be removed. The
 * interpretation of this adjacency search is different for different algorithm, depending on
 * the assumptions of the algorithm. A mapping from {x, y} to S({x, y}) is returned for edges
 * x *-* y that have been removed.</p>
 *
 * <p>Optionally uses Heuristic 3 from Causation, Prediction and Search, which (like FAS-Stable)
 * renders the output invariant to the order of the input variables (See Tsagris).</p>
 *
 * <p>This algorithm was described in the earlier edition of this book:</p>
 *
 * <p>Spirtes, P., Glymour, C. N., Scheines, R., & Heckerman, D. (2000). Causation,
 * prediction, and search. MIT press.</p>
 *
 * @author Joseph Ramsey.
 * @see Pc
 * @see Fci
 */
public class Fas implements IFas {

    /**
     * The independence test. This should be appropriate to the types
     */
    private final IndependenceTest test;

    /**
     * The logger, by default the empty logger.
     */
    private final TetradLogger logger = TetradLogger.getInstance();

    /**
     * Specification of which edges are forbidden or required.
     */
    private Knowledge knowledge = new Knowledge();
    /**
     * The maximum number of variables conditioned on in any conditional independence test. If the depth is -1, it will
     * be taken to be the maximum value, which is 1000. Otherwise, it should be set to a non-negative integer.
     */
    private int depth = 1000;
    private int numIndependenceTests;

    /**
     * The sepsets found during the search.
     */
    private SepsetMap sepset = new SepsetMap();
    /**
     * True iff verbose output should be printed.
     */
    private boolean verbose;

    /**
     * Which heuristic to use to fix variable order (1, 2, 3, or 0 = none).
     */
    private int heuristic;

    /**
     * FAS-Stable.
     */
    private boolean stable;
    private long elapsedTime = 0L;

    //==========================CONSTRUCTORS=============================//

    /**
     * Constructs a new FastAdjacencySearch.
     */
    public Fas(IndependenceTest test) {
        this.test = test;
    }

    //==========================PUBLIC METHODS===========================//

    /**
     * Discovers all adjacencies in data.  The procedure is to remove edges in the graph which connect pairs of
     * variables which are independent conditional on some other set of variables in the graph (the "sepset"). These are
     * removed in tiers.  First, edges which are independent conditional on zero other variables are removed, then edges
     * which are independent conditional on one other variable are removed, then two, then three, and so on, until no
     * more edges can be removed from the graph.  The edges which remain in the graph after this procedure are the
     * adjacencies in the data.
     *
     * @return a SepSet, which indicates which variables are independent conditional on which other variables
     */
    public Graph search(List<Node> nodes) {
        long startTime = MillisecondTimes.timeMillis();
        nodes = new ArrayList<>(nodes);

        if (verbose) {
            this.logger.log("info", "Starting Fast Adjacency Search.");
        }

        this.test.setVerbose(this.verbose);

        int _depth = this.depth;

        if (_depth == -1) {
            _depth = 1000;
        }

        this.sepset = new SepsetMap();

        List<Edge> edges = new ArrayList<>();
        Map<Edge, Double> scores = new HashMap<>();

        if (this.heuristic == 1) {
            Collections.sort(nodes);
        }

        for (int i = 0; i < nodes.size(); i++) {
            for (int j = i + 1; j < nodes.size(); j++) {
                edges.add(Edges.undirectedEdge(nodes.get(i), nodes.get(j)));
            }
        }

        for (Edge edge : edges) {
            this.test.checkIndependence(edge.getNode1(), edge.getNode2(), new ArrayList<>());
            scores.put(edge, this.test.getScore());
        }

        if (this.heuristic == 2 || this.heuristic == 3) {
            edges.sort(Comparator.comparing(scores::get));
        }

        Map<Node, Set<Node>> adjacencies = new HashMap<>();

        for (Node node : nodes) {
            Set<Node> set = new LinkedHashSet<>();

            for (Node _node : nodes) {
                if (_node == node) continue;
                set.add(_node);
            }

            adjacencies.put(node, set);
        }

        for (Edge edge : new ArrayList<>(edges)) {
            if (scores.get(edge) != null && scores.get(edge) < 0
                    || (this.knowledge.isForbidden(edge.getNode1().getName(), edge.getNode2().getName())
                    && (this.knowledge.isForbidden(edge.getNode2().getName(), edge.getNode1().getName())))) {
                edges.remove(edge);
                adjacencies.get(edge.getNode1()).remove(edge.getNode2());
                adjacencies.get(edge.getNode2()).remove(edge.getNode1());
                this.sepset.set(edge.getNode1(), edge.getNode2(), new ArrayList<>());
            }
        }

        for (int d = 0; d <= _depth; d++) {
            boolean more;

            if (this.stable) {
                Map<Node, Set<Node>> adjacenciesCopy = new HashMap<>();

                for (Node node : adjacencies.keySet()) {
                    adjacenciesCopy.put(node, new LinkedHashSet<>(adjacencies.get(node)));
                }

                adjacencies = adjacenciesCopy;
            }

            more = searchAtDepth(scores, edges, this.test, adjacencies, d);

            if (!more) {
                break;
            }
        }

        // The search graph. It is assumed going in that all of the true adjacencies of x are in this graph for every node
        // x. It is hoped (i.e. true in the large sample limit) that true adjacencies are never removed.
        Graph graph = new EdgeListGraph(nodes);

        for (int i = 0; i < nodes.size(); i++) {
            for (int j = i + 1; j < nodes.size(); j++) {
                Node x = nodes.get(i);
                Node y = nodes.get(j);

                if (adjacencies.get(x).contains(y)) {
                    graph.addUndirectedEdge(x, y);
                }
            }
        }

        if (verbose) {
            this.logger.log("info", "Finishing Fast Adjacency Search.");
        }

        this.elapsedTime = MillisecondTimes.timeMillis() - startTime;

        return graph;
    }

    /**
     * Sets the depth of the search, which is the maximum number of variables that ben be conditioned
     * on in any conditional independence test.
     *
     * @param depth This maximum.
     */
    public void setDepth(int depth) {
        if (depth < -1) {
            throw new IllegalArgumentException(
                    "Depth must be -1 (unlimited) or >= 0.");
        }

        this.depth = depth;
    }

    /**
     * Sets the knowledge to be used int the search.
     *
     * @param knowledge This knoweldge.
     * @see Knowledge
     */
    public void setKnowledge(Knowledge knowledge) {
        this.knowledge = new Knowledge(knowledge);
    }

    /**
     * The number of independence tests.
     */
    /**
     * Returns the nubmer of independence tests that were done.
     *
     * @return This number.
     */
    public int getNumIndependenceTests() {
        return this.numIndependenceTests;
    }

    /**
     * Returns the sepsets that were discovered in the search. A 'sepset' for test X _||_ Y | Z1,...,Zm would be
     * {Z1,...,Zm}
     *
     * @return A map of these sepsets indexed by {X, Y}.
     */
    public SepsetMap getSepsets() {
        return this.sepset;
    }

    /**
     * Sets whether verbose output should be printed.
     *
     * @param verbose True iff the case.
     */
    public void setVerbose(boolean verbose) {
        this.verbose = verbose;
    }

    /**
     * Runs the search and returns the resulting (undirected) graph.
     *
     * @return This graph.
     */
    @Override
    public Graph search() {
        return search(test.getVariables());
    }

    /**
     * Returns the elapsed time of the search.
     *
     * @return This elapsed time.
     */
    public long getElapsedTime() {
        return elapsedTime;
    }

    /**
     * Returns the nodes from the test.
     *
     * @return These nodes.
     */
    @Override
    public List<Node> getNodes() {
        return this.test.getVariables();
    }

    /**
     * There are no ambiguous triples for this search, for any nodes.
     *
     * @param node The nodes in question.
     * @return An empty list.
     */
    @Override
    public List<Triple> getAmbiguousTriples(Node node) {
        return new ArrayList<>();
    }

    /**
     * This is not used here.
     *
     * @param out This print stream.
     */
    @Override
    public void setOut(PrintStream out) {
        throw new UnsupportedOperationException("Print to out for FAS is not used.");
    }

    /**
     * Sets the heuristic to use to fix variable order (1, 2, 3, or 0 = none).
     *
     * @param heuristic This heuristic.
     */
    public void setHeuristic(int heuristic) {
        this.heuristic = heuristic;
    }

    /**
     * Sets whether the stable algorithm shoudl be used.
     *
     * @param stable True iff the case.
     */
    public void setStable(boolean stable) {
        this.stable = stable;
    }

    //==============================PRIVATE METHODS======================/

    private int freeDegree(Map<Node, Set<Node>> adjacencies) {
        int max = 0;

        for (Node x : adjacencies.keySet()) {
            Set<Node> opposites = adjacencies.get(x);

            for (Node y : opposites) {
                Set<Node> adjx = new LinkedHashSet<>(opposites);
                adjx.remove(y);

                if (adjx.size() > max) {
                    max = adjx.size();
                }
            }
        }

        return max;
    }

    private boolean searchAtDepth(Map<Edge, Double> scores, List<Edge> edges, IndependenceTest test, Map<Node, Set<Node>> adjacencies, int depth) {

        for (Edge edge : edges) {
            Node x = edge.getNode1();
            Node y = edge.getNode2();

            if (Thread.currentThread().isInterrupted()) {
                break;
            }

            boolean b = checkSide(scores, test, adjacencies, depth, x, y);
            if (!b) checkSide(scores, test, adjacencies, depth, y, x);
        }

        return freeDegree(adjacencies) > depth;
    }

    private boolean checkSide(Map<Edge, Double> scores, IndependenceTest test, Map<Node, Set<Node>> adjacencies, int depth, Node x, Node y) {
        if (!adjacencies.get(x).contains(y)) return false;

        List<Node> _adjx = new ArrayList<>(adjacencies.get(x));
        _adjx.remove(y);

        if (this.heuristic == 1 || this.heuristic == 2) {
            Collections.sort(_adjx);
        }

        List<Node> ppx = possibleParents(x, _adjx, this.knowledge, y);

        Map<Node, Double> scores2 = new HashMap<>();

        for (Node node : ppx) {
            Double _score = scores.get(Edges.undirectedEdge(node, x));
            scores2.put(node, _score);
        }

        if (this.heuristic == 3) {
            ppx.sort(Comparator.comparing(scores2::get));
            Collections.reverse(ppx);
        }

        if (ppx.size() >= depth) {
            ChoiceGenerator cg = new ChoiceGenerator(ppx.size(), depth);
            int[] choice;

            while ((choice = cg.next()) != null) {
                if (Thread.currentThread().isInterrupted()) {
                    break;
                }

                List<Node> Z = GraphUtils.asList(choice, ppx);

                this.numIndependenceTests++;
                boolean independent = test.checkIndependence(x, y, Z).isIndependent();

                boolean noEdgeRequired =
                        this.knowledge.noEdgeRequired(x.getName(), y.getName());

                if (independent && noEdgeRequired) {
                    adjacencies.get(x).remove(y);
                    adjacencies.get(y).remove(x);

                    getSepsets().set(x, y, Z);

                    return true;
                }

            }
        }

        return false;
    }

    private List<Node> possibleParents(Node x, List<Node> adjx,
                                       Knowledge knowledge, Node y) {
        List<Node> possibleParents = new LinkedList<>();
        String _x = x.getName();

        for (Node z : adjx) {
            if (z == x) continue;
            if (z == y) continue;
            String _z = z.getName();

            if (possibleParentOf(_z, _x, knowledge)) {
                possibleParents.add(z);
            }
        }

        return possibleParents;
    }

    private boolean possibleParentOf(String z, String x, Knowledge knowledge) {
        return !knowledge.isForbidden(z, x) && !knowledge.isRequired(x, z);
    }
}

