/**
 * Autogenerated by Thrift Compiler (0.9.1)
 *
 * DO NOT EDIT UNLESS YOU ARE SURE THAT YOU KNOW WHAT YOU ARE DOING
 *  @generated
 */
package com.yy.tinytimes.thrift.mms.server;

import org.apache.thrift.scheme.IScheme;
import org.apache.thrift.scheme.SchemeFactory;
import org.apache.thrift.scheme.StandardScheme;

import org.apache.thrift.scheme.TupleScheme;
import org.apache.thrift.protocol.TTupleProtocol;
import org.apache.thrift.protocol.TProtocolException;
import org.apache.thrift.EncodingUtils;
import org.apache.thrift.TException;
import org.apache.thrift.async.AsyncMethodCallback;
import org.apache.thrift.server.AbstractNonblockingServer.*;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.util.EnumMap;
import java.util.Set;
import java.util.HashSet;
import java.util.EnumSet;
import java.util.Collections;
import java.util.BitSet;
import java.nio.ByteBuffer;
import java.util.Arrays;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 监控/举报信息上传请求包
 */
public class MmsReportReq implements org.apache.thrift.TBase<MmsReportReq, MmsReportReq._Fields>, java.io.Serializable, Cloneable, Comparable<MmsReportReq> {
  private static final org.apache.thrift.protocol.TStruct STRUCT_DESC = new org.apache.thrift.protocol.TStruct("MmsReportReq");

  private static final org.apache.thrift.protocol.TField MMS_SIGN_FIELD_DESC = new org.apache.thrift.protocol.TField("mmsSign", org.apache.thrift.protocol.TType.STRUCT, (short)1);
  private static final org.apache.thrift.protocol.TField CHID_FIELD_DESC = new org.apache.thrift.protocol.TField("chid", org.apache.thrift.protocol.TType.STRING, (short)2);
  private static final org.apache.thrift.protocol.TField APPID_FIELD_DESC = new org.apache.thrift.protocol.TField("appid", org.apache.thrift.protocol.TType.STRING, (short)3);
  private static final org.apache.thrift.protocol.TField REPORTS_FIELD_DESC = new org.apache.thrift.protocol.TField("reports", org.apache.thrift.protocol.TType.LIST, (short)4);

  private static final Map<Class<? extends IScheme>, SchemeFactory> schemes = new HashMap<Class<? extends IScheme>, SchemeFactory>();
  static {
    schemes.put(StandardScheme.class, new MmsReportReqStandardSchemeFactory());
    schemes.put(TupleScheme.class, new MmsReportReqTupleSchemeFactory());
  }

  /**
   * 认证签名
   */
  public MmsSign mmsSign; // required
  /**
   * 通道ID
   */
  public String chid; // required
  /**
   * 应用appid
   */
  public String appid; // required
  /**
   * 上报材料
   */
  public List<MmsReport> reports; // required

  /** The set of fields this struct contains, along with convenience methods for finding and manipulating them. */
  public enum _Fields implements org.apache.thrift.TFieldIdEnum {
    /**
     * 认证签名
     */
    MMS_SIGN((short)1, "mmsSign"),
    /**
     * 通道ID
     */
    CHID((short)2, "chid"),
    /**
     * 应用appid
     */
    APPID((short)3, "appid"),
    /**
     * 上报材料
     */
    REPORTS((short)4, "reports");

    private static final Map<String, _Fields> byName = new HashMap<String, _Fields>();

    static {
      for (_Fields field : EnumSet.allOf(_Fields.class)) {
        byName.put(field.getFieldName(), field);
      }
    }

    /**
     * Find the _Fields constant that matches fieldId, or null if its not found.
     */
    public static _Fields findByThriftId(int fieldId) {
      switch(fieldId) {
        case 1: // MMS_SIGN
          return MMS_SIGN;
        case 2: // CHID
          return CHID;
        case 3: // APPID
          return APPID;
        case 4: // REPORTS
          return REPORTS;
        default:
          return null;
      }
    }

