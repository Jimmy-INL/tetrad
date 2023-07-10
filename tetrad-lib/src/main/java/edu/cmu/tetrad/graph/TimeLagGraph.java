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

package edu.cmu.tetrad.graph;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.*;

/**
 * Represents a time series graph--that is, a graph with a fixed number S of lags, with edges into initial lags
 * only--that is, into nodes in the first R lags, for some R. Edge structure repeats every R nodes.
 *
 * @author josephramsey
 */
public class TimeLagGraph implements Graph {
    static final long serialVersionUID = 23L;
    private final Map<String, Object> attributes = new HashMap<>();
    /**
     * Fires property change events.
     */
    private transient PropertyChangeSupport pcs;
    private EdgeListGraph graph = new EdgeListGraph();
    private int maxLag = 1;
    private int numInitialLags = 1;
    private List<Node> lag0Nodes = new ArrayList<>();
    private boolean pag;
    private boolean cpdag;
    private Paths paths;

    private Set<Triple> underLineTriples = new HashSet<>();
    private Set<Triple> dottedUnderLineTriples = new HashSet<>();
    private Set<Triple> ambiguousTriples = new HashSet<>();

    public TimeLagGraph() {
    }

    public TimeLagGraph(TimeLagGraph graph) {
        this.graph = new EdgeListGraph(graph.getGraph());
        this.maxLag = graph.getMaxLag();
        this.numInitialLags = graph.getNumInitialLags();
        this.lag0Nodes = graph.getLag0Nodes();
        this.pag = graph.pag;
        this.cpdag = graph.cpdag;
        this.paths = new Paths(this.graph);

        this.graph.addPropertyChangeListener(evt -> getPcs().firePropertyChange(evt));
    }

    /**
     * Generates a simple exemplar of this class to test serialization.
     */
    public static TimeLagGraph serializableInstance() {
        return new TimeLagGraph();
    }

    /**
     * Nodes may be added into the getModel time step only. That is, node.getLag() must be 0.
     */
    public boolean addNode(Node node) {

        NodeId id = getNodeId(node);

        if (id.getLag() != 0) {
            node = node.like(id.getName());
        }

        boolean added = getGraph().addNode(node);

        if (!this.lag0Nodes.contains(node) && !node.getName().startsWith("E_")) {
            this.lag0Nodes.add(node);
        }

        if (node.getNodeType() == NodeType.ERROR) {
            for (int i = 1; i <= getMaxLag(); i++) {
                Node node1 = node.like(id.getName() + ":" + i);

                if (i < getNumInitialLags()) {
                    getGraph().addNode(node1);
                }
            }
        } else {
            for (int i = 1; i <= getMaxLag(); i++) {
                String name = id.getName() + ":" + i;
                Node node1 = node.like(name);

                if (getGraph().getNode(name) == null) {
                    getGraph().addNode(node1);
                }
            }
        }

        getPcs().firePropertyChange("editingFinished", null, null);

        return added;
    }

    public boolean removeNode(Node node) {
        if (!containsNode(node)) {
            throw new IllegalArgumentException("That is not a node in this graph: " + node);
        }

        NodeId id = getNodeId(node);

        for (int lag = 0; lag < this.maxLag; lag++) {
            Node _node = getNode(id.getName(), lag);
            if (_node != null) {
                getGraph().removeNode(_node);
            }
            if (_node != null && lag == 0) {
                this.lag0Nodes.remove(_node);
            }
        }

        getPcs().firePropertyChange("editingFinished", null, null);

        return getGraph().containsNode(node) && getGraph().removeNode(node);
    }

    public boolean addEdge(Edge edge) {
        if (!Edges.isDirectedEdge(edge)) {
            throw new IllegalArgumentException("Only directed edges supported: " + edge);
        }

        if (!this.lag0Nodes.contains(edge.getNode2())) {
            throw new IllegalArgumentException("Edges into the current time lag only: " + edge);
        }

        Node node1 = Edges.getDirectedEdgeTail(edge);
        Node node2 = Edges.getDirectedEdgeHead(edge);

        NodeId id1 = getNodeId(node1);
        NodeId id2 = getNodeId(node2);
        int lag = id1.getLag() - id2.getLag();

        if (lag < 0) {
            throw new IllegalArgumentException("Backward edges not permitted: " + edge);
        }

        for (int _lag = getNodeId(node2).getLag() % getNumInitialLags(); _lag <= getMaxLag() - lag; _lag += getNumInitialLags()) {
            Node from = getNode(id1.getName(), _lag + lag);
            Node to = getNode(id2.getName(), _lag);

            if (from == null || to == null) {
                continue;
            }

            Edge _edge = Edges.directedEdge(from, to);

            if (!getGraph().containsEdge(_edge)) {
                getGraph().addDirectedEdge(from, to);
            }
        }

        return true;
    }

