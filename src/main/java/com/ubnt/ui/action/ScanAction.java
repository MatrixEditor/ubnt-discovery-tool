package com.ubnt.ui.action; //@date 07.12.2022

import com.ubnt.discovery.UbntDiscoveryTool;
import com.ubnt.discovery.UbntResourceBundle;
import com.ubnt.ui.UbntUiTableModel;

import javax.swing.*;
import java.awt.event.ActionEvent;

public class ScanAction extends AbstractAction {

    private final UbntUiTableModel model;

    private long time;

    public ScanAction(UbntUiTableModel model) {
        super(UbntResourceBundle.getString("action.scan"));
        this.model = model;
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
        model.setScanning(true);
        time = System.currentTimeMillis();
        UbntDiscoveryTool.scheduleScan();
    }
}
