/**
 * Autogenerated by Thrift Compiler (0.9.1)
 *
 * DO NOT EDIT UNLESS YOU ARE SURE THAT YOU KNOW WHAT YOU ARE DOING
 *  @generated
 */
package com.yy.tinytimes.thrift.mms.keywords;

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

public class Result implements org.apache.thrift.TBase<Result, Result._Fields>, java.io.Serializable, Cloneable, Comparable<Result> {
  private static final org.apache.thrift.protocol.TStruct STRUCT_DESC = new org.apache.thrift.protocol.TStruct("Result");

  private static final org.apache.thrift.protocol.TField SUCCESS_FIELD_DESC = new org.apache.thrift.protocol.TField("success", org.apache.thrift.protocol.TType.BOOL, (short)1);
  private static final org.apache.thrift.protocol.TField KEYWORD_LIST_FIELD_DESC = new org.apache.thrift.protocol.TField("keywordList", org.apache.thrift.protocol.TType.LIST, (short)2);
  private static final org.apache.thrift.protocol.TField REGULAR_FIELD_DESC = new org.apache.thrift.protocol.TField("regular", org.apache.thrift.protocol.TType.STRING, (short)3);
  private static final org.apache.thrift.protocol.TField MESSAGE_FIELD_DESC = new org.apache.thrift.protocol.TField("message", org.apache.thrift.protocol.TType.STRING, (short)4);

  private static final Map<Class<? extends IScheme>, SchemeFactory> schemes = new HashMap<Class<? extends IScheme>, SchemeFactory>();
  static {
    schemes.put(StandardScheme.class, new ResultStandardSchemeFactory());
    schemes.put(TupleScheme.class, new ResultTupleSchemeFactory());
  }

  public boolean success; // required
  public List<String> keywordList; // required
  public String regular; // required
  public String message; // required

  /** The set of fields this struct contains, along with convenience methods for finding and manipulating them. */
  public enum _Fields implements org.apache.thrift.TFieldIdEnum {
    SUCCESS((short)1, "success"),
    KEYWORD_LIST((short)2, "keywordList"),
    REGULAR((short)3, "regular"),
    MESSAGE((short)4, "message");

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
        case 1: // SUCCESS
          return SUCCESS;
        case 2: // KEYWORD_LIST
          return KEYWORD_LIST;
        case 3: // REGULAR
          return REGULAR;
        case 4: // MESSAGE
          return MESSAGE;
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
  private static final int __SUCCESS_ISSET_ID = 0;
  private byte __isset_bitfield = 0;
  public static final Map<_Fields, org.apache.thrift.meta_data.FieldMetaData> metaDataMap;
  static {
    Map<_Fields, org.apache.thrift.meta_data.FieldMetaData> tmpMap = new EnumMap<_Fields, org.apache.thrift.meta_data.FieldMetaData>(_Fields.class);
    tmpMap.put(_Fields.SUCCESS, new org.apache.thrift.meta_data.FieldMetaData("success", org.apache.thrift.TFieldRequirementType.DEFAULT, 
        new org.apache.thrift.meta_data.FieldValueMetaData(org.apache.thrift.protocol.TType.BOOL)));
    tmpMap.put(_Fields.KEYWORD_LIST, new org.apache.thrift.meta_data.FieldMetaData("keywordList", org.apache.thrift.TFieldRequirementType.DEFAULT, 
        new org.apache.thrift.meta_data.ListMetaData(org.apache.thrift.protocol.TType.LIST, 
            new org.apache.thrift.meta_data.FieldValueMetaData(org.apache.thrift.protocol.TType.STRING))));
    tmpMap.put(_Fields.REGULAR, new org.apache.thrift.meta_data.FieldMetaData("regular", org.apache.thrift.TFieldRequirementType.DEFAULT, 
        new org.apache.thrift.meta_data.FieldValueMetaData(org.apache.thrift.protocol.TType.STRING)));
    tmpMap.put(_Fields.MESSAGE, new org.apache.thrift.meta_data.FieldMetaData("message", org.apache.thrift.TFieldRequirementType.DEFAULT, 
        new org.apache.thrift.meta_data.FieldValueMetaData(org.apache.thrift.protocol.TType.STRING)));
    metaDataMap = Collections.unmodifiableMap(tmpMap);
    org.apache.thrift.meta_data.FieldMetaData.addStructMetaDataMap(Result.class, metaDataMap);
  }

