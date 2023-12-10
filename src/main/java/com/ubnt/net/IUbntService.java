package com.ubnt.net;//@date 07.12.2022

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * Basic interface for local network devices by Ubiquiti. Objects of this
 * class store {@link Record}s that describe different functionalities ans
 * attributes of this service.
 * <p>
 * This class implements functionalities from the {@link Iterable} interface:
 * <pre>
 *     {@link IUbntService} service = ...;
 *     for ({@link Record} record : service) {
 *         ...
 *     }
 * </pre>
 *
 * @see Record
 */
public interface IUbntService extends Iterable<IUbntService.Record> {

    int HW_ADDRESS    = 1;
    int IPINFO        = 2;
    int FW_VERSION    = 3;
    int ADDRESS_ENTRY = 4;
    int MAC_ENTRY     = 5;
    int USERNAME      = 6;
    int SALT          = 7;
    int RND_CHALLENGE = 8;
    int CHALLENGE     = 9;
    int UPTIME        = 10;
    int HOSTNAME      = 11;
    int PLATFORM      = 12;
    int ESSID         = 13;

    /**
     * The WIFI mode can be defined using the following enum:
     * <pre>{@code
     * public enum WirelessMode {
     *    UNDEFINED(-1),
     *    AUTO(0),
     *    ADHOC(1),
     *    MANAGED(2),
     *    MASTER(3),
     *    REPEATER(4),
     *    SECONDARY(5),
     *    MONITOR(6);
     * }
     * }</pre>
     */
    int WIFI_MODE     = 14;
    int WEB_UI        = 16;
    int MODEL         = 20;

    //v2-specific
    int SEQ           = 18;
    int SOURCE_MAC    = 19;
    int MODEL_V2      = 21;
    int SHORT_VERSION = 22;
    int DEFAULT       = 23;
    int LOCATING      = 24;
    int DHCPC         = 25;
    int DHCPC_BOUND   = 26;
    int REQ_W         = 27;
    int SSHD_PORT     = 28;

    /**
     * Returns the Web-UI port of version 1 and 2 packets.
     *
     * @return the Web-UI port
     */
    default int getWebUiPort() {
        if (!hasWebUi()) {
            return 0;
        }

        int port = ((Number) get(WEB_UI).getPayload()).intValue();
        if (getPacketVersion() == 1) {
            port &= 0xFFFF;
        } else {
            port = (port >> 8) & 0xFF;
        }
        return port;
    }


    /**
     * Returns the packet version that was used to discover this service.
     *
     * @return the packet version
     */
    int getPacketVersion();

    /**
     * Sets the packet version.
     *
     * @param version the version (either 1 or 2)
     */
    void setPacketVersion(int version);

    /**
     * Adds a new {@link Record} to this {@code UbntService}.
     *
     * @param record the record to add.
     */
    void add(Record record);

    /**
     * Removes the given record from this service.
     *
     * @param record the record to remove
     * @return {@code true} if the record has been removed successfully
     */
    boolean remove(Record record);

    /**
     * Tries to resolve a {@link Record} with the given type.
     *
     * @param type the type too lookup
     * @return {@code null} if no {@link Record} with the provided type
     *         has been found; the {@code Record} otherwise.
     */
    default Record get(int type) {
        for (Record record : this) {
            if (record.getType() == type) {
                return record;
            }
        }
        return null;
    }

    /**
     * @return the qualified model name if present.
     */
    String getModelName();

    /**
     * @return the sender's address
     */
    InetAddress getSourceAddress();

    /**
     * Internally changes the source address of this service.
     *
     * @param address the new address
     */
    void setSourceAddress(InetAddress address);

    /**
     * @return the interface this service was discovered on.
     */
    String getInterface();

    /**
     * Internally changes the {@link NetworkInterface} this service was
     * discovered on.
     *
     * @param networkInterface the new interface
     */
    void setNetworkInterface(String networkInterface);

    /**
     * Returns the raw hardware address if present. This method first queries
     * the {@link #get(int)} method with {@link #HW_ADDRESS}.
     *
     * @return the raw hardware address
     */
    default byte[] getRawHardwareAddress() {
        Record record = get(HW_ADDRESS);
        if (record != null) {
            Object data = record.getPayload();
            if (data instanceof String) {
                // By default, we are expecting a string that
                // contains four numbers separated with a '.'
                return ((String) data).replaceAll("\\.", "")
                                      .getBytes();
            }
            else if (data instanceof byte[]) {
                return (byte[]) data;
            }
        }
        return new byte[4];
    }

    /**
     * @return the status of this service
     */
    default String getStatus() {
        Record record = get(DEFAULT);
        if (record != null) {
            Object payload = record.getPayload();
            if (payload instanceof String) return payload.toString();
            return (Boolean) record.getPayload() ? "Pending" : "Managed/Adopted";
        }
        return "Unknown";
    }

    /**
     * Checks whether this service contains a Web-UI.
     *
     * @return whether this service contains a Web-UI.
     */
    default boolean hasWebUi() {
        return get(WEB_UI) != null;
    }

