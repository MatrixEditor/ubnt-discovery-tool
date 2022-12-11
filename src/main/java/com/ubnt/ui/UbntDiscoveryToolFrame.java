package com.ubnt.ui; //@date 07.12.2022

import com.ubnt.discovery.QueryScheduler;
import com.ubnt.discovery.UbntDiscoveryTool;
import com.ubnt.discovery.UbntResourceBundle;
import com.ubnt.net.QueryServer;
import com.ubnt.ui.action.*;
import com.ubnt.ui.info.UbntServiceInfoDialog;
import com.ubnt.ui.info.UbntUiDetailsDialog;
import jdk.jfr.Description;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;
import javax.swing.border.LineBorder;
import javax.swing.border.TitledBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.TableModel;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

import static com.ubnt.discovery.UbntResourceBundle.*;

public class UbntDiscoveryToolFrame extends JFrame
        implements QueryScheduler.ScheduleListener {

    /**
     * The main table displaying all discovered services.
     * <p>
     * <b>NOTE:</b> this field is set to public for internal usage. That
     * state will be removed soon.
     */
    public UbntTable table;

    /**
     * Simple listener wrapper.
     */
    protected Handler handler;

    /**
     * The text field with a filter function
     */
    private JTextField textFieldSearch;

    /**
     * The label displaying an info text.
     */
    private JLabel helpLabel;

    /**
     * The scan action button.
     */
    private JButton scanButton;

    /**
     * THe clear action button.
     */
    private JButton clearButton;

    /**
     * The exit action button.
     */
    private JButton exitButton;

    /**
     * A global instance of the scan action used by the {@link #toolBar} and
     * the {@link #scanButton}.
     */
    private Action scanAction;

    /**
     * A global instance of the exit action used by the {@link #toolBar} and
     * the {@link #exitButton}.
     */
    private Action exitAction;

    /**
     * The default icon for the scan action if no scan is running.
     */
    private ImageIcon scanReadyIcon;

    /**
     * The default icon if a scan is running in background.
     */
    private ImageIcon scanRunningIcon;

    /**
     * The main tool bar storing the button actions.
     */
    private JToolBar toolBar;

    /**
     * A global instance of the clear action used by the {@link #toolBar} and
     * the {@link #clearButton}.
     */
    private DelegateAction clearAction;

    /**
     * See {@link OpenInfoAction} for more details.
     */
    private Action infoAction;

    /**
     * The details frame covering details about every service.
     */
    private UbntUiDetailsDialog detailsDialog;

    /**
     * See {@link ExportAction} class for more details.
     */
    private ExportAction exportAction;

    /**
     * See {@link ImportAction} class for more details.
     */
    private ImportAction importAction;

    /**
     * The global service model.
     */
    private UbntServiceTableModel model;

    /**
     * The toolbar's progress indicator.
     */
    private JProgressBar progressBar;

    /**
     * Creates the main frame for the {@code UbntDiscoveryTool}.
     *
     * @param title the frame's title
     */
    public UbntDiscoveryToolFrame(final String title) {
        super(title);
        // Do not close automatically, this action will be delegated
        // via the Handler class
        setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);

        setup();
        initComponents();
    }

    /**
     * Creates a {@link JLabel} from the given resource bundle key.
     *
     * @param key the resource bundle key
     * @return a new {@link JLabel}
     * @see UbntResourceBundle#getString(String)
     */
    private static JLabel createLabel(String key) {
        return new JLabel(getString(key));
    }

    /**
     * Sets up basic variables of this frame.
     */
    protected void setup() {
        model = new UbntServiceTableModel();
        for (QueryServer server : UbntDiscoveryTool.getServers()) {
            server.addListener(model);
        }

        handler = new Handler();
        detailsDialog = new UbntServiceInfoDialog(this, null, true);

        table = new UbntTable(model);
        table.setAutoResizeMode(JTable.AUTO_RESIZE_LAST_COLUMN);
        table.addMouseListener(handler);
        textFieldSearch = new JTextField(18);
        textFieldSearch.getDocument().addDocumentListener(handler);

        helpLabel   = createLabel("table.services.help");
        scanAction  = new ScanAction(table);
        clearAction = new DelegateAction(getString("action.clear"));
        clearAction.addListener(new ClearAction(table));
        clearAction.putValue(Action.SHORT_DESCRIPTION, getString("action.clear.tooltip"));

        exitAction = new ExitAction(this);
        infoAction = new OpenInfoAction(table, detailsDialog);
        exportAction = new ExportAction(model, this);
        importAction = new ImportAction(model, this, progressBar = new JProgressBar());

        ImageIcon image = getResourceIcon("/com/ubnt/icons/ubnt-tool-icon_64.svg");
        if (image != null) {
            setIconImage(image.getImage());
        }

        scanReadyIcon   = getResourceIcon("/com/ubnt/icons/reload.svg");
        scanRunningIcon = getResourceIcon("/com/ubnt/icons/reload_active.svg");

        if (scanReadyIcon != null) {
            scanAction.putValue(Action.SMALL_ICON, scanReadyIcon);
        }

        ImageIcon exitIcon = getResourceIcon("/com/ubnt/icons/exit.svg");
        if (exitIcon != null) {
            exitAction.putValue(Action.SMALL_ICON, exitIcon);
        }

        ImageIcon clearIcon = getResourceIcon("/com/ubnt/icons/clear.svg");
        if (clearIcon != null) {
            clearAction.putValue(Action.SMALL_ICON, clearIcon);
        }

        scanButton  = new JButton(scanAction);
        clearButton = new JButton(clearAction);
        exitButton  = new JButton(exitAction);

        // The icons will be used on the tool bar
        scanButton.setIcon(null);
        clearButton.setIcon(null);
        exitButton.setIcon(null);
    }

    /**
     * Sets up all graphical components.
     */
    protected void initComponents() {
        toolBar = new JToolBar();
        toolBar.setBorder(new LineBorder(toolBar.getBackground().darker()));

        Action[] actions = {
          scanAction, clearAction, infoAction, null,
                importAction, exportAction, null,
          exitAction
        };

        for (Action action : actions) {
            if (action == null) toolBar.addSeparator();
            else toolBar.add(action);
        }
        toolBar.addSeparator();
        toolBar.add(progressBar);

        JPanel helpContext = new JPanel(new FlowLayout());
        helpContext.add(helpLabel);

        JButton[] actionButtons = {
                scanButton, clearButton, exitButton
        };
        JPanel actionContext = new JPanel(new FlowLayout());

        int size = 0;
        for (JButton button : actionButtons) {
            if (button.getPreferredSize().width * 2 > size) {
                size = button.getPreferredSize().width * 2;
            }
            actionContext.add(button);
        }

        for (JButton button : actionButtons) {
            Dimension preferredSize = button.getPreferredSize();
            preferredSize.width = size;
            button.setPreferredSize(preferredSize);
        }

        JPanel tableContext = new JPanel(new BorderLayout(0, 0));

        JScrollPane view = new JScrollPane(table);
        view.getViewport().setOpaque(false);
        tableContext.add(view, BorderLayout.CENTER);

        JPanel countContext = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JLabel countLabel   = new JLabel("0");
        countContext.add(createLabel("label.count"));
        countContext.add(countLabel);
        table.getModel().addTableModelListener(new ServiceCountListener(countLabel));

        JPanel searchContext = new JPanel(new FlowLayout(FlowLayout.CENTER));
        searchContext.add(createLabel("label.search"));
        searchContext.add(textFieldSearch);

        final JPanel content = new JPanel(new GridBagLayout());
        content.add(searchContext,
                    GridBagUtil.gbc().fill(0).gridXY(0, 0).finish());
        content.add(countContext,
                    GridBagUtil.gbc().fill(0).anchor(22).gridXY(2, 0).finish());
        content.add(Box.createHorizontalGlue(),
                    GridBagUtil.gbc().gridXY(1, 0).weightX(3.0)
                               .fill(2).anchor(10).widthXY(0, 1).finish());

        tableContext.add(content, BorderLayout.NORTH);

        final JPanel centerContext = new JPanel(new BorderLayout(0, 0));
        centerContext.add(tableContext, BorderLayout.CENTER);

        Border titled = new TitledBorder(getString("table.services.title"));
        Border border = new CompoundBorder(
                titled, BorderFactory.createEmptyBorder(0, 5, 5, 5)
        );
        centerContext.setBorder(border);

        final JPanel actionPane = new JPanel();
        actionPane.setLayout(new BoxLayout(actionPane, BoxLayout.Y_AXIS));
        actionPane.add(helpContext);
        actionPane.add(actionContext);

        JPanel contentPane = new JPanel(new BorderLayout());
        JPanel wrapper     = new JPanel(new BorderLayout());
        wrapper.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        wrapper.add(actionPane, BorderLayout.SOUTH);
        wrapper.add(centerContext, BorderLayout.CENTER);
        contentPane.add(wrapper, BorderLayout.CENTER);
        contentPane.add(toolBar, BorderLayout.NORTH);

        setContentPane(contentPane);
    }

    /**
     * Invoked when a second has expired.
     *
     * @param finished whether the {@link QueryScheduler} has finished
     * @param second the current second
     */
    @Override
    public void nextSecond(boolean finished, long second) {
        EventQueue.invokeLater(() -> {
            if (finished) {
                model.setScanning(false);
                scanButton.setText(getString("button.scan"));
                if (scanAction != null) {
                    scanAction.putValue(Action.SMALL_ICON, scanReadyIcon);
                }
            } else {
                scanButton.setText(format("button.scanning", second / 1000));
                if (scanRunningIcon != null) {
                    scanAction.putValue(Action.SMALL_ICON, scanRunningIcon);
                }
            }
            if (scanAction != null) {
                scanAction.setEnabled(finished);
            }
            scanButton.setEnabled(finished);

            scanButton.setIcon(null);
            toolBar.repaint();
        });
    }

    /**
     * Listener for counting the discovered services.
     */
    private static class ServiceCountListener implements TableModelListener {

        private final JLabel countLabel;

        private ServiceCountListener(JLabel countLabel) {this.countLabel = countLabel;}

        /**
         * This fine grain notification tells listeners the exact range
         * of cells, rows, or columns that changed.
         *
         * @param e a {@code TableModelEvent} to notify listener that a table model
         *         has changed
         */
        @Override
        public void tableChanged(TableModelEvent e) {
            TableModel model = (TableModel) e.getSource();
            countLabel.setText(String.valueOf(model.getRowCount()));
        }
    }

    /**
     * Action multicast handler.
     */
    private class Handler implements DocumentListener, MouseListener {

        /**
         * Invoked when the mouse button has been clicked (pressed
         * and released) on a component.
         *
         * @param e the event to be processed
         */
        @Override
        public void mouseClicked(MouseEvent e) {
            if (e.getClickCount() == 2) {
                infoAction.actionPerformed(null);
            }
        }

        /**
         * Invoked when a mouse button has been pressed on a component.
         *
         * @param e the event to be processed
         */
        @Override
        public void mousePressed(MouseEvent e) {

        }

        /**
         * Invoked when a mouse button has been released on a component.
         *
         * @param e the event to be processed
         */
        @Override
        public void mouseReleased(MouseEvent e) {

        }

        /**
         * Invoked when the mouse enters a component.
         *
         * @param e the event to be processed
         */
        @Override
        public void mouseEntered(MouseEvent e) {

        }

        /**
         * Invoked when the mouse exits a component.
         *
         * @param e the event to be processed
         */
        @Override
        public void mouseExited(MouseEvent e) {

        }

        /**
         * Gives notification that there was an insert into the document.  The
         * range given by the DocumentEvent bounds the freshly inserted region.
         *
         * @param e the document event
         */
        @Override
        public void insertUpdate(DocumentEvent e) {

        }

        /**
         * Gives notification that a portion of the document has been
         * removed.  The range is given in terms of what the view last
         * saw (that is, before updating sticky positions).
         *
         * @param e the document event
         */
        @Override
        public void removeUpdate(DocumentEvent e) {

        }

        /**
         * Gives notification that an attribute or set of attributes changed.
         *
         * @param e the document event
         */
        @Override
        public void changedUpdate(DocumentEvent e) {

        }


    }
}
