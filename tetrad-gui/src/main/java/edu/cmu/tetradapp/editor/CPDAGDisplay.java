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

import edu.cmu.tetrad.graph.Graph;
import edu.cmu.tetrad.graph.GraphNode;
import edu.cmu.tetrad.graph.GraphTransforms;
import edu.cmu.tetrad.graph.Node;
import edu.cmu.tetrad.util.JOptionUtils;
import edu.cmu.tetrad.util.TetradSerializable;
import edu.cmu.tetradapp.workbench.DisplayEdge;
import edu.cmu.tetradapp.workbench.DisplayNode;
import edu.cmu.tetradapp.workbench.GraphWorkbench;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.List;

/**
 * Assumes that the search method of the CPDAG search has been run and shows the various options for DAG's consistent
 * with correlation information over the variables.
 *
 * @author josephramsey
 * @version $Id: $Id
 */
public class CPDAGDisplay extends JPanel implements GraphEditable {
    private GraphWorkbench workbench;

    /**
     * <p>Constructor for CPDAGDisplay.</p>
     *
     * @param graph a {@link edu.cmu.tetrad.graph.Graph} object
     */
    public CPDAGDisplay(Graph graph) {
        List<Graph> dags = GraphTransforms.generateCpdagDags(graph, false);

        if (dags.size() == 0) {
            JOptionPane.showMessageDialog(
                    JOptionUtils.centeringComp(),
                    "There are no consistent DAG's.");
            return;
        }

        Graph dag = dags.get(0);
        this.workbench = new GraphWorkbench(dag);

        SpinnerNumberModel model =
                new SpinnerNumberModel(1, 1, dags.size(), 1);
        model.addChangeListener(e -> {
            int index = model.getNumber().intValue();
            CPDAGDisplay.this.workbench.setGraph(
                    dags.get(index - 1));
        });

        JSpinner spinner = new JSpinner();
        JComboBox<String> orient = new JComboBox<>(
                new String[]{"Orient --- only", "Orient ---, &lt;->"});
        spinner.setModel(model);
        JLabel totalLabel = new JLabel(" of " + dags.size());

        orient.setMaximumSize(orient.getPreferredSize());
        orient.addActionListener(e -> {
            JComboBox box = (JComboBox) e.getSource();
            String option = (String) box.getSelectedItem();

            if ("Orient --- only".equals(option)) {
                List _dags = GraphTransforms.generateCpdagDags(graph, false);
                dags.clear();
                dags.addAll(_dags);
                SpinnerNumberModel model1 =
                        new SpinnerNumberModel(1, 1,
                                dags.size(), 1);
                model1.addChangeListener(e1 -> {
                    int index =
                            model1.getNumber().intValue();
                    CPDAGDisplay.this.workbench.setGraph(
                            dags.get(index - 1));
                });
                spinner.setModel(model1);
                totalLabel.setText(" of " + dags.size());
                CPDAGDisplay.this.workbench.setGraph(dags.get(0));
            } else if ("Orient ---, &lt;->".equals(option)) {
                List _dags = GraphTransforms.generateCpdagDags(graph, true);
                dags.clear();
                dags.addAll(_dags);
                SpinnerNumberModel model1 =
                        new SpinnerNumberModel(1, 1,
                                dags.size(), 1);
                model1.addChangeListener(e12 -> {
                    int index =
                            model1.getNumber().intValue();
                    CPDAGDisplay.this.workbench.setGraph(
                            dags.get(index - 1));
                });
                spinner.setModel(model1);
                totalLabel.setText(" of " + dags.size());
                CPDAGDisplay.this.workbench.setGraph(dags.get(0));
            }
        });

        spinner.setPreferredSize(new Dimension(50, 20));
        spinner.setMaximumSize(spinner.getPreferredSize());
        Box b = Box.createVerticalBox();
        Box b1 = Box.createHorizontalBox();
        b1.add(Box.createHorizontalGlue());
        b1.add(orient);
        b1.add(Box.createHorizontalStrut(10));
        b1.add(Box.createHorizontalGlue());
        b1.add(new JLabel("DAG "));
        b1.add(spinner);
        b1.add(totalLabel);

        b.add(b1);

        Box b2 = Box.createHorizontalBox();
        JPanel graphPanel = new JPanel();
        graphPanel.setLayout(new BorderLayout());
        JScrollPane jScrollPane = new JScrollPane(this.workbench);
        jScrollPane.setPreferredSize(new Dimension(400, 400));
        graphPanel.add(jScrollPane);
        graphPanel.setBorder(new TitledBorder("DAG in forbid_latent_common_causes"));
        b2.add(graphPanel);
        b.add(b2);

        setLayout(new BorderLayout());
        add(menuBar(), BorderLayout.NORTH);
        add(b, BorderLayout.CENTER);
    }

    /**
     * <p>getSelectedModelComponents.</p>
     *
     * @return a {@link java.util.List} object
     */
    public List getSelectedModelComponents() {
        Component[] components = getWorkbench().getComponents();
        List<TetradSerializable> selectedModelComponents =
                new ArrayList<>();

        for (Component comp : components) {
            if (comp instanceof DisplayNode) {
                selectedModelComponents.add(
                        ((DisplayNode) comp).getModelNode());
            } else if (comp instanceof DisplayEdge) {
                selectedModelComponents.add(
                        ((DisplayEdge) comp).getModelEdge());
            }
        }

        return selectedModelComponents;
    }

    /** {@inheritDoc} */
    public void pasteSubsession(List<Object> sessionElements, Point upperLeft) {
        getWorkbench().pasteSubgraph(sessionElements, upperLeft);
        getWorkbench().deselectAll();

        for (Object o : sessionElements) {

            if (o instanceof GraphNode) {
                Node modelNode = (Node) o;
                getWorkbench().selectNode(modelNode);
            }
        }

        getWorkbench().selectConnectingEdges();
    }

    /**
     * <p>Getter for the field <code>workbench</code>.</p>
     *
     * @return a {@link edu.cmu.tetradapp.workbench.GraphWorkbench} object
     */
    public GraphWorkbench getWorkbench() {
        return this.workbench;
    }

    /**
     * <p>getGraph.</p>
     *
     * @return a {@link edu.cmu.tetrad.graph.Graph} object
     */
    public Graph getGraph() {
        return this.workbench.getGraph();
    }

    /** {@inheritDoc} */
    public void setGraph(Graph graph) {
        this.workbench.setGraph(graph);
    }

    /**
     * Creates the "file" menu, which allows the user to load, save, and post workbench models.
     *
     * @return this menu.
     */
    private JMenuBar menuBar() {
        JMenu edit = new JMenu("Edit");
        JMenuItem copy = new JMenuItem(new CopySubgraphAction(this));
        copy.setAccelerator(
                KeyStroke.getKeyStroke(KeyEvent.VK_C, InputEvent.CTRL_DOWN_MASK));
        edit.add(copy);

        JMenuBar menuBar = new JMenuBar();
        menuBar.add(edit);

        return menuBar;
    }
}



