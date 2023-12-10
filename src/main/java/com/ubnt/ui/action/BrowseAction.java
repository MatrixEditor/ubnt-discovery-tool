package com.ubnt.ui.action; //@date 09.12.2022

import com.ubnt.net.IUbntService;
import com.ubnt.ui.UbntTextField;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.text.MessageFormat;
import java.util.function.Supplier;

import static com.ubnt.discovery.UbntResourceBundle.getResourceIcon;
import static com.ubnt.discovery.UbntResourceBundle.getString;

public class BrowseAction extends AbstractAction {

    /**
     * Format string that contains the command that will be executed on
     * command line when trying to open an url. Note that this format
     * will depend on the used OS.
     */
    private static final String openBrowserFormat;

    static {
        String platform = System.getProperty("os.name");
        if ("linux".equalsIgnoreCase(platform)
                || "freebsd".equalsIgnoreCase(platform)) {
            // maybe add a check here and change the command to
            // 'sensible-browser' if xdg-open is not installed.
            openBrowserFormat = "xdg-open {0}";
        }
        else if (platform == null || !platform.startsWith("Mac")) {
            openBrowserFormat = "cmd.exe /C \"start {0}\"";
        }
        else {
            openBrowserFormat = "open {0}";
        }
    }

    /**
     * Method reference to query the {@link IUbntService}.
     */
    private final Supplier<IUbntService> serviceSupplier;

    /**
     * Instantiates a new Browse action.
     *
     * @param serviceSupplier the service supplier
     */
    public BrowseAction(Supplier<IUbntService> serviceSupplier) {
        super(getString("action.open.browser"));
        this.serviceSupplier = serviceSupplier;
        putValue(Action.SMALL_ICON, getResourceIcon("/com/ubnt/icons/web.svg"));
    }

    /**
     * Invoked when an action occurs.
     *
     * @param e the event to be processed
     */
    @Override
    public void actionPerformed(ActionEvent e) {
        if (serviceSupplier != null) {
            IUbntService service = serviceSupplier.get();

            if (service != null) {
                String uri = service.getSourceAddress().getHostAddress();

                // by now (should be queried via method reference)
                int port = 80;

                // Basically executing the following command via a terminal
                // cmd.exe /C "start http[s]://<ip>" on windows
                //
                // This call should be implemented soon:
                // open(uri);
            }
        }
    }

    /**
     * Opens the given url by executing {@link #openBrowserFormat}.
     *
     * @param url the url to open
     */
    private void open(String url) {
        try {
            Runtime runtime = Runtime.getRuntime();
            runtime.exec(MessageFormat.format(openBrowserFormat, url));
        }
        catch (Exception e) {
            String errorMsg = "Error while open link '"+url+"'";
            JOptionPane.showMessageDialog(null, new UbntTextField(errorMsg));
        }
    }
}
