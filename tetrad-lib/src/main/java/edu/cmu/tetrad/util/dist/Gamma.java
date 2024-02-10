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

package edu.cmu.tetrad.util.dist;

import edu.cmu.tetrad.util.RandomUtil;

/**
 * Wraps a chi square distribution for purposes of drawing random samples. Methods are provided to allow parameters to
 * be manipulated in an interface.
 *
 * @author josephramsey
 * @version $Id: $Id
 */
public class Gamma implements Distribution {
    private static final long serialVersionUID = 23L;

    private double alpha;
    private double lambda;

    private Gamma() {
        this.alpha = .5;
        this.lambda = .7;
    }

    /**
     * Generates a simple exemplar of this class to test serialization.
     *
     * @return The exemplar.
     */
    public static Gamma serializableInstance() {
        return new Gamma();
    }

    /**
     * <p>getNumParameters.</p>
     *
     * @return a int
     */
    public int getNumParameters() {
        return 2;
    }

    /**
     * <p>getName.</p>
     *
     * @return a {@link java.lang.String} object
     */
    public String getName() {
        return "Gamma";
    }

    /**
     * {@inheritDoc}
     */
    public void setParameter(int index, double value) {
        if (index == 0) {
            this.alpha = value;
        } else if (index == 1) {
            this.lambda = value;
        }

        throw new IllegalArgumentException();
    }

    /**
     * {@inheritDoc}
     */
    public double getParameter(int index) {
        if (index == 0) {
            return this.alpha;
        } else if (index == 1) {
            return this.lambda;
        }

        throw new IllegalArgumentException();
    }

    /**
     * {@inheritDoc}
     */
    public String getParameterName(int index) {
        if (index == 0) {
            return "Alpha";
        } else if (index == 1) {
            return "Lambda";
        }

        throw new IllegalArgumentException();
    }

    /**
     * <p>nextRandom.</p>
     *
     * @return a double
     */
    public double nextRandom() {
        return RandomUtil.getInstance().nextGamma(this.alpha, this.lambda);
    }

    /**
     * <p>toString.</p>
     *
     * @return a {@link java.lang.String} object
     */
    public String toString() {
        return "Gamma(" + this.alpha + ", " + this.lambda + ")";
    }
}



