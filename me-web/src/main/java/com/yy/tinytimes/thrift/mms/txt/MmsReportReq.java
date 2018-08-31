package com.yy.tinytimes.thrift.mms.txt;

import com.facebook.swift.codec.ThriftField;
import com.facebook.swift.codec.ThriftStruct;

import java.util.List;

import static com.google.common.base.Objects.toStringHelper;

@ThriftStruct("MmsReportReq")
public class MmsReportReq
{
    public MmsReportReq() {
    }

    private MmsSign mmsSign;

    @ThriftField(value=1, name="mmsSign")
    public MmsSign getMmsSign() { return mmsSign; }

    @ThriftField
    public void setMmsSign(final MmsSign mmsSign) { this.mmsSign = mmsSign; }

    private String chid;

    @ThriftField(value=2, name="chid")
    public String getChid() { return chid; }

    @ThriftField
    public void setChid(final String chid) { this.chid = chid; }

    private String appid;

    @ThriftField(value=3, name="appid")
    public String getAppid() { return appid; }

    @ThriftField
    public void setAppid(final String appid) { this.appid = appid; }

    private List<MmsReport> reports;

    @ThriftField(value=4, name="reports")
    public List<MmsReport> getReports() { return reports; }

    @ThriftField
    public void setReports(final List<MmsReport> reports) { this.reports = reports; }

    @Override
    public String toString()
    {
        return toStringHelper(this)
            .add("mmsSign", mmsSign)
            .add("chid", chid)
            .add("appid", appid)
            .add("reports", reports)
            .toString();
    }
}