    /**
     * Find the _Fields constant that matches fieldId, throwing an exception
     * if it is not found.
     */
    public static _Fields findByThriftIdOrThrow(int fieldId) {
      _Fields fields = findByThriftId(fieldId);
      if (fields == null) throw new IllegalArgumentException("Field " + fieldId + " doesn't exist!");
      return fields;
    }

    /**
     * Find the _Fields constant that matches name, or null if its not found.
     */
    public static _Fields findByName(String name) {
      return byName.get(name);
    }

    private final short _thriftId;
    private final String _fieldName;

    _Fields(short thriftId, String fieldName) {
      _thriftId = thriftId;
      _fieldName = fieldName;
    }

    public short getThriftFieldId() {
      return _thriftId;
    }

    public String getFieldName() {
      return _fieldName;
    }
  }

  // isset id assignments
  public static final Map<_Fields, org.apache.thrift.meta_data.FieldMetaData> metaDataMap;
  static {
    Map<_Fields, org.apache.thrift.meta_data.FieldMetaData> tmpMap = new EnumMap<_Fields, org.apache.thrift.meta_data.FieldMetaData>(_Fields.class);
    tmpMap.put(_Fields.MMS_SIGN, new org.apache.thrift.meta_data.FieldMetaData("mmsSign", org.apache.thrift.TFieldRequirementType.DEFAULT, 
        new org.apache.thrift.meta_data.StructMetaData(org.apache.thrift.protocol.TType.STRUCT, MmsSign.class)));
    tmpMap.put(_Fields.CHID, new org.apache.thrift.meta_data.FieldMetaData("chid", org.apache.thrift.TFieldRequirementType.DEFAULT, 
        new org.apache.thrift.meta_data.FieldValueMetaData(org.apache.thrift.protocol.TType.STRING)));
    tmpMap.put(_Fields.APPID, new org.apache.thrift.meta_data.FieldMetaData("appid", org.apache.thrift.TFieldRequirementType.DEFAULT, 
        new org.apache.thrift.meta_data.FieldValueMetaData(org.apache.thrift.protocol.TType.STRING)));
    tmpMap.put(_Fields.REPORTS, new org.apache.thrift.meta_data.FieldMetaData("reports", org.apache.thrift.TFieldRequirementType.DEFAULT, 
        new org.apache.thrift.meta_data.ListMetaData(org.apache.thrift.protocol.TType.LIST, 
            new org.apache.thrift.meta_data.StructMetaData(org.apache.thrift.protocol.TType.STRUCT, MmsReport.class))));
    metaDataMap = Collections.unmodifiableMap(tmpMap);
    org.apache.thrift.meta_data.FieldMetaData.addStructMetaDataMap(MmsReportReq.class, metaDataMap);
  }

  public MmsReportReq() {
  }

  public MmsReportReq(
    MmsSign mmsSign,
    String chid,
    String appid,
    List<MmsReport> reports)
  {
    this();
    this.mmsSign = mmsSign;
    this.chid = chid;
    this.appid = appid;
    this.reports = reports;
  }

  /**
   * Performs a deep copy on <i>other</i>.
   */
  public MmsReportReq(MmsReportReq other) {
    if (other.isSetMmsSign()) {
      this.mmsSign = new MmsSign(other.mmsSign);
    }
    if (other.isSetChid()) {
      this.chid = other.chid;
    }
    if (other.isSetAppid()) {
      this.appid = other.appid;
    }
    if (other.isSetReports()) {
      List<MmsReport> __this__reports = new ArrayList<MmsReport>(other.reports.size());
      for (MmsReport other_element : other.reports) {
        __this__reports.add(new MmsReport(other_element));
      }
      this.reports = __this__reports;
    }
  }

  public MmsReportReq deepCopy() {
    return new MmsReportReq(this);
  }

  @Override
  public void clear() {
    this.mmsSign = null;
    this.chid = null;
    this.appid = null;
    this.reports = null;
  }

  /**
   * 认证签名
   */
  public MmsSign getMmsSign() {
    return this.mmsSign;
  }