  public Result() {
  }

  public Result(
    boolean success,
    List<String> keywordList,
    String regular,
    String message)
  {
    this();
    this.success = success;
    setSuccessIsSet(true);
    this.keywordList = keywordList;
    this.regular = regular;
    this.message = message;
  }

  /**
   * Performs a deep copy on <i>other</i>.
   */
  public Result(Result other) {
    __isset_bitfield = other.__isset_bitfield;
    this.success = other.success;
    if (other.isSetKeywordList()) {
      List<String> __this__keywordList = new ArrayList<String>(other.keywordList);
      this.keywordList = __this__keywordList;
    }
    if (other.isSetRegular()) {
      this.regular = other.regular;
    }
    if (other.isSetMessage()) {
      this.message = other.message;
    }
  }

  public Result deepCopy() {
    return new Result(this);
  }

  @Override
  public void clear() {
    setSuccessIsSet(false);
    this.success = false;
    this.keywordList = null;
    this.regular = null;
    this.message = null;
  }

  public boolean isSuccess() {
    return this.success;
  }

  public Result setSuccess(boolean success) {
    this.success = success;
    setSuccessIsSet(true);
    return this;
  }

  public void unsetSuccess() {
    __isset_bitfield = EncodingUtils.clearBit(__isset_bitfield, __SUCCESS_ISSET_ID);
  }

  /** Returns true if field success is set (has been assigned a value) and false otherwise */
  public boolean isSetSuccess() {
    return EncodingUtils.testBit(__isset_bitfield, __SUCCESS_ISSET_ID);
  }

  public void setSuccessIsSet(boolean value) {
    __isset_bitfield = EncodingUtils.setBit(__isset_bitfield, __SUCCESS_ISSET_ID, value);
  }

  public int getKeywordListSize() {
    return (this.keywordList == null) ? 0 : this.keywordList.size();
  }

  public java.util.Iterator<String> getKeywordListIterator() {
    return (this.keywordList == null) ? null : this.keywordList.iterator();
  }

  public void addToKeywordList(String elem) {
    if (this.keywordList == null) {
      this.keywordList = new ArrayList<String>();
    }
    this.keywordList.add(elem);
  }

  public List<String> getKeywordList() {
    return this.keywordList;
  }

  public Result setKeywordList(List<String> keywordList) {
    this.keywordList = keywordList;
    return this;
  }

  public void unsetKeywordList() {
    this.keywordList = null;
  }

  /** Returns true if field keywordList is set (has been assigned a value) and false otherwise */
  public boolean isSetKeywordList() {
    return this.keywordList != null;
  }

  public void setKeywordListIsSet(boolean value) {
    if (!value) {
      this.keywordList = null;
    }
  }

  public String getRegular() {
    return this.regular;
  }

  public Result setRegular(String regular) {
    this.regular = regular;
    return this;
  }

  public void unsetRegular() {
    this.regular = null;
  }

  /** Returns true if field regular is set (has been assigned a value) and false otherwise */
  public boolean isSetRegular() {
    return this.regular != null;
  }

  public void setRegularIsSet(boolean value) {
    if (!value) {
      this.regular = null;
    }
  }

  public String getMessage() {
    return this.message;
  }

  public Result setMessage(String message) {
    this.message = message;
    return this;
  }

  public void unsetMessage() {
    this.message = null;
  }

  /** Returns true if field message is set (has been assigned a value) and false otherwise */
  public boolean isSetMessage() {
    return this.message != null;
  }

