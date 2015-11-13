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

package edu.cmu.tetrad.test;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;


public class TestTetradCmd extends TestCase {
    public TestTetradCmd(String name) {
        super(name);
    }

    // These files are in the wrong place...need to fix... TODO.

    public void rtest1() {
        new edu.cmu.tetrad.cmd.TetradCmd(new String[] {"-verbose", "-data", "sample_data/data1.txt",
                     "-datatype", "continuous", "-algorithm", "pc", "-depth", "3",
                     "-significance", "0.001", "-knowledge", "sample_data/knowledge1.txt"});


    }


    public void rtest2() {
        new edu.cmu.tetrad.cmd.TetradCmd(new String[] {"-data", "sample_data/avatarwithdependencies.esv",
                     "-datatype", "discrete", "-algorithm", "cpc", "-depth", "3",
                     "-significance", "0.001", "-knowledge", "sample_data/avatarknowledge.txt"});


    }

    public void rtest3() {
        new edu.cmu.tetrad.cmd.TetradCmd(new String[] {"-data", "eigenvox2.txt",
                     "-datatype", "continuous", "-algorithm", "cpc", "-depth", "3",
                     "-significance", "0.001", "-verbose", "-whitespace"});


    }

    public void rtest4() {
        new edu.cmu.tetrad.cmd.TetradCmd(new String[] {"-covariance", "lead.modified.txt",
                "-algorithm", "cpc", "-depth", "3",
                "-significance", "0.001", "-verbose", "-whitespace"});


    }

    public void test1() {}

    public static Test suite() {
        return new TestSuite(TestTetradCmd.class);
    }
}


