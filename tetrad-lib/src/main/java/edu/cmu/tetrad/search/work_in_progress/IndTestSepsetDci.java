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

package edu.cmu.tetrad.search.work_in_progress;

import edu.cmu.tetrad.data.DataSet;
import edu.cmu.tetrad.graph.IndependenceFact;
import edu.cmu.tetrad.graph.Node;
import edu.cmu.tetrad.graph.NodeType;
import edu.cmu.tetrad.search.IndependenceTest;
import edu.cmu.tetrad.search.test.IndependenceResult;
import edu.cmu.tetrad.search.utils.LogUtilsSearch;
import edu.cmu.tetrad.util.NumberFormatUtil;
import edu.cmu.tetrad.util.TetradLogger;

import java.text.NumberFormat;
import java.util.*;


/**
 * Checks independence facts for variables associated with a sepset by simply querying the sepset
 *
 * @author Robert Tillman
 * @version $Id: $Id
 */
public class IndTestSepsetDci implements IndependenceTest {

    /**
     * The sepset being queried
     */
    private final SepsetMapDci sepset;

    /**
     * The map from nodes to variables.
     */
    private final Map<Node, Node> nodesToVariables;

    /**
     * The map from variables to nodes.
     */
    private final Map<Node, Node> variablesToNodes;

    /**
     * The list of observed variables (i.e. variables for observed nodes).
     */
    private final List<Node> observedVars;
    private boolean verbose;

    /**
     * Constructs a new independence test that returns d-separation facts for the given graph as independence results.
     *
     * @param sepset a {@link edu.cmu.tetrad.search.work_in_progress.SepsetMapDci} object
     * @param nodes  a {@link java.util.List} object
     */
    public IndTestSepsetDci(SepsetMapDci sepset, List<Node> nodes) {
        if (sepset == null) {
            throw new NullPointerException();
        }

        this.sepset = sepset;
        this.nodesToVariables = new HashMap<>();
        this.variablesToNodes = new HashMap<>();

        for (Node node : nodes) {
            this.nodesToVariables.put(node, node);
            this.variablesToNodes.put(node, node);
        }

        this.observedVars = calcObservedVars(nodes);
    }

    /**
     * {@inheritDoc}
     * <p>
     * Required by IndependenceTest.
     */
    public IndependenceTest indTestSubset(List<Node> vars) {
        if (vars.isEmpty()) {
            throw new IllegalArgumentException("Subset may not be empty.");
        }

        for (Node var : vars) {
            if (!getVariables().contains(var)) {
                throw new IllegalArgumentException(
                        "All vars must be original vars");
            }
        }

        return this;
    }

    /**
     * {@inheritDoc}
     * <p>
     * Checks the indicated independence fact.
     *
     * @param x a {@link edu.cmu.tetrad.graph.Node} object
     * @param y a {@link edu.cmu.tetrad.graph.Node} object
     * @param z a {@link java.util.Set} object
     * @return a {@link edu.cmu.tetrad.search.test.IndependenceResult} object
     */
    public IndependenceResult checkIndependence(Node x, Node y, Set<Node> z) {
        if (z == null) {
            throw new NullPointerException();
        }

        for (Node node : z) {
            if (node == null) {
                throw new NullPointerException();
            }
        }

        boolean independent = false;

        if (this.sepset.get(x, y) != null) {
            Set<Set<Node>> condSets = this.sepset.getSet(x, y);
            for (Set<Node> condSet : condSets) {
                if (condSet.size() == z.size() && condSet.containsAll(z)) {
                    final double pValue = 1.0;

                    if (this.verbose) {
                        String message = LogUtilsSearch.independenceFactMsg(x, y, z, pValue);
                        TetradLogger.getInstance().forceLogMessage(message);
                    }
                    independent = true;
                    break;
                }
            }
        }

        if (this.verbose) {
            if (independent) {
                NumberFormat nf = NumberFormatUtil.getInstance().getNumberFormat();
                TetradLogger.getInstance().forceLogMessage(
                        LogUtilsSearch.independenceFact(x, y, z) + " score = " + LogUtilsSearch.independenceFactMsg(x, y, z, getPValue()));
            }
        }

        if (Double.isNaN(getPValue())) {
            throw new RuntimeException("Undefined p-value encountered when testing " +
                    LogUtilsSearch.independenceFact(x, y, z));
        }

        return new IndependenceResult(new IndependenceFact(x, y, z), independent, getPValue(), getAlpha() - getPValue());
    }

    /**
     * Needed for IndependenceTest interface. P value is not meaningful here.
     *
     * @return a double
     */
    public double getPValue() {
        return Double.NaN;
    }

    /**
     * <p>getVariables.</p>
     *
     * @return the list of TetradNodes over which this independence checker is capable of determinine independence
     * relations-- that is, all the variables in the given graph or the given data set.
     */
    public List<Node> getVariables() {
        return Collections.unmodifiableList(this.observedVars);
    }

    /**
     * {@inheritDoc}
     */
    public boolean determines(List<Node> z, Node x1) {
        return z.contains(x1);
    }

    /**
     * <p>getAlpha.</p>
     *
     * @return a double
     */
    public double getAlpha() {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     */
    public void setAlpha(double alpha) {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     */
    public Node getVariable(String name) {
        for (int i = 0; i < getVariables().size(); i++) {
            Node variable = getVariables().get(i);

            if (variable.getName().equals(name)) {
                return variable;
            }
        }

        return null;
    }

    /**
     * <p>getVariable.</p>
     *
     * @param node a {@link edu.cmu.tetrad.graph.Node} object
     * @return the variable associated with the given node in the graph.
     */
    public Node getVariable(Node node) {
        return this.nodesToVariables.get(node);
    }

    /**
     * <p>getNode.</p>
     *
     * @param variable a {@link edu.cmu.tetrad.graph.Node} object
     * @return the node associated with the given variable in the graph.
     */
    public Node getNode(Node variable) {
        return this.variablesToNodes.get(variable);
    }

    /**
     * <p>toString.</p>
     *
     * @return a {@link java.lang.String} object
     */
    public String toString() {
        return "D-separation";
    }

    /**
     * <p>getData.</p>
     *
     * @return a {@link edu.cmu.tetrad.data.DataSet} object
     */
    public DataSet getData() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    /**
     * <p>isVerbose.</p>
     *
     * @return a boolean
     */
    public boolean isVerbose() {
        return this.verbose;
    }

    /**
     * {@inheritDoc}
     */
    public void setVerbose(boolean verbose) {
        this.verbose = verbose;
    }

    /**
     * @return the list of observed nodes in the given graph.
     */
    private List<Node> calcObservedVars(List<Node> nodes) {
        List<Node> observedVars = new ArrayList<>();

        for (Node node : nodes) {
            if (node.getNodeType() == NodeType.MEASURED) {
                observedVars.add(getVariable(node));
            }
        }

        return observedVars;
    }
}






