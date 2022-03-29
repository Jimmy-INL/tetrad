/*
 * Copyright (C) 2017 University of Pittsburgh.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA 02110-1301  USA
 */
package edu.cmu.tetradapp.editor;

import edu.cmu.tetrad.algcomparison.algorithm.Algorithm;
import edu.cmu.tetrad.algcomparison.algorithm.oracle.pag.RfciBsc;
import edu.cmu.tetrad.algcomparison.utils.TakesIndependenceWrapper;
import edu.cmu.tetrad.algcomparison.utils.UsesScoreWrapper;
import edu.cmu.tetrad.annotation.Score;
import edu.cmu.tetrad.annotation.TestOfIndependence;
import edu.cmu.tetrad.util.ParamDescription;
import edu.cmu.tetrad.util.ParamDescriptions;
import edu.cmu.tetrad.util.Parameters;
import edu.cmu.tetrad.util.Params;
import edu.cmu.tetradapp.model.GeneralAlgorithmRunner;
import edu.cmu.tetradapp.ui.PaddingPanel;
import edu.cmu.tetradapp.util.DoubleTextField;
import edu.cmu.tetradapp.util.IntTextField;
import edu.cmu.tetradapp.util.StringTextField;

import javax.swing.*;
import java.awt.*;
import java.text.DecimalFormat;
import java.util.List;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Dec 4, 2017 5:05:42 PM
 *
 * @author Kevin V. Bui (kvb2@pitt.edu)
 */
public class AlgorithmParameterPanel extends JPanel {

    private static final long serialVersionUID = 274638263704283474L;

    protected static final String DEFAULT_TITLE_BORDER = "Algorithm Parameters";

    protected final JPanel mainPanel = new JPanel();

    public AlgorithmParameterPanel() {
        initComponents();
    }

    private void initComponents() {
        this.mainPanel.setLayout(new BoxLayout(this.mainPanel, BoxLayout.Y_AXIS));

        setLayout(new BorderLayout());
        add(this.mainPanel, BorderLayout.NORTH);
    }

    public void addToPanel(final GeneralAlgorithmRunner algorithmRunner) {
        this.mainPanel.removeAll();

        final Algorithm algorithm = algorithmRunner.getAlgorithm();
        final Parameters parameters = algorithmRunner.getParameters();

        // Hard-coded parameter groups for Rfci-Bsc
        if (algorithm instanceof RfciBsc) {
            // Phase one: PAG and constraints candidates Searching
            String title = algorithm
                    .getClass().getAnnotation(edu.cmu.tetrad.annotation.Algorithm.class).name();
            Set<String> params = new LinkedHashSet<>();
            // RFCI
            params.add(Params.DEPTH);
            params.add(Params.MAX_PATH_LENGTH);
            params.add(Params.COMPLETE_RULE_SET_USED);
            params.add(Params.VERBOSE);
            this.mainPanel.add(createSubPanel(title, params, parameters));
            this.mainPanel.add(Box.createVerticalStrut(10));

            // Stage one: PAG and constraints candidates Searching
            title = "Stage One: PAG and constraints candidates Searching";
            params = new LinkedHashSet<>();
            // Thresholds
            params.add(Params.NUM_RANDOMIZED_SEARCH_MODELS);
            //params.add(Params.THRESHOLD_NO_RANDOM_DATA_SEARCH);
            //params.add(Params.CUTOFF_DATA_SEARCH);
            this.mainPanel.add(createSubPanel(title, params, parameters));
            this.mainPanel.add(Box.createVerticalStrut(10));

            // Stage two: Bayesian Scoring of Constraints
            title = "Stage Two: Bayesian Scoring of Constraints";
            params = new LinkedHashSet<>();
            params.add(Params.NUM_BSC_BOOTSTRAP_SAMPLES);
            params.add(Params.THRESHOLD_NO_RANDOM_CONSTRAIN_SEARCH);
            //params.add(Params.CUTOFF_CONSTRAIN_SEARCH);
            params.add(Params.LOWER_BOUND);
            params.add(Params.UPPER_BOUND);
            params.add(Params.OUTPUT_RBD);
            this.mainPanel.add(createSubPanel(title, params, parameters));
            this.mainPanel.add(Box.createVerticalStrut(10));

        } else {
            // add algorithm parameters
            Set<String> params = Params.getAlgorithmParameters(algorithm);

            if (!params.isEmpty()) {
                final String title = algorithm
                        .getClass().getAnnotation(edu.cmu.tetrad.annotation.Algorithm.class).name();
                this.mainPanel.add(createSubPanel(title, params, parameters));
                this.mainPanel.add(Box.createVerticalStrut(10));
            }

            params = Params.getScoreParameters(algorithm);
            if (!params.isEmpty()) {
                final String title = ((UsesScoreWrapper) algorithm).getScoreWrapper()
                        .getClass().getAnnotation(Score.class).name();
                this.mainPanel.add(createSubPanel(title, params, parameters));
                this.mainPanel.add(Box.createVerticalStrut(10));
            }

            params = Params.getTestParameters(algorithm);
            if (!params.isEmpty()) {
                final String title = ((TakesIndependenceWrapper) algorithm).getIndependenceWrapper()
                        .getClass().getAnnotation(TestOfIndependence.class).name();
                this.mainPanel.add(createSubPanel(title, params, parameters));
                this.mainPanel.add(Box.createVerticalStrut(10));
            }

            if (algorithmRunner.getSourceGraph() == null) {
                params = Params.getBootstrappingParameters(algorithm);
                if (!params.isEmpty()) {
                    this.mainPanel.add(createSubPanel("Bootstrapping", params, parameters));
                    this.mainPanel.add(Box.createVerticalStrut(10));
                }
            }
        }

    }

