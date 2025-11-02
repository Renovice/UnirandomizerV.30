package com.dabomstew.pkrandom.pokemon.editors;

import com.dabomstew.pkrandom.log.ManualEditRegistry;
import com.dabomstew.pkromio.constants.MoveIDs;
import com.dabomstew.pkromio.gamedata.*;
import com.dabomstew.pkromio.romhandlers.RomHandler;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Learnsets Sheet editor showing level-up moves for each Pokemon
 * Displays moves learned at each level in a spreadsheet format
 */
public class LearnsetsSheetPanel extends JPanel {

    private final RomHandler romHandler;
    private final List<Species> pokemonList;
    private final List<Move> moveList;
    private final Map<Integer, List<MoveLearnt>> movesLearnt;
    private final int maxMovesPerPokemon = 20; // Display up to 20 move slots

    private JTable frozenTable;
    private JTable mainTable;
    private LearnsetsTableModel tableModel;
    private boolean copyPasteModeEnabled = false;
    private final EditorUtils.FindState findState = new EditorUtils.FindState();
    private final PokemonIconCache iconCache;
    private final Map<Integer, List<MoveLearnt>> movesBackup = new HashMap<>();

    public LearnsetsSheetPanel(RomHandler romHandler) {
        this.romHandler = romHandler;
        this.pokemonList = romHandler.getSpecies();
        this.moveList = romHandler.getMoves();
        this.movesLearnt = romHandler.getMovesLearnt();
        this.iconCache = PokemonIconCache.get(romHandler);
        initializeUI();
        createBackup();
    }

    private void initializeUI() {
        setLayout(new BorderLayout());
        setBackground(Color.WHITE);

        // Create styled toolbar
        add(createStyledToolbar(), BorderLayout.NORTH);

        // Create frozen column table
        JPanel tablePanel = createFrozenColumnTable();
        add(tablePanel, BorderLayout.CENTER);
    }

