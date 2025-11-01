package com.dabomstew.pkrandom.pokemon.editors;

import com.dabomstew.pkrandom.log.ManualEditLogFormatter;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;
import java.util.Map;

/**
 * Simple dialog that displays a formatted summary of manual editor changes and allows saving the log to disk.
 */
public final class ManualEditLogDialog extends JDialog {

    private final JTextArea logArea;
    private final JButton saveButton;
    private final JButton closeButton;
    private final String suggestedFileName;

    private ManualEditLogDialog(Window owner, Map<String, List<String>> sections, String suggestedFileName) {
        super(owner, "Manual Editor Changes", ModalityType.APPLICATION_MODAL);
        this.suggestedFileName = suggestedFileName;

        logArea = new JTextArea(ManualEditLogFormatter.buildLogText(sections));
        logArea.setEditable(false);
        logArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        logArea.setLineWrap(false);

        JScrollPane scrollPane = new JScrollPane(logArea);
        scrollPane.setPreferredSize(new Dimension(700, 450));

        saveButton = new JButton("Save Log...");
        saveButton.addActionListener(this::onSave);

        closeButton = new JButton("Close");
        closeButton.addActionListener(e -> dispose());

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttonPanel.add(saveButton);
        buttonPanel.add(closeButton);

        setLayout(new BorderLayout(10, 10));
        add(scrollPane, BorderLayout.CENTER);
        add(buttonPanel, BorderLayout.SOUTH);

        pack();
        setLocationRelativeTo(owner);
    }

    private void onSave(ActionEvent e) {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Save Manual Change Log");
        if (suggestedFileName != null && !suggestedFileName.isEmpty()) {
            chooser.setSelectedFile(new File(suggestedFileName + "_editor_changes.log"));
        }
        int result = chooser.showSaveDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            File file = chooser.getSelectedFile();
            if (!file.getName().toLowerCase().endsWith(".log")) {
                file = new File(file.getParentFile(), file.getName() + ".log");
            }
            try {
                Files.write(file.toPath(), logArea.getText().getBytes(StandardCharsets.UTF_8));
                JOptionPane.showMessageDialog(this,
                        "Log saved to " + file.getAbsolutePath(),
                        "Log Saved",
                        JOptionPane.INFORMATION_MESSAGE);
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(this,
                        "Failed to save log:\n" + ex.getMessage(),
                        "Save Failed",
                        JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    public static void show(Window owner, Map<String, List<String>> sections, String suggestedFileName) {
        ManualEditLogDialog dialog = new ManualEditLogDialog(owner, sections, suggestedFileName);
        dialog.setVisible(true);
    }
}