    protected Box[] toArray(final Map<String, Box> parameterComponents) {
        final ParamDescriptions paramDescs = ParamDescriptions.getInstance();

        final List<Box> boolComps = new LinkedList<>();
        final List<Box> otherComps = new LinkedList<>();
        parameterComponents.forEach((k, v) -> {
            if (paramDescs.get(k).getDefaultValue() instanceof Boolean) {
                boolComps.add(v);
            } else {
                otherComps.add(v);
            }
        });

        return Stream.concat(otherComps.stream(), boolComps.stream())
                .toArray(Box[]::new);
    }

    protected Map<String, Box> createParameterComponents(final Set<String> params, final Parameters parameters) {
        final ParamDescriptions paramDescs = ParamDescriptions.getInstance();
        return params.stream()
                .collect(Collectors.toMap(
                        Function.identity(),
                        e -> createParameterComponent(e, parameters, paramDescs.get(e)),
                        (u, v) -> {
                            throw new IllegalStateException(String.format("Duplicate key %s.", u));
                        },
                        TreeMap::new));
    }

    protected Box createParameterComponent(final String parameter, final Parameters parameters, final ParamDescription paramDesc) {
        final JComponent component;
        final Object defaultValue = paramDesc.getDefaultValue();
        if (defaultValue instanceof Double) {
            final double lowerBoundDouble = paramDesc.getLowerBoundDouble();
            final double upperBoundDouble = paramDesc.getUpperBoundDouble();
            component = getDoubleField(parameter, parameters, (Double) defaultValue, lowerBoundDouble, upperBoundDouble);
        } else if (defaultValue instanceof Integer) {
            final int lowerBoundInt = paramDesc.getLowerBoundInt();
            final int upperBoundInt = paramDesc.getUpperBoundInt();
            component = getIntTextField(parameter, parameters, (Integer) defaultValue, lowerBoundInt, upperBoundInt);
        } else if (defaultValue instanceof Boolean) {
            component = getBooleanSelectionBox(parameter, parameters, (Boolean) defaultValue);
        } else if (defaultValue instanceof String) {
            component = getStringField(parameter, parameters, (String) defaultValue);
        } else {
            throw new IllegalArgumentException("Unexpected type: " + defaultValue.getClass());
        }

        final Box paramRow = Box.createHorizontalBox();

        final JLabel paramLabel = new JLabel(paramDesc.getShortDescription());
        final String longDescription = paramDesc.getLongDescription();
        if (longDescription != null) {
            paramLabel.setToolTipText(longDescription);
        }
        paramRow.add(paramLabel);
        paramRow.add(Box.createHorizontalGlue());
        paramRow.add(component);

        return paramRow;
    }

    protected JPanel createSubPanel(final String title, final Set<String> params, final Parameters parameters) {
        final JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createTitledBorder(title));

        final Box paramsBox = Box.createVerticalBox();

