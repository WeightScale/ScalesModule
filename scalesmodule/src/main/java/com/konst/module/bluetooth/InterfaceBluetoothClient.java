package com.konst.module.bluetooth;

import com.konst.module.Commands;
import com.konst.module.ObjectCommand;

/**
 * @author Kostya  on 21.07.2016.
 */
public interface InterfaceBluetoothClient {

    void write(String data);
    ObjectCommand sendCommand(Commands cmd);

    boolean writeByte(byte ch);
    int getByte();
}
