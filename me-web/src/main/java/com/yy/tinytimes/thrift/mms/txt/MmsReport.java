package com.yy.tinytimes.thrift.mms.txt;

import com.facebook.swift.codec.ThriftField;
import com.facebook.swift.codec.ThriftStruct;

import java.util.List;

import static com.google.common.base.Objects.toStringHelper;

@ThriftStruct("MmsReport")
public class MmsReport
{
    public MmsReport() {
    }

    private String serial;

    @ThriftField(value=1, name="serial")
    public String getSerial() { return serial; }

    @ThriftField
    public void setSerial(final String serial) { this.serial = serial; }

    private long uid;

    @ThriftField(value=2, name="uid")
    public long getUid() { return uid; }

    @ThriftField
    public void setUid(final long uid) { this.uid = uid; }

    private String reportTime;

    @ThriftField(value=3, name="reportTime")
    public String getReportTime() { return reportTime; }

    @ThriftField
    public void setReportTime(final String reportTime) { this.reportTime = reportTime; }

    private String reportComment;

    @ThriftField(value=4, name="reportComment")
    public String getReportComment() { return reportComment; }

    @ThriftField
    public void setReportComment(final String reportComment) { this.reportComment = reportComment; }

    private List<MmsReportAttc> attachments;

    @ThriftField(value=5, name="attachments")
    public List<MmsReportAttc> getAttachments() { return attachments; }

    @ThriftField
    public void setAttachments(final List<MmsReportAttc> attachments) { this.attachments = attachments; }

    private long uploadUid;

    @ThriftField(value=6, name="uploadUid")
    public long getUploadUid() { return uploadUid; }

    @ThriftField
    public void setUploadUid(final long uploadUid) { this.uploadUid = uploadUid; }

    private String severity;

    @ThriftField(value=7, name="severity")
    public String getSeverity() { return severity; }

    @ThriftField
    public void setSeverity(final String severity) { this.severity = severity; }

    private long sid;

    @ThriftField(value=8, name="sid")
    public long getSid() { return sid; }

    @ThriftField
    public void setSid(final long sid) { this.sid = sid; }

    private long ssid;

    @ThriftField(value=9, name="ssid")
    public long getSsid() { return ssid; }

    @ThriftField
    public void setSsid(final long ssid) { this.ssid = ssid; }

    private long owid;

    @ThriftField(value=10, name="owid")
    public long getOwid() { return owid; }

    @ThriftField
    public void setOwid(final long owid) { this.owid = owid; }

    private long pcu;

    @ThriftField(value=11, name="pcu")
    public long getPcu() { return pcu; }

    @ThriftField
    public void setPcu(final long pcu) { this.pcu = pcu; }

    private String extPar;

    @ThriftField(value=12, name="extPar")
    public String getExtPar() { return extPar; }

    @ThriftField
    public void setExtPar(final String extPar) { this.extPar = extPar; }

    @Override
    public String toString()
    {
        return toStringHelper(this)
            .add("serial", serial)
            .add("uid", uid)
            .add("reportTime", reportTime)
            .add("reportComment", reportComment)
            .add("attachments", attachments)
            .add("uploadUid", uploadUid)
            .add("severity", severity)
            .add("sid", sid)
            .add("ssid", ssid)
            .add("owid", owid)
            .add("pcu", pcu)
            .add("extPar", extPar)
            .toString();
    }
}
