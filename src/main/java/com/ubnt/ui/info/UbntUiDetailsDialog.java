package com.ubnt.ui.info; //@date 09.12.2022

import com.ubnt.discovery.UbntResourceBundle;
import com.ubnt.net.IUbntService;

import javax.swing.*;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.awt.event.*;

/**
 * Abstract dialog class that provides a way to create dialog frames with
 * different global actions and a customizable content.
 * <p>
 * All actions should be created by overriding the {@link #createToolBarActions()}
 * method, because they will be added automatically to the main toolbar of
 * this dialog. The center component is created via {@link #createBody()}
 * and won't be added if the method returns {@code null}.
 * <p>
 * This dialog is designed to be non-disposable, therefore it will be invisible
 * upon closing it. Because it would cost much more resources to create new
 * dialog frames each time service details should be displayed, the service's
 * attributed are loaded via {@link #loadService(IUbntService)}.
 *
 * @see UbntServiceInfoDialog
 */
public abstract class UbntUiDetailsDialog extends JDialog {

    /**
     * Simple string for displaying unknown content.
     */
    protected static final String UNKNOWN =
            UbntResourceBundle.getString("label.unknown");

    /**
     * The service which details should be displayed by this frame.
     */
    protected IUbntService service;

    /**
     * All actions are added to this {@link JToolBar} instead of creating a
     * {@link JButton} for each action.
     */
    protected JToolBar actionToolBar;

    /**
     * The body created by subclasses via {@link #createBody()}
     */
    protected Component body;

    /**
     * An array storing all actions of this dialog.
     */
    private Action[] actions;

    /**
     * The main content panel.
     */
    private JPanel content;

    /**
     * Private action handler implementing the closing feature.
     */
    private Handler handler;

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
    public UbntUiDetailsDialog(Frame owner, String title, boolean modal) {
        super(owner, title, modal);
        setupComponents();
        installComponents();
        installActions();
        installToolBarActions();
    }

    /**
     * Sets up all main components.
     */
    protected void setupComponents() {
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        content = new JPanel(new BorderLayout());

        actionToolBar = new JToolBar();
        actionToolBar.setBorder(new LineBorder(actionToolBar.getBackground().darker()));

        body = createBody();
    }

    /**
     * Installs all main components as well as the created body component.
     */
    protected void installComponents() {
        if (body != null) {
            content.add(body, BorderLayout.CENTER);
        }

        content.add(actionToolBar, BorderLayout.NORTH);
        Dimension dimension = new Dimension(560, 320);
        content.setPreferredSize(dimension);
        setContentPane(content);
        setSize(dimension);
        pack();
    }

    /**
     * Creates the body component for this dialog.
     *
     * @return the footer component.
     */
    protected Component createBody() {
        return null;
    }

    /**
     * Installs all global context actions that are not associated with the
     * toolbar.
     */
    protected void installActions() {
        KeyStroke keyStroke = KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0);
        JRootPane pane      = getRootPane();

        pane.registerKeyboardAction(handler = new Handler(), keyStroke, JComponent.WHEN_IN_FOCUSED_WINDOW);
        addWindowListener(handler);
    }

    /**
     * Installs all toolbar actions.
     */
    protected void installToolBarActions() {
        this.actions = createToolBarActions();

        if (actions != null && actionToolBar != null) {
            for (Action action : this.actions) {
                // A null value indicates that we should add a Separator to
                // the toolbar
                if (action == null) {
                    actionToolBar.addSeparator();
                } else {
                    actionToolBar.add(action);
                }
            }
        }
    }

    /**
     * Creates actions that should be added to the {@link #actionToolBar}. Note
     * that individual elements of the returned array can be {@code null} to
     * indicate that a separator should be added.
     *
     * @return the actions to install
     */
    protected Action[] createToolBarActions() {
        return new Action[0];
    }

    /**
     * Loads the given service to display it.
     *
     * @param service the service to display
     */
    public void loadService(IUbntService service) {
        this.service = service;
    }

    /**
     * Shows or hides this {@code Dialog} depending on the value of parameter
     * {@code b}.
     *
     * @param b if {@code true}, makes the {@code Dialog} visible,
     *         otherwise hides the {@code Dialog}.
     * @see Dialog#isModal
     */
    @Override
    public void setVisible(boolean b) {
        setLocationRelativeTo(getOwner());
        super.setVisible(b);
    }

    /**
     * Returns the service that gets displayed.
     *
     * @return the service
     */
    public IUbntService getService() {
        return service;
    }

    // private class to handle closing events.
    private class Handler extends WindowAdapter implements ActionListener {

        /**
         * Invoked when an action occurs.
         *
         * @param e the event to be processed
         */
        @Override
        public void actionPerformed(ActionEvent e) {
            windowClosing(null);
        }

        /**
         * Invoked when a window is in the process of being closed.
         * The close operation can be overridden at this point.
         *
         * @param e ignored
         */
        @Override
        public void windowClosing(WindowEvent e) {
            UbntUiDetailsDialog.this.setVisible(false);
            loadService(null);
        }
    }
}
