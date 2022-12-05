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

package edu.cmu.tetradapp.editor;

import edu.cmu.tetrad.graph.*;
import edu.cmu.tetrad.util.JOptionUtils;
import edu.cmu.tetradapp.util.DesktopController;
import edu.cmu.tetradapp.util.IntTextField;
import edu.cmu.tetradapp.util.WatchedProcess;
import edu.cmu.tetradapp.workbench.GraphWorkbench;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.ClipboardOwner;
import java.awt.datatransfer.Transferable;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.prefs.Preferences;

/**
 * Puts up a panel letting the user show undirectedPaths about some node in the graph.
 *
 * @author Joseph Ramsey jdramsey@andrew.cmu.edu
 */
class PathsAction extends AbstractAction implements ClipboardOwner {
    private final GraphWorkbench workbench;
    private List<Node> nodes1, nodes2;
    private JTextArea textArea;
    private String method;

    public PathsAction(GraphWorkbench workbench) {
        super("Paths");
        this.workbench = workbench;
    }

    public void actionPerformed(ActionEvent e) {
        Graph graph = this.workbench.getGraph();

        this.textArea = new JTextArea();
        JScrollPane scroll = new JScrollPane(this.textArea);
        scroll.setPreferredSize(new Dimension(600, 400));

        List<Node> allNodes = graph.getNodes();
        allNodes.sort(Comparator.naturalOrder());
        allNodes.add(new GraphNode("SELECT_ALL"));
        Node[] array = allNodes.toArray(new Node[0]);

        Node pathFrom = graph.getNode(Preferences.userRoot().get("pathFrom", ""));

        if (pathFrom == null) {
            this.nodes1 = Collections.singletonList(graph.getNodes().get(0));
        } else {
            this.nodes1 = Collections.singletonList(pathFrom);
        }

        JComboBox node1Box = new JComboBox(array);

        node1Box.addActionListener(e1 -> {
            JComboBox box = (JComboBox) e1.getSource();
            Node node = (Node) box.getSelectedItem();
            System.out.println(node);

            assert node != null;
            if ("SELECT_ALL".equals(node.getName())) {
                PathsAction.this.nodes1 = new ArrayList<>(graph.getNodes());
            } else {
                PathsAction.this.nodes1 = Collections.singletonList(node);
            }

            Preferences.userRoot().put("pathFrom", node.getName());

//                update(graph, textArea, nodes1, nodes2, method);
        });

        node1Box.setSelectedItem(this.nodes1.get(0));

        Node pathTo = graph.getNode(Preferences.userRoot().get("pathTo", ""));

        if (pathTo == null) {
            this.nodes2 = Collections.singletonList(graph.getNodes().get(0));
        } else {
            this.nodes2 = Collections.singletonList(pathTo);
        }

        JComboBox node2Box = new JComboBox(array);

        node2Box.addActionListener(e12 -> {
            JComboBox box = (JComboBox) e12.getSource();
            Node node = (Node) box.getSelectedItem();
            System.out.println(node);

            if ("SELECT_ALL".equals(node.getName())) {
                PathsAction.this.nodes2 = new ArrayList<>(graph.getNodes());
            } else {
                PathsAction.this.nodes2 = Collections.singletonList(node);
            }

            Preferences.userRoot().put("pathTo", node.getName());

//                update(graph, textArea, nodes1, nodes2, method);
        });

        node2Box.setSelectedItem(this.nodes2.get(0));

        JComboBox methodBox = new JComboBox(new String[]{"Directed Paths", "Semidirected Paths", "Treks",
                "Adjacents"});
        this.method = Preferences.userRoot().get("pathMethod", "Directed Paths");

        methodBox.addActionListener(e13 -> {
            JComboBox box = (JComboBox) e13.getSource();
            PathsAction.this.method = (String) box.getSelectedItem();
            Preferences.userRoot().put("pathMethod", PathsAction.this.method);
//                update(graph, textArea, nodes1, nodes2, method);
        });

        methodBox.setSelectedItem(this.method);

        IntTextField maxField = new IntTextField(Preferences.userRoot().getInt("pathMaxLength", 3), 2);

        maxField.setFilter((value, oldValue) -> {
            try {
                setMaxLength(value);
//                    update(graph, textArea, nodes1, nodes2, method);
                return value;
            } catch (Exception e14) {
                return oldValue;
            }
        });

        JButton updateButton = new JButton(("Update"));

        updateButton.addActionListener(e15 -> update(graph, PathsAction.this.textArea,
                PathsAction.this.nodes1, PathsAction.this.nodes2, PathsAction.this.method));

        allDirectedPaths(graph, this.textArea, this.nodes1, this.nodes2);

        Box b = Box.createVerticalBox();

        Box b1 = Box.createHorizontalBox();
        b1.add(new JLabel("From "));
        b1.add(node1Box);
        b1.add(Box.createHorizontalGlue());
        b1.add(new JLabel(" To "));
        b1.add(node2Box);
        b1.add(Box.createHorizontalGlue());
        b1.add(methodBox);
        b1.add(new JLabel("Max length"));
        b1.add(maxField);
        b1.add(updateButton);
        b.add(b1);

        Box b2 = Box.createHorizontalBox();
        b2.add(scroll);
        this.textArea.setCaretPosition(0);
        b.add(b2);

        JPanel panel = new JPanel();
        panel.setLayout(new BorderLayout());
        panel.add(b);

        EditorWindow window = new EditorWindow(panel,
                "Directed Paths", "Close", false, this.workbench);
        DesktopController.getInstance().addEditorWindow(window, JLayeredPane.PALETTE_LAYER);
        window.setVisible(true);

        update(graph, this.textArea, this.nodes1, this.nodes2, this.method);
    }

