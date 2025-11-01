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
 * Main editor frame for Generation 7 Pokémon games (Sun, Moon, Ultra Sun, Ultra
 * Moon).
 * Mirrors the Gen 6 editor styling while delegating to the shared sheet panels.
 */
public class Gen7EditorFrame extends JFrame {

    private final RomHandler romHandler;
    private JTabbedPane tabbedPane;

    private Gen7PersonalSheetPanel personalSheetPanel;
    private Gen7TMsSheetPanel tmsSheetPanel;
    private Gen7LearnsetsSheetPanel learnsetsSheetPanel;
    private Gen7EggMovesSheetPanel eggMovesSheetPanel;
    private Gen7EvolutionsSheetPanel evolutionsSheetPanel;
    private Gen7MovesSheetPanel movesSheetPanel;
    private Gen7MoveTutorsSheetPanel moveTutorsSheetPanel;

    public Gen7EditorFrame(RomHandler romHandler) {
        this.romHandler = romHandler;
        initializeUI();
    }

    private void initializeUI() {
        setTitle("Gen 7 Pokemon Editor - " + romHandler.getROMName());
        setSize(1200, 800);
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
                moveTutorsSheetPanel.onWindowClosing();
            }
        });

        tabbedPane = new JTabbedPane();
        tabbedPane.setFont(new Font("Segoe UI", Font.BOLD, 12));
        tabbedPane.setBackground(Color.WHITE);
        tabbedPane.setForeground(Color.BLACK);

        personalSheetPanel = new Gen7PersonalSheetPanel(romHandler);
        tabbedPane.addTab("Personal Sheet", personalSheetPanel);

        tmsSheetPanel = new Gen7TMsSheetPanel(romHandler);
        tabbedPane.addTab("TMs Sheet", tmsSheetPanel);

        learnsetsSheetPanel = new Gen7LearnsetsSheetPanel(romHandler);
        tabbedPane.addTab("Learnsets Sheet", learnsetsSheetPanel);

        eggMovesSheetPanel = new Gen7EggMovesSheetPanel(romHandler);
        tabbedPane.addTab("Egg Moves", eggMovesSheetPanel);

        evolutionsSheetPanel = new Gen7EvolutionsSheetPanel(romHandler);
        tabbedPane.addTab("Evolutions Sheet", evolutionsSheetPanel);

        movesSheetPanel = new Gen7MovesSheetPanel(romHandler);
        tabbedPane.addTab("Moves Sheet", movesSheetPanel);

        moveTutorsSheetPanel = new Gen7MoveTutorsSheetPanel(romHandler);
        tabbedPane.addTab("Move Tutors", moveTutorsSheetPanel);

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