    public boolean removeEdge(Edge edge) {
        if (!Edges.isDirectedEdge(edge))
            throw new IllegalArgumentException("Only directed edges are expected in the model.");

        Node node1 = Edges.getDirectedEdgeTail(edge);
        Node node2 = Edges.getDirectedEdgeHead(edge);

        NodeId id1 = getNodeId(node1);
        NodeId id2 = getNodeId(node2);
        int lag = id1.getLag() - id2.getLag();

        boolean removed = false;

        for (int _lag = 0; _lag <= getMaxLag(); _lag++) {
            Node from = getNode(id1.getName(), _lag + lag);
            Node to = getNode(id2.getName(), _lag);

            if (from != null && to != null) {
                Edge _edge = getGraph().getEdge(from, to);

                if (_edge != null) {
                    boolean b = getGraph().removeEdge(_edge);
                    removed = removed || b;
                }
            }
        }

        return removed;
    }

    public boolean setMaxLag(int maxLag) {
        if (maxLag < 0) {
            throw new IllegalArgumentException("Max lag must be at least 0: " + maxLag);
        }

        List<Node> lag0Nodes = getLag0Nodes();

        boolean changed = false;

        if (maxLag > this.getMaxLag()) {
            this.maxLag = maxLag;
            for (Node node : lag0Nodes) {
                addNode(node);
            }

            for (Node node : lag0Nodes) {
                List<Edge> edges = getGraph().getEdges(node);

                for (Edge edge : edges) {
                    boolean b = addEdge(edge);
                    changed = changed || b;
                }
            }
        } else if (maxLag < this.getMaxLag()) {
            for (Node node : lag0Nodes) {
                List<Edge> edges = getGraph().getEdges(node);

                for (Edge edge : edges) {
                    Node tail = Edges.getDirectedEdgeTail(edge);

                    if (getNodeId(tail).getLag() > maxLag) {
                        getGraph().removeEdge(edge);
                    }
                }
            }

            for (Node _node : getNodes()) {
                if (getNodeId(_node).getLag() > maxLag) {
                    boolean b = getGraph().removeNode(_node);
                    changed = changed || b;
                }
            }

            this.maxLag = maxLag;
        }

        getPcs().firePropertyChange("editingFinished", null, null);

        return changed;
    }

    public boolean removeHighLagEdges(int maxLag) {
        List<Node> lag0Nodes = getLag0Nodes();
        boolean changed = false;

        for (Node node : lag0Nodes) {
            List<Edge> edges = getGraph().getEdges(node);

            for (Edge edge : new ArrayList<>(edges)) {
                Node tail = Edges.getDirectedEdgeTail(edge);

                if (getNodeId(tail).getLag() > maxLag) {
                    boolean b = getGraph().removeEdge(edge);
                    changed = changed || b;
                }
            }
        }

        return changed;
    }

    public boolean setNumInitialLags(int numInitialLags) {
        if (numInitialLags < 1) {
            throw new IllegalArgumentException("The number of initial lags must be at least 1: " + numInitialLags);
        }

        if (numInitialLags == this.numInitialLags) return false;

        List<Node> lag0Nodes = getLag0Nodes();
        boolean changed = false;

        for (Node node : lag0Nodes) {
            NodeId id = getNodeId(node);

            for (int lag = 1; lag <= getMaxLag(); lag++) {
                Node _node = getNode(id.getName(), lag);
                List<Node> nodesInto = getGraph().getNodesInTo(_node, Endpoint.ARROW);

                for (Node _node2 : nodesInto) {
                    Edge edge = Edges.directedEdge(_node2, _node);
                    boolean b = getGraph().removeEdge(edge);
                    changed = changed || b;
                }
            }
        }

        this.numInitialLags = numInitialLags;

        for (Node node : lag0Nodes) {
            for (int lag = 0; lag < numInitialLags; lag++) {
                List<Edge> edges = getGraph().getEdges(node);

                for (Edge edge : edges) {
                    boolean b = addEdge(edge);
                    changed = changed || b;
                }
            }
        }

        getPcs().firePropertyChange("editingFinished", null, null);

        return changed;
    }

