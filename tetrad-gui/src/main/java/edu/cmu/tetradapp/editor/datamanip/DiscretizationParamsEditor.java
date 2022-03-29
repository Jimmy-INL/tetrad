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

package edu.cmu.tetradapp.editor.datamanip;

import edu.cmu.tetrad.data.*;
import edu.cmu.tetrad.graph.Node;
import edu.cmu.tetrad.util.Parameters;
import edu.cmu.tetradapp.editor.FinalizingParameterEditor;
import edu.cmu.tetradapp.model.DataWrapper;
import edu.cmu.tetradapp.util.IntSpinner;
import edu.cmu.tetradapp.workbench.LayoutUtils;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;
import java.util.*;
import java.util.prefs.Preferences;

/**
 * Allows the user to specify how a selected list of columns should be
 * discretized.
 *
 * @author Tyler Gibson
 * @author Joseph Ramsey
 */
public class DiscretizationParamsEditor extends JPanel implements FinalizingParameterEditor {

    /**
     * The data set that will be discretized.
     */
    private DataSet sourceDataSet;

    /**
     * A map from nodes to their editors.
     */
    private final Map<Node, DiscretizationEditor> nodeEditors = new HashMap<>();


    /**
     * The params we are editing.
     */
//    private DiscretizationParams params;


    /**
     * A tabbed pane to store the editors in.
     */
    private JTabbedPane editorPane;

    private Parameters parameters;


    /**
     * Constructs a new editor that will allow the user to specify how to
     * discretize each of the columns in the given list. The editor will return
     * the discretized data set.
     */
    public DiscretizationParamsEditor() {

    }

    //============================= Public Methods ===================================//


