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

package edu.cmu.tetradapp.model;

import edu.cmu.tetrad.calculator.expression.Expression;
import edu.cmu.tetrad.calculator.expression.VariableExpression;
import edu.cmu.tetrad.data.KnowledgeBoxInput;
import edu.cmu.tetrad.graph.*;
import edu.cmu.tetrad.search.IndependenceTest;
import edu.cmu.tetrad.search.test.MsepTest;
import edu.cmu.tetrad.sem.GeneralizedSemIm;
import edu.cmu.tetrad.sem.GeneralizedSemPm;
import edu.cmu.tetrad.session.SimulationParamsSource;
import edu.cmu.tetrad.util.Parameters;
import edu.cmu.tetrad.util.TetradLogger;
import edu.cmu.tetrad.util.TetradSerializableUtils;
import edu.cmu.tetradapp.util.IonInput;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Holds a tetrad-style graph with all of the constructors necessary for it to serve as a model for the tetrad
 * application.
 *
 * @author josephramsey
 */
public class GraphWrapper implements KnowledgeBoxInput, IonInput, IndTestProducer,
        SimulationParamsSource, GraphSettable, MultipleGraphSource {
    private static final long serialVersionUID = 23L;
    private int numModels = 1;
    private int modelIndex;
    private String modelSourceName;

    /**
     * @serial Can be null.
     */
    private String name;

    /**
     * @serial Cannot be null.
     */
    private List<Graph> graphs;
    private Map<String, String> allParamSettings;
    private Parameters parameters;

    //=============================CONSTRUCTORS==========================//

    private GraphWrapper() {
    }

    public GraphWrapper(GraphSource graphSource, Parameters parameters) {
        this.parameters = parameters;

        if (graphSource instanceof Simulation) {
            Simulation simulation = (Simulation) graphSource;
            this.graphs = simulation.getGraphs();
            this.numModels = this.graphs.size();
            this.modelIndex = 0;
            this.modelSourceName = simulation.getName();
        } else {
            setGraph(new EdgeListGraph(graphSource.getGraph()));
        }

 //       log();
    }

    public GraphWrapper(Graph graph) {
        if (graph == null) {
            throw new NullPointerException("Graph must not be null.");
        }
        setGraph(graph);
 //       log();
    }

    public GraphWrapper(Graph graph, String message) {
        TetradLogger.getInstance().log("info", message);

        if (graph == null) {
            throw new NullPointerException("Graph must not be null.");
        }

        setGraph(graph);
    }

    public GraphWrapper(Parameters parameters) {
        this.parameters = parameters;
        setGraph(new EdgeListGraph());
//        log();
    }

    public GraphWrapper(Simulation simulation, Parameters parameters) {
        this.parameters = parameters;
        this.graphs = simulation.getGraphs();
        this.numModels = this.graphs.size();
        this.modelIndex = 0;
        this.modelSourceName = simulation.getName();

//        log();
    }

    public GraphWrapper(DataWrapper wrapper) {
        if (wrapper instanceof Simulation) {
            Simulation simulation = (Simulation) wrapper;
            this.graphs = simulation.getGraphs();
            this.numModels = this.graphs.size();
            this.modelIndex = 0;
            this.modelSourceName = simulation.getName();
        } else {
            setGraph(new EdgeListGraph(wrapper.getVariables()));
        }

        LayoutUtil.defaultLayout(getGraph());
    }

    public GraphWrapper(GeneralizedSemImWrapper wrapper) {
        this(GraphWrapper.getStrongestInfluenceGraph(wrapper.getSemIms().get(0)));
        if (wrapper.getSemIms() == null || wrapper.getSemIms().size() > 1) {
            throw new IllegalArgumentException("I'm sorry; this editor can only edit a single generalized SEM IM.");
        }
    }

    /**
     * Generates a simple exemplar of this class to test serialization.
     *
     * @see TetradSerializableUtils
     */
    public static GraphWrapper serializableInstance() {
        return new GraphWrapper(Dag.serializableInstance());
    }

    //==============================PUBLIC METHODS======================//

    private static String findParameter(Expression expression, String name) {
        List<Expression> expressions = expression.getExpressions();

        if (expression.getToken().equals("*")) {
            Expression expression1 = expressions.get(1);
            VariableExpression varExpr = (VariableExpression) expression1;

            if (varExpr.getVariable().equals(name)) {
                Expression expression2 = expressions.get(0);
                VariableExpression constExpr = (VariableExpression) expression2;
                return constExpr.getVariable();
            }
        }

        for (Expression _expression : expressions) {
            String param = GraphWrapper.findParameter(_expression, name);

            if (param != null) {
                return param;
            }
        }

        return null;
    }

    private static Graph getStrongestInfluenceGraph(GeneralizedSemIm im) {
        GeneralizedSemPm pm = im.getGeneralizedSemPm();
        Graph imGraph = im.getGeneralizedSemPm().getGraph();

        List<Node> nodes = new ArrayList<>();

        for (Node node : imGraph.getNodes()) {
            if (!(node.getNodeType() == NodeType.ERROR)) {
                nodes.add(node);
            }
        }

        Graph graph2 = new EdgeListGraph(nodes);

        for (Edge edge : imGraph.getEdges()) {
            Node node1 = edge.getNode1();
            Node node2 = edge.getNode2();

            if (!graph2.containsNode(node1)) continue;
            if (!graph2.containsNode(node2)) continue;

            if (graph2.isAdjacentTo(node1, node2)) {
                continue;
            }

            List<Edge> edges = imGraph.getEdges(node1, node2);

            if (edges.size() == 1) {
                graph2.addEdge(edges.iterator().next());
            } else {
                Expression expression1 = pm.getNodeExpression(node1);
                Expression expression2 = pm.getNodeExpression(node2);

                String param1 = GraphWrapper.findParameter(expression1, node2.getName());
                String param2 = GraphWrapper.findParameter(expression2, node1.getName());

                if (param1 == null || param2 == null) {
                    continue;
                }

                double value1 = im.getParameterValue(param1);
                double value2 = im.getParameterValue(param2);

                if (value2 > value1) {
                    graph2.addDirectedEdge(node1, node2);
                } else if (value1 > value2) {
                    graph2.addDirectedEdge(node2, node1);
                }
            }

        }

        return graph2;
    }

    public Graph getGraph() {
        return this.graphs.get(getModelIndex());
    }

    public void setGraph(Graph graph) {
        this.graphs = new ArrayList<>();
        this.graphs.add(new EdgeListGraph(graph));
 //       log();
    }

    public boolean allowRandomGraph() {
        return true;
    }

    @Override
    public IndependenceTest getIndependenceTest() {
        return new MsepTest(getGraph());
    }

    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Graph getSourceGraph() {
        return getGraph();
    }

    public Graph getResultGraph() {
        return getGraph();
    }

    public List<String> getVariableNames() {
        return getGraph().getNodeNames();
    }

    public List<Node> getVariables() {
        return getGraph().getNodes();
    }

    @Override
    public Map<String, String> getParamSettings() {
        Map<String, String> paramSettings = new HashMap<>();
        paramSettings.put("# Vars", Integer.toString(getGraph().getNumNodes()));
        paramSettings.put("# Edges", Integer.toString(getGraph().getNumEdges()));
        if (getGraph().paths().existsDirectedCycle()) paramSettings.put("Cyclic", null);
        return paramSettings;
    }

    @Override
    public Map<String, String> getAllParamSettings() {
        return this.allParamSettings;
    }

    //==========================PRIVATE METaHODS===========================//

    @Override
    public void setAllParamSettings(Map<String, String> paramSettings) {
        this.allParamSettings = paramSettings;
    }

    public Parameters getParameters() {
        return this.parameters;
    }

//    private void log() {
//        TetradLogger.getInstance().log("info", "General Graph");
//        TetradLogger.getInstance().log("graph", "" + getGraph());
//    }

    /**
     * Adds semantic checks to the default deserialization method. This method must have the standard signature for a
     * readObject method, and the body of the method must begin with "s.defaultReadObject();". Other than that, any
     * semantic checks can be specified and do not need to stay the same from version to version. A readObject method of
     * this form may be added to any class, even if Tetrad sessions were previously saved out using a version of the
     * class that didn't include it. (That's what the "s.defaultReadObject();" is for. See J. Bloch, Effective Java, for
     * help.
     */
    private void readObject(ObjectInputStream s)
            throws IOException, ClassNotFoundException {
        s.defaultReadObject();
    }

    public int getNumModels() {
        return this.numModels;
    }

    public int getModelIndex() {
        return this.modelIndex;
    }

    public void setModelIndex(int modelIndex) {
        this.modelIndex = modelIndex;
    }

    public String getModelSourceName() {
        return this.modelSourceName;
    }

    @Override
    public List<Graph> getGraphs() {
        return this.graphs;
    }
}





