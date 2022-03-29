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

import edu.cmu.tetrad.data.ContinuousVariable;
import edu.cmu.tetrad.data.DataSet;
import edu.cmu.tetrad.data.DiscreteVariable;
import edu.cmu.tetrad.data.Variable;
import edu.cmu.tetrad.graph.Node;
import edu.cmu.tetrad.graph.NodeVariableType;

import javax.swing.table.AbstractTableModel;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.ArrayList;

/**
 * Wraps a dataSet which is possibly smaller than the display window in a larger
 * AbstractTableModel which will fill the window.
 *
 * @author Joseph Ramsey
 */
class TabularDataTable extends AbstractTableModel {

    private static final long serialVersionUID = 8832459230421410126L;

    /**
     * The DataSet being displayed.
     */
    private DataSet dataSet;

    /**
     * True iff category names for discrete variables should be shown.
     */
    private boolean categoryNamesShown = true;

    /**
     * Fires property change events.
     */
    private final PropertyChangeSupport pcs = new PropertyChangeSupport(this);

    /**
     * Table header notations
     */
    private final String columnHeaderNotationDefault = "C";
    private final String columnHeaderNotationContinuous = "-C";
    private final String columnHeaderNotationDiscrete = "-D";
    private final String columnHeaderNotationInterventionStatus = "-I_S";
    private final String columnHeaderNotationInterventionValue = "-I_V";

    /**
     * Constructs a new DisplayTableModel to wrap the given dataSet.
     *
     * @param dataSet the dataSet.
     */
    public TabularDataTable(final DataSet dataSet) {
        this.dataSet = dataSet;
    }

    /**
     * Note that returning null here has two effects. First, it
     */
    public String getColumnName(final int col) {
        return null;
    }

    /**
     * @return the number of rows in the wrapper table model. Guarantees that
     * this number will be at least 100.
     */
    public int getRowCount() {
        final int maxRowCount = this.dataSet.getNumRows() + 3;
        return (maxRowCount < 100) ? 100 : maxRowCount;
    }

    /**
     * @return the number of columns in the wrapper table model. Guarantees that
     * this number will be at least 30.
     */
    public int getColumnCount() {
        return (this.dataSet.getNumColumns() < 30) ? 30
                : this.dataSet.getNumColumns() + getNumLeadingCols() + 1;
    }

    /**
     * @return the value at the given (row, col) coordinates of the table as an
     * Object. If the variable for the col is a DiscreteVariable, the String
     * value (as opposed to the integer index value) is extracted and returned.
     * If the coordinates are out of range of the wrapped table model, 'null' is
     * returned. Otherwise, the value stored in the wrapped table model at the
     * given coordinates is returned.
     */
    @Override
    public Object getValueAt(final int row, final int col) {
        final int columnIndex = col - getNumLeadingCols();
        final int rowIndex = row - 2;

//        if (col == 1) {
//            if (row == 1) {
//                return "MULT";
//            }
//            else if (rowIndex >= 0 && rowIndex < dataSet.getNumRows()) {
//                return dataSet.getMultiplier(rowIndex);
//            }
//        }
//        else
        if (col >= getNumLeadingCols()
                && col < this.dataSet.getNumColumns() + getNumLeadingCols()) {
            final Node variable = this.dataSet.getVariable(columnIndex);

            if (row == 0) {
                // Append "-D" notation to discrete variables, "-C" for continuous
                // and append additional "-I" for those added interventional variables - Zhou
                String columnHeader = this.columnHeaderNotationDefault + Integer.toString(columnIndex + 1);

                if (variable instanceof DiscreteVariable) {
                    columnHeader += this.columnHeaderNotationDiscrete;
                } else if (variable instanceof ContinuousVariable) {
                    columnHeader += this.columnHeaderNotationContinuous;
                }

                // Add header notations for interventional status and value
                if (variable.getNodeVariableType() == NodeVariableType.INTERVENTION_STATUS) {
                    columnHeader += this.columnHeaderNotationInterventionStatus;
                } else if (variable.getNodeVariableType() == NodeVariableType.INTERVENTION_VALUE) {
                    columnHeader += this.columnHeaderNotationInterventionValue;
                }

                return columnHeader;
            } else if (row == 1) {
                return this.dataSet.getVariable(columnIndex).getName();
            } else if (rowIndex >= this.dataSet.getNumRows()) {
                return null;
            } else {
                if (variable instanceof DiscreteVariable) {
                    ((DiscreteVariable) variable).setCategoryNamesDisplayed(
                            isCategoryNamesShown());
                }

                final Object value = this.dataSet.getObject(rowIndex, columnIndex);

                if (((Variable) variable).isMissingValue(value)) {
                    return "*";
                } else {
                    return value;
                }
            }
        } else if (col >= this.dataSet.getNumColumns() + getNumLeadingCols()) {
            if (row == 0) {
                return "C" + Integer.toString(columnIndex + 1);
            }
        }

        return null;
    }