    /**
     * Sets up the GUI.
     */
    public void setup() {
        System.out.println("setup");

        final List<Node> variables = this.sourceDataSet.getVariables();
        final List<Node> allVariables = new LinkedList<>();
        final List<Node> discretizeVars = new LinkedList<>();

        for (final Node node : variables) {
            discretizeVars.add(node);
            allVariables.add(node);
        }

        for (final Node node : allVariables) {
            this.nodeEditors.put(node, createEditor(node));
        }

        finalizeEdit();

        // create discretized ar list.
        /*
      The list of variables to discretize.
     */
        final JList discretizeVariableList = new JList(new VariableListModel(allVariables));
        discretizeVariableList.setCellRenderer(new VariableBoxRenderer());
        discretizeVariableList.getSelectionModel().setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        discretizeVariableList.addListSelectionListener(new ListSelectionListener() {
            public void valueChanged(final ListSelectionEvent e) {
                final JList list = (JList) e.getSource();
                final List<Node> selected = getSelected(list);

                finalizeEdit();

                if (selected.size() == 1) {
                    DiscretizationParamsEditor.this.editorPane.removeAll();
                    final Node node = selected.get(0);
                    DiscretizationParamsEditor.this.editorPane.add(node.getName(), (JPanel) DiscretizationParamsEditor.this.nodeEditors.get(node));
                } else if (1 < selected.size()) {
                    if (allContinuous(selected)) {
                        DiscretizationParamsEditor.this.editorPane.removeAll();
                        final Node first = selected.get(0);
                        final Node last = selected.get(selected.size() - 1);
                        final String label = first.getName() + " - " + last.getName();
                        DiscretizationParamsEditor.this.editorPane.add(label, new VariableSelectionEditor(selected));
                    } else {
                        DiscretizationParamsEditor.this.editorPane.removeAll();
                    }
                }
            }

            private boolean allContinuous(final List<Node> selected) {
                for (final Node node : selected) {
                    if (!(node instanceof ContinuousVariable)) {
                        return false;
                    }
                }

                return true;
            }
        });
        // Add entries for previously selected variables.
        for (final Node node : discretizeVars) {
            if (node instanceof ContinuousVariable) {
                final ContinuousVariable continuousVariable = (ContinuousVariable) node;
                final ContinuousDiscretizationEditor editor = new ContinuousDiscretizationEditor(
                        this.sourceDataSet, continuousVariable);
                final DiscretizationSpec spec = getSpecs().get(node);
                if (spec == null) continue;
                editor.setDiscretizationSpec(spec);
                this.nodeEditors.put(node, editor);
            } else if (node instanceof DiscreteVariable) {
                final DiscreteVariable variable = (DiscreteVariable) node;
                final DiscreteDiscretizationEditor editor = new DiscreteDiscretizationEditor(variable);
                final DiscretizationSpec spec = getSpecs().get(node);
                if (spec == null) continue;
                editor.setDiscretizationSpec(spec);
                this.nodeEditors.put(node, editor);
            }
        }

        // set up the tabbed pane
        this.editorPane = new JTabbedPane();

        final JScrollPane editorScrollPane = new JScrollPane(this.editorPane);
        editorScrollPane.setPreferredSize(new Dimension(400, 350));

//        JCheckBox copyUnselectedCheckBox =
//                new JCheckBox("Copy unselected columns into new data set");
//        copyUnselectedCheckBox.setHorizontalTextPosition(AbstractButton.LEFT);
//        copyUnselectedCheckBox.setSelected(Preferences.userRoot().getBoolean(
//                "copyUnselectedColumns", false));
//        copyUnselectedCheckBox.addActionListener(new ActionListener() {
//            public void actionPerformed(ActionEvent e) {
//                JCheckBox checkBox = (JCheckBox) e.getSource();
//                Preferences.userRoot().putBoolean("copyUnselectedColumns",
//                        checkBox.isSelected());
//            }
//        });

        discretizeVariableList.setSelectedIndex(0);

        final Box hBox = Box.createHorizontalBox();
        hBox.add(Box.createHorizontalStrut(5));

        // build the continuous variable box.
        final Box selectionBox = Box.createVerticalBox();

        selectionBox.add(Box.createVerticalStrut(5));
        selectionBox.add(Box.createVerticalGlue());

        // build the discrete variable box
        final Box discreteSelectionBox = Box.createVerticalBox();
        final JLabel discreteLabel = new JLabel("Variables:");

        final JScrollPane discreteListPane = new JScrollPane(discretizeVariableList);
        final int width2 = Math.max(100, discreteLabel.getPreferredSize().width);
        LayoutUtils.setAllSizes(discreteListPane, new Dimension(width2, 350 - discreteLabel.getPreferredSize().height));

        discreteSelectionBox.add(Box.createVerticalStrut(5));
        discreteSelectionBox.add(LayoutUtils.leftAlignJLabel(discreteLabel));
        discreteSelectionBox.add(discreteListPane);
        discreteSelectionBox.add(Box.createVerticalGlue());

        hBox.add(selectionBox);
        hBox.add(Box.createHorizontalStrut(4));
        hBox.add(discreteSelectionBox);
        hBox.add(Box.createHorizontalStrut(8));

        final Box vBox = Box.createVerticalBox();
        vBox.add(Box.createVerticalStrut(5));
        vBox.add(editorScrollPane);

//        Box b4 = Box.createHorizontalBox();
//        b4.add(Box.createHorizontalGlue());
//        b4.add(copyUnselectedCheckBox);

//        vBox.add(b4);
//        vBox.add(Box.createVerticalStrut(10));

        hBox.add(vBox);
        hBox.add(Box.createHorizontalStrut(5));

        add(hBox, BorderLayout.CENTER);
    }


    /**
     * Adds all the discretization info to the params.
     *
     * @return true iff the edit was finalized.
     */
    public boolean finalizeEdit() {
        // if there was no editors, then nothing can be done so return false.
        if (this.nodeEditors.isEmpty()) {
            return false;
        }
        final Map<Node, DiscretizationSpec> map = new HashMap<>();
        for (final Node node : this.nodeEditors.keySet()) {
            final DiscretizationEditor editor = this.nodeEditors.get(node);
            map.put(node, editor.getDiscretizationSpec());
        }
        this.parameters.set("discretizationSpecs", map);
        return true;
    }


    /**
     * Sets the previous params, must be <code>DiscretizationParams</code>.
     *
     * @param params
     */
    public void setParams(final Parameters params) {
        this.parameters = params;
        this.parameters.set("discretizationSpecs", new HashMap<Node, DiscretizationSpec>());
    }

    /**
     * The parant model should be a <code>DataWrapper</code>.
     */
    public void setParentModels(final Object[] parentModels) {
        if (parentModels == null || parentModels.length == 0) {
            throw new IllegalArgumentException("There must be parent model");
        }
        DataWrapper data = null;
        for (final Object parent : parentModels) {
            if (parent instanceof DataWrapper) {
                data = (DataWrapper) parent;
            }
        }
        if (data == null) {
            throw new IllegalArgumentException("Should have have a data wrapper as a parent");
        }
        final DataModel model = data.getSelectedDataModel();
        if (!(model instanceof DataSet)) {
            throw new IllegalArgumentException("The dataset must be a rectangular dataset");
        }
        this.sourceDataSet = (DataSet) model;
    }

