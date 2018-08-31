package com.yy.tinytimes.thrift.mms.txt;

import com.facebook.swift.codec.ThriftField;
import com.facebook.swift.codec.ThriftStruct;

import static com.google.common.base.Objects.toStringHelper;

@ThriftStruct("MmsReportAttc")
public class MmsReportAttc
{
    public MmsReportAttc() {
    }

    private String attcType;

    @ThriftField(value=1, name="attcType")
    public String getAttcType() { return attcType; }

    @ThriftField
    public void setAttcType(final String attcType) { this.attcType = attcType; }

    private String attcText;

    @ThriftField(value=2, name="attcText")
    public String getAttcText() { return attcText; }

    @ThriftField
    public void setAttcText(final String attcText) { this.attcText = attcText; }

    private String attcUrl;

    @ThriftField(value=3, name="attcUrl")
    public String getAttcUrl() { return attcUrl; }

    @ThriftField
    public void setAttcUrl(final String attcUrl) { this.attcUrl = attcUrl; }

    private byte [] attcFile;

    @ThriftField(value=4, name="attcFile")
    public byte [] getAttcFile() { return attcFile; }

    @ThriftField
    public void setAttcFile(final byte [] attcFile) { this.attcFile = attcFile; }

    @Override
    public String toString()
    {
        return toStringHelper(this)
            .add("attcType", attcType)
            .add("attcText", attcText)
            .add("attcUrl", attcUrl)
            .add("attcFile", attcFile)
            .toString();
    }
}
