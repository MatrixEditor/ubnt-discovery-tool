package com.ubnt.net; //@date 06.12.2022

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.*;

/**
 * Basic implementation of an {@link IUbntService}.
 */
public abstract class BaseService implements IUbntService {

    /**
     * A simple constant defining an unknown type.
     */
    private static final String UNKNOWN = "unknown";

    /**
     * Defines all wireless modes an {@link IUbntService} can support.
     */
    private static final String[] WIRELESS_MODES =
            {"auto", "adhoc", "station", "ap", "repeater", "secondary", "monitor"};

    /**
     * Stores all model definitions in a synchronized map.
     * <p>
     * The models are defined in {@code /com/ubnt/models} and can be loaded
     * via the {@link #loadModels(String)} method.
     *
     * @see #loadModels(String)
     */
    public static Map<String, String> models =
            Collections.synchronizedMap(new HashMap<>());

    /**
     * A simple list storing all parsed records.
     */
    protected List<Record> recordList;

    /**
     * The device's ip-address
     */
    private InetAddress address;

    /**
     * The networkInterface this service was discovered on.
     */
    private String networkInterface;

    /**
     * The desired packet version of this service.
     */
    private int packetVersion;

    /**
     * Creates a new service with zero records.
     */
    public BaseService() {
        recordList = new LinkedList<>();
    }

    /**
     * Returns the model description for the given model name.
     *
     * @param modelName the model's name
     * @return the description
     */
    public static String getModelDescription(String modelName) {
        String desc = models.get(modelName);
        if (desc == null) {
            desc = UNKNOWN;
        }
        // cloud_key models are mapped as well and identified through
        // a ;true at the end
        if (desc.contains(";")) {
            return desc.substring(0, desc.indexOf(';'));
        }
        return desc;
    }

    /**
     * Returns whether the given model is a cloud key.
     *
     * @param modelName the model name
     * @return {@code true} if it is an {@code Unifi} CloudKey.
     */
    public static boolean isCloudKey(String modelName) {
        String desc = models.get(modelName);
        return desc != null && desc.contains(";");
    }

    /**
     * Loads all defined models from the {@code /com/ubnt/models} file.
     * <p>
     * Note that this method will ignore all lines that contain only the
     * model's name (length < 2).
     */
    public static void loadModels() {
        loadModels("/com/ubnt/models");
    }

    /**
     * Loads all defined models from the {@code /com/ubnt/models} file.
     * <p>
     * Note that this method will ignore all lines that contain only the
     * model's name (length < 2).
     *
     * @param path the resource path
     */
    public static void loadModels(String path) {
        ClassLoader loader = IUbntService.class.getClassLoader();

        InputStream stream = loader.getResourceAsStream(path);
        if (stream != null) {
            try (BufferedReader bis = new BufferedReader(new InputStreamReader(stream))) {
                bis.lines().map(s -> s.split("="))
                   .filter(x -> x.length >= 2)
                   .forEach(x -> models.putIfAbsent(x[0], x[1]));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Returns the wireless mode display name.
     *
     * @param mode the wireless mode
     * @return the corresponding name
     */
    public static String getWirelessModeName(int mode) {
        if (WIRELESS_MODES.length <= mode) {
            return UNKNOWN;
        }
        return WIRELESS_MODES[mode];
    }

    /**
     * Adds a new {@link Record} to this {@code UbntService}.
     *
     * @param record the record to add.
     */
    @Override
    public void add(Record record) {
        if (record != null) {
            recordList.add(record);
        }
    }

    /**
     * Returns the packet version that was used to discover this service.
     *
     * @return the packet version
     */
    @Override
    public int getPacketVersion() {
        return packetVersion;
    }

    /**
     * Sets the packet version.
     *
     * @param version the version (either 1 or 2)
     */
    @Override
    public void setPacketVersion(int version) {
        this.packetVersion = version;
    }

    /**
     * Removes the given record from this service.
     *
     * @param record the record to remove
     * @return {@code true} if the record has been removed successfully
     */
    @Override
    public boolean remove(Record record) {
        return recordList.remove(record);
    }

    /**
     * @return the sender's address
     */
    @Override
    public InetAddress getSourceAddress() {
        return address;
    }

    /**
     * Internally changes the source address of this service.
     *
     * @param address the new address
     */
    @Override
    public void setSourceAddress(InetAddress address) {
        this.address = address;
    }

    /**
     * @return the interface this service was discovered on.
     */
    @Override
    public String getInterface() {
        return networkInterface;
    }

    /**
     * Internally changes the {@link NetworkInterface} this service was
     * discovered on.
     *
     * @param networkInterface the new interface
     */
    @Override
    public void setNetworkInterface(String networkInterface) {
        this.networkInterface = networkInterface;
    }

    /**
     * Returns an iterator over elements of type {@code Record}.
     *
     * @return an Iterator.
     */
    @Override
    public Iterator<Record> iterator() {
        return recordList.iterator();
    }
}
