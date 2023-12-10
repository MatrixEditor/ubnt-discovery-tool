package com.ubnt.ui; //@date 06.12.2022

import com.ubnt.net.IDiscoveryListener;
import com.ubnt.net.IUbntService;
import com.ubnt.net.UbntIOUtilities;

import javax.swing.table.AbstractTableModel;
import java.util.LinkedList;
import java.util.List;

/**
 * Abstract table model with {@link IUbntService}s as its rows.
 */
public abstract class UbntUiTableModel
        extends AbstractTableModel implements IDiscoveryListener {

    /**
     * The discovered services as a list == rows in this table model
     */
    protected List<IUbntService> services;

    /**
     * The displayed services.
     */
    protected List<IUbntService> rows;

    /**
     * Tells whether the tools scans for services.
     */
    private volatile boolean scanning;

    /**
     * Creates a new table model.
     */
    public UbntUiTableModel() {
        services = new LinkedList<>();
        rows = new LinkedList<>();
    }

    /**
     * Returns false.  This is the default implementation for all cells.
     *
     * @param rowIndex    the row being queried
     * @param columnIndex the column being queried
     * @return false
     */
    @Override
    public boolean isCellEditable(int rowIndex, int columnIndex) {
        return false;
    }

    /**
     * @return whether the tools are scanning for services at the moment.
     */
    public synchronized boolean isScanning() {
        return scanning;
    }

    /**
     * Sets whether the tools are scanning for services at the moment.
     *
     * @param scanning {@code true} or {@code false}
     */
    public synchronized void setScanning(boolean scanning) {
        this.scanning = scanning;
    }

    /**
     * Clears all rows and synchronizes them with the cached services.
     */
    public synchronized void reload() {
        rows.clear();
        for (IUbntService service : services) {
            insert(service, false);
        }
        fireTableDataChanged();
    }

    /**
     * Returns the {@link IUbntService} at the specified row.
     *
     * @param row the row to fetch
     * @return the service at the given row
     */
    public synchronized IUbntService getServiceAt(int row) {
        IUbntService service = null;
        if (row >= 0 && row < rows.size()) {
            service = rows.get(row);
        }
        return service;
    }

    /**
     * Invoked when a new service has been discovered.
     *
     * @param service the new service
     */
    @Override
    public synchronized void onServiceLocated(IUbntService service) {
        if (service != null && isScanning()) {
            IUbntService cached = UbntIOUtilities.getCachedService(service, services);
            if (cached == null) {
                services.add(service);
            } else {
                services.set(services.indexOf(cached), service);
            }
            insert(service, true);
        }
    }

    /**
     * Inserts the given service at the end of this table model.
     *
     * @param service    the service to add
     * @param fireUpdate whether an update should be fired
     */
    private synchronized void insert(IUbntService service, boolean fireUpdate) {
        if (service == null) {
            return;
        }

        IUbntService cached = UbntIOUtilities.getCachedService(service, rows);
        if (cached == null) {
            rows.add(service);
            if (!fireUpdate) return;

            int insertedRow = rows.size() - 1;
            fireTableRowsInserted(insertedRow, insertedRow);
        } else {
            int index = rows.indexOf(cached);
            rows.set(index, service);
            if (!fireUpdate) return;

            try {
                if (index != rows.size()) {
                    fireTableRowsInserted(index, index);
                }
            } catch (ArrayIndexOutOfBoundsException e) {
                // Ignore this error. NPE will be displayed as the rows'
                // values to indicate an error occurred. THis happens when
                // a filter is applied to the table during a scan.
            }
        }
    }

    /**
     * Clears all cached services.
     */
    public synchronized void clearAll() {
        services.clear();
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
}
