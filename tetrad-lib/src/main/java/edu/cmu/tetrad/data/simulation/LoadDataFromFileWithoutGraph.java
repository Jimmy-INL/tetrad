package edu.cmu.tetrad.data.simulation;

import edu.cmu.tetrad.algcomparison.simulation.Simulation;
import edu.cmu.tetrad.algcomparison.statistic.utils.SimulationPath;
import edu.cmu.tetrad.algcomparison.utils.ParameterValues;
import edu.cmu.tetrad.data.*;
import edu.cmu.tetrad.graph.Graph;
import edu.cmu.tetrad.util.Parameters;
import edu.pitt.dbmi.data.reader.Delimiter;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author jdramsey
 */
public class LoadDataFromFileWithoutGraph implements Simulation, SimulationPath, ParameterValues {
    static final long serialVersionUID = 23L;
    private DataSet dataSet;
    private int numDataSets = 1;
    private String path;
    private Map<String, Object> parameterValues = new HashMap<>();

    public LoadDataFromFileWithoutGraph(String path) {
        this.dataSet = null;
        this.path = path;
    }

    @Override
    public void createData(Parameters parameters, boolean newModel) {
        try {
            File file = new File(path);
            System.out.println("Loading data from " + file.getAbsolutePath());
            this.dataSet = DataUtils.loadContinuousData(file, "//", '\"' ,
                    "*", true, Delimiter.TAB);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public Graph getTrueGraph(int index) {
        return null;
    }

    @Override
    public DataModel getDataModel(int index) {
        return dataSet;
    }

    @Override
    public String getDescription() {
        return "Load single file to run.";
    }

    @Override
    public List<String> getParameters() {
        return new ArrayList<>();
    }

    @Override
    public int getNumDataModels() {
        return numDataSets;
    }

    @Override
    public DataType getDataType() {
        return DataType.Continuous;
    }

    @Override
    public String getPath() {
        return path;
    }

    @Override
    public Map<String, Object> paremeterValues() {
        return parameterValues;
    }
}
