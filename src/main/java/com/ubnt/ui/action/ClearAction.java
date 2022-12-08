package com.ubnt.ui.action; //@date 08.12.2022

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
     * Instantiates a new Clear action.
     *
     * @param tableModel the table model
     */
    public ClearAction(UbntUiTableModel tableModel) {
        this.tableModel = tableModel;
    }

    /**
     * Invoked when an action occurs.
     *
     * @param e the event to be processed
     */
    @Override
    public void actionPerformed(ActionEvent e) {
        tableModel.setScanning(false);
        tableModel.clearAll();
        tableModel.reload();
    }
}
