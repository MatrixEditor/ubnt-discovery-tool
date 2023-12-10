package com.ubnt.ui; //@date 07.12.2022

import com.ubnt.discovery.UbntResourceBundle;
import com.ubnt.net.IUbntService;
import com.ubnt.net.IpInfo;

/**
 * Main frame table model.
 */
public class UbntServiceTableModel extends UbntUiTableModel {

    /**
     * The column names.
     */
    private final String[] columns = {
            UbntResourceBundle.getString("table.services.column.product"),
            UbntResourceBundle.getString("table.services.column.ip"),
            UbntResourceBundle.getString("table.services.column.mac"),
            UbntResourceBundle.getString("table.services.column.hostname"),
            UbntResourceBundle.getString("table.services.column.status"),
            UbntResourceBundle.getString("table.services.column.fwversion"),
    };

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
        return columns.length;
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
        IUbntService service = getServiceAt(rowIndex);
        if (service == null) {
            return "NPE";
        }

        IUbntService.Record record;
        switch (columnIndex) {
            case 0: // model:
                return service.getModelName();

            case 1: //ip
            case 2: //mac
                record = service.get(IUbntService.IPINFO);
                break;

            case 3: // hostname:
                record = service.get(IUbntService.HOSTNAME);
                break;

            case 4: // status:
                return service.getStatus();

            case 5: // firmware
                record = service.get(IUbntService.FW_VERSION);
                break;

            default:
                record = null;
                break;
        }

        Object payload = record == null ? null : record.getPayload();
        if (payload instanceof IpInfo) {
            IpInfo info = (IpInfo) payload;
            return columnIndex == 1 ? info.getIP() : info.getMAC();
        }

        return record == null ? "" : record.getPayload().toString();
    }
}
