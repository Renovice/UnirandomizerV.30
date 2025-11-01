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
 * Main editor frame for Generation 4 Pokemon games.
 * Provides tabbed editors for Personal Data, TMs, Learnsets, Evolutions, Moves, and Sprites.
 */
public class Gen4EditorFrame extends JFrame {

    private final RomHandler romHandler;
    private JTabbedPane tabbedPane;

    // Editor panels
    private Gen4PersonalSheetPanel personalSheetPanel;
    private Gen4TMsSheetPanel tmsSheetPanel;
    private Gen4LearnsetsSheetPanel learnsetsSheetPanel;
    private Gen4EggMovesSheetPanel eggMovesSheetPanel;
    private Gen4EvolutionsSheetPanel evolutionsSheetPanel;
    private MovesSheetPanel movesSheetPanel;
    // TODO: Pokemon Sprites editor coming soon
    // private SpritesEditorPanel spritesEditorPanel;

    public Gen4EditorFrame(RomHandler romHandler) {
        this.romHandler = romHandler;
        initializeUI();
    }

    private void initializeUI() {
        setTitle("Gen 4 Pokemon Editor - " + romHandler.getROMName());
        setSize(1600, 900);
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
            }
        });

        // Create tabbed pane with better styling
        tabbedPane = new JTabbedPane();
        tabbedPane.setFont(new Font("Segoe UI", Font.BOLD, 12));
        tabbedPane.setBackground(Color.WHITE);
        tabbedPane.setForeground(Color.BLACK); // Black text for readability

        // Create Personal Sheet panel
        personalSheetPanel = new Gen4PersonalSheetPanel(romHandler);
        tabbedPane.addTab("Personal Sheet", personalSheetPanel);

        // Create TMs Sheet panel
        tmsSheetPanel = new Gen4TMsSheetPanel(romHandler);
        tabbedPane.addTab("TMs Sheet", tmsSheetPanel);

        // Create Learnsets Sheet panel
        learnsetsSheetPanel = new Gen4LearnsetsSheetPanel(romHandler);
        tabbedPane.addTab("Learnsets Sheet", learnsetsSheetPanel);

        eggMovesSheetPanel = new Gen4EggMovesSheetPanel(romHandler);
        tabbedPane.addTab("Egg Moves", eggMovesSheetPanel);

        // Create Evolutions Sheet panel
        evolutionsSheetPanel = new Gen4EvolutionsSheetPanel(romHandler);
        tabbedPane.addTab("Evolutions Sheet", evolutionsSheetPanel);

        // Create Moves Sheet panel
        movesSheetPanel = new MovesSheetPanel(romHandler);
        tabbedPane.addTab("Moves Sheet", movesSheetPanel);

        // TODO: Add Sprites editor
        // spritesEditorPanel = new SpritesEditorPanel(romHandler);
        // tabbedPane.addTab("Pokemon Sprites", spritesEditorPanel);

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
        // TODO: Save sprites when implemented

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
