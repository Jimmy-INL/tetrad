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

import edu.cmu.tetrad.data.Knowledge;
import edu.cmu.tetrad.graph.*;
import edu.cmu.tetrad.search.IndependenceTest;
import edu.cmu.tetrad.search.test.MsepTest;
import edu.cmu.tetrad.util.Parameters;
import edu.cmu.tetrad.util.TetradSerializable;
import edu.cmu.tetradapp.model.DagWrapper;
import edu.cmu.tetradapp.model.IndTestProducer;
import edu.cmu.tetradapp.session.DelegatesEditing;
import edu.cmu.tetradapp.ui.PaddingPanel;
import edu.cmu.tetradapp.util.DesktopController;
import edu.cmu.tetradapp.util.GraphUtils;
import edu.cmu.tetradapp.util.LayoutEditable;
import edu.cmu.tetradapp.workbench.DisplayEdge;
import edu.cmu.tetradapp.workbench.DisplayNode;
import edu.cmu.tetradapp.workbench.GraphWorkbench;
import edu.cmu.tetradapp.workbench.LayoutMenu;

import javax.swing.*;
import javax.swing.event.InternalFrameAdapter;
import javax.swing.event.InternalFrameEvent;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.beans.PropertyChangeEvent;
import java.io.Serial;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * Displays a workbench editing workbench area together with a toolbench for editing tetrad-style graphs.
 *
 * @author Aaron Powers
 * @author josephramsey
 * @author Zhou Yuan
 * @version $Id: $Id
 */