  /**
   * 认证签名
   */
  public MmsReportReq setMmsSign(MmsSign mmsSign) {
    this.mmsSign = mmsSign;
    return this;
  }

  public void unsetMmsSign() {
    this.mmsSign = null;
  }

  /** Returns true if field mmsSign is set (has been assigned a value) and false otherwise */
  public boolean isSetMmsSign() {
    return this.mmsSign != null;
  }

  public void setMmsSignIsSet(boolean value) {
    if (!value) {
      this.mmsSign = null;
    }
  }

  /**
   * 通道ID
   */
  public String getChid() {
    return this.chid;
  }

  /**
   * 通道ID
   */
  public MmsReportReq setChid(String chid) {
    this.chid = chid;
    return this;
  }

  public void unsetChid() {
    this.chid = null;
  }

  /** Returns true if field chid is set (has been assigned a value) and false otherwise */
  public boolean isSetChid() {
    return this.chid != null;
  }

  public void setChidIsSet(boolean value) {
    if (!value) {
      this.chid = null;
    }
  }

  /**
   * 应用appid
   */
  public String getAppid() {
    return this.appid;
  }

  /**
   * 应用appid
   */
  public MmsReportReq setAppid(String appid) {
    this.appid = appid;
    return this;
  }

  public void unsetAppid() {
    this.appid = null;
  }

  /** Returns true if field appid is set (has been assigned a value) and false otherwise */
  public boolean isSetAppid() {
    return this.appid != null;
  }

  public void setAppidIsSet(boolean value) {
    if (!value) {
      this.appid = null;
    }
  }

  public int getReportsSize() {
    return (this.reports == null) ? 0 : this.reports.size();
  }

  public java.util.Iterator<MmsReport> getReportsIterator() {
    return (this.reports == null) ? null : this.reports.iterator();
  }

  public void addToReports(MmsReport elem) {
    if (this.reports == null) {
      this.reports = new ArrayList<MmsReport>();
    }
    this.reports.add(elem);
  }

  /**
   * 上报材料
   */
  public List<MmsReport> getReports() {
    return this.reports;
  }

  /**
   * 上报材料
   */
  public MmsReportReq setReports(List<MmsReport> reports) {
    this.reports = reports;
    return this;
  }

  public void unsetReports() {
    this.reports = null;
  }

  /** Returns true if field reports is set (has been assigned a value) and false otherwise */
  public boolean isSetReports() {
    return this.reports != null;
  }

  public void setReportsIsSet(boolean value) {
    if (!value) {
      this.reports = null;
    }
  }

  public void setFieldValue(_Fields field, Object value) {
    switch (field) {
    case MMS_SIGN:
      if (value == null) {
        unsetMmsSign();
      } else {
        setMmsSign((MmsSign)value);
      }
      break;

    case CHID:
      if (value == null) {
        unsetChid();
      } else {
        setChid((String)value);
      }
      break;

    case APPID:
      if (value == null) {
        unsetAppid();
      } else {
        setAppid((String)value);
      }
      break;

    case REPORTS:
      if (value == null) {
        unsetReports();
      } else {
        setReports((List<MmsReport>)value);
      }
      break;

    }
  }

  public Object getFieldValue(_Fields field) {
    switch (field) {
    case MMS_SIGN:
      return getMmsSign();

    case CHID:
      return getChid();

    case APPID:
      return getAppid();

    case REPORTS:
      return getReports();

    }
    throw new IllegalStateException();
  }

  /** Returns true if field corresponding to fieldID is set (has been assigned a value) and false otherwise */
  public boolean isSet(_Fields field) {
    if (field == null) {
      throw new IllegalArgumentException();
    }

    switch (field) {
    case MMS_SIGN:
      return isSetMmsSign();
    case CHID:
      return isSetChid();
    case APPID:
      return isSetAppid();
    case REPORTS:
      return isSetReports();
    }
    throw new IllegalStateException();
  }

  @Override
  public boolean equals(Object that) {
    if (that == null)
      return false;
    if (that instanceof MmsReportReq)
      return this.equals((MmsReportReq)that);
    return false;
  }

