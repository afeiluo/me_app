package com.yy.tinytimes.thrift.mms.txt;

import com.facebook.swift.codec.ThriftField;
import com.facebook.swift.codec.ThriftStruct;

import static com.google.common.base.Objects.toStringHelper;

@ThriftStruct("MmsReportCmdRsp")
public class MmsReportCmdRsp
{
    public MmsReportCmdRsp() {
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

    @Override
    public String toString()
    {
        return toStringHelper(this)
            .add("code", code)
            .add("msg", msg)
            .toString();
    }
}
