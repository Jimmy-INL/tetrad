///////////////////////////////////////////////////////////////////////////////
// For information as to what this class does, see the Javadoc, below.       //
// Copyright (C) 1998, 1999, 2000, 2001, 2002, 2003, 2004, 2005, 2006,       //
// 2007, 2008, 2009, 2010, 2014, 2015 by Peter Spirtes, Richard Scheines, Joseph   //
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

package edu.cmu.tetradapp.model;

import edu.cmu.tetrad.graph.*;
import edu.cmu.tetrad.session.Session;
import edu.cmu.tetrad.session.SessionAdapter;
import edu.cmu.tetrad.session.SessionEvent;
import edu.cmu.tetrad.session.SessionNode;
import edu.cmu.tetrad.util.JOptionUtils;
import edu.cmu.tetrad.util.TetradSerializableUtils;
import edu.cmu.tetradapp.util.SessionWrapperIndirectRef;

import javax.swing.*;
import java.awt.*;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.List;
import java.util.*;

/**
 * Wraps a Session as a Graph so that an AbstractWorkbench can be used to edit
 * it.
 *
 * @author Joseph Ramsey jdramsey@andrew.cmu.edu
 * @see edu.cmu.tetrad.session.Session
 * @see edu.cmu.tetrad.graph.Graph
 */
public class SessionWrapper extends EdgeListGraph implements SessionWrapperIndirectRef {
    static final long serialVersionUID = 23L;

    /**
     * The session being wrapped.
     *
     * @serial Cannot be null.
     */
    private final Session session;

    /**
     * The set of SessionNodeWrappers.
     *
     * @serial Cannot be null.
     */
    private final Set<Node> sessionNodeWrappers = new HashSet<>();

    /**
     * The set of SessionEdges.
     *
     * @serial Cannot be null.
     */
    private final Set<Edge> sessionEdges = new HashSet<>();

    /**
     * The property change support.
     */
    private transient PropertyChangeSupport propertyChangeSupport;

    /**
     * Handles incoming session events, basically by redirecting to any
     * listeners of this session.
     */
    private transient SessionHandler sessionHandler;
    private final boolean highlighted = false;
    private boolean pag;
    private boolean CPDAG;

    //==========================CONSTRUCTORS=======================//

    /**
     * Constructs a new session with the given name.
     */
    public SessionWrapper(final Session session) {
        if (session == null) {
            throw new NullPointerException("Session must not be null.");
        }
        this.session = session;
        this.session.addSessionListener(getSessionHandler());
    }

    /**
     * Generates a simple exemplar of this class to test serialization.
     *
     * @see TetradSerializableUtils
     */
    public static SessionWrapper serializableInstance() {
        return new SessionWrapper(Session.serializableInstance());
    }

    //==================PUBLIC METHODS IMPLEMENTING Graph===========//

    /**
     * Adds an edge to the workbench (cast as indicated) and fires a
     * PropertyChangeEvent, property "edgeAdded," with the new edge as the
     * newValue. The nodes connected by the edge must both be
     * SessionNodeWrappers that already lie in the workbench.
     *
     * @param edge the edge to be added.
     * @return true if the edge was added, false if not.
     */
    public boolean addEdge(final Edge edge) {
        final SessionNodeWrapper from =
                (SessionNodeWrapper) Edges.getDirectedEdgeTail(edge);
        final SessionNodeWrapper to =
                (SessionNodeWrapper) Edges.getDirectedEdgeHead(edge);
        final SessionNode parent = from.getSessionNode();
        final SessionNode child = to.getSessionNode();

        final boolean added = child.addParent2(parent);
//        boolean added = child.addParent(parent);

        if (added) {
            this.sessionEdges.add(edge);
            getPropertyChangeSupport().firePropertyChange("edgeAdded", null,
                    edge);
            return true;
        } else {
            return false;
        }
    }

    /**
     * Adds a PropertyChangeListener to the workbench.
     *
     * @param e the property change listener.
     */
    public void addPropertyChangeListener(final PropertyChangeListener e) {
        getPropertyChangeSupport().addPropertyChangeListener(e);
    }

