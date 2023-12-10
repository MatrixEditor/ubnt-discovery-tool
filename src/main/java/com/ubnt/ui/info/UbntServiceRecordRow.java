package com.ubnt.ui.info; //@date 09.12.2022

import com.ubnt.net.IUbntService;

import java.util.AbstractMap;

/**
 * An Entry maintaining an immutable record type name and record.
 *
 * @see UbntServiceInfoModel
 */
public class UbntServiceRecordRow
        extends AbstractMap.SimpleImmutableEntry<String, IUbntService.Record> {

    /**
     * Creates an entry representing a mapping from the specified
     * key to the specified value.
     *
     * @param key the key represented by this entry
     * @param value the value represented by this entry
     */
    public UbntServiceRecordRow(String key, IUbntService.Record value) {
        super(key, value);
    }
}
