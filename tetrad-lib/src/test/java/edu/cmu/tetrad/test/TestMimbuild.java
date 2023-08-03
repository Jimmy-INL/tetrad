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

package edu.cmu.tetrad.test;

import edu.cmu.tetrad.data.Clusters;
import edu.cmu.tetrad.data.CovarianceMatrix;
import edu.cmu.tetrad.data.DataGraphUtils;
import edu.cmu.tetrad.data.DataSet;
import edu.cmu.tetrad.graph.Graph;
import edu.cmu.tetrad.graph.Node;
import edu.cmu.tetrad.graph.NodeType;
import edu.cmu.tetrad.search.Bpc;
import edu.cmu.tetrad.search.Fofc;
import edu.cmu.tetrad.search.Mimbuild;
import edu.cmu.tetrad.search.MimbuildTrek;
import edu.cmu.tetrad.search.utils.BpcTestType;
import edu.cmu.tetrad.search.utils.GraphSearchUtils;
import edu.cmu.tetrad.search.utils.MimUtils;
import edu.cmu.tetrad.sem.ReidentifyVariables;
import edu.cmu.tetrad.sem.SemIm;
import edu.cmu.tetrad.sem.SemPm;
import edu.cmu.tetrad.util.Parameters;
import edu.cmu.tetrad.util.RandomUtil;
import org.junit.Ignore;
import org.junit.Test;

import java.io.IOException;
import java.rmi.MarshalledObject;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import static org.junit.Assert.assertEquals;


/**
 * @author josephramsey
 */
@Ignore
public class TestMimbuild {

    @Test
    public void test1() {
        RandomUtil.getInstance().setSeed(49283494L);

        for (int r = 0; r < 1; r++) {
            Graph mim = DataGraphUtils.randomSingleFactorModel(5, 5, 6, 0, 0, 0);

            Graph mimStructure = structure(mim);

            Parameters params = new Parameters();
            params.set("coefLow", 0.0);
            params.set("coefHigh", 1.0);

            SemPm pm = new SemPm(mim);
            SemIm im = new SemIm(pm, params);
            DataSet data = im.simulateData(300, false);

            final String algorithm = "FOFC";
            Graph searchGraph;
            List<List<Node>> partition;

            if (algorithm.equals("FOFC")) {
                Fofc fofc = new Fofc(data, BpcTestType.TETRAD_WISHART,
                        Fofc.Algorithm.GAP, 0.001);
                searchGraph = fofc.search();
                partition = fofc.getClusters();
            } else if (algorithm.equals("BPC")) {
                final BpcTestType testType = BpcTestType.TETRAD_WISHART;
                final BpcTestType purifyType = BpcTestType.TETRAD_BASED;

                Bpc bpc = new Bpc(
                        data, 0.001,
                        testType
                );
                searchGraph = bpc.search();

                partition = MimUtils.convertToClusters2(searchGraph);
            } else {
                throw new IllegalStateException();
            }

            List<String> latentVarList = reidentifyVariables(mim, data, partition);

            Graph mimbuildStructure;

            for (int mimbuildMethod : new int[]{2}) {
                if (mimbuildMethod == 2) {
                    Mimbuild mimbuild = new Mimbuild();
                    mimbuild.setPenaltyDiscount(1);
                    mimbuild.setMinClusterSize(3);
                    mimbuildStructure = mimbuild.search(partition, latentVarList, new CovarianceMatrix(data));
                    int shd = GraphSearchUtils.structuralHammingDistance(mimStructure, mimbuildStructure);
                    assertEquals(7, shd);
                } else if (mimbuildMethod == 3) {
//                    System.out.println("Mimbuild Trek\n");
                    MimbuildTrek mimbuild = new MimbuildTrek();
                    mimbuild.setAlpha(0.1);
                    mimbuild.setMinClusterSize(3);
                    mimbuildStructure = mimbuild.search(partition, latentVarList, new CovarianceMatrix(data));
                    int shd = GraphSearchUtils.structuralHammingDistance(mimStructure, mimbuildStructure);
                    assertEquals(3, shd);
                } else {
                    throw new IllegalStateException();
                }
            }

        }

    }


    private Graph changeLatentNames(Graph full, Clusters measurements, List<String> latentVarList) {
        Graph g2 = null;

        try {
            g2 = (Graph) new MarshalledObject(full).get();
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }

        for (int i = 0; i < measurements.getNumClusters(); i++) {
            List<String> d = measurements.getCluster(i);
            String latentName = latentVarList.get(i);

            for (Node node : full.getNodes()) {
                if (!(node.getNodeType() == NodeType.LATENT)) {
                    continue;
                }

                List<Node> _children = new ArrayList<>(full.getChildren(node));

                _children.removeAll(ReidentifyVariables.getLatents(full));

                List<String> childNames = getNames(_children);

                if (new HashSet<>(childNames).equals(new HashSet<>(d))) {
                    assert g2 != null;
                    g2.getNode(node.getName()).setName(latentName);
                }
            }
        }

        return g2;
    }

    private List<String> getNames(List<Node> nodes) {
        List<String> names = new ArrayList<>();
        for (Node node : nodes) {
            names.add(node.getName());
        }
        return names;
    }


    private List<String> reidentifyVariables(Graph mim, DataSet data, List<List<Node>> partition) {
        List<String> latentVarList = null;

        if (2 == 1) {
            latentVarList = ReidentifyVariables.reidentifyVariables1(partition, mim);
        } else if (2 == 2) {
            latentVarList = ReidentifyVariables.reidentifyVariables2(partition, mim, data);
        } else {
            throw new IllegalStateException();
        }

        return latentVarList;
    }

    private Graph structure(Graph mim) {
        List<Node> latents = new ArrayList<>();

        for (Node node : mim.getNodes()) {
            if (node.getNodeType() == NodeType.LATENT) {
                latents.add(node);
            }
        }

        return mim.subgraph(latents);
    }
}


