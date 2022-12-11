package com.ubnt.xml; //@date 11.12.2022

import com.ubnt.net.IUbntService;
import com.ubnt.net.IpInfo;
import com.ubnt.net.UbntDiscoveryServer;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.net.InetAddress;
import java.net.NetworkInterface;

/**
 * Builds XML-{@link Document}s by adding the XML representation of
 * {@link IUbntService} objects.
 *
 * @see UbntServiceXMLHandler
 * @see UbntServiceXMLWriter
 */
public class UbntServiceXMLBuilder {

    /**
     * The global document instance.
     */
    private final Document document;

    /**
     * The &lt;services&gt; element.
     */
    private final Element services;

    /**
     * Creates a new {@link UbntServiceXMLBuilder}.
     *
     * @throws ParserConfigurationException if a DocumentBuilder cannot be
     *                                      created which satisfies the
     *                                      configuration requested.
     */
    public UbntServiceXMLBuilder() throws ParserConfigurationException {
        DocumentBuilderFactory factory         = DocumentBuilderFactory.newInstance();
        DocumentBuilder        documentBuilder = factory.newDocumentBuilder();

        document = documentBuilder.newDocument();
        Element root = document.createElement("iubntservice");

        services = document.createElement("services");
        root.appendChild(services);
        document.appendChild(root);
    }

    /**
     * Returns the finished document.
     *
     * @return the document
     */
    public Document getDocument() {
        return document;
    }

    /**
     * Converts the provided service to XML and appends it to the end of the
     * service list in the {@link #document}.
     *
     * @param service the service to add
     * @return this builder
     */
    public UbntServiceXMLBuilder append(final IUbntService service) {
        Element element = document.createElement("service");

        Element     address = document.createElement("address");
        InetAddress source  = service.getSourceAddress();
        if (source != null) {
            address.appendChild(document.createTextNode(source.getHostAddress()));
        } else {
            IUbntService.Record record = service.get(IUbntService.IPINFO);
            String              addr   = UbntDiscoveryServer.UBNT_MULTICAST_V4;
            if (record != null) {
                addr = ((IpInfo) record.getPayload()).getIP();
            }
            address.appendChild(document.createTextNode(addr));
        }

        Element netInterface = document.createElement("interface");
        // There is one bind() call in the IDiscoveryServer.class that creates
        // an IDiscoveryChannel with a null interface, so we have to check
        // that here.
        String networkInterface = service.getInterface();
        if (networkInterface == null) {
            netInterface.appendChild(document.createTextNode("null"));
        } else {
            netInterface.appendChild(document.createTextNode(networkInterface));
        }


        Element version = document.createElement("version");
        version.appendChild(document.createTextNode(String.valueOf(service.getPacketVersion())));

        element.appendChild(address);
        element.appendChild(netInterface);
        element.appendChild(version);

        Element recordList = document.createElement("records");
        for (IUbntService.Record record : service) {
            String      type  = String.valueOf(record.getType());
            RecordClass cls   = RecordClass.getPayloadClass(record.getPayload());
            String      value = null;
            if (cls == RecordClass.IPINFO) {
                IpInfo info = (IpInfo) record.getPayload();
                value = info.getMAC() + ";" + info.getIP();
            } else {
                value = String.valueOf(record.getPayload());
            }

            Element recordElement = document.createElement("record");
            recordElement.setAttribute("type", type);
            if (cls != RecordClass.STRING) {
                recordElement.setAttribute("class", cls.name());
            }

            recordElement.appendChild(document.createTextNode(value));
            recordList.appendChild(recordElement);
        }

        element.appendChild(recordList);
        services.appendChild(element);
        return this;
    }

    /**
     * Clears all elements of the {@link #document}.
     */
    public void clear() {
        NodeList nodeList = document.getChildNodes();
        for (int i = 0; i < nodeList.getLength(); i++) {
            document.removeChild(nodeList.item(i));
        }
    }
}
