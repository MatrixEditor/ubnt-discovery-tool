package com.ubnt.net; //@date 07.12.2022

/**
 * A simple class storing the MAC-Address and IP-Address of a discovered
 * {@link IUbntService}.
 */
public final class IpInfo {

    /**
     * The device's MAC-Address.
     */
    private final String mac;

    /**
     * The device's IP-Address
     */
    private final String ip;


    /**
     * Creates a new {@link IpInfo}.
     *
     * @param mac the MAC-Address.
     * @param ip the IP-Address.
     */
    public IpInfo(String mac, String ip) {
        this.mac = mac;
        this.ip  = ip;
    }

    /**
     * The device's MAC-Address.
     */
    public String getMAC() {
        return mac;
    }

    /**
     * The device's IP-Address
     */
    public String getIP() {
        return ip;
    }

    @Override
    public String toString() {
        return "IpInfo{" +
                "mac='" + mac + '\'' +
                ", ip='" + ip + '\'' +
                '}';
    }
}
