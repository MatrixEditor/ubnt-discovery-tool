package com.ubnt.net; //@date 06.12.2022

import java.io.IOException;
import java.net.*;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Basic implementation of a {@code QueryServer} providing an interface for
 * sending {@code DatagramPacket}s and listen for them on different
 * {@code DatagramSocket}s.
 * <p>
 * This implementation automatically creates a {@code DatagramSocket} per
 * {@code InetAddress} on each {@code NetworkInterface}. Note that by setting
 * the system property {@code "ubnt.ipv6.enabled"} to {@code true}, this
 * server will also create IPv6 sockets.
 * <p>
 * By default, logging is enabled and the {@code Logger}'s level is set to
 * {@link Level#ALL}. All listening operations will be executed on an external
 * thread within an {@link Executors#newFixedThreadPool(int)}.
 *
 * @see QueryServer
 */
public abstract class IDiscoveryServer extends QueryServer {

    /**
     * The logger for all {@link IDiscoveryServer} objects.
     */
    public static final Logger idsLogger =
            Logger.getLogger(IDiscoveryServer.class.getSimpleName());

    static {
        idsLogger.setLevel(Level.ALL);
    }

    /**
     * A simple {@link List} object storing all {@link DatagramPacket}s that
     * will be sent via the {@link #sendAll()} method.
     */
    protected final List<DatagramPacket> packetCache;

    /**
     * A simple {@link List} object storing all {@link IDiscoveryChannel}s.
     */
    protected final List<IDiscoveryChannel> channelCache;

    /**
     * A simple variable to indicate whether this server should finish.
     */
    protected volatile boolean finished;

    /**
     * An {@link ExecutorService} needed to execute different actions and to fire
     * events.
     */
    protected ExecutorService executorService;

    /**
     * Creates a new {@link IDiscoveryServer} with the given identifier.
     *
     * @param name the name of this service.
     */
    protected IDiscoveryServer(String name) {
        super(name);
        executorService = Executors.newFixedThreadPool(4);
        finished = false;
        packetCache = new ArrayList<>(10);
        channelCache = new ArrayList<>(5);

        setup();
    }

    /**
     * Collects all available {@link NetworkInterface}s without throwing an
     * exception.
     *
     * @return all available interfaces
     */
    public static Collection<NetworkInterface> listInterfaces() {
        List<NetworkInterface> list = new LinkedList<>();
        try {
            Enumeration<NetworkInterface> enumeration =
                    NetworkInterface.getNetworkInterfaces();

            while (enumeration.hasMoreElements()) {
                NetworkInterface iface = enumeration.nextElement();
                list.add(iface);
                iface.subInterfaces().forEach(list::add);
            }
        } catch (SocketException e) {
            idsLogger.log(Level.WARNING, "[IDS]::listInterfaces()", e);
        }
        return list;
    }

    /**
     * Returns whether the given {@link InetAddress} is an {@code Inet4Address}
     * and a loopback address.
     *
     * @param address the {@code InetAddress} object
     * @return {@code true} if the given address is a loopback address.
     */
    public static boolean isNonLoopback(InetAddress address) {
        return !address.isLoopbackAddress();
    }

    /**
     * Binds this {@link IDiscoveryServer} to the given {@code NetworkInterface}
     * and address.
     * <p>
     * At first, all opened {@link IDiscoveryChannel} are going to be queried
     * and filtered by the given parameters. If the {@code DatagramChannel}
     * returned by {@code #getChannel(Selector, NetworkInterface, InetAddress)}
     * is not {@code null}, there is already a channel opened on this address
     * and interface.
     * <p>
     * Next, all socket relevant configuration will be applied to a
     * {@link DatagramSocket} object. The default receiving buffer size will
     * be {@code 4096*2}.
     *
     * @param networkInterface the net interface
     * @param address          the address to bind to
     * @return {@code true} if no error occurs
     */
    public boolean bind(String networkInterface, InetAddress address) {
        DatagramSocket socket = null;
        try {
            socket = getSocket(networkInterface, address);
            socket.setReceiveBufferSize(4096 * 2);
            final String name = String.format("%#02x", this.channelCache.size());
            IDiscoveryChannel channel = new IDiscoveryChannel(name, socket, networkInterface, address);
            channelCache.add(channel);

            idsLogger.info("[IDS@" + channel.name + "]::Bind(success=" + socket.getLocalAddress()
                    + ":" + socket.getLocalPort() + ", interface=" + networkInterface + ")");
            return true;
        } catch (IOException e) {
            idsLogger.log(Level.WARNING, "[IDS]::Bind(" + e.getMessage() + ")", (Throwable) null);
            return false;
        }
    }

    /**
     * Sends the given {@link DatagramPacket}.
     *
     * @param packet the paket to send
     * @throws IOException if the channel has been closed
     */
    public void send(DatagramPacket packet) throws IOException {
        idsLogger.info("[IDS]::Send(start)");

        List<Integer> errors = new ArrayList<>(10);

        synchronized (channelCache) {
            for (int i = 0; i < channelCache.size(); i++) {
                IDiscoveryChannel channel = channelCache.get(i);
                if (channel.datagramSocket.getLocalAddress() instanceof Inet6Address) {
                    // We don't want to send IPv4-packets on IPv6-interfaces
                    if (!(packet.getAddress() instanceof Inet6Address)) continue;
                }

                if (channel.datagramSocket.getLocalAddress() instanceof Inet4Address) {
                    // We don't want to send IPv6-packets on IPv4-interfaces
                    if (!(packet.getAddress() instanceof Inet4Address)) continue;
                }
                try {
                    idsLogger.info(
                            "[IDS@" + channel.name + "]::Send(from=" + channel.datagramSocket.getLocalAddress()
                                    + " to=" + packet.getSocketAddress() + ")");
                    channel.datagramSocket.send(packet);
                } catch (SocketException e) {
                    errors.add(i);
                } catch (Exception e2) {
                    idsLogger.log(Level.WARNING, "[IDS@" + channel.name + "]::Send(" + e2.getMessage() + ")", e2);
                }
            }
        }

        if (errors.size() > 0) {
            for (Integer idx : errors) {
                close(channelCache.get(idx));
            }
        }
        idsLogger.info("[IDS]::Send(end)");
    }

    /**
     * Closes the given {@link IDiscoveryChannel} and removes it from the cache.
     *
     * @param channel the channel to close
     */
    protected void close(IDiscoveryChannel channel) {
        synchronized (channelCache) {
            try {
                DatagramSocket socket = channel.datagramSocket;
                socket.close();
            } catch (Exception e) {
                idsLogger.log(Level.SEVERE, e.getMessage(), e);
            }

            channelCache.remove(channel);
        }
    }

    /**
     * Starts this server.
     *
     * @see Thread#run()
     */
    @Override
    public void run() {
        finished = false;
        executorService = Executors.newFixedThreadPool(channelCache.size());
        idsLogger.info("[IDS]::Run(channelCount=" + channelCache.size() + ")");

        try {
            for (IDiscoveryChannel channel : channelCache) {
                listen(channel);
            }
        } catch (IOException e) {
            idsLogger.log(Level.WARNING, "[IDS]::Run(error=" + e.getMessage() + ")", e);
        }

    }

    /**
     * Sends all packets that were cached.
     */
    @Override
    public void sendAll() throws IOException {
        for (DatagramPacket packet : packetCache) {
            send(packet);
        }
    }

    /**
     * Listens for {@link DatagramPacket} on the given {@link IDiscoveryChannel}
     * and breaks if the received packet length is {@code 0}.
     * <p>
     * This method will be executed with the {@link #executorService} on a
     * fixed size thread pool.
     *
     * @param channel the channel to listen on.
     * @throws IOException if an error occurs
     */
    protected void listen(IDiscoveryChannel channel) throws IOException {
        executorService.execute(() -> {
            try {
                byte[] buffer = new byte[1024];
                DatagramSocket socket = channel.datagramSocket;
                while (!isFinished()) {
                    DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                    socket.receive(packet);
                    // REVISIT: reformat logging messages.

                    //query packets will be discarded
                    if (packet.getLength() == 4) {
                        idsLogger.info("[IDS@" + channel.name + "]::Listen(from="
                                + packet.getAddress() + "): Query Packet discarded!");
                        continue;
                    } else if (packet.getLength() == 0) break;

                    idsLogger.info("[IDS@" + channel.name + "]::Listen(from=" + packet.getAddress()
                            + "): Received Packet with length: " + packet.getLength());
                    IUbntService service = parsePacket(packet);
                    Arrays.fill(buffer, (byte) 0);
                    if (service != null) {
                        service.setNetworkInterface(channel.networkInterface);
                        service.setSourceAddress(packet.getAddress());

                        idsLogger.info("[IDS@" + channel.name + "]::Listen(newService at " + packet.getAddress() + ")");
                        fireOnServiceDiscovered(service);
                    } else {
                        idsLogger.warning("[IDS@" + channel.name + "]::Listen(Skipping empty Service result!)");
                    }

                }
            } catch (SocketTimeoutException timeoutException) {
                //ignore
            } catch (IOException e) {
                idsLogger.log(Level.WARNING, "[IDS]Listen(" + e.getMessage() + ")", e);
            }
        });
    }

    /**
     * Sets up this server by creating all {@link DatagramSocket}s.
     */
    protected void setup() {
        for (NetworkInterface networkInterface : listInterfaces()) {
            Enumeration<InetAddress> enumeration = networkInterface.getInetAddresses();
            while (enumeration.hasMoreElements()) {
                InetAddress address = enumeration.nextElement();
                if (System.getProperty("ubnt.ipv6.enabled", "false")
                        .equals("false")) {
                    if (!(address instanceof Inet4Address)) continue;
                }

                if (isNonLoopback(address)) {
                    bind(networkInterface.getName(), address);
                }
            }
        }

        try {
            bind(null, Inet4Address.getByName("0.0.0.0"));
        } catch (UnknownHostException e) {
            idsLogger.severe("[IDS]::setup(" + e.getMessage() + ")");
        }

    }

    /**
     * Marks this thread as either a daemon thread or a user thread.
     *
     * @return whether the executor should create a daemon {@link Thread}.
     * @see Thread#setDaemon(boolean)
     */
    @Override
    public boolean isDaemon() {
        try {
            packetCache.addAll(createQueryPackets());
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    /**
     * Creates a new datagram socket that can send and receive packets from
     * the UDP broadcast.
     *
     * @param networkInterface the net interface
     * @param address          the inet address
     * @return the newly created {@link DatagramSocket}
     */
    protected abstract DatagramSocket getSocket(
            String networkInterface, InetAddress address) throws IOException;

    /**
     * Tells the caller whether this {@link QueryServer} has finished
     * executing its statements.
     *
     * @return whether this object has done its job
     */
    @Override
    public synchronized boolean isFinished() {
        return finished;
    }

    /**
     * Finished this runnable task. (has the same effect as the {@code join()}
     * takes in {@link Thread}).
     */
    @Override
    public synchronized void doFinish() {
        finished = true;
    }

    /**
     * Creates the needed query packets for discovery.
     *
     * @return the collection of query packets to send
     */
    protected abstract Collection<DatagramPacket> createQueryPackets()
            throws IOException;

    /**
     * Parses the given UDP-Packet and returns a service device object.
     *
     * @param packet the packet to parse
     * @return the service device storing the packet's values
     */
    protected abstract IUbntService parsePacket(DatagramPacket packet);

    /**
     * Clears all discovered services and cached packets.
     *
     * @throws IOException if an I/O error occurs
     */
    @Override
    public void clearAll() throws IOException {
        packetCache.clear();
    }

    /**
     * {@code IDiscoveryChannel} objects are used as an attachment to store the
     * linked {@link DatagramSocket} together with its {@link InetAddress}
     * and their {@link NetworkInterface}.
     */
    protected static final class IDiscoveryChannel {

        /**
         * Internal identifier that will be used in log output.
         */
        private final String name;

        /**
         * The linked channel.
         */
        private DatagramSocket datagramSocket;

        /**
         * The specified {@link NetworkInterface}.
         */
        private String networkInterface;

        /**
         * The network address for this {@link IDiscoveryChannel}.
         */
        private InetAddress address;

        /**
         * Creates a new {@link IDiscoveryChannel} with the given configuration
         * variables.
         *
         * @param datagramSocket   the datagram channel
         * @param networkInterface the linked {@link NetworkInterface}
         * @param address          the inet address
         */
        public IDiscoveryChannel(DatagramSocket datagramSocket, String networkInterface,
                                 InetAddress address) {
            this(String.format("%#02x", (int) (Math.random() * 100)), datagramSocket, networkInterface, address);
        }

        /**
         * Creates a new {@link IDiscoveryChannel} with the given configuration
         * variables.
         *
         * @param name             the channel's name
         * @param datagramSocket   the datagram channel
         * @param networkInterface the linked {@link NetworkInterface}
         * @param address          the inet address
         */
        public IDiscoveryChannel(String name, DatagramSocket datagramSocket, String networkInterface,
                                 InetAddress address) {
            this.name = name;
            this.datagramSocket = datagramSocket;
            this.networkInterface = networkInterface;
            this.address = address;
        }

        /**
         * Returns whether this {@link IDiscoveryChannel} can be applied to
         * the given configuration variables.
         *
         * @param networkInterface the interface to check
         * @param address          the address to check
         * @return whether the given variables are equal to the stored ones
         */
        public boolean is(String networkInterface, InetAddress address) {
            if (this.networkInterface == null && networkInterface == null) {
                return true;
            } else return this.networkInterface != null && this.networkInterface.equals(networkInterface)
                    && this.address != null && this.address.equals(address);
        }
    }

}
