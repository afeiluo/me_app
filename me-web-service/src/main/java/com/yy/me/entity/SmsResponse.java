package com.yy.me.entity;

import org.apache.commons.lang3.builder.ToStringBuilder;

public class SmsResponse {
    
    private String message;
    private int code;
    private Object object;

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public Object getObject() {
        return object;
    }

    public void setObject(Object object) {
        this.object = object;
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }

}
