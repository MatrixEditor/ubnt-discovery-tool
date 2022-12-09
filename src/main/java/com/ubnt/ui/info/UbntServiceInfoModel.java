package com.ubnt.ui.info; //@date 09.12.2022

import com.ubnt.discovery.UbntResourceBundle;
import com.ubnt.net.IUbntService;
import com.ubnt.net.IUbntService.Record;

import javax.swing.table.AbstractTableModel;
import java.util.*;

class UbntServiceInfoModel extends AbstractTableModel {

    // This map is just to decrease the amount of calls to the
    // Record#getTypeName() method
    private final Map<Integer, String> rowNames = new HashMap<>();

    private final String[] columns = {
            UbntResourceBundle.getString("table.info.column.type"),
            UbntResourceBundle.getString("table.info.column.value")
    };

    private final List<UbntServiceRecordRow> rows;

    public UbntServiceInfoModel() {
        rows = new LinkedList<>();
    }

    public synchronized void clear() {
        rows.clear();
        fireTableDataChanged();
    }

    public synchronized void sort(Comparator<UbntServiceRecordRow> comparator) {
        if (comparator != null) {
            rows.sort(comparator);
            fireTableStructureChanged();
        }
    }

    public synchronized void load(final IUbntService service) {
        if (service == null) {
            return;
        }

        for (Record record : service) {
            // guaranteed non null value
            String typename = rowNames.get(record.getType());
            if (typename == null) {
                typename = record.getTypeName();
                rowNames.put(record.getType(), typename);
            }

            rows.add(new UbntServiceRecordRow(typename, record));
        }
        fireTableDataChanged();
    }

    /**
     * Returns the value for the cell at <code>columnIndex</code> and
     * <code>rowIndex</code>.
     *
     * @param rowIndex the row whose value is to be queried
     * @param columnIndex the column whose value is to be queried
     * @return the value Object at the specified cell
     */
    @Override
    public synchronized Object getValueAt(int rowIndex, int columnIndex) {
        if (rowIndex < getRowCount() && columnIndex <= 1) {
            Map.Entry<String, Record> entry = rows.get(rowIndex);

            return columnIndex == 0 ? entry.getKey() : entry.getValue().getPayload();
        }
        return UbntUiDetailsDialog.UNKNOWN;
    }

    /**
     * Returns the number of rows in the model. A
     * <code>JTable</code> uses this method to determine how many rows it
     * should display.  This method should be quick, as it
     * is called frequently during rendering.
     *
     * @return the number of rows in the model
     * @see #getColumnCount
     */
    @Override
    public synchronized int getRowCount() {
        return rows.size();
    }

    /**
     * Returns the number of columns in the model. A
     * <code>JTable</code> uses this method to determine how many columns it
     * should create and display by default.
     *
     * @return the number of columns in the model
     * @see #getRowCount
     */
    @Override
    public int getColumnCount() {
        // static value, therefore this method does not have to be
        // executed in a synchronized context
        return columns.length;
    }

    /**
     * Returns a default name for the column.
     *
     * @param column the column being queried
     * @return a string containing the default name of <code>column</code>
     */
    @Override
    public String getColumnName(int column) {
        return columns[column];
    }

}
