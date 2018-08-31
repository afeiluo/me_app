package com.yy.tinytimes.thrift.turnover.rank;

import com.facebook.swift.codec.ThriftField;
import com.facebook.swift.codec.ThriftStruct;

import static com.google.common.base.Objects.*;

@ThriftStruct("TRank")
public class TRank
{
    public TRank() {
    }

    private long uid;

    @ThriftField(value=1, name="uid")
    public long getUid() { return uid; }

    @ThriftField
    public void setUid(final long uid) { this.uid = uid; }

    private long value;

    @ThriftField(value=2, name="value")
    public long getValue() { return value; }

    @ThriftField
    public void setValue(final long value) { this.value = value; }

    private long rank;

    @ThriftField(value=3, name="rank")
    public long getRank() { return rank; }

    @ThriftField
    public void setRank(final long rank) { this.rank = rank; }

    @Override
    public String toString()
    {
        return toStringHelper(this)
            .add("uid", uid)
            .add("value", value)
            .add("rank", rank)
            .toString();
    }
}