    private JPanel createStyledToolbar() {
        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 8));
        toolbar.setBackground(new Color(250, 250, 250));
        toolbar.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(200, 200, 200)),
                new EmptyBorder(5, 5, 5, 5)));

        JButton saveButton = EditorUtils.createStyledButton("Save", new Color(76, 175, 80));
        saveButton.addActionListener(e -> save());

        JButton reloadButton = EditorUtils.createStyledButton("Reload", new Color(96, 96, 96));
        reloadButton.addActionListener(e -> reload());

        JButton exportButton = EditorUtils.createStyledButton("Export CSV", new Color(33, 150, 243));
        exportButton.addActionListener(e -> exportToCSV());

        JButton importButton = EditorUtils.createStyledButton("Import CSV", new Color(0, 188, 212));
        importButton.addActionListener(e -> importFromCSV());

        JButton addMoveButton = EditorUtils.createStyledButton("Add Move", new Color(63, 81, 181));
        addMoveButton.addActionListener(e -> addMoveSlot());

        JButton removeMoveButton = EditorUtils.createStyledButton("Remove Move", new Color(229, 57, 53));
        removeMoveButton.addActionListener(e -> removeMoveSlot());

        JButton fillSlotsButton = EditorUtils.createStyledButton("Fill Slots", new Color(142, 36, 170));
        fillSlotsButton.addActionListener(e -> fillAllMoveSlots());

        JToggleButton copyPasteButton = EditorUtils.createStyledToggleButton("Copy/Paste Mode", new Color(255, 152, 0));
        copyPasteButton.addActionListener(e -> toggleCopyPasteMode(copyPasteButton.isSelected()));

        JButton findButton = EditorUtils.createStyledButton("Find", new Color(0, 150, 136));
        findButton.addActionListener(e -> showFindDialog());

        toolbar.add(saveButton);
        toolbar.add(reloadButton);
        toolbar.add(exportButton);
        toolbar.add(importButton);
        toolbar.add(Box.createHorizontalStrut(5));
        toolbar.add(addMoveButton);
        toolbar.add(removeMoveButton);
        toolbar.add(fillSlotsButton);
        toolbar.add(Box.createHorizontalStrut(5));
        toolbar.add(copyPasteButton);
        toolbar.add(findButton);
        toolbar.add(Box.createHorizontalStrut(10));

        JLabel infoLabel = new JLabel("Level-up Movesets - Edit moves and levels");
        infoLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        infoLabel.setForeground(new Color(100, 100, 100));
        toolbar.add(infoLabel);

        return toolbar;
    }

    private JPanel createFrozenColumnTable() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(Color.WHITE);

        // Create table model
        tableModel = new LearnsetsTableModel(pokemonList, moveList, movesLearnt, maxMovesPerPokemon);

        // Create frozen table (ID and Name columns)
        TableModel frozenModel = new AbstractTableModel() {
            public int getRowCount() {
                return tableModel.getRowCount();
            }

            public int getColumnCount() {
                return 2;
            }

            public Object getValueAt(int row, int col) {
                return tableModel.getValueAt(row, col);
            }

            public String getColumnName(int col) {
                return tableModel.getColumnName(col);
            }

            public Class<?> getColumnClass(int col) {
                return tableModel.getColumnClass(col);
            }
        };

        frozenTable = new JTable(frozenModel) {
            @Override
            public void scrollRectToVisible(java.awt.Rectangle aRect) {
                // Disable horizontal scrolling for frozen table
                aRect.x = 0;
                super.scrollRectToVisible(aRect);
            }

            @Override
            public void changeSelection(int rowIndex, int columnIndex, boolean toggle, boolean extend) {
                super.changeSelection(rowIndex, columnIndex, toggle, extend);
                if (rowIndex >= 0 && EditorUtils.isFullRowSelectionActive(this) && getColumnCount() > 0) {
                    getColumnModel().getSelectionModel().setSelectionInterval(0, getColumnCount() - 1);
                    if (mainTable != null) {
                        EditorUtils.runWithFrozenSyncSuppressed(mainTable, () -> {
                            if (mainTable.getColumnCount() > 0) {
                                mainTable.setColumnSelectionInterval(0, mainTable.getColumnCount() - 1);
                            } else {
                                mainTable.getColumnModel().getSelectionModel().clearSelection();
                            }
                        });
                    }
                }
            }
        };
        styleTable(frozenTable, true);
        TableLayoutDefaults.configureFrozenColumns(frozenTable.getColumnModel(), iconCache.hasIcons());
        TableLayoutDefaults.applyRowHeight(frozenTable, iconCache.hasIcons());

        if (iconCache.hasIcons()) {
            frozenTable.getColumnModel().getColumn(1).setCellRenderer(new FrozenNameRenderer());
        }

        // Add mouse listener for ID column row selection behavior
        java.awt.event.MouseAdapter frozenRowSelector = new java.awt.event.MouseAdapter() {
            private int lastClickedRow = -1;
            private boolean draggingFullRow = false;

            @Override
            public void mousePressed(java.awt.event.MouseEvent e) {
                int row = frozenTable.rowAtPoint(e.getPoint());
                int col = frozenTable.columnAtPoint(e.getPoint());

                // Only intercept clicks on ID column (column 0)
                if (col == 0 && row >= 0) {
                    draggingFullRow = true;
                    // Toggle entire row selection
                    if (lastClickedRow == row && frozenTable.getSelectedRow() == row) {
                        // Clicking same ID again - deselect
                        EditorUtils.setFullRowSelectionActive(frozenTable, false);
                        frozenTable.clearSelection();
                        frozenTable.getColumnModel().getSelectionModel().clearSelection();
                        if (mainTable != null) {
                            EditorUtils.runWithFrozenSyncSuppressed(mainTable, () -> {
                                mainTable.clearSelection();
                                mainTable.getColumnModel().getSelectionModel().clearSelection();
                            });
                        }
                        lastClickedRow = -1;
                    } else {
                        // Select entire row
                        EditorUtils.setFullRowSelectionActive(frozenTable, true);
                        frozenTable.setRowSelectionInterval(row, row);
                        if (frozenTable.getColumnCount() > 0) {
                            frozenTable.setColumnSelectionInterval(0, frozenTable.getColumnCount() - 1);
                        }
                        if (mainTable != null) {
                            EditorUtils.runWithFrozenSyncSuppressed(mainTable, () -> {
                                mainTable.setRowSelectionInterval(row, row);
                                if (mainTable.getColumnCount() > 0) {
                                    mainTable.setColumnSelectionInterval(0, mainTable.getColumnCount() - 1);
                                } else {
                                    mainTable.getColumnModel().getSelectionModel().clearSelection();
                                }
                            });
                        }
                        lastClickedRow = row;
                    }
                    e.consume(); // Prevent default cell selection
                } else {
                    draggingFullRow = false;
                    EditorUtils.setFullRowSelectionActive(frozenTable, false);
                    lastClickedRow = -1;
                }
            }

            @Override
            public void mouseDragged(java.awt.event.MouseEvent e) {
                if (!draggingFullRow) {
                    return;
                }
                int row = frozenTable.rowAtPoint(e.getPoint());
                if (row < 0) {
                    return;
                }
                if (row != frozenTable.getSelectedRow()) {
                    EditorUtils.setFullRowSelectionActive(frozenTable, true);
                    frozenTable.setRowSelectionInterval(row, row);
                    if (frozenTable.getColumnCount() > 0) {
                        frozenTable.setColumnSelectionInterval(0, frozenTable.getColumnCount() - 1);
                    }
                    if (mainTable != null) {
                        EditorUtils.runWithFrozenSyncSuppressed(mainTable, () -> {
                            mainTable.setRowSelectionInterval(row, row);
                            if (mainTable.getColumnCount() > 0) {
                                mainTable.setColumnSelectionInterval(0, mainTable.getColumnCount() - 1);
                            } else {
                                mainTable.getColumnModel().getSelectionModel().clearSelection();
                            }
                        });
                    }
                    lastClickedRow = row;
                }
            }

            @Override
            public void mouseReleased(java.awt.event.MouseEvent e) {
                draggingFullRow = false;
            }
        };
        frozenTable.addMouseListener(frozenRowSelector);
        frozenTable.addMouseMotionListener(frozenRowSelector);

        // Create main scrollable table
        TableModel mainModel = new AbstractTableModel() {
            public int getRowCount() {
                return tableModel.getRowCount();
            }

            public int getColumnCount() {
                return tableModel.getColumnCount() - 2;
            }

            public Object getValueAt(int row, int col) {
                return tableModel.getValueAt(row, col + 2);
            }

            public void setValueAt(Object val, int row, int col) {
                tableModel.setValueAt(val, row, col + 2);
            }

            public String getColumnName(int col) {
                return tableModel.getColumnName(col + 2);
            }

            public Class<?> getColumnClass(int col) {
                return tableModel.getColumnClass(col + 2);
            }

            public boolean isCellEditable(int row, int col) {
                if (copyPasteModeEnabled) {
                    return false;
                }
                return tableModel.isCellEditable(row, col + 2);
            }
        };

        mainTable = new JTable(mainModel);
        styleTable(mainTable, false);
        setupMainTableColumns();
        TableLayoutDefaults.refreshHeaderPreferredWidth(mainTable);
        TableLayoutDefaults.applyRowHeight(mainTable, iconCache.hasIcons());

        // Sync row selection model between tables
        frozenTable.setSelectionModel(mainTable.getSelectionModel());

        // Allow programmatic full-row highlight without sharing column selections
        frozenTable.getColumnModel().getSelectionModel().setSelectionMode(ListSelectionModel.SINGLE_INTERVAL_SELECTION);
        mainTable.getColumnModel().getSelectionModel().setSelectionMode(ListSelectionModel.SINGLE_INTERVAL_SELECTION);
        EditorUtils.installFrozenColumnSync(frozenTable, mainTable);

        // Create scroll panes
        JScrollPane frozenScrollPane = new JScrollPane(frozenTable);
        frozenScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_NEVER);
        frozenScrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        frozenScrollPane.setBorder(BorderFactory.createMatteBorder(0, 0, 0, 1, new Color(200, 200, 200)));
        frozenScrollPane.setColumnHeaderView(frozenTable.getTableHeader());
        frozenScrollPane.getViewport().setBackground(Color.WHITE);

        JScrollPane mainScrollPane = new JScrollPane(mainTable);
        mainScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        mainScrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        mainScrollPane.setColumnHeaderView(mainTable.getTableHeader());
        mainScrollPane.setBorder(BorderFactory.createEmptyBorder());
        mainScrollPane.getViewport().setBackground(Color.WHITE);
        EditorUtils.installHeaderViewportSync(mainScrollPane);

        // Sync scrolling
        EditorUtils.linkVerticalScrollBars(frozenScrollPane, mainScrollPane);

        // Layout
        JPanel leftPanel = new JPanel(new BorderLayout());
        leftPanel.add(frozenScrollPane, BorderLayout.CENTER);
        int frozenWidth = TableLayoutDefaults.frozenPanelWidth(iconCache.hasIcons());
        leftPanel.setPreferredSize(new Dimension(frozenWidth, 0));
        leftPanel.setMinimumSize(new Dimension(frozenWidth, 0));
        EditorUtils.addHorizontalScrollbarSpacer(leftPanel, mainScrollPane);

        panel.add(leftPanel, BorderLayout.WEST);
        panel.add(mainScrollPane, BorderLayout.CENTER);

        return panel;
    }

    private void stopEditing() {
        if (mainTable != null && mainTable.isEditing()) {
            mainTable.getCellEditor().stopCellEditing();
        }
        if (frozenTable != null && frozenTable.isEditing()) {
            frozenTable.getCellEditor().stopCellEditing();
        }
    }

    private List<String> collectLearnsetChangesForLog() {
        Set<Integer> speciesNumbers = new HashSet<>(movesBackup.keySet());
        speciesNumbers.addAll(movesLearnt.keySet());

        List<String> changes = new ArrayList<>();
        for (Integer number : speciesNumbers) {
            List<MoveLearnt> before = movesBackup.get(number);
            List<MoveLearnt> after = movesLearnt.get(number);
            if (learnsetsEqual(before, after)) {
                continue;
            }
            String speciesName = getSpeciesName(number);
            String beforeText = formatLearnsetForLog(before);
            String afterText = formatLearnsetForLog(after);
            changes.add(String.format("%s: (old) %s -> (new) %s", speciesName, beforeText, afterText));
        }
        return changes;
    }

    private boolean learnsetsEqual(List<MoveLearnt> a, List<MoveLearnt> b) {
        if (a == null || a.isEmpty()) {
            return b == null || b.isEmpty();
        }
        if (b == null) {
            return false;
        }
        if (a.size() != b.size()) {
            return false;
        }
        for (int i = 0; i < a.size(); i++) {
            if (!Objects.equals(a.get(i), b.get(i))) {
                return false;
            }
        }
        return true;
    }

    private String formatLearnsetForLog(List<MoveLearnt> learnset) {
        if (learnset == null || learnset.isEmpty()) {
            return "None";
        }
        List<String> parts = new ArrayList<>();
        for (MoveLearnt ml : learnset) {
            String moveName = "Move #" + ml.move;
            if (ml.move >= 0 && ml.move < moveList.size()) {
                Move move = moveList.get(ml.move);
                if (move != null && move.name != null) {
                    moveName = move.name;
                }
            }
            parts.add(String.format("Lv%02d %s", ml.level, moveName));
        }
        return String.join(", ", parts);
    }

    private String getSpeciesName(int number) {
        if (number >= 0 && number < pokemonList.size()) {
            Species species = pokemonList.get(number);
            if (species != null && species.getName() != null) {
                return species.getFullName();
            }
        }
        return "Pokemon #" + number;
    }

    private class FrozenNameRenderer extends TableLayoutDefaults.StripedCellRenderer {
        FrozenNameRenderer() {
            super(true, 1);
            setIconTextGap(10);
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                boolean isSelected, boolean hasFocus, int row, int column) {
            Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            if (iconCache.hasIcons()) {
                ImageIcon icon = iconCache.getIcon(resolveSpecies(table, row));
                setIcon(icon); // Use the icon directly from cache (48x48) - fits better in 64px row
            } else {
                setIcon(null);
            }
            setText(value == null ? "" : value.toString());
            return c;
        }

        private Species resolveSpecies(JTable table, int viewRow) {
            int modelRow = table.convertRowIndexToModel(viewRow);
            int index = modelRow + 1;
            if (index >= 0 && index < pokemonList.size()) {
                return pokemonList.get(index);
            }
            return null;
        }
    }

    private void createBackup() {
        movesBackup.clear();
        for (Map.Entry<Integer, List<MoveLearnt>> entry : movesLearnt.entrySet()) {
            List<MoveLearnt> copy = new ArrayList<>();
            for (MoveLearnt moveLearnt : entry.getValue()) {
                copy.add(new MoveLearnt(moveLearnt));
            }
            movesBackup.put(entry.getKey(), copy);
        }
    }

    private void restoreFromBackup() {
        for (Map.Entry<Integer, List<MoveLearnt>> entry : movesBackup.entrySet()) {
            List<MoveLearnt> targetList = movesLearnt.computeIfAbsent(entry.getKey(), k -> new ArrayList<>());
            targetList.clear();
            for (MoveLearnt backupMove : entry.getValue()) {
                targetList.add(new MoveLearnt(backupMove));
            }
        }
        // Remove any keys that weren't in the backup
        movesLearnt.keySet().removeIf(key -> !movesBackup.containsKey(key));

        tableModel.fireTableDataChanged();
        if (frozenTable != null) {
            frozenTable.repaint();
        }
        if (mainTable != null) {
            mainTable.repaint();
        }
    }

    private void commitChanges() {
        createBackup();
    }

    public void onWindowClosing() {
        stopEditing();
        restoreFromBackup();
    }

    private void styleTable(JTable table, boolean isFrozen) {
        if (isFrozen) {
            TableLayoutDefaults.applySheetTableStyle(table, true, 1);
        } else {
            TableLayoutDefaults.applySheetTableStyle(table, false);
        }
    }

    private void setupMainTableColumns() {
        for (int i = 0; i < mainTable.getColumnCount(); i++) {
            TableColumn column = mainTable.getColumnModel().getColumn(i);

            if (i % 2 == 0) {
                // Move column (EVEN indices)
                column.setPreferredWidth(200);
                column.setCellEditor(new MoveComboBoxEditor(moveList));
            } else {
                // Level column (ODD indices)
                column.setPreferredWidth(100);
            }
        }
    }

    private void exportToCSV() {
        stopEditing();
        EditorUtils.exportTableToCSV(this, tableModel, "Learnsets Sheet");
    }

    private void importFromCSV() {
        if (copyPasteModeEnabled) {
            JOptionPane.showMessageDialog(this,
                    "Disable Copy/Paste Mode before importing.",
                    "Import CSV",
                    JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        stopEditing();

        EditorUtils.CsvData csvData = EditorUtils.chooseCsvFile(this, "Learnsets Sheet");
        if (csvData == null) {
            return;
        }

        try {
            int applied = EditorUtils.applyCsvDataToTable(csvData.getRows(), tableModel, true);
            tableModel.fireTableDataChanged();
            if (frozenTable != null) {
                frozenTable.repaint();
            }
            if (mainTable != null) {
                mainTable.repaint();
            }
            JOptionPane.showMessageDialog(this,
                    String.format("Imported %d rows from %s.", applied, csvData.getFile().getName()),
                    "Import Complete",
                    JOptionPane.INFORMATION_MESSAGE);
        } catch (IllegalArgumentException ex) {
            JOptionPane.showMessageDialog(this,
                    ex.getMessage(),
                    "Import Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private void toggleCopyPasteMode(boolean enabled) {
        copyPasteModeEnabled = enabled;
        if (enabled) {
            if (mainTable.isEditing()) {
                mainTable.getCellEditor().stopCellEditing();
            }
            if (frozenTable.isEditing()) {
                frozenTable.getCellEditor().stopCellEditing();
            }
            JOptionPane.showMessageDialog(this,
                    "Copy/Paste Mode ON\n\n" +
                            "- Tables are now read-only\n" +
                            "- Select cells and press Ctrl+C to copy moveset data\n" +
                            "- Toggle off to resume editing",
                    "Copy/Paste Mode",
                    JOptionPane.INFORMATION_MESSAGE);
        }
        frozenTable.repaint();
        mainTable.repaint();
    }

    private void showFindDialog() {
        stopEditing();
        EditorUtils.FindOptions options = EditorUtils.showFindDialog(this, findState.getLastOptions());
        if (options == null) {
            return;
        }
        EditorUtils.performFind(this, frozenTable, mainTable, tableModel, 2, findState, options);
    }

    public void save() {
        stopEditing();

        ManualEditRegistry.getInstance().addEntries("Learnsets", collectLearnsetChangesForLog());

        // Save learnsets back to ROM handler
        romHandler.setMovesLearnt(movesLearnt);

        JOptionPane.showMessageDialog(this,
                "- Learnsets updated successfully!\n\nChanges will be saved when you save/randomize the ROM.",
                "Save Complete",
                JOptionPane.INFORMATION_MESSAGE);
        commitChanges();
    }

    private void reload() {
        int result = JOptionPane.showConfirmDialog(this,
                "Reload learnsets from ROM?\nAny unsaved changes will be lost.",
                "Confirm Reload",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE);

        if (result == JOptionPane.YES_OPTION) {
            stopEditing();
            restoreFromBackup();
        }
    }

    private void addMoveSlot() {
        if (copyPasteModeEnabled) {
            JOptionPane.showMessageDialog(this,
                    "Copy/Paste Mode is enabled.\nDisable it to make changes.",
                    "Read-Only Mode",
                    JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        JOptionPane.showMessageDialog(this,
                "Add Move feature: Select a Pokemon row, then use this to add a new move slot.\n(Feature coming soon)",
                "Add Move",
                JOptionPane.INFORMATION_MESSAGE);
    }

    private void removeMoveSlot() {
        if (copyPasteModeEnabled) {
            JOptionPane.showMessageDialog(this,
                    "Copy/Paste Mode is enabled.\nDisable it to make changes.",
                    "Read-Only Mode",
                    JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        JOptionPane.showMessageDialog(this,
                "Remove Move feature: Select a Pokemon row and move slot to remove.\n(Feature coming soon)",
                "Remove Move",
                JOptionPane.INFORMATION_MESSAGE);
    }

    private void fillAllMoveSlots() {
        if (copyPasteModeEnabled) {
            JOptionPane.showMessageDialog(this,
                    "Copy/Paste Mode is enabled.\nDisable it to make changes.",
                    "Read-Only Mode",
                    JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        int choice = JOptionPane.showConfirmDialog(this,
                "This will give every Pokemon the maximum amount of moves when randomized.\nContinue?",
                "Fill All Move Slots",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE);
        if (choice != JOptionPane.YES_OPTION) {
            return;
        }

        stopEditing();

        int tackleId = MoveIDs.tackle;
        int slotsFilled = 0;
        int speciesAffected = 0;

        for (int i = 1; i < pokemonList.size(); i++) {
            Species species = pokemonList.get(i);
            if (species == null) {
                continue;
            }

            List<MoveLearnt> learnset = movesLearnt.get(species.getNumber());
            if (learnset == null) {
                learnset = new ArrayList<>();
                movesLearnt.put(species.getNumber(), learnset);
            }

            boolean updatedThisSpecies = false;

            for (int slot = 0; slot < maxMovesPerPokemon; slot++) {
                if (slot < learnset.size()) {
                    MoveLearnt entry = learnset.get(slot);
                    if (entry == null) {
                        entry = new MoveLearnt(tackleId, 1);
                        learnset.set(slot, entry);
                        slotsFilled++;
                        updatedThisSpecies = true;
                    } else if (entry.move <= 0) {
                        entry.move = tackleId;
                        if (entry.level <= 0) {
                            entry.level = 1;
                        }
                        slotsFilled++;
                        updatedThisSpecies = true;
                    }
                } else {
                    learnset.add(new MoveLearnt(tackleId, 1));
                    slotsFilled++;
                    updatedThisSpecies = true;
                }
            }

            if (updatedThisSpecies) {
                speciesAffected++;
            }
        }

        tableModel.fireTableDataChanged();
        frozenTable.repaint();
        mainTable.repaint();

        JOptionPane.showMessageDialog(this,
                String.format("Filled %d move slots across %d Pokemon.", slotsFilled, speciesAffected),
                "Fill Complete",
                JOptionPane.INFORMATION_MESSAGE);
    }

    /**
     * ComboBox editor for moves
     */
    private static class MoveComboBoxEditor extends DefaultCellEditor {
        private Map<String, Integer> moveNameToId = new HashMap<>();

        public MoveComboBoxEditor(List<Move> moves) {
            super(new JComboBox<String>());
            JComboBox<String> comboBox = (JComboBox<String>) getComponent();
            comboBox.setFont(new Font("Segoe UI", Font.PLAIN, 14));
            comboBox.addItem("");
            moveNameToId.put("", 0);

            for (int i = 0; i < moves.size(); i++) {
                Move move = moves.get(i);
                if (move != null) {
                    comboBox.addItem(move.name); // Just the move name, no ID
                    moveNameToId.put(move.name, i);
                }
            }

            EditorUtils.installSearchableComboBox(comboBox);
        }

        @Override
        public Object getCellEditorValue() {
            JComboBox<String> comboBox = (JComboBox<String>) getComponent();
            Object selected = comboBox.getSelectedItem();
            String selectedName = selected != null ? selected.toString() : "";
            Integer moveId = moveNameToId.get(selectedName);
            return moveId != null ? (moveId + ": " + selectedName) : "";
        }
    }

    /**
     * Table model for learnsets
     */
    private static class LearnsetsTableModel extends AbstractTableModel {
        private final List<Species> pokemonList;
        private final List<Move> moveList;
        private final Map<Integer, List<MoveLearnt>> movesLearnt;
        private final int maxMoves;
        private final String[] columnNames;

        public LearnsetsTableModel(List<Species> pokemonList, List<Move> moveList,
                Map<Integer, List<MoveLearnt>> movesLearnt, int maxMoves) {
            this.pokemonList = pokemonList;
            this.moveList = moveList;
            this.movesLearnt = movesLearnt;
            this.maxMoves = maxMoves;

            // Build column names: ID, Name, Move, Lvl, Move, Lvl, ... (EXACT match to
            // PokEditor)
            columnNames = new String[2 + maxMoves * 2];
            columnNames[0] = "ID";
            columnNames[1] = "Name";
            for (int i = 0; i < maxMoves; i++) {
                columnNames[2 + i * 2] = "Move"; // Move comes FIRST (PokEditor order)
                columnNames[2 + i * 2 + 1] = "Lvl"; // Level comes SECOND
            }
        }

        @Override
        public int getRowCount() {
            return pokemonList.size() - 1;
        }

        @Override
        public int getColumnCount() {
            return columnNames.length;
        }

        @Override
        public String getColumnName(int column) {
            return columnNames[column];
        }

        @Override
        public Class<?> getColumnClass(int col) {
            if (col <= 1)
                return String.class;
            if (col % 2 == 0)
                return String.class; // Move columns (EVEN indices after frozen columns)
            return Integer.class; // Level columns (ODD indices after frozen columns)
        }

        @Override
        public boolean isCellEditable(int row, int col) {
            return col > 1;
        }

        @Override
        public Object getValueAt(int row, int col) {
            Species pokemon = pokemonList.get(row + 1);
            if (pokemon == null)
                return col <= 1 ? "" : (col % 2 == 0 ? "" : 0);

            if (col == 0)
                return pokemon.getNumber();
            if (col == 1)
                return pokemon.getName();

            // Get learnset for this Pokemon
            List<MoveLearnt> learnset = movesLearnt.get(pokemon.getNumber());
            if (learnset == null)
                return col % 2 == 0 ? "" : 0;

            int moveIndex = (col - 2) / 2;
            if (moveIndex >= learnset.size())
                return col % 2 == 0 ? "" : 0;

            MoveLearnt ml = learnset.get(moveIndex);
            if (col % 2 == 0) {
                // Move column (EVEN indices) - Display just the move name, no ID
                Move move = ml.move < moveList.size() ? moveList.get(ml.move) : null;
                return move != null ? move.name : "";
            } else {
                // Level column (ODD indices)
                return ml.level;
            }
        }

        @Override
        public void setValueAt(Object value, int row, int col) {
            if (col <= 1)
                return;

            Species pokemon = pokemonList.get(row + 1);
            if (pokemon == null)
                return;

            List<MoveLearnt> learnset = movesLearnt.get(pokemon.getNumber());
            if (learnset == null) {
                learnset = new ArrayList<MoveLearnt>();
                movesLearnt.put(pokemon.getNumber(), learnset);
            }

            int moveIndex = (col - 2) / 2;

            // Ensure learnset is large enough
            while (moveIndex >= learnset.size()) {
                learnset.add(new MoveLearnt(0, 1));
            }

            MoveLearnt ml = learnset.get(moveIndex);

            if (col % 2 == 0) {
                // Move column (EVEN indices)
                ml.move = parseMoveValue(value.toString());
            } else {
                // Level column (ODD indices)
                ml.level = clampLevel(parseInt(value));
            }

            fireTableCellUpdated(row, col);
        }

        private int parseInt(Object val) {
            if (val instanceof Integer)
                return (Integer) val;
            try {
                return Integer.parseInt(val.toString());
            } catch (Exception e) {
                return 0;
            }
        }

        private int clampLevel(int level) {
            if (level < 0) {
                return 0;
            }
            return Math.min(100, level);
        }

        private int parseMoveValue(String val) {
            if (val == null || val.isEmpty())
                return 0;

            // Handle both formats: "ID: Name" (from internal storage) and "Name" (from
            // dropdown)
            if (val.contains(":")) {
                String[] parts = val.split(":");
                try {
                    return Integer.parseInt(parts[0].trim());
                } catch (Exception e) {
                    return 0;
                }
            } else {
                // Just a move name - find the ID
                for (int i = 0; i < moveList.size(); i++) {
                    Move move = moveList.get(i);
                    if (move != null && move.name.equals(val)) {
                        return i;
                    }
                }
                return 0;
            }
        }
    }
}
