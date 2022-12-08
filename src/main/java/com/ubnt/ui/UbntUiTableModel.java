package com.ubnt.ui; //@date 06.12.2022

import com.ubnt.net.IDiscoveryListener;
import com.ubnt.net.IUbntService;
import com.ubnt.net.IpInfo;

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
        rows     = new LinkedList<>();
    }

    /**
     * Returns false.  This is the default implementation for all cells.
     *
     * @param rowIndex the row being queried
     * @param columnIndex the column being queried
     * @return false
     */
    @Override
    public boolean isCellEditable(int rowIndex, int columnIndex) {
        return false;
    }


    /**
     * @return whether the tools is scanning for services at the moment.
     */
    public synchronized boolean isScanning() {
        return scanning;
    }

    /**
     * Sets whether the tools is scanning for services at the moment.
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
            IUbntService cached = getCachedService(service, services);
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
     * @param service the service to add
     * @param fireUpdate whether an update should be fired
     */
    private synchronized void insert(IUbntService service, boolean fireUpdate) {
        if (service == null) {
            return;
        }

        IUbntService cached = getCachedService(service, rows);
        if (cached == null) {
            rows.add(service);
            if (!fireUpdate) return;

            int insertedRow = rows.size() - 1;
            fireTableRowsInserted(insertedRow, insertedRow);
        } else {
            int index = rows.indexOf(cached);
            rows.set(index, service);
            if (!fireUpdate) return;

            fireTableRowsInserted(index, index);
        }
    }

    /**
     * Clears all cached services.
     */
    public synchronized void clearAll() {
        services.clear();
    }

    /**
     * Returns a cached service by comparing the MAC-Address.
     *
     * @param service the service to lookup
     * @param list the service cache
     * @return {@code null} if none was found or the discovered service
     */
    private IUbntService getCachedService(final IUbntService service,
                                          List<IUbntService> list) {

        String              hardwareAddress = null;
        IUbntService.Record record          = service.get(IUbntService.IPINFO);

        if (record != null) {
            hardwareAddress = ((IpInfo) record.getPayload()).getMAC();
        }
        for (IUbntService cached : list) {
            record = cached.get(IUbntService.IPINFO);
            if (record != null && hardwareAddress != null) {
                if (((IpInfo) record.getPayload()).getMAC().equals(hardwareAddress)) {
                    return cached;
                }
            }
        }
        return null;
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
    public int getRowCount() {
        return services.size();
    }
}
