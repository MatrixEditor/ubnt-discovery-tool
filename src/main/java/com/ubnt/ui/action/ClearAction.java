package com.ubnt.ui.action; //@date 08.12.2022

import com.ubnt.ui.UbntTable;
import com.ubnt.ui.UbntUiTableModel;

import javax.swing.*;
import java.awt.event.ActionEvent;

/**
 * Clears the content of the linked {@link UbntUiTableModel}.
 */
public class ClearAction extends AbstractAction {

    /**
     * The linked model.
     */
    private final UbntUiTableModel tableModel;

    /**
     * The linked table (used to clear row filter)
     */
    private final UbntTable table;


    /**
     * Instantiates a new Clear action.
     *
     * @param table the table
     */
    public ClearAction(UbntTable table) {
        this.tableModel = (UbntUiTableModel) table.getModel();
        this.table      = table;
    }

    /**
     * Invoked when an action occurs.
     *
     * @param e the event to be processed
     */
    @Override
    public void actionPerformed(ActionEvent e) {
        final int count = tableModel.getRowCount();

        table.getRowSorter().setSortKeys(null);
        tableModel.setScanning(false);
        tableModel.clearAll();
        tableModel.reload();

        // REVISION: This call is done because it reduces the occupied memory
        // by this application. In test cases this call reduces up to 25Mb of
        // space after a big list of services got deleted. Of course, this
        // call is unnecessary if there are only a few services that got
        // removed.
        if (count > 10) System.gc();
    }
}
