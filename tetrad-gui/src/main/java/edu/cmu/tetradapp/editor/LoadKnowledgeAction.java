///////////////////////////////////////////////////////////////////////////////
// For information as to what this class does, see the Javadoc, below.       //
// Copyright (C) 1998, 1999, 2000, 2001, 2002, 2003, 2004, 2005, 2006,       //
// 2007, 2008, 2009, 2010, 2014, 2015 by Peter Spirtes, Richard Scheines, Joseph   //
// Ramsey, and Clark Glymour.                                                //
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

import edu.cmu.tetrad.data.DataUtils;
import edu.cmu.tetrad.data.DelimiterType;
import edu.cmu.tetrad.data.IKnowledge;
import edu.cmu.tetrad.util.JOptionUtils;
import edu.cmu.tetradapp.model.KnowledgeEditable;
import edu.cmu.tetradapp.util.StringTextField;

import javax.swing.*;
import javax.swing.border.MatteBorder;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.prefs.Preferences;

/**
 * Loads knowledge.
 *
 * @author Joseph Ramsey jdramsey@andrew.cmu.edu
 */
final class LoadKnowledgeAction extends AbstractAction {

    /**
     * The component that file choosers will be centered on.
     */
    private final KnowledgeEditable knowledgeEditable;

    /**
     * Comment indicator for the file.
     */
    private String commentIndicator = "//";

    /**
     * Delimiters for the file.
     */
    private String delimiters = "\t";

    /**
     * Creates a new load data action for the given knowledgeEditable.
     *
     * @param knowledgeEditable The component to center the wizard on.
     */
    public LoadKnowledgeAction(final KnowledgeEditable knowledgeEditable) {
        super("Load Knowledge...");

        if (knowledgeEditable == null) {
            throw new NullPointerException();
        }

        this.knowledgeEditable = knowledgeEditable;
    }

    private static JFileChooser getJFileChooser() {
        final JFileChooser chooser = new JFileChooser();
        final String sessionSaveLocation = Preferences.userRoot().get(
                "fileSaveLocation", Preferences.userRoot().absolutePath());
        chooser.setCurrentDirectory(new File(sessionSaveLocation));
        chooser.resetChoosableFileFilters();
        chooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
        return chooser;
    }

    /**
     * Performs the action of loading a session from a file.
     */
    public void actionPerformed(final ActionEvent e) {
        int ret = 1;

        while (ret == 1) {
            final JFileChooser chooser = getJFileChooser();
            chooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);

            final Component comp =
                    (this.knowledgeEditable instanceof Component) ? (Component) this.knowledgeEditable : null;

            chooser.showOpenDialog(comp);

            final File file = chooser.getSelectedFile();

            if (file != null) {
                Preferences.userRoot().put("fileSaveLocation", file.getParent());
            }

            final KnowledgeLoaderWizard wizard =
                    new KnowledgeLoaderWizard(file, this.knowledgeEditable);
            wizard.setCommentIndicator(this.commentIndicator);

            ret = JOptionPane.showOptionDialog(null, wizard,
                    "Knowledge Import Wizard", JOptionPane.OK_CANCEL_OPTION,
                    JOptionPane.PLAIN_MESSAGE, null, new String[]{"Cancel",
                            "Select Another File", "Import Data"}, "Import Data");

            this.delimiters = wizard.getDelimiters();
            this.commentIndicator = wizard.getCommentIndicator();

            // Import...
            if (ret == JOptionPane.OK_OPTION) {
                try {
                    final IKnowledge knowledge = DataUtils.loadKnowledge(file, DelimiterType.WHITESPACE,
                            "//");
                    this.knowledgeEditable.setKnowledge(knowledge);
                } catch (final Exception e1) {
                    String message = e1.getMessage() ==
                            null ? e1.getClass().getName() : e1.getMessage();

                    if ("".equals(message)) {
                        message = "Could not load knowledge.";
                    }

                    JOptionPane.showMessageDialog(JOptionUtils.centeringComp(),
                            message);
                }
            }
        }
    }

    public String getDelimiters() {
        return this.delimiters;
    }
}

final class KnowledgeLoaderWizard extends JPanel {
    private final JTextArea fileTextArea;
    private String delimiters = " \t";
    private String commentIndicator = "//";

