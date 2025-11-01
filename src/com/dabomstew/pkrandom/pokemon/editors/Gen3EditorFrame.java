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
 * Main editor frame for Generation 3 Pokemon games.
 * Mirrors the Gen 4 editor layout for a consistent editing experience.
 */
public class Gen3EditorFrame extends JFrame {

    private final RomHandler romHandler;
    private JTabbedPane tabbedPane;

    // Editor panels reused from later generations
    private Gen3PersonalSheetPanel personalSheetPanel;
    private Gen3TMsSheetPanel tmsSheetPanel;
    private Gen3LearnsetsSheetPanel learnsetsSheetPanel;
    private Gen3EggMovesSheetPanel eggMovesSheetPanel;
    private Gen3EvolutionsSheetPanel evolutionsSheetPanel;
    private MovesSheetPanel movesSheetPanel;

    public Gen3EditorFrame(RomHandler romHandler) {
        this.romHandler = romHandler;
        initializeUI();
    }

    private void initializeUI() {
        setTitle("Gen 3 Pokemon Editor - " + romHandler.getROMName());
        setSize(1600, 900);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                personalSheetPanel.onWindowClosing();
                tmsSheetPanel.onWindowClosing();
                learnsetsSheetPanel.onWindowClosing();
                eggMovesSheetPanel.onWindowClosing();
                evolutionsSheetPanel.onWindowClosing();
                movesSheetPanel.onWindowClosing();
            }
        });

        tabbedPane = new JTabbedPane();
        tabbedPane.setFont(new Font("Segoe UI", Font.BOLD, 12));
        tabbedPane.setBackground(Color.WHITE);
        tabbedPane.setForeground(Color.BLACK);

        personalSheetPanel = new Gen3PersonalSheetPanel(romHandler);
        tabbedPane.addTab("Personal Sheet", personalSheetPanel);

        tmsSheetPanel = new Gen3TMsSheetPanel(romHandler);
        tabbedPane.addTab("TMs Sheet", tmsSheetPanel);

        learnsetsSheetPanel = new Gen3LearnsetsSheetPanel(romHandler);
        tabbedPane.addTab("Learnsets Sheet", learnsetsSheetPanel);

        eggMovesSheetPanel = new Gen3EggMovesSheetPanel(romHandler);
        tabbedPane.addTab("Egg Moves", eggMovesSheetPanel);

        evolutionsSheetPanel = new Gen3EvolutionsSheetPanel(romHandler);
        tabbedPane.addTab("Evolutions Sheet", evolutionsSheetPanel);

        movesSheetPanel = new MovesSheetPanel(romHandler);
        tabbedPane.addTab("Moves Sheet", movesSheetPanel);

        add(tabbedPane, BorderLayout.CENTER);

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
        personalSheetPanel.save();
        tmsSheetPanel.save();
    learnsetsSheetPanel.save();
    eggMovesSheetPanel.save();
        evolutionsSheetPanel.save();
        movesSheetPanel.save();

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
