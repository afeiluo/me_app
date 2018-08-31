package com.yy.tinytimes.thrift.turnover.settle;

import com.facebook.swift.codec.ThriftField;
import com.facebook.swift.codec.ThriftStruct;

@ThriftStruct("TServiceException")
public class TServiceException extends RuntimeException
{
    private static final long serialVersionUID = 1L;

    public TServiceException() {
    }

    private int code;

    @ThriftField(value=1, name="code")
    public int getCode() { return code; }

    @ThriftField
    public void setCode(final int code) { this.code = code; }

    private String message;

    @ThriftField(value=2, name="message")
    public String getMessage() { return message; }

    @ThriftField
    public void setMessage(final String message) { this.message = message; }
}
