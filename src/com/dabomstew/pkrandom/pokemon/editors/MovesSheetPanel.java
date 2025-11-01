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
        for (int viewCol = 0; viewCol < mainTable.getColumnCount(); viewCol++) {
            int modelCol = viewCol + 2;
            if (modelCol >= tableModel.getColumnCount()) {
                continue;
            }

            TableColumn column = mainTable.getColumnModel().getColumn(viewCol);
            int width = tableModel.getPreferredWidthForColumn(modelCol);
            column.setPreferredWidth(width);
            column.setMinWidth(Math.max(60, width - 60));
            column.setMaxWidth(width + 120);
            column.setWidth(width);

            if (modelCol == tableModel.getCategoryColumnIndex()) {
                column.setCellEditor(new CategoryComboBoxEditor());
            } else if (modelCol == tableModel.getTypeColumnIndex()) {
                column.setCellRenderer(new TypeCellRenderer());
                column.setCellEditor(new TypeComboBoxEditor(romHandler));
            } else if (modelCol == tableModel.getTargetColumnIndex()) {
                column.setCellEditor(new TargetComboBoxEditor());
            } else if (tableModel.isFlagColumn(modelCol)) {
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
    private final boolean supportsExtendedMoveFlags;
    private final boolean showContestColumns;

        private final List<String> columnNames;

        private final int colId;
        private final int colName;
        private final int colEffect;
        private final int colCategory;
        private final int colPower;
        private final int colType;
        private final int colAccuracy;
        private final int colPp;
        private final int colEffectChance;
        private final int colTarget;
        private final int colPriority;
        private final int colMakesContact;
        private final int colBlockedByProtect;
        private final int colMagicCoat;
        private final int colSnatch;
        private final int colMirrorMove;
        private final int colKingsRock;
        private final int colHideHpBars;
        private final int colRemoveTargetShadow;
        private final int colContestEffect;
        private final int colContestType;

        public MovesDataTableModel(List<Move> movesList, RomHandler romHandler) {
            this.movesList = movesList;
            this.romHandler = romHandler;
            boolean isGen3 = romHandler instanceof com.dabomstew.pkromio.romhandlers.Gen3RomHandler;
            this.supportsExtendedMoveFlags = !isGen3;
            this.showContestColumns = !isGen3;

            List<String> cols = new ArrayList<>();
            colId = addColumn(cols, "ID");
            colName = addColumn(cols, "Name");
            colEffect = addColumn(cols, "Effect");
            colCategory = addColumn(cols, "Category");
            colPower = addColumn(cols, "Power");
            colType = addColumn(cols, "Type");
            colAccuracy = addColumn(cols, "Accuracy");
            colPp = addColumn(cols, "PP");
            colEffectChance = addColumn(cols, "<html><center>Effect<br>Chance</center></html>");
            colTarget = addColumn(cols, "Target");
            colPriority = addColumn(cols, "Priority");
            colMakesContact = addColumn(cols, "<html><center>Makes<br>Contact</center></html>");
            colBlockedByProtect = addColumn(cols, "<html><center>Blocked by<br>Protect</center></html>");
            colMagicCoat = addColumn(cols, "<html><center>Reflected by<br>Magic Coat</center></html>");
            colSnatch = addColumn(cols, "<html><center>Affected by<br>Snatch</center></html>");
            colMirrorMove = addColumn(cols, "<html><center>Affected by<br>Mirror Move</center></html>");
            colKingsRock = addColumn(cols, "<html><center>Triggers<br>King's Rock</center></html>");
            if (supportsExtendedMoveFlags) {
                colHideHpBars = addColumn(cols, "<html><center>Hides<br>HP Bars</center></html>");
                colRemoveTargetShadow = addColumn(cols, "<html><center>Remove Target<br>Shadow</center></html>");
            } else {
                colHideHpBars = -1;
                colRemoveTargetShadow = -1;
            }
            if (showContestColumns) {
                colContestEffect = addColumn(cols, "<html><center>Contest<br>Effect</center></html>");
                colContestType = addColumn(cols, "<html><center>Contest<br>Type</center></html>");
            } else {
                colContestEffect = -1;
                colContestType = -1;
            }
            columnNames = java.util.Collections.unmodifiableList(cols);
        }

        private int addColumn(List<String> cols, String name) {
            int idx = cols.size();
            cols.add(name);
            return idx;
        }

        @Override
        public int getRowCount() {
            return movesList.size();
        }

        @Override
        public int getColumnCount() {
            return columnNames.size();
        }

        @Override
        public String getColumnName(int column) {
            return columnNames.get(column);
        }

        @Override
        public Class<?> getColumnClass(int col) {
            if (col == colId) {
                return Integer.class;
            }
            if (col == colName || col == colCategory || col == colType || col == colTarget) {
                return String.class;
            }
            if (isFlagColumn(col)) {
                return Boolean.class;
            }
            return Integer.class;
        }

        @Override
        public boolean isCellEditable(int row, int col) {
            return col > colId;
        }

        @Override
        public Object getValueAt(int row, int col) {
            if (row >= movesList.size()) {
                return defaultValueForColumn(col);
            }

            Move move = movesList.get(row);
            if (move == null) {
                return defaultValueForColumn(col);
            }

            if (col == colId) {
                return move.number;
            }
            if (col == colName) {
                return move.name != null ? move.name : "";
            }
            if (col == colEffect) {
                return move.effectIndex;
            }
            if (col == colCategory) {
                return getCategoryName(move.category);
            }
            if (col == colPower) {
                return move.power;
            }
            if (col == colType) {
                return move.type != null ? move.type.name() : "";
            }
            if (col == colAccuracy) {
                return (int) move.hitratio;
            }
            if (col == colPp) {
                return move.pp;
            }
            if (col == colEffectChance) {
                return move.secondaryEffectChance;
            }
            if (col == colTarget) {
                return getTargetName(move.target);
            }
            if (col == colPriority) {
                return move.priority;
            }
            if (col == colMakesContact) {
                return move.makesContact;
            }
            if (col == colBlockedByProtect) {
                return move.isProtectedFromProtect;
            }
            if (col == colMagicCoat) {
                return move.isMagicCoatAffected;
            }
            if (col == colSnatch) {
                return move.isSnatchAffected;
            }
            if (col == colMirrorMove) {
                return move.isMirrorMoveAffected;
            }
            if (col == colKingsRock) {
                return move.isFlinchMove;
            }
            if (col == colHideHpBars && supportsExtendedMoveFlags) {
                return move.hidesHpBars;
            }
            if (col == colRemoveTargetShadow && supportsExtendedMoveFlags) {
                return move.removesTargetShadow;
            }
            if (col == colContestEffect && showContestColumns) {
                return move.contestEffect;
            }
            if (col == colContestType && showContestColumns) {
                return move.contestType;
            }
            return defaultValueForColumn(col);
        }

        private Object defaultValueForColumn(int col) {
            if (col == colId) {
                return 0;
            }
            if (col == colName || col == colCategory || col == colType || col == colTarget) {
                return "";
            }
            if (isFlagColumn(col)) {
                return false;
            }
            return 0;
        }

        @Override
        public void setValueAt(Object val, int row, int col) {
            if (row >= movesList.size()) {
                return;
            }
            Move move = movesList.get(row);
            if (move == null) {
                return;
            }

            try {
                if (col == colName) {
                    move.name = val.toString();
                } else if (col == colEffect) {
                    move.effectIndex = parseInt(val);
                } else if (col == colCategory) {
                    move.category = parseCategoryName(val.toString());
                } else if (col == colPower) {
                    move.power = parseBoundedInt(val, 0, 255);
                } else if (col == colType) {
                    if (val != null && !val.toString().isEmpty()) {
                        move.type = Type.valueOf(val.toString());
                    }
                } else if (col == colAccuracy) {
                    move.hitratio = parseBoundedInt(val, 0, 255);
                } else if (col == colPp) {
                    move.pp = parseBoundedInt(val, 0, 255);
                } else if (col == colEffectChance) {
                    move.secondaryEffectChance = parseInt(val);
                } else if (col == colTarget) {
                    move.target = parseTargetName(val.toString());
                } else if (col == colPriority) {
                    move.priority = (byte) parseInt(val);
                } else if (col == colMakesContact) {
                    move.makesContact = parseBoolean(val);
                } else if (col == colBlockedByProtect) {
                    move.isProtectedFromProtect = parseBoolean(val);
                } else if (col == colMagicCoat) {
                    move.isMagicCoatAffected = parseBoolean(val);
                } else if (col == colSnatch) {
                    move.isSnatchAffected = parseBoolean(val);
                } else if (col == colMirrorMove) {
                    move.isMirrorMoveAffected = parseBoolean(val);
                } else if (col == colKingsRock) {
                    move.isFlinchMove = parseBoolean(val);
                } else if (col == colHideHpBars && supportsExtendedMoveFlags) {
                    move.hidesHpBars = parseBoolean(val);
                } else if (col == colRemoveTargetShadow && supportsExtendedMoveFlags) {
                    move.removesTargetShadow = parseBoolean(val);
                } else if (col == colContestEffect && showContestColumns) {
                    move.contestEffect = parseInt(val);
                } else if (col == colContestType && showContestColumns) {
                    move.contestType = parseInt(val);
                }
                fireTableCellUpdated(row, col);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        public boolean isFlagColumn(int column) {
            if (column == colMakesContact || column == colBlockedByProtect || column == colMagicCoat
                    || column == colSnatch || column == colMirrorMove || column == colKingsRock) {
                return true;
            }
            if (!supportsExtendedMoveFlags) {
                return false;
            }
            return column == colHideHpBars || column == colRemoveTargetShadow;
        }

        public int getPreferredWidthForColumn(int column) {
            if (column == colEffect) return 240;
            if (column == colCategory) return 140;
            if (column == colPower) return 90;
            if (column == colType) return 140;
            if (column == colAccuracy) return 90;
            if (column == colPp) return 90;
            if (column == colEffectChance) return 140;
            if (column == colTarget) return 200;
            if (column == colPriority) return 90;
            if (column == colMakesContact || column == colBlockedByProtect || column == colMagicCoat
                    || column == colSnatch || column == colMirrorMove) {
                return 140;
            }
            if (column == colKingsRock) {
                return 160;
            }
            if (column == colHideHpBars || column == colRemoveTargetShadow) {
                return 140;
            }
            if ((column == colContestEffect && showContestColumns)
                    || (column == colContestType && showContestColumns)) {
                return 110;
            }
            return 120;
        }

        public int getCategoryColumnIndex() {
            return colCategory;
        }

        public int getTypeColumnIndex() {
            return colType;
        }

        public int getTargetColumnIndex() {
            return colTarget;
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