    public NodeId getNodeId(Node node) {
        String _name = node.getName();
        String[] tokens = _name.split(":");
        if (tokens.length > 2) throw new IllegalArgumentException("Name may contain only one colon: " + _name);
        if (tokens[0].length() == 0) throw new IllegalArgumentException("Part to the left of the colon may " +
                "not be empty; that's the name of the variable: " + _name);
        String name = tokens[0];
        int lag;

        if (tokens.length == 1) {
            lag = 0;
        } else {
            lag = Integer.parseInt(tokens[1]);
            if (lag == 0) throw new IllegalArgumentException("Lag 0 edges don't have :0 descriptors");
        }

        if (lag < 0) throw new IllegalArgumentException("Lag is less than 0: " + lag);
        if (lag > getMaxLag()) throw new IllegalArgumentException("Lag is greater than the maximum lag: " + lag);

        return new NodeId(name, lag);
    }

    public Node getNode(String name, int lag) {
        if (name.length() == 0) throw new IllegalArgumentException("Empty node name: " + name);
        if (lag < 0) throw new IllegalArgumentException("Negative lag: " + lag);

        String _name;

        if (lag == 0) {
            _name = name;
        } else {
            _name = name + ":" + lag;
        }

        return getNode(_name);
    }

    public List<Node> getLag0Nodes() {
        return new ArrayList<>(this.lag0Nodes);
    }

    private EdgeListGraph getGraph() {
        return this.graph;
    }

    public int getMaxLag() {
        return this.maxLag;
    }

    public int getNumInitialLags() {
        return this.numInitialLags;
    }

    public String toString() {
        return getGraph().toString() + "\n" + this.lag0Nodes;
    }

    public boolean addDirectedEdge(Node node1, Node node2) {
        return this.graph.addDirectedEdge(node1, node2);
    }

    public boolean addUndirectedEdge(Node node1, Node node2) {
        throw new UnsupportedOperationException("Undirected edges not currently supported.");
    }

    public boolean addNondirectedEdge(Node node1, Node node2) {
        throw new UnsupportedOperationException("Nondireced edges not supported.");
    }

    public boolean addPartiallyOrientedEdge(Node node1, Node node2) {
        throw new UnsupportedOperationException("Partially oriented edges not supported.");
    }

    public boolean addBidirectedEdge(Node node1, Node node2) {
        throw new UnsupportedOperationException("Bidireced edges not currently supported.");
    }

    public boolean isDefNoncollider(Node node1, Node node2, Node node3) {
        return getGraph().isDefNoncollider(node1, node2, node3);
    }

    public boolean isDefCollider(Node node1, Node node2, Node node3) {
        return getGraph().isDefCollider(node1, node2, node3);
    }

    public List<Node> getChildren(Node node) {
        return getGraph().getChildren(node);
    }

    public int getDegree() {
        return getGraph().getDegree();
    }

    public Edge getEdge(Node node1, Node node2) {
        return getGraph().getEdge(node1, node2);
    }

    public Edge getDirectedEdge(Node node1, Node node2) {
        return getGraph().getDirectedEdge(node1, node2);
    }

    public List<Node> getParents(Node node) {
        return getGraph().getParents(node);
    }

    public int getIndegree(Node node) {
        return getGraph().getIndegree(node);
    }

    @Override
    public int getDegree(Node node) {
        return getGraph().getDegree(node);
    }

    public int getOutdegree(Node node) {
        return getGraph().getOutdegree(node);
    }

    public boolean isAdjacentTo(Node node1, Node node2) {
        return getGraph().isAdjacentTo(node1, node2);
    }

    public boolean isChildOf(Node node1, Node node2) {
        return getGraph().isChildOf(node1, node2);
    }

    @Override
    public boolean isParentOf(Node node1, Node node2) {
        return graph.isParentOf(node1, node2);
    }

    public void transferNodesAndEdges(Graph graph) throws IllegalArgumentException {
        getGraph().transferNodesAndEdges(graph);
    }

    public void transferAttributes(Graph graph) throws IllegalArgumentException {
        getGraph().transferAttributes(graph);
    }

