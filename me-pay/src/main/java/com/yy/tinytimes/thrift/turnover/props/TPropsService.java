package com.yy.tinytimes.thrift.turnover.props;

import com.facebook.swift.codec.*;
import com.facebook.swift.service.*;
import com.google.common.util.concurrent.ListenableFuture;
import java.io.*;
import java.util.*;

@ThriftService("TPropsService")
public interface TPropsService
{
    @ThriftService("TPropsService")
    public interface Async
    {
        @ThriftMethod(value = "ping2")
        ListenableFuture<Void> ping2();

        @ThriftMethod(value = "getLastWeekRecvPropsRecByUid")
        ListenableFuture<List<TWeekPropsRecvInfo>> getLastWeekRecvPropsRecByUid(
            @ThriftField(value=1, name="uid") final long uid,
            @ThriftField(value=2, name="appid") final TAppId appid,
            @ThriftField(value=3, name="page") final int page,
            @ThriftField(value=4, name="pagesize") final int pagesize,
            @ThriftField(value=5, name="lastId") final long lastId
        );

        @ThriftMethod(value = "getPropsUrl")
        ListenableFuture<Map<Integer, String>> getPropsUrl(
            @ThriftField(value=1, name="appid") final TAppId appid,
            @ThriftField(value=2, name="usedChannel") final UsedChannelType usedChannel,
            @ThriftField(value=3, name="specField") final String specField
        );

        @ThriftMethod(value = "reportPropsUsedForRank")
        ListenableFuture<Integer> reportPropsUsedForRank(
            @ThriftField(value=1, name="usedUid") final long usedUid,
            @ThriftField(value=2, name="recvUid") final long recvUid,
            @ThriftField(value=3, name="appid") final TAppId appid,
            @ThriftField(value=4, name="currencyType") final TCurrencyType currencyType,
            @ThriftField(value=5, name="price") final int price,
            @ThriftField(value=6, name="count") final int count,
            @ThriftField(value=7, name="revenueRate") final int revenueRate,
            @ThriftField(value=8, name="revenueType") final int revenueType,
            @ThriftField(value=9, name="usedTime") final long usedTime,
            @ThriftField(value=10, name="seq") final String seq,
            @ThriftField(value=11, name="sid") final long sid,
            @ThriftField(value=12, name="ssid") final long ssid
        );
    }
    @ThriftMethod(value = "ping2")
    void ping2() throws com.yy.cs.center.remoting.RemotingException;

    @ThriftMethod(value = "getLastWeekRecvPropsRecByUid")
    List<TWeekPropsRecvInfo> getLastWeekRecvPropsRecByUid(
        @ThriftField(value=1, name="uid") final long uid,
        @ThriftField(value=2, name="appid") final TAppId appid,
        @ThriftField(value=3, name="page") final int page,
        @ThriftField(value=4, name="pagesize") final int pagesize,
        @ThriftField(value=5, name="lastId") final long lastId
    ) throws com.yy.cs.center.remoting.RemotingException;

    @ThriftMethod(value = "getPropsUrl")
    Map<Integer, String> getPropsUrl(
        @ThriftField(value=1, name="appid") final TAppId appid,
        @ThriftField(value=2, name="usedChannel") final UsedChannelType usedChannel,
        @ThriftField(value=3, name="specField") final String specField
    ) throws com.yy.cs.center.remoting.RemotingException;

    @ThriftMethod(value = "reportPropsUsedForRank")
    int reportPropsUsedForRank(
        @ThriftField(value=1, name="usedUid") final long usedUid,
        @ThriftField(value=2, name="recvUid") final long recvUid,
        @ThriftField(value=3, name="appid") final TAppId appid,
        @ThriftField(value=4, name="currencyType") final TCurrencyType currencyType,
        @ThriftField(value=5, name="price") final int price,
        @ThriftField(value=6, name="count") final int count,
        @ThriftField(value=7, name="revenueRate") final int revenueRate,
        @ThriftField(value=8, name="revenueType") final int revenueType,
        @ThriftField(value=9, name="usedTime") final long usedTime,
        @ThriftField(value=10, name="seq") final String seq,
        @ThriftField(value=11, name="sid") final long sid,
        @ThriftField(value=12, name="ssid") final long ssid
    ) throws com.yy.cs.center.remoting.RemotingException;
}