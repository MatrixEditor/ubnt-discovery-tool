package com.ubnt.net.test; //@date 08.12.2022

import com.ubnt.discovery.UbntDiscoveryTool;
import com.ubnt.net.DefaultService;
import com.ubnt.net.IUbntService;
import com.ubnt.net.IUbntService.Record;
import com.ubnt.net.IpInfo;
import com.ubnt.net.UbntDiscoveryServer;
import com.ubnt.ui.UbntUiTableModel;

import javax.swing.table.TableModel;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.Arrays;

import static com.ubnt.net.IUbntService.*;

public class UbntDiscoveryClientTest {

    private static Record create(int type, Object payload) {
        // REVISIT: the length is important somehow
        Record record =  new Record(type, 0, 0, null);
        record.setPayload(payload);
        return record;
    }

    public static void main(String[] args) throws Exception {
        UbntDiscoveryTool.main(args);

        IUbntService service = new DefaultService();
        IpInfo info = new IpInfo("12:34:56:78:90:11", "127.0.0.1");
        service.add(create(IPINFO, info));
        service.add(create(HW_ADDRESS, "123.123.123.123"));
        service.add(create(HOSTNAME, "Localhost"));
        service.add(create(MODEL, "U7PG2"));
        service.add(create(FW_VERSION, "BZZ..."));
        service.add(create(DEFAULT, true));

        TableModel model = UbntDiscoveryTool.getFrame().table.getModel();
        if (model instanceof UbntUiTableModel) {
            ((UbntUiTableModel) model).setScanning(true);
            ((UbntUiTableModel) model).onServiceLocated(service);
            ((UbntUiTableModel) model).setScanning(false);
        }

    }
}
