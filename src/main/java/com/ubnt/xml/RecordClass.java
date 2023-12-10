package com.ubnt.xml; //@date 11.12.2022

import com.ubnt.net.IpInfo;

/**
 * Simple wrapper class to delegate the conversion of {@link String} to the
 * right payload class. Currently, there are only three types of payload:
 * <ul>
 *     <li>{@link #IPINFO}: payload is of type {@link IpInfo}</li>
 *     <li>{@link #STRING}: (default) payload is a string</li>
 *     <li>{@link #NUMBER}: the payload will be parsed into {@link Long}</li>
 * </ul>
 *
 * @see UbntServiceXMLHandler
 */
public enum RecordClass {
    /**
     * The default payload class does nothing and returns the provided
     * character sequence.
     */
    STRING,

    /**
     * Number payloads will always be parsed into an {@link Long} objects,
     * which provides the conversion to int via {@link Number#intValue()}.
     */
    NUMBER {
        /**
         * The {@link Long} value parsed from the given string.
         *
         * @param characters {@inheritDoc}
         * @return {@inheritDoc}
         */
        @Override
        public Object getPayload(String characters) {
            return Long.parseLong(characters);
        }
    },

    /**
     * {@code IPINFO} payloads will be converted to {@link IpInfo} objects,
     */
    IPINFO {
        /**
         * Converts the given string into an {@link IpInfo} object.
         *
         * @param characters {@inheritDoc}
         * @return {@inheritDoc}
         */
        @Override
        public Object getPayload(String characters) {
            String[] values = characters.split(";");
            if (values.length != 2) {
                throw new IllegalArgumentException(
                        "Could not parse IPINFO: invalid length " + values.length);
            }
            return new IpInfo(values[0], values[1]);
        }
    };

    /**
     * Returns the {@link RecordClass} according to the given payload.
     *
     * @param payload the payload to inspect
     * @return the according {@link RecordClass}
     */
    public static RecordClass getPayloadClass(Object payload) {
        if (payload instanceof Number) {
            return NUMBER;
        }
        else if (payload instanceof IpInfo)  {
            return IPINFO;
        }

        return STRING;
    }

    /**
     * Converts the given string into an accurate payload representation. By
     * default, return the characters.
     *
     * @param characters the input characters
     * @return the converted payload
     */
    public Object getPayload(String characters) {
        if (characters.equalsIgnoreCase("null")) {
            return null;
        }
        return characters;
    }

}
