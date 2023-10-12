package edu.cmu.tetrad.data.simulation;

import edu.cmu.tetrad.algcomparison.simulation.Simulation;
import edu.cmu.tetrad.data.DataModel;
import edu.cmu.tetrad.data.DataSet;
import edu.cmu.tetrad.data.DataType;
import edu.cmu.tetrad.data.SimpleDataLoader;
import edu.cmu.tetrad.graph.Graph;
import edu.cmu.tetrad.graph.GraphSaveLoadUtils;
import edu.cmu.tetrad.graph.LayoutUtil;
import edu.cmu.tetrad.util.Parameters;
import edu.cmu.tetrad.util.Params;
import edu.pitt.dbmi.data.reader.Delimiter;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * @author josephramsey
 */
public class LoadContinuousDataAndGraphs implements Simulation {
    private static final long serialVersionUID = 23L;
    private final String path;
    private final List<Graph> graphs = new ArrayList<>();
    private final List<String> usedParameters = new ArrayList<>();
    private List<DataSet> dataSets = new ArrayList<>();

    public LoadContinuousDataAndGraphs(String path) {
        this.path = path;
    }

    @Override
    public void createData(Parameters parameters, boolean newModel) {
//        if (!newModel && !dataSets.isEmpty()) return;

        this.dataSets = new ArrayList<>();

        if (new File(this.path + "/data").exists()) {
            int numDataSets = Objects.requireNonNull(new File(this.path + "/data").listFiles()).length;

            try {
                for (int i = 0; i < numDataSets; i++) {
                    File file2 = new File(this.path + "/graph/graph." + (i + 1) + ".txt");
                    System.out.println("Loading graph from " + file2.getAbsolutePath());
                    this.graphs.add(GraphSaveLoadUtils.loadGraphTxt(file2));

                    LayoutUtil.defaultLayout(this.graphs.get(i));

                    File file1 = new File(this.path + "/data/data." + (i + 1) + ".txt");

                    System.out.println("Loading data from " + file1.getAbsolutePath());
                    DataSet data = SimpleDataLoader.loadContinuousData(file1, "//", '\"',
                            "*", true, Delimiter.TAB, false);
                    this.dataSets.add(data);
                }

                File paramFile = new File(this.path, "parameters.txt");
                System.out.println("Loading parameters from " + paramFile.getAbsolutePath());
                BufferedReader r = new BufferedReader(new FileReader(paramFile));

                String line;

                while ((line = r.readLine()) != null) {
                    if (line.contains(" = ")) {
                        String[] tokens = line.split(" = ");
                        String key = tokens[0];
                        String value = tokens[1];

                        this.usedParameters.add(key);
                        try {
                            double _value = Double.parseDouble(value);
                            parameters.set(key, _value);
                        } catch (NumberFormatException e) {
                            if (value.equalsIgnoreCase("true") || value.equalsIgnoreCase("false")) {
                                parameters.set(key, Boolean.valueOf(value));
                            } else {
                                parameters.set(key, value);
                            }
                        }
                        System.out.println(key + " : " + value);
                    }
                }

                parameters.set(Params.NUM_RUNS, numDataSets);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public Graph getTrueGraph(int index) {
        return this.graphs.get(index);
    }

    @Override
    public DataModel getDataModel(int index) {
        return this.dataSets.get(index);
    }

    @Override
    public String getDescription() {
        try {
            File file = new File(this.path, "parameters.txt");
            BufferedReader r = new BufferedReader(new FileReader(file));

            StringBuilder b = new StringBuilder();
            b.append("Load data sets and graphs from a directory.").append("\n\n");
            String line;

            while ((line = r.readLine()) != null) {
                if (line.trim().isEmpty()) continue;
                b.append(line).append("\n");
            }

            return b.toString();
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
}
