package com.ubnt.net; //@date 07.12.2022

import java.util.List;
import java.util.StringJoiner;
import java.util.function.Function;

import static com.ubnt.net.IUbntService.*;

/**
 * Sample class for IO specific operations.
 */
public class UbntIOUtilities {

    /**
     * Creates an {@code bytes.length * 8} bits long value. This number should
     * be casted to an integer if possible.
     *
     * @param bytes the buffer
     * @return an unsigned {@code bytes.length * 8} bits integer.
     */
    public static Number parseInt(byte[] bytes) {
        long result = 0;
        for (int i = 0; i < bytes.length; i++) {
            int shift = ((bytes.length - 1) - i) * 8;
            result += ((long) (Byte.toUnsignedInt(bytes[i]) & 255)) << shift;
        }
        return result;
    }

    /**
     * Sets up all {@link RecordParser} for v1 packets.
     */
    public static void setupParsersV1() {
        Record.register(HW_ADDRESS, new IpAddressRecordParser());
        Record.register(IPINFO, new IpInfoRecordParser());
        Record.register(FW_VERSION, String::new);
        Record.register(ADDRESS_ENTRY, new IpAddressRecordParser());
        Record.register(MAC_ENTRY, JoiningRecordParser.macParser());
        Record.register(USERNAME, String::new);
        Record.register(SALT, new HexStringRecordParser());
        Record.register(RND_CHALLENGE, new HexStringRecordParser());
        Record.register(CHALLENGE, new HexStringRecordParser());
        Record.register(UPTIME, new IntegerRecordParser());
        Record.register(HOSTNAME, String::new);
        Record.register(PLATFORM, String::new);
        Record.register(ESSID, String::new);
        Record.register(WIFI_MODE, new IntegerRecordParser());
        Record.register(WEB_UI, new IntegerRecordParser());

        Record.register(MODEL, String::new);
    }

    /**
     * Sets up all {@link RecordParser} for v2 packets.
     */
    public static void setupParsersV2() {
        if (Record.getParser(HW_ADDRESS) == null) {
            setupParsersV1();
        }
        Record.register(SEQ, new IntegerRecordParser());
        Record.register(SOURCE_MAC, JoiningRecordParser.macParser());
        Record.register(MODEL_V2, String::new);
        Record.register(SHORT_VERSION, String::new);
        Record.register(REQ_W, String::new);
        Record.register(MODEL_V2, String::new);

        Record.register(DEFAULT, new BooleanRecordParser());
        Record.register(LOCATING, new BooleanRecordParser());
        Record.register(DHCPC, new BooleanRecordParser());
        Record.register(DHCPC_BOUND, new BooleanRecordParser());
        Record.register(SSHD_PORT, new IntegerRecordParser());
    }

    /**
     * Returns a cached service by comparing the MAC-Address.
     *
     * @param service the service to lookup
     * @param list    the service cache
     * @return {@code null} if none was found or the discovered service
     */
    public static synchronized IUbntService getCachedService(
            final IUbntService service, List<IUbntService> list) {

        String hardwareAddress = null;
        Record record = service.get(IPINFO);

        if (record != null) {
            hardwareAddress = ((IpInfo) record.getPayload()).getMAC();
        }
        for (IUbntService cached : list) {
            record = cached.get(IPINFO);
            if (record != null && hardwareAddress != null) {
                if (((IpInfo) record.getPayload()).getMAC().equals(hardwareAddress)) {
                    return cached;
                }
            }
        }
        return null;
    }

    public static class JoiningRecordParser implements RecordParser {

        private final String delimiter;
        private final int length;
        private final Function<Byte, Object> wrapper;

        public JoiningRecordParser(String delimiter, int length, Function<Byte, Object> wrapper) {
            this.delimiter = delimiter;
            this.length = length;
            this.wrapper = wrapper;
        }

        static JoiningRecordParser macParser() {
            return new JoiningRecordParser(":", 6, (b) -> {
                String s = Integer.toHexString(b & 255).toUpperCase();
                if (s.length() == 1) {
                    s = "0" + s;
                }
                return s;
            });
        }

        /**
         * Parses the given buffer and returns the record payload wrapped into
         * an object.
         *
         * @param data   the raw data
         * @param start  the start of the record
         * @param length the data length
         * @return the payload as an object
         */
        @Override
        public Object parseData(byte[] data, int start, int length) {
            StringJoiner joiner = new StringJoiner(delimiter);
            for (int i = start; i < (start + this.length) && i < (start + length); i++) {
                joiner.add(wrapper.apply(data[i]).toString());
            }
            return joiner.toString();
        }
    }

    public static class IpAddressRecordParser implements RecordParser {
        /**
         * Parses the given buffer and returns the record payload wrapped into
         * an object.
         *
         * @param data   the raw data
         * @param start  the start of the record
         * @param length the data length
         * @return the payload as an object
         */
        @Override
        public Object parseData(byte[] data, int start, int length) {
            StringJoiner joiner = new StringJoiner(".");
            for (int i = start; i < start + 4; i++) {
                joiner.add(String.valueOf(Byte.toUnsignedInt(data[i])));
            }
            return joiner.toString();
        }
    }

    public static class HexStringRecordParser implements RecordParser {

        /**
         * Parses the given buffer and returns the record payload wrapped into
         * an object.
         *
         * @param data   the raw data
         * @param start  the start of the record
         * @param length the data length
         * @return the payload as an object
         */
        @Override
        public Object parseData(byte[] data, int start, int length) {
            StringBuilder builder = new StringBuilder();
            for (int i = start; i < start + length; i++) {
                String s = Integer.toHexString(data[i]).toUpperCase();
                if (s.length() == 1) s = "0" + s;
                builder.append(s);
            }
            return builder.toString();
        }
    }

    public static class IntegerRecordParser implements RecordParser {

        /**
         * Parses the given buffer and returns the record payload wrapped into
         * an object.
         *
         * @param data   the raw data
         * @param start  the start of the record
         * @param length the data length
         * @return the payload as an object
         */
        @Override
        public Object parseData(byte[] data, int start, int length) {
            byte[] buf = new byte[length];
            System.arraycopy(data, start, buf, 0, length);
            return parseInt(buf);
        }
    }

    public static class BooleanRecordParser implements RecordParser {

        /**
         * Parses the given buffer and returns the record payload wrapped into
         * an object.
         *
         * @param data   the raw data
         * @param start  the start of the record
         * @param length the data length
         * @return the payload as an object
         */
        @Override
        public Object parseData(byte[] data, int start, int length) {
            return data[start] == 1;
        }
    }

    public static class IpInfoRecordParser implements RecordParser {

        private final RecordParser macParser = JoiningRecordParser.macParser();
        private final RecordParser ipParser = new IpAddressRecordParser();

        /**
         * Parses the given buffer and returns the record payload wrapped into
         * an object.
         *
         * @param data   the raw data
         * @param start  the start of the record
         * @param length the data length
         * @return the payload as an object
         */
        @Override
        public Object parseData(byte[] data, int start, int length) {
            //maybe validate length
            return new IpInfo(
                    (String) macParser.parseData(data, start, 6),
                    (String) ipParser.parseData(data, start + 6, 4)
            );
        }
    }

}
