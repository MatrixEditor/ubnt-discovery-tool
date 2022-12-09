package com.ubnt.discovery; //@date 06.12.2022


import com.ubnt.net.QueryServer;
import com.ubnt.net.UbntDiscoveryServer;
import com.ubnt.net.UbntIOUtilities;
import com.ubnt.ui.UbntDiscoveryToolFrame;

import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.util.Properties;
import java.util.logging.Handler;
import java.util.logging.LogManager;
import java.util.logging.Logger;

/**
 * The {@code UbntDiscoveryTool} is an adapted version of the original discovery
 * tool provided by {@code Ubiquity}.
 * <p>
 * This tool contains the following threading model:
 * <ul>
 *     <li><b>Scheduler:</b> A daemon {@link Thread} sending query packets
 *     every 10 seconds on all servers via {@link #getServers()}.</li>
 *     <li>
 *      <b>QueryServer[]:</b> Every {@link QueryServer} will be started
 *      on a new {@code Thread} (most likely a daemon thread)
 *         <ul>
 *              <li><b>IDiscoveryChannel[]:</b> every channel will get a new
 *              {@code Thread} to listen for {@code DatagramPacket}s.</li>
 *         </ul>
 *     </li>
 * </ul>
 * <p>
 * Pre-defined properties will be loaded via {@link #loadProperties()} from
 * a configuration file in the same directory the tool has been started in.
 */
public final class UbntDiscoveryTool {

    /**
     * Version identifier for printing the version of this program.
     */
    public static final String VERSION = "--version";

    /**
     * THe default pathname for the {@link UbntDiscoveryTool}'s configuration.
     */
    public static final String PATHNAME = "ubnt-tool.properties";

    /**
     * The GUI's main frame.
     */
    public static UbntDiscoveryToolFrame frame;

    /**
     * The build id of this tool defined as {@code main.build.id}.
     */
    private static String buildId;

    /**
     * The build id of this tool defined as {@code main.version}.
     */
    private static String versionId;

    /**
     * All server classes that should run on start of this tool.
     */
    private static Class<?>[] serverClasses;

    /**
     * All runnable server objects from the {@code ubnt.net} module.
     */
    private static QueryServer[] servers;

    /**
     * The query-scheduler task.
     */
    private static QueryScheduler scheduler;

    /**
     * Global configuration.
     */
    private static Properties configuration;

    static {
        // setting uo version2 parsers will also execute the setupParsersV1()
        // method and registers its sub-parsers.
        UbntIOUtilities.setupParsersV2();

        serverClasses = new Class[]{UbntDiscoveryServer.class};
    }

    public static void main(String[] args) {
        for (String argument : args) {
            if (argument.equals(VERSION)) {
                String message = UbntResourceBundle.format(
                        "main.version.fmt",
                        version(), getBuildId(),
                        UbntResourceBundle.getString("main.build.time"));
                System.out.println(message);
                return;
            }
        }

        loadProperties();
        setLookAndFeel();

        try {
            LogManager  manager = LogManager.getLogManager();
            ClassLoader loader  = UbntDiscoveryTool.class.getClassLoader();

            try (InputStream stream = loader.getResourceAsStream("/logger.properties")) {
                if (stream != null) {
                    manager.readConfiguration(stream);
                }
            }

            for (Handler handler : Logger.getGlobal().getHandlers()) {
                handler.setFormatter(new UbntLogFormatter());
            }
        } catch (IOException e) {
            System.err.println(e.toString());
        }

        setupServers();
        frame = new UbntDiscoveryToolFrame(UbntResourceBundle.getString("main.title"));
        frame.pack();

        int width = getInteger("frame.width", 800);
        if (width < 240) width = 240;

        int height = getInteger("frame.height", 400);
        if (height < 180) height = 180;

        frame.setSize(new Dimension(width, height));
        frame.setLocationRelativeTo(null);

        Runtime.getRuntime()
               .addShutdownHook(new Thread(UbntDiscoveryTool::onShutdown));

        SwingUtilities.invokeLater(() -> frame.setVisible(true));
    }

    /**
     * Creates all {@link QueryServer} instances.
     */
    private static void setupServers() {
        servers = new QueryServer[serverClasses.length];
        for (int i = 0; i < serverClasses.length; i++) {
            Class<?> cls = serverClasses[i];
            try {
                QueryServer server = (QueryServer) cls.getDeclaredConstructor().newInstance();
                servers[i] = server;
            } catch (ReflectiveOperationException e) {
                System.err.println(e.toString());
            }
        }
    }

