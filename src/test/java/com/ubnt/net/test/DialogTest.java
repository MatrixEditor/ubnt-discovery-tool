package com.ubnt.net.test; //@date 09.12.2022

import com.formdev.flatlaf.FlatLightLaf;
import com.ubnt.ui.info.UbntServiceDetails;
import com.ubnt.ui.info.UbntUiDetailsDialog;

public class DialogTest {

    public static void main(String[] args) {
        FlatLightLaf.setup();
        UbntUiDetailsDialog dialog = new UbntServiceDetails(null, "Title", true);
        dialog.setVisible(true);

    }
}
