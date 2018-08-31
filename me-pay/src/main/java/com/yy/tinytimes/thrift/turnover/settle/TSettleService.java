package com.yy.tinytimes.thrift.turnover.settle;

import com.facebook.swift.codec.ThriftField;
import com.facebook.swift.service.ThriftException;
import com.facebook.swift.service.ThriftMethod;
import com.facebook.swift.service.ThriftService;
import com.google.common.util.concurrent.ListenableFuture;

@ThriftService("TSettleService")
public interface TSettleService
{
    @ThriftService("TSettleService")
    public interface Async
    {
        @ThriftMethod(value = "userMonthSettle")
        ListenableFuture<Integer> userMonthSettle(
                @ThriftField(value = 1, name = "uid") final long uid,
                @ThriftField(value = 2, name = "appid") final int appid,
                @ThriftField(value = 3, name = "sid") final long sid,
                @ThriftField(value = 4, name = "withDrawAmount") final long withDrawAmount,
                @ThriftField(value = 5, name = "withDrawAccount") final String withDrawAccount,
                @ThriftField(value = 6, name = "withDrawAccountType") final int withDrawAccountType,
                @ThriftField(value = 7, name = "userInfoExpand") final String userInfoExpand
        );

        @ThriftMethod(value = "userMonthSettleCheck")
        ListenableFuture<Integer> userMonthSettleCheck(
                @ThriftField(value = 1, name = "uid") final long uid,
                @ThriftField(value = 2, name = "appid") final int appid,
                @ThriftField(value = 3, name = "sid") final long sid,
                @ThriftField(value = 4, name = "withDrawAmount") final long withDrawAmount,
                @ThriftField(value = 5, name = "withDrawAccount") final String withDrawAccount,
                @ThriftField(value = 6, name = "withDrawAccountType") final int withDrawAccountType
        );
    }
    @ThriftMethod(value = "userMonthSettle",
                  exception = {
                      @ThriftException(type=TServiceException.class, id=1)
                  })
    int userMonthSettle(
            @ThriftField(value = 1, name = "uid") final long uid,
            @ThriftField(value = 2, name = "appid") final int appid,
            @ThriftField(value = 3, name = "sid") final long sid,
            @ThriftField(value = 4, name = "withDrawAmount") final long withDrawAmount,
            @ThriftField(value = 5, name = "withDrawAccount") final String withDrawAccount,
            @ThriftField(value = 6, name = "withDrawAccountType") final int withDrawAccountType,
            @ThriftField(value = 7, name = "userInfoExpand") final String userInfoExpand
    ) throws TServiceException, com.yy.cs.center.remoting.RemotingException;

    @ThriftMethod(value = "userMonthSettleCheck",
                  exception = {
                      @ThriftException(type=TServiceException.class, id=1)
                  })
    int userMonthSettleCheck(
            @ThriftField(value = 1, name = "uid") final long uid,
            @ThriftField(value = 2, name = "appid") final int appid,
            @ThriftField(value = 3, name = "sid") final long sid,
            @ThriftField(value = 4, name = "withDrawAmount") final long withDrawAmount,
            @ThriftField(value = 5, name = "withDrawAccount") final String withDrawAccount,
            @ThriftField(value = 6, name = "withDrawAccountType") final int withDrawAccountType
    ) throws TServiceException, com.yy.cs.center.remoting.RemotingException;
}