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
package edu.cmu.tetrad.data;

import edu.cmu.tetrad.graph.Node;

/**
 * <p>
 * Interface implemented by classes, instantiations of which can serve as data models in Tetrad. Data models may be
 * named if desired; if provided, these names will be used for display purposes.
 * <p>
 * This interface is relatively free of methods, mainly because classes that can serve as data models in Tetrad are
 * diverse, including continuous and discrete data sets, covariance and correlation matrices, graphs, and lists of other
 * data models. So this is primarily a taqging interface.
 *
 * @author josephramsey
 */
public interface DataModel
        extends KnowledgeTransferable, VariableSource {

    long serialVersionUID = 23L;

    /**
     * @return the name of the data model (may be null).
     */
    String getName();

    /**
     * Sets the name of the data model (may be null).
     * @param name the name to set
     */
    void setName(String name);

    /**
     * Renders the data model as as String.
     */
    String toString();

    /**
     * @return true if the data model is continuous, false otherwise.
     */
    boolean isContinuous();

    /**
     * @return true if the data model is discrete, false otherwise.
     */
    boolean isDiscrete();

    /**
     * @return true if the data model is mixed continuous/discrete, false otherwise.
     */
    boolean isMixed();

    /**
     * @return the variable with the given name, or null if no such variable exists.
     */
    Node getVariable(String name);

    /**
     * @return a copy of the data model.
     */
    DataModel copy();
}
