package com.ubnt.net; //@date 07.12.2022

import com.ubnt.discovery.UbntResourceBundle;

/**
 * Default service implementation. (internal)
 */
public class DefaultService extends BaseService {

    //more methods?: firmware...

    /**
     * @return the qualified model name if present.
     */
    @Override
    public String getModelName() {
        Record record = get(MODEL);
        if (record == null) {
            record = get(MODEL_V2);
        }

        if (record != null) {
            return (String) record.getPayload();
        }
        return null;
    }

    /**
     * @return the uptime formatted as a {@link String}
     */
    public String getUptime() {
        Record record = get(UPTIME);
        if (record == null) {
            return "";
        }

        long value = 0;
        Object payload = record.getPayload();
        if (payload instanceof Number) {
            value = ((Number) payload).longValue();
        }


        long days = value / 86400;
        long l1   = (value / 3600) % 24;
        long l2   = (value / 60) % 60;
        long l3   = value % 60;

        StringBuilder builder = new StringBuilder();
        if (days == 1) {
            builder.append(UbntResourceBundle.format("uptime.day", (Long) days));
        } else {
            builder.append(UbntResourceBundle.format("uptime.days", (Long) days));
        }

        return builder.append(" ")
                      .append(UbntResourceBundle.format("uptime.hms", l1, l2, l3))
                      .toString();
    }
}
