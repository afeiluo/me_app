package com.yy.tinytimes.thrift.mms.txt;

import com.facebook.swift.codec.ThriftField;
import com.facebook.swift.service.ThriftMethod;
import com.facebook.swift.service.ThriftService;
import com.google.common.util.concurrent.ListenableFuture;

@ThriftService("MmsReportServ")
public interface MmsReportServ extends com.yy.tinytimes.thrift.mms.txt.BaseMmsThriftServ
{
    @ThriftService("MmsReportServ")
    public interface Async extends com.yy.tinytimes.thrift.mms.txt.BaseMmsThriftServ.Async
    {
        @ThriftMethod(value = "pushReports")
        ListenableFuture<MmsReportRsp> pushReports(
                @ThriftField(value = 1, name = "mmsReportReq") final MmsReportReq mmsReportReq
        );

        @ThriftMethod(value = "pushReportsCmd")
        ListenableFuture<MmsReportCmdRsp> pushReportsCmd(
                @ThriftField(value = 1, name = "mmsReportCmdReq") final MmsReportCmdReq mmsReportCmdReq
        );
    }
    @ThriftMethod(value = "pushReports")
    MmsReportRsp pushReports(
            @ThriftField(value = 1, name = "mmsReportReq") final MmsReportReq mmsReportReq
    ) throws com.yy.cs.center.remoting.RemotingException;

    @ThriftMethod(value = "pushReportsCmd")
    MmsReportCmdRsp pushReportsCmd(
            @ThriftField(value = 1, name = "mmsReportCmdReq") final MmsReportCmdReq mmsReportCmdReq
    ) throws com.yy.cs.center.remoting.RemotingException;
}