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

package edu.cmu.tetradapp.editor;

import edu.cmu.tetrad.data.ContinuousVariable;
import edu.cmu.tetrad.data.DiscreteVariable;
import edu.cmu.tetrad.data.Histogram;
import edu.cmu.tetrad.graph.Node;
import edu.cmu.tetrad.util.StatUtils;
import edu.cmu.tetradapp.util.DoubleTextField;
import edu.cmu.tetradapp.util.IntSpinner;
import org.apache.commons.math3.util.FastMath;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ItemEvent;
import java.awt.event.MouseEvent;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.List;
import java.util.*;

/**
 * View for the Histogram class. Shows a histogram and gives controls for conditioning, etc.
 *
 * @author josephramsey
 */
public class HistogramPanel extends JPanel {
    private Node node;
    private boolean showControlPanel = true;
    public static final String[] tiles = {"1-tile", "2-tile", "tertile", "quartile", "quintile", "sextile",
            "septile", "octile", "nontile", "decile"};

    public HistogramPanel(Histogram histogram, Node node) {
        this(histogram, true, node);
    }

    /**
     * Constructs the view with a given histogram and data set.
     */
    public HistogramPanel(Histogram histogram, boolean showControlPanel, Node node) {
        this.node = node;
        this.histogram = histogram;

        this.showControlPanel = showControlPanel;

        setLayout(new BorderLayout());
    }

    //========================== Private Methods ============           ====================//

    /**
     * The line color around the histogram.
     */
    private static final Color LINE_COLOR = Color.GRAY.darker();

    /**
     * Bar colors for the histogram (ripped from causality lab)
     */
    private static final Color[] BAR_COLORS = {
            new Color(153, 102, 102), new Color(102, 102, 153), new Color(102, 153, 102), new Color(153, 102, 153),
            new Color(153, 153, 102), new Color(102, 153, 153), new Color(204, 153, 153), new Color(153, 153, 204),
            new Color(153, 204, 153), new Color(204, 153, 204),
            new Color(204, 204, 153), new Color(153, 204, 204), new Color(255, 204, 204), new Color(204, 204, 255),
            new Color(204, 255, 204)
    };

    private int paddingX;

    /**
     * The histogram to display.
     */
    private final Histogram histogram;

    /**
     * Format for continuous data.
     */
    private final NumberFormat format = new DecimalFormat("0.#");// NumberFormatUtil.getInstance().getNumberFormat();

    /**
     * A map from the rectangles that define the bars, to the number of units in the bar.
     */
    private final Map<Rectangle, Integer> rectMap = new LinkedHashMap<>();

    /**
     * Constructs the histogram display panel given the initial histogram to display.
     *
     * @param histogram The histogram to display.
     */
    public HistogramPanel(Histogram histogram, boolean showControlPanel) {
        this.showControlPanel = showControlPanel;

        paddingX = showControlPanel ? 40 : 0;

        if (histogram == null) {
            throw new NullPointerException("Given histogram must be null");
        }
        this.histogram = histogram;

        this.setToolTipText(" ");
    }

    //============================ PUblic Methods =============================//

    /**
     * Updates the histogram that is dispalyed to the given one.
     */
    public synchronized void updateView() {
        if (getHistogram() == null) {
            throw new NullPointerException("The given histogram must not be null");
        }
//            this.displayString = null;
        this.repaint();
    }


    public String getToolTipText(MouseEvent evt) {
        Point point = evt.getPoint();
        for (Rectangle rect : this.rectMap.keySet()) {
            if (rect.contains(point)) {
                Integer i = this.rectMap.get(rect);
                if (i != null) {
                    return i.toString();
                }
                break;
            }
        }
        return null;
    }


