package com.ubnt.ui; //@date 07.12.2022

import com.ubnt.net.IUbntService;

import javax.swing.*;
import javax.swing.table.TableRowSorter;
import java.awt.event.MouseEvent;

/**
 * Simple table wrapper storing an extra variable with the {@link UbntUiTableModel}
 * instance.
 *
 * @see UbntUiTableModel
 */
public class UbntTable extends JTable {

    /**
     * Whether the sorting is supported.
     */
    private static final boolean isSortingSupported;

    static {
        String version = System.getProperty("java.version");
        // Row Filters are available since 1.6, so we have to check against
        // the currently running java version.
        isSortingSupported = version.compareTo("1.6") >= 0;
    }

    /**
     * the configured model.
     */
    protected UbntUiTableModel model;

    /**
     * Constructs a <code>JTable</code> that is initialized with
     * <code>dm</code> as the data model, a default column model,
     * and a default selection model.
     *
     * @param model the data model for the table
     * @see #createDefaultColumnModel
     * @see #createDefaultSelectionModel
     */
    public UbntTable(UbntUiTableModel model) {
        super(model);
        setup(model);
    }

    /**
     * Sets up this table with the given model.
     *
     * @param model the configured model
     */
    protected void setup(UbntUiTableModel model) {
        this.model = model;

        if (model instanceof UbntServiceTableModel) {
            //different actions
        }

        if (isSortingSupported) {
            TableRowSorter<UbntUiTableModel> sorter =
                    new TableRowSorter<>(model);

            setRowSorter(sorter);
        }
    }

    @Override
    public String getToolTipText(MouseEvent event) {
        int rowAtPoint = rowAtPoint(event.getPoint());
        if (rowAtPoint >= 0 && isSortingSupported) {
            rowAtPoint = getRowSorter().convertRowIndexToModel(rowAtPoint);
        }

        StringBuilder text    = new StringBuilder("<html>");
        IUbntService  service = model.getServiceAt(rowAtPoint);
        if (service == null) {
            return super.getToolTipText(event);
        }

        for (IUbntService.Record record : service) {
            String name = record.getTypeName();
            // This prevents the tooltip to display raw content
            if (!name.equals(String.valueOf(record.getType()))) {
                text.append("<p>").append(name)
                    .append(": ").append(record.getPayload()).append("</p>");
            }
        }

        return text.append("</html>").toString();
    }
}
