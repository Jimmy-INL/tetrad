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
import edu.cmu.tetrad.graph.Graph;
import edu.cmu.tetrad.graph.Node;
import edu.cmu.tetrad.graph.SemGraph;
import edu.cmu.tetrad.sem.GeneralizedSemPm;
import edu.cmu.tetradapp.model.GeneralizedSemPmWrapper;
import edu.cmu.tetradapp.session.DelegatesEditing;
import edu.cmu.tetradapp.util.DesktopController;
import edu.cmu.tetradapp.util.IntTextField;
import edu.cmu.tetradapp.util.LayoutEditable;
import edu.cmu.tetradapp.workbench.LayoutMenu;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.text.ParseException;
import java.util.HashMap;
import java.util.Map;
import java.util.prefs.Preferences;

/**
 * Edits a SEM PM model.
 *
 * @author Donald Crimbchin
 * @author josephramsey
 * @version $Id: $Id
 */
public final class GeneralizedSemPmEditor extends JPanel implements DelegatesEditing,
        LayoutEditable {

    /**
     * The SemPm being edited.
     */
    private final GeneralizedSemPm semPm;
    /**
     * A reference to the error terms menu item so it can be reset.
     */
    private final JMenuItem errorTerms;
    /**
     * A common map of nodes to launched editors so that they can all be closed when this editor is closed.
     */
    private final Map<Object, EditorWindow> launchedEditors = new HashMap<>();
    /**
     * The graphical editor for the SemPm.
     */
    private GeneralizedSemPmGraphicalEditor graphicalEditor;
    /**
     * The graphical editor for the SemPm.
     */
    private GeneralizedSemPmListEditor listEditor;
    /**
     * The editor for initial value distributions.
     */
    private GeneralizedSemPmParamsEditor parameterEditor;

    /**
     * <p>Constructor for GeneralizedSemPmEditor.</p>
     *
     * @param wrapper a {@link edu.cmu.tetradapp.model.GeneralizedSemPmWrapper} object
     */
    public GeneralizedSemPmEditor(GeneralizedSemPmWrapper wrapper) {
        GeneralizedSemPm semPm = wrapper.getSemPm();
        if (semPm == null) {
            throw new NullPointerException("Generalized SEM PM must not be null.");
        }

        this.semPm = semPm;
        setLayout(new BorderLayout());

        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.add("Variables", listEditor());
        tabbedPane.add("Parameters", initialValuesEditor());
        tabbedPane.add("Graph", graphicalEditor());

        tabbedPane.addChangeListener(new ChangeListener() {
            public void stateChanged(ChangeEvent changeEvent) {
                GeneralizedSemPmEditor.this.graphicalEditor.refreshLabels();
                GeneralizedSemPmEditor.this.listEditor.refreshLabels();
                GeneralizedSemPmEditor.this.parameterEditor.refreshLabels();
            }
        });

        add(tabbedPane, BorderLayout.CENTER);

        JMenuBar menuBar = new JMenuBar();
        JMenu file = new JMenu("File");
        menuBar.add(file);
//        file.add(new SaveScreenshot(this, true, "Save Screenshot..."));
        file.add(new SaveComponentImage(this.graphicalEditor.getWorkbench(),
                "Save Graph Image..."));

        // By default, hide the error terms.
        SemGraph graph = (SemGraph) this.graphicalEditor.getWorkbench().getGraph();
        boolean shown = wrapper.isShowErrors();
        graph.setShowErrorTerms(shown);

        this.errorTerms = new JMenuItem();

        if (shown) {
            this.errorTerms.setText("Hide Error Terms");
        } else {
            this.errorTerms.setText("Show Error Terms");
        }

        this.errorTerms.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                JMenuItem menuItem = (JMenuItem) e.getSource();

                if ("Hide Error Terms".equals(menuItem.getText())) {
                    menuItem.setText("Show Error Terms");
                    SemGraph graph = (SemGraph) GeneralizedSemPmEditor.this.graphicalEditor.getWorkbench().getGraph();
                    graph.setShowErrorTerms(false);
                    wrapper.setShowErrors(false);
                    graphicalEditor().refreshLabels();
                } else if ("Show Error Terms".equals(menuItem.getText())) {
                    menuItem.setText("Hide Error Terms");
                    SemGraph graph = (SemGraph) GeneralizedSemPmEditor.this.graphicalEditor.getWorkbench().getGraph();
                    graph.setShowErrorTerms(true);
                    wrapper.setShowErrors(true);
                    graphicalEditor().refreshLabels();
                }
            }
        });

        JMenuItem templateMenu = new JMenuItem("Apply Templates...");

        templateMenu.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent actionEvent) {
                GeneralizedTemplateEditor editor = new GeneralizedTemplateEditor(getSemPm());

                String tabTitle = tabbedPane.getTitleAt(tabbedPane.getSelectedIndex());

                if ("Parameters".equals(tabTitle)) {
//                    editor.useParametersAsStartup();
                }

                JPanel panel = new JPanel();
                panel.setLayout(new BorderLayout());
                panel.add(editor, BorderLayout.CENTER);

                EditorWindow editorWindow
                        = new EditorWindow(panel, "Apply Templates", "OK", false, GeneralizedSemPmEditor.this);

                DesktopController.getInstance().addEditorWindow(editorWindow, JLayeredPane.PALETTE_LAYER);
                editorWindow.pack();
                editorWindow.setVisible(true);

                editorWindow.addInternalFrameListener(new InternalFrameAdapter() {
                    public void internalFrameClosing(InternalFrameEvent internalFrameEvent) {
                        if (!editorWindow.isCanceled()) {
                            GeneralizedSemPm _semPm = editor.getSemPm();
                            GeneralizedSemPm semPm = GeneralizedSemPmEditor.this.semPm;

                            for (Node node : _semPm.getNodes()) {
                                try {
                                    semPm.setNodeExpression(node, _semPm.getNodeExpressionString(node));
                                } catch (ParseException e) {
                                    JOptionPane.showMessageDialog(GeneralizedSemPmEditor.this,
                                            "Could not set the expression for " + node + " to "
                                                    + _semPm.getNodeExpressionString(node));
                                }
                            }

                            for (String startsWith : _semPm.startsWithPrefixes()) {
                                try {
                                    semPm.setStartsWithParametersTemplate(startsWith, _semPm.getStartsWithParameterTemplate(startsWith));
                                } catch (ParseException e) {
                                    JOptionPane.showMessageDialog(GeneralizedSemPmEditor.this,
                                            "Could not set the expression for " + startsWith + " to "
                                                    + _semPm.getParameterExpressionString(_semPm.getStartsWithParameterTemplate(startsWith)));
                                }
                            }

                            for (String parameter : _semPm.getParameters()) {
                                try {
                                    boolean found = false;

                                    for (String startsWith : _semPm.startsWithPrefixes()) {
                                        if (parameter.startsWith(startsWith)) {
                                            semPm.setParameterExpression(parameter, _semPm.getStartsWithParameterTemplate(startsWith));
                                            found = true;
                                            break;
                                        }
                                    }

                                    if (!found) {
                                        semPm.setParameterExpression(parameter, _semPm.getParameterExpressionString(parameter));
                                    }
                                } catch (ParseException e) {
                                    JOptionPane.showMessageDialog(GeneralizedSemPmEditor.this,
                                            "Could not set the expression for " + parameter + " to "
                                                    + _semPm.getParameterExpressionString(parameter));
                                }
                            }

                            try {
                                semPm.setVariablesTemplate(_semPm.getVariablesTemplate());
                                semPm.setErrorsTemplate(_semPm.getErrorsTemplate());
                                semPm.setParametersTemplate(_semPm.getParametersTemplate());
                            } catch (ParseException e) {
                                throw new RuntimeException("Could not set templates from copy of GeneralizedPm to "
                                        + "actual GeneralizedPm.");
                            }

                            GeneralizedSemPmEditor.this.graphicalEditor.refreshLabels();
                            GeneralizedSemPmEditor.this.listEditor.refreshLabels();
                            GeneralizedSemPmEditor.this.parameterEditor.refreshLabels();

                            firePropertyChange("modelChanged", null, null);
                        }
                    }
                });

            }
        });

        JMenuItem lengthCutoff = new JMenuItem("Formula Cutoff");

        lengthCutoff.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                int length = Preferences.userRoot().getInt("maxExpressionLength", 25);

                IntTextField lengthField = new IntTextField(length, 4);
                lengthField.setFilter(new IntTextField.Filter() {
                    public int filter(int value, int oldValue) {
                        try {
                            if (value > 0) {
                                Preferences.userRoot().putInt("maxExpressionLength", value);
                                return value;
                            } else {
                                return 0;
                            }
                        } catch (Exception e) {
                            return oldValue;
                        }
                    }
                });

                Box b = Box.createVerticalBox();

                Box b1 = Box.createHorizontalBox();
                b1.add(new JLabel("Formulas longer than "));
                b1.add(lengthField);
                b1.add(new JLabel(" will be replaced in the graph by \"--long formula--\"."));
                b.add(b1);

                b.setBorder(new EmptyBorder(5, 5, 5, 5));

                JPanel panel = new JPanel();
                panel.setLayout(new BorderLayout());
                panel.add(b, BorderLayout.CENTER);

                EditorWindow editorWindow
                        = new EditorWindow(panel, "Apply Templates", "OK", false, GeneralizedSemPmEditor.this);

                editorWindow.addInternalFrameListener(new InternalFrameAdapter() {
                    public void internalFrameClosing(InternalFrameEvent event) {
                        GeneralizedSemPmEditor.this.graphicalEditor.refreshLabels();
                    }
                });

                DesktopController.getInstance().addEditorWindow(editorWindow, JLayeredPane.PALETTE_LAYER);
                editorWindow.pack();
                editorWindow.setVisible(true);
            }
        });

        JMenu params = new JMenu("Tools");
        params.add(this.errorTerms);
        params.add(templateMenu);
        params.add(lengthCutoff);
        menuBar.add(params);

        menuBar.add(new LayoutMenu(this));

        add(menuBar, BorderLayout.NORTH);

        // When the dialog closes, we want to close all generalized expression editors. We do this by
        // detecting when the ancestor of this editor has been removed.
        addAncestorListener(new AncestorListener() {
            public void ancestorAdded(AncestorEvent ancestorEvent) {
            }

            public void ancestorRemoved(AncestorEvent ancestorEvent) {
                for (Object o : GeneralizedSemPmEditor.this.launchedEditors.keySet()) {
                    EditorWindow window = GeneralizedSemPmEditor.this.launchedEditors.get(o);
                    window.closeDialog();
                }
            }

            public void ancestorMoved(AncestorEvent ancestorEvent) {
            }
        });

    }

    private SemGraph getSemGraph() {
        return this.semPm.getGraph();
    }

    /**
     * <p>getEditDelegate.</p>
     *
     * @return a {@link javax.swing.JComponent} object
     */
    public JComponent getEditDelegate() {
        return graphicalEditor();
    }

    /**
     * <p>getGraph.</p>
     *
     * @return a {@link edu.cmu.tetrad.graph.Graph} object
     */
    public Graph getGraph() {
        return graphicalEditor().getWorkbench().getGraph();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Map getModelEdgesToDisplay() {
        return graphicalEditor().getWorkbench().getModelEdgesToDisplay();
    }

    /**
     * <p>getModelNodesToDisplay.</p>
     *
     * @return a {@link java.util.Map} object
     */
    public Map getModelNodesToDisplay() {
        return graphicalEditor().getWorkbench().getModelNodesToDisplay();
    }

    /**
     * <p>getKnowledge.</p>
     *
     * @return a {@link edu.cmu.tetrad.data.Knowledge} object
     */
    public Knowledge getKnowledge() {
        return graphicalEditor().getWorkbench().getKnowledge();
    }

    /**
     * <p>getSourceGraph.</p>
     *
     * @return a {@link edu.cmu.tetrad.graph.Graph} object
     */
    public Graph getSourceGraph() {
        return graphicalEditor().getWorkbench().getSourceGraph();
    }

    /**
     * {@inheritDoc}
     */
    public void layoutByGraph(Graph graph) {
        SemGraph _graph = (SemGraph) graphicalEditor().getWorkbench().getGraph();
        _graph.setShowErrorTerms(false);
        graphicalEditor().getWorkbench().layoutByGraph(graph);
        _graph.resetErrorPositions();

        // Oh no do't you dare do this! You will lose all labels! jdramsey 4/17/10
//        graphicalEditor().getWorkbench().setGraph(_graph);
        this.errorTerms.setText("Show Error Terms");
    }

    /**
     * <p>layoutByKnowledge.</p>
     */
    public void layoutByKnowledge() {
        SemGraph _graph = (SemGraph) graphicalEditor().getWorkbench().getGraph();
        _graph.setShowErrorTerms(false);
        graphicalEditor().getWorkbench().layoutByKnowledge();
        _graph.resetErrorPositions();
//        graphicalEditor().getWorkbench().setGraph(_graph);
        this.errorTerms.setText("Show Error Terms");
    }

    //========================PRIVATE METHODS===========================//
    private GeneralizedSemPm getSemPm() {
        return this.semPm;
    }

    private GeneralizedSemPmGraphicalEditor graphicalEditor() {
        if (this.graphicalEditor == null) {
            this.graphicalEditor = new GeneralizedSemPmGraphicalEditor(getSemPm(), this.launchedEditors);
            this.graphicalEditor.enableEditing(false);

            this.graphicalEditor.addPropertyChangeListener(new PropertyChangeListener() {
                public void propertyChange(PropertyChangeEvent event) {
                    if ("modelChanged".equals(event.getPropertyName())) {
                        firePropertyChange("modelChanged", null, null);
                    }
                }
            });
        }
        return this.graphicalEditor;
    }

    private GeneralizedSemPmListEditor listEditor() {
        if (this.listEditor == null) {
            this.listEditor = new GeneralizedSemPmListEditor(getSemPm(), initialValuesEditor(), this.launchedEditors);

            this.listEditor.addPropertyChangeListener(new PropertyChangeListener() {
                public void propertyChange(PropertyChangeEvent event) {
                    if ("modelChanged".equals(event.getPropertyName())) {
                        firePropertyChange("modelChanged", null, null);
                    }
                }
            });
        }
        return this.listEditor;
    }

    private GeneralizedSemPmParamsEditor initialValuesEditor() {
        if (this.parameterEditor == null) {
            this.parameterEditor = new GeneralizedSemPmParamsEditor(getSemPm(), this.launchedEditors);
        }
        return this.parameterEditor;
    }
}
