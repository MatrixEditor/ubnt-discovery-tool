package com.ubnt.discovery; //@date 06.12.2022

import java.util.logging.Formatter;
import java.util.logging.LogRecord;

/**
 * The {@link UbntLogFormatter} is used to format any loggable content.
 */
public class UbntLogFormatter extends Formatter {

    /**
     * The line separator used in the {@link #format(LogRecord)} method.
     */
    private final String lineSeparator = System.getProperty("line.separator");

    /**
     * Format the given log record and return the formatted string.
     * <p>
     * The resulting formatted String will normally include a
     * localized and formatted version of the LogRecord's message field.
     * It is recommended to use the {@link Formatter#formatMessage}
     * convenience method to localize and format the message field.
     *
     * @param record the log record to be formatted.
     * @return the formatted log record
     */
    @Override
    public String format(LogRecord record) {
        StringBuilder buffer = new StringBuilder();

        if (record.getSourceClassName() != null) {
            buffer.append(record.getSourceClassName());
        } else {
            buffer.append(record.getLoggerName());
        }

        String message = formatMessage(record);
        return buffer.append("(")
                     .append(record.getLevel().getName())
                     .append(")")
                     .append(message)
                     .append(lineSeparator)
                     .toString();
    }
}
