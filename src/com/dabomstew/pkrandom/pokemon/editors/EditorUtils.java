package com.dabomstew.pkrandom.pokemon.editors;

import com.dabomstew.pkromio.gamedata.Species;
import javax.swing.*;
import javax.swing.event.ChangeListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.filechooser.FileFilter;
import javax.swing.table.TableModel;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.ComboBoxModel;
import javax.swing.text.JTextComponent;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.AWTEventListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.HierarchyEvent;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * Utility methods for editor panels
 */
public class EditorUtils {

    private static final String SUPPRESS_FROZEN_SYNC_KEY = "EditorUtils.suppressFrozenSync";
    private static final String FULL_ROW_SELECTION_ACTIVE_KEY = "EditorUtils.fullRowSelectionActive";
    private static final String COMBOBOX_SEARCH_INSTALLED_KEY = "EditorUtils.comboSearchInstalled";
    private static volatile boolean comboBoxGlobalSearchEnabled;

    static {
        try {
            enableGlobalComboBoxSearch();
        } catch (HeadlessException ignored) {
            // Ignore in headless mode.
        }
    }

    /**
     * Returns the species name combined with its forme suffix, if any.
     */
    public static String speciesNameWithSuffix(Species species) {
        if (species == null) {
            return "";
        }
        String baseName = species.getName() != null ? species.getName() : "";
        String suffix = species.getFormeSuffix();
        if (suffix != null && !suffix.isEmpty()) {
            return baseName + suffix;
        }
        return baseName;
    }

    private static int formeDepth(Species species) {
        int depth = 0;
        Species current = species;
        while (current != null && !current.isBaseForme()) {
            depth++;
            current = current.getBaseForme();
        }
        return depth;
    }

    /**
     * Returns a display-ready name for species, indenting and prefixing formes.
     */
    public static String formatSpeciesDisplayName(Species species) {
        if (species == null) {
            return "";
        }
        String nameWithSuffix = speciesNameWithSuffix(species);
        int depth = formeDepth(species);
        if (depth <= 0) {
            return nameWithSuffix;
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 1; i < depth; i++) {
            sb.append("  ");
        }
        sb.append("-> ").append(nameWithSuffix);
        return sb.toString();
    }

    /**
     * Formats a display name suitable for combo boxes that include the species id.
     */
    public static String formatSpeciesDisplayNameWithId(Species species) {
        if (species == null) {
            return "0: (None)";
        }
        return species.getNumber() + ": " + formatSpeciesDisplayName(species);
    }

    /**
     * Parses the leading species id from a string formatted as "<id>: <name>".
     * Returns -1 if the id cannot be read.
     */
    public static int parseLeadingSpeciesId(String value) {
        if (value == null) {
            return -1;
        }
        String trimmed = value.trim();
        int colon = trimmed.indexOf(':');
        String token = colon >= 0 ? trimmed.substring(0, colon).trim() : trimmed;
        if (token.isEmpty()) {
            return -1;
        }
        try {
            return Integer.parseInt(token);
        } catch (NumberFormatException ex) {
            return -1;
        }
    }

