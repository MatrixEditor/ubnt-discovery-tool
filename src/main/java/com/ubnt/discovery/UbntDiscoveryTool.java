package com.ubnt.discovery; //@date 06.12.2022


import com.beust.jcommander.IStringConverter;
import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParameterException;
import com.ubnt.net.*;
import com.ubnt.ui.UbntDiscoveryToolFrame;
import com.ubnt.xml.UbntServiceXMLHandler;
import org.xml.sax.SAXException;

import javax.swing.*;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.awt.*;
import java.io.*;
import java.util.List;
import java.util.*;
import java.util.logging.*;
import java.util.stream.Collectors;

import static com.ubnt.net.IUbntService.IPINFO;

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
    public static final String VERSION = "-version";

    /**
     * CLI identifier for activating the CLI version of this tool.
     *
     * @since 1.3
     */
    public static final String CLI_ARG = "-cli";

    /**
     * THe default pathname for the {@link UbntDiscoveryTool}'s configuration.
     */
    public static final String PATHNAME = "ubnt-tool.properties";

    /**
     * The GUI's main frame.
     */
    private static UbntDiscoveryToolFrame frame;

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
        setupLogging();
        // setting uo version2 parsers will also execute the setupParsersV1()
        // method and registers its sub-parsers.
        UbntIOUtilities.setupParsersV2();

        serverClasses = new Class[]{UbntDiscoveryServer.class};
    }

    public static void main(String[] args) {
        for (String argument : args) {
            if (argument.equals(VERSION)) {
                String message = UbntResourceBundle.format("main.version.fmt", version(), getBuildId(), UbntResourceBundle.getString("main.build.time"));
                System.out.println(message);
                return;
            }
            if (argument.equals(CLI_ARG)) {
                UbntDiscoveryTool.CLI.run(args);
                return;
            }
        }

        loadProperties();
        setLookAndFeel();

        setupServers();
        frame = new UbntDiscoveryToolFrame(UbntResourceBundle.getString("main.title"));
        frame.pack();

        int width = getInteger("frame.width", 800);
        if (width < 240) width = 240;

        int height = getInteger("frame.height", 400);
        if (height < 180) height = 180;

        frame.setSize(new Dimension(width, height));
        frame.setLocationRelativeTo(null);

        Runtime.getRuntime().addShutdownHook(new Thread(UbntDiscoveryTool::onShutdown));

        SwingUtilities.invokeLater(() -> frame.setVisible(true));
    }

    /**
     * Tries to apply a custom logging configuration.
     */
    private static void setupLogging() {
        try {
            LogManager manager = LogManager.getLogManager();
            try (InputStream stream = UbntDiscoveryTool.class.getResourceAsStream("/com/ubnt/logging.properties")) {
                if (stream != null) {
                    manager.readConfiguration(stream);
                } else {
                    File file = new File("logging.properties");
                    if (file.exists()) {
                        manager.readConfiguration(new FileInputStream(file));
                    }
                }
            }

            for (Handler handler : Logger.getGlobal().getHandlers()) {
                handler.setFormatter(new UbntLogFormatter());
            }
        } catch (IOException e) {
            System.err.println(e.toString());
        }
    }

    /**
     * Creates all {@link QueryServer} instances.
     */
    public static void setupServers() {
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
    public static void loadProperties() {
        try {
            File file = new File(PATHNAME);
            if (file.exists()) {
                configuration = new Properties();
                configuration.load(new FileReader(file));
            }
        } catch (IOException e) {
            System.err.println(e.toString());
        }

        if (getProperty("ubnt.ipv6.enabled", "false").equalsIgnoreCase("true")) {
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
     * @param value        the configuration key
     * @param defaultValue the default expected value
     * @return the configuration
     */
    public static String getProperty(String value, String defaultValue) {
        return configuration == null ? defaultValue : configuration.getProperty(value, defaultValue);
    }

    /**
     * Gets a boolean configuration property.
     *
     * @param key          the configuration key
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
     * @param key          the configuration key
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
     * @param key   the configuration key
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
                Thread thread = new Thread(server);
                if (server.isDaemon()) {
                    thread.setDaemon(true);
                }
                server.sendAll();
                thread.start();
            } catch (IOException e) {
                //log that
                e.printStackTrace();
            }
        }
    }

    /**
     * Returns the main frame of this application.
     *
     * @return the main frame.
     */
    public static UbntDiscoveryToolFrame getFrame() {
        return frame;
    }

    /**
     * Command line interface (CLI) of the {@link UbntDiscoveryTool} class.
     */
    public static final class CLI implements
            IDiscoveryListener, QueryScheduler.ScheduleListener {

        // internal logger that displays useful information
        private static final Logger logger =
                Logger.getLogger(UbntDiscoveryTool.class.getSimpleName());

        // simple list storing all discovered services
        private final List<IUbntService> services = new ArrayList<>();

        @Parameter(names = {"-I", "-interface"}, variableArity = true, descriptionKey = "cli.option.interface")
        List<String> interfaces = new ArrayList<>();

        @Parameter(names = "-sec", descriptionKey = "cli.option.seconds")
        long seconds = 10000L;

        @Parameter(names = "-ratio", descriptionKey = "cli.option.ratio")
        int ratio = 20;

        @Parameter(names = {"-g", "-grouped"}, descriptionKey = "cli.option.grouped")
        boolean grouped = false;

        @Parameter(names = "-file", descriptionKey = "cli.option.xml")
        String xmlPath;

        @Parameter(names = {"-h", "-help"}, descriptionKey = "cli.option.help")
        boolean help;

        @Parameter(names = "-v", descriptionKey = "cli.option.verbosity", converter = LogLevelConverter.class)
        Level logLevel = Level.WARNING;

        /**
         * Runs the CLI version of the {@link UbntDiscoveryTool}. Note that all arguments before {@code -cli}
         * will be removed first.
         *
         * @param args the CLI args
         */
        public static void run(String[] args) {
            UbntDiscoveryTool.CLI cli = new CLI();

            List<String> arguments = List.of(args);
            JCommander commander = JCommander.newBuilder()
                    .addObject(cli)
                    .resourceBundle(UbntResourceBundle.bundle)
                    .build();

            try {
                List<String> newArgs = arguments.subList(arguments.indexOf(CLI_ARG) + 1, arguments.size());
                commander.parse(newArgs.toArray(String[]::new));

                if (cli.help) {
                    commander.usage();
                    System.exit(0);
                }
            } catch (ParameterException e) {
                logger.warning(e.toString());
                commander.usage();
                System.exit(1);
            }

            cli.configureLogging();

            if (cli.xmlPath == null) {
                setupServers();
                for (QueryServer server : getServers()) {
                    server.addListener(cli);
                }

                QueryScheduler scheduler = new QueryScheduler(cli.seconds, cli.ratio, cli);
                Thread schedulerThread = new Thread(scheduler);
                logger.info("Starting to receive Packets...");
                schedulerThread.start();
            } else {
                SAXParserFactory saxParserFactory = SAXParserFactory.newInstance();
                UbntServiceXMLHandler handler = new UbntServiceXMLHandler(IUbntService.Factory.getDefaultFactory());
                handler.addListener(cli);

                try (InputStream stream = new FileInputStream(cli.xmlPath)) {
                    SAXParser parser = saxParserFactory.newSAXParser();
                    parser.parse(stream, handler);
                } catch (IOException | SAXException | ParserConfigurationException e) {
                    logger.throwing(cli.getClass().getName(), "run", e);
                    System.exit(1);
                }

                cli.displayServices();
            }
        }

        /**
         * Configures the verbosity of all loggers.
         */
        private void configureLogging() {
            logger.setLevel(logLevel);
            IDiscoveryServer.idsLogger.setLevel(logLevel);
            UbntDiscoveryServer.dsLogger.setLevel(logLevel);
            BaseServiceParser.vXLogger.setLevel(logLevel);
        }

        /**
         * {@inheritDoc}
         *
         * @param service the new service
         */
        @Override
        public void onServiceLocated(IUbntService service) {
            IUbntService.Record record = service.get(IPINFO);
            String address = service.getSourceAddress().toString();
            if (record != null) {
                address = ((IpInfo)record.getPayload()).getMAC();
            }

            logger.info("[CLI] Received Discovery Response from " + service.getSourceAddress());
            IUbntService cached = UbntIOUtilities.getCachedService(service, services);
            if (cached == null) {
                services.add(service);
                logger.info("[CLI] Added new Service with MAC/Address: " + address);
            } else {
                services.set(services.indexOf(cached), service);
                logger.info("[CLI] Replaced Service at " + service.getSourceAddress() + " with newer response!");
            }
        }

        /**
         * {@inheritDoc}
         *
         * @param finished whether the {@link QueryScheduler} has finished
         * @param second   the current second
         */
        @Override
        public void nextSecond(boolean finished, long second) {
            if (finished) {
                logger.info("[CLI] Finished receiving packets! (Got "+services.size()+" service[s])");
                displayServices();
                System.exit(0);
            }
        }

        /**
         * Displays all encountered services using the provided CLI args.
         */
        private void displayServices() {
            Collection<IUbntService> stream;
            if (!interfaces.isEmpty()) {
                stream = services.stream()
                        .filter(service -> interfaces.contains(service.getInterface()))
                        .collect(Collectors.toList());
            } else {
                stream = new ArrayList<>(services);
            }

            if (stream.isEmpty()) {
                String filter = String.join(" && ", this.interfaces);
                logger.warning("[CLI] Could not resolve any UbntServices with filter: " + filter);
                return;
            }

            logger.info("[CLI] Got "+stream.size()+" service(s) to display!");
            if (!this.grouped) {
                stream.forEach(this::displayService);
            } else {
                Map<String, List<IUbntService>> serviceMap = new HashMap<>();
                stream.forEach(service -> {
                    String name = service.getInterface();
                    if (name == null) {
                        name = "<no interface>";
                    }
                    serviceMap.computeIfAbsent(name, k -> new ArrayList<>())
                            .add(service);
                });

                logger.info("[CLI] Got " + serviceMap.size() + " distinct interface result(s).");
                final String header = "~".repeat(24);
                for (String interfaceName : serviceMap.keySet()) {
                    System.out.printf("%s '%s' %s\n", header, interfaceName, header);
                    serviceMap.get(interfaceName).forEach(this::displayService);
                    System.out.println();
                }
            }

        }

        /**
         * Displays a discovered {@link IUbntService}.
         *
         * @param service the service instance to display
         */
        private void displayService(IUbntService service) {
            String name = service.getModelName();
            if (name == null) {
                name = "UbntService";
            }

            String interfaceName = service.getInterface();
            if (interfaceName == null) {
                interfaceName = "<no interface>";
            }

            final String header = "=".repeat(12);
            System.out.printf("%s '%s' v%#05x@%s %s\n", header, name, service.getPacketVersion(), interfaceName, header);
            for (IUbntService.Record record : service) {
                final String recordName = record.getTypeName();
                // Simple workaround to hide unknown fields:
                if (record.isDefined()) {
                    System.out.printf("%13s (%#02x): %s\n", recordName, record.getType(), record.getPayload());
                } else {
                    final int length = record.getPayload().toString().length();
                    System.out.printf("%13s (%#02x): length=%d (%#02x)\n", "<Unknown>", record.getType(), length, length);
                }
            }
            System.out.println();
        }

        /**
         * Small class to decide the current log level.
         */
        private static final class LogLevelConverter implements IStringConverter<Level> {
            /**
             * {@inheritDoc}
             */
            @Override
            public Level convert(String value) {
                if (value != null) {
                    try {
                        return Level.parse(value);
                    } catch (IllegalArgumentException e) {
                        return Level.ALL;
                    }
                }
                return Level.WARNING;
            }
        }
    }
}
