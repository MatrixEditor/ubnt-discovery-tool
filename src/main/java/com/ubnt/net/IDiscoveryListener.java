package com.ubnt.net; //@date 06.12.2022

import java.util.EventListener;

/**
 * {@code IDiscoveryListener}s are used by {@code IDiscoveryServer}s to notify
 * objects that a new service has been discovered.
 */
@FunctionalInterface
public interface IDiscoveryListener extends EventListener {

    /**
     * Invoked when a new service has been discovered.
     *
     * @param service the new service
     */
    void onServiceLocated(IUbntService service);
}
