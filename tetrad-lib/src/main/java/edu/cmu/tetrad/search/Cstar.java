package edu.cmu.tetrad.search;

import edu.cmu.tetrad.algcomparison.algorithm.oracle.cpdag.RestrictedBoss;
import edu.cmu.tetrad.algcomparison.independence.IndependenceWrapper;
import edu.cmu.tetrad.algcomparison.score.ScoreWrapper;
import edu.cmu.tetrad.data.*;
import edu.cmu.tetrad.graph.*;
import edu.cmu.tetrad.search.score.Score;
import edu.cmu.tetrad.util.*;
import edu.pitt.dbmi.data.reader.Delimiter;

import java.io.*;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.*;
import java.util.concurrent.*;

/**
 * <p>Implements the CStaR algorithm (Steckoven et al., 2012), which finds a CPDAG of that
 * data and then tries all orientations of the undirected edges about a variable in the CPDAG to estimate a minimum
 * bound on the effect for a given edge. Some references include the following:</p>
 *
 * <p>Stekhoven, D. J., Moraes, I., Sveinbjörnsson, G., Hennig, L., Maathuis, M. H., and
 * Bühlmann, P. (2012). Causal stability ranking. Bioinformatics, 28(21), 2819-2823.</p>
 *
 * <p>Meinshausen, N., and Bühlmann, P. (2010). Stability selection. Journal of the Royal
 * Statistical Society: Series B (Statistical Methodology), 72(4), 417-473.</p>
 *
 * <p>Colombo, D., and Maathuis, M. H. (2014). Order-independent constraint-based causal
 * structure learning. The Journal of Machine Learning Research, 15(1), 3741-3782.</p>
 *
 * <p>This class is not configured to respect knowledge of forbidden and required
 * edges.</p>
 *
 * @author josephramsey
 * @see Ida
 */
public class Cstar {
    private boolean parallelized = false;
    private int numSubsamples = 30;
    private int qFrom = 1;
    private int qTo = 1;
    private int qIncrement = 1;
    private double selectionAlpha = 0.0;
    private IndependenceWrapper test;
    private ScoreWrapper score;
    private Parameters parameters = new Parameters();
    private CpdagAlgorithm cpdagAlgorithm = CpdagAlgorithm.PC_STABLE;
    private SampleStyle sampleStyle = SampleStyle.BOOTSTRAP;
    private boolean verbose;

    /**
     * Constructor.
     */
    public Cstar(IndependenceWrapper test, ScoreWrapper score, Parameters parameters) {
        this.test = test;
        this.score = score;
        this.parameters = parameters;
    }

    /**
     * Returns a list of records for making a CSTaR table.
     *
     * @param allRecords the list of all records.
     * @return The list for the CSTaR table.
     */
    public static LinkedList<Record> cStar(LinkedList<LinkedList<Record>> allRecords) {
        Map<Edge, List<Record>> map = new HashMap<>();

        for (List<Record> records : allRecords) {
            for (Record record : records) {
                Edge edge = Edges.directedEdge(record.getCauseNode(), record.getEffectNode());
                map.computeIfAbsent(edge, k -> new ArrayList<>());
                map.get(edge).add(record);
            }
        }

        LinkedList<Record> cstar = new LinkedList<>();

        for (Edge edge : map.keySet()) {
            List<Record> recordList = map.get(edge);

            double[] pis = new double[recordList.size()];
            double[] effects = new double[recordList.size()];

            for (int i = 0; i < recordList.size(); i++) {
                pis[i] = recordList.get(i).getPi();
                effects[i] = recordList.get(i).getMinBeta();
            }

            double medianPis = StatUtils.median(pis);
            double medianEffects = StatUtils.median(effects);

            Record _record = new Record(edge.getNode1(), edge.getNode2(), medianPis, medianEffects, -1, -1);
            cstar.add(_record);
        }

        cstar.sort((o1, o2) -> {
            if (o1.getPi() == o2.getPi()) {
                return Double.compare(o2.getMinBeta(), o1.getMinBeta());
            } else {
                return Double.compare(o2.getPi(), o1.getPi());
            }
        });

        return cstar;
    }

    // Meinhausen and Buhlmann E(|V|) bound
    private static double er(double pi, double q, double p) {
        return p * Cstar.pcer(pi, q, p);
    }

    // Meinhausen and Buhlmann per comparison error rate (PCER)
    private static double pcer(double pi, double q, double p) {
        return (q * q) / (p * p * (2 * pi - 1));
    }