    /**
     * Paints the histogram and related items.
     */
    public void paintComponent(Graphics graphics) {
        int paddingY = 15;
        int height = getHeight();
        int width = getWidth();
        int displayedHeight = (int) (height - paddingY);
        int space = 2;
        int dash = 10;

        // set up variables.
        this.rectMap.clear();
        Graphics2D g2d = (Graphics2D) graphics;
        Histogram histogram = this.getHistogram();
        int[] freqs = histogram.getFrequencies();
        int categories = freqs.length;
        int barWidth = FastMath.max((width - paddingX) / categories, 2) - space;
        int topFreq = HistogramPanel.getMax(freqs);
        double scale = displayedHeight / (double) topFreq;
        FontMetrics fontMetrics = g2d.getFontMetrics();
        // draw background/surrounding box.
        g2d.setColor(this.getBackground());
        g2d.fillRect(0, 0, width + 2 * space, height);
        g2d.setColor(Color.WHITE);
        g2d.fillRect(0, 0, width, height);
        // draw the histogram
        for (int i = 0; i < categories; i++) {
            int freq = freqs[i];
            int y = (int) FastMath.ceil(scale * freq);
            int x = space * (i + 1) + barWidth * i + paddingX;
            g2d.setColor(HistogramPanel.getBarColor(i));
            Rectangle rect = new Rectangle(x, (height - y - space), barWidth, y);
            g2d.fill(rect);
            this.rectMap.put(rect, freq);
        }
        //border
        g2d.setColor(HistogramPanel.LINE_COLOR);
        g2d.drawRect(paddingX, 0, width - paddingX, height);
        // draw the buttom line
        g2d.setColor(HistogramPanel.LINE_COLOR);

        Node target = histogram.getTargetNode();

        if (target instanceof ContinuousVariable) {
            Map<Integer, Double> pointsAndValues = pickGoodPointsAndValues(histogram.getMin(),
                    histogram.getMax());

            if (showControlPanel) {
                for (int point : pointsAndValues.keySet()) {
                    double value = pointsAndValues.get(point);
                    if (point < width + space - 10) {
                        g2d.drawString(this.format.format(value), point + 2, height);
                    }
                    g2d.drawLine(point, height + dash, point, height);
                }
            }
        } else if (target instanceof DiscreteVariable) {
            DiscreteVariable var = (DiscreteVariable) target;
            java.util.List<String> _categories = var.getCategories();
            int i = -1;

            if (showControlPanel) {
                for (Rectangle rect : this.rectMap.keySet()) {
                    int x = (int) rect.getX();
                    g2d.drawString(_categories.get(++i), x, height);
                }
            }
        }

        if (showControlPanel) {
            // draw the side line
            g2d.setColor(HistogramPanel.LINE_COLOR);
            int topY = height - (int) FastMath.ceil(scale * topFreq);
            String top = String.valueOf(topFreq);
            g2d.drawString(top, paddingX - fontMetrics.stringWidth(top), topY - 2);
            g2d.drawLine(paddingX - dash, topY, paddingX, topY);
            g2d.drawString("0", paddingX - fontMetrics.stringWidth("0"), height - 2);
            g2d.drawLine(paddingX - dash, height, paddingX, height);
            int hSize = (height - topY) / 4;
            for (int i = 1; i < 4; i++) {
                int topHeight = height - hSize * i;
                g2d.drawLine(paddingX - dash, topHeight, paddingX, topHeight);
            }
        }

    }

    //========================== private methods ==========================//

    private Map<Integer, Double> pickGoodPointsAndValues(double minValue, double maxValue) {
        double range = maxValue - minValue;
        int powerOfTen = (int) FastMath.floor(FastMath.log(range) / FastMath.log(10));
        Map<Integer, Double> points = new HashMap<>();

        int low = (int) FastMath.floor(minValue / FastMath.pow(10, powerOfTen));
        int high = (int) FastMath.ceil(maxValue / FastMath.pow(10, powerOfTen));

        for (int i = low; i < high; i++) {
            double realValue = i * FastMath.pow(10, powerOfTen);
            Integer intValue = translateToInt(minValue, maxValue, realValue);

            if (intValue == null) {
                continue;
            }

            points.put(intValue, realValue);
        }

        return points;
    }

    private Integer translateToInt(double minValue, double maxValue, double value) {
        if (minValue >= maxValue) {
            throw new IllegalArgumentException();
        }
        if (paddingX >= 332) {
            throw new IllegalArgumentException();
        }

        double ratio = (value - minValue) / (maxValue - minValue);

        int intValue = (int) (FastMath.round(paddingX + ratio * (double) (332 - paddingX)));

        if (intValue < paddingX || intValue > 332) {
            return null;
        }

        return intValue;
    }

    private static Color getBarColor(int i) {
        return Color.RED.darker();
//            return HistogramPanel.BAR_COLORS[i % HistogramPanel.BAR_COLORS.length];
    }

    private static int getMax(int[] freqs) {
        int max = freqs[0];
        for (int i = 1; i < freqs.length; i++) {
            int current = freqs[i];
            if (current > max) {
                max = current;
            }
        }
        return max;
    }

    /**
     * The histogram we are displaying.
     */
    public Histogram getHistogram() {
        return this.histogram;
    }
}

class HistogramController extends JPanel {

    /**
     * The histogram we are working on.
     */
    private final Histogram histogram;

    /**
     * Combo box of all the variables.
     */
    private final JComboBox targetSelector;

    /**
     * A spinner that deals with category selection.
     */
    private IntSpinner numBarsSelector;

    private final JComboBox newConditioningVariableSelector;
    private final JButton newConditioningVariableButton;
    private final JButton removeConditioningVariableButton;
    private final List<ConditioningPanel> conditioningPanels = new ArrayList<>();

    // To provide some memory of previous settings for the inquiry dialogs.
    private final Map<Node, ConditioningPanel> conditioningPanelMap = new HashMap<>();