    /**
     * Export table data to CSV file
     */
    public static void exportTableToCSV(Component parent, TableModel tableModel, String sheetName) {
        JFileChooser fc = new JFileChooser(System.getProperty("user.dir"));
        fc.setDialogTitle("Export " + sheetName + " to CSV");
        fc.setAcceptAllFileFilterUsed(false);
        fc.setFileSelectionMode(JFileChooser.FILES_ONLY);

        fc.addChoosableFileFilter(new FileFilter() {
            @Override
            public boolean accept(File f) {
                return f.isDirectory() || f.getName().toLowerCase().endsWith(".csv");
            }

            @Override
            public String getDescription() {
                return "CSV file (*.csv)";
            }
        });

        int returnVal = fc.showSaveDialog(parent);

        if (returnVal == JFileChooser.APPROVE_OPTION) {
            File selected = fc.getSelectedFile();
            String path = selected.getAbsolutePath();
            if (!path.toLowerCase().endsWith(".csv")) {
                path = path + ".csv";
            }

            try (BufferedWriter writer = new BufferedWriter(new FileWriter(path))) {
                // Write header row
                for (int col = 0; col < tableModel.getColumnCount(); col++) {
                    String colName = tableModel.getColumnName(col);
                    // Remove HTML tags from column names
                    colName = colName.replaceAll("<[^>]*>", " ").replaceAll("\\s+", " ").trim();
                    writer.write(escapeCSV(colName));
                    if (col < tableModel.getColumnCount() - 1) {
                        writer.write(",");
                    }
                }
                writer.write("\n");

                // Write data rows
                for (int row = 0; row < tableModel.getRowCount(); row++) {
                    for (int col = 0; col < tableModel.getColumnCount(); col++) {
                        Object value = tableModel.getValueAt(row, col);
                        if (value != null) {
                            writer.write(escapeCSV(value.toString()));
                        }
                        if (col < tableModel.getColumnCount() - 1) {
                            writer.write(",");
                        }
                    }
                    writer.write("\n");
                }

                JOptionPane.showMessageDialog(parent,
                        "Successfully exported to:\n" + path,
                        "Export Complete",
                        JOptionPane.INFORMATION_MESSAGE);
            } catch (IOException e) {
                e.printStackTrace();
                JOptionPane.showMessageDialog(parent,
                        "Error exporting CSV: " + e.getMessage(),
                        "Export Error",
                        JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    /**
     * Escape CSV special characters
     */
    private static String escapeCSV(String value) {
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }

    /**
     * Create a styled button for editor toolbars
     */
    public static JButton createStyledButton(String text, Color bgColor) {
        JButton button = new JButton(text) {
            @Override
            protected void paintComponent(Graphics g) {
                g.setColor(getBackground());
                g.fillRect(0, 0, getWidth(), getHeight());
                super.paintComponent(g);
            }
        };

        button.setFont(new Font("Segoe UI", Font.BOLD, 12));
        button.setForeground(Color.WHITE);
        button.setBackground(bgColor);
        button.setFocusPainted(false);
        button.setOpaque(false);
        button.setContentAreaFilled(false);
        button.setBorderPainted(true);
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));

        button.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(bgColor.darker(), 2),
                BorderFactory.createEmptyBorder(8, 16, 8, 16)));

        return button;
    }