    /**
     * @return true
     */
    public boolean mustBeShown() {
        return true;
    }

    //=============================== Private Methods ================================//


    private static List<Node> getSelected(final JList list) {
        final List selected = list.getSelectedValuesList();
        final List<Node> nodes = new LinkedList<>();
        if (selected != null) {
            for (final Object o : selected) {
                nodes.add((Node) o);
            }
        }
        return nodes;
    }


    private DiscretizationEditor createEditor(final Node node) {
        if (node instanceof ContinuousVariable) {
            return new ContinuousDiscretizationEditor(this.sourceDataSet, (ContinuousVariable) node
            );
        } else if (node instanceof DiscreteVariable) {
            return new DiscreteDiscretizationEditor((DiscreteVariable) node);
        }

        throw new IllegalStateException();
    }


    private boolean globalChangeVerification() {
        if (!Preferences.userRoot().getBoolean("ignoreGlobalDiscretizationWarning", false)) {
            final Box box = Box.createVerticalBox();
            final String message = "<html>This action will change the number of categories for all selected variables<br>" +
                    "and override any previous work. Are you sure you want continue?</html>";
            box.add(new JLabel(message));
            box.add(Box.createVerticalStrut(5));
            final JCheckBox checkBox = new JCheckBox("Don't show this again");
            checkBox.setHorizontalTextPosition(AbstractButton.LEFT);
            checkBox.setHorizontalAlignment(AbstractButton.RIGHT);
            checkBox.addActionListener(new ActionListener() {
                public void actionPerformed(final ActionEvent e) {
                    final JCheckBox box = (JCheckBox) e.getSource();
                    Preferences.userRoot().putBoolean("ignoreGlobalDiscretizationWarning", box.isSelected());
                }
            });
            box.add(checkBox);
            box.add(Box.createVerticalStrut(5));
            final int option = JOptionPane.showConfirmDialog(this, box, "Discretization Warning", JOptionPane.YES_NO_OPTION);
            return JOptionPane.YES_OPTION == option;
        }
        return true;
    }

    /**
     * Changes the number of categories on the editors for the given nodes.
     */
    private void changeNumberOfCategories(final int numOfCats, final List<Node> nodes) {
        for (final Node node : nodes) {
            final DiscretizationEditor editor = this.nodeEditors.get(node);
            if (editor != null && editor instanceof ContinuousDiscretizationEditor) {
                ((ContinuousDiscretizationEditor) editor).setNumCategories(numOfCats);
            }
        }
    }


    /**
     * Changes the method of the editor.
     */
    private void changeMethod(final List<Node> nodes, final ContinuousDiscretizationEditor.Method method) {
        for (final Node node : nodes) {
            final DiscretizationEditor editor = this.nodeEditors.get(node);
            if (editor != null && editor instanceof ContinuousDiscretizationEditor) {
                ((ContinuousDiscretizationEditor) editor).setMethod(method);
            }
        }
    }


    /**
     * @return the common mehtod if there is one.
     */
    private ContinuousDiscretizationEditor.Method getCommonMethod(final List<Node> nodes) {
        ContinuousDiscretizationEditor.Method method = null;
        for (final Node node : nodes) {
            final DiscretizationEditor editor = this.nodeEditors.get(node);
            if (editor != null && editor instanceof ContinuousDiscretizationEditor) {
                final ContinuousDiscretizationEditor _editor = (ContinuousDiscretizationEditor) editor;

                if (method != null && method != _editor.getMethod()) {
                    return null;
                }
                method = _editor.getMethod();

            }
        }
        return method;
    }


    /**
     * @return the default category num to use for the given nodes. If they all have the same
     * number then its returned otherwise 3 is returned (or something else?)
     */
    private int getDefaultCategoryNum(final List<Node> nodes) {
        if (nodes.isEmpty()) {
            return 3;
        }
        final DiscretizationEditor editor = this.nodeEditors.get(nodes.get(0));

        if (editor != null && editor instanceof ContinuousDiscretizationEditor) {
            final ContinuousDiscretizationEditor _editor = (ContinuousDiscretizationEditor) editor;

            final int value = _editor.getNumCategories();
            for (int i = 1; i < nodes.size(); i++) {
//                editor = this.nodeEditors.get(nodes.get(i));
                if (value != _editor.getNumCategories()) {
                    return 3;
                }
            }
            return value;
        }

        return -1;
    }

