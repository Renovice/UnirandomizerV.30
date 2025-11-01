package com.dabomstew.pkrandom.pokemon.editors;

import com.dabomstew.pkrandom.log.ManualEditRegistry;
import com.dabomstew.pkromio.gamedata.*;
import com.dabomstew.pkromio.romhandlers.RomHandler;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Moves Sheet editor matching PokEditor's EXACT implementation
 * Based on MovesTable.java from PokEditor
 */
public class MovesSheetPanel extends JPanel {

    private final RomHandler romHandler;
    private final List<Move> movesList;
    private JTable frozenTable;
    private JTable mainTable;
    private MovesDataTableModel tableModel;
    private boolean copyPasteModeEnabled = false;
    private final EditorUtils.FindState findState = new EditorUtils.FindState();
    private final Map<Move, MoveBackup> backupData = new HashMap<>();

    public MovesSheetPanel(RomHandler romHandler) {
        this.romHandler = romHandler;
        this.movesList = romHandler.getMoves();
        initializeUI();
        createBackup();
    }

    private void initializeUI() {
        setLayout(new BorderLayout());
        setBackground(Color.WHITE);

        // Create toolbar
        add(createStyledToolbar(), BorderLayout.NORTH);

        // Create split table with frozen columns
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

        JToggleButton copyPasteButton = EditorUtils.createStyledToggleButton("Copy/Paste Mode", new Color(255, 152, 0));
        copyPasteButton.addActionListener(e -> toggleCopyPasteMode(copyPasteButton.isSelected()));

        JButton findButton = EditorUtils.createStyledButton("Find", new Color(0, 150, 136));
        findButton.addActionListener(e -> showFindDialog());

        toolbar.add(saveButton);
        toolbar.add(reloadButton);
        toolbar.add(exportButton);
        toolbar.add(importButton);
        toolbar.add(Box.createHorizontalStrut(5));
        toolbar.add(copyPasteButton);
        toolbar.add(findButton);
        toolbar.add(Box.createHorizontalStrut(10));

        JLabel infoLabel = new JLabel("Edit move data directly in the table");
        infoLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        infoLabel.setForeground(new Color(100, 100, 100));
        toolbar.add(infoLabel);

        return toolbar;
    }

    private JPanel createFrozenColumnTable() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(Color.WHITE);

        tableModel = new MovesDataTableModel(movesList, romHandler);

        // Frozen table (ID and Name)
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

            public boolean isCellEditable(int row, int col) {
                if (copyPasteModeEnabled) {
                    return false;
                }
                return col == 1; // Name is editable
            }