    /**
     * Constructs the editor panel given the initial histogram and the dataset.
     */
    public HistogramController(HistogramPanel histogramPanel) {
        this.setLayout(new BorderLayout());
        this.histogram = histogramPanel.getHistogram();
        Node target = this.histogram.getTargetNode();
        this.targetSelector = new JComboBox();
        ListCellRenderer renderer = new VariableBoxRenderer();
        this.targetSelector.setRenderer(renderer);

        List<Node> variables = this.histogram.getDataSet().getVariables();
        Collections.sort(variables);

        for (Node node : variables) {
            this.targetSelector.addItem(node);

            if (node == target) {
                this.targetSelector.setSelectedItem(node);
            }
        }

        this.targetSelector.addItemListener(e -> {
            if (e.getStateChange() == ItemEvent.SELECTED) {
                Node node = (Node) e.getItem();
                getHistogram().setTarget(node.getName());
                int maxBins = HistogramController.getMaxCategoryValue(getHistogram());
                int numBins = getHistogram().getNumBins();

                // Don't try to set the max on the existing num bars selector; there is (at least currently)
                // a bug in the IntSpinner that prevents the max from being increased once it's decreased, so
                // you can go from continuous to discrete but not discrete to continuous and have the number
                // of bins be reasonable. jdramsey 7/17/13
                HistogramController.this.numBarsSelector = new IntSpinner(numBins, 1, 3);
                HistogramController.this.numBarsSelector.setMin(2);
                HistogramController.this.numBarsSelector.setMax(maxBins);

                HistogramController.this.numBarsSelector.addChangeListener(e1 -> {
                    JSpinner s = (JSpinner) e1.getSource();
                    if ((getHistogram().getTargetNode() instanceof ContinuousVariable)) {
                        int value = (Integer) s.getValue();
                        getHistogram().setNumBins(value);
                        changeHistogram();
                    }
                });

                for (ConditioningPanel panel : new ArrayList<>(HistogramController.this.conditioningPanels)) {
                    HistogramController.this.conditioningPanels.remove(panel);
                }

                buildEditArea();
                resetConditioning();
                changeHistogram();
            }
        });

        this.numBarsSelector = new IntSpinner(this.histogram.getNumBins(), 1, 3);
        this.numBarsSelector.setMin(2);
        this.numBarsSelector.setMax(HistogramController.getMaxCategoryValue(this.histogram));

        this.numBarsSelector.addChangeListener(e -> {
            JSpinner s = (JSpinner) e.getSource();
            if ((getHistogram().getTargetNode() instanceof ContinuousVariable)) {
                int value = (Integer) s.getValue();
                getHistogram().setNumBins(value);
                changeHistogram();
            }
        });

        this.newConditioningVariableSelector = new JComboBox();

        for (Node node : variables) {
            this.newConditioningVariableSelector.addItem(node);
        }

        this.newConditioningVariableSelector.addItemListener(e -> {
            if (e.getStateChange() == ItemEvent.SELECTED) {
                System.out.println("New conditioning varible " + e.getItem());
            }
        });

        this.newConditioningVariableButton = new JButton("Add");

        this.newConditioningVariableButton.addActionListener(e -> {
            Node selected = (Node) HistogramController.this.newConditioningVariableSelector.getSelectedItem();

            if (selected == HistogramController.this.targetSelector.getSelectedItem()) {
                JOptionPane.showMessageDialog(HistogramController.this,
                        "The target variable cannot be conditioned on.");
                return;
            }

            for (ConditioningPanel panel : HistogramController.this.conditioningPanels) {
                if (selected == panel.getVariable()) {
                    JOptionPane.showMessageDialog(HistogramController.this,
                            "There is already a conditioning variable called " + selected + ".");
                    return;
                }
            }

            if (selected instanceof ContinuousVariable) {
                ContinuousVariable _var = (ContinuousVariable) selected;

                ContinuousConditioningPanel panel1 = (ContinuousConditioningPanel) HistogramController.this.conditioningPanelMap.get(_var);

                if (panel1 == null) {
                    panel1 = ContinuousConditioningPanel.getDefault(_var, HistogramController.this.histogram);
                }

                ContinuousInquiryPanel panel2 = new ContinuousInquiryPanel(_var, HistogramController.this.histogram, panel1);

                JOptionPane.showOptionDialog(HistogramController.this, panel2,
                        null, JOptionPane.DEFAULT_OPTION,
                        JOptionPane.PLAIN_MESSAGE, null, null, null);

                ContinuousConditioningPanel.Type type = panel2.getType();
                double low = panel2.getLow();
                double high = panel2.getHigh();
                int ntile = panel2.getNtile();
                int ntileIndex = panel2.getNtileIndex();

                ContinuousConditioningPanel panel3 = new ContinuousConditioningPanel(_var, low, high, ntile, ntileIndex, type);

                HistogramController.this.conditioningPanels.add(panel3);
                HistogramController.this.conditioningPanelMap.put(_var, panel3);
            } else if (selected instanceof DiscreteVariable) {
                DiscreteVariable _var = (DiscreteVariable) selected;
                DiscreteConditioningPanel panel1 = (DiscreteConditioningPanel) HistogramController.this.conditioningPanelMap.get(_var);

                if (panel1 == null) {
                    panel1 = DiscreteConditioningPanel.getDefault(_var);
                    HistogramController.this.conditioningPanelMap.put(_var, panel1);
                }

                DiscreteInquiryPanel panel2 = new DiscreteInquiryPanel(_var, panel1);

                JOptionPane.showOptionDialog(HistogramController.this, panel2,
                        null, JOptionPane.DEFAULT_OPTION,
                        JOptionPane.PLAIN_MESSAGE, null, null, null);

                String category = (String) panel2.getValuesDropdown().getSelectedItem();
                int index = _var.getIndex(category);

                DiscreteConditioningPanel panel3 = new DiscreteConditioningPanel(_var, index);
                HistogramController.this.conditioningPanels.add(panel3);
                HistogramController.this.conditioningPanelMap.put(_var, panel3);
            } else {
                throw new IllegalStateException();
            }

            buildEditArea();
            resetConditioning();
            changeHistogram();
        });

        this.removeConditioningVariableButton = new JButton("Remove Checked");

        this.removeConditioningVariableButton.addActionListener(e -> {
            for (ConditioningPanel panel : new ArrayList<>(HistogramController.this.conditioningPanels)) {
                if (panel.isSelected()) {
                    panel.setSelected(false);
                    HistogramController.this.conditioningPanels.remove(panel);
                }
            }

            buildEditArea();
            resetConditioning();
            changeHistogram();
        });

        // build the gui.
        HistogramController.restrictSize(this.targetSelector);
        HistogramController.restrictSize(this.numBarsSelector);
        HistogramController.restrictSize(this.newConditioningVariableSelector);
        HistogramController.restrictSize(this.newConditioningVariableButton);
        HistogramController.restrictSize(this.removeConditioningVariableButton);

        buildEditArea();
    }