  public boolean equals(MmsReportReq that) {
    if (that == null)
      return false;

    boolean this_present_mmsSign = true && this.isSetMmsSign();
    boolean that_present_mmsSign = true && that.isSetMmsSign();
    if (this_present_mmsSign || that_present_mmsSign) {
      if (!(this_present_mmsSign && that_present_mmsSign))
        return false;
      if (!this.mmsSign.equals(that.mmsSign))
        return false;
    }

    boolean this_present_chid = true && this.isSetChid();
    boolean that_present_chid = true && that.isSetChid();
    if (this_present_chid || that_present_chid) {
      if (!(this_present_chid && that_present_chid))
        return false;
      if (!this.chid.equals(that.chid))
        return false;
    }

    boolean this_present_appid = true && this.isSetAppid();
    boolean that_present_appid = true && that.isSetAppid();
    if (this_present_appid || that_present_appid) {
      if (!(this_present_appid && that_present_appid))
        return false;
      if (!this.appid.equals(that.appid))
        return false;
    }

    boolean this_present_reports = true && this.isSetReports();
    boolean that_present_reports = true && that.isSetReports();
    if (this_present_reports || that_present_reports) {
      if (!(this_present_reports && that_present_reports))
        return false;
      if (!this.reports.equals(that.reports))
        return false;
    }

    return true;
  }

  @Override
  public int hashCode() {
    return 0;
  }

  @Override
  public int compareTo(MmsReportReq other) {
    if (!getClass().equals(other.getClass())) {
      return getClass().getName().compareTo(other.getClass().getName());
    }

    int lastComparison = 0;

    lastComparison = Boolean.valueOf(isSetMmsSign()).compareTo(other.isSetMmsSign());
    if (lastComparison != 0) {
      return lastComparison;
    }
    if (isSetMmsSign()) {
      lastComparison = org.apache.thrift.TBaseHelper.compareTo(this.mmsSign, other.mmsSign);
      if (lastComparison != 0) {
        return lastComparison;
      }
    }
    lastComparison = Boolean.valueOf(isSetChid()).compareTo(other.isSetChid());
    if (lastComparison != 0) {
      return lastComparison;
    }
    if (isSetChid()) {
      lastComparison = org.apache.thrift.TBaseHelper.compareTo(this.chid, other.chid);
      if (lastComparison != 0) {
        return lastComparison;
      }
    }
    lastComparison = Boolean.valueOf(isSetAppid()).compareTo(other.isSetAppid());
    if (lastComparison != 0) {
      return lastComparison;
    }
    if (isSetAppid()) {
      lastComparison = org.apache.thrift.TBaseHelper.compareTo(this.appid, other.appid);
      if (lastComparison != 0) {
        return lastComparison;
      }
    }
    lastComparison = Boolean.valueOf(isSetReports()).compareTo(other.isSetReports());
    if (lastComparison != 0) {
      return lastComparison;
    }
    if (isSetReports()) {
      lastComparison = org.apache.thrift.TBaseHelper.compareTo(this.reports, other.reports);
      if (lastComparison != 0) {
        return lastComparison;
      }
    }
    return 0;
  }

  public _Fields fieldForId(int fieldId) {
    return _Fields.findByThriftId(fieldId);
  }

  public void read(org.apache.thrift.protocol.TProtocol iprot) throws org.apache.thrift.TException {
    schemes.get(iprot.getScheme()).getScheme().read(iprot, this);
  }

  public void write(org.apache.thrift.protocol.TProtocol oprot) throws org.apache.thrift.TException {
    schemes.get(oprot.getScheme()).getScheme().write(oprot, this);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder("MmsReportReq(");
    boolean first = true;

    sb.append("mmsSign:");
    if (this.mmsSign == null) {
      sb.append("null");
    } else {
      sb.append(this.mmsSign);
    }
    first = false;
    if (!first) sb.append(", ");
    sb.append("chid:");
    if (this.chid == null) {
      sb.append("null");
    } else {
      sb.append(this.chid);
    }
    first = false;
    if (!first) sb.append(", ");
    sb.append("appid:");
    if (this.appid == null) {
      sb.append("null");
    } else {
      sb.append(this.appid);
    }
    first = false;
    if (!first) sb.append(", ");
    sb.append("reports:");
    if (this.reports == null) {
      sb.append("null");
    } else {
      sb.append(this.reports);
    }
    first = false;
    sb.append(")");
    return sb.toString();
  }

