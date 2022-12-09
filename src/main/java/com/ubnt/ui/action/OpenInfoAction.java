package com.ubnt.ui.action; //@date 09.12.2022

import com.ubnt.discovery.UbntResourceBundle;
import com.ubnt.net.IUbntService;
import com.ubnt.ui.UbntTable;
import com.ubnt.ui.UbntUiTableModel;
import com.ubnt.ui.info.UbntUiDetailsDialog;

import javax.swing.*;
import java.awt.event.ActionEvent;

import static com.ubnt.discovery.UbntResourceBundle.getResourceIcon;

public class OpenInfoAction extends AbstractAction {

    private final UbntTable table;

    private final UbntUiDetailsDialog detailsDialog;

    public OpenInfoAction(UbntTable table, UbntUiDetailsDialog detailsDialog) {
        super(UbntResourceBundle.getString("button.open.info"));
        this.table         = table;
        this.detailsDialog = detailsDialog;

        putValue(SHORT_DESCRIPTION, getValue(NAME));
        putValue(SMALL_ICON, getResourceIcon("/com/ubnt/icons/show_details.svg"));

    }

    /**
     * Invoked when an action occurs.
     *
     * @param e the event to be processed
     */
    @Override
    public void actionPerformed(ActionEvent e) {
        int row = table.getSelectedRow();
        if (row != -1) {
            row = table.convertRowIndexToModel(row);

            UbntUiTableModel model   = (UbntUiTableModel) table.getModel();
            IUbntService     service = model.getServiceAt(row);

            if (service != null && detailsDialog != null) {
                // Although, this is unlikely to happen, a check should be done
                // to make sure there are no display errors.
                if (!detailsDialog.isVisible()) {
                    detailsDialog.loadService(service);
                    detailsDialog.setVisible(true);
                }
            }
        }
    }
}