public final class DagEditor extends JPanel
        implements GraphEditable, LayoutEditable, DelegatesEditing, IndTestProducer {

    @Serial
    private static final long serialVersionUID = -6082746735835257666L;

    /**
     * The parameters for the graph.
     */
    private final Parameters parameters;

    /**
     * The scroll pane for the graph editor.
     */
    private final JScrollPane graphEditorScroll = new JScrollPane();

    /**
     * The table for the edge types.
     */
    private final EdgeTypeTable edgeTypeTable;

    /**
     * The workbench for the graph.
     */
    private GraphWorkbench workbench;

    /**
     * <p>Constructor for DagEditor.</p>
     *
     * @param dagWrapper a {@link edu.cmu.tetradapp.model.DagWrapper} object
     */
    public DagEditor(DagWrapper dagWrapper) {
        setLayout(new BorderLayout());
        this.parameters = dagWrapper.getParameters();
        this.edgeTypeTable = new EdgeTypeTable();

        initUI(dagWrapper);
    }

    //===========================PUBLIC METHODS======================//

    /**
     * {@inheritDoc}
     * <p>
     * Sets the name of this editor.
     */
    @Override
    public void setName(String name) {
        String oldName = getName();
        super.setName(name);
        firePropertyChange("name", oldName, getName());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public JComponent getEditDelegate() {
        return getWorkbench();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public GraphWorkbench getWorkbench() {
        return this.workbench;
    }

    /**
     * {@inheritDoc}
     * <p>
     * Returns a list of all the SessionNodeWrappers (TetradNodes) and SessionNodeEdges that are model components for
     * the respective SessionNodes and SessionEdges selected in the workbench. Note that the workbench, not the
     * SessionEditorNodes themselves, keeps track of the selection.
     */
    @Override
    public List getSelectedModelComponents() {
        List<Component> selectedComponents
                = getWorkbench().getSelectedComponents();
        List<TetradSerializable> selectedModelComponents
                = new ArrayList<>();

        selectedComponents.forEach(comp -> {
            if (comp instanceof DisplayNode) {
                selectedModelComponents.add(
                        ((DisplayNode) comp).getModelNode());
            } else if (comp instanceof DisplayEdge) {
                selectedModelComponents.add(
                        ((DisplayEdge) comp).getModelEdge());
            }
        });

        return selectedModelComponents;
    }

    /**
     * {@inheritDoc}
     * <p>
     * Pastes list of session elements into the workbench.
     */
    @Override
    public void pasteSubsession(List<Object> sessionElements, Point upperLeft) {
        getWorkbench().pasteSubgraph(sessionElements, upperLeft);
        getWorkbench().deselectAll();

        sessionElements.forEach(sessionElement -> {
            if (sessionElement instanceof GraphNode) {
                Node modelNode = (Node) sessionElement;
                getWorkbench().selectNode(modelNode);
            }
        });

        getWorkbench().selectConnectingEdges();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Graph getGraph() {
        return this.workbench.getGraph();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setGraph(Graph graph) {
        try {
            Dag dag = new Dag(graph);
            this.workbench.setGraph(dag);
        } catch (Exception e) {
            throw new RuntimeException("Not a DAG", e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Map getModelEdgesToDisplay() {
        return this.workbench.getModelEdgesToDisplay();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Map getModelNodesToDisplay() {
        return this.workbench.getModelNodesToDisplay();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Knowledge getKnowledge() {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Graph getSourceGraph() {
        return getWorkbench().getGraph();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void layoutByGraph(Graph graph) {
        getWorkbench().layoutByGraph(graph);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void layoutByKnowledge() {
        // Does nothing.
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Rectangle getVisibleRect() {
        return getWorkbench().getVisibleRect();
    }

    //===========================PRIVATE METHODS========================//
    private void initUI(DagWrapper dagWrapper) {
        Graph graph = dagWrapper.getGraph();

        this.workbench = new GraphWorkbench(graph);

        this.workbench.addPropertyChangeListener((PropertyChangeEvent evt) -> {
            String propertyName = evt.getPropertyName();

            // Update the bootstrap table if there's changes to the edges or node renaming
            String[] events = {"graph", "edgeAdded", "edgeRemoved"};

            if (Arrays.asList(events).contains(propertyName)) {
                if (getWorkbench() != null) {
                    Graph targetGraph = getWorkbench().getGraph();

                    // Update the dagWrapper
                    dagWrapper.setGraph(targetGraph);
                    // Also need to update the UI
//                    updateBootstrapTable(targetGraph);
                }
            } else if ("modelChanged".equals(propertyName)) {
                firePropertyChange("modelChanged", null, null);
            }
        });

        // Graph menu at the very top of the window
        JMenuBar menuBar = createGraphMenuBar();

        // Add the model selection to top if multiple models
        modelSelectin(dagWrapper);

        // topBox Left side toolbar
        DagGraphToolbar graphToolbar = new DagGraphToolbar(getWorkbench());
        graphToolbar.setMaximumSize(new Dimension(140, 450));

        // topBox right side graph editor
        this.graphEditorScroll.setPreferredSize(new Dimension(760, 450));
        this.graphEditorScroll.setViewportView(this.workbench);

        // topBox contains the topGraphBox and the instructionBox underneath
        Box topBox = Box.createVerticalBox();
        topBox.setPreferredSize(new Dimension(820, 400));

        // topGraphBox contains the vertical graph toolbar and graph editor
        Box topGraphBox = Box.createHorizontalBox();
        topGraphBox.add(graphToolbar);
        topGraphBox.add(this.graphEditorScroll);

        // Instruction with info button
        Box instructionBox = Box.createHorizontalBox();
        instructionBox.setMaximumSize(new Dimension(820, 40));

        JLabel label = new JLabel("Double click variable/node rectangle to change name.");
        label.setFont(new Font("SansSerif", Font.PLAIN, 12));

//        // Info button added by Zhou to show edge types
//        JButton infoBtn = new JButton(new ImageIcon(ImageUtils.getImage(this, "info.png")));
//        infoBtn.setBorder(new EmptyBorder(0, 0, 0, 0));
//
//        // Clock info button to show edge types instructions - Zhou
//        infoBtn.addActionListener(new ActionListener() {
//            @Override
//            public void actionPerformed(ActionEvent e) {
//                // Initialize helpSet
//                final String helpHS = "/docs/javahelp/TetradHelp.hs";
//
//                try {
//                    URL url = this.getClass().getResource(helpHS);
//                    HelpSet helpSet = new HelpSet(null, url);
//
//                    helpSet.setHomeID("graph_edge_types");
//                    HelpBroker broker = helpSet.createHelpBroker();
//                    ActionListener listener = new CSH.DisplayHelpFromSource(broker);
//                    listener.actionPerformed(e);
//                } catch (Exception ee) {
//                    System.out.println("HelpSet " + ee.getMessage());
//                    System.out.println("HelpSet " + helpHS + " not found");
//                    throw new IllegalArgumentException();
//                }
//            }
//        });
//
        instructionBox.add(label);
//        instructionBox.add(Box.createHorizontalStrut(2));
//        instructionBox.add(infoBtn);

        // Add to topBox
        topBox.add(topGraphBox);
        topBox.add(instructionBox);

        this.edgeTypeTable.setPreferredSize(new Dimension(820, 150));

//        //Use JSplitPane to allow resize the bottom box - Zhou
//        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, new PaddingPanel(topBox), new PaddingPanel(edgeTypeTable));
//        splitPane.setDividerLocation((int) (splitPane.getPreferredSize().getHeight() - 150));

        // Switching to tabbed pane because of resizing problems with the split pane... jdramsey 2021.08.25
        JTabbedPane tabbedPane = new JTabbedPane(SwingConstants.RIGHT);
        tabbedPane.addTab("Graph", new PaddingPanel(topBox));
        tabbedPane.addTab("Edges", this.edgeTypeTable);

        // Add to parent container
        add(menuBar, BorderLayout.NORTH);
        add(tabbedPane, BorderLayout.CENTER);

        this.edgeTypeTable.update(graph);

        // Performs relayout.
        // It means invalid content is asked for all the sizes and
        // all the subcomponents' sizes are set to proper values by LayoutManager.
        validate();
    }

    /**
     * Updates the graph in workbench when changing graph model
     */
    private void updateGraphWorkbench(Graph graph) {
        this.workbench = new GraphWorkbench(graph);
        this.graphEditorScroll.setViewportView(this.workbench);

        validate();
    }

    /**
     * Updates bootstrap table on adding/removing edges or graph changes
     */
    private void updateBootstrapTable(Graph graph) {
        this.edgeTypeTable.update(graph);

        validate();
    }

    /**
     * Creates the UI component for choosing from multiple graph models
     */
    private void modelSelectin(DagWrapper dagWrapper) {
        int numModels = dagWrapper.getNumModels();

        if (numModels > 1) {
            List<Integer> models = new ArrayList<>();
            for (int i = 0; i < numModels; i++) {
                models.add(i + 1);
            }

            JComboBox<Integer> comboBox = new JComboBox(models.toArray());

            // Remember the selected model on reopen
            comboBox.setSelectedIndex(dagWrapper.getModelIndex());

            comboBox.addActionListener((ActionEvent e) -> {
                dagWrapper.setModelIndex(comboBox.getSelectedIndex());

                // Update the graph workbench
                updateGraphWorkbench(dagWrapper.getGraph());

                // Update the bootstrap table
//                updateBootstrapTable(dagWrapper.getGraph());
            });

            // Put together
            Box modelSelectionBox = Box.createHorizontalBox();
            modelSelectionBox.add(new JLabel("Using model "));
            modelSelectionBox.add(comboBox);
            modelSelectionBox.add(new JLabel(" from "));
            modelSelectionBox.add(new JLabel(dagWrapper.getModelSourceName()));
            modelSelectionBox.add(Box.createHorizontalStrut(20));
            modelSelectionBox.add(Box.createHorizontalGlue());

            // Add to upper right
            add(modelSelectionBox, BorderLayout.EAST);
        }
    }

    private JMenuBar createGraphMenuBar() {
        JMenuBar menuBar = new JMenuBar();

        JMenu fileMenu = new GraphFileMenu(this, getWorkbench(), false);
//        JMenu fileMenu = createFileMenu();
        JMenu editMenu = createEditMenu();
        JMenu graphMenu = createGraphMenu();

        menuBar.add(fileMenu);
        menuBar.add(editMenu);
        menuBar.add(graphMenu);
        menuBar.add(new LayoutMenu(this));

        return menuBar;
    }

    /**
     * Creates the "file" menu, which allows the user to load, save, and post workbench models.
     *
     * @return this menu.
     */
    private JMenu createEditMenu() {

        JMenu edit = new JMenu("Edit");

        JMenuItem cut = new JMenuItem(new CutSubgraphAction(this));
        JMenuItem copy = new JMenuItem(new CopySubgraphAction(this));
        JMenuItem paste = new JMenuItem(new PasteSubgraphAction(this));
        JMenuItem undoLast = new JMenuItem(new UndoLastAction(workbench));
        JMenuItem redoLast = new JMenuItem(new RedoLastAction(workbench));
        JMenuItem setToOriginal = new JMenuItem(new ResetGraph(workbench));

        cut.setAccelerator(
                KeyStroke.getKeyStroke(KeyEvent.VK_X, InputEvent.CTRL_DOWN_MASK));
        copy.setAccelerator(
                KeyStroke.getKeyStroke(KeyEvent.VK_C, InputEvent.CTRL_DOWN_MASK));
        paste.setAccelerator(
                KeyStroke.getKeyStroke(KeyEvent.VK_V, InputEvent.CTRL_DOWN_MASK));
        undoLast.setAccelerator(
                KeyStroke.getKeyStroke(KeyEvent.VK_Z, InputEvent.CTRL_DOWN_MASK));
        redoLast.setAccelerator(
                KeyStroke.getKeyStroke(KeyEvent.VK_Y, InputEvent.CTRL_DOWN_MASK));
        setToOriginal.setAccelerator(
                KeyStroke.getKeyStroke(KeyEvent.VK_R, InputEvent.CTRL_DOWN_MASK));

        edit.add(cut);
        edit.add(copy);
        edit.add(paste);
        edit.addSeparator();

        edit.add(undoLast);
        edit.add(redoLast);
        edit.add(setToOriginal);

        return edit;
    }

    private JMenu createGraphMenu() {
        JMenu graph = new JMenu("Graph");

        JMenuItem randomGraph = new JMenuItem("Random Graph");
        graph.add(randomGraph);
        graph.addSeparator();

        graph.add(new GraphPropertiesAction(this.workbench));
        graph.add(new PathsAction(this.workbench));
        graph.add(new UnderliningsAction(this.workbench));
        graph.addSeparator();

        graph.add(GraphUtils.getHighlightMenu(this.workbench));
        graph.add(GraphUtils.getCheckGraphMenu(this.workbench));

//        JMenu revert = new JMenu("Revert Graph");
//        graph.add(revert);
//        JMenuItem undoLast = new JMenuItem(new UndoLastAction(this.workbench));
//        JMenuItem redoLast = new JMenuItem(new RedoLastAction(this.workbench));
//        JMenuItem setToOriginal = new JMenuItem(new SetToOriginalAction(this.workbench));
//        revert.add(undoLast);
//        revert.add(redoLast);
//        revert.add(setToOriginal);

//        undoLast.setAccelerator(
//                KeyStroke.getKeyStroke(KeyEvent.VK_Z, InputEvent.CTRL_DOWN_MASK));
//        redoLast.setAccelerator(
//                KeyStroke.getKeyStroke(KeyEvent.VK_Y, InputEvent.CTRL_DOWN_MASK));
//        setToOriginal.setAccelerator(
//                KeyStroke.getKeyStroke(KeyEvent.VK_O, InputEvent.CTRL_DOWN_MASK));

        randomGraph.addActionListener(e -> {
            GraphParamsEditor editor = new GraphParamsEditor();
            editor.setParams(this.parameters);

            EditorWindow editorWindow = new EditorWindow(editor, "Edit Random Graph Parameters",
                    "Done", true, this);

            DesktopController.getInstance().addEditorWindow(editorWindow, JLayeredPane.PALETTE_LAYER);
            editorWindow.pack();
            editorWindow.setVisible(true);

            editorWindow.addInternalFrameListener(new InternalFrameAdapter() {
                @Override
                public void internalFrameClosed(InternalFrameEvent e1) {
                    EditorWindow window = (EditorWindow) e1.getSource();

                    if (window.isCanceled()) {
                        return;
                    }

//                    RandomUtil.getInstance().setSeed(new Date().getTime());
                    Graph graph1 = edu.cmu.tetradapp.util.GraphUtils.makeRandomGraph(getGraph(), DagEditor.this.parameters);

                    boolean addCycles = DagEditor.this.parameters.getBoolean("randomAddCycles", false);

                    if (addCycles) {
                        int newGraphNumMeasuredNodes = DagEditor.this.parameters.getInt("newGraphNumMeasuredNodes", 10);
                        int newGraphNumEdges = DagEditor.this.parameters.getInt("newGraphNumEdges", 10);
                        graph1 = RandomGraph.randomCyclicGraph2(newGraphNumMeasuredNodes, newGraphNumEdges, 8);
                    }

                    getWorkbench().setGraph(graph1);
                }
            });
        });

        return graph;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public IndependenceTest getIndependenceTest() {
        return new MsepTest(this.workbench.getGraph());
    }
}
