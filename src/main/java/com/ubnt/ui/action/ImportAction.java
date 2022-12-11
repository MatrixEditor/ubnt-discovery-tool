package com.ubnt.ui.action; //@date 08.12.2022

import com.ubnt.discovery.UbntResourceBundle;
import com.ubnt.net.IDiscoveryListener;
import com.ubnt.net.IUbntService;
import com.ubnt.ui.UbntUiTableModel;
import com.ubnt.xml.UbntServiceXMLHandler;
import org.xml.sax.SAXException;

import javax.swing.*;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

//TBA

/**
 * Imports all services from a selected XML file.
 */
public class ImportAction extends AbstractAction
        implements IDiscoveryListener, UbntServiceXMLHandler.FinishListener {

    /**
     * The global xml handler.
     */
    private final UbntServiceXMLHandler handler;

    /**
     * The model that will contain the imported services.
     */
    private final UbntUiTableModel model;

    /**
     * The parent container used to create the {@link JFileChooser} and
     * {@link JOptionPane}s.
     */
    private final Window parent;

    /**
     * Internal progress bar used to display the current progress.
     */
    private final JProgressBar progressBar;

    /**
     * The file chooser used to choose the source file.
     */
    private JFileChooser fileChooser;

    private int count;

    /**
     * Creates a new import action.
     *
     * @param model the model to use
     * @param parent the parent container (can be {@code null})
     * @param progressBar progress indicator
     */
    public ImportAction(UbntUiTableModel model, Window parent, JProgressBar progressBar) {
        super(UbntResourceBundle.getString("action.import"));
        this.model       = model;
        this.parent      = parent;
        this.progressBar = progressBar;

        this.handler = new UbntServiceXMLHandler(IUbntService.Factory.getDefaultFactory());
        handler.addListener(model);
        handler.addListener(this);
        handler.setFinishListener(this);

        putValue(Action.SMALL_ICON,
                 UbntResourceBundle.getResourceIcon("/com/ubnt/icons/import.svg"));

        progressBar.setVisible(false);
        progressBar.setValue(0);
        progressBar.setMaximum(100);
    }

    /**
     * Invoked when an action occurs.
     *
     * @param e the event to be processed
     */
    @Override
    public void actionPerformed(ActionEvent e) {
        if (fileChooser == null) {
            fileChooser = new JFileChooser();

            fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
            fileChooser.setCurrentDirectory(new File(System.getProperty("user.dir")));
        }
        int result = fileChooser.showOpenDialog(parent);

        if (result == JFileChooser.APPROVE_OPTION) {
            progressBar.setValue(0);
            count = model.getRowCount();

            Thread thread = new Thread(new Importer());
            thread.setDaemon(true);
            thread.start();
        }
    }

    /**
     * Invoked when a new service has been discovered.
     *
     * @param service the new service
     */
    @Override
    public void onServiceLocated(IUbntService service) {
        int value = progressBar.getValue() + 1;
        if (value > progressBar.getMaximum()) {
            progressBar.setValue(0);
        } else {
            progressBar.setValue(value);
        }
        progressBar.repaint();
        progressBar.getParent().repaint();
    }

    /**
     * Invoked when the importing process has finished.
     */
    @Override
    public void onFinish() {
        count = model.getRowCount() - count;
        String msg = UbntResourceBundle.format("action.import.dialog.success", String.valueOf(count));

        progressBar.setVisible(false);
        model.setScanning(false);

        JOptionPane.showMessageDialog(parent, msg);
    }

    /**
     * Simple delegator class for importing all services.
     */
    private class Importer implements Runnable {

        /**
         * When an object implementing interface <code>Runnable</code> is used
         * to create a thread, starting the thread causes the object's
         * <code>run</code> method to be called in that separately executing
         * thread.
         *
         * @see Thread#run()
         */
        @Override
        public void run() {
            SAXParserFactory factory = SAXParserFactory.newInstance();
            factory.setValidating(true);


            String msg = null;
            try (InputStream stream = new FileInputStream(fileChooser.getSelectedFile())) {
                SAXParser parser = factory.newSAXParser();

                // This block is needed to ensure the loaded services will be
                // added to the linked model.
                model.setScanning(true);
                progressBar.setVisible(true);

                parser.parse(stream, handler);
            } catch (IOException | SAXException | ParserConfigurationException ex) {
                msg = UbntResourceBundle.format("action.import.dialog.error", ex.toString());
                JOptionPane.showMessageDialog(null, msg);
            }

        }
    }
}
