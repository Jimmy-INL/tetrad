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

import edu.cmu.tetrad.data.Knowledge;
import edu.cmu.tetrad.data.KnowledgeBoxInput;
import edu.cmu.tetrad.graph.Edge;
import edu.cmu.tetrad.graph.Edges;
import edu.cmu.tetrad.graph.Graph;
import edu.cmu.tetrad.graph.Node;
import edu.cmu.tetrad.util.Parameters;
import edu.cmu.tetrad.util.TetradLogger;
import edu.cmu.tetrad.util.TetradSerializableUtils;

import java.util.List;

/**
 * @author kaalpurush
 */
public class RequiredGraphModel extends KnowledgeBoxModel {

    private static final long serialVersionUID = 23L;

    private final Graph resultGraph;

    public RequiredGraphModel(BayesPmWrapper wrapper, Parameters params) {
        this((KnowledgeBoxInput) wrapper, params);
    }

    public RequiredGraphModel(GraphWrapper wrapper, Parameters params) {
        this((KnowledgeBoxInput) wrapper, params);
    }

    public RequiredGraphModel(StandardizedSemImWrapper wrapper, Parameters params) {
        this((KnowledgeBoxInput) wrapper, params);
    }

    public RequiredGraphModel(SemImWrapper wrapper, Parameters params) {
        this((KnowledgeBoxInput) wrapper, params);
    }

    public RequiredGraphModel(SemPmWrapper wrapper, Parameters params) {
        this((KnowledgeBoxInput) wrapper, params);
    }

    public RequiredGraphModel(DataWrapper wrapper, Parameters params) {
        this((KnowledgeBoxInput) wrapper, params);
    }

    public RequiredGraphModel(TimeLagGraphWrapper wrapper, Parameters params) {
        this((KnowledgeBoxInput) wrapper, params);
    }

    public RequiredGraphModel(GeneralizedSemImWrapper wrapper, Parameters params) {
        this((KnowledgeBoxInput) wrapper, params);
    }

    public RequiredGraphModel(BayesImWrapper wrapper, Parameters params) {
        this((KnowledgeBoxInput) wrapper, params);
    }

    public RequiredGraphModel(SemGraphWrapper wrapper, Parameters params) {
        this((KnowledgeBoxInput) wrapper, params);
    }

    public RequiredGraphModel(GeneralizedSemPmWrapper wrapper, Parameters params) {
        this((KnowledgeBoxInput) wrapper, params);
    }

    public RequiredGraphModel(DagWrapper wrapper, Parameters params) {
        this((KnowledgeBoxInput) wrapper, params);
    }

    public RequiredGraphModel(DirichletBayesImWrapper wrapper, Parameters params) {
        this((KnowledgeBoxInput) wrapper, params);
    }

    public RequiredGraphModel(BuildPureClustersRunner wrapper, Parameters params) {
        this((KnowledgeBoxInput) wrapper, params);
    }

    public RequiredGraphModel(PurifyRunner wrapper, Parameters params) {
        this((KnowledgeBoxInput) wrapper, params);
    }

    public RequiredGraphModel(MeasurementModelWrapper wrapper, Parameters params) {
        this((KnowledgeBoxInput) wrapper, params);
    }

    public RequiredGraphModel(KnowledgeBoxInput input, Parameters params) {
        this(params, input);
    }

    /**
     * Constructor from dataWrapper edge
     */
    public RequiredGraphModel(Parameters params, KnowledgeBoxInput input) {
        super(new KnowledgeBoxInput[]{input}, params);

        if (input == null) {
            throw new NullPointerException();
        }

        this.resultGraph = input.getResultGraph();

        createKnowledge();

        TetradLogger.getInstance().log("info", "Knowledge");

        // This is a conundrum. At this point I dont know whether I am in a
        // simulation or not. If in a simulation, I should print the knowledge.
        // If not, I should wait for resetParams to be called. For now I'm
        // printing the knowledge if it's not empty.
        if (!((Knowledge) params.get("knowledge", new Knowledge())).isEmpty()) {
            TetradLogger.getInstance().log("knowledge", params.get("knowledge", new Knowledge()).toString());
        }
    }

    /**
     * Generates a simple exemplar of this class to test serialization.
     *
     * @see TetradSerializableUtils
     */
    public static RequiredGraphModel serializableInstance() {
        return new RequiredGraphModel(new Parameters(), GraphWrapper.serializableInstance());
    }

    private void createKnowledge() {
        Knowledge knwl = getKnowledge();
        if (knwl == null) {
            return;
        }

        knwl.clear();

        if (this.resultGraph == null) {
            throw new NullPointerException("I couldn't find a parent graph.");
        }

        List<Node> nodes = this.resultGraph.getNodes();

        int numOfNodes = nodes.size();
        for (int i = 0; i < numOfNodes; i++) {
            for (int j = i + 1; j < numOfNodes; j++) {
                Node n1 = nodes.get(i);
                Node n2 = nodes.get(j);

                if (n1.getName().startsWith("E_") || n2.getName().startsWith("E_")) {
                    continue;
                }

                Edge edge = this.resultGraph.getEdge(n1, n2);

                if (edge != null) {
                    if (edge.isDirected()) {
                        knwl.setRequired(edge.getNode1().getName(), edge.getNode2().getName());
                    } else if (Edges.isUndirectedEdge(edge)) {
                        knwl.setRequired(n1.getName(), n2.getName());
                        knwl.setRequired(n2.getName(), n1.getName());
                    }
                }
            }
        }
    }

    public Graph getResultGraph() {
        return this.resultGraph;
    }
}