  public void setMessageIsSet(boolean value) {
    if (!value) {
      this.message = null;
    }
  }

  public void setFieldValue(_Fields field, Object value) {
    switch (field) {
    case SUCCESS:
      if (value == null) {
        unsetSuccess();
      } else {
        setSuccess((Boolean)value);
      }
      break;

    case KEYWORD_LIST:
      if (value == null) {
        unsetKeywordList();
      } else {
        setKeywordList((List<String>)value);
      }
      break;

    case REGULAR:
      if (value == null) {
        unsetRegular();
      } else {
        setRegular((String)value);
      }
      break;

    case MESSAGE:
      if (value == null) {
        unsetMessage();
      } else {
        setMessage((String)value);
      }
      break;

    }
  }

  public Object getFieldValue(_Fields field) {
    switch (field) {
    case SUCCESS:
      return Boolean.valueOf(isSuccess());

    case KEYWORD_LIST:
      return getKeywordList();

    case REGULAR:
      return getRegular();

    case MESSAGE:
      return getMessage();

    }
    throw new IllegalStateException();
  }

  /** Returns true if field corresponding to fieldID is set (has been assigned a value) and false otherwise */
  public boolean isSet(_Fields field) {
    if (field == null) {
      throw new IllegalArgumentException();
    }

    switch (field) {
    case SUCCESS:
      return isSetSuccess();
    case KEYWORD_LIST:
      return isSetKeywordList();
    case REGULAR:
      return isSetRegular();
    case MESSAGE:
      return isSetMessage();
    }
    throw new IllegalStateException();
  }

  @Override
  public boolean equals(Object that) {
    if (that == null)
      return false;
    if (that instanceof Result)
      return this.equals((Result)that);
    return false;
  }

  public boolean equals(Result that) {
    if (that == null)
      return false;

    boolean this_present_success = true;
    boolean that_present_success = true;
    if (this_present_success || that_present_success) {
      if (!(this_present_success && that_present_success))
        return false;
      if (this.success != that.success)
        return false;
    }

    boolean this_present_keywordList = true && this.isSetKeywordList();
    boolean that_present_keywordList = true && that.isSetKeywordList();
    if (this_present_keywordList || that_present_keywordList) {
      if (!(this_present_keywordList && that_present_keywordList))
        return false;
      if (!this.keywordList.equals(that.keywordList))
        return false;
    }

    boolean this_present_regular = true && this.isSetRegular();
    boolean that_present_regular = true && that.isSetRegular();
    if (this_present_regular || that_present_regular) {
      if (!(this_present_regular && that_present_regular))
        return false;
      if (!this.regular.equals(that.regular))
        return false;
    }

    boolean this_present_message = true && this.isSetMessage();
    boolean that_present_message = true && that.isSetMessage();
    if (this_present_message || that_present_message) {
      if (!(this_present_message && that_present_message))
        return false;
      if (!this.message.equals(that.message))
        return false;
    }

    return true;
  }

  @Override
  public int hashCode() {
    return 0;
  }

