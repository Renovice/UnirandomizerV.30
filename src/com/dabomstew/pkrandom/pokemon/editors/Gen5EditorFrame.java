package com.dabomstew.pkrandom.pokemon.editors;

import com.dabomstew.pkrandom.log.ManualEditRegistry;
import com.dabomstew.pkromio.romhandlers.RomHandler;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.List;
import java.util.Map;

/**
 * Main editor frame for Generation 5 Pokemon games.
 * Provides tabbed editors for Personal Data, TMs, Learnsets, Evolutions, and Moves.
 * 1:1 copy of Gen4EditorFrame structure.
 */
public class Gen5EditorFrame extends JFrame {

    private final RomHandler romHandler;
    private JTabbedPane tabbedPane;

    // Editor panels
    private Gen5PersonalSheetPanel personalSheetPanel;
    private Gen5TMsSheetPanel tmsSheetPanel;
    private Gen5LearnsetsSheetPanel learnsetsSheetPanel;
    private Gen5EvolutionsSheetPanel evolutionsSheetPanel;
    private Gen5MovesSheetPanel movesSheetPanel;
    private Gen5MoveTutorsSheetPanel moveTutorsSheetPanel;
    private Gen5EggMovesSheetPanel eggMovesSheetPanel;

    public Gen5EditorFrame(RomHandler romHandler) {
        this.romHandler = romHandler;
        initializeUI();
    }

    private void initializeUI() {
        setTitle("Gen 5 Pokemon Editor - " + romHandler.getROMName());
        setSize(1200, 800);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        // Add window listener to restore unsaved changes when window closes
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                // Restore any unsaved changes in all panels
                personalSheetPanel.onWindowClosing();
                tmsSheetPanel.onWindowClosing();
                learnsetsSheetPanel.onWindowClosing();
                eggMovesSheetPanel.onWindowClosing();
                evolutionsSheetPanel.onWindowClosing();
                movesSheetPanel.onWindowClosing();
                moveTutorsSheetPanel.onWindowClosing();
            }
        });

        // Create tabbed pane with better styling
        tabbedPane = new JTabbedPane();
        tabbedPane.setFont(new Font("Segoe UI", Font.BOLD, 12));
        tabbedPane.setBackground(Color.WHITE);
        tabbedPane.setForeground(Color.BLACK); // Black text for readability

        // Create Personal Sheet panel
        personalSheetPanel = new Gen5PersonalSheetPanel(romHandler);
        tabbedPane.addTab("Personal Sheet", personalSheetPanel);

        // Create TMs Sheet panel
        tmsSheetPanel = new Gen5TMsSheetPanel(romHandler);
        tabbedPane.addTab("TMs Sheet", tmsSheetPanel);

        // Create Learnsets Sheet panel
        learnsetsSheetPanel = new Gen5LearnsetsSheetPanel(romHandler);
        tabbedPane.addTab("Learnsets Sheet", learnsetsSheetPanel);

        // Create Egg Moves panel
        eggMovesSheetPanel = new Gen5EggMovesSheetPanel(romHandler);
        tabbedPane.addTab("Egg Moves", eggMovesSheetPanel);

        // Create Evolutions Sheet panel
        evolutionsSheetPanel = new Gen5EvolutionsSheetPanel(romHandler);
        tabbedPane.addTab("Evolutions Sheet", evolutionsSheetPanel);

        // Create Moves Sheet panel
        movesSheetPanel = new Gen5MovesSheetPanel(romHandler);
        tabbedPane.addTab("Moves Sheet", movesSheetPanel);

        // Create Move Tutors panel
        moveTutorsSheetPanel = new Gen5MoveTutorsSheetPanel(romHandler);
        tabbedPane.addTab("Move Tutors", moveTutorsSheetPanel);

        add(tabbedPane, BorderLayout.CENTER);

        // Add menu bar
        createMenuBar();
    }

    private void createMenuBar() {
        JMenuBar menuBar = new JMenuBar();

        JMenu fileMenu = new JMenu("File");
        JMenuItem saveAllItem = new JMenuItem("Save All");
        saveAllItem.addActionListener(e -> saveAll());
        JMenuItem closeItem = new JMenuItem("Close");
        closeItem.addActionListener(e -> dispose());

        fileMenu.add(saveAllItem);
        fileMenu.addSeparator();
        fileMenu.add(closeItem);

        menuBar.add(fileMenu);
        setJMenuBar(menuBar);
    }

    private void saveAll() {
        // Save all panels
        personalSheetPanel.save();
        tmsSheetPanel.save();
        learnsetsSheetPanel.save();
        eggMovesSheetPanel.save();
        evolutionsSheetPanel.save();
        movesSheetPanel.save();
        moveTutorsSheetPanel.save();

        Map<String, List<String>> manualSections = ManualEditRegistry.getInstance().snapshot();
        if (!manualSections.isEmpty()) {
            ManualEditLogDialog.show(this, manualSections, romHandler.getROMName());
        }

        JOptionPane.showMessageDialog(this,
                "All changes saved successfully!",
                "Save Complete",
                JOptionPane.INFORMATION_MESSAGE);
    }
}
