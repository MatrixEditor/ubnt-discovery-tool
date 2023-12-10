package com.ubnt.net; //@date 07.12.2022

import java.io.IOException;
import java.net.*;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Default server implementation for discovering ubiquiti devices in the
 * local network.
 *
 * @see IDiscoveryServer
 * @see IUbntService
 * @see BaseServiceParser
 */
public class UbntDiscoveryServer extends IDiscoveryServer {

    /**
     * The default query buffer which will be used to create the query
     * packets.
     */
    public static final byte[] QUERY_V1 = {1, 0, 0, 0};

    /**
     * As defined in {@code background.js} of the web-app of the
     * {@code UbntDiscoveryTool}, these bytes are used as a query packet for
     * version {@code 2} stations.
     *
     * @see #createQueryPackets()
     */
    public static final byte[] QUERY_V2 = {2, 8, 0, 0};

    /**
     * The default broadcast address. ({@code 255.255.255.255})
     */
    public static final byte[] BROADCAST_V4 = {-1, -1, -1, -1};

    /**
     * THe default IPv6 broadcast address. ({@code ff02::1})
     */
    public static final byte[] BROADCAST_V6 =
            {-1, 2, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1};

    /**
     * The default UBNT multicast ipv4 address.
     */
    public static final String UBNT_MULTICAST_V4 = "233.89.188.1";

    /**
     * The broadcast port for the UBNT device discovery protocol.
     */
    public static final int UBNT_PORT = 10001;

    /**
     * The default logger for all {@link UbntDiscoveryServer} objects.
     */
    public static final Logger dsLogger =
            Logger.getLogger(UbntDiscoveryServer.class.getSimpleName());

    /**
     * The {@link IUbntService.Parser} objects registered to different versions of discovery
     * response packets.
     *
     * @see IUbntService.Parser
     */
    private static final Map<Integer, IUbntService.Parser> parserCache =
            Collections.synchronizedMap(new IdentityHashMap<>());

    static {
        // Using a ServiceLoader to initialize all service parser classes. Each
        // should define a static block where a new instance would be added to
        // the parserCache by calling
        ServiceLoader<IUbntService.Parser> parserServiceLoader =
                ServiceLoader.load(IUbntService.Parser.class, UbntDiscoveryServer.class.getClassLoader());

        dsLogger.setLevel(Level.OFF);
        for (IUbntService.Parser serviceParser : parserServiceLoader) {
            dsLogger.info("[UDS]::static{parser="
                                  + serviceParser.getClass().getSimpleName() + "}");
        }
    }

    /**
     * Creates a new {@link IDiscoveryServer} with the given identifier.
     */
    public UbntDiscoveryServer() {
        super(UbntDiscoveryServer.class.getSimpleName());
    }

    /**
     * Registers the given {@link IUbntService.Parser} to the provided version.
     *
     * @param version the targeted version number
     * @param parser the parser to use
     */
    public static synchronized void registerVersion(int version, IUbntService.Parser parser) {
        if (parser != null) {
            parserCache.putIfAbsent(version, parser);
        }
    }

    /**
     * Removes the parser that is mapped to the given version number.
     *
     * @param version the version number
     */
    public static synchronized void removeVersion(int version) {
        parserCache.remove(version);
    }

    /**
     * Returns the registered parser instance.
     *
     * @param version the packet version
     */
    public static synchronized IUbntService.Parser ofVersion(int version) {
        return parserCache.get(version);
    }

    /**
     * Parses the given UDP-Packet and returns a service device object.
     *
     * @param packet the packet to parse
     * @return the service device storing the packet's values
     */
    @Override
    protected IUbntService parsePacket(DatagramPacket packet) {
        byte[] data;
        if (packet == null || (data = packet.getData()) == null || data.length == 0) {
            return null;
        }
        int version = data[0];
        dsLogger.info(String.format("[UDS]::ParsePacket(v=%#02x, rawLength=%d, from=%s)",
                                    version, data.length, packet.getAddress()));


        IUbntService.Parser parser = parserCache.get(version);
        if (parser == null) {
            dsLogger.warning("[UDS]::ParsePacket(unknownVersion=" + version + ")\n");
            return null;
        }
        return parser.parse(data, packet.getLength());
    }

    /**
     * Creates a new datagram socket that can send and receive packets from
     * the UDP broadcast.
     *
     * @param networkInterface the net interface
     * @param address the inet address
     * @return the newly created {@link DatagramSocket}
     */
    @Override
    protected DatagramSocket getSocket(String networkInterface, InetAddress address) throws IOException {
        // Port must be 0, see #3
        SocketAddress socketAddress = new InetSocketAddress(address, 0);

        MulticastSocket socket = new MulticastSocket(socketAddress);
        socket.setBroadcast(true);
        if (address instanceof Inet4Address) {
            socket.joinGroup(InetAddress.getByName(UBNT_MULTICAST_V4));
        }
        return socket;
    }

    /**
     * Creates the needed query packets for discovery.
     *
     * @return the collection of query packets to send
     */
    @Override
    protected Collection<DatagramPacket> createQueryPackets() throws IOException {
        return List.of(new DatagramPacket(
                QUERY_V1.clone(), 4,
                InetAddress.getByAddress(BROADCAST_V4), UBNT_PORT
        ), new DatagramPacket(
                QUERY_V2.clone(), QUERY_V2.length,
                InetAddress.getByAddress(BROADCAST_V4), UBNT_PORT
        ), new DatagramPacket(
                QUERY_V1.clone(), 4,
                InetAddress.getByAddress(BROADCAST_V6), UBNT_PORT
        ), new DatagramPacket(
                QUERY_V2.clone(), QUERY_V2.length,
                InetAddress.getByAddress(BROADCAST_V6), UBNT_PORT
        ));
    }
}
