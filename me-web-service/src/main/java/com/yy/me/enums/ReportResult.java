package com.yy.me.enums;

public enum ReportResult {
        UNKONWN(0), NOT_ACTIVE(1), ALREADY_REPORTED(2), SHORT_TIME_REPORT(3), WARN(4),
        FIRST_FORBID(5), SECOND_FORBID(6),MAX_REPORT_COUNT(7), EVIL_USER(8),MAX_DAY_REPORT_COUNT(9),
        MAX_FIVE_DAY_REPORT_COUNT(10),GONGPING_REPEAT_REPORT(11),IM_REPEAT_REPORT(12), ALREADY_REPORTED_FROM_HOME_PAGE_OR_IM_RIGHT_MENU(13);
        private int code;

        ReportResult(int code) {
            this.code = code;
        }

        public int getCode() {
            return code;
        }
    }