    public boolean isCellEditable(final int row, final int col) {
        return row > 0 && col >= 1;
    }

    /**
     * Sets the value at the given (row, col) coordinates of the table as an
     * Object. If the variable for the col is a DiscreteVariable, the String
     * value (as opposed to the integer index value) is extracted and returned.
     * If the coordinates are out of range of the wrapped table model, 'null' is
     * returned. Otherwise, the value stored in the wrapped table model at the
     * given coordinates is returned.
     */
    public void setValueAt(final Object value, final int row, final int col) {
        this.dataSet.ensureColumns(col - getNumLeadingCols() + 1, new ArrayList<>());
        this.dataSet.ensureRows(row - getNumLeadingRows() + 1);

        if (col == 0) {
            throw new IllegalArgumentException("Bad col index: " + col);
        }

        if (col >= getNumLeadingCols()
                && col < this.dataSet.getNumColumns() + getNumLeadingCols()) {
            if (row == 1) {
                setColumnName(col, value);
            } else if (row > 1) {
                try {
                    pasteIntoColumn(row, col, value);
                } catch (final Exception e) {
                    e.printStackTrace();
                    this.pcs.firePropertyChange("modelChanged", null, null);
                    return;
                }
            }
        }

        fireTableDataChanged();
        this.pcs.firePropertyChange("modelChanged", null, null);
    }

    /**
     * Col index here is JTable index.
     */
    private void addColumnsOutTo(final int col) {
        for (int i = this.dataSet.getNumColumns() + getNumLeadingCols();
             i <= col; i++) {
            final ContinuousVariable var = new ContinuousVariable("");
            this.dataSet.addVariable(var);

            System.out.println("Adding " + var + " col " + this.dataSet.getColumn(var));
        }

        this.pcs.firePropertyChange("modelChanged", null, null);
    }

    private String newColumnName(final String suggestedName) {
        if (!existsColByName(suggestedName)) {
            return suggestedName;
        }

        int i = 0;

        while (true) {
            final String proposedName = suggestedName + "-" + (++i);
            if (!existsColByName(proposedName)) {
                return proposedName;
            }
        }
    }

    private boolean existsColByName(final String proposedName) {
        for (int i = 0; i < this.dataSet.getNumColumns(); i++) {
            final String name = this.dataSet.getVariable(i).getName();
            if (name.equals(proposedName)) {
                return true;
            }
        }
        return false;
    }

    private void setColumnName(final int col, final Object value) {
        final String oldName = this.dataSet.getVariable(col - getNumLeadingCols()).getName();
        final String newName = (String) value;

        if (oldName.equals(newName)) {
            return;
        }

//        try {
//            pcs.firePropertyChange("propesedVariableNameChange", oldName, newName);
//        } catch (IllegalArgumentException e) {
//            JOptionPane.showMessageDialog(JOptionUtils.centeringComp(), e.getMessage());
//            return;
//        }
//
//        pcs.firePropertyChange("variableNameChange", oldName, newName);
        this.dataSet.getVariable(col - getNumLeadingCols()).setName(newName);
        this.pcs.firePropertyChange("modelChanged", null, null);
        this.pcs.firePropertyChange("variableNameChanged", oldName, newName);
    }