    /**
     * Loads all tool configurations.
     */
    private static void loadProperties() {
        try {
            File file = new File(PATHNAME);
            if (file.exists()) {
                configuration = new Properties();
                configuration.load(new FileReader(file));
            }
        } catch (IOException e) {
            System.err.println(e.toString());
        }

        if (getProperty("ubnt.ipv6.enabled", "false")
                .equalsIgnoreCase("true")) {
            System.setProperty("ubnt.ipv6.enabled", Boolean.TRUE.toString());
        }
    }

    /**
     * Simple method as a shutdown hook used to save the properties if
     * any changes have been made.
     */
    private static void onShutdown() {
        put("frame.width", String.valueOf(frame.getWidth()));
        put("frame.height", String.valueOf(frame.getHeight()));

        if (getBoolean("ubnt.config.save", false)) {
            try {
                File file = new File("./", PATHNAME);
                try (FileOutputStream stream = new FileOutputStream(file)) {
                    configuration.store(stream, "");
                }
            } catch (IOException e) {
                System.err.println(e.toString());
            }
        }
    }

    /**
     * Sets the {@link LookAndFeel} either from the loaded configuration via
     * the {@code ubnt.ui.laf} property or starts with the system look and
     * feel.
     */
    private static void setLookAndFeel() {
        try {
            String clsName = getProperty("ubnt.ui.laf", null);

            if (clsName != null) {
                if (System.getProperty("java.version").compareTo("9") > 0) {
                    System.err.println("Could not start FlatLaf on Java running version < 1.9");
                } else {
                    Class<?> cls = Class.forName(clsName);
                    cls.getMethod("setup").invoke(null);
                    return;
                }
            }

        } catch (Exception e) {
            System.err.println(e.toString());
        }
        try {
            String lookAndFeel = UIManager.getSystemLookAndFeelClassName();
            UIManager.setLookAndFeel(lookAndFeel);
        } catch (Exception e) {
            System.err.println(e.toString());
        }
    }

    /**
     * @return the version string of this tool
     */
    public static String version() {
        if (versionId == null) {
            versionId = UbntResourceBundle.getString("main.version");
        }
        return versionId;
    }

    /**
     * @return the currently configured build id
     */
    public static String getBuildId() {
        if (buildId == null) {
            buildId = UbntResourceBundle.getString("main.build.id");
        }
        return buildId;
    }

    /**
     * All runnable server objects from the {@code ubnt.net} module.
     *
     * @return the server objects
     */
    public static QueryServer[] getServers() {
        return servers == null ? new QueryServer[0] : servers;
    }

    /**
     * Starts the scheduler thread that sends query packets every 10 seconds.
     */
    public static void scheduleScan() {
        if (scheduler != null) {
            if (scheduler.isRunning()) {
                return;
            }
        }
        scheduler = new QueryScheduler(10000L, 20, (QueryScheduler.ScheduleListener) frame);

        Thread schedulerThread = new Thread(scheduler);
        schedulerThread.setDaemon(true);
        schedulerThread.start();
    }

    /**
     * Gets a configuration property.
     *
     * @param value the configuration key
     * @param defaultValue the default expected value
     * @return the configuration
     */
    public static String getProperty(String value, String defaultValue) {
        return configuration == null ? defaultValue
                : configuration.getProperty(value, defaultValue);
    }

    /**
     * Gets a boolean configuration property.
     *
     * @param key the configuration key
     * @param defaultValue the default expected value
     * @return the configuration
     */
    public static Boolean getBoolean(String key, boolean defaultValue) {
        String result = getProperty(key, String.valueOf(defaultValue));
        return Boolean.parseBoolean(result);
    }

    /**
     * Gets an integer configuration property.
     *
     * @param key the configuration key
     * @param defaultValue the default expected value
     * @return the configuration
     */
    public static Integer getInteger(String key, int defaultValue) {
        String config = getProperty(key, String.valueOf(defaultValue));
        try {
            return Integer.parseInt(config);
        } catch (NumberFormatException e) {
            return defaultValue;
        }

    }

    /**
     * Sets a configuration property.
     *
     * @param value the configuration value
     * @param key the configuration key
     */
    public static void put(String key, Object value) {
        if (configuration != null) {
            configuration.put(key, value);
        }
    }

    /**
     * Sends all query packets and starts all {@link QueryServer}s.
     */
    public static void queryAndStart() {
        for (QueryServer server : getServers()) {
            try {
                server.sendAll();
                Thread thread = new Thread(server);
                if (server.isDaemon()) {
                    thread.setDaemon(true);
                }
                thread.start();
            } catch (IOException e) {
                //log that
                e.printStackTrace();
            }
        }
    }
}
