package com.ubnt.ui.action; //@date 07.12.2022

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.LinkedList;
import java.util.List;

/**
 * Delegates action to a list of {@link ActionListener}.
 */
public class DelegateAction extends AbstractAction {

    /**
     * THe delegation listeners
     */
    private final List<ActionListener> listenerList = new LinkedList<>();

    /**
     * Creates an {@code Action} with the specified name.
     *
     * @param name the name ({@code Action.NAME}) for the action; a
     *         value of {@code null} is ignored
     */
    public DelegateAction(String name) {
        super(name);
    }

    /**
     * Creates an {@code Action} with the specified name and small icon.
     *
     * @param name the name ({@code Action.NAME}) for the action; a
     *         value of {@code null} is ignored
     * @param icon the small icon ({@code Action.SMALL_ICON}) for the action; a
     *         value of {@code null} is ignored
     */
    public DelegateAction(String name, Icon icon) {
        super(name, icon);
    }

    /**
     * Invoked when an action occurs.
     *
     * @param e the event to be processed
     */
    @Override
    public void actionPerformed(ActionEvent e) {
        for (ActionListener listener : listenerList) {
            listener.actionPerformed(e);
        }
    }


    /**
     * Adds a new action listener.
     *
     * @param actionListener the listener to add
     */
    public void addListener(ActionListener actionListener) {
        if (actionListener != null) {
            listenerList.add(actionListener);
        }
    }
}
