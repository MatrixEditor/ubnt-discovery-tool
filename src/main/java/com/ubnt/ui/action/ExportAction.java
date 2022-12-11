package com.ubnt.ui.action; //@date 08.12.2022

import com.ubnt.discovery.UbntResourceBundle;
import com.ubnt.ui.UbntUiTableModel;
import com.ubnt.xml.UbntServiceXMLBuilder;
import com.ubnt.xml.UbntServiceXMLWriter;

import javax.swing.*;
import javax.xml.parsers.ParserConfigurationException;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.File;

import static com.ubnt.discovery.UbntResourceBundle.format;
import static com.ubnt.discovery.UbntResourceBundle.getString;

/**
 * Exports all services in XML format.
 */
public class ExportAction extends AbstractAction {

    /**
     * The model that stores all service instances.
     */
    private final UbntUiTableModel model;

    /**
     * The parent window used to create the {@link JOptionPane}s.
     */
    private final Window parent;

    /**
     * Creates an new {@link ExportAction}.
     *
     * @param model the table model to use
     * @param parent the parent container
     */
    public ExportAction(UbntUiTableModel model, Window parent) {
        super(getString("action.export"));
        this.model  = model;
        this.parent = parent;
        putValue(Action.SMALL_ICON, UbntResourceBundle.getResourceIcon("/com/ubnt/icons/export.svg"));
    }

    /**
     * Invoked when an action occurs.
     *
     * @param e the event to be processed
     */
    @Override
    public void actionPerformed(ActionEvent e) {
        // We do not want to get any errors during a scan.
        if (model.isScanning()) {
            JOptionPane.showMessageDialog(parent, getString("action.export.dialog.scan"));
            return;
        }

        if (model.getRowCount() == 0) {
            JOptionPane.showMessageDialog(parent, getString("action.export.dialog.empty"));
            return;
        }

        try {
            UbntServiceXMLBuilder builder     = new UbntServiceXMLBuilder();
            JFileChooser          fileChooser = new JFileChooser();

            fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
            int result = fileChooser.showSaveDialog(parent);

            if (result == JFileChooser.APPROVE_OPTION) {
                File file = fileChooser.getSelectedFile();
                if (file != null) {

                    for (int i = 0; i < model.getRowCount(); i++) {
                        builder.append(model.getServiceAt(i));
                    }

                    // TODO: 11.12.2022 Add error message and try-catch block
                    if (UbntServiceXMLWriter.writeXML(file, builder.getDocument())) {
                        String msg = format("action.export.dialog.success", file.getName(),
                                            file.getAbsoluteFile().getAbsolutePath());
                        builder.clear();
                        JOptionPane.showMessageDialog(parent, msg);
                    }
                    System.gc();
                }
            }
        } catch (ParserConfigurationException ee) {
            JOptionPane.showMessageDialog(parent, format("action.export.dialog.error", ee.toString()));
        }
    }
}