    private void resetConditioning() {

        // Need to set the conditions on the histogram and also update the list of conditions in the view.
        this.histogram.removeConditioningVariables();

        for (ConditioningPanel panel : this.conditioningPanels) {
            if (panel instanceof edu.cmu.tetradapp.editor.HistogramController.ContinuousConditioningPanel) {
                Node node = panel.getVariable();
                double low = ((edu.cmu.tetradapp.editor.HistogramController.ContinuousConditioningPanel) panel).getLow();
                double high = ((edu.cmu.tetradapp.editor.HistogramController.ContinuousConditioningPanel) panel).getHigh();
                this.histogram.addConditioningVariable(node.getName(), low, high);

            } else if (panel instanceof edu.cmu.tetradapp.editor.HistogramController.DiscreteConditioningPanel) {
                Node node = panel.getVariable();
                int index = ((edu.cmu.tetradapp.editor.HistogramController.DiscreteConditioningPanel) panel).getIndex();
                this.histogram.addConditioningVariable(node.getName(), index);
            }
        }
    }

    private void buildEditArea() {
        Box main = Box.createVerticalBox();
        Box b1 = Box.createHorizontalBox();
        b1.add(new JLabel("Histogram for: "));
        b1.add(this.targetSelector);
        b1.add(new JLabel("# Bars: "));
        b1.add(this.numBarsSelector);
        b1.add(Box.createHorizontalGlue());
        main.add(b1);

        main.add(Box.createVerticalStrut(20));

        Box b3 = Box.createHorizontalBox();
        JLabel l1 = new JLabel("Conditioning on: ");
        l1.setFont(l1.getFont().deriveFont(Font.ITALIC));
        b3.add(l1);
        b3.add(Box.createHorizontalGlue());
        main.add(b3);

        main.add(Box.createVerticalStrut(20));

        for (ConditioningPanel panel : this.conditioningPanels) {
            main.add(panel.getBox());
            main.add(Box.createVerticalStrut(10));
        }

        main.add(Box.createVerticalStrut(10));

        main.add(Box.createVerticalGlue());

        for (int i = this.newConditioningVariableSelector.getItemCount() - 1; i >= 0; i--) {
            this.newConditioningVariableSelector.removeItemAt(i);
        }

        List<Node> variables = this.histogram.getDataSet().getVariables();
        Collections.sort(variables);

        NODE:
        for (Node node : variables) {
            if (node == this.targetSelector.getSelectedItem()) continue;

            for (ConditioningPanel panel : this.conditioningPanels) {
                if (node == panel.getVariable()) continue NODE;
            }

            this.newConditioningVariableSelector.addItem(node);
        }

        Box b6 = Box.createHorizontalBox();
        b6.add(this.newConditioningVariableSelector);
        b6.add(this.newConditioningVariableButton);
        b6.add(Box.createHorizontalGlue());
        main.add(b6);

        Box b7 = Box.createHorizontalBox();
        b7.add(this.removeConditioningVariableButton);
        b7.add(Box.createHorizontalGlue());
        main.add(b7);

        this.removeAll();
        this.add(main, BorderLayout.CENTER);
        revalidate();
        repaint();
    }

