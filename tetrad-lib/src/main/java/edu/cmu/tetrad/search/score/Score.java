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

package edu.cmu.tetrad.search.score;

import edu.cmu.tetrad.graph.Node;

import java.util.List;

/**
 * Interface for a score. Most methods are given defaults so that such a score will be easy to implement in Python usign
 * JPype.
 *
 * @author josephramsey
 */
public interface Score {

    /**
     * The score of a node given its parents.
     *
     * @param node    The node.
     * @param parents The parents.
     * @return The score.
     */
    double localScore(int node, int... parents);

    /**
     * The variables of the score.
     *
     * @return This list.
     */
    List<Node> getVariables();

    /**
     * The sample size of the data.
     *
     * @return This size.
     */
    int getSampleSize();

    /**
     * A string representation of the score.
     *
     * @return This string.
     */
    String toString();


    default double localScoreDiff(int x, int y, int[] z) {
        return localScore(y, append(z, x)) - localScore(y, z);
    }

    default int[] append(int[] parents, int extra) {
        int[] all = new int[parents.length + 1];
        System.arraycopy(parents, 0, all, 0, parents.length);
        all[parents.length] = extra;
        return all;
    }

    default double localScoreDiff(int x, int y) {
        return localScore(y, x) - localScore(y);
    }

    default double localScore(int node, int parent) {
        return localScore(node, new int[]{parent});
    }

    default double localScore(int node) {
        return localScore(node, new int[0]);
    }

    default Node getVariable(String targetName) {
        for (Node node : getVariables()) {
            if (node.getName().equals(targetName)) {
                return node;
            }
        }

        return null;
    }

    default boolean isEffectEdge(double bump) {
        return false;
    }

    default int getMaxDegree() {
        return 1000;
    }

    default boolean determines(List<Node> z, Node y) {
        throw new UnsupportedOperationException("Method determines() is not implemented for this score.");
    }
}