    @Override
    public Paths paths() {
        return this.paths;
    }

    public boolean isParameterizable(Node node) {
        return getNodeId(node).getLag() < getNumInitialLags();
    }

    public boolean isTimeLagModel() {
        return true;
    }

    public TimeLagGraph getTimeLagGraph() {
        return this;
    }

    @Override
    public Set<Node> getSepset(Node n1, Node n2) {
        return this.graph.getSepset(n1, n2);
    }

    public boolean isExogenous(Node node) {
        return getGraph().isExogenous(node);
    }

    public List<Node> getAdjacentNodes(Node node) {
        return getGraph().getAdjacentNodes(node);
    }

    public Endpoint getEndpoint(Node node1, Node node2) {
        return getGraph().getEndpoint(node1, node2);
    }

    public boolean setEndpoint(Node from, Node to, Endpoint endPoint) throws IllegalArgumentException {
        return getGraph().setEndpoint(from, to, endPoint);
    }

    public List<Node> getNodesInTo(Node node, Endpoint endpoint) {
        return getGraph().getNodesInTo(node, endpoint);
    }

    public List<Node> getNodesOutTo(Node node, Endpoint endpoint) {
        return getGraph().getNodesOutTo(node, endpoint);
    }

    public void addPropertyChangeListener(PropertyChangeListener l) {
        getPcs().addPropertyChangeListener(l);
        getGraph().addPropertyChangeListener(l);
    }

    public Set<Edge> getEdges() {
        return getGraph().getEdges();
    }

    public boolean containsEdge(Edge edge) {
        return getGraph().containsEdge(edge);
    }

    public boolean containsNode(Node node) {
        return getGraph().containsNode(node);
    }

    public List<Edge> getEdges(Node node) {
        if (getGraph().containsNode(node)) {
            return getGraph().getEdges(node);
        } else {
            return null;
        }
    }

    public List<Edge> getEdges(Node node1, Node node2) {
        return getGraph().getEdges(node1, node2);
    }

    public int hashCode() {
        return getGraph().hashCode();
    }

    public boolean equals(Object o) {
        if (!(o instanceof Graph)) return false;
        return getGraph().equals(o);
    }

    public void fullyConnect(Endpoint endpoint) {
        getGraph().fullyConnect(endpoint);
    }

    public void reorientAllWith(Endpoint endpoint) {
        getGraph().reorientAllWith(endpoint);
    }

    public Node getNode(String name) {
        return getGraph().getNode(name);
    }

    public int getNumNodes() {
        return getGraph().getNumNodes();
    }

    public int getNumEdges() {
        return getGraph().getNumEdges();
    }

    public int getNumEdges(Node node) {
        return getGraph().getNumEdges(node);
    }

    public Graph subgraph(List<Node> nodes) {
        return getGraph().subgraph(nodes);
    }

    public List<Node> getNodes() {
        return getGraph().getNodes();
    }

    @Override
    public void setNodes(List<Node> nodes) {
        throw new IllegalArgumentException("Sorry, you cannot replace the variables for a time lag graph.");
    }

    public List<String> getNodeNames() {
        return getGraph().getNodeNames();
    }

    public void clear() {
        getGraph().clear();
    }

    public boolean removeEdge(Node node1, Node node2) {
        return removeEdge(getEdge(node1, node2));
    }

    public boolean removeEdges(Collection<Edge> edges) {
        boolean change = false;

        for (Edge edge : edges) {
            boolean _change = removeEdge(edge);
            change = change || _change;
        }

        return change;
    }

    public boolean removeNodes(List<Node> nodes) {
        return getGraph().removeNodes(nodes);
    }

    public boolean removeEdges(Node node1, Node node2) {
        return removeEdges(getEdges(node1, node2));
    }

    /**
     * @return this object.
     */
    private PropertyChangeSupport getPcs() {
        if (this.pcs == null) {
            this.pcs = new PropertyChangeSupport(this);
        }
        return this.pcs;
    }

    @Override
    public Map<String, Object> getAllAttributes() {
        return this.attributes;
    }

    @Override
    public Object getAttribute(String key) {
        return this.attributes.get(key);
    }

    @Override
    public void removeAttribute(String key) {
        this.attributes.remove(key);
    }

    @Override
    public void addAttribute(String key, Object value) {
        this.attributes.put(key, value);
    }

