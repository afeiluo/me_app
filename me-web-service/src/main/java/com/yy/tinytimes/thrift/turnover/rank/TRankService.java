package com.yy.tinytimes.thrift.turnover.rank;

import com.facebook.swift.codec.*;
import com.facebook.swift.service.*;
import com.google.common.util.concurrent.ListenableFuture;
import java.io.*;
import java.util.*;

@ThriftService("TRankService")
public interface TRankService
{
    @ThriftService("TRankService")
    public interface Async
    {
        @ThriftMethod(value = "ping2")
        ListenableFuture<Void> ping2();

        @ThriftMethod(value = "getOneRank")
        ListenableFuture<TRank> getOneRank(
                @ThriftField(value = 1, name = "code") final String code,
                @ThriftField(value = 2, name = "rtype") final String rtype,
                @ThriftField(value = 3, name = "ctype") final String ctype,
                @ThriftField(value = 4, name = "latest") final boolean latest,
                @ThriftField(value = 5, name = "id") final String id,
                @ThriftField(value = 6, name = "timeParam") final int timeParam
        );

        @ThriftMethod(value = "queryRank")
        ListenableFuture<List<TRank>> queryRank(
                @ThriftField(value = 1, name = "code") final String code,
                @ThriftField(value = 2, name = "rtype") final String rtype,
                @ThriftField(value = 3, name = "ctype") final String ctype,
                @ThriftField(value = 4, name = "latest") final boolean latest,
                @ThriftField(value = 5, name = "size") final int size,
                @ThriftField(value = 6, name = "timeParam") final int timeParam
        );
    }
    @ThriftMethod(value = "ping2")
    void ping2() throws com.yy.cs.center.remoting.RemotingException;

    @ThriftMethod(value = "getOneRank")
    TRank getOneRank(
            @ThriftField(value = 1, name = "code") final String code,
            @ThriftField(value = 2, name = "rtype") final String rtype,
            @ThriftField(value = 3, name = "ctype") final String ctype,
            @ThriftField(value = 4, name = "latest") final boolean latest,
            @ThriftField(value = 5, name = "id") final String id,
            @ThriftField(value = 6, name = "timeParam") final int timeParam
    ) throws com.yy.cs.center.remoting.RemotingException;

    @ThriftMethod(value = "queryRank")
    List<TRank> queryRank(
            @ThriftField(value = 1, name = "code") final String code,
            @ThriftField(value = 2, name = "rtype") final String rtype,
            @ThriftField(value = 3, name = "ctype") final String ctype,
            @ThriftField(value = 4, name = "latest") final boolean latest,
            @ThriftField(value = 5, name = "size") final int size,
            @ThriftField(value = 6, name = "timeParam") final int timeParam
    ) throws com.yy.cs.center.remoting.RemotingException;
}