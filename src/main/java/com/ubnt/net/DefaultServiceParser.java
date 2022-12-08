package com.ubnt.net; //@date 07.12.2022

/**
 * The basic implementation of the {@link BaseServiceParser} using the
 * {@link DefaultServiceFactory}. By loading and initialising this class
 * with {@link Class#forName(String)}, parsers for two packet versions (1
 * and 2).
 *
 * @see BaseServiceParser
 */
public class DefaultServiceParser extends BaseServiceParser {

    static {
        UbntDiscoveryServer.registerVersion(1, new DefaultServiceParser());
        UbntDiscoveryServer.registerVersion(2, new DefaultServiceParser());
    }

    /**
     * Creates a new parser.
     */
    public DefaultServiceParser() {
        super(IUbntService.Factory.getDefaultFactory());
    }
}