  public void validate() throws org.apache.thrift.TException {
    // check for required fields
    // check for sub-struct validity
    if (mmsSign != null) {
      mmsSign.validate();
    }
  }

  private void writeObject(java.io.ObjectOutputStream out) throws java.io.IOException {
    try {
      write(new org.apache.thrift.protocol.TCompactProtocol(new org.apache.thrift.transport.TIOStreamTransport(out)));
    } catch (org.apache.thrift.TException te) {
      throw new java.io.IOException(te);
    }
  }

  private void readObject(java.io.ObjectInputStream in) throws java.io.IOException, ClassNotFoundException {
    try {
      read(new org.apache.thrift.protocol.TCompactProtocol(new org.apache.thrift.transport.TIOStreamTransport(in)));
    } catch (org.apache.thrift.TException te) {
      throw new java.io.IOException(te);
    }
  }

  private static class MmsReportReqStandardSchemeFactory implements SchemeFactory {
    public MmsReportReqStandardScheme getScheme() {
      return new MmsReportReqStandardScheme();
    }
  }

  private static class MmsReportReqStandardScheme extends StandardScheme<MmsReportReq> {

    public void read(org.apache.thrift.protocol.TProtocol iprot, MmsReportReq struct) throws org.apache.thrift.TException {
      org.apache.thrift.protocol.TField schemeField;
      iprot.readStructBegin();
      while (true)
      {
        schemeField = iprot.readFieldBegin();
        if (schemeField.type == org.apache.thrift.protocol.TType.STOP) { 
          break;
        }
        switch (schemeField.id) {
          case 1: // MMS_SIGN
            if (schemeField.type == org.apache.thrift.protocol.TType.STRUCT) {
              struct.mmsSign = new MmsSign();
              struct.mmsSign.read(iprot);
              struct.setMmsSignIsSet(true);
            } else { 
              org.apache.thrift.protocol.TProtocolUtil.skip(iprot, schemeField.type);
            }
            break;
          case 2: // CHID
            if (schemeField.type == org.apache.thrift.protocol.TType.STRING) {
              struct.chid = iprot.readString();
              struct.setChidIsSet(true);
            } else { 
              org.apache.thrift.protocol.TProtocolUtil.skip(iprot, schemeField.type);
            }
            break;
          case 3: // APPID
            if (schemeField.type == org.apache.thrift.protocol.TType.STRING) {
              struct.appid = iprot.readString();
              struct.setAppidIsSet(true);
            } else { 
              org.apache.thrift.protocol.TProtocolUtil.skip(iprot, schemeField.type);
            }
            break;
          case 4: // REPORTS
            if (schemeField.type == org.apache.thrift.protocol.TType.LIST) {
              {
                org.apache.thrift.protocol.TList _list16 = iprot.readListBegin();
                struct.reports = new ArrayList<MmsReport>(_list16.size);
                for (int _i17 = 0; _i17 < _list16.size; ++_i17)
                {
                  MmsReport _elem18;
                  _elem18 = new MmsReport();
                  _elem18.read(iprot);
                  struct.reports.add(_elem18);
                }
                iprot.readListEnd();
              }
              struct.setReportsIsSet(true);
            } else { 
              org.apache.thrift.protocol.TProtocolUtil.skip(iprot, schemeField.type);
            }
            break;
          default:
            org.apache.thrift.protocol.TProtocolUtil.skip(iprot, schemeField.type);
        }
        iprot.readFieldEnd();
      }
      iprot.readStructEnd();

      // check for required fields of primitive type, which can't be checked in the validate method
      struct.validate();
    }