    /**
     * Create a styled toggle button for editor toolbars
     */
    public static JToggleButton createStyledToggleButton(String text, Color bgColor) {
        JToggleButton button = new JToggleButton(text) {
            @Override
            protected void paintComponent(Graphics g) {
                if (isSelected()) {
                    g.setColor(bgColor.darker());
                } else {
                    g.setColor(getBackground());
                }
                g.fillRect(0, 0, getWidth(), getHeight());
                super.paintComponent(g);
            }
        };

        button.setFont(new Font("Segoe UI", Font.BOLD, 12));
        button.setForeground(Color.WHITE);
        button.setBackground(bgColor);
        button.setFocusPainted(false);
        button.setOpaque(false);
        button.setContentAreaFilled(false);
        button.setBorderPainted(true);
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));

        button.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(bgColor.darker(), 2),
                BorderFactory.createEmptyBorder(8, 16, 8, 16)));

        return button;
    }

    /**
     * Prompt the user to select a CSV file and return the parsed data.
     */
    public static CsvData chooseCsvFile(Component parent, String sheetName) {
        JFileChooser fc = new JFileChooser(System.getProperty("user.dir"));
        fc.setDialogTitle("Import " + sheetName + " from CSV");
        fc.setAcceptAllFileFilterUsed(false);
        fc.setFileSelectionMode(JFileChooser.FILES_ONLY);
        fc.addChoosableFileFilter(new FileFilter() {
            @Override
            public boolean accept(File f) {
                return f.isDirectory() || f.getName().toLowerCase().endsWith(".csv");
            }

            @Override
            public String getDescription() {
                return "CSV file (*.csv)";
            }
        });

        int result = fc.showOpenDialog(parent);
        if (result != JFileChooser.APPROVE_OPTION) {
            return null;
        }

        File selected = fc.getSelectedFile();
        List<String[]> rows = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(selected))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parsed = parseCsvLine(line);
                if (rows.isEmpty() && parsed.length > 0) {
                    parsed[0] = stripBom(parsed[0]);
                }
                rows.add(parsed);
            }
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(parent,
                    "Error reading CSV: " + ex.getMessage(),
                    "Import Error",
                    JOptionPane.ERROR_MESSAGE);
            return null;
        }

        if (rows.isEmpty()) {
            JOptionPane.showMessageDialog(parent,
                    "CSV file is empty.",
                    "Import Error",
                    JOptionPane.WARNING_MESSAGE);
            return null;
        }

        return new CsvData(selected, rows);
    }

    /**
     * Apply CSV data to a table model.
     *
     * @return number of rows that were updated
     */
    public static int applyCsvDataToTable(List<String[]> rows, TableModel model, boolean skipNonEditable) {
        if (rows == null || rows.size() <= 1) {
            throw new IllegalArgumentException("CSV file does not contain any data rows.");
        }

        int expectedColumns = model.getColumnCount();
        if (expectedColumns == 0) {
            throw new IllegalArgumentException("Target table has no columns.");
        }

        String[] header = rows.get(0);
        if (header.length != expectedColumns) {
            throw new IllegalArgumentException(
                    "Column count mismatch between CSV (" + header.length + ") and table (" + expectedColumns + ").");
        }

        int maxRows = Math.min(model.getRowCount(), rows.size() - 1);
        for (int rowIndex = 0; rowIndex < maxRows; rowIndex++) {
            String[] csvRow = rows.get(rowIndex + 1);
            if (csvRow.length != expectedColumns) {
                String[] adjusted = new String[expectedColumns];
                System.arraycopy(csvRow, 0, adjusted, 0, Math.min(csvRow.length, expectedColumns));
                csvRow = adjusted;
            }

            for (int colIndex = 0; colIndex < expectedColumns; colIndex++) {
                if (skipNonEditable && !model.isCellEditable(rowIndex, colIndex)) {
                    continue;
                }
                String rawValue = csvRow[colIndex] != null ? csvRow[colIndex] : "";
                Object coerced = coerceValue(rawValue, model.getColumnClass(colIndex));
                model.setValueAt(coerced, rowIndex, colIndex);
            }
        }

        return maxRows;
    }

    /**
     * Display a shared Find dialog and capture search options.
     */
    public static FindOptions showFindDialog(Component parent, FindOptions previousOptions) {
        JTextField queryField = new JTextField(previousOptions != null ? previousOptions.getQuery() : "");
        JCheckBox matchCase = new JCheckBox("Match case", previousOptions != null && previousOptions.isMatchCase());
        JCheckBox matchEntire = new JCheckBox("Match entire cell",
                previousOptions != null && previousOptions.isMatchEntireCell());

        JPanel panel = new JPanel(new GridLayout(0, 1, 0, 4));
        panel.add(new JLabel("Find what:"));
        panel.add(queryField);
        panel.add(matchCase);
        panel.add(matchEntire);

        int result = JOptionPane.showConfirmDialog(parent, panel, "Find", JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE);
        if (result != JOptionPane.OK_OPTION) {
            return null;
        }

        String query = queryField.getText();
        if (query == null || query.trim().isEmpty()) {
            JOptionPane.showMessageDialog(parent,
                    "Enter text to search for.",
                    "Find",
                    JOptionPane.INFORMATION_MESSAGE);
            return null;
        }

        return new FindOptions(query.trim(), matchCase.isSelected(), matchEntire.isSelected());
    }

    /**
     * Execute a find operation on the supplied table model.
     */
    public static boolean performFind(Component parent,
            JTable frozenTable,
            JTable mainTable,
            TableModel model,
            int frozenColumns,
            FindState state,
            FindOptions options) {
        if (options == null) {
            return false;
        }

        String query = options.getQuery();
        if (query == null || query.isEmpty()) {
            JOptionPane.showMessageDialog(parent,
                    "Enter text to search for.",
                    "Find",
                    JOptionPane.INFORMATION_MESSAGE);
            return false;
        }

        int rowCount = model.getRowCount();
        int columnCount = model.getColumnCount();
        if (rowCount == 0 || columnCount == 0) {
            JOptionPane.showMessageDialog(parent,
                    "There is no data to search.",
                    "Find",
                    JOptionPane.INFORMATION_MESSAGE);
            return false;
        }

        int totalCells = rowCount * columnCount;
        int startIndex = 0;
        if (state != null && state.canContinueWith(options)) {
            startIndex = state.getLastIndex(columnCount) + 1;
        }

        String normalizedQuery = options.isMatchCase() ? query : query.toLowerCase(Locale.ROOT);

        for (int offset = 0; offset < totalCells; offset++) {
            int linearIndex = (startIndex + offset) % totalCells;
            int row = linearIndex / columnCount;
            int col = linearIndex % columnCount;

            Object value = model.getValueAt(row, col);
            String text = value == null ? "" : value.toString();
            String compareText = options.isMatchCase() ? text : text.toLowerCase(Locale.ROOT);

            boolean match = options.isMatchEntireCell()
                    ? compareText.equals(normalizedQuery)
                    : compareText.contains(normalizedQuery);

            if (match) {
                if (state != null) {
                    state.update(options, row, col);
                }
                selectCell(frozenTable, mainTable, frozenColumns, row, col);
                return true;
            }
        }

        if (state != null) {
            state.update(options, -1, -1);
        }

        Toolkit.getDefaultToolkit().beep();
        JOptionPane.showMessageDialog(parent,
                "No matches found for \"" + query + "\".",
                "Find",
                JOptionPane.INFORMATION_MESSAGE);
        return false;
    }

    /**
     * Install case-insensitive type-ahead search support on a combo box.
     * Matching behavior mimics the pk3DS editors: the combo remains editable so the
     * user can type directly,
     * the dropdown stays open, and the closest matching entry is highlighted as
     * they type.
     */
    public static void installSearchableComboBox(JComboBox<?> comboBox) {
        if (comboBox == null) {
            return;
        }
        if (Boolean.TRUE.equals(comboBox.getClientProperty("EditorUtils.disableComboSearch"))) {
            return;
        }
        if (comboBox.getClientProperty(COMBOBOX_SEARCH_INSTALLED_KEY) != null) {
            return;
        }

        comboBox.setEditable(true);
        comboBox.putClientProperty("JComboBox.isTableCellEditor", Boolean.TRUE);
        Component editorComponent = comboBox.getEditor().getEditorComponent();
        if (!(editorComponent instanceof JTextComponent)) {
            comboBox.putClientProperty(COMBOBOX_SEARCH_INSTALLED_KEY, Boolean.TRUE);
            return;
        }

        ComboSearchSupport support = new ComboSearchSupport(comboBox, (JTextComponent) editorComponent);
        support.install();
        comboBox.putClientProperty(COMBOBOX_SEARCH_INSTALLED_KEY, support);
    }

    public static void enableGlobalComboBoxSearch() {
        if (comboBoxGlobalSearchEnabled || GraphicsEnvironment.isHeadless()) {
            return;
        }

        AWTEventListener listener = event -> {
            if (!(event instanceof HierarchyEvent)) {
                return;
            }
            HierarchyEvent hierarchyEvent = (HierarchyEvent) event;
            if ((hierarchyEvent.getChangeFlags() & HierarchyEvent.DISPLAYABILITY_CHANGED) == 0) {
                return;
            }
            Component component = hierarchyEvent.getComponent();
            if (!(component instanceof JComboBox<?>)) {
                return;
            }
            JComboBox<?> combo = (JComboBox<?>) component;
            if (combo.isDisplayable()) {
                installSearchableComboBox(combo);
            }
        };

        Toolkit.getDefaultToolkit().addAWTEventListener(listener, AWTEvent.HIERARCHY_EVENT_MASK);
        comboBoxGlobalSearchEnabled = true;
    }

    private static class ComboSearchSupport implements DocumentListener, FocusListener, ActionListener {
        private final JComboBox<?> comboBox;
        private final JTextComponent editor;
        private final List<Object> items = new ArrayList<>();
        private List<Object> currentItems;
        private boolean adjusting;
        private Object lastSelection;
        private String lastEditorText;
        private boolean userHasTyped;

        ComboSearchSupport(JComboBox<?> comboBox, JTextComponent editor) {
            this.comboBox = comboBox;
            this.editor = editor;

            ComboBoxModel<?> model = comboBox.getModel();
            for (int i = 0; i < model.getSize(); i++) {
                items.add(model.getElementAt(i));
            }

            currentItems = new ArrayList<>(items);
            lastSelection = comboBox.getSelectedItem();
            lastEditorText = editor.getText();
            userHasTyped = false;
        }

        void install() {
            editor.getDocument().addDocumentListener(this);
            editor.addFocusListener(this);
            comboBox.addActionListener(this);
        }

        @Override
        public void insertUpdate(DocumentEvent e) {
            handleDocumentChange();
        }

        @Override
        public void removeUpdate(DocumentEvent e) {
            handleDocumentChange();
        }

        @Override
        public void changedUpdate(DocumentEvent e) {
            handleDocumentChange();
        }

        private void handleDocumentChange() {
            if (adjusting) {
                return;
            }
            userHasTyped = true;
            SwingUtilities.invokeLater(() -> updateSelection(true));
        }

        private void updateSelection(boolean forcePopup) {
            if (adjusting) {
                return;
            }

            String query = editor.getText();
            if (query == null) {
                query = "";
            }
            String filterText = userHasTyped ? query : "";
            adjusting = true;
            updateModelForQuery(filterText);
            applyEditorText(query, query.length());
            if ((forcePopup || !(query.isEmpty())) && comboBox.isDisplayable()) {
                comboBox.setPopupVisible(true);
            }
            adjusting = false;
            lastEditorText = editor.getText();
        }

        private void selectItemIfChanged(Object item) {
            if (!Objects.equals(comboBox.getSelectedItem(), item)) {
                comboBox.setSelectedItem(item);
            }
        }

        private void applyEditorText(String text, int typedLength) {
            editor.setText(text);
            int len = text.length();
            int highlightStart = Math.max(0, Math.min(typedLength, len));
            if (highlightStart < len) {
                editor.setCaretPosition(highlightStart);
                editor.moveCaretPosition(len);
            } else {
                editor.setCaretPosition(len);
            }
        }

        @Override
        public void focusGained(FocusEvent e) {
            SwingUtilities.invokeLater(() -> updateSelection(true));
        }

        @Override
        public void focusLost(FocusEvent e) {
            SwingUtilities.invokeLater(this::commitSelection);
        }

        private void commitSelection() {
            if (adjusting) {
                return;
            }

            adjusting = true;
            Object selected = comboBox.getSelectedItem();
            if (selected == null) {
                selected = findExactMatch(editor.getText());
            }
            if (selected == null) {
                selected = lastSelection;
            } else {
                lastSelection = selected;
            }

            if (selected != null) {
                selectItemIfChanged(selected);
                applyEditorText(selected.toString(), selected.toString().length());
            } else {
                applyEditorText("", 0);
            }

            comboBox.setPopupVisible(false);
            restoreAllItems();
            adjusting = false;
            lastEditorText = editor.getText();
            userHasTyped = false;
        }

        private Object findExactMatch(String text) {
            if (text == null) {
                return null;
            }
            String lower = text.toLowerCase(Locale.ROOT);
            for (Object item : items) {
                if (item == null) {
                    if (lower.isEmpty()) {
                        return item;
                    }
                    continue;
                }
                String label = item.toString();
                if (label != null && label.toLowerCase(Locale.ROOT).equals(lower)) {
                    return item;
                }
            }
            return null;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            if (adjusting) {
                return;
            }
            Object selected = comboBox.getSelectedItem();
            adjusting = true;
            if (selected != null) {
                lastSelection = selected;
                applyEditorText(selected.toString(), selected.toString().length());
            } else {
                applyEditorText("", 0);
            }
            restoreAllItems();
            adjusting = false;
            lastEditorText = editor.getText();
            userHasTyped = false;
        }

        private void updateModelForQuery(String query) {
            List<Object> matches = getMatches(query);
            if (!matches.equals(currentItems)) {
                currentItems = new ArrayList<>(matches);
                @SuppressWarnings({ "rawtypes", "unchecked" })
                DefaultComboBoxModel model = new DefaultComboBoxModel(currentItems.toArray());
                comboBox.setModel(model);
            }
            comboBox.getEditor().setItem(query);
        }

        private List<Object> getMatches(String query) {
            if (query == null || query.isEmpty()) {
                return new ArrayList<>(items);
            }

            String lower = query.toLowerCase(Locale.ROOT);
            LinkedHashSet<Object> ordered = new LinkedHashSet<>();

            for (Object item : items) {
                if (item == null) {
                    continue;
                }
                String label = item.toString();
                if (label == null) {
                    continue;
                }
                if (label.toLowerCase(Locale.ROOT).startsWith(lower)) {
                    ordered.add(item);
                }
            }

            for (Object item : items) {
                if (item == null) {
                    continue;
                }
                String label = item.toString();
                if (label == null) {
                    continue;
                }
                String lowerLabel = label.toLowerCase(Locale.ROOT);
                if (!lowerLabel.startsWith(lower) && lowerLabel.contains(lower)) {
                    ordered.add(item);
                }
            }

            if (ordered.isEmpty()) {
                return new ArrayList<>(items);
            }

            return new ArrayList<>(ordered);
        }

        private void restoreAllItems() {
            if (currentItems == null || currentItems.size() == items.size()) {
                return;
            }
            currentItems = new ArrayList<>(items);
            @SuppressWarnings({ "rawtypes", "unchecked" })
            DefaultComboBoxModel model = new DefaultComboBoxModel(currentItems.toArray());
            comboBox.setModel(model);
            Object text = editor.getText();
            if (text != null) {
                comboBox.getEditor().setItem(text);
            }
            if (lastSelection != null) {
                selectItemIfChanged(lastSelection);
            }
            userHasTyped = false;
        }
    }

    private static void selectCell(JTable frozenTable, JTable mainTable, int frozenColumns, int row, int column) {
        if (mainTable != null) {
            runWithFrozenSyncSuppressed(mainTable,
                    () -> performSelectCell(frozenTable, mainTable, frozenColumns, row, column));
        } else {
            performSelectCell(frozenTable, null, frozenColumns, row, column);
        }
    }

    private static void performSelectCell(JTable frozenTable, JTable mainTable, int frozenColumns, int row,
            int column) {
        selectRow(frozenTable, row);
        selectRow(mainTable, row);

        if (column < frozenColumns && frozenTable != null) {
            selectFullRowColumns(frozenTable);
            scrollRowIntoView(frozenTable, row, column);
            if (mainTable != null) {
                if (mainTable.getColumnCount() > 0) {
                    selectFullRowColumns(mainTable);
                    scrollRowIntoView(mainTable, row, 0);
                } else {
                    clearColumnSelection(mainTable);
                }
                mainTable.requestFocusInWindow();
            }
            frozenTable.requestFocusInWindow();
        } else if (mainTable != null) {
            int mainColumn = column - frozenColumns;
            if (mainTable.getColumnCount() > 0) {
                selectColumn(mainTable, mainColumn);
                scrollRowIntoView(mainTable, row, mainColumn);
            } else {
                clearColumnSelection(mainTable);
            }
            if (frozenTable != null) {
                selectFullRowColumns(frozenTable);
                scrollRowIntoView(frozenTable, row, 0);
            }
            mainTable.requestFocusInWindow();
        }
    }

    private static void selectRow(JTable table, int row) {
        if (table == null || table.getRowCount() == 0) {
            return;
        }
        int safeRow = Math.max(0, Math.min(row, table.getRowCount() - 1));
        table.setRowSelectionInterval(safeRow, safeRow);
    }

    private static void selectColumn(JTable table, int column) {
        if (table == null) {
            return;
        }
        int columnCount = table.getColumnCount();
        if (columnCount <= 0) {
            table.getColumnModel().getSelectionModel().clearSelection();
            return;
        }
        int safeColumn = Math.max(0, Math.min(column, columnCount - 1));
        table.getColumnModel().getSelectionModel().setSelectionInterval(safeColumn, safeColumn);
    }

    private static void selectFullRowColumns(JTable table) {
        if (table == null) {
            return;
        }
        int columnCount = table.getColumnCount();
        if (columnCount <= 0) {
            table.getColumnModel().getSelectionModel().clearSelection();
            return;
        }
        table.getColumnModel().getSelectionModel().setSelectionInterval(0, columnCount - 1);
    }

    private static void clearColumnSelection(JTable table) {
        if (table == null) {
            return;
        }
        table.getColumnModel().getSelectionModel().clearSelection();
    }

    private static void scrollRowIntoView(JTable table, int row, int column) {
        if (table == null || table.getRowCount() == 0) {
            return;
        }
        int safeRow = Math.max(0, Math.min(row, table.getRowCount() - 1));
        int columnCount = table.getColumnCount();
        if (columnCount <= 0) {
            int rowHeight = table.getRowHeight(safeRow);
            Rectangle fallback = new Rectangle(0, safeRow * rowHeight, 1, rowHeight);
            table.scrollRectToVisible(fallback);
            return;
        }
        int safeColumn = Math.max(0, Math.min(column, columnCount - 1));
        Rectangle cellRect = table.getCellRect(safeRow, safeColumn, true);
        table.scrollRectToVisible(cellRect);
    }

    public static void installFrozenColumnSync(JTable frozenTable, JTable mainTable) {
        if (frozenTable == null || mainTable == null ||
                frozenTable.getColumnModel() == null || mainTable.getColumnModel() == null) {
            return;
        }

        setFullRowSelectionActive(frozenTable, false);
        ListSelectionModel frozenColumnModel = frozenTable.getColumnModel().getSelectionModel();
        ListSelectionModel mainColumnModel = mainTable.getColumnModel().getSelectionModel();

        mainTable.putClientProperty(SUPPRESS_FROZEN_SYNC_KEY, Boolean.FALSE);

        mainTable.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mousePressed(java.awt.event.MouseEvent e) {
                if (!isFullRowSelection(mainTable)) {
                    frozenColumnModel.clearSelection();
                    setFullRowSelectionActive(frozenTable, false);
                }
            }
        });

        mainColumnModel.addListSelectionListener(e -> {
            if (e.getValueIsAdjusting()) {
                return;
            }
            if (Boolean.TRUE.equals(mainTable.getClientProperty(SUPPRESS_FROZEN_SYNC_KEY))) {
                return;
            }
            if (!isFullRowSelection(mainTable)) {
                frozenColumnModel.clearSelection();
                setFullRowSelectionActive(frozenTable, false);
            }
        });
    }

    /**
     * Keeps two scroll panes (typically frozen + main tables) vertically aligned.
     * Shares the vertical scroll bar model and mirrors viewport Y positions so the
     * frozen columns stay in sync even at scroll extremes.
     */
    public static void linkVerticalScrollBars(JScrollPane frozenScrollPane, JScrollPane mainScrollPane) {
        if (frozenScrollPane == null || mainScrollPane == null) {
            return;
        }
        JScrollBar mainVertical = mainScrollPane.getVerticalScrollBar();
        JScrollBar frozenVertical = frozenScrollPane.getVerticalScrollBar();
        if (mainVertical == null || frozenVertical == null) {
            return;
        }
        frozenVertical.setModel(mainVertical.getModel());

        JViewport mainViewport = mainScrollPane.getViewport();
        JViewport frozenViewport = frozenScrollPane.getViewport();
        if (mainViewport == null || frozenViewport == null) {
            return;
        }

        final boolean[] updating = new boolean[1];
        Runnable syncFromMain = () -> {
            if (updating[0]) {
                return;
            }
            Point mainPos = mainViewport.getViewPosition();
            Point frozenPos = frozenViewport.getViewPosition();
            if (frozenPos.y != mainPos.y) {
                updating[0] = true;
                frozenViewport.setViewPosition(new Point(frozenPos.x, mainPos.y));
                updating[0] = false;
            }
        };

        mainViewport.addChangeListener(e -> syncFromMain.run());
        frozenViewport.addChangeListener(e -> {
            if (updating[0]) {
                return;
            }
            Point frozenPos = frozenViewport.getViewPosition();
            Point mainPos = mainViewport.getViewPosition();
            if (mainPos.y != frozenPos.y) {
                updating[0] = true;
                mainViewport.setViewPosition(new Point(mainPos.x, frozenPos.y));
                updating[0] = false;
            }
        });
        syncFromMain.run();
    }

    /**
     * Adds a spacer beneath the frozen table so the horizontal scrollbar visually spans the full width
     * when it appears.
     */
    public static void addHorizontalScrollbarSpacer(JPanel frozenContainer, JScrollPane mainScrollPane) {
        if (frozenContainer == null || mainScrollPane == null) {
            return;
        }
        JScrollBar horizontalBar = mainScrollPane.getHorizontalScrollBar();
        if (horizontalBar == null) {
            return;
        }
        JPanel spacer = new JPanel();
        spacer.setOpaque(true);
        spacer.setBackground(horizontalBar.getBackground());
        int barHeight = horizontalBar.getPreferredSize().height;
        spacer.setPreferredSize(new Dimension(0, barHeight));
        spacer.setVisible(horizontalBar.isVisible());

        horizontalBar.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentShown(ComponentEvent e) {
                spacer.setVisible(true);
            }

            @Override
            public void componentHidden(ComponentEvent e) {
                spacer.setVisible(false);
            }
        });

        frozenContainer.add(spacer, BorderLayout.SOUTH);
    }

    public static void runWithFrozenSyncSuppressed(JTable mainTable, Runnable action) {
        if (action == null) {
            return;
        }
        if (mainTable == null) {
            action.run();
            return;
        }
        Object previous = mainTable.getClientProperty(SUPPRESS_FROZEN_SYNC_KEY);
        mainTable.putClientProperty(SUPPRESS_FROZEN_SYNC_KEY, Boolean.TRUE);
        try {
            action.run();
        } finally {
            if (previous == null) {
                mainTable.putClientProperty(SUPPRESS_FROZEN_SYNC_KEY, Boolean.FALSE);
            } else {
                mainTable.putClientProperty(SUPPRESS_FROZEN_SYNC_KEY, previous);
            }
        }
    }

    private static boolean isFullRowSelection(JTable table) {
        if (table == null) {
            return false;
        }
        int columnCount = table.getColumnCount();
        if (columnCount <= 0) {
            return false;
        }
        ListSelectionModel columnSelectionModel = table.getColumnModel().getSelectionModel();
        if (columnSelectionModel == null || columnSelectionModel.isSelectionEmpty()) {
            return false;
        }
        return columnSelectionModel.getMinSelectionIndex() == 0
                && columnSelectionModel.getMaxSelectionIndex() == columnCount - 1;
    }

    public static void setFullRowSelectionActive(JTable frozenTable, boolean active) {
        if (frozenTable == null) {
            return;
        }
        frozenTable.putClientProperty(FULL_ROW_SELECTION_ACTIVE_KEY, active);
    }

    public static boolean isFullRowSelectionActive(JTable frozenTable) {
        return frozenTable != null && Boolean.TRUE.equals(frozenTable.getClientProperty(FULL_ROW_SELECTION_ACTIVE_KEY));
    }

    public static void installHeaderViewportSync(JScrollPane scrollPane) {
        if (scrollPane == null) {
            return;
        }
        JViewport viewport = scrollPane.getViewport();
        JViewport columnHeader = scrollPane.getColumnHeader();
        if (viewport == null || columnHeader == null) {
            return;
        }

        ChangeListener listener = e -> syncHeaderViewport(scrollPane);
        viewport.addChangeListener(listener);

        JScrollBar horizontal = scrollPane.getHorizontalScrollBar();
        if (horizontal != null) {
            horizontal.addAdjustmentListener(e -> syncHeaderViewport(scrollPane));
        }

        syncHeaderViewport(scrollPane);
    }

    private static void syncHeaderViewport(JScrollPane scrollPane) {
        if (scrollPane == null) {
            return;
        }
        JViewport viewport = scrollPane.getViewport();
        JViewport columnHeader = scrollPane.getColumnHeader();
        if (viewport == null || columnHeader == null) {
            return;
        }
        Point viewPosition = viewport.getViewPosition();
        Point headerPosition = columnHeader.getViewPosition();
        if (headerPosition.x != viewPosition.x) {
            columnHeader.setViewPosition(new Point(viewPosition.x, headerPosition.y));
        }
    }

    private static Object coerceValue(String rawValue, Class<?> columnClass) {
        if (columnClass == null) {
            return rawValue;
        }

        if (columnClass == Boolean.class || columnClass == boolean.class) {
            return parseBooleanCell(rawValue);
        }

        return rawValue;
    }

    private static boolean parseBooleanCell(String value) {
        if (value == null) {
            return false;
        }
        String normalized = value.trim().toLowerCase(Locale.ROOT);
        return normalized.equals("true") || normalized.equals("1") || normalized.equals("yes");
    }

    private static String[] parseCsvLine(String line) {
        List<String> values = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;

        for (int i = 0; i < line.length(); i++) {
            char ch = line.charAt(i);
            if (ch == '"') {
                if (inQuotes && i + 1 < line.length() && line.charAt(i + 1) == '"') {
                    current.append('"');
                    i++;
                } else {
                    inQuotes = !inQuotes;
                }
            } else if (ch == ',' && !inQuotes) {
                values.add(current.toString());
                current.setLength(0);
            } else {
                current.append(ch);
            }
        }
        values.add(current.toString());

        return values.toArray(new String[0]);
    }

    private static String stripBom(String value) {
        if (value != null && !value.isEmpty() && value.charAt(0) == '\ufeff') {
            return value.substring(1);
        }
        return value;
    }

    /**
     * Encapsulates CSV data selected by the user.
     */
    public static class CsvData {
        private final File file;
        private final List<String[]> rows;

        public CsvData(File file, List<String[]> rows) {
            this.file = file;
            this.rows = rows;
        }

        public File getFile() {
            return file;
        }

        public List<String[]> getRows() {
            return rows;
        }
    }

    /**
     * Find dialog configuration.
     */
    public static class FindOptions {
        private final String query;
        private final boolean matchCase;
        private final boolean matchEntireCell;

        public FindOptions(String query, boolean matchCase, boolean matchEntireCell) {
            this.query = query;
            this.matchCase = matchCase;
            this.matchEntireCell = matchEntireCell;
        }

        public String getQuery() {
            return query;
        }

        public boolean isMatchCase() {
            return matchCase;
        }

        public boolean isMatchEntireCell() {
            return matchEntireCell;
        }

        private boolean sameSearch(FindOptions other) {
            return other != null
                    && matchCase == other.matchCase
                    && matchEntireCell == other.matchEntireCell
                    && query.equals(other.query);
        }
    }

    /**
     * Stores state for repeated find operations.
     */
    public static class FindState {
        private FindOptions lastOptions;
        private int lastRow = -1;
        private int lastCol = -1;

        public boolean canContinueWith(FindOptions options) {
            return lastOptions != null
                    && lastRow >= 0
                    && lastCol >= 0
                    && lastOptions.sameSearch(options);
        }

        public int getLastIndex(int totalColumns) {
            return lastRow * totalColumns + lastCol;
        }

        public void update(FindOptions options, int row, int col) {
            this.lastOptions = options;
            this.lastRow = row;
            this.lastCol = col;
        }

        public FindOptions getLastOptions() {
            return lastOptions;
        }
    }
}