    public Set<Triple> getAmbiguousTriples() {
        return new HashSet<>(this.ambiguousTriples);
    }

    public void setAmbiguousTriples(Set<Triple> triples) {
        this.ambiguousTriples.clear();

        for (Triple triple : triples) {
            addAmbiguousTriple(triple.getX(), triple.getY(), triple.getZ());
        }
    }

    public Set<Triple> getUnderLines() {
        return new HashSet<>(this.underLineTriples);
    }

    public Set<Triple> getDottedUnderlines() {
        return new HashSet<>(this.dottedUnderLineTriples);
    }

    /**
     * States whether r-s-r is an underline triple or not.
     */
    public boolean isAmbiguousTriple(Node x, Node y, Node z) {
        return this.ambiguousTriples.contains(new Triple(x, y, z));
    }

    /**
     * States whether r-s-r is an underline triple or not.
     */
    public boolean isUnderlineTriple(Node x, Node y, Node z) {
        return this.underLineTriples.contains(new Triple(x, y, z));
    }

    public void addAmbiguousTriple(Node x, Node y, Node z) {
        this.ambiguousTriples.add(new Triple(x, y, z));
    }

    public void addUnderlineTriple(Node x, Node y, Node z) {
        Triple triple = new Triple(x, y, z);

        if (!triple.alongPathIn(this)) {
            return;
        }

        this.underLineTriples.add(new Triple(x, y, z));
    }

    public void addDottedUnderlineTriple(Node x, Node y, Node z) {
        Triple triple = new Triple(x, y, z);

        if (!triple.alongPathIn(this)) {
            return;
        }

        this.dottedUnderLineTriples.add(triple);
    }

    public void removeAmbiguousTriple(Node x, Node y, Node z) {
        this.ambiguousTriples.remove(new Triple(x, y, z));
    }

    public void removeUnderlineTriple(Node x, Node y, Node z) {
        this.underLineTriples.remove(new Triple(x, y, z));
    }

    public void removeDottedUnderlineTriple(Node x, Node y, Node z) {
        this.dottedUnderLineTriples.remove(new Triple(x, y, z));
    }

    public void setUnderLineTriples(Set<Triple> triples) {
        this.underLineTriples.clear();

        for (Triple triple : triples) {
            addUnderlineTriple(triple.getX(), triple.getY(), triple.getZ());
        }
    }

    public void setDottedUnderLineTriples(Set<Triple> triples) {
        this.dottedUnderLineTriples.clear();

        for (Triple triple : triples) {
            addDottedUnderlineTriple(triple.getX(), triple.getY(), triple.getZ());
        }
    }

    public void removeTriplesNotInGraph() {
        for (Triple triple : new HashSet<>(this.ambiguousTriples)) {
            if (!containsNode(triple.getX()) || !containsNode(triple.getY())
                    || !containsNode(triple.getZ())) {
                this.ambiguousTriples.remove(triple);
                continue;
            }

            if (!isAdjacentTo(triple.getX(), triple.getY())
                    || !isAdjacentTo(triple.getY(), triple.getZ())) {
                this.ambiguousTriples.remove(triple);
            }
        }

        for (Triple triple : new HashSet<>(this.underLineTriples)) {
            if (!containsNode(triple.getX()) || !containsNode(triple.getY())
                    || !containsNode(triple.getZ())) {
                this.underLineTriples.remove(triple);
                continue;
            }

            if (!isAdjacentTo(triple.getX(), triple.getY()) || !isAdjacentTo(triple.getY(), triple.getZ())) {
                this.underLineTriples.remove(triple);
            }
        }

        for (Triple triple : new HashSet<>(this.dottedUnderLineTriples)) {
            if (!containsNode(triple.getX()) || !containsNode(triple.getY()) || !containsNode(triple.getZ())) {
                this.dottedUnderLineTriples.remove(triple);
                continue;
            }

            if (!isAdjacentTo(triple.getX(), triple.getY()) || isAdjacentTo(triple.getY(), triple.getZ())) {
                this.dottedUnderLineTriples.remove(triple);
            }
        }
    }

    public static class NodeId {
        private final String name;
        private final int lag;

        public NodeId(String name, int lag) {
            this.name = name;
            this.lag = lag;
        }

        public String getName() {
            return this.name;
        }

        public int getLag() {
            return this.lag;
        }
    }
}



