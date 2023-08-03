package edu.pitt.dbmi.algo.resampling;

import edu.cmu.tetrad.algcomparison.algorithm.Algorithm;
import edu.cmu.tetrad.algcomparison.algorithm.MultiDataSetAlgorithm;
import edu.cmu.tetrad.algcomparison.score.ScoreWrapper;
import edu.cmu.tetrad.data.DataModel;
import edu.cmu.tetrad.data.DataSet;
import edu.cmu.tetrad.data.DataUtils;
import edu.cmu.tetrad.data.Knowledge;
import edu.cmu.tetrad.graph.Graph;
import edu.cmu.tetrad.util.Parameters;
import edu.cmu.tetrad.util.Params;
import edu.pitt.dbmi.algo.resampling.task.GeneralResamplingSearchRunnable;
import org.apache.commons.math3.random.RandomGenerator;
import org.apache.commons.math3.random.SynchronizedRandomGenerator;
import org.apache.commons.math3.random.Well44497b;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.Future;

/**
 * Sep 7, 2018 1:38:50 PM
 *
 * @author Chirayu Kong Wongchokprasitti, PhD (chw20@pitt.edu)
 */
public class GeneralResamplingSearch {

    private final int numberResampling;
    private final List<Graph> graphs = Collections.synchronizedList(new ArrayList<>());
    private final ForkJoinPool pool;
    private Algorithm algorithm;
    private MultiDataSetAlgorithm multiDataSetAlgorithm;
    private double percentResampleSize = 100.;
    private boolean resamplingWithReplacement = true;
    private boolean runParallel;
    private boolean addOriginalDataset;
    private boolean verbose;
    private DataSet data;

    private List<DataSet> dataSets;

    /**
     * Specification of forbidden and required edges.
     */
    private Knowledge knowledge = new Knowledge();

    private PrintStream out = System.out;

    private Parameters parameters;

    /**
     * An initial graph to start from.
     */
    private Graph externalGraph;
    private int numNograph = 0;
    private ScoreWrapper scoreWrapper;

    public GeneralResamplingSearch(DataSet data, int numberResampling) {
        this.data = data;
        this.pool = ForkJoinPool.commonPool();
        this.numberResampling = numberResampling;
    }

    public GeneralResamplingSearch(List<DataSet> dataSets, int numberResampling) {
        this.dataSets = dataSets;
        this.pool = ForkJoinPool.commonPool();
        this.numberResampling = numberResampling;
    }

    public void setAlgorithm(Algorithm algorithm) {
        this.algorithm = algorithm;
        this.multiDataSetAlgorithm = null;
    }

    public void setMultiDataSetAlgorithm(MultiDataSetAlgorithm multiDataSetAlgorithm) {
        this.multiDataSetAlgorithm = multiDataSetAlgorithm;
        this.algorithm = null;
    }

    public void setPercentResampleSize(double percentResampleSize) {
        this.percentResampleSize = percentResampleSize;
    }

    public void setResamplingWithReplacement(boolean resamplingWithReplacement) {
        this.resamplingWithReplacement = resamplingWithReplacement;
    }

    public void setRunParallel(boolean runParallel) {
        this.runParallel = runParallel;
    }

    public void setAddOriginalDataset(boolean addOriginalDataset) {
        this.addOriginalDataset = addOriginalDataset;
    }

    public void setVerbose(boolean verbose) {
        this.verbose = verbose;
    }

    public void setData(DataSet data) {
        this.data = data;
    }

    /**
     * Sets the background knowledge.
     *
     * @param knowledge the knowledge object, specifying forbidden and required edges.
     */
    public void setKnowledge(Knowledge knowledge) {
        this.knowledge = new Knowledge((Knowledge) knowledge);
    }

    public void setExternalGraph(Graph externalGraph) {
        this.externalGraph = externalGraph;
    }

    /**
     * @return the output stream that output (except for log output) should be sent to.
     */
    public PrintStream getOut() {
        return this.out;
    }

    /**
     * Sets the output stream that output (except for log output) should be sent to. By default System.out.
     */
    public void setOut(PrintStream out) {
        this.out = out;
    }

    public void setParameters(Parameters parameters) {
        this.parameters = parameters;
    }