        final Box[] boxes = toArray(createParameterComponents(params, parameters));
        final int lastIndex = boxes.length - 1;
        for (int i = 0; i < lastIndex; i++) {
            paramsBox.add(boxes[i]);
            paramsBox.add(Box.createVerticalStrut(10));
        }
        paramsBox.add(boxes[lastIndex]);

        panel.add(new PaddingPanel(paramsBox), BorderLayout.CENTER);

        return panel;
    }

    protected DoubleTextField getDoubleField(final String parameter, final Parameters parameters,
                                             final double defaultValue, final double lowerBound, final double upperBound) {
        final DoubleTextField field = new DoubleTextField(parameters.getDouble(parameter, defaultValue),
                8, new DecimalFormat("0.####"), new DecimalFormat("0.0#E0"), 0.001);

        field.setFilter((value, oldValue) -> {
            if (value == field.getValue()) {
                return oldValue;
            }

            if (value < lowerBound) {
                return oldValue;
            }

            if (value > upperBound) {
                return oldValue;
            }

            try {
                parameters.set(parameter, value);
            } catch (final Exception e) {
                // Ignore.
            }

            return value;
        });

        return field;
    }

    protected IntTextField getIntTextField(final String parameter, final Parameters parameters,
                                           final int defaultValue, final double lowerBound, final double upperBound) {
        final IntTextField field = new IntTextField(parameters.getInt(parameter, defaultValue), 8);

        field.setFilter((value, oldValue) -> {
            if (value == field.getValue()) {
                return oldValue;
            }

            if (value < lowerBound) {
                return oldValue;
            }

            if (value > upperBound) {
                return oldValue;
            }

            try {
                parameters.set(parameter, value);
            } catch (final Exception e) {
                // Ignore.
            }

            return value;
        });

        return field;
    }

    // Joe's old implementation with dropdown yes or no
    protected JComboBox getBooleanBox(final String parameter, final Parameters parameters, final boolean defaultValue) {
        final JComboBox<String> box = new JComboBox<>(new String[]{"Yes", "No"});

        final boolean aBoolean = parameters.getBoolean(parameter, defaultValue);
        if (aBoolean) {
            box.setSelectedItem("Yes");
        } else {
            box.setSelectedItem("No");
        }

        box.addActionListener((e) -> {
            if (((JComboBox) e.getSource()).getSelectedItem().equals("Yes")) {
                parameters.set(parameter, true);
            } else {
                parameters.set(parameter, false);
            }
        });

        box.setMaximumSize(box.getPreferredSize());

        return box;
    }

    // Zhou's new implementation with yes/no radio buttons
    protected Box getBooleanSelectionBox(final String parameter, final Parameters parameters, final boolean defaultValue) {
        final Box selectionBox = Box.createHorizontalBox();

        final JRadioButton yesButton = new JRadioButton("Yes");
        final JRadioButton noButton = new JRadioButton("No");

        // Button group to ensure only only one option can be selected
        final ButtonGroup selectionBtnGrp = new ButtonGroup();
        selectionBtnGrp.add(yesButton);
        selectionBtnGrp.add(noButton);

        final boolean aBoolean = parameters.getBoolean(parameter, defaultValue);

        // Set default selection
        if (aBoolean) {
            yesButton.setSelected(true);
        } else {
            noButton.setSelected(true);
        }

        // Add to containing box
        selectionBox.add(yesButton);
        selectionBox.add(noButton);

        // Event listener
        yesButton.addActionListener((e) -> {
            final JRadioButton button = (JRadioButton) e.getSource();
            if (button.isSelected()) {
                parameters.set(parameter, true);
            }
        });

        // Event listener
        noButton.addActionListener((e) -> {
            final JRadioButton button = (JRadioButton) e.getSource();
            if (button.isSelected()) {
                parameters.set(parameter, false);
            }
        });

        return selectionBox;
    }

    protected StringTextField getStringField(final String parameter, final Parameters parameters, final String defaultValue) {
        final StringTextField field = new StringTextField(parameters.getString(parameter, defaultValue), 20);

        field.setFilter((value, oldValue) -> {
            if (value.equals(field.getValue().trim())) {
                return oldValue;
            }

            try {
                parameters.set(parameter, value);
            } catch (final Exception e) {
                // Ignore.
            }

            return value;
        });

        return field;
    }

}
