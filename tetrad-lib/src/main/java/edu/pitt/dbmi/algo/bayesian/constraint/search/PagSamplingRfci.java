package edu.pitt.dbmi.algo.bayesian.constraint.search;

import edu.cmu.tetrad.data.DataSet;
import edu.cmu.tetrad.data.Knowledge;
import edu.cmu.tetrad.graph.Graph;
import edu.cmu.tetrad.search.IGraphSearch;
import edu.cmu.tetrad.search.Rfci;
import edu.cmu.tetrad.search.test.IndTestProbabilistic;
import edu.cmu.tetrad.search.utils.GraphSearchUtils;
import edu.cmu.tetrad.util.GraphSampling;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.*;

/**
 * Jan 29, 2023 4:10:52 PM
 *
 * @author Kevin V. Bui (kvb2univpitt@gmail.com)
 */
public class PagSamplingRfci implements IGraphSearch {

    private final int NUM_THREADS = 10;
    private final DataSet dataSet;
    // PagSamplingRfci
    private int numRandomizedSearchModels = 10;
    private boolean verbose = false;
    // Rfci parameters
    private int depth = -1;
    private int maxPathLength = -1;
    // IndTestProbabilistic parameters
    private boolean threshold = true;
    private double cutoff = 0.5;
    private double priorEquivalentSampleSize = 10;
    /**
     * The background knowledge.
     */
    private Knowledge knowledge;

    public PagSamplingRfci(DataSet dataSet) {
        this.dataSet = dataSet;
    }

    @Override
    public Graph search() {
        List<Graph> graphs = runSearches();

        return GraphSampling.createGraphWithHighProbabilityEdges(graphs);
    }

    List<Callable<Graph>> createTasks(int numOfTasks) {
        List<Callable<Graph>> callableTasks = new LinkedList<>();

        for (int i = 0; i < numOfTasks; i++) {
            callableTasks.add(new RfciSearchTask());
        }

        return callableTasks;
    }

    private List<Graph> runSearches() {
        List<Graph> graphs = new LinkedList<>();

        ExecutorService pool = Executors.newFixedThreadPool(NUM_THREADS);
        try {
            while (graphs.size() < numRandomizedSearchModels && !Thread.currentThread().isInterrupted()) {
                List<Callable<Graph>> callableTasks = createTasks(numRandomizedSearchModels - graphs.size());
                List<Future<Graph>> completedTasks = pool.invokeAll(callableTasks);
                for (Future<Graph> completedTask : completedTasks) {
                    try {
                        Graph graph = completedTask.get();
                        if (graph != null && GraphSearchUtils.isLegalPag(graph).isLegalPag()) {
                            graphs.add(graph);
                        }
                    } catch (ExecutionException exception) {
                        exception.printStackTrace(System.err);
                    }
                }
            }
        } catch (InterruptedException exception) {
            exception.printStackTrace(System.err);
        } finally {
            shutdownAndAwaitTermination(pool);
        }

        return graphs;
    }

    /**
     * Call shutdown to reject incoming tasks, and then calling shutdownNow, if necessary, to cancel any lingering
     * tasks.
     */
    private void shutdownAndAwaitTermination(ExecutorService pool) {
        pool.shutdown();
        try {
            if (!pool.awaitTermination(5, TimeUnit.SECONDS)) {
                pool.shutdownNow();
                if (!pool.awaitTermination(5, TimeUnit.SECONDS)) {
                    System.err.println("Pool did not terminate");
                }
            }
        } catch (InterruptedException ie) {
            pool.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    public void setNumRandomizedSearchModels(int numRandomizedSearchModels) {
        this.numRandomizedSearchModels = numRandomizedSearchModels;
    }

    public void setVerbose(boolean verbose) {
        this.verbose = verbose;
    }

    public void setDepth(int depth) {
        this.depth = depth;
    }

    public void setMaxPathLength(int maxPathLength) {
        this.maxPathLength = maxPathLength;
    }

    public void setThreshold(boolean threshold) {
        this.threshold = threshold;
    }

    public void setCutoff(double cutoff) {
        this.cutoff = cutoff;
    }

    public void setPriorEquivalentSampleSize(double priorEquivalentSampleSize) {
        this.priorEquivalentSampleSize = priorEquivalentSampleSize;
    }

    public void setKnowledge(Knowledge knowledge) {
        this.knowledge = knowledge;
    }

    private class RfciSearchTask implements Callable<Graph> {

        public RfciSearchTask() {
        }

        @Override
        public Graph call() throws Exception {
            try {
                IndTestProbabilistic independenceTest = new IndTestProbabilistic(dataSet);
                independenceTest.setThreshold(threshold);
                independenceTest.setCutoff(cutoff);
                independenceTest.setPriorEquivalentSampleSize(priorEquivalentSampleSize);
                independenceTest.setVerbose(verbose);

                Rfci rfci = new Rfci(independenceTest);
                if (knowledge != null) {
                    rfci.setKnowledge(knowledge);
                }
                rfci.setDepth(depth);
                rfci.setMaxPathLength(maxPathLength);
                rfci.setVerbose(verbose);

                return rfci.search();
            } catch (Exception exception) {
                exception.printStackTrace(System.err);

                return null;
            }
        }

    }

}
