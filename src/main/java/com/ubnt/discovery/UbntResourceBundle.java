package com.ubnt.discovery; //@date 06.12.2022

import com.ubnt.imageio.SVGUtil;

import javax.swing.*;
import java.net.URL;
import java.text.MessageFormat;
import java.util.PropertyResourceBundle;
import java.util.ResourceBundle;

/**
 * The main resource bundle manager for localizing strings and loading
 * images.
 */
public final class UbntResourceBundle {

    /**
     * The resource bundle for this discovery tool.
     */
    public static ResourceBundle bundle;

    static {
        // ensure we don't get any NotInitializedException
        bundle = null;
        try {
            // The actual file will be placed in the resource section of
            // this repository: /com/ubnt/discovery/messages.properties.
            // In future versions there may be some additional languages.
            bundle = PropertyResourceBundle.getBundle("com.ubnt.discovery.messages");
        } catch (Exception e) {
            String msg = String.format("Failed to load resource bundle: %s", e);
            System.err.println(msg);
        }
    }

    /**
     * Gets a string for the given key from this resource bundle or one of its
     * parents.
     *
     * @param key the property key
     * @return the string for the given key
     */
    public static String getString(String key) {
        if (bundle == null) {
            return key;
        }

        try {
            return bundle.getString(key);
        } catch (Exception e) {
            String msg = String.format("No translation for '%s': %s", key, e);
            System.err.println(msg);
        }
        return key;
    }

    /**
     * Creates a MessageFormat with the given pattern and uses it to format
     * the given arguments.
     *
     * @param key the property key
     * @param fmt the format arguments
     * @return the formatted string
     */
    public static String format(String key, Object... fmt) {
        return bundle == null ? key : MessageFormat.format(getString(key), fmt);
    }

    /**
     * Loads a resource icon.
     *
     * @param path the image path
     * @return the loaded SVG-Image or {@code null} on error
     */
    public static ImageIcon getResourceIcon(String path) {
        try {
            URL url = UbntResourceBundle.class.getResource(normalizePath(path));
            if (url != null) {
                return new ImageIcon(SVGUtil.loadSVG(url));
            }
        } catch (Exception e) {
            System.err.println(e.toString());
        }
        return null;
    }

    /**
     * Trying to resolve '_dark.svg' images.
     *
     * @param path the base path
     * @return the edited path
     */
    private static String normalizePath(String path) {
        String laf = UbntDiscoveryTool.getProperty("ubnt.ui.laf", null);
        if (laf == null) {
            return path;
        }

        try {
            // First, checking if the current look and feel is dark by
            // executing the FlatLaf#isLafDark() method.
            Class<?> cls = Class.forName("com.formdev.flatlaf.FlatLaf");
            if ((Boolean) cls.getMethod("isLafDark").invoke(null)) {
                // Querying for the resource with the name plus the
                // '_dark.svg' suffix.
                String name = path.substring(0, path.lastIndexOf('.'));
                return UbntResourceBundle.class.getResource(name + "_dark.svg") != null
                        ? name + "_dark.svg"
                        : name + ".svg";
            }
        } catch (Exception e) {
            // ignore that
        }
        return path;
    }

}