    public KnowledgeLoaderWizard(final File file,
                                 final KnowledgeEditable knowledgeEditable) {
        if (file == null) {
            throw new NullPointerException();
        }

        if (knowledgeEditable == null) {
            throw new NullPointerException();
        }

        setBorder(new MatteBorder(10, 10, 10, 10, getBackground()));
        setLayout(new BorderLayout());

        final JTextArea sampleTextArea = new JTextArea("???");
        sampleTextArea.setEditable(false);
        sampleTextArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        sampleTextArea.setTabSize(6);
        final JScrollPane sampleScroll = new JScrollPane(sampleTextArea);
        sampleScroll.setPreferredSize(new Dimension(200, 300));
        sampleScroll.setBorder(new TitledBorder("Prototype"));

        this.fileTextArea = new JTextArea();
        this.fileTextArea.setEditable(false);
        this.fileTextArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        setText(file, this.fileTextArea);
        final JScrollPane fileScroll = new JScrollPane(this.fileTextArea);
        fileScroll.setPreferredSize(new Dimension(400, 300));
        fileScroll.setBorder(new TitledBorder("File You Selected"));

        sampleTextArea.setText(knowledgeSampleText());
        sampleTextArea.setCaretPosition(0);

        final JComboBox delimiterBox =
                new JComboBox(new String[]{"Whitespace", "Tab", "Comma"});
        delimiterBox.setMaximumSize(delimiterBox.getPreferredSize());
        delimiterBox.addActionListener(new ActionListener() {
            public void actionPerformed(final ActionEvent e) {
                final JComboBox box = (JComboBox) e.getSource();
                final String choice = (String) box.getSelectedItem();

                if ("Whitespace".equals(choice)) {
                    KnowledgeLoaderWizard.this.delimiters = " \t";
                } else if ("Tab".equals(choice)) {
                    KnowledgeLoaderWizard.this.delimiters = "\t";
                } else if ("Comma".equals(choice)) {
                    KnowledgeLoaderWizard.this.delimiters = "\t";
                }
            }
        });

        final StringTextField commentIndicatorField = new StringTextField(getCommentIndicator(), 4);
        commentIndicatorField.setFilter(new StringTextField.Filter() {
            public String filter(final String value, final String oldValue) {
                setCommentIndicator(value);
                return value;
            }
        });

        commentIndicatorField.setFont(new Font("Monospaced", Font.PLAIN, 12));

        final Box b1 = Box.createHorizontalBox();
        b1.add(Box.createHorizontalGlue());
        b1.add(new JLabel("Delimiter: "));
        b1.add(delimiterBox);
        b1.add(Box.createHorizontalStrut(5));
        b1.add(new JLabel("Comment Indicator: "));
        b1.add(commentIndicatorField);

        final Box b2 = Box.createHorizontalBox();

        final JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
                sampleScroll, fileScroll);

        b2.add(splitPane);

        final Box b3 = Box.createVerticalBox();
        b3.add(b1);
        b3.add(Box.createVerticalStrut(10));
        b3.add(b2);
        add(b3, BorderLayout.CENTER);
    }

    private static String knowledgeSampleText() {
        return "/knowledge" + "\n0 x1 x2" + "\n1 x3 x4" + "\n4 x5";
    }

    private static void setText(final File file, final JTextArea textArea) {
        final int numLines = 40;
        final int numCols = 100;

        try {
            final BufferedReader in = new BufferedReader(new FileReader(file));
            String line;
            int lineNumber = 0;

            while ((line = in.readLine()) != null && (++lineNumber < numLines)) {
                if (line.length() < numCols) {
                    textArea.append(line.substring(0, line.length()) + "\n");
                } else {
                    textArea.append(line.substring(0, numCols) + "...\n");
                }
            }

            textArea.append("...");
            textArea.setCaretPosition(0);

            in.close();
        } catch (final IOException e) {
            e.printStackTrace();
        }
    }

    public String getText() {
        return this.fileTextArea.getText();
    }

    public String getCommentIndicator() {
        return this.commentIndicator;
    }

    public void setCommentIndicator(final String commentIndicator) {
        if (commentIndicator == null) {
            throw new NullPointerException();
        }
        this.commentIndicator = commentIndicator;
    }

    public String getDelimiters() {
        return this.delimiters;
    }
}






