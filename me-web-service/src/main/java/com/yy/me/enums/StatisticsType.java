package com.yy.me.enums;

public enum StatisticsType {
    TRUNCATE("truncate"), LS_START("ls_start"), LS_STOP("ls_stop"), LS_GUEST_ENTER("ls_guest_enter"), LS_GUEST_LEAVE("ls_guest_leave"), LS_HANDS_UP("ls_hands_up"), LS_LINK("ls_link"), LS_LINK_CANCEL("ls_link_cancel"), LS_HANDS_DOWN("ls_hands_down"), USER_REGIST("user_regist"), USER_CHANGE_HEADER("user_change_header"), MACHINE_LS_LIKE("machine_ls_like"), MACHINE_LS_CHAT("machine_ls_chat"), MACHINE_SHARE3P("machine_share3p"), SHARE3P("share3p"), LS_LIKE("ls_like"), LS_CHAT("ls_chat");
    private String value;

    private StatisticsType(String value) {
        this.value = value;
    }

    public String getValue() {
        return this.value;
    }
}
