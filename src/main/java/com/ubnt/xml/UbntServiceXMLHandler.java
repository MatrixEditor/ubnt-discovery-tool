package com.ubnt.xml; //@date 10.12.2022

import com.ubnt.net.IDiscoveryListener;
import com.ubnt.net.IUbntService;
import org.xml.sax.Attributes;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import java.awt.*;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/**
 * Default SAX-Handler reacting on imported XML documents.
 */
public class UbntServiceXMLHandler extends DefaultHandler {

    /**
     * All listeners that should be notified when a new service has been
     * parsed from XML.
     */
    private final List<IDiscoveryListener> listenerList =
            Collections.synchronizedList(new LinkedList<>());

    /**
     * The service factory used to create an empty service.
     */
    private final IUbntService.Factory factory;

    /**
     * Character content of the current node.
     */
    private String chars = null;

    /**
     * The currently used record class.
     */
    private String recordClass;

    /**
     * The record type to create.
     */
    private String recordType;

    /**
     * The current service.
     */
    private IUbntService service;

    /**
     * The listener that will be notifier when this handler has imported all
     * services.
     */
    private FinishListener finishListener;

    public UbntServiceXMLHandler(IUbntService.Factory factory) {
        this.factory = factory;
    }

    /**
     * Returns the {@link RecordClass} equivalent to the given name.
     *
     * @param name the class name
     * @return the {@link RecordClass} equivalent to the given name
     * @throws IllegalArgumentException if no {@link RecordClass} could have
     *                                  been found.
     */
    public static RecordClass getRecordClass(String name) {
        for (RecordClass rClass : RecordClass.values()) {
            if (rClass.name().equalsIgnoreCase(name)) {
                return rClass;
            }
        }
        throw new IllegalArgumentException("Could not find RClass: " + name);
    }

    /**
     * Adds the given {@link IDiscoveryListener} to this handler.
     *
     * @param listener the listener to add
     */
    public void addListener(IDiscoveryListener listener) {
        if (listener != null) {
            this.listenerList.add(listener);
        }
    }

    /**
     * Sets the finish listener.
     *
     * @param finishListener the finish listener
     */
    public void setFinishListener(FinishListener finishListener) {
        this.finishListener = finishListener;
    }

    /**
     * Resolve an external entity.
     *
     * @param publicId The public identifier, or null if none is
     *         available.
     * @param systemId The system identifier provided in the XML
     *         document.
     * @return The new input source, or null to require the
     *         default behaviour.
     * @throws IOException  If there is an error setting
     *                      up the new input source.
     * @throws SAXException Any SAX exception, possibly
     *                      wrapping another exception.
     * @see EntityResolver#resolveEntity
     */
    @Override
    public InputSource resolveEntity(String publicId, String systemId)
            throws IOException, SAXException {
        return new InputSource(
                UbntServiceXMLHandler.class.getResourceAsStream("/com/ubnt/xml/iubntservice.dtd")
        );
    }

    /**
     * Receive notification of the start of an element.
     *
     * @param uri The Namespace URI, or the empty string if the
     *         element has no Namespace URI or if Namespace
     *         processing is not being performed.
     * @param localName The local name (without prefix), or the
     *         empty string if Namespace processing is not being
     *         performed.
     * @param qName The qualified name (with prefix), or the
     *         empty string if qualified names are not available.
     * @param attributes The attributes attached to the element.  If
     *         there are no attributes, it shall be an empty
     *         Attributes object.
     * @throws SAXException Any SAX exception, possibly wrapping another
     *                      exception.
     */
    @Override
    public void startElement(String uri, String localName, String qName, Attributes attributes)
            throws SAXException {
        switch (qName.toLowerCase()) {
            // Here, we are creating the new service that will get all the
            // attributes we parse next.
            case "service": {
                if (this.service != null) {
                    throw new IllegalStateException("service must be null");
                }
                this.service = factory.createService();
                break;
            }

            case "record": {
                recordClass = attributes.getValue("class");
                recordType  = attributes.getValue("type");
                break;
            }

            default: {
                break;
            }
        }
    }

    /**
     * Receive notification of the end of an element.
     *
     * @param uri The Namespace URI, or the empty string if the
     *         element has no Namespace URI or if Namespace
     *         processing is not being performed.
     * @param localName The local name (without prefix), or the
     *         empty string if Namespace processing is not being
     *         performed.
     * @param qName The qualified name (with prefix), or the
     *         empty string if qualified names are not available.
     * @throws SAXException Any SAX exception, possibly
     *                      wrapping another exception.
     */
    @Override
    public void endElement(String uri, String localName, String qName)
            throws SAXException {

        switch (qName.toLowerCase()) {
            case "service": {
                requireNonNullService();
                final IUbntService extService = service;
                // We want this notification happen on another thread as this
                // one should not add element to swing containers.
                EventQueue.invokeLater(() -> notifyListeners(extService));
                service = null;
                break;
            }

            case "address": {
                requireNonNullService();
                try {
                    String address = chars;
                    service.setSourceAddress(InetAddress.getByName(address));
                } catch (UnknownHostException e) {
                    throw new IllegalStateException(e);
                }
                break;
            }

            case "interface": {
                requireNonNullService();
                String name = chars;
                // See UbntServiceXMLBuilder for details why this field
                // can be null.
                if (!name.equalsIgnoreCase("null")) {
                    service.setNetworkInterface(name);
                }
                chars = null;
                break;
            }

            case "version": {
                requireNonNullService();
                service.setPacketVersion(Integer.parseInt(chars));
                chars = null;
                break;
            }

            case "record": {
                requireNonNullService();
                RecordClass rClass = getRecordClass(recordClass);
                int         type   = Integer.parseInt(recordType);

                IUbntService.Record record = new IUbntService.Record(
                        type, 0, 0, null
                );
                record.setPayload(rClass.getPayload(chars));
                service.add(record);

                recordType  = null;
                recordClass = null;
                chars       = null;
                break;
            }

            case "iubntservice": {
                if (finishListener != null) {
                    EventQueue.invokeLater(finishListener::onFinish);
                }
                break;
            }

            default:
                break;
        }
    }

    /**
     * Checks whether the {@link #service} field is {@code null}.
     */
    private void requireNonNullService() {
        if (this.service == null) {
            throw new IllegalStateException("service is null");
        }
    }

    /**
     * Notifies all listeners about the given service.
     *
     * @param service the service all listeners should be notified about
     */
    private void notifyListeners(IUbntService service) {
        for (IDiscoveryListener listener : listenerList) {
            if (listener != null) {
                listener.onServiceLocated(service);
            }
        }
    }

    /**
     * Receive notification of character data inside an element.
     *
     * @param ch The characters.
     * @param start The start position in the character array.
     * @param length The number of characters to use from the
     *         character array.
     * @throws SAXException Any SAX exception, possibly
     *                      wrapping another exception.
     */
    @Override
    public void characters(char[] ch, int start, int length)
            throws SAXException {
        this.chars = new String(ch, start, length);
    }

    public interface FinishListener {
        void onFinish();
    }

}