    //========================== Private Methods ================================//


    /**
     * @return the max category value that should be accepted for the given histogram.
     */
    private static int getMaxCategoryValue(Histogram histogram) {
        Node node = histogram.getTargetNode();

        if (node instanceof DiscreteVariable) {
            DiscreteVariable var = (DiscreteVariable) node;
            return var.getNumCategories();
        } else {
            return 40;
        }
    }

    private Histogram getHistogram() {
        return this.histogram;
    }

    // This causes the histogram panel to update.
    private void changeHistogram() {
        firePropertyChange("histogramChanged", null, this.histogram);
    }

    private static void restrictSize(JComponent component) {
        component.setMaximumSize(component.getPreferredSize());
    }

    //========================== Inner classes ===========================//


    private static class VariableBoxRenderer extends DefaultListCellRenderer {

        public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
            Node node = (Node) value;
            if (node == null) {
                this.setText("");
            } else {
                this.setText(node.getName());
            }
            if (isSelected) {
                setBackground(list.getSelectionBackground());
                setForeground(list.getSelectionForeground());
            } else {
                setBackground(list.getBackground());
                setForeground(list.getForeground());
            }

            return this;
        }
    }

    private interface ConditioningPanel {
        Box getBox();

        // selected for removal.
        boolean isSelected();

        Node getVariable();

        void setSelected(boolean b);
    }

    public static class DiscreteConditioningPanel implements ConditioningPanel {
        private final DiscreteVariable variable;
        private final String value;
        private final Box box;

        // Set selected if this checkbox should be removed.
        private final JCheckBox checkBox;
        private final int index;

        public DiscreteConditioningPanel(DiscreteVariable variable, int valueIndex) {
            if (variable == null) throw new NullPointerException();
            if (valueIndex < 0 || valueIndex >= variable.getNumCategories()) {
                throw new IllegalArgumentException("Not a category for this varible.");
            }

            this.variable = variable;
            this.value = variable.getCategory(valueIndex);
            this.index = valueIndex;

            Box b4 = Box.createHorizontalBox();
            b4.add(Box.createRigidArea(new Dimension(10, 0)));
            b4.add(new JLabel(variable + " = " + variable.getCategory(valueIndex)));
            b4.add(Box.createHorizontalGlue());
            this.checkBox = new JCheckBox();
            HistogramController.restrictSize(this.checkBox);
            b4.add(this.checkBox);
            this.box = b4;
        }

        public static edu.cmu.tetradapp.editor.HistogramController.DiscreteConditioningPanel getDefault(DiscreteVariable var) {
            return new edu.cmu.tetradapp.editor.HistogramController.DiscreteConditioningPanel(var, 0);
        }

        public DiscreteVariable getVariable() {
            return this.variable;
        }

        public String getValue() {
            return this.value;
        }

        public int getIndex() {
            return this.index;
        }

        public Box getBox() {
            return this.box;
        }

        public boolean isSelected() {
            return this.checkBox.isSelected();
        }

        public void setSelected(boolean b) {
            this.checkBox.setSelected(false);
        }

    }

    public static class ContinuousConditioningPanel implements ConditioningPanel {

        public int getNtile() {
            return this.ntile;
        }

        public int getNtileIndex() {
            return this.ntileIndex;
        }

        public enum Type {Range, Ntile, AboveAverage, BelowAverage}

        private final ContinuousVariable variable;
        private final Box box;

        private final edu.cmu.tetradapp.editor.HistogramController.ContinuousConditioningPanel.Type type;
        private final double low;
        private final double high;
        private final int ntile;
        private final int ntileIndex;

        // Mark selected if this panel is to be removed.
        private final JCheckBox checkBox;

        public ContinuousConditioningPanel(ContinuousVariable variable, double low, double high, int ntile, int ntileIndex, edu.cmu.tetradapp.editor.HistogramController.ContinuousConditioningPanel.Type type) {
            if (variable == null) throw new NullPointerException();
            if (low >= high) {
                throw new IllegalArgumentException("Low >= high.");
            }
            if (ntile < 2 || ntile > 10) {
                throw new IllegalArgumentException("Ntile should be in range 2 to 10: " + ntile);
            }

            this.variable = variable;
            NumberFormat nf = new DecimalFormat("0.0000");

            this.type = type;
            this.low = low;
            this.high = high;
            this.ntile = ntile;
            this.ntileIndex = ntileIndex;

            Box b4 = Box.createHorizontalBox();
            b4.add(Box.createRigidArea(new Dimension(10, 0)));

            if (type == edu.cmu.tetradapp.editor.HistogramController.ContinuousConditioningPanel.Type.Range) {
                b4.add(new JLabel(variable + " = (" + nf.format(low) + ", " + nf.format(high) + ")"));
            } else if (type == edu.cmu.tetradapp.editor.HistogramController.ContinuousConditioningPanel.Type.AboveAverage) {
                b4.add(new JLabel(variable + " = Above Average"));
            } else if (type == edu.cmu.tetradapp.editor.HistogramController.ContinuousConditioningPanel.Type.BelowAverage) {
                b4.add(new JLabel(variable + " = Below Average"));
            } else if (type == edu.cmu.tetradapp.editor.HistogramController.ContinuousConditioningPanel.Type.Ntile) {
                b4.add(new JLabel(variable + " = " + edu.cmu.tetradapp.editor.HistogramPanel.tiles[ntile - 1] + " " + ntileIndex));
            }

            b4.add(Box.createHorizontalGlue());
            this.checkBox = new JCheckBox();
            HistogramController.restrictSize(this.checkBox);
            b4.add(this.checkBox);
            this.box = b4;

        }

        public static edu.cmu.tetradapp.editor.HistogramController.ContinuousConditioningPanel getDefault(ContinuousVariable variable, Histogram histogram) {
            double[] data = histogram.getContinuousData(variable.getName());
            double max = StatUtils.max(data);
            double avg = StatUtils.mean(data);
            return new edu.cmu.tetradapp.editor.HistogramController.ContinuousConditioningPanel(variable, avg, max, 2, 1, edu.cmu.tetradapp.editor.HistogramController.ContinuousConditioningPanel.Type.AboveAverage);
        }

        public ContinuousVariable getVariable() {
            return this.variable;
        }

        public edu.cmu.tetradapp.editor.HistogramController.ContinuousConditioningPanel.Type getType() {
            return this.type;
        }

        public Box getBox() {
            return this.box;
        }

        public boolean isSelected() {
            return this.checkBox.isSelected();
        }

        public void setSelected(boolean b) {
            this.checkBox.setSelected(false);
        }

        public double getLow() {
            return this.low;
        }

        public double getHigh() {
            return this.high;
        }
    }
}