            public void setValueAt(Object val, int row, int col) {
                tableModel.setValueAt(val, row, col);
            }
        };

        frozenTable = new JTable(frozenModel) {
            @Override
            public void scrollRectToVisible(java.awt.Rectangle aRect) {
                // Disable horizontal scrolling for frozen table
                aRect.x = 0;
                super.scrollRectToVisible(aRect);
            }
        };
        styleTable(frozenTable, true);
        TableLayoutDefaults.configureFrozenColumns(frozenTable.getColumnModel(), false);

        // Main scrollable table
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

        TableLayoutDefaults.applyRowHeight(frozenTable, false);
        TableLayoutDefaults.applyRowHeight(mainTable, false);

        // Sync row selection between frozen and main tables
        frozenTable.setSelectionModel(mainTable.getSelectionModel());
        frozenTable.getColumnModel().getSelectionModel().setSelectionMode(ListSelectionModel.SINGLE_INTERVAL_SELECTION);
        mainTable.getColumnModel().getSelectionModel().setSelectionMode(ListSelectionModel.SINGLE_INTERVAL_SELECTION);
        EditorUtils.installFrozenColumnSync(frozenTable, mainTable);

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
        mainScrollPane.getViewport().setBackground(Color.WHITE);
        mainScrollPane.setBorder(BorderFactory.createEmptyBorder());
        EditorUtils.installHeaderViewportSync(mainScrollPane);

        mainScrollPane.getVerticalScrollBar()
                .addAdjustmentListener(e -> frozenScrollPane.getVerticalScrollBar().setValue(e.getValue()));

        JPanel leftPanel = new JPanel(new BorderLayout());
        leftPanel.add(frozenScrollPane, BorderLayout.CENTER);
        int frozenWidth = TableLayoutDefaults.frozenPanelWidth(false);
        leftPanel.setPreferredSize(new Dimension(frozenWidth, 0));
        leftPanel.setMinimumSize(new Dimension(frozenWidth, 0));

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

    private List<String> collectMoveChangesForLog() {
        List<String> changes = new ArrayList<>();
        for (Move move : movesList) {
            if (move == null) {
                continue;
            }
            MoveBackup backup = backupData.get(move);
            if (backup == null) {
                continue;
            }
            Move before = backup.getSnapshot();
            if (moveHasChanged(before, move)) {
                String name = move.name != null ? move.name : "Move";
                changes.add(name + " (#" + move.number + ") updated");
            }
        }
        return changes;
    }

    private boolean moveHasChanged(Move before, Move after) {
        if (!Objects.equals(before.name, after.name))
            return true;
        if (before.effectIndex != after.effectIndex)
            return true;
        if (before.category != after.category)
            return true;
        if (before.power != after.power)
            return true;
        if (!Objects.equals(before.type, after.type))
            return true;
        if ((int) before.hitratio != (int) after.hitratio)
            return true;
        if (before.pp != after.pp)
            return true;
        if (before.secondaryEffectChance != after.secondaryEffectChance)
            return true;
        if (before.target != after.target)
            return true;
        if (before.priority != after.priority)
            return true;
        if (before.makesContact != after.makesContact)
            return true;
        if (before.isProtectedFromProtect != after.isProtectedFromProtect)
            return true;
        if (before.isMagicCoatAffected != after.isMagicCoatAffected)
            return true;
        if (before.isSnatchAffected != after.isSnatchAffected)
            return true;
        if (before.isMirrorMoveAffected != after.isMirrorMoveAffected)
            return true;
        if (before.isFlinchMove != after.isFlinchMove)
            return true;
        if (before.hidesHpBars != after.hidesHpBars)
            return true;
        if (before.removesTargetShadow != after.removesTargetShadow)
            return true;
        if (before.contestEffect != after.contestEffect)
            return true;
        if (before.contestType != after.contestType)
            return true;
        return false;
    }

    private void createBackup() {
        backupData.clear();
        for (Move move : movesList) {
            if (move != null) {
                backupData.put(move, new MoveBackup(move));
            }
        }
    }

    private void restoreFromBackup() {
        for (Move move : movesList) {
            if (move != null) {
                MoveBackup backup = backupData.get(move);
                if (backup != null) {
                    backup.restoreTo(move);
                }
            }
        }
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
        TableLayoutDefaults.applyRowHeight(table, false);
    }

    private void setupMainTableColumns() {
        // Column widths - adjusted for better display with multi-line headers
        int[] widths = {
                240, // Effect
                140, // Category
                90, // Power
                140, // Type
                90, // Accuracy
                90, // PP
                140, // Effect Chance
                200, // Target
                90, // Priority
                140, 140, 140, 140, 140, 140, 140, 160, // move flags
                110, // Contest Effect
                110 // Contest Type
        };

        for (int i = 0; i < widths.length && i < mainTable.getColumnCount(); i++) {
            TableColumn column = mainTable.getColumnModel().getColumn(i);
            column.setPreferredWidth(widths[i]);
            column.setMinWidth(Math.max(60, widths[i] - 60));
            column.setMaxWidth(widths[i] + 120);
            column.setWidth(widths[i]);

            int actualCol = i + 2;
            if (actualCol == 3) {
                // Category dropdown
                column.setCellEditor(new CategoryComboBoxEditor());
            } else if (actualCol == 5) {
                // Type dropdown with colors
                column.setCellRenderer(new TypeCellRenderer());
                column.setCellEditor(new TypeComboBoxEditor(romHandler));
            } else if (actualCol == 9) {
                // Target dropdown
                column.setCellEditor(new TargetComboBoxEditor());
            } else if (actualCol >= 11 && actualCol <= 18) {
                // Checkboxes for move flags - use stable custom editor
                column.setCellEditor(new StableCheckBoxEditor());
                column.setCellRenderer(new CheckBoxRenderer());
            }
        }
        mainTable.doLayout();

        // Set header height to accommodate multi-line text
        mainTable.getTableHeader().setPreferredSize(new Dimension(
                mainTable.getTableHeader().getPreferredSize().width, 60));
    }

    public void save() {
        stopEditing();
        ManualEditRegistry.getInstance().addEntries("Moves Data", collectMoveChangesForLog());
        JOptionPane.showMessageDialog(this,
                "Moves updated successfully!",
                "Save Complete",
                JOptionPane.INFORMATION_MESSAGE);
        commitChanges();
    }

    private void reload() {
        stopEditing();
        restoreFromBackup();
    }

    private void exportToCSV() {
        stopEditing();
        EditorUtils.exportTableToCSV(this, tableModel, "Moves Sheet");
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

        EditorUtils.CsvData csvData = EditorUtils.chooseCsvFile(this, "Moves Sheet");
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
                            "- Select cells and press Ctrl+C to copy\n" +
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

    // Renderers and Editors
    private static class TypeCellRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                boolean isSelected, boolean hasFocus, int row, int column) {
            Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            setHorizontalAlignment(SwingConstants.CENTER);
            setFont(new Font("Segoe UI", Font.BOLD, 11));

            if (!isSelected && value != null && !value.toString().isEmpty()) {
                try {
                    Type type = Type.valueOf(value.toString());
                    c.setBackground(getTypeColor(type));
                    c.setForeground(Color.WHITE);
                } catch (Exception e) {
                    c.setBackground(
                            row % 2 == 0 ? TableLayoutDefaults.EVEN_ROW_COLOR : TableLayoutDefaults.ODD_ROW_COLOR);
                    c.setForeground(Color.BLACK);
                }
            } else if (!isSelected) {
                c.setBackground(row % 2 == 0 ? TableLayoutDefaults.EVEN_ROW_COLOR : TableLayoutDefaults.ODD_ROW_COLOR);
                c.setForeground(Color.BLACK);
            }
            setBorder(noFocusBorder);
            return c;
        }

        private Color getTypeColor(Type type) {
            switch (type) {
                case NORMAL:
                    return new Color(168, 168, 120);
                case FIGHTING:
                    return new Color(192, 48, 40);
                case FLYING:
                    return new Color(168, 144, 240);
                case POISON:
                    return new Color(160, 64, 160);
                case GROUND:
                    return new Color(224, 192, 104);
                case ROCK:
                    return new Color(184, 160, 56);
                case BUG:
                    return new Color(168, 184, 32);
                case GHOST:
                    return new Color(112, 88, 152);
                case STEEL:
                    return new Color(184, 184, 208);
                case FIRE:
                    return new Color(240, 128, 48);
                case WATER:
                    return new Color(104, 144, 240);
                case GRASS:
                    return new Color(120, 200, 80);
                case ELECTRIC:
                    return new Color(248, 208, 48);
                case PSYCHIC:
                    return new Color(248, 88, 136);
                case ICE:
                    return new Color(152, 216, 216);
                case DRAGON:
                    return new Color(112, 56, 248);
                case DARK:
                    return new Color(112, 88, 72);
                case FAIRY:
                    return new Color(238, 153, 238);
                default:
                    return Color.WHITE;
            }
        }
    }

    private static class CheckBoxRenderer extends JCheckBox implements TableCellRenderer {
        public CheckBoxRenderer() {
            setHorizontalAlignment(SwingConstants.CENTER);
            setVerticalAlignment(SwingConstants.CENTER);
            setOpaque(true);
            setBorderPainted(true);
            setFocusPainted(false);
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                boolean isSelected, boolean hasFocus, int row, int column) {
            // Safely handle different value types
            boolean checked = false;
            if (value instanceof Boolean) {
                checked = (Boolean) value;
            } else if (value != null) {
                // Try to parse string values
                String strValue = value.toString().toLowerCase();
                checked = strValue.equals("true") || strValue.equals("1");
            }

            setSelected(checked);
            setEnabled(true);

            // Set background color
            if (isSelected) {
                setBackground(table.getSelectionBackground());
                setForeground(table.getSelectionForeground());
            } else {
                setBackground(row % 2 == 0 ? TableLayoutDefaults.EVEN_ROW_COLOR : TableLayoutDefaults.ODD_ROW_COLOR);
                setForeground(Color.BLACK);
            }

            // Remove focus border to prevent flickering
            setBorder(BorderFactory.createEmptyBorder(1, 1, 1, 1));

            return this;
        }
    }

    /**
     * Stable checkbox editor that doesn't flicker or move when clicked
     */
    private static class StableCheckBoxEditor extends AbstractCellEditor implements TableCellEditor {
        private final JCheckBox checkBox;

        public StableCheckBoxEditor() {
            checkBox = new JCheckBox();
            checkBox.setHorizontalAlignment(SwingConstants.CENTER);
            checkBox.setVerticalAlignment(SwingConstants.CENTER);
            checkBox.setOpaque(true);
            checkBox.setBorderPainted(false);
            checkBox.setFocusPainted(false);
            checkBox.setBackground(Color.WHITE);

            // Toggle on click - this prevents the "jumping" behavior
            checkBox.addActionListener(e -> {
                // Stop editing immediately after toggle
                fireEditingStopped();
            });
        }

        @Override
        public Component getTableCellEditorComponent(JTable table, Object value,
                boolean isSelected, int row, int column) {
            // Parse the current value
            boolean checked = false;
            if (value instanceof Boolean) {
                checked = (Boolean) value;
            } else if (value != null) {
                String strValue = value.toString().toLowerCase();
                checked = strValue.equals("true") || strValue.equals("1");
            }

            checkBox.setSelected(checked);

            // Keep the selection background visible when editing
            if (isSelected) {
                checkBox.setBackground(table.getSelectionBackground());
                checkBox.setForeground(table.getSelectionForeground());
            } else {
                checkBox.setBackground(
                        row % 2 == 0 ? TableLayoutDefaults.EVEN_ROW_COLOR : TableLayoutDefaults.ODD_ROW_COLOR);
                checkBox.setForeground(Color.BLACK);
            }

            return checkBox;
        }

        @Override
        public Object getCellEditorValue() {
            return checkBox.isSelected();
        }
    }

    private static class CategoryComboBoxEditor extends DefaultCellEditor {
        // Matching PokEditor's category keys from MovesTable.java line 17
        private static final String[] CATEGORIES = { "Physical", "Special", "Status" };

        public CategoryComboBoxEditor() {
            super(new JComboBox<String>());
            JComboBox<String> comboBox = (JComboBox<String>) getComponent();
            comboBox.setFont(new Font("Segoe UI", Font.PLAIN, 11));
            for (String category : CATEGORIES) {
                comboBox.addItem(category);
            }
            EditorUtils.installSearchableComboBox(comboBox);
        }
    }

    private class TypeComboBoxEditor extends DefaultCellEditor {
        public TypeComboBoxEditor(RomHandler romHandler) {
            super(new JComboBox<String>());
            JComboBox<String> comboBox = (JComboBox<String>) getComponent();
            comboBox.setFont(new Font("Segoe UI", Font.PLAIN, 11));
            // Use the ROM's actual type list instead of hardcoded Type.values()
            // This respects Gen 5 vanilla (17 types) vs modded with Fairy (18 types)
            for (Type type : romHandler.getTypeTable().getTypes()) {
                comboBox.addItem(type.name());
            }
            EditorUtils.installSearchableComboBox(comboBox);
        }
    }

    private static class TargetComboBoxEditor extends DefaultCellEditor {
        // Matching PokEditor's target keys from MovesTable.java line 18
        private static final String[] TARGETS = {
                "Selected Pokemon", "Automatic", "Random", "Both Foes", "All Except User",
                "User", "User Side", "Entire Field", "Foe Side", "Ally", "User or Ally", "Me First"
        };

        public TargetComboBoxEditor() {
            super(new JComboBox<String>());
            JComboBox<String> comboBox = (JComboBox<String>) getComponent();
            comboBox.setFont(new Font("Segoe UI", Font.PLAIN, 11));
            for (String target : TARGETS) {
                comboBox.addItem(target);
            }
            EditorUtils.installSearchableComboBox(comboBox);
        }
    }

    /**
     * Table model for moves data matching PokEditor EXACTLY
     */
    private static class MovesDataTableModel extends AbstractTableModel {
        private final List<Move> movesList;
        private final RomHandler romHandler;

        // Column names with HTML line breaks for better display
        private final String[] columnNames = {
                "ID", "Name",
                "Effect", "Category", "Power", "Type", "Accuracy", "PP",
                "<html><center>Effect<br>Chance</center></html>",
                "Target", "Priority",
                "<html><center>Makes<br>Contact</center></html>",
                "<html><center>Blocked by<br>Protect</center></html>",
                "<html><center>Reflected by<br>Magic Coat</center></html>",
                "<html><center>Affected by<br>Snatch</center></html>",
                "<html><center>Affected by<br>Mirror Move</center></html>",
                "<html><center>Triggers<br>King's Rock</center></html>",
                "<html><center>Hides<br>HP Bars</center></html>",
                "<html><center>Remove Target<br>Shadow</center></html>",
                "<html><center>Contest<br>Effect</center></html>",
                "<html><center>Contest<br>Type</center></html>"
        };

        public MovesDataTableModel(List<Move> movesList, RomHandler romHandler) {
            this.movesList = movesList;
            this.romHandler = romHandler;
        }

        @Override
        public int getRowCount() {
            return movesList.size();
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
            if (col == 0)
                return Integer.class; // ID
            if (col == 1 || col == 3 || col == 5 || col == 9)
                return String.class; // Name, Category, Type, Target
            if (col >= 11 && col <= 18)
                return Boolean.class; // Checkboxes
            return Integer.class; // Everything else
        }

        @Override
        public boolean isCellEditable(int row, int col) {
            return col > 0; // Everything except ID is editable
        }

        @Override
        public Object getValueAt(int row, int col) {
            // Handle empty/invalid rows
            if (row >= movesList.size()) {
                // Return appropriate default based on column type
                if (col >= 11 && col <= 18)
                    return false; // Checkboxes
                if (col == 0)
                    return 0; // ID
                return ""; // Strings
            }

            Move move = movesList.get(row);
            if (move == null) {
                // Return appropriate default based on column type
                if (col >= 11 && col <= 18)
                    return false; // Checkboxes
                if (col == 0)
                    return 0; // ID
                return ""; // Strings
            }

            switch (col) {
                case 0:
                    return move.number; // ID
                case 1:
                    return move.name != null ? move.name : ""; // Name
                case 2:
                    return move.effectIndex; // Effect
                case 3:
                    return getCategoryName(move.category); // Category
                case 4:
                    return move.power; // Power
                case 5:
                    return move.type != null ? move.type.name() : ""; // Type
                case 6:
                    return (int) move.hitratio; // Accuracy (convert double to int for display)
                case 7:
                    return move.pp; // PP
                case 8:
                    return move.secondaryEffectChance; // Effect Chance (already a percentage 0-100)
                case 9:
                    return getTargetName(move.target); // Target
                case 10:
                    return move.priority; // Priority
                case 11:
                    return move.makesContact; // Makes Contact
                case 12:
                    return move.isProtectedFromProtect; // Blocked by Protect
                case 13:
                    return move.isMagicCoatAffected; // Reflected by Magic Coat
                case 14:
                    return move.isSnatchAffected; // Affected by Snatch
                case 15:
                    return move.isMirrorMoveAffected; // Affected by Mirror Move
                case 16:
                    return move.isFlinchMove; // Triggers King's Rock
                case 17:
                    return move.hidesHpBars; // Hides HP Bars
                case 18:
                    return move.removesTargetShadow; // Remove Target Shadow
                case 19:
                    return move.contestEffect; // Contest Effect
                case 20:
                    return move.contestType; // Contest Type
                default:
                    // Return appropriate default based on column type
                    if (col >= 11 && col <= 18)
                        return false; // Checkboxes
                    return 0; // Integers
            }
        }

        @Override
        public void setValueAt(Object val, int row, int col) {
            if (row >= movesList.size())
                return;
            Move move = movesList.get(row);
            if (move == null)
                return;

            try {
                switch (col) {
                    case 1:
                        move.name = val.toString();
                        break; // Name
                    case 2:
                        move.effectIndex = parseInt(val);
                        break; // Effect
                    case 3:
                        move.category = parseCategoryName(val.toString());
                        break; // Category
                    case 4:
                        move.power = parseBoundedInt(val, 0, 255);
                        break; // Power
                    case 5:
                        if (val != null && !val.toString().isEmpty()) {
                            move.type = Type.valueOf(val.toString());
                        }
                        break; // Type
                    case 6:
                        move.hitratio = parseBoundedInt(val, 0, 255);
                        break; // Accuracy
                    case 7:
                        move.pp = parseBoundedInt(val, 0, 255);
                        break; // PP
                    case 8:
                        move.secondaryEffectChance = parseInt(val);
                        break; // Effect Chance (already a percentage)
                    case 9:
                        move.target = parseTargetName(val.toString());
                        break; // Target
                    case 10:
                        move.priority = (byte) parseInt(val);
                        break; // Priority
                    case 11:
                        move.makesContact = parseBoolean(val);
                        break; // Makes Contact
                    case 12:
                        move.isProtectedFromProtect = parseBoolean(val);
                        break; // Blocked by Protect
                    case 13:
                        move.isMagicCoatAffected = parseBoolean(val);
                        break; // Reflected by Magic Coat
                    case 14:
                        move.isSnatchAffected = parseBoolean(val);
                        break; // Affected by Snatch
                    case 15:
                        move.isMirrorMoveAffected = parseBoolean(val);
                        break; // Affected by Mirror Move
                    case 16:
                        move.isFlinchMove = parseBoolean(val);
                        break; // Triggers King's Rock
                    case 17:
                        move.hidesHpBars = parseBoolean(val);
                        break; // Hides HP Bars
                    case 18:
                        move.removesTargetShadow = parseBoolean(val);
                        break; // Remove Target Shadow
                    case 19:
                        move.contestEffect = parseInt(val);
                        break; // Contest Effect
                    case 20:
                        move.contestType = parseInt(val);
                        break; // Contest Type
                }
                fireTableCellUpdated(row, col);
            } catch (Exception e) {
                e.printStackTrace();
            }
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

        private int parseBoundedInt(Object val, int min, int max) {
            int parsed = parseInt(val);
            if (parsed < min) {
                return min;
            }
            if (parsed > max) {
                return max;
            }
            return parsed;
        }

        // Convert category index to PokEditor's category names
        private String getCategoryName(MoveCategory category) {
            if (category == null)
                return "Status";
            switch (category) {
                case PHYSICAL:
                    return "Physical";
                case SPECIAL:
                    return "Special";
                case STATUS:
                    return "Status";
                default:
                    return "Status";
            }
        }

        // Convert PokEditor's category names to MoveCategory enum
        private MoveCategory parseCategoryName(String name) {
            if (name == null)
                return MoveCategory.STATUS;
            switch (name) {
                case "Physical":
                    return MoveCategory.PHYSICAL;
                case "Special":
                    return MoveCategory.SPECIAL;
                case "Status":
                    return MoveCategory.STATUS;
                default:
                    return MoveCategory.STATUS;
            }
        }

        // Convert target index to PokEditor's target names
        private String getTargetName(int target) {
            String[] targets = {
                    "Selected Pokemon", "Automatic", "Random", "Both Foes", "All Except User",
                    "User", "User Side", "Entire Field", "Foe Side", "Ally", "User or Ally", "Me First"
            };
            if (target >= 0 && target < targets.length) {
                return targets[target];
            }
            return "Selected Pokemon";
        }

        // Convert PokEditor's target names to index
        private int parseTargetName(String targetName) {
            String[] targets = {
                    "Selected Pokemon", "Automatic", "Random", "Both Foes", "All Except User",
                    "User", "User Side", "Entire Field", "Foe Side", "Ally", "User or Ally", "Me First"
            };
            for (int i = 0; i < targets.length; i++) {
                if (targets[i].equals(targetName)) {
                    return i;
                }
            }
            return 0; // Default to "Selected Pokemon"
        }

        // Parse boolean from various object types
        private boolean parseBoolean(Object val) {
            if (val instanceof Boolean) {
                return (Boolean) val;
            } else if (val != null) {
                String str = val.toString().toLowerCase();
                return str.equals("true") || str.equals("1");
            }
            return false;
        }
    }

    private static class MoveBackup {
        private final Move snapshot;

        MoveBackup(Move move) {
            this.snapshot = new Move(move);
        }

        void restoreTo(Move move) {
            copyMove(snapshot, move);
        }

        Move getSnapshot() {
            return new Move(snapshot);
        }

        private static void copyMove(Move source, Move target) {
            target.name = source.name;
            target.number = source.number;
            target.internalId = source.internalId;
            target.power = source.power;
            target.pp = source.pp;
            target.hitratio = source.hitratio;
            target.type = source.type;
            target.category = source.category;
            target.statChangeMoveType = source.statChangeMoveType;

            target.statChanges = new Move.StatChange[source.statChanges.length];
            for (int i = 0; i < source.statChanges.length; i++) {
                target.statChanges[i] = Move.StatChange.copy(source.statChanges[i]);
            }

            target.statusMoveType = source.statusMoveType;
            target.statusType = source.statusType;
            target.criticalChance = source.criticalChance;
            target.statusPercentChance = source.statusPercentChance;
            target.flinchPercentChance = source.flinchPercentChance;
            target.recoilPercent = source.recoilPercent;
            target.absorbPercent = source.absorbPercent;
            target.priority = source.priority;
            target.makesContact = source.makesContact;
            target.isChargeMove = source.isChargeMove;
            target.isRechargeMove = source.isRechargeMove;
            target.isPunchMove = source.isPunchMove;
            target.isSoundMove = source.isSoundMove;
            target.isTrapMove = source.isTrapMove;
            target.effectIndex = source.effectIndex;
            target.target = source.target;
            target.hitCount = source.hitCount;
            target.isProtectedFromProtect = source.isProtectedFromProtect;
            target.isMagicCoatAffected = source.isMagicCoatAffected;
            target.isSnatchAffected = source.isSnatchAffected;
            target.isMirrorMoveAffected = source.isMirrorMoveAffected;
            target.isFlinchMove = source.isFlinchMove;
            target.hidesHpBars = source.hidesHpBars;
            target.removesTargetShadow = source.removesTargetShadow;
            target.secondaryEffectChance = source.secondaryEffectChance;
            target.contestEffect = source.contestEffect;
            target.contestType = source.contestType;
        }
    }
}
