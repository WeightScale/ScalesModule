package com.konst.module;

/**
 * Created by Kostya on 08.02.2016.
 */
public class ErrorDeviceException extends Throwable {

    public ErrorDeviceException(String detailMessage) {
        super(detailMessage);
    }

    @Override
    public String getMessage() {
        return super.getMessage();
    }


}
