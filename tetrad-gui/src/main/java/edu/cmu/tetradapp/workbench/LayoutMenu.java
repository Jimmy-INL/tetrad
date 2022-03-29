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

package edu.cmu.tetradapp.workbench;

import edu.cmu.tetrad.graph.Graph;
import edu.cmu.tetrad.graph.Node;
import edu.cmu.tetrad.graph.NodeType;
import edu.cmu.tetradapp.util.CopyLayoutAction;
import edu.cmu.tetradapp.util.LayoutEditable;
import edu.cmu.tetradapp.util.PasteLayoutAction;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;

/**
 * Builds a menu for layout operations on graphs. Interacts with classes that
 * implement the LayoutEditable interface.
 *
 * @author Joseph Ramsey
 */
public class LayoutMenu extends JMenu {
    private final LayoutEditable layoutEditable;
    private final CopyLayoutAction copyLayoutAction;

    public LayoutMenu(final LayoutEditable layoutEditable) {
        super("Layout");
        this.layoutEditable = layoutEditable;

        if (layoutEditable.getGraph().isTimeLagModel()) {

            final JMenuItem topToBottom = new JMenuItem("Top to bottom");
            add(topToBottom);

            topToBottom.addActionListener(new ActionListener() {
                public void actionPerformed(final ActionEvent e) {
                    LayoutUtils.topToBottomLayout(getLayoutEditable());

                    // Copy the laid out graph to the clipboard.
                    getCopyLayoutAction().actionPerformed(null);
                }
            });

            final JMenuItem leftToRight = new JMenuItem("Left to right");
            add(leftToRight);

            leftToRight.addActionListener(new ActionListener() {
                public void actionPerformed(final ActionEvent e) {
                    LayoutUtils.leftToRightLayout(getLayoutEditable());

                    // Copy the laid out graph to the clipboard.
                    getCopyLayoutAction().actionPerformed(null);
                }
            });

            final JMenuItem bottomToTop = new JMenuItem("Bottom to top");
            add(bottomToTop);

            bottomToTop.addActionListener(new ActionListener() {
                public void actionPerformed(final ActionEvent e) {
                    LayoutUtils.bottomToTopLayout(getLayoutEditable());

                    // Copy the laid out graph to the clipboard.
                    getCopyLayoutAction().actionPerformed(null);
                }
            });

            final JMenuItem rightToLeft = new JMenuItem("Right to left");
            add(rightToLeft);

            rightToLeft.addActionListener(new ActionListener() {
                public void actionPerformed(final ActionEvent e) {
                    LayoutUtils.rightToLeftLayout(getLayoutEditable());

                    // Copy the laid out graph to the clipboard.
                    getCopyLayoutAction().actionPerformed(null);
                }
            });

            final JMenuItem likeLag0 = new JMenuItem("Copy lag 0");
            add(likeLag0);

            likeLag0.addActionListener(new ActionListener() {
                public void actionPerformed(final ActionEvent e) {
                    if (LayoutUtils.getLayout() == LayoutUtils.Layout.topToBottom
                            || LayoutUtils.getLayout() == LayoutUtils.Layout.lag0TopToBottom) {
                        LayoutUtils.copyLag0LayoutTopToBottom(getLayoutEditable());
                    } else if (LayoutUtils.getLayout() == LayoutUtils.Layout.bottomToTop
                            || LayoutUtils.getLayout() == LayoutUtils.Layout.lag0BottomToTop) {
                        LayoutUtils.copyLag0LayoutBottomToTop(getLayoutEditable());
                    } else if (LayoutUtils.getLayout() == LayoutUtils.Layout.leftToRight
                            || LayoutUtils.getLayout() == LayoutUtils.Layout.lag0LeftToRight) {
                        LayoutUtils.copyLag0LayoutLeftToRight(getLayoutEditable());
                    } else if (LayoutUtils.getLayout() == LayoutUtils.Layout.rightToLeft
                            || LayoutUtils.getLayout() == LayoutUtils.Layout.lag0RightToLeft) {
                        LayoutUtils.copyLag0LayoutRightToLeft(getLayoutEditable());
                    } else {
//                        LayoutUtils.topToBottomLayout(getLayoutEditable());
                    }

                    // Copy the laid out graph to the clipboard.
                    getCopyLayoutAction().actionPerformed(null);
                }
            });

            addSeparator();
        }


        final JMenuItem circleLayout = new JMenuItem("Circle");
        add(circleLayout);

        circleLayout.addActionListener(new ActionListener() {
            public void actionPerformed(final ActionEvent e) {
                LayoutUtils.circleLayout(getLayoutEditable());

                // Copy the laid out graph to the clipboard.
                getCopyLayoutAction().actionPerformed(null);
            }

        });

        if (getLayoutEditable().getKnowledge() != null) {
            final JMenuItem knowledgeTiersLayout = new JMenuItem("Knowledge Tiers");
            add(knowledgeTiersLayout);

            knowledgeTiersLayout.addActionListener(new ActionListener() {
                public void actionPerformed(final ActionEvent e) {
                    LayoutUtils.knowledgeLayout(getLayoutEditable());

                    // Copy the laid out graph to the clipboard.
                    getCopyLayoutAction().actionPerformed(null);
                }
            });
        }

//        if (getLayoutEditable().getSourceGraph() != null) {
//            JMenuItem lastResultLayout = new JMenuItem("Source Graph");
//            add(lastResultLayout);
//
//            lastResultLayout.addActionListener(new ActionListener() {
//                public void actionPerformed(ActionEvent e) {
//                    LayoutUtils.sourceGraphLayout(getLayoutEditable());
//
//                    // Copy the laid out graph to the clipboard.
//                    getCopyLayoutAction().actionPerformed(null);
//                }
//            });
//        }

//        JMenuItem layeredDrawing = new JMenuItem("Layered Drawing");
//        add(layeredDrawing);
//
//        layeredDrawing.addActionListener(new ActionListener() {
//            public void actionPerformed(ActionEvent e) {
//                LayoutEditable layoutEditable = getLayoutEditable();
//                LayoutUtils.layeredDrawingLayout(layoutEditable);
//
//                // Copy the laid out graph to the clipboard.
//                getCopyLayoutAction().actionPerformed(null);
//            }
//        });


        final JMenuItem fruchtermanReingold = new JMenuItem("Fruchterman-Reingold");
        add(fruchtermanReingold);

        fruchtermanReingold.addActionListener(new ActionListener() {
            public void actionPerformed(final ActionEvent e) {
                LayoutUtils.fruchtermanReingoldLayout(getLayoutEditable());

                // Copy the laid out graph to the clipboard.
                getCopyLayoutAction().actionPerformed(null);
            }
        });

        final JMenuItem kamadaKawai = new JMenuItem("Kamada-Kawai");
        add(kamadaKawai);

        kamadaKawai.addActionListener(new ActionListener() {
            public void actionPerformed(final ActionEvent e) {
                final LayoutEditable layoutEditable = getLayoutEditable();
                LayoutUtils.kamadaKawaiLayout(layoutEditable);

                // Copy the laid out graph to the clipboard.
                getCopyLayoutAction().actionPerformed(null);
            }
        });

        final JMenuItem distanceFromSelected = new JMenuItem("Distance From Selected");
        add(distanceFromSelected);

        distanceFromSelected.addActionListener(new ActionListener() {
            public void actionPerformed(final ActionEvent e) {
                final LayoutEditable layoutEditable = getLayoutEditable();
                LayoutUtils.distanceFromSelectedLayout(layoutEditable);

                // Copy the laid out graph to the clipboard.
                getCopyLayoutAction().actionPerformed(null);
            }
        });

        final JMenuItem causalOrder = new JMenuItem("Causal Order");
        add(causalOrder);

        causalOrder.addActionListener(new ActionListener() {
            public void actionPerformed(final ActionEvent e) {
                final LayoutEditable layoutEditable = getLayoutEditable();
                final Graph graph = layoutEditable.getGraph();

                for (final Node node : new ArrayList<>(graph.getNodes())) {
                    if (node.getNodeType() == NodeType.ERROR) {
                        graph.removeNode(node);
                    }
                }

                final CausalOrder layout1 = new CausalOrder(layoutEditable);
                layout1.doLayout();
                layoutEditable.layoutByGraph(graph);
                LayoutUtils.layout = LayoutUtils.Layout.distanceFromSelected;

                // Copy the laid out graph to the clipboard.
                getCopyLayoutAction().actionPerformed(null);
            }
        });

//        JMenuItem brainSpecial = new JMenuItem("Brain Special");
//        add(brainSpecial);
//
//        brainSpecial.addActionListener(new ActionListener() {
//            public void actionPerformed(ActionEvent e) {
//                final LayoutEditable layoutEditable = getLayoutEditable();
//                Graph graph = layoutEditable.getGraph();
//
//                for (Node node : new ArrayList<Node>(graph.getNodes())) {
//                    if (node.getNodeType() == NodeType.ERROR) {
//                        graph.removeNode(node);
//                    }
//                }
//
//                BrainSpecial layout1 = new BrainSpecial(layoutEditable);
//                layout1.doLayout();
//                layoutEditable.layoutByGraph(graph);
//                LayoutUtils.layout = LayoutUtils.Layout.distanceFromSelected;
//
////                 Copy the laid out graph to the clipboard.
//                getCopyLayoutAction().actionPerformed(null);
//            }
//        });

        addSeparator();

        this.copyLayoutAction = new CopyLayoutAction(getLayoutEditable());
        add(getCopyLayoutAction());
        add(new PasteLayoutAction(getLayoutEditable()));
    }

    private LayoutEditable getLayoutEditable() {
        return this.layoutEditable;
    }

    private CopyLayoutAction getCopyLayoutAction() {
        return this.copyLayoutAction;
    }


}





