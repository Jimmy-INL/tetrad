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

import edu.cmu.tetrad.algcomparison.algorithm.Algorithm;
import edu.cmu.tetrad.algcomparison.algorithm.MultiDataSetAlgorithm;
import edu.cmu.tetrad.algcomparison.algorithm.cluster.ClusterAlgorithm;
import edu.cmu.tetrad.algcomparison.independence.IndependenceWrapper;
import edu.cmu.tetrad.algcomparison.independence.MSeparationTest;
import edu.cmu.tetrad.algcomparison.independence.TakesGraph;
import edu.cmu.tetrad.algcomparison.score.MSeparationScore;
import edu.cmu.tetrad.algcomparison.score.ScoreWrapper;
import edu.cmu.tetrad.algcomparison.utils.HasKnowledge;
import edu.cmu.tetrad.algcomparison.utils.TakesIndependenceWrapper;
import edu.cmu.tetrad.algcomparison.utils.UsesScoreWrapper;
import edu.cmu.tetrad.data.*;
import edu.cmu.tetrad.graph.Graph;
import edu.cmu.tetrad.graph.LayoutUtil;
import edu.cmu.tetrad.graph.Node;
import edu.cmu.tetrad.graph.Triple;
import edu.cmu.tetrad.search.score.Score;
import edu.cmu.tetrad.search.IndependenceTest;
import edu.cmu.tetrad.search.test.ScoreIndTest;
import edu.cmu.tetrad.search.utils.GraphSearchUtils;
import edu.cmu.tetrad.search.utils.MeekRules;
import edu.cmu.tetrad.search.utils.TsUtils;
import edu.cmu.tetrad.session.ParamsResettable;
import edu.cmu.tetrad.util.Parameters;
import edu.cmu.tetrad.util.Params;
import edu.cmu.tetrad.util.RandomUtil;
import edu.cmu.tetrad.util.Unmarshallable;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.*;

/**
 * Stores an algorithms in the format of the algorithm comparison API.
 *
 * @author josephramsey
 */