    private void update(Graph graph, JTextArea textArea, List<Node> nodes1, List<Node> nodes2, String method) {
        Window owner = (Window) JOptionUtils.centeringComp().getTopLevelAncestor();

        WatchedProcess process = new WatchedProcess(owner) {
            public void watch() {
                if ("Directed Paths".equals(method)) {
                    textArea.setText("");
                    allDirectedPaths(graph, textArea, nodes1, nodes2);
                } else if ("Semidirected Paths".equals(method)) {
                    textArea.setText("");
                    allSemidirectedPaths(graph, textArea, nodes1, nodes2);
                } else if ("Treks".equals(method)) {
                    textArea.setText("");
                    allTreks(graph, textArea, nodes1, nodes2);
                } else if ("Adjacents".equals(method)) {
                    textArea.setText("");
                    adjacentNodes(graph, textArea, nodes1, nodes2);
                }
            }
        };

        process.watch();
    }

    private void allDirectedPaths(Graph graph, JTextArea textArea, List<Node> nodes1, List<Node> nodes2) {
        boolean pathListed = false;

        for (Node node1 : nodes1) {
            for (Node node2 : nodes2) {
                List<List<Node>> directedPaths = GraphUtils.directedPathsFromTo(graph, node1, node2,
                        Preferences.userRoot().getInt("pathMaxLength", 3));

                if (!directedPaths.isEmpty()) {
                    textArea.append("\n\nFrom " + node1 + " to " + node2 + ":");

                    for (List<Node> directedPath : directedPaths) {
                        textArea.append("\n    ");

                        textArea.append(directedPath.get(0).toString());

                        for (int m = 1; m < directedPath.size(); m++) {
                            Node n0 = directedPath.get(m - 1);
                            Node n1 = directedPath.get(m);

                            Edge edge = graph.getEdge(n0, n1);

                            Endpoint endpoint0 = edge.getProximalEndpoint(n0);
                            Endpoint endpoint1 = edge.getProximalEndpoint(n1);

                            textArea.append(endpoint0 == Endpoint.ARROW ? "<" : "-");
                            textArea.append("-");
                            textArea.append(endpoint1 == Endpoint.ARROW ? ">" : "-");

                            textArea.append(n1.toString());
                        }
                    }

                    pathListed = true;
                }

                directedPaths = GraphUtils.directedPathsFromTo(graph, node2, node1, Preferences.userRoot().getInt("pathMaxLength", 3));

                if (!directedPaths.isEmpty()) {
                    textArea.append("\n\nFrom " + node2 + " to " + node1 + ":");

                    for (List<Node> directedPath : directedPaths) {
                        textArea.append("\n    ");

                        textArea.append(directedPath.get(0).toString());

                        for (int m = 1; m < directedPath.size(); m++) {
                            Node n0 = directedPath.get(m - 1);
                            Node n1 = directedPath.get(m);

                            Edge edge = graph.getEdge(n0, n1);

                            Endpoint endpoint0 = edge.getProximalEndpoint(n0);
                            Endpoint endpoint1 = edge.getProximalEndpoint(n1);

                            textArea.append(endpoint0 == Endpoint.ARROW ? "<" : "-");
                            textArea.append("-");
                            textArea.append(endpoint1 == Endpoint.ARROW ? ">" : "-");

                            textArea.append(n1.toString());
                        }
                    }

                    pathListed = true;
                }
            }
        }

        if (!pathListed) {
            textArea.append("No directedPaths listed.");
        }
    }