    public Map<Node, DiscretizationSpec> getSpecs() {
        return (Map<Node, DiscretizationSpec>) this.parameters.get("discretizationSpecs");
    }

    //============================= Inner class ===============================//


    /**
     * Editor that edits a collection of variables.
     */
    private class VariableSelectionEditor extends JPanel {

        private final List<Node> nodes;

        public VariableSelectionEditor(final List<Node> vars) {
            setLayout(new BorderLayout());
            this.nodes = vars;
            final IntSpinner spinner = new IntSpinner(getDefaultCategoryNum(vars), 1, 3);
            final ContinuousDiscretizationEditor.Method method = getCommonMethod(vars);
            spinner.setMin(2);
            spinner.setFilter(new IntSpinner.Filter() {
                public int filter(final int oldValue, final int newValue) {
                    if (true) {//globalChangeVerification()) {
                        changeNumberOfCategories(newValue, VariableSelectionEditor.this.nodes);
                        return newValue;
                    }
                    return oldValue;
                }
            });

            final Box vBox = Box.createVerticalBox();

            vBox.add(new JLabel("Discretization Method: "));

            final JRadioButton none = new JRadioButton("Don't Discretize",
                    method == ContinuousDiscretizationEditor.Method.NONE);
            final JRadioButton equalBuckets = new JRadioButton("Evenly Distributed Values",
                    method == ContinuousDiscretizationEditor.Method.EQUAL_SIZE_BUCKETS);
            final JRadioButton equalInterval = new JRadioButton("Evenly Distributed Intervals",
                    method == ContinuousDiscretizationEditor.Method.EVENLY_DIVIDED_INTERNVALS);
            none.setHorizontalTextPosition(AbstractButton.RIGHT);
            equalBuckets.setHorizontalTextPosition(AbstractButton.RIGHT);
            equalInterval.setHorizontalTextPosition(AbstractButton.RIGHT);

            none.addActionListener(new ActionListener() {
                public void actionPerformed(final ActionEvent e) {
                    changeMethod(VariableSelectionEditor.this.nodes, ContinuousDiscretizationEditor.Method.NONE);
                }
            });

            equalBuckets.addActionListener(new ActionListener() {
                public void actionPerformed(final ActionEvent e) {
                    changeMethod(VariableSelectionEditor.this.nodes, ContinuousDiscretizationEditor.Method.EQUAL_SIZE_BUCKETS);
                }
            });

            equalInterval.addActionListener(new ActionListener() {
                public void actionPerformed(final ActionEvent e) {
                    changeMethod(VariableSelectionEditor.this.nodes, ContinuousDiscretizationEditor.Method.EVENLY_DIVIDED_INTERNVALS);
                }
            });

            final ButtonGroup group = new ButtonGroup();
            group.add(none);
            group.add(equalBuckets);
            group.add(equalInterval);

            vBox.add(none);
            vBox.add(equalBuckets);
            vBox.add(equalInterval);

            none.setSelected(true);

            final Box buttons = Box.createHorizontalBox();
            buttons.add(vBox);
            buttons.add(Box.createHorizontalGlue());
            buttons.setBorder(new EmptyBorder(15, 5, 5, 5));

            final Box cats = Box.createHorizontalBox();
            cats.add(new JLabel(" Change number of categories: "));
            cats.add(spinner);
            cats.add(Box.createHorizontalGlue());
            cats.setBorder(new EmptyBorder(5, 5, 5, 5));

            final Box vBox1 = Box.createVerticalBox();
            vBox1.add(buttons);
            vBox1.add(cats);
            vBox1.add(Box.createVerticalGlue());

            this.add(vBox1, BorderLayout.NORTH);
        }


    }


    private static class VariableListModel extends AbstractListModel {

        private final Vector<Node> variables;


        public VariableListModel(final List<Node> variables) {
            this.variables = new Vector<>(variables);
        }


        public int getSize() {
            return this.variables.size();
        }

        public Object getElementAt(final int index) {
            return this.variables.get(index);
        }


    }


    private static class VariableBoxRenderer extends DefaultListCellRenderer {

        public Component getListCellRendererComponent(final JList list, final Object value, final int index, final boolean isSelected, final boolean cellHasFocus) {
            final Node node = (Node) value;
            if (node == null) {
                this.setText("");
            } else {
                this.setText(node.getName());
            }
            if (isSelected) {
                setBackground(list.getSelectionBackground());
                setForeground(list.getSelectionForeground());
            } else {
                setBackground(list.getBackground());
                setForeground(list.getForeground());
            }

            return this;
        }
    }

}






