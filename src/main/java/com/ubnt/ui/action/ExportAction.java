package com.ubnt.ui.action; //@date 08.12.2022

import com.ubnt.discovery.UbntResourceBundle;

import javax.swing.*;
import java.awt.event.ActionEvent;

public class ExportAction extends AbstractAction {

    public ExportAction() {
        super(UbntResourceBundle.getString("action.export"));
        putValue(Action.SMALL_ICON, UbntResourceBundle.getResourceIcon("/com/ubnt/icons/export.svg"));
        setEnabled(false);
    }

    /**
     * Invoked when an action occurs.
     *
     * @param e the event to be processed
     */
    @Override
    public void actionPerformed(ActionEvent e) {

    }
}
