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

package edu.cmu.tetradapp.model;

import edu.cmu.tetrad.data.*;
import edu.cmu.tetrad.util.Parameters;
import edu.cmu.tetrad.util.TetradSerializableUtils;

import java.io.IOException;
import java.io.ObjectInputStream;

/**
 * Wraps a data model so that a random sample will automatically be drawn on construction from a BayesIm.
 *
 * @author josephramsey
 * @version $Id: $Id
 */
public class BootstrapSamplerWrapper extends DataWrapper {
    private static final long serialVersionUID = 23L;

    /**
     * @serial Cannot be null.
     */
    private final DataSet outputDataSet;

    //=============================CONSTRUCTORS===========================//

    /**
     * <p>Constructor for BootstrapSamplerWrapper.</p>
     *
     * @param wrapper a {@link edu.cmu.tetradapp.model.DataWrapper} object
     * @param params  a {@link edu.cmu.tetrad.util.Parameters} object
     */
    public BootstrapSamplerWrapper(DataWrapper wrapper,
                                   Parameters params) {
        if (wrapper == null) {
            throw new NullPointerException();
        }

        if (params == null) {
            throw new NullPointerException();
        }

        DataModelList bootstraps = new DataModelList();
        DataModelList oldDataSets = wrapper.getDataModelList();

        for (DataModel dataModel : oldDataSets) {
            DataSet dataSet = (DataSet) dataModel;
            BootstrapSampler sampler = new BootstrapSampler();
            DataSet bootstrap = sampler.sample(dataSet, params.getInt("sampleSize", 1000));
            bootstraps.add(bootstrap);
            if (oldDataSets.getSelectedModel() == dataModel) {
                bootstraps.setSelectedModel(bootstrap);
            }
        }

        setDataModel(bootstraps);
        setSourceGraph(wrapper.getSourceGraph());

        DataModel dataModel = wrapper.getSelectedDataModel();
        DataSet dataSet = (DataSet) dataModel;
        BootstrapSampler sampler = new BootstrapSampler();
        this.outputDataSet = sampler.sample(dataSet, params.getInt("sampleSize", 1000));

        LogDataUtils.logDataModelList("Bootstrap sample of data in the parent node.", getDataModelList());

    }

    /**
     * Generates a simple exemplar of this class to test serialization.
     *
     * @return a {@link edu.cmu.tetradapp.model.PcRunner} object
     * @see TetradSerializableUtils
     */
    public static PcRunner serializableInstance() {
        return PcRunner.serializableInstance();
    }

    //=============================PUBLIC METHODS=========================//

    /**
     * <p>getOutputDataset.</p>
     *
     * @return a {@link edu.cmu.tetrad.data.DataSet} object
     */
    public DataSet getOutputDataset() {
        return this.outputDataSet;
    }

    /**
     * Adds semantic checks to the default deserialization method. This method must have the standard signature for a
     * readObject method, and the body of the method must begin with "s.defaultReadObject();". Other than that, any
     * semantic checks can be specified and do not need to stay the same from version to version. A readObject method of
     * this form may be added to any class, even if Tetrad sessions were previously saved out using a version of the
     * class that didn't include it. (That's what the "s.defaultReadObject();" is for. See J. Bloch, Effective Java, for
     * help.
     *
     * @param s
     * @throws IOException            If any.
     * @throws ClassNotFoundException If any.
     */
    private void readObject(ObjectInputStream s)
            throws IOException, ClassNotFoundException {
        s.defaultReadObject();

        if (this.outputDataSet == null) {
            throw new NullPointerException();
        }
    }
}