    /**
     * Returns the Web-UI protocol (http or https) if present. This method
     * first queries the {@link #get(int)} method with {@link #WEB_UI}.
     *
     * @return the Web-UI protocol, either http or https
     */
    default String getWebUiProtocol() {
        if (!hasWebUi()) {
            return "unknown";
        }

        int    value = ((Number) get(WEB_UI).getPayload()).intValue();
        String protocol;
        if (getPacketVersion() == 1) {
            protocol = (value >> 16) > 0 ? "https" : "http";
        } else {
            protocol = (value & 0xFF) > 0 ? "https" : "http";
        }
        return protocol;
    }

    /**
     * A functional interface for parsing data into {@link Record} objects.
     * Different standardized implementations are available in the
     * {@link UbntIOUtilities} class.
     */
    @FunctionalInterface
    interface RecordParser {

        /**
         * Parses the given buffer and returns the record payload wrapped into
         * an object.
         *
         * @param data the raw data
         * @param length the data length
         * @param start the start of the record
         * @return the payload as an object
         */
        Object parseData(byte[] data, int start, int length);

    }

    /**
     * A factory for creating {@link IUbntService} objects.
     */
    abstract class Factory {

        private static Factory defaultFactory;

        public static Factory getDefaultFactory() {
            if (defaultFactory == null) {
                synchronized (Factory.class) {
                    defaultFactory = new DefaultServiceFactory();
                }
            }
            return defaultFactory;
        }

        /**
         * Creates a new {@link IUbntService}.
         *
         * @return the newly creates service object
         */
        public abstract IUbntService createService();
    }

    /**
     * A functional abstract class to parse raw bytes into an {@link IUbntService}.
     */
    abstract class Parser {

        /**
         * Parses the given buffer using all defined {@link RecordParser}
         * definitions stored in {@link Record#parsers}.
         * <p>
         * The returned {@code IUbntService} could be {@code null} if unexpected
         * errors occur.
         *
         * @param data the raw data
         * @param length the data length
         * @return an {@link IUbntService} with all parsed {@link Record}s.
         */
        public abstract IUbntService parse(byte[] data, int length);
    }

    /**
     * {@code Record}s are provided by every {@link IUbntService} in order to
     * describe it.
     */
    final class Record {

        /**
         * Default parser cache.
         */
        public static final Map<Integer, RecordParser> parsers = new HashMap<>();

        /**
         * Stores all type names.
         */
        private static final Map<Integer, String> typenameMap = new HashMap<>();

        /**
         * The raw data buffer. Note that this object is shared between all
         * records and should not be modified.
         */
        private final byte[] data;

        /**
         * The {@link Record}'s type.
         */
        private final int type;

        /**
         * The raw {@link Record}'s data length.
         */
        private final int length;

        /**
         * The starting offset in relation to the {@link #data} variable
         */
        private final int start;

        /**
         * the payload created by a {@link RecordParser}.
         */
        private Object payload;

        public Record(int type, int length, int start, byte[] data) {
            this.type   = type;
            this.length = length;
            this.start  = start;
            this.data   = data;
        }

        /**
         * Registers a new {@link RecordParser} for the given type if not
         * already registered.
         *
         * @param type the record's type
         * @param parser the parser
         */
        public static void register(int type, RecordParser parser) {
            parsers.putIfAbsent(type, parser);
        }

        /**
         * Returns the configured {@link RecordParser} for the given type.
         *
         * @param type the record type
         * @return the configured {@link RecordParser}
         */
        public static RecordParser getParser(int type) {
            return parsers.get(type);
        }

        /**
         * Returns the cached typename for the given {@link Record}.
         *
         * @param record the record
         * @return the typename for the given {@link Record}.
         */
        public static String getTypename(Record record) {
            // This map is just to decrease the amount of calls to the
            // Record#getTypeName() method
            if (typenameMap.isEmpty()) {
                try {
                    for (Field field : IUbntService.class.getFields()) {
                        if (Modifier.isStatic(field.getModifiers()) &&
                                field.getType() == int.class) {
                            int value = field.getInt(null);
                            typenameMap.putIfAbsent(value, field.getName());
                        }
                    }
                } catch (ReflectiveOperationException ignored) {
                }
            }
            return typenameMap.getOrDefault(record.getType(),
                                            String.valueOf(record.getType()));
        }

        public boolean isDefined() {
            return !String.valueOf(getType()).equals(getTypeName());
        }

        /**
         * @return the {@link Record}'s type.
         */
        public int getType() {
            return type;
        }

        /**
         * @return the raw {@link Record}'s data length.
         */
        public int getLength() {
            return length;
        }

        /**
         * @return the starting offset in relation to the {@link #data} variable
         */
        public int getStart() {
            return start;
        }

        /**
         * Returns the data of this record.
         *
         * @return the data of this record.
         */
        public byte[] getData() {
            return Arrays.copyOfRange(data, start, start + length);
        }

        /**
         * @return the record's payload
         */
        public Object getPayload() {
            return payload;
        }

        /**
         * Sets a new payload.
         *
         * @param payload the record's payload
         */
        public void setPayload(Object payload) {
            this.payload = payload;
        }

        /**
         * Returns the name of the record's type.
         *
         * @return the name of this record
         * @implSpec This method iterates over all constants defined in
         *         {@link IUbntService} and checks whether the record's type
         *         matches the field's value.
         */
        public String getTypeName() {
            return getTypename(this);
        }

    }
}
