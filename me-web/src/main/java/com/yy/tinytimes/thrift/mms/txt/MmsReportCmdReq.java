package com.yy.tinytimes.thrift.mms.txt;

import com.facebook.swift.codec.ThriftField;
import com.facebook.swift.codec.ThriftStruct;

import static com.google.common.base.Objects.toStringHelper;

@ThriftStruct("MmsReportCmdReq")
public class MmsReportCmdReq
{
    public MmsReportCmdReq() {
    }

    private String appKey;

    @ThriftField(value=1, name="appKey")
    public String getAppKey() { return appKey; }

    @ThriftField
    public void setAppKey(final String appKey) { this.appKey = appKey; }

    private String serial;

    @ThriftField(value=2, name="serial")
    public String getSerial() { return serial; }

    @ThriftField
    public void setSerial(final String serial) { this.serial = serial; }

    private String cmd;

    @ThriftField(value=3, name="cmd")
    public String getCmd() { return cmd; }

    @ThriftField
    public void setCmd(final String cmd) { this.cmd = cmd; }

    private String reason;

    @ThriftField(value=4, name="reason")
    public String getReason() { return reason; }

    @ThriftField
    public void setReason(final String reason) { this.reason = reason; }

    private String msg;

    @ThriftField(value=5, name="msg")
    public String getMsg() { return msg; }

    @ThriftField
    public void setMsg(final String msg) { this.msg = msg; }

    private String extPar;

    @ThriftField(value=6, name="extPar")
    public String getExtPar() { return extPar; }

    @ThriftField
    public void setExtPar(final String extPar) { this.extPar = extPar; }

    private String sign;

    @ThriftField(value=7, name="sign")
    public String getSign() { return sign; }

    @ThriftField
    public void setSign(final String sign) { this.sign = sign; }

    private String status;

    @ThriftField(value=8, name="status")
    public String getStatus() { return status; }

    @ThriftField
    public void setStatus(final String status) { this.status = status; }

    @Override
    public String toString()
    {
        return toStringHelper(this)
            .add("appKey", appKey)
            .add("serial", serial)
            .add("cmd", cmd)
            .add("reason", reason)
            .add("msg", msg)
            .add("extPar", extPar)
            .add("sign", sign)
            .add("status", status)
            .toString();
    }
}
