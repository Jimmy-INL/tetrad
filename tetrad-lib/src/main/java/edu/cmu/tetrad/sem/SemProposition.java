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

package edu.cmu.tetrad.sem;

import edu.cmu.tetrad.graph.Node;
import edu.cmu.tetrad.util.TetradSerializable;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.Arrays;
import java.util.List;

/**
 * Represents propositions over the variables of a particular BayesIm describing and event of a fairly general
 * sort--namely, conjunctions of propositions that particular variables take on values from a particular disjunctive
 * list of categories. For example, X1 = 1 or 2 and X2 = 3 and X3 = 1 or 3 and X4 = 2 or 3 or 5. The proposition is
 * created by allowing or disallowing particular categories. Notice that "knowing nothing" about a variable is the same
 * as saying that all categories for that variable are allowed, so the proposition by default allows all categories for
 * all variables--i.e. it is a tautology.
 *
 * @author josephramsey
 * @version $Id: $Id
 */
public final class SemProposition implements TetradSerializable {
    private static final long serialVersionUID = 23L;

    /**
     * @serial Cannot be null.
     */
    private final SemIm semIm;

    /**
     * @serial Cannot be null.
     */
    private final double[] values;

    /**
     * Creates a new Proposition which allows all values.
     *
     * @param semIm a {@link edu.cmu.tetrad.sem.SemIm} object
     */
    public SemProposition(SemIm semIm) {
        if (semIm == null) {
            throw new NullPointerException();
        }

        this.semIm = semIm;
        this.values = new double[semIm.getVariableNodes().size()];

        Arrays.fill(this.values, Double.NaN);
    }

    /**
     * <p>Constructor for SemProposition.</p>
     *
     * @param proposition a {@link edu.cmu.tetrad.sem.SemProposition} object
     */
    public SemProposition(SemProposition proposition) {
        this.semIm = proposition.semIm;
        this.values = Arrays.copyOf(proposition.values, proposition.values.length);
    }

    /**
     * <p>tautology.</p>
     *
     * @param semIm a {@link edu.cmu.tetrad.sem.SemIm} object
     * @return a {@link edu.cmu.tetrad.sem.SemProposition} object
     */
    public static SemProposition tautology(SemIm semIm) {
        return new SemProposition(semIm);
    }

    /**
     * Generates a simple exemplar of this class to test serialization.
     *
     * @return a {@link edu.cmu.tetrad.sem.SemProposition} object
     */
    public static SemProposition serializableInstance() {
        return new SemProposition(SemIm.serializableInstance());
    }

    /**
     * <p>Getter for the field <code>semIm</code>.</p>
     *
     * @return the Bayes IM that this is a proposition for.
     */
    public SemIm getSemIm() {
        return this.semIm;
    }

    /**
     * <p>getNumVariables.</p>
     *
     * @return the number of variables for the proposition.
     */
    public int getNumVariables() {
        return this.values.length;
    }

    /**
     * <p>getNodeIndex.</p>
     *
     * @return the index of the variable with the given name, or -1 if such a variable does not exist.
     */
    public int getNodeIndex() {

        return -1;
    }

    /**
     * {@inheritDoc}
     */
    public boolean equals(Object o) {
        if (o == null) {
            return false;
        }

        if (!(o instanceof SemProposition proposition)) {
            throw new IllegalArgumentException();
        }

        if (!(this.semIm == proposition.semIm)) {
            return false;
        }

        for (int i = 0; i < this.values.length; i++) {
            if (!(Double.isNaN(this.values[i]) && Double.isNaN(proposition.values[i]))) {
                if (this.values[i] != proposition.values[i]) {
                    return false;
                }
            }
        }

        return true;
    }

    /**
     * <p>hashCode.</p>
     *
     * @return a int
     */
    public int hashCode() {
        int hashCode = 37;
        hashCode = 19 * hashCode + this.semIm.hashCode();
        hashCode = 19 * hashCode + Arrays.hashCode(this.values);
        return hashCode;
    }

    /**
     * <p>toString.</p>
     *
     * @return a {@link java.lang.String} object
     */
    public String toString() {
        List<Node> nodes = this.semIm.getVariableNodes();
        StringBuilder buf = new StringBuilder();
        buf.append("\nProposition: ");

        for (int i = 0; i < nodes.size(); i++) {
            Node node = nodes.get(i);
            buf.append("\n").append(node).append(" = ").append(this.values[i]);
        }

        return buf.toString();
    }

    /**
     * Adds semantic checks to the default deserialization method. This method must have the standard signature for a
     * readObject method, and the body of the method must begin with "s.defaultReadObject();". Other than that, any
     * semantic checks can be specified and do not need to stay the same from version to version. A readObject method of
     * this form may be added to any class, even if Tetrad sessions were previously saved out using a version of the
     * class that didn't include it. (That's what the "s.defaultReadObject();" is for. See J. Bloch, Effective Java, for
     * help.
     *
     * @param s The object input stream.
     * @throws IOException            If any.
     * @throws ClassNotFoundException If any.
     */
    private void readObject(ObjectInputStream s)
            throws IOException, ClassNotFoundException {
        s.defaultReadObject();
    }

    /**
     * <p>getValue.</p>
     *
     * @param i a int
     * @return a double
     */
    public double getValue(int i) {
        return this.values[i];
    }

    /**
     * <p>setValue.</p>
     *
     * @param i     a int
     * @param value a double
     */
    public void setValue(int i, double value) {
        this.values[i] = value;
    }

    /**
     * <p>getValue.</p>
     *
     * @param node a {@link edu.cmu.tetrad.graph.Node} object
     * @return a double
     */
    public double getValue(Node node) {
        List<Node> nodes = this.semIm.getVariableNodes();
        return this.values[nodes.indexOf(node)];
    }

    /**
     * <p>setValue.</p>
     *
     * @param node  a {@link edu.cmu.tetrad.graph.Node} object
     * @param value a double
     */
    public void setValue(Node node, double value) {
        List<Node> nodes = this.semIm.getVariableNodes();
        this.values[nodes.indexOf(node)] = value;
    }
}