    /**
     * The row and column indices are JTable indices.
     */
    private void pasteIntoColumn(final int row, final int col, Object value) {
        final int dataRow = row - getNumLeadingRows();
        final int dataCol = col - getNumLeadingCols();
        Node variable = this.dataSet.getVariable(dataCol);

        if (variable instanceof ContinuousVariable && value instanceof Number) {
            this.dataSet.setObject(dataRow, dataCol, value);
            return;
        }

        if ("".equals(value) || value == null) {
            return;
        }

        final String valueTrimmed = ((String) value).trim();
        boolean quoted = false;

        if (valueTrimmed.startsWith("\"") && valueTrimmed.endsWith("\"")) {
            value = valueTrimmed.substring(1, valueTrimmed.length() - 1);
            quoted = true;
        }

        if (!(variable instanceof DiscreteVariable)
                && isEmpty(this.dataSet, dataCol)
                && (quoted || !isNumber((String) value))) {
            variable = swapDiscreteColumnForContinuous(col);
        }

        if (value instanceof String && ((String) value).trim().equals("*")) {
            value = ((Variable) variable).getMissingValueMarker();
        }

        this.dataSet.setObject(dataRow, dataCol, value);

        this.pcs.firePropertyChange("modelChanged", null, null);
    }

    private boolean isEmpty(final DataSet dataSet, final int column) {
        final Node variable = dataSet.getVariable(column);
        final Object missingValue = ((Variable) variable).getMissingValueMarker();

        for (int i = 0; i < dataSet.getNumRows(); i++) {
            if (!(dataSet.getObject(i, column).equals(missingValue))) {
                return false;
            }
        }

        return true;
    }

    private Node swapDiscreteColumnForContinuous(final int col) {
        final Node variable = this.dataSet.getVariable(col - getNumLeadingCols());
        if (variable == null) {
            throw new NullPointerException();
        }
        if (!isEmpty(this.dataSet, col - getNumLeadingCols())) {
            throw new IllegalArgumentException("Old column not empty.");
        }
        final String name = variable.getName();
        final DiscreteVariable var = new DiscreteVariable(name);
        var.setCategoryNamesDisplayed(true);
        this.dataSet.removeColumn(col - getNumLeadingCols());
        this.dataSet.addVariable(col - getNumLeadingCols(), var);
        this.pcs.firePropertyChange("modelChanged", null, null);
        return var;
    }

    private boolean isNumber(final String value) {
        try {
            Double.parseDouble(value);
            return true;
        } catch (final NumberFormatException e) {
            return false;
        }
    }

    /**
     * @return the DataSet being presented.
     */
    public DataSet getDataSet() {
        return this.dataSet;
    }

    public void setDataSet(final DataSet data) {
        if (data == null) {
            throw new NullPointerException("Data set was null.");
        }
        this.dataSet = data;
    }

    private int getNumLeadingRows() {
        /*
      The number of initial "special" columns not used to display the data
      set.
         */
        final int numLeadingRows = 2;
        return numLeadingRows;
    }

    private int getNumLeadingCols() {
        /*
      The number of initial "special" columns not used to display the data
      set.
         */
        final int numLeadingCols = 1;
        return numLeadingCols;
    }

    public void setCategoryNamesShown(final boolean selected) {
        this.categoryNamesShown = selected;
        fireTableDataChanged();
    }

    public boolean isCategoryNamesShown() {
        return this.categoryNamesShown;
    }

    public void addPropertyChangeListener(final PropertyChangeListener listener) {
        this.pcs.addPropertyChangeListener(listener);
    }
}