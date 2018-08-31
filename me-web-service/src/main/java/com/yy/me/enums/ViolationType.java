package com.yy.me.enums;

/**
 * 处罚的信箱消息类型
 *
 * @author Jiang Chengyan
 *
 */
public enum ViolationType {
    U_BANED_HEAD_ONE_TIME("u_baned_head_one_time"),
    U_BANED_HEAD_1("u_baned_head_1"),
    U_BANED_HEAD_2("u_baned_head_2"),
    U_BANED_HEAD_3("u_baned_head_3"),
    U_BANED_LS_STOP("u_baned_ls_stop"),
    U_BANED_LS_STOP_2("u_baned_ls_stop_2"),
    U_BANED_LS_ACCOUNT("u_baned_ls_account"),
    U_BANED_LS_ACCOUNT_2("u_baned_ls_account_2"),
    U_BANED_LS_LINK_ACCOUNT("u_baned_ls_link_account"),
    U_BANED_LS_DEVICE("u_baned_ls_device"),
    U_BANED_LS_DEVICE_2("u_baned_ls_device_2"),
    U_BANED_NICK("u_baned_nick"),
    U_BANED_SIGNATURE("u_baned_signature");

    ViolationType(String type) {
        this.value = type;
    }

    private String value;

    public String getValue() {
        return value;
    }
}
