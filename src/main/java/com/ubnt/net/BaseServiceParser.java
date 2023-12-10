package com.ubnt.net; //@date 07.12.2022

import com.ubnt.net.IUbntService.Record;
import com.ubnt.net.IUbntService.RecordParser;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Abstract implementation of an {@link IUbntService.Parser}.
 */
public abstract class BaseServiceParser extends IUbntService.Parser {

    /**
     * the platform logger
     */
    public static final Logger vXLogger
            = Logger.getLogger(BaseServiceParser.class.getSimpleName());

    static {
        vXLogger.setLevel(Level.OFF);
    }

    /**
     * A factory wrapper used to create the {@link IUbntService}.
     */
    private final IUbntService.Factory factory;

    private final RecordParser defaultParser;

    public BaseServiceParser(IUbntService.Factory factory) {
        this.factory = factory;
        this.defaultParser = new UbntIOUtilities.HexStringRecordParser();
    }

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
    @Override
    public IUbntService parse(byte[] data, int length) {
        vXLogger.info("[ASP]::Parse(Parsing packet; " + length + " bytes)");
        byte cmd = data[1];

        byte[] sizeBuf = new byte[2];
        System.arraycopy(data, 2, sizeBuf, 0, 2);
        int dataLength = UbntIOUtilities.parseInt(sizeBuf).intValue();
        vXLogger.info("[ASP]::Parse(dataLength=" + dataLength + ")");

        int realLength = dataLength + 1 + 1 + 2;
        int index      = 4;
        if (realLength != length) {
            vXLogger.warning("[ASP]::Parse(Packet has invalid data length, discarding...)");
            return null;
        }

        IUbntService service = factory.createService();
        service.setPacketVersion(data[0]);
        while (index < realLength) {
            int type = data[index++];
            System.arraycopy(data, index, sizeBuf, 0, 2);

            int size = UbntIOUtilities.parseInt(sizeBuf).intValue();
            index += 2;
            if (index + size > realLength) {
                vXLogger.warning("[ASP]::Parse(Invalid record length; type=" + type + ")");
                return null;
            }
            Record record = new Record(type, size, index, data);

            RecordParser parser = Record.getParser(type);
            if (parser != null) {
                Object o = parser.parseData(data, index, size);
                record.setPayload(o);
            }
            else {
                record.setPayload(defaultParser.parseData(data, index, size));
            }
            service.add(record);
            index += size;
        }

        if (cmd != 0) {
            handleCommandCompletion(cmd, service);
        }

        vXLogger.info("[ASP]::Parse(success)");
        return service;
    }

    /**
     * [Not implemented]: command completion upon receiving a command byte
     * <p>
     * Note that version 2 packets contain up to three different command
     * values: {@code 0} == no command, {@code 6} for normalizing the
     * timestamp of the message and {@code 11} == invoking SSH.
     */
    protected void handleCommandCompletion(byte cmd, IUbntService service) {
    }

}