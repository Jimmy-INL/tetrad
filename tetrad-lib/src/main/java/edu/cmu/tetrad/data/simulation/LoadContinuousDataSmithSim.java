package edu.cmu.tetrad.data.simulation;

import edu.cmu.tetrad.algcomparison.simulation.Simulation;
import edu.cmu.tetrad.algcomparison.utils.HasParameterValues;
import edu.cmu.tetrad.annotation.Experimental;
import edu.cmu.tetrad.data.DataModel;
import edu.cmu.tetrad.data.DataSet;
import edu.cmu.tetrad.data.DataType;
import edu.cmu.tetrad.data.SimpleDataLoader;
import edu.cmu.tetrad.graph.EdgeListGraph;
import edu.cmu.tetrad.graph.Graph;
import edu.cmu.tetrad.graph.LayoutUtil;
import edu.cmu.tetrad.graph.Node;
import edu.cmu.tetrad.util.Parameters;
import edu.cmu.tetrad.util.Params;
import edu.pitt.dbmi.data.reader.Delimiter;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * @author josephramsey
 */
@Experimental
public class LoadContinuousDataSmithSim implements Simulation, HasParameterValues {
    private static final long serialVersionUID = 23L;
    private final String path;
    private final List<String> usedParameters = new ArrayList<>();
    private final Parameters parametersValues = new Parameters();
    private Graph graph;
    private List<DataSet> dataSets = new ArrayList<>();

    public LoadContinuousDataSmithSim(String path) {
        this.path = path;
        String structure = new File(path).getName();
        this.parametersValues.set("structure", structure);
    }

    @Override
    public void createData(Parameters parameters, boolean newModel) {
        if (!this.dataSets.isEmpty()) return;

        this.dataSets = new ArrayList<>();

        File dir = new File(this.path + "/data");

        if (dir.exists()) {
            File[] files = dir.listFiles();

            assert files != null;
            for (File file : files) {
                if (!file.getName().endsWith(".txt")) continue;
                System.out.println("Loading data from " + file.getAbsolutePath());
                try {
                    DataSet dataSet = SimpleDataLoader.loadContinuousData(file, "//", '\"',
                            "*", true, Delimiter.TAB, false);
                    this.dataSets.add(dataSet);
                } catch (Exception e) {
                    System.out.println("Couldn't parse " + file.getAbsolutePath());
                }
            }
        }

        File dir2 = new File(this.path + "/graph");

        if (dir2.exists()) {
            File[] files = dir2.listFiles();

            assert files != null;
            for (File file : files) {
                if (!file.getName().endsWith(".txt")) continue;

                System.out.println("Loading graph from " + file.getAbsolutePath());
                this.graph = readGraph(file);

                LayoutUtil.circleLayout(this.graph);

                break;
            }
        }

        if (parameters.get(Params.NUM_RUNS) != null) {
            parameters.set(Params.NUM_RUNS, parameters.get(Params.NUM_RUNS));
        } else {
            parameters.set(Params.NUM_RUNS, this.dataSets.size());
        }

        System.out.println();
    }

    @Override
    public Graph getTrueGraph(int index) {
        return this.graph;
    }

    @Override
    public DataModel getDataModel(int index) {
        return this.dataSets.get(index);
    }

    public String getDescription() {
        try {
            return "Load data sets and graphs from a directory." + "\n\n";
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public List<String> getParameters() {
        return this.usedParameters;
    }

    @Override
    public int getNumDataModels() {
        return this.dataSets.size();
    }

    @Override
    public DataType getDataType() {
        return DataType.Continuous;
    }

    @Override
    public Parameters getParameterValues() {
        return this.parametersValues;
    }


    public Graph readGraph(File file) {
        try {
            DataSet data = SimpleDataLoader.loadContinuousData(file, "//", '\"',
                    "*", true, Delimiter.TAB, false);
            List<Node> variables = data.getVariables();
            Graph graph = new EdgeListGraph(variables);

            for (int i = 0; i < variables.size(); i++) {
                for (int j = 0; j < variables.size(); j++) {
                    if (i == j) continue;

                    if (data.getDouble(i, j) != 0) {
                        graph.addDirectedEdge(variables.get(i), variables.get(j));
                    }
                }
            }

            return graph;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