class ContinuousInquiryPanel extends JPanel {
    private final JComboBox ntileCombo;
    private final JComboBox ntileIndexCombo;
    private final DoubleTextField field1;
    private final DoubleTextField field2;
    private edu.cmu.tetradapp.editor.HistogramController.ContinuousConditioningPanel.Type type;
    private final Map<String, Integer> ntileMap = new HashMap<>();
    private final double[] data;

    /**
     * @param variable          This is the variable being conditioned on. Must be continuous and one of the variables
     *                          in the histogram.
     * @param histogram         We need this to get the column of data for the variable.
     * @param conditioningPanel We will try to get some initialization information out of the conditioning
     *                          panel. This must be for the same variable as variable.
     */
    public ContinuousInquiryPanel(ContinuousVariable variable, Histogram histogram,
                                  edu.cmu.tetradapp.editor.HistogramController.ContinuousConditioningPanel conditioningPanel) {
        this.data = histogram.getContinuousData(variable.getName());

        if (conditioningPanel == null)
            throw new NullPointerException();
        if (!(variable == conditioningPanel.getVariable()))
            throw new IllegalArgumentException("Wrong variable for conditioning panel.");

        // There is some order dependence in the below; careful rearranging things.
        NumberFormat nf = new DecimalFormat("0.00");

        this.field1 = new DoubleTextField(conditioningPanel.getLow(), 4, nf);
        this.field2 = new DoubleTextField(conditioningPanel.getHigh(), 4, nf);

        JRadioButton radio1 = new JRadioButton();
        JRadioButton radio2 = new JRadioButton();
        JRadioButton radio3 = new JRadioButton();
        JRadioButton radio4 = new JRadioButton();

        radio1.addActionListener(e -> {
            ContinuousInquiryPanel.this.type = HistogramController.ContinuousConditioningPanel.Type.AboveAverage;
            ContinuousInquiryPanel.this.field1.setValue(StatUtils.mean(ContinuousInquiryPanel.this.data));
            ContinuousInquiryPanel.this.field2.setValue(StatUtils.max(ContinuousInquiryPanel.this.data));
        });

        radio2.addActionListener(e -> {
            ContinuousInquiryPanel.this.type = HistogramController.ContinuousConditioningPanel.Type.BelowAverage;
            ContinuousInquiryPanel.this.field1.setValue(StatUtils.min(ContinuousInquiryPanel.this.data));
            ContinuousInquiryPanel.this.field2.setValue(StatUtils.mean(ContinuousInquiryPanel.this.data));
        });

        radio3.addActionListener(e -> {
            ContinuousInquiryPanel.this.type = HistogramController.ContinuousConditioningPanel.Type.Ntile;
            double[] breakpoints = ContinuousInquiryPanel.getNtileBreakpoints(ContinuousInquiryPanel.this.data, getNtile());
            double breakpoint1 = breakpoints[getNtileIndex() - 1];
            double breakpoint2 = breakpoints[getNtileIndex()];
            ContinuousInquiryPanel.this.field1.setValue(breakpoint1);
            ContinuousInquiryPanel.this.field2.setValue(breakpoint2);
        });

        radio4.addActionListener(e -> ContinuousInquiryPanel.this.type = HistogramController.ContinuousConditioningPanel.Type.Range);

        ButtonGroup group = new ButtonGroup();
        group.add(radio1);
        group.add(radio2);
        group.add(radio3);
        group.add(radio4);

        this.type = conditioningPanel.getType();

        this.ntileCombo = new JComboBox();
        this.ntileIndexCombo = new JComboBox();

        int ntile = conditioningPanel.getNtile();
        int ntileIndex = conditioningPanel.getNtileIndex();

        for (int n = 2; n <= 10; n++) {
            this.ntileCombo.addItem(edu.cmu.tetradapp.editor.HistogramPanel.tiles[n - 1]);
            this.ntileMap.put(edu.cmu.tetradapp.editor.HistogramPanel.tiles[n - 1], n);
        }

        for (int n = 1; n <= ntile; n++) {
            this.ntileIndexCombo.addItem(n);
        }

        this.ntileCombo.setSelectedItem(edu.cmu.tetradapp.editor.HistogramPanel.tiles[ntile - 1]);
        this.ntileIndexCombo.setSelectedItem(ntileIndex);

        this.ntileCombo.addItemListener(e -> {
            String item = (String) e.getItem();
            int ntileIndex1 = ContinuousInquiryPanel.this.ntileMap.get(item);

            for (int i = ContinuousInquiryPanel.this.ntileIndexCombo.getItemCount() - 1; i >= 0; i--) {
                ContinuousInquiryPanel.this.ntileIndexCombo.removeItemAt(i);
            }

            for (int n = 1; n <= ntileIndex1; n++) {
                ContinuousInquiryPanel.this.ntileIndexCombo.addItem(n);
            }

            double[] breakpoints = ContinuousInquiryPanel.getNtileBreakpoints(ContinuousInquiryPanel.this.data, getNtile());
            double breakpoint1 = breakpoints[getNtileIndex() - 1];
            double breakpoint2 = breakpoints[getNtileIndex()];
            ContinuousInquiryPanel.this.field1.setValue(breakpoint1);
            ContinuousInquiryPanel.this.field2.setValue(breakpoint2);
        });

        this.ntileIndexCombo.addItemListener(e -> {
            int ntile1 = getNtile();
            int ntileIndex12 = getNtileIndex();
            double[] breakpoints = ContinuousInquiryPanel.getNtileBreakpoints(ContinuousInquiryPanel.this.data, ntile1);
            double breakpoint1 = breakpoints[ntileIndex12 - 1];
            double breakpoint2 = breakpoints[ntileIndex12];
            ContinuousInquiryPanel.this.field1.setValue(breakpoint1);
            ContinuousInquiryPanel.this.field2.setValue(breakpoint2);
        });


        if (this.type == edu.cmu.tetradapp.editor.HistogramController.ContinuousConditioningPanel.Type.AboveAverage) {
            radio1.setSelected(true);
            this.field1.setValue(StatUtils.mean(this.data));
            this.field2.setValue(StatUtils.max(this.data));
        } else if (this.type == edu.cmu.tetradapp.editor.HistogramController.ContinuousConditioningPanel.Type.BelowAverage) {
            radio2.setSelected(true);
            this.field1.setValue(StatUtils.min(this.data));
            this.field2.setValue(StatUtils.mean(this.data));
        } else if (this.type == edu.cmu.tetradapp.editor.HistogramController.ContinuousConditioningPanel.Type.Ntile) {
            radio3.setSelected(true);
            double[] breakpoints = ContinuousInquiryPanel.getNtileBreakpoints(this.data, getNtile());
            double breakpoint1 = breakpoints[getNtileIndex() - 1];
            double breakpoint2 = breakpoints[getNtileIndex()];
            this.field1.setValue(breakpoint1);
            this.field2.setValue(breakpoint2);
        } else if (this.type == edu.cmu.tetradapp.editor.HistogramController.ContinuousConditioningPanel.Type.Range) {
            radio4.setSelected(true);
        }

        Box main = Box.createVerticalBox();

        Box b0 = Box.createHorizontalBox();
        b0.add(new JLabel("Condition on " + variable.getName() + " as:"));
        b0.add(Box.createHorizontalGlue());
        main.add(b0);
        main.add(Box.createVerticalStrut(10));

        Box b1 = Box.createHorizontalBox();
        b1.add(radio1);
        b1.add(new JLabel("Above average"));
        b1.add(Box.createHorizontalGlue());
        main.add(b1);

        Box b2 = Box.createHorizontalBox();
        b2.add(radio2);
        b2.add(new JLabel("Below average"));
        b2.add(Box.createHorizontalGlue());
        main.add(b2);

        Box b3 = Box.createHorizontalBox();
        b3.add(radio3);
        b3.add(new JLabel("In "));
        b3.add(this.ntileCombo);
        b3.add(this.ntileIndexCombo);
        b3.add(Box.createHorizontalGlue());
        main.add(b3);

        Box b4 = Box.createHorizontalBox();
        b4.add(radio4);
        b4.add(new JLabel("In ("));
        b4.add(this.field1);
        b4.add(new JLabel(", "));
        b4.add(this.field2);
        b4.add(new JLabel(")"));
        b4.add(Box.createHorizontalGlue());
        main.add(b4);

        add(main, BorderLayout.CENTER);
    }

