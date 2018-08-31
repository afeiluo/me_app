package com.yy.tinytimes.thrift.mms.txt;

import com.facebook.swift.codec.ThriftField;
import com.facebook.swift.service.ThriftMethod;
import com.facebook.swift.service.ThriftService;
import com.google.common.util.concurrent.ListenableFuture;

@ThriftService("BaseMmsThriftServ")
public interface BaseMmsThriftServ
{
    @ThriftService("BaseMmsThriftServ")
    public interface Async
    {
        @ThriftMethod(value = "ping")
        ListenableFuture<Void> ping(
                @ThriftField(value = 1, name = "randomId") final int randomId
        );
    }
    @ThriftMethod(value = "ping")
    void ping(
            @ThriftField(value = 1, name = "randomId") final int randomId
    ) throws com.yy.cs.center.remoting.RemotingException;
}