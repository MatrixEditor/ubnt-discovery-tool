package com.ubnt.ui.info; //@date 09.12.2022

import com.ubnt.discovery.UbntResourceBundle;
import com.ubnt.net.IUbntService;
import com.ubnt.net.IpInfo;
import com.ubnt.ui.action.BrowseAction;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.text.MessageFormat;
import java.util.Comparator;

/**
 * Basic implementation of the {@link UbntUiDetailsDialog} including different
 * ways to sort the service's attributes and to open the webUI.
 *
 * @see UbntUiDetailsDialog
 */
public class UbntServiceInfoDialog extends UbntUiDetailsDialog {

    /**
     * The service model that stores all records that should be displayed.
     */
    private UbntServiceInfoModel model;

    /**
     * The action that gets called when trying to navigate to the webUi.
     */
    private BrowseAction browseAction;

    /**
     * Sorts the contents of {@link #model} by the record's type value.
     */
    private SortAction sortByType;

    /**
     * Sorts the contents of {@link #model} by the record's type name.
     */
    private SortAction sortByName;

    /**
     * Sorts the contents of {@link #model} by their values.
     */
    private SortAction sortByValue;

    /**
     * Creates a {@code UbntUiDetailsDialog} by executing the setup and install
     * methods in the following order:
     * <ol>
     *     <li>{@link #setupComponents()}</li>
     *     <li>{@link #installComponents()}</li>
     *     <li>{@link #installActions()}</li>
     *     <li>{@link #installToolBarActions()}</li>
     * </ol>
     *
     * @param owner the {@code Frame} from which the dialog is displayed
     * @param title the {@code String} to display in the dialog's
     *         title bar
     * @param modal specifies whether dialog blocks user input to other top-level
     *         windows when shown. If {@code true}, the modality type property is set to
     *         {@code DEFAULT_MODALITY_TYPE} otherwise the dialog is modeless
     * @throws HeadlessException if {@code GraphicsEnvironment.isHeadless()}
     *                           returns {@code true}.
     */
    public UbntServiceInfoDialog(Frame owner, String title, boolean modal) {
        super(owner, title, modal);
        ImageIcon icon = UbntResourceBundle.getResourceIcon("/com/ubnt/icons/device_info_icon.svg");
        if (icon != null) {
            setIconImage(icon.getImage());
        }
    }

    /**
     * Creates the body component for this dialog.
     *
     * @return the footer component.
     */
    @Override
    protected Component createBody() {
        JTable table = new JTable(model = new UbntServiceInfoModel());

        table.setRowSelectionAllowed(true);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        sortByType = new SortAction(
                "action.sort.bytype", "sort_byType.svg",
                this::sortByTypeValue);

        sortByName = new SortAction(
                "action.sort.byname", "sort_byName.svg",
                this::sortByTypeName
        );

        sortByValue = new SortAction(
                "action.sort.byvalue", "sort_byValue.svg",
                this::sortByValue
        );

        return new JScrollPane(table);
    }

    /**
     * Loads the given service to display it.
     *
     * @param service the service to display
     */
    @Override
    public void loadService(IUbntService service) {
        super.loadService(service);
        model.clear();
        model.load(service);

        if (service != null) {
            String title = UbntResourceBundle.getString("table.info.title");

            IUbntService.Record info = service.get(IUbntService.IPINFO);
            IUbntService.Record name = service.get(IUbntService.HOSTNAME);
            if (info != null && name != null) {
                IpInfo ipInfo = (IpInfo) info.getPayload();
                title = MessageFormat.format(title, name.getPayload(), ipInfo.getMAC());
                setTitle(title);
            }

        } else {
            setTitle(null);
        }
    }

    /**
     * Creates actions that should be added to the {@link #actionToolBar}. Note
     * that individual elements of the returned array can be {@code null} to
     * indicate that a separator should be added.
     *
     * @return the actions to install
     */
    @Override
    protected Action[] createToolBarActions() {
        return new Action[]{
                browseAction = new BrowseAction(this::getService),
                null,
                sortByType,
                sortByName,
                sortByValue
        };
    }

    /**
     * Sorts the contents of {@link #model} by the record's type value.
     *
     * @param r0 first row
     * @param r1 second row
     * @return result of {@link Integer#compare(int, int)}
     */
    private int sortByTypeValue(UbntServiceRecordRow r0, UbntServiceRecordRow r1) {
        return Integer.compare(r0.getValue().getType(), r1.getValue().getType());
    }

    /**
     * Sorts the contents of {@link #model} by the record's type name.
     *
     * @param r0 first row
     * @param r1 second row
     * @return result of {@link String#compareTo(String)}
     */
    private int sortByTypeName(UbntServiceRecordRow r0, UbntServiceRecordRow r1) {
        return r0.getKey().compareTo(r1.getKey());
    }

    /**
     * Sorts the contents of {@link #model} by their values.
     *
     * @param r0 first row
     * @param r1 second row
     * @return result of {@link String#compareTo(String)}
     */
    private int sortByValue(UbntServiceRecordRow r0, UbntServiceRecordRow r1) {
        Object s  = r0.getValue().getPayload();
        Object s1 = r1.getValue().getPayload();

        String n  = s == null ? UNKNOWN : s.toString();
        String n0 = s1 == null ? UNKNOWN : s1.toString();
        return n.compareTo(n0);
    }

    /**
     * Simple action class that delegates the sorting mechanism of this
     * dialog.
     */
    private class SortAction extends AbstractAction {

        /**
         * Equivalent to a {@link RowSorter}.
         */
        private final Comparator<UbntServiceRecordRow> filter;

        public SortAction(String descKey, String iconPath,
                          Comparator<UbntServiceRecordRow> filter) {
            super(descKey);
            this.filter = filter;
            putValue(Action.SHORT_DESCRIPTION, UbntResourceBundle.getString(descKey));
            putValue(Action.SMALL_ICON, UbntResourceBundle.getResourceIcon("/com/ubnt/icons/" + iconPath));
        }

        /**
         * Invoked when an action occurs.
         *
         * @param e the event to be processed
         */
        @Override
        public void actionPerformed(ActionEvent e) {
            model.sort(filter);
        }
    }

}