  @Override
  public int compareTo(Result other) {
    if (!getClass().equals(other.getClass())) {
      return getClass().getName().compareTo(other.getClass().getName());
    }

    int lastComparison = 0;

    lastComparison = Boolean.valueOf(isSetSuccess()).compareTo(other.isSetSuccess());
    if (lastComparison != 0) {
      return lastComparison;
    }
    if (isSetSuccess()) {
      lastComparison = org.apache.thrift.TBaseHelper.compareTo(this.success, other.success);
      if (lastComparison != 0) {
        return lastComparison;
      }
    }
    lastComparison = Boolean.valueOf(isSetKeywordList()).compareTo(other.isSetKeywordList());
    if (lastComparison != 0) {
      return lastComparison;
    }
    if (isSetKeywordList()) {
      lastComparison = org.apache.thrift.TBaseHelper.compareTo(this.keywordList, other.keywordList);
      if (lastComparison != 0) {
        return lastComparison;
      }
    }
    lastComparison = Boolean.valueOf(isSetRegular()).compareTo(other.isSetRegular());
    if (lastComparison != 0) {
      return lastComparison;
    }
    if (isSetRegular()) {
      lastComparison = org.apache.thrift.TBaseHelper.compareTo(this.regular, other.regular);
      if (lastComparison != 0) {
        return lastComparison;
      }
    }
    lastComparison = Boolean.valueOf(isSetMessage()).compareTo(other.isSetMessage());
    if (lastComparison != 0) {
      return lastComparison;
    }
    if (isSetMessage()) {
      lastComparison = org.apache.thrift.TBaseHelper.compareTo(this.message, other.message);
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
    StringBuilder sb = new StringBuilder("Result(");
    boolean first = true;

    sb.append("success:");
    sb.append(this.success);
    first = false;
    if (!first) sb.append(", ");
    sb.append("keywordList:");
    if (this.keywordList == null) {
      sb.append("null");
    } else {
      sb.append(this.keywordList);
    }
    first = false;
    if (!first) sb.append(", ");
    sb.append("regular:");
    if (this.regular == null) {
      sb.append("null");
    } else {
      sb.append(this.regular);
    }
    first = false;
    if (!first) sb.append(", ");
    sb.append("message:");
    if (this.message == null) {
      sb.append("null");
    } else {
      sb.append(this.message);
    }
    first = false;
    sb.append(")");
    return sb.toString();
  }

  public void validate() throws org.apache.thrift.TException {
    // check for required fields
    // check for sub-struct validity
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
      // it doesn't seem like you should have to do this, but java serialization is wacky, and doesn't call the default constructor.
      __isset_bitfield = 0;
      read(new org.apache.thrift.protocol.TCompactProtocol(new org.apache.thrift.transport.TIOStreamTransport(in)));
    } catch (org.apache.thrift.TException te) {
      throw new java.io.IOException(te);
    }
  }

  private static class ResultStandardSchemeFactory implements SchemeFactory {
    public ResultStandardScheme getScheme() {
      return new ResultStandardScheme();
    }
  }

  private static class ResultStandardScheme extends StandardScheme<Result> {