    public edu.cmu.tetradapp.editor.HistogramController.ContinuousConditioningPanel.Type getType() {
        return this.type;
    }

    public double getLow() {
        return this.field1.getValue();
    }

    public double getHigh() {
        return this.field2.getValue();
    }

    public int getNtile() {
        String selectedItem = (String) this.ntileCombo.getSelectedItem();
        return this.ntileMap.get(selectedItem);
    }

    public int getNtileIndex() {
        Object selectedItem = this.ntileIndexCombo.getSelectedItem();
        return selectedItem == null ? 1 : (Integer) selectedItem;
    }

    /**
     * @return an array of breakpoints that divides the data into equal sized buckets,
     * including the min and max.
     */
    public static double[] getNtileBreakpoints(double[] data, int ntiles) {
        double[] _data = new double[data.length];
        System.arraycopy(data, 0, _data, 0, _data.length);

        // first sort the _data.
        Arrays.sort(_data);
        List<Chunk> chunks = new ArrayList<>(_data.length);
        int startChunkCount = 0;
        double lastValue = _data[0];

        for (int i = 0; i < _data.length; i++) {
            double value = _data[i];
            if (value != lastValue) {
                chunks.add(new Chunk(startChunkCount, i, value));
                startChunkCount = i;
            }
            lastValue = value;
        }

        chunks.add(new Chunk(startChunkCount, _data.length, _data[_data.length - 1]));

        // now find the breakpoints.
        double interval = _data.length / (double) ntiles;
        double[] breakpoints = new double[ntiles + 1];
        breakpoints[0] = StatUtils.min(_data);

        int current = 1;
        int freq = 0;

        for (Chunk chunk : chunks) {
            int valuesInChunk = chunk.getNumberOfValuesInChunk();
            int halfChunk = (int) (valuesInChunk * .5);

            // if more than half the values in the chunk fit this bucket then put here,
            // otherwise the chunk should be added to the next bucket.
            if (freq + halfChunk <= interval) {
                freq += valuesInChunk;
            } else {
                freq = valuesInChunk;
            }

            if (interval <= freq) {
                freq = 0;
                if (current < ntiles + 1) {
                    breakpoints[current++] = chunk.value;
                }
            }
        }

        for (int i = current; i < breakpoints.length; i++) {
            breakpoints[i] = StatUtils.max(_data);
        }

        return breakpoints;
    }