public class GeneralAlgorithmRunner implements AlgorithmRunner, ParamsResettable,
        Unmarshallable, IndTestProducer,
        KnowledgeBoxInput {

    static final long serialVersionUID = 23L;

    private DataWrapper dataWrapper;
    private String name;
    private Algorithm algorithm;
    private Parameters parameters;
    private Graph sourceGraph;
    private Graph externalGraph;
    private List<Graph> graphList = new ArrayList<>();
    private Knowledge knowledge;
    private final Map<String, Object> userAlgoSelections = new HashMap<>();
    private transient List<IndependenceTest> independenceTests;

    //===========================CONSTRUCTORS===========================//
    public GeneralAlgorithmRunner(GeneralAlgorithmRunner runner, Parameters parameters) {
        this(runner.getDataWrapper(), runner, parameters, null, null);
        this.sourceGraph = runner.sourceGraph;
        this.knowledge = runner.knowledge;
        this.algorithm = runner.algorithm;
        this.parameters = parameters;

        this.userAlgoSelections.putAll(runner.userAlgoSelections);
    }

    public GeneralAlgorithmRunner(DataWrapper dataWrapper, Parameters parameters) {
        this(dataWrapper, null, parameters, null, null);
    }

    /**
     * Constructs a wrapper for the given DataWrapper. The DatWrapper must
     * contain a DataSet that is either a DataSet or a DataSet or a DataList
     * containing either a DataSet or a DataSet as its selected model.
     */
    public GeneralAlgorithmRunner(DataWrapper dataWrapper, Parameters parameters,
                                  KnowledgeBoxModel knowledgeBoxModel) {
        this(dataWrapper, null, parameters, knowledgeBoxModel, null);
    }

    public GeneralAlgorithmRunner(DataWrapper dataWrapper, GraphSource graphSource, Parameters parameters) {
        this(dataWrapper, graphSource, parameters, null, null);
    }

    public GeneralAlgorithmRunner(DataWrapper dataWrapper, GraphSource graphSource,
                                  KnowledgeBoxModel knowledgeBoxModel,
                                  Parameters parameters) {
        this(dataWrapper, graphSource, parameters, knowledgeBoxModel, null);
    }

    /**
     * Constructs a wrapper for the given DataWrapper. The DatWrapper must
     * contain a DataSet that is either a DataSet or a DataSet or a DataList
     * containing either a DataSet or a DataSet as its selected model.
     */
    public GeneralAlgorithmRunner(DataWrapper dataWrapper, Parameters parameters,
                                  KnowledgeBoxModel knowledgeBoxModel, IndependenceFactsModel facts) {
        this(dataWrapper, null, parameters, knowledgeBoxModel, facts);
    }

    public GeneralAlgorithmRunner(DataWrapper dataWrapper, GeneralAlgorithmRunner runner, Parameters parameters) {
        this(dataWrapper, null, parameters, null, null);
        this.algorithm = runner.algorithm;

        this.userAlgoSelections.putAll(runner.userAlgoSelections);
    }

    /**
     * Constructs a wrapper for the given DataWrapper. The DatWrapper must
     * contain a DataSet that is either a DataSet or a DataSet or a DataList
     * containing either a DataSet or a DataSet as its selected model.
     */
    public GeneralAlgorithmRunner(DataWrapper dataWrapper, GeneralAlgorithmRunner runner, Parameters parameters,
                                  KnowledgeBoxModel knowledgeBoxModel) {
        this(dataWrapper, null, parameters, knowledgeBoxModel, null);
        this.algorithm = runner.algorithm;

        this.userAlgoSelections.putAll(runner.userAlgoSelections);
    }

    public GeneralAlgorithmRunner(DataWrapper dataWrapper, GraphSource graphSource, GeneralAlgorithmRunner runner,
                                  Parameters parameters) {
        this(dataWrapper, graphSource, parameters, null, null);
        this.algorithm = runner.algorithm;

        this.userAlgoSelections.putAll(runner.userAlgoSelections);
    }

    /**
     * Constructs a wrapper for the given DataWrapper. The DatWrapper must
     * contain a DataSet that is either a DataSet or a DataSet or a DataList
     * containing either a DataSet or a DataSet as its selected model.
     */
    public GeneralAlgorithmRunner(DataWrapper dataWrapper, GraphSource graphSource, GeneralAlgorithmRunner runner,
                                  Parameters parameters,
                                  KnowledgeBoxModel knowledgeBoxModel) {
        this(dataWrapper, graphSource, parameters, knowledgeBoxModel, null);
        this.algorithm = runner.algorithm;

        this.userAlgoSelections.putAll(runner.userAlgoSelections);
    }

    /**
     * Constucts a wrapper for the given graph.
     */
    public GeneralAlgorithmRunner(GraphSource graphSource, GeneralAlgorithmRunner runner, Parameters parameters) {
        this(null, graphSource, parameters, null, null);
        this.algorithm = runner.algorithm;

        this.userAlgoSelections.putAll(runner.userAlgoSelections);
    }

    public GeneralAlgorithmRunner(GraphSource graphSource, Parameters parameters,
                                  KnowledgeBoxModel knowledgeBoxModel) {
        this(null, graphSource, parameters, knowledgeBoxModel, null);
    }

    public GeneralAlgorithmRunner(IndependenceFactsModel model,
                                  Parameters parameters, KnowledgeBoxModel knowledgeBoxModel) {
        this(null, null, parameters, knowledgeBoxModel, model);
    }

    /**
     * Constucts a wrapper for the given graph.
     */
    public GeneralAlgorithmRunner(GraphSource graphSource, Parameters parameters) {
        this(null, graphSource, parameters, null, null);
    }

    /**
     * Constructs a wrapper for the given DataWrapper. The DatWrapper must
     * contain a DataSet that is either a DataSet or a DataSet or a DataList
     * containing either a DataSet or a DataSet as its selected model.
     */
    public GeneralAlgorithmRunner(DataWrapper dataWrapper, GraphSource graphSource, Parameters parameters,
                                  KnowledgeBoxModel knowledgeBoxModel, IndependenceFactsModel facts) {
        if (parameters == null) {
            throw new NullPointerException();
        }

        this.parameters = parameters;

        if (graphSource instanceof GeneralAlgorithmRunner) {
            this.algorithm = ((GeneralAlgorithmRunner) graphSource).getAlgorithm();
        }

        if (dataWrapper != null) {
            this.dataWrapper = dataWrapper;

            if (dataWrapper.getDataModelList().isEmpty() && dataWrapper instanceof Simulation) {
                ((Simulation) dataWrapper).createSimulation();
            }
        }

        if (graphSource != null) {
            if (dataWrapper == null && graphSource instanceof DataWrapper) {
                this.dataWrapper = (DataWrapper) graphSource;
            } else {
                this.sourceGraph = graphSource.getGraph();
            }
        }

        if (dataWrapper != null) {
            List<String> names = this.dataWrapper.getVariableNames();
            transferVarNamesToParams(names);
        }

        if (knowledgeBoxModel != null) {
            this.knowledge = knowledgeBoxModel.getKnowledge();
        } else {
            this.knowledge = new Knowledge();
        }

        if (facts != null) {
            getParameters().set("independenceFacts", facts.getFacts());
        }
    }

    //============================PUBLIC METHODS==========================//
    @Override
    public void execute() {
        List<Graph> graphList = new ArrayList<>();

        if (this.independenceTests != null) {
            this.independenceTests.clear();
        }

        Algorithm algo = getAlgorithm();

        if (this.knowledge != null && !knowledge.isEmpty()) {
            if (algo instanceof HasKnowledge) {
                ((HasKnowledge) algo).setKnowledge(this.knowledge.copy());
            } else {
                throw new IllegalArgumentException("Knowledge has been supplied, but this algorithm does not use knowledge.");
            }
        }

        if (getDataModelList().size() == 0 && getSourceGraph() != null) {
            if (algo instanceof UsesScoreWrapper) {
                // We inject the graph to the score to satisfy the tests like MSeparationScore - Zhou
                ScoreWrapper scoreWrapper = ((UsesScoreWrapper) algo).getScoreWrapper();
                if (scoreWrapper instanceof MSeparationScore) {
                    ((MSeparationScore) scoreWrapper).setGraph(getSourceGraph());
                }
            }

            if (algo instanceof TakesIndependenceWrapper) {
                IndependenceWrapper wrapper = ((TakesIndependenceWrapper) algo).getIndependenceWrapper();
                if (wrapper instanceof MSeparationTest) {
                    ((MSeparationTest) wrapper).setGraph(getSourceGraph());
                }
            }

            if (algo instanceof TakesGraph) {
                ((TakesGraph) algo).setGraph(this.sourceGraph);
            }

            if (this.algorithm instanceof HasKnowledge) {
                Knowledge knowledge1 = TsUtils.getKnowledge(getSourceGraph());

                if (this.knowledge.isEmpty() && !knowledge1.isEmpty()) {
                    ((HasKnowledge) algo).setKnowledge(knowledge1);
                } else {
                    ((HasKnowledge) this.algorithm).setKnowledge(this.knowledge.copy());
                }
            }

            Graph graph = algo.search(null, this.parameters);

            LayoutUtil.circleLayout(graph, 200, 200, 150);

            graphList.add(graph);
        } else {
            if (getAlgorithm() instanceof MultiDataSetAlgorithm) {
                for (int k = 0; k < this.parameters.getInt("numRuns"); k++) {
                    Knowledge knowledge1 = getDataModelList().get(0).getKnowledge();
                    List<DataModel> dataSets = new ArrayList<>(getDataModelList());
                    for (DataModel dataSet : dataSets) dataSet.setKnowledge(knowledge1);
                    int randomSelectionSize = this.parameters.getInt("randomSelectionSize");
                    if (randomSelectionSize == 0) {
                        randomSelectionSize = dataSets.size();
                    }
                    if (dataSets.size() < randomSelectionSize) {
                        throw new IllegalArgumentException("Sorry, the 'random selection size' is greater than "
                                + "the number of data sets: " + randomSelectionSize + " > " + dataSets.size());
                    }
                    RandomUtil.shuffle(dataSets);

                    List<DataModel> sub = new ArrayList<>();
                    for (int j = 0; j < randomSelectionSize; j++) {
                        sub.add(dataSets.get(j));
                    }

                    if (algo instanceof TakesGraph) {
                        ((TakesGraph) algo).setGraph(this.sourceGraph);
                    }

                    if (this.algorithm instanceof HasKnowledge) {
                        ((HasKnowledge) this.algorithm).setKnowledge(this.knowledge.copy());
                    }

                    graphList.add(((MultiDataSetAlgorithm) algo).search(sub, this.parameters));
                }
            } else if (getAlgorithm() instanceof ClusterAlgorithm) {
                for (int k = 0; k < this.parameters.getInt("numRuns"); k++) {
                    getDataModelList().forEach(dataModel -> {
                        if (dataModel instanceof ICovarianceMatrix) {
                            ICovarianceMatrix dataSet = (ICovarianceMatrix) dataModel;

                            if (algo instanceof TakesGraph) {
                                ((TakesGraph) algo).setGraph(this.sourceGraph);
                            }

                            if (this.algorithm instanceof HasKnowledge) {
                                ((HasKnowledge) this.algorithm).setKnowledge(this.knowledge.copy());
                            }

                            Graph graph = this.algorithm.search(dataSet, this.parameters);

                            LayoutUtil.circleLayout(graph, 200, 200, 150);

                            graphList.add(graph);
                        } else if (dataModel instanceof DataSet) {
                            DataSet dataSet = (DataSet) dataModel;

                            if (!dataSet.isContinuous()) {
                                throw new IllegalArgumentException("Sorry, you need a continuous dataset for a cluster algorithm.");
                            }

                            if (algo instanceof TakesGraph) {
                                ((TakesGraph) algo).setGraph(this.sourceGraph);
                            }

                            if (this.algorithm instanceof HasKnowledge) {
                                ((HasKnowledge) this.algorithm).setKnowledge(this.knowledge.copy());
                            }

                            Graph graph = this.algorithm.search(dataSet, this.parameters);
                            LayoutUtil.circleLayout(graph, 200, 200, 150);

                            graphList.add(graph);
                        }
                    });
                }
            } else {
                if (getDataModelList().size() != 1) {
                    throw new IllegalArgumentException("Expecting a single dataset here.");
                }

                if (algo != null) {
                    getDataModelList().forEach(data -> {
                        Knowledge knowledgeFromData = data.getKnowledge();
                        if (!(knowledgeFromData == null || knowledgeFromData.getVariables().isEmpty())) {
                            this.knowledge = knowledgeFromData;
                        }

                        DataType algDataType = algo.getDataType();

                        if (algo instanceof TakesGraph) {
                            ((TakesGraph) algo).setGraph(this.sourceGraph);
                        }

                        if (this.algorithm instanceof HasKnowledge) {
                            ((HasKnowledge) this.algorithm).setKnowledge(this.knowledge.copy());
                        }

                        if (data instanceof ICovarianceMatrix && parameters.getInt(Params.NUMBER_RESAMPLING) > 0) {
                            throw new IllegalArgumentException("Sorry, you need a tabular dataset in order to do bootstrapping.");
                        }

                        if (data.isContinuous() && (algDataType == DataType.Continuous || algDataType == DataType.Mixed)) {
                            Graph graph = algo.search(data, this.parameters);
                            LayoutUtil.circleLayout(graph, 200, 200, 150);
                            graphList.add(graph);
                        } else if (data.isDiscrete() && (algDataType == DataType.Discrete || algDataType == DataType.Mixed)) {
                            Graph graph = algo.search(data, this.parameters);
                            LayoutUtil.circleLayout(graph, 200, 200, 150);
                            graphList.add(graph);
                        } else if (data.isMixed() && algDataType == DataType.Mixed) {
                            Graph graph = algo.search(data, this.parameters);
                            LayoutUtil.circleLayout(graph, 200, 200, 150);
                            graphList.add(graph);
                        } else {
                            throw new IllegalArgumentException("The algorithm was not expecting that type of data.");
                        }
                    });
                }
            }
        }

        if (knowledge != null && knowledge.getNumTiers() > 0) {
            for (Graph graph : graphList) {
                GraphSearchUtils.arrangeByKnowledgeTiers(graph, knowledge);
            }
        } else {
            for (Graph graph : graphList) {
                LayoutUtil.circleLayout(graph, 225, 200, 150);
            }
        }

        this.graphList = graphList;
    }

    public boolean hasMissingValues() {
        DataModelList dataModelList = getDataModelList();
        if (dataModelList.containsEmptyData()) {
            return false;
        } else {
            if (dataModelList.get(0) instanceof CovarianceMatrix) {
                return false;
            }

            DataSet dataSet = (DataSet) dataModelList.get(0);

            return dataSet.existsMissingValue();
        }
    }

    /**
     * By default, algorithm do not support knowledge. Those that do will speak
     * up.
     */
    @Override
    public boolean supportsKnowledge() {
        return false;
    }

    @Override
    public MeekRules getMeekRules() {
        return null;
    }

    @Override
    public void setExternalGraph(Graph graph) {
        this.externalGraph = graph;
    }

    @Override
    public Graph getExternalGraph() {
        return this.externalGraph;
    }

    @Override
    public String getAlgorithmName() {
        return null;
    }

    @Override
    public final Graph getSourceGraph() {
        return this.sourceGraph;
    }

    @Override
    public Graph getResultGraph() {
        return getGraph();
    }

    @Override
    public final DataModel getDataModel() {
        if (this.dataWrapper != null) {
            DataModelList dataModelList = this.dataWrapper.getDataModelList();

            if (dataModelList.size() == 1) {
                return dataModelList.get(0);
            } else {
                return dataModelList;
            }
        } else {

            // Do not throw an exception here!
            return new BoxDataSet(new VerticalDoubleDataBox(0, 0), new ArrayList<>());
        }
    }

    @Override
    public Parameters getParams() {
        return null;
    }

    public final DataModelList getDataModelList() {
        if (this.dataWrapper == null) {
            return new DataModelList();
        }
        return this.dataWrapper.getDataModelList();
    }

    public final Parameters getParameters() {
        return this.parameters;
    }

    @Override
    public Object getResettableParams() {
        return this.getParameters();
    }

    @Override
    public void resetParams(Object params) {
        this.parameters = (Parameters) params;
    }

    //===========================PRIVATE METHODS==========================//
    private void transferVarNamesToParams(List<String> names) {
        getParameters().set("varNames", names);
    }

    /**
     * Adds semantic checks to the default deserialization method. This method
     * must have the standard signature for a readObject method, and the body of
     * the method must begin with "s.defaultReadObject();". Other than that, any
     * semantic checks can be specified and do not need to stay the same from
     * version to version. A readObject method of this form may be added to any
     * class, even if Tetrad sessions were previously saved out using a version
     * of the class that didn't include it. (That's what the
     * "s.defaultReadObject();" is for. See J. Bloch, Effective Java, for help.
     */
    private void readObject(ObjectInputStream s)
            throws IOException, ClassNotFoundException {
        s.defaultReadObject();
    }

    @Override
    public IndependenceTest getIndependenceTest() {
        if (this.independenceTests == null) {
            this.independenceTests = new ArrayList<>();
        }

        if (this.independenceTests.size() == 1) {
            return this.independenceTests.get(0);
        }

        Algorithm algo = getAlgorithm();

        if (getDataModelList().size() == 0 && getSourceGraph() != null) {
            // We inject the graph to the test to satisfy the tests like MSeparationTest - Zhou
            IndependenceWrapper test = new MSeparationTest(getSourceGraph());

            if (this.independenceTests == null) {
                this.independenceTests = new ArrayList<>();
            }

            // Grabbing this independence test for the independence tests interface. JR 2020.8.24
//            IndependenceTest test = indTestWrapper.getTest(null, parameters);
            this.independenceTests.add(test.getTest(null, this.parameters));
        } else if (algo instanceof TakesIndependenceWrapper) {
            if (getDataModelList().size() == 1) {
                IndependenceWrapper indTestWrapper = ((TakesIndependenceWrapper) getAlgorithm()).getIndependenceWrapper();

                if (this.independenceTests == null) {
                    this.independenceTests = new ArrayList<>();
                }

                // Grabbing this independence test for the independence tests interface. JR 2020.8.24
                IndependenceTest test = indTestWrapper.getTest(getDataModelList().get(0), this.parameters);
                this.independenceTests.add(test);
            }
        } else if (algo instanceof UsesScoreWrapper) {
            if (getDataModelList().size() == 1) {
                ScoreWrapper wrapper = ((UsesScoreWrapper) getAlgorithm()).getScoreWrapper();

                if (this.independenceTests == null) {
                    this.independenceTests = new ArrayList<>();
                }

                // Grabbing this independence score for the independence tests interface. JR 2020.8.24
                Score score = wrapper.getScore(getDataModelList().get(0), this.parameters);
                this.independenceTests.add(new ScoreIndTest(score));
            }
        }

        if (this.independenceTests.isEmpty()) {
            throw new IllegalArgumentException("One or more of the parents was a search that didn't use "
                    + "a test or a score.");
        }

        return this.independenceTests.get(0);
    }

    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public void setName(String name) {
        this.name = name;
    }

    public Algorithm getAlgorithm() {
        return this.algorithm;
    }

    public void setAlgorithm(Algorithm algorithm) {
        if (algorithm == null) {
            throw new NullPointerException("Algorithm not specified");
        }
        this.algorithm = algorithm;
    }

    @Override
    public List<String> getTriplesClassificationTypes() {
        return Collections.EMPTY_LIST;
    }

    @Override
    public List<List<Triple>> getTriplesLists(Node node) {
        return Collections.EMPTY_LIST;
    }

    @Override
    public Map<String, String> getParamSettings() {
        return Collections.EMPTY_MAP;
    }

    @Override
    public void setAllParamSettings(Map<String, String> paramSettings) {

    }

    @Override
    public Map<String, String> getAllParamSettings() {
        return Collections.EMPTY_MAP;
    }

    @Override
    public Graph getGraph() {
        if (this.graphList == null || this.graphList.isEmpty()) {
            return null;
        } else {
            return this.graphList.get(0);
        }
    }

    @Override
    public List<Graph> getGraphs() {
        return this.graphList;
    }

    public Knowledge getKnowledge() {
        return this.knowledge;
    }

    public DataWrapper getDataWrapper() {
        return this.dataWrapper;
    }

    @Override
    public List<Node> getVariables() {
        return Collections.EMPTY_LIST;
    }

    @Override
    public List<String> getVariableNames() {
        return Collections.EMPTY_LIST;
    }

    public List<Graph> getCompareGraphs(List<Graph> graphs) {
        if (graphs == null) {
            throw new NullPointerException();
        }

        List<Graph> compareGraphs = new ArrayList<>();

        for (Graph graph : graphs) {
            compareGraphs.add(this.algorithm.getComparisonGraph(graph));
        }

        return compareGraphs;
    }

    public Map<String, Object> getUserAlgoSelections() {
        return this.userAlgoSelections;
    }

}
