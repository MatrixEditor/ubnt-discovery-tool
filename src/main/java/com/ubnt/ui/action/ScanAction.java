package com.ubnt.ui.action; //@date 07.12.2022

import com.ubnt.discovery.UbntDiscoveryTool;
import com.ubnt.discovery.UbntResourceBundle;
import com.ubnt.ui.UbntTable;
import com.ubnt.ui.UbntUiTableModel;

import javax.swing.*;
import java.awt.event.ActionEvent;

public class ScanAction extends AbstractAction {

    /**
     * The linked model.
     */
    private final UbntUiTableModel model;

    /**
     * The linked table (used to clear row filter)
     */
    private final UbntTable table;

    /**
     * System runtime in milliseconds.
     */
    private long time;

    public ScanAction(UbntTable table) {
        super(UbntResourceBundle.getString("action.scan"));
        this.model = (UbntUiTableModel) table.getModel();
        this.table = table;
        this.time  = 0L;
        putValue(Action.SHORT_DESCRIPTION, UbntResourceBundle.getString("action.scan.tooltip"));
    }

    /**
     * Invoked when an action occurs.
     *
     * @param e the event to be processed
     */
    @Override
    public void actionPerformed(ActionEvent e) {
        if (System.currentTimeMillis() > time + 5500) {
            model.clearAll();
        }

        // Because the fireTableRowInserted() method in UbntUiTableModel causes
        // ArrayIndexOutOfBoundsExceptions in the RowSorter class, we have to
        // specify that all sorting keys should be cleared.
        table.getRowSorter().setSortKeys(null);
        model.setScanning(true);
        time = System.currentTimeMillis();
        UbntDiscoveryTool.scheduleScan();
    }
}
