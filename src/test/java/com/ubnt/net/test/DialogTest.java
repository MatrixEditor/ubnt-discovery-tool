package com.ubnt.net.test; //@date 09.12.2022

import com.ubnt.ui.info.UbntServiceInfoDialog;
import com.ubnt.ui.info.UbntUiDetailsDialog;

public class DialogTest {

    public static void main(String[] args) {
        //FlatLightLaf.setup();
        UbntUiDetailsDialog dialog = new UbntServiceInfoDialog(null, "Title", true);
        dialog.setVisible(true);

    }
}