    /**
     * Adds a node to the workbench and fires a PropertyChangeEvent for property
     * "nodeAdded" with the new node as the new value.
     *
     * @param node the node to be added.
     * @return true if nodes were added, false if not.
     */
    public boolean addNode(final Node node) {
        final SessionNodeWrapper wrapper = (SessionNodeWrapper) node;
        final SessionNode sessionNode = wrapper.getSessionNode();

        try {
            this.session.addNode(sessionNode);
            this.sessionNodeWrappers.add(node);
            getPropertyChangeSupport().firePropertyChange("nodeAdded", null,
                    node);
            return true;
        } catch (final IllegalArgumentException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Pastes a list of session elements (SessionNodeWrappers and SessionEdges)
     * into the workbench.
     */
    public void pasteSubsession(final List sessionElements, final Point upperLeft) {

        // Extract the SessionNodes from the SessionNodeWrappers
        // and pass the list of them to the Session.  Choose a unique
        // name for each of the session wrappers.
        final List<SessionNode> sessionNodes = new ArrayList<>();
        final List<SessionNodeWrapper> sessionNodeWrappers =
                new ArrayList<>();
        final List<Edge> sessionEdges = new ArrayList<>();

        final Point oldUpperLeft = EditorUtils.getTopLeftPoint(sessionElements);
        final int deltaX = upperLeft.x - oldUpperLeft.x;
        final int deltaY = upperLeft.y - oldUpperLeft.y;

        for (final Object sessionElement : sessionElements) {
            if (sessionElement instanceof SessionNodeWrapper) {
                final SessionNodeWrapper wrapper =
                        (SessionNodeWrapper) sessionElement;
                sessionNodeWrappers.add(wrapper);
                adjustNameAndPosition(wrapper, sessionNodeWrappers, deltaX,
                        deltaY);
                final SessionNode sessionNode = wrapper.getSessionNode();
                sessionNodes.add(sessionNode);
            } else if (sessionElement instanceof Edge) {
                sessionEdges.add((Edge) sessionElement);
            } else {
                throw new IllegalArgumentException("The list of session " +
                        "elements should contain only SessionNodeWrappers " +
                        "and SessionEdges: " + sessionElement);
            }
        }

        // KEY STEP: Add the session nodes to the session.
        try {
            this.session.addNodeList(sessionNodes);
        } catch (final Exception e) {
            throw new RuntimeException("There was an error when trying to " +
                    "add session nodes to the session.", e);
        }

        // If that worked, go ahead and put the session node wrappers and
        // session edges into the main data structures and throw events
        // for everything.
        this.sessionNodeWrappers.addAll(sessionNodeWrappers);
        this.sessionEdges.addAll(sessionEdges);

        for (final Object sessionNodeWrapper : sessionNodeWrappers) {
            final Node node = (Node) sessionNodeWrapper;
            getPropertyChangeSupport().firePropertyChange("nodeAdded", null,
                    node);
        }

        for (final Object sessionEdge : sessionEdges) {
            final Edge edge = (Edge) sessionEdge;
            getPropertyChangeSupport().firePropertyChange("edgeAdded", null,
                    edge);
        }
    }

    /**
     * Indirect reference to session handler to avoid saving out listeners
     * during serialization.
     */
    private SessionHandler getSessionHandler() {
        if (this.sessionHandler == null) {
            this.sessionHandler = new SessionHandler();
        }

        return this.sessionHandler;
    }

    /**
     * Adjusts the name to avoid name conflicts in the new session and, if the
     * name is adjusted, adjusts the position so the user can see the two
     * nodes.
     *
     * @param wrapper             The wrapper which is being adjusted
     * @param sessionNodeWrappers a list of wrappers that the name might
     * @param deltaX              the shift in x
     * @param deltaY              the shift in y.
     */
    private void adjustNameAndPosition(final SessionNodeWrapper wrapper,
                                       final List sessionNodeWrappers, final int deltaX, final int deltaY) {
        final String originalName = wrapper.getSessionName();
        final String base = extractBase(originalName);
        final String uniqueName = nextUniqueName(base, sessionNodeWrappers);

        if (!uniqueName.equals(originalName)) {
            wrapper.setSessionName(uniqueName);
            wrapper.setCenterX(wrapper.getCenterX() + deltaX);
            wrapper.setCenterY(wrapper.getCenterY() + deltaY);
        }
    }

    /**
     * @return the substring of <code>name</code> up to but not including a
     * contiguous string of digits at the end. For example, given "Graph123"
     */
    private String extractBase(final String name) {

        if (name == null) {
            throw new NullPointerException("Name must not be null.");
        }

        final StringBuilder buffer = new StringBuilder(name);

        for (int i = buffer.length() - 1; i >= 0; i--) {
            if (!Character.isDigit(buffer.charAt(i))) {
                return buffer.substring(0, i + 1);
            }
        }

        return "Node";
    }

    /**
     * @param base                the string base of the name--for example,
     *                            "Graph".
     * @param sessionNodeWrappers list of wrappers with names that cannot be the
     *                            next string in the sequence.
     * @return the next string in the sequence--for example, "Graph1".
     */
    private String nextUniqueName(final String base, final List sessionNodeWrappers) {

        if (base == null) {
            throw new NullPointerException("Base name must be non-null.");
        }

        int i = 0;    // Sequence 1, 2, 3, ...
        String name;

        loop:
        while (true) {
            i++;

            name = base + i;

            Iterator iterator = this.sessionNodeWrappers.iterator();
            for (final Iterator j = iterator; j.hasNext(); ) {
                final SessionNodeWrapper wrapper = (SessionNodeWrapper) j.next();

                if (wrapper.getSessionName().equals(name)) {
                    continue loop;
                }
            }

            iterator = sessionNodeWrappers.iterator();
            for (final Iterator j = iterator; j.hasNext(); ) {
                final SessionNodeWrapper wrapper = (SessionNodeWrapper) j.next();

                if (wrapper.getSessionName().equals(name)) {
                    continue loop;
                }
            }

            break;
        }
        return base + i;
    }

    /**
     * @return true just in case the given object is this object.
     */
    public boolean equals(final Object o) {
        if (o == null) {
            return false;
        }

        return o == this;
    }

    /**
     * @return the list of edges in the workbench.  No particular ordering of
     * the edges in the list is guaranteed.
     */
    public Set<Edge> getEdges() {
        return new HashSet<>(this.sessionEdges);
    }

    public Edge getEdge(final Node node1, final Node node2) {
        return null;
    }

    public Edge getDirectedEdge(final Node node1, final Node node2) {
        return null;
    }

    /**
     * Determines whether this workbench contains the given edge.
     *
     * @param edge the edge to check.
     * @return true iff the workbench contain 'edge'.
     */
    public boolean containsEdge(final Edge edge) {
        return this.sessionEdges.contains(edge);
    }

    /**
     * Determines whether this workbench contains the given node.
     */
    public boolean containsNode(final Node node) {
        return this.sessionNodeWrappers.contains(node);
    }

    /**
     * @return the list of edges connected to a particular node. No particular
     * ordering of the edges in the list is guaranteed.
     */
    public List<Edge> getEdges(final Node node) {
        final List<Edge> edgeList = new LinkedList<>();

        for (final Edge edge : this.sessionEdges) {
            if ((edge.getNode1() == node) || (edge.getNode2() == node)) {
                edgeList.add(edge);
            }
        }

        return edgeList;
    }

    /**
     * @return the node with the given string name.  In case of accidental
     * duplicates, the first node encountered with the given name is returned.
     * In case no node exists with the given name, null is returned.
     */
    public Node getNode(final String name) {
        for (final Node sessionNodeWrapper : this.sessionNodeWrappers) {
            final SessionNodeWrapper wrapper =
                    (SessionNodeWrapper) sessionNodeWrapper;

            if (wrapper.getSessionName().equals(name)) {
                return wrapper;
            }
        }

        return null;
    }

    /**
     * @return the number of nodes in the workbench.
     */
    public int getNumNodes() {
        return this.sessionNodeWrappers.size();
    }

    /**
     * @return the number of edges in the (entire) workbench.
     */
    public int getNumEdges() {
        return this.sessionEdges.size();
    }

    /**
     * @param node the node in question
     * @return the number of edges in the workbench which are connected to a
     * particular node.
     */
    public int getNumEdges(final Node node) {

        final Set<Edge> edgeSet = new HashSet<>();

        for (final Edge edge : this.sessionEdges) {
            if ((edge.getNode1() == node) || (edge.getNode2() == node)) {
                edgeSet.add(edge);
            }
        }

        return edgeSet.size();
    }

    public List<Node> getNodes() {
        return new ArrayList<>(this.sessionNodeWrappers);
    }

    /**
     * Removes an edge from the workbench.
     */
    public boolean removeEdge(final Edge edge) {
        if (this.sessionEdges.contains(edge)) {
            final SessionNodeWrapper nodeAWrapper =
                    (SessionNodeWrapper) edge.getNode1();
            final SessionNodeWrapper nodeBWrapper =
                    (SessionNodeWrapper) edge.getNode2();
            final SessionNode nodeA = nodeAWrapper.getSessionNode();
            final SessionNode nodeB = nodeBWrapper.getSessionNode();
            final boolean removed = nodeB.removeParent(nodeA);

            if (removed) {
                this.sessionEdges.remove(edge);
                getPropertyChangeSupport().firePropertyChange("edgeRemoved",
                        edge, null);

                return true;
            }
        }

        return false;
    }

    /**
     * Removes the edge connecting the two given nodes, provided there is
     * exactly one such edge.
     */
    public boolean removeEdge(final Node node1, final Node node2) {
        return false;
    }

    /**
     * Removes all nodes (and therefore all edges) from the workbench.
     */
    public void clear() {
        throw new UnsupportedOperationException();
    }

    /**
     * Removes a property change listener from the workbench.
     *
     * @param e the property change listener.
     */
    public void removePropertyChangeListener(final PropertyChangeListener e) {
        getPropertyChangeSupport().removePropertyChangeListener(e);
    }

    /**
     * Removes a node from the workbench.
     *
     * @param node the node to be removed.
     * @return true if the node was removed, false if not.
     */
    public boolean removeNode(final Node node) {
        if (this.sessionNodeWrappers.contains(node)) {
            for (final Edge edge : getEdges(node)) {
                removeEdge(edge);
            }

            final SessionNodeWrapper wrapper = (SessionNodeWrapper) node;
            final SessionNode sessionNode = wrapper.getSessionNode();

            try {
                this.session.removeNode(sessionNode);
                this.sessionNodeWrappers.remove(wrapper);
                getPropertyChangeSupport().firePropertyChange("nodeRemoved",
                        node, null);

                return true;
            } catch (final IllegalArgumentException e) {
                return false;
            }
        }

        return false;
    }

    /**
     * Iterates through the collection and removes any permissible nodes found.
     * The order in which nodes are removed is the order in which they are
     * presented in the iterator.
     *
     * @param newNodes the Collection of nodes.
     * @return true if nodes were added, false if not.
     */
    public boolean removeNodes(final List newNodes) {
        throw new UnsupportedOperationException();
    }

    /**
     * Iterates through the collection and removes any permissible edges found.
     * The order in which edges are added is the order in which they are
     * presented in the iterator.
     *
     * @param edges the Collection of edges.
     * @return true if edges were added, false if not.
     */
    public boolean removeEdges(final Collection<Edge> edges) {

        boolean removed = false;

        for (final Edge edge : edges) {
            removed = removed || removeEdge(edge);
        }

        return removed;
    }

    /**
     * Removes all edges connecting node A to node B.  In most cases, this will
     * remove at most one edge, but since multiple edges are permitted in some
     * workbench implementations, the number will in some cases be greater than
     * one.
     */
    public boolean removeEdges(final Node nodeA, final Node nodeB) {
        boolean removed = false;

        for (final Edge edge : getEdges()) {
            if ((edge.getNode1() == nodeA) && (edge.getNode2() == nodeB)) {
                removed = removed || removeEdge(edge);
            }
        }

        return true;
    }

    /**
     * @return the endpoint along the edge from node to node2 at the node2 end.
     */
    public Endpoint getEndpoint(final Node node1, final Node node2) {
        return getEdge(node1, node2).getProximalEndpoint(node2);
    }

    /**
     * Sets the endpoint type at the 'to' end of the edge from 'from' to 'to' to
     * the given endpoint.
     *
     * @throws UnsupportedOperationException since this graph may contains only
     *                                       directed edges.
     */
    public boolean setEndpoint(final Node from, final Node to, final Endpoint endPoint) {
        throw new UnsupportedOperationException();
    }

    /**
     * @return a string representation of the workbench.
     */
    public String toString() {
        return "Wrapper for " + this.session.toString();
    }

    public void transferNodesAndEdges(final Graph graph)
            throws IllegalArgumentException {
        throw new UnsupportedOperationException();
    }

    public Set<Triple> getAmbiguousTriples() {
        return new HashSet<>();
    }

    public Set<Triple> getUnderLines() {
        return new HashSet<>();
    }

    public Set<Triple> getDottedUnderlines() {
        return new HashSet<>();
    }

    /**
     * States whether x-y-x is an underline triple or not.
     */
    public boolean isAmbiguousTriple(final Node x, final Node y, final Node z) {
        throw new UnsupportedOperationException();
    }

    /**
     * States whether x-y-x is an underline triple or not.
     */
    public boolean isUnderlineTriple(final Node x, final Node y, final Node z) {
        throw new UnsupportedOperationException();
    }

    /**
     * States whether x-y-x is an underline triple or not.
     */
    public boolean isDottedUnderlineTriple(final Node x, final Node y, final Node z) {
        throw new UnsupportedOperationException();
    }

    public void addAmbiguousTriple(final Node x, final Node y, final Node z) {
        throw new UnsupportedOperationException();
    }

    public void addUnderlineTriple(final Node x, final Node y, final Node z) {
        throw new UnsupportedOperationException();
    }

    public void addDottedUnderlineTriple(final Node x, final Node y, final Node z) {
        throw new UnsupportedOperationException();
    }

    public void removeAmbiguousTriple(final Node x, final Node y, final Node z) {
        throw new UnsupportedOperationException();
    }

    public void removeUnderlineTriple(final Node x, final Node y, final Node z) {
        throw new UnsupportedOperationException();
    }

    public void removeDottedUnderlineTriple(final Node x, final Node y, final Node z) {
        throw new UnsupportedOperationException();
    }


    public void setAmbiguousTriples(final Set<Triple> triples) {
        throw new UnsupportedOperationException();
    }

    public void setUnderLineTriples(final Set<Triple> triples) {
        throw new UnsupportedOperationException();
    }


    public void setDottedUnderLineTriples(final Set<Triple> triples) {
        throw new UnsupportedOperationException();
    }

    public List<Node> getCausalOrdering() {
        throw new UnsupportedOperationException();
    }

    public List<String> getNodeNames() {
        throw new UnsupportedOperationException();
    }

    public boolean existsInducingPath(final Node node1, final Node node2) {
        throw new UnsupportedOperationException();
    }

    public Graph subgraph(final List nodes) {
        throw new UnsupportedOperationException();
    }

    //** ***************OTHER PUBLIC METHODS ******************** */

    /**
     * @return a reference to the session being edited.
     */
    public Session getSession() {
        return this.session;
    }

    /**
     * @return the name of the session. The name cannot be null.
     */
    public String getName() {
        return this.session.getName();
    }

    /**
     * Sets the name of the session. The name cannot be null.
     */
    public void setName(final String name) {
        final String oldName = this.session.getName();
        this.session.setName(name);
        this.propertyChangeSupport.firePropertyChange("name", oldName,
                this.session.getName());
    }

    //======================PRIVATE METHODS=========================//

    /**
     * @return the property change support.
     */
    private PropertyChangeSupport getPropertyChangeSupport() {
        if (this.propertyChangeSupport == null) {
            this.propertyChangeSupport = new PropertyChangeSupport(this);
        }

        return this.propertyChangeSupport;
    }

    @Override
    public List<String> getTriplesClassificationTypes() {
        return null;
    }

    @Override
    public List<List<Triple>> getTriplesLists(final Node node) {
        return null;
    }

    @Override
    public boolean isPag() {
        return this.pag;
    }

    @Override
    public void setPag(final boolean pag) {
        this.pag = pag;
    }

    @Override
    public boolean isCPDAG() {
        return this.CPDAG;
    }

    @Override
    public void setCPDAG(final boolean CPDAG) {
        this.CPDAG = CPDAG;
    }

    /**
     * Handles <code>SessionEvent</code>s. Hides the handling of these from the
     * API.
     */
    private class SessionHandler extends SessionAdapter {

        /**
         * Allows the user to verify that an edge added to a node that already
         * has a model in it is OK.
         */
        public void addingEdge(final SessionEvent event) {
            final String message =
                    "Child node already created. If you add this edge,\n" +
                            "the content of the child node will be made\n" +
                            "consistent with the parent.";

            final int ret = JOptionPane.showConfirmDialog(
                    JOptionUtils.centeringComp(), message, "Warning",
                    JOptionPane.OK_CANCEL_OPTION);

            if (ret == JOptionPane.CANCEL_OPTION) {
                final SessionNode sessionNode = (SessionNode) event.getSource();
                sessionNode.setNextEdgeAddAllowed(false);
            }
        }
    }

    /**
     * @return the edges connecting node1 and node2.
     */
    public List<Edge> getEdges(final Node node1, final Node node2) {
        throw new UnsupportedOperationException();
    }
//
//    // Unused methods from Graph
//
//    /**
//     * Adds a directed edge --&gt; to the graph.
//     */
//    public boolean addDirectedEdge(Node node1, Node node2) {
//        throw new UnsupportedOperationException();
//    }
//
//    /**
//     * Adds an undirected edge --- to the graph.
//     */
//    public boolean addUndirectedEdge(Node node1, Node node2) {
//        throw new UnsupportedOperationException();
//    }
//
//    /**
//     * Adds an nondirected edges o-o to the graph.
//     */
//    public boolean addNondirectedEdge(Node node1, Node node2) {
//        throw new UnsupportedOperationException();
//    }
//
//    /**
//     * Adds a bidirected edges &lt;-&gt; to the graph.
//     */
//    public boolean addBidirectedEdge(Node node1, Node node2) {
//        throw new UnsupportedOperationException();
//    }
//
//    /**
//     * Adds a partially oriented edge o-&gt; to the graph.
//     */
//    public boolean addPartiallyOrientedEdge(Node node1, Node node2) {
//        throw new UnsupportedOperationException();
//    }
//
//    /**
//     * @return true iff there is a directed cycle in the graph.
//     */
//    public boolean existsDirectedCycle() {
//        throw new UnsupportedOperationException();
//    }
//
//    public boolean isDirectedFromTo(Node node1, Node node2) {
//        throw new UnsupportedOperationException();
//    }
//
//    public boolean isUndirectedFromTo(Node node1, Node node2) {
//        throw new UnsupportedOperationException();
//    }
//
//    public boolean defVisible(Edge edge) {
//        throw new UnsupportedOperationException();
//    }
//
//    /**
//     * @return true iff there is a directed path from node1 to node2 in the
//     * graph.
//     */
//    public boolean existsDirectedPathFromTo(Node node1, Node node2) {
//        throw new UnsupportedOperationException();
//    }
//
//    public boolean existsUndirectedPathFromTo(Node node1, Node node2) {
//        throw new UnsupportedOperationException();
//    }
//
//    public boolean existsSemiDirectedPathFromTo(Node node1, Set nodes2) {
//        throw new UnsupportedOperationException();
//    }
//
//    public boolean existsSemiDirectedPathFromTo(Node node1, Node node2) {
//        throw new UnsupportedOperationException();
//    }
//
//    /**
//     * @return true iff a trek exists between two nodes in the graph.  A trek
//     * exists if there is a directed path between the two nodes or else, for
//     * some third node in the graph, there is a path to each of the two nodes in
//     * question.
//     */
//    public boolean existsTrek(Node node1, Node node2) {
//        throw new UnsupportedOperationException();
//    }
//
//    /**
//     * @return the list of ancestors for the given nodes.
//     */
//    public List<Node> getAncestors(List nodes) {
//        throw new UnsupportedOperationException();
//    }
//
//    /**
//     * @return the Collection of children for a node.
//     */
//    public List<Node> getChildren(Node node) {
//        throw new UnsupportedOperationException();
//    }
//
//    public int getConnectivity() {
//        throw new UnsupportedOperationException();
//    }
//
//    public List<Node> getDescendants(List nodes) {
//        throw new UnsupportedOperationException();
//    }
//
//    /**
//     * @return a matrix of endpoints for the nodes in this graph, with nodes in
//     * the same order as getNodes().
//     */
//    public Endpoint[][] getEndpointMatrix() {
//        throw new UnsupportedOperationException();
//    }
//
//    /**
//     * @return the list of nodes adjacent to the given node.
//     */
//    public List<Node> getAdjacentNodes(Node node) {
//        throw new UnsupportedOperationException();
//    }
//
//    /**
//     * @return the number of arrow endpoint adjacent to an edge.
//     */
//    public int getIndegree(Node node) {
//        throw new UnsupportedOperationException();
//    }
//
//    @Override
//    public int getDegree(Node node) {
//        throw new UnsupportedOperationException();
//    }
//
//    /**
//     * @return the number of null endpoints adjacent to an edge.
//     */
//    public int getOutdegree(Node node) {
//        throw new UnsupportedOperationException();
//    }
//

    /**
     * @return the list of parents for a node.
     */
    public List<Node> getParents(final Node node) {
        return new ArrayList<Node>(((SessionNode) node).getParents());
    }
//
//    /**
//     * Determines whether one node is an ancestor of another.
//     */
//    public boolean isAncestorOf(Node node1, Node node2) {
//        throw new UnsupportedOperationException();
//    }
//
//    public boolean possibleAncestor(Node node1, Node node2) {
//        throw new UnsupportedOperationException();
//    }
//
//    /**
//     * @return true iff node1 is adjacent to node2 in the graph.
//     */
//    public boolean isAdjacentTo(Node node1, Node node2) {
//        throw new UnsupportedOperationException();
//    }
//
//    /**
//     * @return true iff node1 is a child of node2 in the graph.
//     */
//    public boolean isChildOf(Node node1, Node node2) {
//        throw new UnsupportedOperationException();
//    }
//
//    /**
//     * @return true iff node1 is a (non-proper) descendant of node2.
//     */
//    public boolean isDescendentOf(Node node1, Node node2) {
//        throw new UnsupportedOperationException();
//    }
//
//    public boolean defNonDescendent(Node node1, Node node2) {
//        throw new UnsupportedOperationException();
//    }
//
//    public boolean isDefNoncollider(Node node1, Node node2, Node node3) {
//        throw new UnsupportedOperationException();
//    }
//
//    public boolean isDefCollider(Node node1, Node node2, Node node3) {
//        throw new UnsupportedOperationException();
//    }
//
//    /**
//     * Determines whether one node is d-separated from another. According to
//     * Spirtes, Richardson &amp; Meek, two nodes are d- connected given some
//     * conditioning set Z if there is an acyclic undirected path U between them,
//     * such that every collider on U is an ancestor of some element in Z and
//     * every non-collider on U is not in Z.  Two elements are d-separated just
//     * in case they are not d-separated.  A collider is a node which two edges
//     * hold in common for which the endpoints leading into the node are both
//     * arrow endpoints.
//     */
//    public boolean isDConnectedTo(Node node1, Node node2, List z) {
//        throw new UnsupportedOperationException();
//    }
//
//    /**
//     * Determines whether one node is d-separated from another. According to
//     * Spirtes, Richardson &amp; Meek, two nodes are d- connected given some
//     * conditioning set Z if there is an acyclic undirected path U between them,
//     * such that every collider on U is an ancestor of some element in Z and
//     * every non-collider on U is not in Z.  Two elements are d-separated just
//     * in case they are not d-separated.  A collider is a node which two edges
//     * hold in common for which the endpoints leading into the node are both
//     * arrow endpoints.
//     */
//    public boolean isDSeparatedFrom(Node node1, Node node2, List z) {
//        throw new UnsupportedOperationException();
//    }
//
//    public boolean possDConnectedTo(Node node1, Node node2, List z) {
//        throw new UnsupportedOperationException();
//    }
//
//    /**
//     * @return true iff the given node is exogenous in the graph.
//     */
//    public boolean isExogenous(Node node) {
//        throw new UnsupportedOperationException();
//    }
//
//    /**
//     * Determines whether one node is a parent of another.
//     */
//    public boolean isParentOf(Node node1, Node node2) {
//        throw new UnsupportedOperationException();
//    }
//
//    /**
//     * Determines whether one node is a proper ancestor of another.
//     */
//    public boolean isProperAncestorOf(Node node1, Node node2) {
//        throw new UnsupportedOperationException();
//    }
//
//    /**
//     * Determines whether one node is a proper decendent of another.
//     */
//    public boolean isProperDescendentOf(Node node1, Node node2) {
//        throw new UnsupportedOperationException();
//    }
//
//    /**
//     * Nodes adjacent to the given node with the given proximal endpoint.
//     */
//    public List<Node> getNodesInTo(Node node, Endpoint n) {
//        throw new UnsupportedOperationException();
//    }
//
//    /**
//     * Nodes adjacent to the given node with the given distal endpoint.
//     */
//    public List<Node> getNodesOutTo(Node node, Endpoint n) {
//        throw new UnsupportedOperationException();
//    }
//
//    /**
//     * Removes all edges from the graph and fully connects it using #-# edges,
//     * where # is the given endpoint.
//     */
//    public void fullyConnect(Endpoint endpoint) {
//        throw new UnsupportedOperationException();
//    }
//
//    public void reorientAllWith(Endpoint endpoint) {
//        throw new UnsupportedOperationException();
//    }
//
//    public void setHighlighted(Edge edge, boolean highlighted) {
//        this.highlighted = highlighted;
//    }
//
//    public boolean isHighlighted(Edge edge) {
//        return highlighted;
//    }
//
//    public boolean isParameterizable(Node node) {
//        return false;
//    }
//
//    public boolean isTimeLagModel() {
//        return false;
//    }
//
//    public TimeLagGraph getTimeLagGraph() {
//        return null;
//    }
//
//    @Override
//    public void removeTriplesNotInGraph() {
//        throw new UnsupportedOperationException();
//    }
//
//    @Override
//    public List<Node> getSepset(Node n1, Node n2) {
//        throw new UnsupportedOperationException();
//    }
//
//    @Override
//    public void setNodes(List<Node> nodes) {
//        throw new UnsupportedOperationException("Sorry, you cannot replace the variables for a time lag graph.");
//    }

    public boolean isSessionChanged() {
        return this.session.isSessionChanged();
    }

    public void setSessionChanged(final boolean sessionChanged) {
        this.session.setSessionChanged(sessionChanged);
    }

    public boolean isNewSession() {
        return this.session.isNewSession();
    }

    public void setNewSession(final boolean newSession) {
        this.session.setNewSession(newSession);
    }

    /**
     * Adds semantic checks to the default deserialization method. This method
     * must have the standard signature for a readObject method, and the body of
     * the method must begin with "s.defaultReadObject();". Other than that, any
     * semantic checks can be specified and do not need to stay the same from
     * version to version. A readObject method of this form may be added to any
     * class, even if Tetrad sessions were previously saved out using a version
     * of the class that didn't include it. (That's what the
     * "s.defaultReadObject();" is for. See J. Bloch, Effective Java, for help.
     *
     * @throws java.io.IOException
     * @throws ClassNotFoundException
     */
    private void readObject(final ObjectInputStream s)
            throws IOException, ClassNotFoundException {
        s.defaultReadObject();

        if (this.session == null) {
            throw new NullPointerException();
        }

        if (this.sessionNodeWrappers == null) {
            throw new NullPointerException();
        }

        if (this.sessionEdges == null) {
            throw new NullPointerException();
        }
    }
}





