package com.yy.tinytimes.thrift.mms.txt;

import com.facebook.swift.codec.ThriftField;
import com.facebook.swift.codec.ThriftStruct;

import java.util.List;

import static com.google.common.base.Objects.toStringHelper;

@ThriftStruct("MmsSign")
public class MmsSign
{
    public MmsSign() {
    }

    private String appKey;

    @ThriftField(value=1, name="appKey")
    public String getAppKey() { return appKey; }

    @ThriftField
    public void setAppKey(final String appKey) { this.appKey = appKey; }

    private String sign;

    @ThriftField(value=2, name="sign")
    public String getSign() { return sign; }

    @ThriftField
    public void setSign(final String sign) { this.sign = sign; }

    private List<String> signParNames;

    @ThriftField(value=3, name="signParNames")
    public List<String> getSignParNames() { return signParNames; }

    @ThriftField
    public void setSignParNames(final List<String> signParNames) { this.signParNames = signParNames; }

    @Override
    public String toString()
    {
        return toStringHelper(this)
            .add("appKey", appKey)
            .add("sign", sign)
            .add("signParNames", signParNames)
            .toString();
    }
}