    public void read(org.apache.thrift.protocol.TProtocol iprot, Result struct) throws org.apache.thrift.TException {
      org.apache.thrift.protocol.TField schemeField;
      iprot.readStructBegin();
      while (true)
      {
        schemeField = iprot.readFieldBegin();
        if (schemeField.type == org.apache.thrift.protocol.TType.STOP) { 
          break;
        }
        switch (schemeField.id) {
          case 1: // SUCCESS
            if (schemeField.type == org.apache.thrift.protocol.TType.BOOL) {
              struct.success = iprot.readBool();
              struct.setSuccessIsSet(true);
            } else { 
              org.apache.thrift.protocol.TProtocolUtil.skip(iprot, schemeField.type);
            }
            break;
          case 2: // KEYWORD_LIST
            if (schemeField.type == org.apache.thrift.protocol.TType.LIST) {
              {
                org.apache.thrift.protocol.TList _list0 = iprot.readListBegin();
                struct.keywordList = new ArrayList<String>(_list0.size);
                for (int _i1 = 0; _i1 < _list0.size; ++_i1)
                {
                  String _elem2;
                  _elem2 = iprot.readString();
                  struct.keywordList.add(_elem2);
                }
                iprot.readListEnd();
              }
              struct.setKeywordListIsSet(true);
            } else { 
              org.apache.thrift.protocol.TProtocolUtil.skip(iprot, schemeField.type);
            }
            break;
          case 3: // REGULAR
            if (schemeField.type == org.apache.thrift.protocol.TType.STRING) {
              struct.regular = iprot.readString();
              struct.setRegularIsSet(true);
            } else { 
              org.apache.thrift.protocol.TProtocolUtil.skip(iprot, schemeField.type);
            }
            break;
          case 4: // MESSAGE
            if (schemeField.type == org.apache.thrift.protocol.TType.STRING) {
              struct.message = iprot.readString();
              struct.setMessageIsSet(true);
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

    public void write(org.apache.thrift.protocol.TProtocol oprot, Result struct) throws org.apache.thrift.TException {
      struct.validate();

      oprot.writeStructBegin(STRUCT_DESC);
      oprot.writeFieldBegin(SUCCESS_FIELD_DESC);
      oprot.writeBool(struct.success);
      oprot.writeFieldEnd();
      if (struct.keywordList != null) {
        oprot.writeFieldBegin(KEYWORD_LIST_FIELD_DESC);
        {
          oprot.writeListBegin(new org.apache.thrift.protocol.TList(org.apache.thrift.protocol.TType.STRING, struct.keywordList.size()));
          for (String _iter3 : struct.keywordList)
          {
            oprot.writeString(_iter3);
          }
          oprot.writeListEnd();
        }
        oprot.writeFieldEnd();
      }
      if (struct.regular != null) {
        oprot.writeFieldBegin(REGULAR_FIELD_DESC);
        oprot.writeString(struct.regular);
        oprot.writeFieldEnd();
      }
      if (struct.message != null) {
        oprot.writeFieldBegin(MESSAGE_FIELD_DESC);
        oprot.writeString(struct.message);
        oprot.writeFieldEnd();
      }
      oprot.writeFieldStop();
      oprot.writeStructEnd();
    }

  }

  private static class ResultTupleSchemeFactory implements SchemeFactory {
    public ResultTupleScheme getScheme() {
      return new ResultTupleScheme();
    }
  }

  private static class ResultTupleScheme extends TupleScheme<Result> {

    @Override
    public void write(org.apache.thrift.protocol.TProtocol prot, Result struct) throws org.apache.thrift.TException {
      TTupleProtocol oprot = (TTupleProtocol) prot;
      BitSet optionals = new BitSet();
      if (struct.isSetSuccess()) {
        optionals.set(0);
      }
      if (struct.isSetKeywordList()) {
        optionals.set(1);
      }
      if (struct.isSetRegular()) {
        optionals.set(2);
      }
      if (struct.isSetMessage()) {
        optionals.set(3);
      }
      oprot.writeBitSet(optionals, 4);
      if (struct.isSetSuccess()) {
        oprot.writeBool(struct.success);
      }
      if (struct.isSetKeywordList()) {
        {
          oprot.writeI32(struct.keywordList.size());
          for (String _iter4 : struct.keywordList)
          {
            oprot.writeString(_iter4);
          }
        }
      }
      if (struct.isSetRegular()) {
        oprot.writeString(struct.regular);
      }
      if (struct.isSetMessage()) {
        oprot.writeString(struct.message);
      }
    }

    @Override
    public void read(org.apache.thrift.protocol.TProtocol prot, Result struct) throws org.apache.thrift.TException {
      TTupleProtocol iprot = (TTupleProtocol) prot;
      BitSet incoming = iprot.readBitSet(4);
      if (incoming.get(0)) {
        struct.success = iprot.readBool();
        struct.setSuccessIsSet(true);
      }
      if (incoming.get(1)) {
        {
          org.apache.thrift.protocol.TList _list5 = new org.apache.thrift.protocol.TList(org.apache.thrift.protocol.TType.STRING, iprot.readI32());
          struct.keywordList = new ArrayList<String>(_list5.size);
          for (int _i6 = 0; _i6 < _list5.size; ++_i6)
          {
            String _elem7;
            _elem7 = iprot.readString();
            struct.keywordList.add(_elem7);
          }
        }
        struct.setKeywordListIsSet(true);
      }
      if (incoming.get(2)) {
        struct.regular = iprot.readString();
        struct.setRegularIsSet(true);
      }
      if (incoming.get(3)) {
        struct.message = iprot.readString();
        struct.setMessageIsSet(true);
      }
    }
  }

}

