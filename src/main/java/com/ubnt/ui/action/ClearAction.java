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
        table.getRowSorter().setSortKeys(null);
        tableModel.setScanning(false);
        tableModel.clearAll();
        tableModel.reload();
    }
}