    /**
     * Represents a chunk of data in a sorted array of data.  If low == high then
     * then the chunk only contains one member.
     */
    private static class Chunk {

        private final int valuesInChunk;
        private final double value;

        public Chunk(int low, int high, double value) {
            this.valuesInChunk = (high - low);
            this.value = value;
        }

        public int getNumberOfValuesInChunk() {
            return this.valuesInChunk;
        }

    }
}

class DiscreteInquiryPanel extends JPanel {
    private final JComboBox valuesDropdown;

    public DiscreteInquiryPanel(DiscreteVariable var, edu.cmu.tetradapp.editor.HistogramController.DiscreteConditioningPanel panel) {
        this.valuesDropdown = new JComboBox();

        for (String category : var.getCategories()) {
            getValuesDropdown().addItem(category);
        }

        this.valuesDropdown.setSelectedItem(panel.getValue());

        Box main = Box.createVerticalBox();
        Box b1 = Box.createHorizontalBox();
        b1.add(new JLabel("Condition on:"));
        b1.add(Box.createHorizontalGlue());
        main.add(b1);
        main.add(Box.createVerticalStrut(10));

        Box b2 = Box.createHorizontalBox();
        b2.add(Box.createHorizontalStrut(10));
        b2.add(new JLabel(var.getName() + " = "));
        b2.add(getValuesDropdown());
        main.add(b2);

        add(main, BorderLayout.CENTER);
    }

    public JComboBox getValuesDropdown() {
        return this.valuesDropdown;
    }
}





