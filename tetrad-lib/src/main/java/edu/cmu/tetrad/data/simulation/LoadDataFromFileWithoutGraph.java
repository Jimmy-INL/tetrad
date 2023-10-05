package edu.cmu.tetrad.data.simulation;

import edu.cmu.tetrad.algcomparison.simulation.Simulation;
import edu.cmu.tetrad.algcomparison.statistic.utils.SimulationPath;
import edu.cmu.tetrad.algcomparison.utils.ParameterValues;
import edu.cmu.tetrad.data.DataModel;
import edu.cmu.tetrad.data.DataSet;
import edu.cmu.tetrad.data.DataType;
import edu.cmu.tetrad.data.SimpleDataLoader;
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
 * @author josephramsey
 */
public class LoadDataFromFileWithoutGraph implements Simulation, SimulationPath, ParameterValues {
    private static final long serialVersionUID = 23L;
    private final String path;
    private final Map<String, Object> parameterValues = new HashMap<>();
    private DataSet dataSet;

    public LoadDataFromFileWithoutGraph(String path) {
        this.dataSet = null;
        this.path = path;
    }

    @Override
    public void createData(Parameters parameters, boolean newModel) {
        try {
            File file = new File(this.path);
            System.out.println("Loading data from " + file.getAbsolutePath());
            this.dataSet = SimpleDataLoader.loadContinuousData(file, "//", '\"',
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
        return this.dataSet;
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
        return 1;
    }

    @Override
    public DataType getDataType() {
        return DataType.Continuous;
    }

    @Override
    public String getPath() {
        return this.path;
    }

    @Override
    public Map<String, Object> parameterValues() {
        return this.parameterValues;
    }
}
