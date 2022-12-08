package com.ubnt.net; //@date 07.12.2022

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
}
