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

package edu.cmu.tetradapp.editor;

import edu.cmu.tetrad.data.DataModel;
import edu.cmu.tetrad.data.IKnowledge;
import edu.cmu.tetrad.data.Knowledge2;
import edu.cmu.tetrad.graph.*;
import edu.cmu.tetrad.search.IndTestType;
import edu.cmu.tetrad.search.SearchGraphUtils;
import edu.cmu.tetrad.util.JOptionUtils;
import edu.cmu.tetrad.util.Parameters;
import edu.cmu.tetradapp.model.*;
import edu.cmu.tetradapp.util.DesktopController;
import edu.cmu.tetradapp.util.LayoutEditable;
import edu.cmu.tetradapp.util.WatchedProcess;
import edu.cmu.tetradapp.workbench.GraphWorkbench;
import edu.cmu.tetradapp.workbench.LayoutMenu;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;
import java.util.Map;

/**
 * Edits some algorithm to search for Markov blanket CPDAGs.
 *
 * @author Joseph Ramsey
 */
public class FciCcdSearchEditor extends AbstractSearchEditor
        implements KnowledgeEditable, LayoutEditable, DoNotScroll {

    //=========================CONSTRUCTORS============================//

    /**
     * Opens up an editor to let the user view the given PcRunner.
     */
    public FciCcdSearchEditor(final PcRunner runner) {
        super(runner, "Result forbid_latent_common_causes");
    }

    /**
     * Opens up an editor to let the user view the given FciRunner.
     */
    public FciCcdSearchEditor(final FciRunner runner) {
        super(runner, "Result allow_latent_common_causes");
    }

    public FciCcdSearchEditor(final CfciRunner runner) {
        super(runner, "Result allow_latent_common_causes");
    }

    public FciCcdSearchEditor(final GFciRunner runner) {
        super(runner, "Result allow_latent_common_causes");
    }

    public FciCcdSearchEditor(final SvarFciRunner runner) {
        super(runner, "Result allow_latent_common_causes");
    }

    public FciCcdSearchEditor(final SvarGFciRunner runner) {
        super(runner, "Result allow_latent_common_causes");
    }

    /**
     * Opens up an editor to let the user view the given CcdRunner.
     */
    public FciCcdSearchEditor(final CcdRunner runner) {
        super(runner, "Result allow_latent_common_causes");
    }

    public FciCcdSearchEditor(final CcdRunner2 runner) {
        super(runner, "Result allow_latent_common_causes");
    }

    public Graph getGraph() {
        return getWorkbench().getGraph();
    }

    @Override
    public Map getModelEdgesToDisplay() {
        return getWorkbench().getModelEdgesToDisplay();
    }

    public Map getModelNodesToDisplay() {
        return getWorkbench().getModelNodesToDisplay();
    }

    public void layoutByGraph(final Graph graph) {
        getWorkbench().layoutByGraph(graph);
    }

    public void layoutByKnowledge() {
        final GraphWorkbench resultWorkbench = getWorkbench();
        final Graph graph = resultWorkbench.getGraph();
        final IKnowledge knowledge = (IKnowledge) getAlgorithmRunner().getParams().get("knowledge", new Knowledge2());
        SearchGraphUtils.arrangeByKnowledgeTiers(graph, knowledge);
    }

    public Rectangle getVisibleRect() {
        return getWorkbench().getVisibleRect();
    }


    //==========================PROTECTED METHODS============================//


    /**
     * Construct the toolbar panel.
     */
    protected JPanel getToolbar() {
        final JPanel toolbar = new JPanel();
        getExecuteButton().setText("Execute*");
        getExecuteButton().addActionListener(new ActionListener() {
            public void actionPerformed(final ActionEvent e) {
                execute();
            }
        });

        final Box b1 = Box.createVerticalBox();
        b1.add(getParamsPanel());
        b1.add(Box.createVerticalStrut(10));
        final Box b2 = Box.createHorizontalBox();
        b2.add(Box.createGlue());
        b2.add(getExecuteButton());
        b1.add(b2);

        final Box b3 = Box.createHorizontalBox();
        final JLabel label = new JLabel("<html>" + "*Please note that some" +
                "<br>searches may take a" + "<br>long time to complete." +
                "</html>");
        label.setHorizontalAlignment(SwingConstants.CENTER);
        label.setVerticalAlignment(SwingConstants.CENTER);
        label.setBorder(new TitledBorder(""));
        b3.add(label);

        b1.add(Box.createVerticalStrut(10));
        b1.add(b3);

        toolbar.add(b1);
        return toolbar;
    }

    protected void addSpecialMenus(final JMenuBar menuBar) {
        if (!(getAlgorithmRunner() instanceof IGesRunner)) {
            final JMenu test = new JMenu("Independence");
            menuBar.add(test);

            IndTestMenuItems.addIndependenceTestChoices(test, this);

//            test.addSeparator();
//
//            AlgorithmRunner algorithmRunner = getAlgorithmRunner();

//            if (algorithmRunner instanceof IndTestProducer) {
//                IndTestProducer p = (IndTestProducer) algorithmRunner;
//                IndependenceFactsAction action =
//                        new IndependenceFactsAction(this, p, "Independence Facts...");
//                test.add(action);
//            }
        }

        final JMenu graph = new JMenu("Graph");
        final JMenuItem showDags = new JMenuItem("Show DAGs in forbid_latent_common_causes");
//        JMenuItem meekOrient = new JMenuItem("Meek Orientation");
        final JMenuItem dagInCPDAG = new JMenuItem("Choose DAG in forbid_latent_common_causes");
        final JMenuItem gesOrient = new JMenuItem("Global Score-based Reorientation");
        final JMenuItem nextGraph = new JMenuItem("Next Graph");
        final JMenuItem previousGraph = new JMenuItem("Previous Graph");

//        graph.add(new LayoutMenu(this));
        graph.add(new GraphPropertiesAction(getWorkbench()));
        graph.add(new PathsAction(getWorkbench()));
//        graph.add(new DirectedPathsAction(getWorkbench()));
//        graph.add(new TreksAction(getWorkbench()));
//        graph.add(new AllPathsAction(getWorkbench()));
//        graph.add(new NeighborhoodsAction(getWorkbench()));
        graph.add(new TriplesAction(getWorkbench().getGraph(), getAlgorithmRunner()));
        graph.addSeparator();

//        graph.add(meekOrient);
        graph.add(dagInCPDAG);
        graph.add(gesOrient);
        graph.addSeparator();

        graph.add(previousGraph);
        graph.add(nextGraph);
        graph.addSeparator();

        graph.add(showDags);

        graph.addSeparator();
        graph.add(new JMenuItem(new SelectBidirectedAction(getWorkbench())));
        graph.add(new JMenuItem(new SelectUndirectedAction(getWorkbench())));

        menuBar.add(graph);

        showDags.addActionListener(new ActionListener() {
            public void actionPerformed(final ActionEvent e) {
                final Window owner = (Window) getTopLevelAncestor();

                new WatchedProcess(owner) {
                    public void watch() {

                        // Needs to be a CPDAG search; this isn't checked
                        // before running the algorithm because of allowable
                        // "slop"--e.g. bidirected edges.
                        final AlgorithmRunner runner = getAlgorithmRunner();
                        final Graph graph = runner.getGraph();


                        if (graph == null) {
                            JOptionPane.showMessageDialog(
                                    JOptionUtils.centeringComp(),
                                    "No result gaph.");
                            return;
                        }

//                        if (runner instanceof ImagesRunner) {
//                            GraphScorer scorer = ((ImagesRunner) runner).getGraphScorer();
//                            Graph _graph = ((ImagesRunner) runner).getTopGraphs().get(getIndex()).getGraph();
//
//                            ScoredGraphsDisplay display = new ScoredGraphsDisplay(_graph, scorer);
//                            GraphWorkbench workbench = getWorkbench();
//
//                            EditorWindow editorWindow =
//                                    new EditorWindow(display, "Independence Facts",
//                                            "Close", false, workbench);
//                            DesktopController.getInstance().addEditorWindow(editorWindow, JLayeredPane.PALETTE_LAYER);
//                            editorWindow.setVisible(true);
//                        }
//                        else {
                        final CPDAGDisplay display = new CPDAGDisplay(graph);
                        final GraphWorkbench workbench = getWorkbench();

                        final EditorWindow editorWindow =
                                new EditorWindow(display, "Independence Facts",
                                        "Close", false, workbench);
                        DesktopController.getInstance().addEditorWindow(editorWindow, JLayeredPane.PALETTE_LAYER);
                        editorWindow.setVisible(true);
//                        }
                    }
                };
            }
        });

//        meekOrient.addActionListener(new ActionListener() {
//            public void actionPerformed(ActionEvent e) {
//                ImpliedOrientation rules = getAlgorithmRunner().getMeekRules();
//                rules.setKnowledge((IKnowledge) getAlgorithmRunner().getParams().get("knowledge", new Knowledge2()));
//                rules.orientImplied(getGraph());
//                getGraphHistory().add(getGraph());
//                getWorkbench().setGraph(getGraph());
//                firePropertyChange("modelChanged", null, null);
//            }
//        });

        dagInCPDAG.addActionListener(new ActionListener() {
            public void actionPerformed(final ActionEvent e) {
                final Graph graph = new EdgeListGraph(getGraph());

                // Removing bidirected edges from the CPDAG before selecting a DAG.                                   4
                for (final Edge edge : graph.getEdges()) {
                    if (Edges.isBidirectedEdge(edge)) {
                        graph.removeEdge(edge);
                    }
                }

                final Graph dag = SearchGraphUtils.dagFromCPDAG(graph);

                getGraphHistory().add(dag);
                getWorkbench().setGraph(dag);

                ((AbstractAlgorithmRunner) getAlgorithmRunner()).setResultGraph(dag);
                firePropertyChange("modelChanged", null, null);
            }
        });

        gesOrient.addActionListener(new ActionListener() {
            public void actionPerformed(final ActionEvent e) {
                final DataModel dataModel = getAlgorithmRunner().getDataModel();

                final Graph graph = SearchGraphUtils.reorient(getGraph(), dataModel, getKnowledge());

                getGraphHistory().add(graph);
                getWorkbench().setGraph(graph);
                firePropertyChange("modelChanged", null, null);
            }

        });

        nextGraph.addActionListener(new ActionListener() {
            public void actionPerformed(final ActionEvent e) {
                final Graph next = getGraphHistory().next();
                getWorkbench().setGraph(next);
                ((AbstractAlgorithmRunner) getAlgorithmRunner()).setResultGraph(next);
                firePropertyChange("modelChanged", null, null);
            }
        });

        previousGraph.addActionListener(new ActionListener() {
            public void actionPerformed(final ActionEvent e) {
                final Graph previous = getGraphHistory().previous();
                getWorkbench().setGraph(previous);
                ((AbstractAlgorithmRunner) getAlgorithmRunner()).setResultGraph(previous);
                firePropertyChange("modelChanged", null, null);
            }
        });

//        if (getAlgorithmRunner().supportsKnowledge()) {
//            menuBar.add(new Knowledge2Menu(this));
//        }

        menuBar.add(new LayoutMenu(this));
    }

    public Graph getSourceGraph() {
        Graph sourceGraph = getWorkbench().getGraph();

        if (sourceGraph == null) {
            sourceGraph = getAlgorithmRunner().getSourceGraph();
        }
        return sourceGraph;
    }

    public List<String> getVarNames() {
        final Parameters params = getAlgorithmRunner().getParams();
        return (List<String>) params.get("varNames", null);
    }

    public void setTestType(final IndTestType testType) {
        super.setTestType(testType);
    }

    public IndTestType getTestType() {
        return super.getTestType();
    }

    public void setKnowledge(final IKnowledge knowledge) {
        getAlgorithmRunner().getParams().set("knowledge", knowledge);
    }

    public IKnowledge getKnowledge() {
        return (IKnowledge) getAlgorithmRunner().getParams().get("knowledge", new Knowledge2());
    }

    //================================PRIVATE METHODS====================//

    private JPanel getParamsPanel() {
        final JPanel paramsPanel = new JPanel();

        final Box b2 = Box.createVerticalBox();

        final JComponent indTestParamBox = getIndTestParamBox();
        if (indTestParamBox != null) {
            b2.add(indTestParamBox);
        }

        paramsPanel.add(b2);
        paramsPanel.setBorder(new TitledBorder("Parameters"));
        return paramsPanel;
    }

    private JComponent getIndTestParamBox() {
        final Parameters params = getAlgorithmRunner().getParams();
        return getIndTestParamBox(params);
    }

    /**
     * Factory to return the correct param editor for independence test params.
     * This will go in a little box in the search editor.
     */
    private JComponent getIndTestParamBox(final Parameters params) {
        if (params == null) {
            throw new NullPointerException();
        }

        if (params instanceof Parameters) {
            final FgesRunner fgesRunner = ((FgesRunner) getAlgorithmRunner());
            return new FgesIndTestParamsEditor(params, fgesRunner.getType());
        }

//        if (params instanceof LagIndTestParams) {
//            return new TimeSeriesIndTestParamsEditor(
//                    (LagIndTestParams) params);
//        }
//
//        if (params instanceof GraphIndTestParams) {
//            return new IndTestParamsEditor((GraphIndTestParams) params);
//        }
//
//        if (params instanceof Parameters) {
//            return new PcIndTestParamsEditor((Parameters) params);
//        }

//        if (params instanceof ParamsOld) {
//            throw new IllegalArgumentException();
////            return new ParamsEditorOld((ParamsOld) params);
//        }
//
//        if (params instanceof Parameters) {
//            return new ParamsEditor((Parameters) params);
//        }
//
//        if (params instanceof Parameters) {
//            return new ParamsEditor((Parameters) params);
//        }

        return new IndTestParamsEditor(params);
    }

    protected void doDefaultArrangement(final Graph resultGraph) {
        if (getLatestWorkbenchGraph() != null) {   //(alreadyLaidOut) {
            GraphUtils.arrangeBySourceGraph(resultGraph,
                    getLatestWorkbenchGraph());
        } else if (getKnowledge().isDefaultToKnowledgeLayout()) {
            SearchGraphUtils.arrangeByKnowledgeTiers(resultGraph,
                    getKnowledge());
//            alreadyLaidOut = true;
        } else {
            GraphUtils.circleLayout(resultGraph, 200, 200, 150);
//            alreadyLaidOut = true;
        }
    }
}