    /**
     * Sets whether the algorithm should be parallelized. Different runs of the algorithms can be run in different
     * threads in parallel.
     *
     * @param parallelized True, if so.
     */
    public void setParallelized(boolean parallelized) {
        this.parallelized = parallelized;
    }

    /**
     * Returns records for a set of variables with expected number of false positives bounded by q.
     *
     * @param dataSet         The full datasets to search over.
     * @param possibleCauses  A set of variables in the datasets over which to search.
     * @param possibleEffects The effect variables.
     * @see Record
     */
    public LinkedList<LinkedList<Record>> getRecords(DataSet dataSet, List<Node> possibleCauses,
                                                     List<Node> possibleEffects) {
        return getRecords(dataSet, possibleCauses, possibleEffects, null);
    }

    /**
     * Returns records for a set of variables with expected number of false positives bounded by q.
     *
     * @param dataSet         The full datasets to search over.
     * @param possibleCauses  A set of variables in the datasets over which to search.
     * @param possibleEffects The effect variables.
     * @param path            A path where interim results are to be stored. If null, interim results will not be
     *                        stored. If the path is specified, then if the process is stopped and restarted, previously
     *                        computed interim results will be loaded.
     * @see Record
     */
    public LinkedList<LinkedList<Record>> getRecords(DataSet dataSet, List<Node> possibleCauses,
                                                     List<Node> possibleEffects, String path) {

        possibleEffects = GraphUtils.replaceNodes(possibleEffects, dataSet.getVariables());
        possibleCauses = GraphUtils.replaceNodes(possibleCauses, dataSet.getVariables());

        int p = possibleCauses.size() * possibleEffects.size();

        List<Integer> qs = new ArrayList<>();

        for (int q = this.qFrom; q <= this.qTo; q += this.qIncrement) {
            if (q <= p) {
                qs.add(q);
            } else {
                TetradLogger.getInstance().forceLogMessage("q = " + q + " > p = " + p + "; skipping");
            }
        }

        if (qs.isEmpty()) return new LinkedList<>();

        LinkedList<LinkedList<Record>> allRecords = new LinkedList<>();

        File _dir = null;

        if (path != null) {
            _dir = new File(path);
            System.out.println("dir = " + _dir.getAbsolutePath());

            boolean b = _dir.mkdirs();

            if (b) {
                System.out.println("Creating directories for " + _dir.getAbsolutePath());
            }
        }

        File dir = _dir;

        if (dir != null) {

            if (new File(dir, "possible.causes.txt").exists() && new File(dir, "possible.causes.txt").exists()) {
                possibleCauses = readVars(dataSet, dir, "possible.causes.txt");
                possibleEffects = readVars(dataSet, dir, "possible.effects.txt");
                dataSet = readData(dir);
            } else {
                writeVars(possibleCauses, dir, "possible.causes.txt");
                writeVars(possibleEffects, dir, "possible.effects.txt");
                writeData(dataSet, dir);
            }
        }

        List<Map<Integer, Map<Node, Double>>> minimalEffects = new ArrayList<>();

        for (int t = 0; t < possibleEffects.size(); t++) {
            minimalEffects.add(new ConcurrentHashMap<>());

            for (int b = 0; b < this.numSubsamples; b++) {
                Map<Node, Double> map = new ConcurrentHashMap<>();
                for (Node node : possibleCauses) map.put(node, 0.0);
                minimalEffects.get(t).put(b, map);
            }
        }

        int[] edgesTotal = new int[1];
        int[] edgesCount = new int[1];

        class Task implements Callable<double[][]> {
            private final List<Node> possibleCauses;
            private final List<Node> possibleEffects;
            private final int k;
            private final DataSet _dataSet;

            private Task(int k, List<Node> possibleCauses, List<Node> possibleEffects, DataSet _dataSet) {
                this.k = k;
                this.possibleCauses = possibleCauses;
                this.possibleEffects = possibleEffects;
                this._dataSet = _dataSet;
            }

            public double[][] call() {
                try {
                    BootstrapSampler sampler = new BootstrapSampler();
                    DataSet sample;

                    if (Cstar.this.sampleStyle == SampleStyle.BOOTSTRAP) {
                        sampler.setWithoutReplacements(false);
                        sample = sampler.sample(this._dataSet, this._dataSet.getNumRows());
                    } else if (Cstar.this.sampleStyle == SampleStyle.SPLIT) {
                        sampler.setWithoutReplacements(true);
                        sample = sampler.sample(this._dataSet, this._dataSet.getNumRows() / 2);
                    } else {
                        throw new IllegalArgumentException("That type of sample is not configured: " + Cstar.this.sampleStyle);
                    }

                    Graph cpdag = null;
                    double[][] effects = null;

                    if (dir != null && new File(dir, "cpdag." + (this.k + 1) + ".txt").exists() && new File(dir, "effects." + (this.k + 1) + ".txt").exists()) {
                        try {
                            cpdag = GraphSaveLoadUtils.loadGraphTxt(new File(dir, "cpdag." + (this.k + 1) + ".txt"));
                            effects = loadMatrix(new File(dir, "effects." + (this.k + 1) + ".txt"));
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }

                    if (cpdag == null || effects == null) {
                        if (Cstar.this.cpdagAlgorithm == CpdagAlgorithm.PC_STABLE) {
                            cpdag = getPatternPcStable(sample);
                        } else if (Cstar.this.cpdagAlgorithm == CpdagAlgorithm.FGES) {
                            cpdag = getPatternFges(sample);
                        } else if (Cstar.this.cpdagAlgorithm == CpdagAlgorithm.BOSS) {
                            cpdag = getPatternBoss(sample);
                        } else if (Cstar.this.cpdagAlgorithm == CpdagAlgorithm.RESTRICTED_BOSS) {
                            cpdag = getPatternRestrictedBoss(sample, this._dataSet);
                        } else {
                            throw new IllegalArgumentException("That type of of cpdag algorithm is not configured: " + Cstar.this.cpdagAlgorithm);
                        }

                        edgesTotal[0] += cpdag.getNumEdges();
                        edgesCount[0]++;

                        if (dir != null) {
                            GraphSaveLoadUtils.saveGraph(cpdag, new File(dir, "cpdag." + (this.k + 1) + ".txt"), false);
                        }

                        Ida ida = new Ida(sample, cpdag, this.possibleCauses);

                        effects = new double[this.possibleCauses.size()][this.possibleEffects.size()];

                        for (int e = 0; e < this.possibleEffects.size(); e++) {
                            Map<Node, Double> minEffects = ida.calculateMinimumEffectsOnY(this.possibleEffects.get(e));

                            for (int c = 0; c < this.possibleCauses.size(); c++) {
                                final Double _e = minEffects.get(this.possibleCauses.get(c));
                                effects[c][e] = _e != null ? _e : 0.0;
                            }
                        }

                        if (dir != null) {
                            saveMatrix(effects, new File(dir, "effects." + (this.k + 1) + ".txt"));
                        } else {
                            saveMatrix(effects, null);
                        }
                    }

                    return effects;
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        }

        List<Callable<double[][]>> tasks = new ArrayList<>();

        for (int k = 0; k < this.numSubsamples; k++) {
            tasks.add(new Task(k, possibleCauses, possibleEffects, dataSet));
        }

        List<double[][]> allEffects = runCallablesDoubleArray(tasks, parallelized);

        int avgEdges = (int) (edgesTotal[0] / (double) edgesCount[0]) + 1;

        qs.clear();
        qs.add(avgEdges);

        List<List<Double>> doubles = new ArrayList<>();

        for (int k = 0; k < this.numSubsamples; k++) {
            double[][] effects = allEffects.get(k);

            if (effects.length != possibleCauses.size() || effects[0].length != possibleEffects.size()) {
                throw new IllegalStateException("Bootstrap " + (k + 1) + " is damaged; delete the pattern and effect files for that bootstrap and rerun");
            }

            List<Double> _doubles = new ArrayList<>();

            for (int c = 0; c < possibleCauses.size(); c++) {
                for (int e = 0; e < possibleEffects.size(); e++) {
                    _doubles.add(effects[c][e]);
                }
            }

            _doubles.sort((o1, o2) -> Double.compare(o2, o1));
            doubles.add(_doubles);
        }

        class Task2 implements Callable<Boolean> {
            private final List<Node> possibleCauses;
            private final List<Node> possibleEffects;
            private final int q;

            private Task2(List<Node> possibleCauses, List<Node> possibleEffects, int q) {
                this.possibleCauses = possibleCauses;
                this.possibleEffects = possibleEffects;

                if (q == 0) {
                    throw new IllegalArgumentException("q must be positive");
                }

                this.q = q;
            }

            public Boolean call() {
                try {
                    if (Cstar.this.verbose) {
                        TetradLogger.getInstance().forceLogMessage("Examining q = " + this.q);
                    }

                    List<Tuple> tuples = new ArrayList<>();

                    for (int e = 0; e < this.possibleEffects.size(); e++) {
                        for (int c = 0; c < this.possibleCauses.size(); c++) {
                            int count = 0;

                            for (int k = 0; k < Cstar.this.numSubsamples; k++) {
                                if (this.q > doubles.get(k).size()) {
                                    continue;
                                }

                                double cutoff = doubles.get(k).get(this.q - 1);

                                if (allEffects.get(k)[c][e] >= cutoff) {
                                    count++;
                                }
                            }

                            double pi = count / ((double) Cstar.this.numSubsamples);
                            if (pi < (this.q / (double) p)) continue;
                            Node cause = this.possibleCauses.get(c);
                            Node effect = this.possibleEffects.get(e);
                            tuples.add(new Tuple(cause, effect, pi, avgMinEffect(this.possibleCauses, this.possibleEffects, allEffects, cause, effect)));
                        }
                    }

                    tuples.sort((o1, o2) -> {
                        if (o1.getPi() == o2.getPi()) {
                            return Double.compare(o2.getMinBeta(), o1.getMinBeta());
                        } else {
                            return Double.compare(o2.getPi(), o1.getPi());
                        }
                    });

                    LinkedList<Record> records = new LinkedList<>();

                    for (Tuple tuple : tuples) {
                        double avg = tuple.getMinBeta();

                        Node causeNode = tuple.getCauseNode();
                        Node effectNode = tuple.getEffectNode();

                        if (tuple.getMinBeta() > selectionAlpha) {
                            records.add(new Record(causeNode, effectNode, tuple.getPi(), avg, this.q, p));
                        }
                    }

                    allRecords.add(records);
                    return true;
                } catch (Exception e) {
                    e.printStackTrace();
                }

                return null;
            }
        }

        List<Callable<Boolean>> task2s = new ArrayList<>();

        for (int q : qs) {
            task2s.add(new Task2(possibleCauses, possibleEffects, q));
        }

        ConcurrencyUtils.runCallables(task2s, parallelized);

        allRecords.sort(Comparator.comparingDouble(List::size));

        return allRecords;
    }

    /**
     * Makes a graph of the estimated predictors to the effect.
     *
     * @param records The list of records obtained from a method above.
     */
    public Graph makeGraph(List<Record> records) {
        List<Node> outNodes = new ArrayList<>();

        Graph graph = new EdgeListGraph(outNodes);

        for (Record record : records) {
            graph.addNode(record.getCauseNode());
            graph.addNode(record.getEffectNode());
            graph.addDirectedEdge(record.getCauseNode(), record.getEffectNode());
        }

        return graph;
    }

    /**
     * Sets qFrom.
     *
     * @param qFrom This integer.
     */
    public void setqFrom(int qFrom) {
        this.qFrom = qFrom;
    }

    /**
     * Sets qTo.
     *
     * @param qTo This integer.
     */
    public void setqTo(int qTo) {
        this.qTo = qTo;
    }

    /**
     * Sets q increment.
     *
     * @param qIncrement This integer.
     */
    public void setqIncrement(int qIncrement) {
        this.qIncrement = qIncrement;
    }

    /**
     * The CSTaR algorithm can use any CPDAG algorithm; here you can set it.
     *
     * @param cpdagAlgorithm The CPDAG algorithm.
     * @see CpdagAlgorithm
     */
    public void setCpdagAlgorithm(CpdagAlgorithm cpdagAlgorithm) {
        this.cpdagAlgorithm = cpdagAlgorithm;
    }

    /**
     * Sets whether verbose output will be printed.
     *
     * @param verbose True, if so.
     */
    public void setVerbose(boolean verbose) {
        this.verbose = verbose;
    }

    /**
     * Sets the selection alpha.
     *
     * @param selectionAlpha This alpha.
     */
    public void setSelectionAlpha(double selectionAlpha) {
        this.selectionAlpha = selectionAlpha;
    }

    /**
     * Sets the sample style.
     *
     * @param sampleStyle This style.
     * @see SampleStyle
     */
    public void setSampleStyle(SampleStyle sampleStyle) {
        this.sampleStyle = sampleStyle;
    }

    /**
     * Sets the number of subsamples.
     *
     * @param numSubsamples This number.
     */
    public void setNumSubsamples(int numSubsamples) {
        this.numSubsamples = numSubsamples;
    }

    private DataSet readData(File dir) {
        try {
            DataSet dataSet = SimpleDataLoader.loadContinuousData(new File(dir, "data.txt"), "//", '*', "*", true, Delimiter.TAB);

            TetradLogger.getInstance().forceLogMessage("Loaded data " + dataSet.getNumRows() + " x " + dataSet.getNumColumns());

            return dataSet;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void writeData(DataSet dataSet, File dir) {
        try {
            PrintStream out = new PrintStream(new FileOutputStream(new File(dir, "data.txt")));
            out.println(dataSet.toString());
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    private List<Node> readVars(DataSet dataSet, File dir, String s) {
        try {
            List<Node> vars = new ArrayList<>();

            File file = new File(dir, s);

            BufferedReader in = new BufferedReader(new FileReader(file));
            String line;

            while ((line = in.readLine()) != null) {
                if (line.trim().isEmpty()) continue;
                vars.add(dataSet.getVariable(line.trim()));
            }

            return vars;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void writeVars(List<Node> vars, File dir, String s) {
        try {
            File file = new File(dir, s);

            PrintStream out = new PrintStream(new FileOutputStream(file));

            for (Node node : vars) {
                out.println(node.getName());
            }

            out.flush();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    private double avgMinEffect(List<Node> possibleCauses, List<Node> possibleEffects, List<double[][]> allEffects, Node causeNode, Node effectNode) {
        List<Double> f = new ArrayList<>();

        if (allEffects == null) {
            throw new NullPointerException("effects null");
        }

        for (int k = 0; k < this.numSubsamples; k++) {
            int c = possibleCauses.indexOf(causeNode);
            int e = possibleEffects.indexOf(effectNode);
            f.add(allEffects.get(k)[c][e]);
        }

        double[] _f = new double[f.size()];
        for (int b = 0; b < f.size(); b++) _f[b] = f.get(b);
        return StatUtils.mean(_f);
    }

    /**
     * Returns a text table from the given records
     */
    public String makeTable(LinkedList<Record> records, boolean printTable, boolean cstar) {
        int numColumns = 7;

        TextTable table = new TextTable(records.size() + 1, numColumns);
//        table.setLatex(true);
        NumberFormat nf = new DecimalFormat("0.0000");
        int column = 0;

        table.setToken(0, column++, "Index");
        table.setToken(0, column++, "Cause");
        table.setToken(0, column++, "Effect");
        table.setToken(0, column++, "PI");
        table.setToken(0, column++, "Effect");
        table.setToken(0, column++, "R-SUM(Pi)");

        if (cstar) {
            table.setToken(0, column, "PCER");
        } else {
            table.setToken(0, column++, "E(V)");
        }

        double sumPi = 0.0;

        if (records.isEmpty()) {
            return "\nThere are no records above chance.\n";
        }

        int p = records.getLast().getP();
        int q = records.getLast().getQ();

        for (int i = 0; i < records.size(); i++) {
            Node cause = records.get(i).getCauseNode();
            Node effect = records.get(i).getEffectNode();

            int R = (i + 1);
            sumPi += records.get(i).getPi();
            column = 0;


            table.setToken(i + 1, column++, String.valueOf(i + 1));
            table.setToken(i + 1, column++, cause.getName());
            table.setToken(i + 1, column++, effect.getName());
            table.setToken(i + 1, column++, nf.format(records.get(i).getPi()));
            table.setToken(i + 1, column++, nf.format(records.get(i).getMinBeta()));
            table.setToken(i + 1, column++, nf.format(R - sumPi));

            if (cstar) {
                double pcer = Cstar.pcer(records.get(i).getPi(), (i + 1), p);
                table.setToken(i + 1, column, records.get(i).getPi() <= 0.5 ? "*" : nf.format(pcer));
            } else {
                double er = Cstar.er(records.get(i).getPi(), (i + 1), p);
                table.setToken(i + 1, column++, records.get(i).getPi() <= 0.5 ? "*" : nf.format(er));
            }
//            double er = Cstar.er(records.get(i).getPi(), (i + 1), p);
//            table.setToken(i + 1, column++, records.get(i).getPi() <= 0.5 ? "*" : nf.format(er));
        }

        return (printTable ? "\n" + table : "") + "p = " + p + " q = " + q + (printTable ? " Type: C = continuous, D = discrete\n" : "");
    }

    private Graph getPatternPcStable(DataSet sample) {
        IndependenceTest test = this.test.getTest(sample, parameters);
        test.setVerbose(false);
        Pc pc = new Pc(test);
        pc.setStable(true);
        pc.setVerbose(false);
        return pc.search();
    }

    private Graph getPatternFges(DataSet sample) {
        Score score = this.score.getScore(sample, parameters);
        Fges fges = new Fges(score);
        fges.setVerbose(false);
        return fges.search();
    }

    private Graph getPatternBoss(DataSet sample) {
        Score score = this.score.getScore(sample, parameters);
        PermutationSearch boss = new PermutationSearch(new Boss(score));
        return boss.search();
    }

    private Graph getPatternRestrictedBoss(DataSet sample, DataSet data) {
        RestrictedBoss restrictedBoss = new RestrictedBoss(score);
        Graph g = restrictedBoss.search(sample, parameters);
        g = GraphUtils.replaceNodes(g, data.getVariables());
        return g;
    }

    private void saveMatrix(double[][] effects, File file) {
        try {
            List<Node> vars = new ArrayList<>();
            for (int i = 0; i < effects[0].length; i++) vars.add(new ContinuousVariable("V" + (i + 1)));
            BoxDataSet data = new BoxDataSet(new DoubleDataBox(effects), vars);
            if (file != null) {
                PrintStream out = new PrintStream(new FileOutputStream(file));
                out.println(data);
            }
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    private double[][] loadMatrix(File file) {
        try {
            DataSet dataSet = SimpleDataLoader.loadContinuousData(file, "//", '\"', "*", true, Delimiter.TAB);

//            TetradLogger.getInstance().forceLogMessage("Loaded data " + dataSet.getNumRows() + " x " + dataSet.getNumColumns());

            return dataSet.getDoubleData().toArray();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private List<double[][]> runCallablesDoubleArray(List<Callable<double[][]>> tasks, boolean parallelized) {
        if (tasks.isEmpty()) return new ArrayList<>();

        List<double[][]> results = new ArrayList<>();

        if (!parallelized) {
            for (Callable<double[][]> task : tasks) {
                try {
                    results.add(task.call());
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        } else {
            ForkJoinPool executorService = ForkJoinPool.commonPool();

            try {
                List<Future<double[][]>> futures = executorService.invokeAll(tasks);

                for (Future<double[][]> future : futures) {
                    results.add(future.get());
                }
            } catch (InterruptedException | ExecutionException e) {
                e.printStackTrace();
            }
        }

        return results;
    }

    /**
     * An enumeration of the options available for determining the CPDAG used for the algorithm.
     */
    public enum CpdagAlgorithm {PC_STABLE, FGES, BOSS, RESTRICTED_BOSS}

    /**
     * An enumeration of the methods for selecting samples from the full dataset.
     */
    public enum SampleStyle {BOOTSTRAP, SPLIT}

    /**
     * Represents a single record in the returned table for CSTaR.
     */
    public static class Record implements TetradSerializable {
        static final long serialVersionUID = 23L;

        private final Node causeNode;
        private final Node target;
        private final double pi;
        private final double effect;
        private final int q;
        private final int p;

        Record(Node predictor, Node target, double pi, double minEffect, int q, int p) {
            this.causeNode = predictor;
            this.target = target;
            this.pi = pi;
            this.effect = minEffect;
            this.q = q;
            this.p = p;
        }

        public Node getCauseNode() {
            return this.causeNode;
        }

        public Node getEffectNode() {
            return this.target;
        }

        public double getPi() {
            return this.pi;
        }

        double getMinBeta() {
            return this.effect;
        }

        public int getQ() {
            return this.q;
        }

        public int getP() {
            return this.p;
        }
    }

    private static class Tuple {
        private final Node cause;
        private final Node effect;
        private final double pi;
        private final double minBeta;

        private Tuple(Node cause, Node effect, double pi, double minBeta) {
            this.cause = cause;
            this.effect = effect;
            this.pi = pi;
            this.minBeta = minBeta;
        }

        public Node getCauseNode() {
            return this.cause;
        }

        public Node getEffectNode() {
            return this.effect;
        }

        public double getPi() {
            return this.pi;
        }

        public double getMinBeta() {
            return this.minBeta;
        }
    }

}