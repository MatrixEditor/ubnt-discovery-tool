package com.ubnt.net; //@date 07.12.2022

class DefaultServiceFactory extends IUbntService.Factory {

    /**
     * Creates a new {@link IUbntService}.
     *
     * @return the newly creates service object
     */
    @Override
    public IUbntService createService() {
        return new DefaultService();
    }
}