    public void write(org.apache.thrift.protocol.TProtocol oprot, MmsReportReq struct) throws org.apache.thrift.TException {
      struct.validate();

      oprot.writeStructBegin(STRUCT_DESC);
      if (struct.mmsSign != null) {
        oprot.writeFieldBegin(MMS_SIGN_FIELD_DESC);
        struct.mmsSign.write(oprot);
        oprot.writeFieldEnd();
      }
      if (struct.chid != null) {
        oprot.writeFieldBegin(CHID_FIELD_DESC);
        oprot.writeString(struct.chid);
        oprot.writeFieldEnd();
      }
      if (struct.appid != null) {
        oprot.writeFieldBegin(APPID_FIELD_DESC);
        oprot.writeString(struct.appid);
        oprot.writeFieldEnd();
      }
      if (struct.reports != null) {
        oprot.writeFieldBegin(REPORTS_FIELD_DESC);
        {
          oprot.writeListBegin(new org.apache.thrift.protocol.TList(org.apache.thrift.protocol.TType.STRUCT, struct.reports.size()));
          for (MmsReport _iter19 : struct.reports)
          {
            _iter19.write(oprot);
          }
          oprot.writeListEnd();
        }
        oprot.writeFieldEnd();
      }
      oprot.writeFieldStop();
      oprot.writeStructEnd();
    }

  }

  private static class MmsReportReqTupleSchemeFactory implements SchemeFactory {
    public MmsReportReqTupleScheme getScheme() {
      return new MmsReportReqTupleScheme();
    }
  }

  private static class MmsReportReqTupleScheme extends TupleScheme<MmsReportReq> {

    @Override
    public void write(org.apache.thrift.protocol.TProtocol prot, MmsReportReq struct) throws org.apache.thrift.TException {
      TTupleProtocol oprot = (TTupleProtocol) prot;
      BitSet optionals = new BitSet();
      if (struct.isSetMmsSign()) {
        optionals.set(0);
      }
      if (struct.isSetChid()) {
        optionals.set(1);
      }
      if (struct.isSetAppid()) {
        optionals.set(2);
      }
      if (struct.isSetReports()) {
        optionals.set(3);
      }
      oprot.writeBitSet(optionals, 4);
      if (struct.isSetMmsSign()) {
        struct.mmsSign.write(oprot);
      }
      if (struct.isSetChid()) {
        oprot.writeString(struct.chid);
      }
      if (struct.isSetAppid()) {
        oprot.writeString(struct.appid);
      }
      if (struct.isSetReports()) {
        {
          oprot.writeI32(struct.reports.size());
          for (MmsReport _iter20 : struct.reports)
          {
            _iter20.write(oprot);
          }
        }
      }
    }

    @Override
    public void read(org.apache.thrift.protocol.TProtocol prot, MmsReportReq struct) throws org.apache.thrift.TException {
      TTupleProtocol iprot = (TTupleProtocol) prot;
      BitSet incoming = iprot.readBitSet(4);
      if (incoming.get(0)) {
        struct.mmsSign = new MmsSign();
        struct.mmsSign.read(iprot);
        struct.setMmsSignIsSet(true);
      }
      if (incoming.get(1)) {
        struct.chid = iprot.readString();
        struct.setChidIsSet(true);
      }
      if (incoming.get(2)) {
        struct.appid = iprot.readString();
        struct.setAppidIsSet(true);
      }
      if (incoming.get(3)) {
        {
          org.apache.thrift.protocol.TList _list21 = new org.apache.thrift.protocol.TList(org.apache.thrift.protocol.TType.STRUCT, iprot.readI32());
          struct.reports = new ArrayList<MmsReport>(_list21.size);
          for (int _i22 = 0; _i22 < _list21.size; ++_i22)
          {
            MmsReport _elem23;
            _elem23 = new MmsReport();
            _elem23.read(iprot);
            struct.reports.add(_elem23);
          }
        }
        struct.setReportsIsSet(true);
      }
    }
  }

}
