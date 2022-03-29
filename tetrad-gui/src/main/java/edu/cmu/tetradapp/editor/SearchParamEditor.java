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
import edu.cmu.tetrad.graph.Graph;
import edu.cmu.tetrad.graph.Node;
import edu.cmu.tetrad.graph.NodeType;
import edu.cmu.tetrad.util.NumberFormatUtil;
import edu.cmu.tetrad.util.Parameters;
import edu.cmu.tetradapp.model.DagWrapper;
import edu.cmu.tetradapp.model.DataWrapper;
import edu.cmu.tetradapp.model.GraphWrapper;
import edu.cmu.tetradapp.model.SemGraphWrapper;
import edu.cmu.tetradapp.util.DoubleTextField;
import edu.cmu.tetradapp.util.IntTextField;

import javax.swing.*;
import javax.swing.border.MatteBorder;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.prefs.Preferences;

/**
 * Edits parameters for PC, FCI, CCD, and GA.
 *
 * @author Shane Harwood
 * @author Joseph Ramsey
 */
public final class SearchParamEditor extends JPanel implements ParameterEditor {

    /**
     * The parameter object being edited.
     */
    private Parameters params;

    /**
     * A text field for editing the alpha value.
     */
    private DoubleTextField alphaField;

    /**
     * The parent models of this search object; should contain a DataModel.
     */
    private Object[] parentModels;

    /**
     * Opens up an editor to let the user view the given PcRunner.
     */
    public SearchParamEditor() {
    }

    public void setParams(final Parameters params) {
        if (params == null) {
            throw new NullPointerException();
        }

        this.params = params;
    }

    public void setParentModels(final Object[] parentModels) {
        if (parentModels == null) {
            throw new NullPointerException();
        }

        this.parentModels = parentModels;
    }

    public void setup() {
        /*
      The variable names from the object being searched over (usually data).
     */
        List varNames = (List<String>) this.params.get("varNames", null);

        DataModel dataModel1 = null;
        Graph graph = null;

        for (final Object parentModel1 : this.parentModels) {
            if (parentModel1 instanceof DataWrapper) {
                final DataWrapper dataWrapper = (DataWrapper) parentModel1;
                dataModel1 = dataWrapper.getSelectedDataModel();
            }

            if (parentModel1 instanceof GraphWrapper) {
                final GraphWrapper graphWrapper = (GraphWrapper) parentModel1;
                graph = graphWrapper.getGraph();
            }

            if (parentModel1 instanceof DagWrapper) {
                final DagWrapper dagWrapper = (DagWrapper) parentModel1;
                graph = dagWrapper.getDag();
            }

            if (parentModel1 instanceof SemGraphWrapper) {
                final SemGraphWrapper semGraphWrapper = (SemGraphWrapper) parentModel1;
                graph = semGraphWrapper.getGraph();
            }
        }

        if (dataModel1 != null) {
            varNames = new ArrayList(dataModel1.getVariableNames());
        } else if (graph != null) {
            final Iterator it = graph.getNodes().iterator();
            varNames = new ArrayList();

            Node temp;

            while (it.hasNext()) {
                temp = (Node) it.next();

                if (temp.getNodeType() == NodeType.MEASURED) {
                    varNames.add(temp.getName());
                }
            }
        } else {
            throw new NullPointerException(
                    "Null model (no graph or data model " +
                            "passed to the search).");
        }

        this.params.set("varNames", varNames);

        final IntTextField depthField = new IntTextField(this.params.getInt("depth", -1), 4);
        depthField.setFilter(new IntTextField.Filter() {
            public int filter(final int value, final int oldValue) {
                try {
                    SearchParamEditor.this.params.set("depth", value);
                    Preferences.userRoot().putInt("depth", value);
                    return value;
                } catch (final Exception e) {
                    return oldValue;
                }
            }
        });

        final double alpha = this.params.getDouble("alpha", 0.001);

        if (!Double.isNaN(alpha)) {
            this.alphaField =
                    new DoubleTextField(alpha, 4, NumberFormatUtil.getInstance().getNumberFormat());
            this.alphaField.setFilter(new DoubleTextField.Filter() {
                public double filter(final double value, final double oldValue) {
                    try {
                        SearchParamEditor.this.params.set("alpha", 0.001);
                        Preferences.userRoot().putDouble("alpha", value);
                        return value;
                    } catch (final Exception e) {
                        return oldValue;
                    }
                }
            });
        }

        setBorder(new MatteBorder(10, 10, 10, 10, super.getBackground()));
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

        if (!Double.isNaN(alpha)) {
            final Box b0 = Box.createHorizontalBox();
            b0.add(new JLabel("Alpha Value:"));
            b0.add(Box.createGlue());
            b0.add(this.alphaField);
            add(b0);
            add(Box.createVerticalStrut(10));
        }

        final Box b1 = Box.createHorizontalBox();
        b1.add(new JLabel("Search Depth:"));
        b1.add(Box.createGlue());
        b1.add(depthField);
        add(b1);
        add(Box.createVerticalStrut(10));
    }

    public boolean mustBeShown() {
        return false;
    }

    private Parameters getParams() {
        return this.params;
    }
}