    private void allSemidirectedPaths(Graph graph, JTextArea textArea, List<Node> nodes1, List<Node> nodes2) {
        boolean pathListed = false;

        for (Node node1 : nodes1) {
            for (Node node2 : nodes2) {
                List<List<Node>> directedPaths = GraphUtils.semidirectedPathsFromTo(graph, node1, node2,
                        Preferences.userRoot().getInt("pathMaxLength", 3));

                if (directedPaths.isEmpty()) {
                    continue;
                } else {
                    pathListed = true;
                }

                textArea.append("\n\nFrom " + node1 + " to " + node2 + ":");

                for (List<Node> directedPath : directedPaths) {
                    textArea.append("\n    ");

                    textArea.append(directedPath.get(0).toString());

                    for (int m = 1; m < directedPath.size(); m++) {
                        Node n0 = directedPath.get(m - 1);
                        Node n1 = directedPath.get(m);

                        Edge edge = graph.getEdge(n0, n1);

                        Endpoint endpoint0 = edge.getProximalEndpoint(n0);
                        Endpoint endpoint1 = edge.getProximalEndpoint(n1);

                        textArea.append(endpoint0 == Endpoint.ARROW ? "<" : "-");
                        textArea.append("-");
                        textArea.append(endpoint1 == Endpoint.ARROW ? ">" : "-");

                        textArea.append(n1.toString());
                    }
                }
            }
        }

        if (!pathListed) {
            textArea.append("No undirectedPaths listed.");
        }
    }

    private void allTreks(Graph graph, JTextArea textArea, List<Node> nodes1, List<Node> nodes2) {
        boolean pathListed = false;

        for (Node node1 : nodes1) {
            for (Node node2 : nodes2) {
                List<List<Node>> treks = GraphUtils.treks(graph, node1, node2, Preferences.userRoot().getInt("pathMaxLength", 3));

                if (treks.isEmpty()) {
                    continue;
                } else {
                    pathListed = true;
                }

                textArea.append("\n\nBetween " + node1 + " and " + node2 + ":");

                for (List<Node> trek : treks) {
                    textArea.append("\n    " + GraphUtils.pathString(graph, trek));
                }
            }
        }

        if (!pathListed) {
            textArea.append("No undirectedPaths listed.");
        }
    }

    private void adjacentNodes(Graph graph, JTextArea textArea, List<Node> nodes1, List<Node> nodes2) {
        for (Node node1 : nodes1) {
            for (Node node2 : nodes2) {
                List<Node> parents = graph.getParents(node1);
                List<Node> children = graph.getChildren(node1);

                List<Node> ambiguous = graph.getAdjacentNodes(node1);
                ambiguous.removeAll(parents);
                ambiguous.removeAll(children);

                textArea.append("\n\nAdjacents for " + node1 + ":");
                textArea.append("\n\nParents: " + niceList(parents));
                textArea.append("\nChildren: " + niceList(children));
                textArea.append("\nAmbiguous: " + niceList(ambiguous));


                List<Node> parents2 = graph.getParents(node2);
                List<Node> children2 = graph.getChildren(node2);

                List<Node> ambiguous2 = graph.getAdjacentNodes(node2);
                ambiguous2.removeAll(parents2);
                ambiguous2.removeAll(children2);

                textArea.append("\n\nAdjacents for " + node2 + ":");
                textArea.append("\n\nParents: " + niceList(parents2));
                textArea.append("\nChildren: " + niceList(children2));
                textArea.append("\nAmbiguous: " + niceList(ambiguous2));
            }
        }
    }

    private String niceList(List<Node> nodes) {
        if (nodes.isEmpty()) {
            return "--NONE--";
        }

        Collections.sort(nodes);

        StringBuilder buf = new StringBuilder();

        for (int i = 0; i < nodes.size(); i++) {
            buf.append(nodes.get(i));

            if (i < nodes.size() - 1) {
                buf.append(", ");
            }
        }

        return buf.toString();
    }


    /**
     * Required by the AbstractAction interface; does nothing.
     */
    public void lostOwnership(Clipboard clipboard, Transferable contents) {
    }

    private void setMaxLength(int maxLength) {
        if (!(maxLength >= -1)) throw new IllegalArgumentException();
        Preferences.userRoot().putInt("pathMaxLength", maxLength);
    }
}



