package com.ubnt.ui.action; //@date 07.12.2022

import com.ubnt.discovery.UbntDiscoveryTool;
import com.ubnt.discovery.UbntResourceBundle;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;

/**
 * Exits this application
 */
public class ExitAction extends AbstractAction
        implements WindowListener {

    /**
     * THe {@link com.ubnt.ui.UbntDiscoveryToolFrame}..
     */
    private final JFrame wrapper;


    /**
     * Instantiates a new Exit action.
     *
     * @param wrapper the wrapper
     */
    public ExitAction(JFrame wrapper) {
        super(UbntResourceBundle.getString("action.exit"));
        this.wrapper = wrapper;
        wrapper.addWindowListener(this);
        putValue(Action.SHORT_DESCRIPTION, UbntResourceBundle.getString("action.exit.tooltip"));
    }

    /**
     * Invoked when an action occurs.
     *
     * @param e the event to be processed
     */
    @Override
    public void actionPerformed(ActionEvent e) {
        int result = 0;
        boolean selected = false;
        if (UbntDiscoveryTool.getProperty("exit.confirm", "true")
                             .equals("true")) {
            JCheckBox box = new JCheckBox(
                    UbntResourceBundle.getString("action.exit.confirm.askagain")
            );
            result = JOptionPane.showConfirmDialog(
                    wrapper, new Object[]{
                            UbntResourceBundle.getString("action.exit.confirm.message"),
                            box
                    }, UbntResourceBundle.getString("action.exit.confirm.title"),
                    JOptionPane.YES_NO_OPTION);

            selected = box.isSelected();
        }

        if (result == JOptionPane.YES_OPTION) {
            UbntDiscoveryTool.put("exit.confirm", Boolean.toString(selected));
            System.exit(0);
        }
    }

    /**
     * Invoked the first time a window is made visible.
     *
     * @param e the event to be processed
     */
    @Override
    public void windowOpened(WindowEvent e) {

    }

    /**
     * Invoked when the user attempts to close the window
     * from the window's system menu.
     *
     * @param e the event to be processed
     */
    @Override
    public void windowClosing(WindowEvent e) {
        actionPerformed(null);
    }

    /**
     * Invoked when a window has been closed as the result
     * of calling dispose on the window.
     *
     * @param e the event to be processed
     */
    @Override
    public void windowClosed(WindowEvent e) {

    }

    /**
     * Invoked when a window is changed from a normal to a
     * minimized state. For many platforms, a minimized window
     * is displayed as the icon specified in the window's
     * iconImage property.
     *
     * @param e the event to be processed
     * @see Frame#setIconImage
     */
    @Override
    public void windowIconified(WindowEvent e) {

    }

    /**
     * Invoked when a window is changed from a minimized
     * to a normal state.
     *
     * @param e the event to be processed
     */
    @Override
    public void windowDeiconified(WindowEvent e) {

    }

    /**
     * Invoked when the Window is set to be the active Window. Only a Frame or
     * a Dialog can be the active Window. The native windowing system may
     * denote the active Window or its children with special decorations, such
     * as a highlighted title bar. The active Window is always either the
     * focused Window, or the first Frame or Dialog that is an owner of the
     * focused Window.
     *
     * @param e the event to be processed
     */
    @Override
    public void windowActivated(WindowEvent e) {

    }

    /**
     * Invoked when a Window is no longer the active Window. Only a Frame or a
     * Dialog can be the active Window. The native windowing system may denote
     * the active Window or its children with special decorations, such as a
     * highlighted title bar. The active Window is always either the focused
     * Window, or the first Frame or Dialog that is an owner of the focused
     * Window.
     *
     * @param e the event to be processed
     */
    @Override
    public void windowDeactivated(WindowEvent e) {

    }
}