    public List<Graph> search() {

        this.graphs.clear();
        this.parameters.set("numberResampling", 0); // This needs to be set to zero to not loop indefinitely

        List<Callable<Graph>> tasks = new ArrayList<>();

        // Running in the sequential form
        if (this.verbose) {
            this.out.println("Running Resamplings in Sequential Mode, numberResampling = " + this.numberResampling);
        }

        if (this.data != null) {
            Long seed = (parameters == null || parameters.get(Params.SEED) == null) ? null : (Long) parameters.get(Params.SEED);
            RandomGenerator randomGenerator = (seed == null || seed < 0) ? null : new SynchronizedRandomGenerator(new Well44497b(seed));
            for (int i1 = 0; i1 < this.numberResampling; i1++) {
                DataSet dataSet;

                if (this.resamplingWithReplacement) {
                    dataSet = (randomGenerator == null)
                            ? DataUtils.getBootstrapSample(data, (int) (data.getNumRows() * this.percentResampleSize / 100.0))
                            : DataUtils.getBootstrapSample(data, (int) (data.getNumRows() * this.percentResampleSize / 100.0), randomGenerator);
                } else {
                    dataSet = (randomGenerator == null)
                            ? DataUtils.getResamplingDataset(data, (int) (data.getNumRows() * this.percentResampleSize / 100.0))
                            : DataUtils.getResamplingDataset(data, (int) (data.getNumRows() * this.percentResampleSize / 100.0), randomGenerator);
                }

                dataSet.setKnowledge(data.getKnowledge());

                GeneralResamplingSearchRunnable task = new GeneralResamplingSearchRunnable(dataSet, this.algorithm, this.parameters, this, this.verbose);
                task.setExternalGraph(this.externalGraph);
                task.setKnowledge(this.knowledge);
                tasks.add(task);
                task.setScoreWrapper(scoreWrapper);
            }

            if (addOriginalDataset) {
                GeneralResamplingSearchRunnable task = new GeneralResamplingSearchRunnable(data.copy(),
                        this.algorithm, this.parameters, this,
                        this.verbose);
                task.setExternalGraph(this.externalGraph);
                task.setKnowledge(this.knowledge);
                tasks.add(task);
                task.setScoreWrapper(scoreWrapper);
            }
        } else {
            for (int i1 = 0; i1 < this.numberResampling; i1++) {
                List<DataModel> dataModels = new ArrayList<>();

                for (DataSet data : this.dataSets) {

                    if (this.resamplingWithReplacement) {
                        DataSet bootstrapSample = DataUtils.getBootstrapSample(data, (int) (data.getNumRows() * this.percentResampleSize / 100.0));
                        bootstrapSample.setKnowledge(data.getKnowledge());
                        dataModels.add(bootstrapSample);
                    } else {
                        DataSet resamplingDataset = DataUtils.getResamplingDataset(data, (int) (data.getNumRows() * this.percentResampleSize / 100.0));
                        resamplingDataset.setKnowledge(data.getKnowledge());
                        dataModels.add(resamplingDataset);
                    }
                }

                GeneralResamplingSearchRunnable task = new GeneralResamplingSearchRunnable(dataModels,
                        this.multiDataSetAlgorithm, this.parameters, this,
                        this.verbose);
                task.setExternalGraph(this.externalGraph);
                task.setKnowledge(dataModels.get(0).getKnowledge());
                task.setScoreWrapper(scoreWrapper);

                tasks.add(task);
            }
        }

        int numNoGraph = 0;

        if (this.runParallel) {
            List<Future<Graph>> futures = this.pool.invokeAll(tasks);
            for (Future<Graph> future : futures) {
                Graph graph;
                try {
                    graph = future.get();

                    if (graph == null) {
                        numNograph++;
                    } else {
                        this.graphs.add(graph);
                    }
                } catch (InterruptedException | ExecutionException e) {
                    e.printStackTrace();
                }
            }
        } else {
            for (Callable<Graph> callable : tasks) {
                try {
                    Graph graph = callable.call();

                    if (graph == null) {
                        numNoGraph++;
                    } else {
                        this.graphs.add(graph);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

        this.parameters.set("numberResampling", this.numberResampling);
        this.numNograph = numNoGraph;

        return this.graphs;
    }

    public int getNumNograph() {
        return numNograph;
    }

    public void setScoreWrapper(ScoreWrapper scoreWrapper) {
        this.scoreWrapper = scoreWrapper;
    }
}
