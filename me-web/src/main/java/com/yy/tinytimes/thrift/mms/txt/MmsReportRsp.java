package com.yy.tinytimes.thrift.mms.txt;

import com.facebook.swift.codec.ThriftField;
import com.facebook.swift.codec.ThriftStruct;

import java.util.List;

import static com.google.common.base.Objects.toStringHelper;

@ThriftStruct("MmsReportRsp")
public class MmsReportRsp
{
    public MmsReportRsp() {
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

    private List<MmsReportRspRec> mmsReportRspRecs;

    @ThriftField(value=3, name="mmsReportRspRecs")
    public List<MmsReportRspRec> getMmsReportRspRecs() { return mmsReportRspRecs; }

    @ThriftField
    public void setMmsReportRspRecs(final List<MmsReportRspRec> mmsReportRspRecs) { this.mmsReportRspRecs = mmsReportRspRecs; }

    @Override
    public String toString()
    {
        return toStringHelper(this)
            .add("code", code)
            .add("msg", msg)
            .add("mmsReportRspRecs", mmsReportRspRecs)
            .toString();
    }
}
