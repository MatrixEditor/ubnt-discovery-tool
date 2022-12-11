package com.ubnt.ui.info; //@date 09.12.2022

import com.ubnt.discovery.UbntResourceBundle;
import com.ubnt.net.DefaultService;
import com.ubnt.net.IUbntService;
import com.ubnt.net.IUbntService.Record;

import javax.swing.table.AbstractTableModel;
import java.util.*;

/**
 * TableModel implementation for the {@link UbntServiceInfoDialog} dialog. This
 * model contains only two columns:
 * <ul>
 *     <li><b>Name:</b> the record's type name</li>
 *     <li><b>Value:</b> the record's value converted to string</li>
 * </ul>
 * In future versions of this class the conversion from {@link Record#getPayload()}
 * to {@code String} may be delegated to a {@code Converter} class.
 *
 * @see UbntServiceInfoDialog
 */
class UbntServiceInfoModel extends AbstractTableModel {

    /**
     * WebUi format string tha should be queried only once.
     */
    private static final String webuiFormat =
            UbntResourceBundle.getString("table.info.webui");

    /**
     * The columns of this model.
     */
    private final String[] columns = {
            UbntResourceBundle.getString("table.info.column.type"),
            UbntResourceBundle.getString("table.info.column.value")
    };

    /**
     * The rows of this model.
     */
    private final List<UbntServiceRecordRow> rows;

    /**
     * The displayed service.
     */
    private volatile IUbntService service;

    /**
     * Instantiates a new Ubnt service info model.
     */
    public UbntServiceInfoModel() {
        rows = new LinkedList<>();
    }

    /**
     * Clears the model's content.
     */
    public synchronized void clear() {
        rows.clear();
        fireTableDataChanged();
    }

    /**
     * Sorts the {@link #rows} with the given sorter.
     *
     * @param comparator the row sorter
     */
    public synchronized void sort(Comparator<UbntServiceRecordRow> comparator) {
        if (comparator != null) {
            rows.sort(comparator);
            fireTableStructureChanged();
        }
    }

    /**
     * Loads the service into this model.
     *
     * @param service the service to display
     */
    public synchronized void load(final IUbntService service) {
        this.service = service;
        if (service == null) {
            return;
        }

        for (Record record : service) {
            // guaranteed non null value
            String typename = record.getTypeName();
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

            Record record = entry.getValue();
            if (columnIndex == 0) {
                return entry.getKey();
            }
            if (record.getType() == IUbntService.WEB_UI) {
                int    port     = service.getWebUiPort();
                String protocol = service.getWebUiProtocol();

                return String.format(webuiFormat, port, protocol);
            } else if (record.getType() == IUbntService.UPTIME) {
                if (service instanceof DefaultService) {
                    return ((DefaultService) service).getUptime();
                }
            }

            return entry.getValue().getPayload();
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
