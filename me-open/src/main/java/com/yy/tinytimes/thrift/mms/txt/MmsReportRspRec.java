package com.yy.tinytimes.thrift.mms.txt;

import com.facebook.swift.codec.ThriftField;
import com.facebook.swift.codec.ThriftStruct;

import static com.google.common.base.Objects.toStringHelper;

@ThriftStruct("MmsReportRspRec")
public class MmsReportRspRec
{
    public MmsReportRspRec() {
    }

    private int code;

    @ThriftField(value=1, name="code")
    public int getCode() { return code; }

    @ThriftField
    public void setCode(final int code) { this.code = code; }

    private String msg;

    @ThriftField(value=2, name="msg")
    public String getMsg() { return msg; }

    @ThriftField
    public void setMsg(final String msg) { this.msg = msg; }

    private String serial;

    @ThriftField(value=3, name="serial")
    public String getSerial() { return serial; }

    @ThriftField
    public void setSerial(final String serial) { this.serial = serial; }

    @Override
    public String toString()
    {
        return toStringHelper(this)
            .add("code", code)
            .add("msg", msg)
            .add("serial", serial)
            .toString();
    }
}
