package com.yy.me.enums;

/**
 * 处罚来源
 * <p>
 * Created by Chris on 16/6/15.
 */
public enum PunishSource {
    AUDIT(1), // 资源管理部审查
    OPEN(2), // 外部调用处罚,目前只有资源管理部主动调用处罚
    OPERATION(3), // 运营处罚
    ;

    private int source;

    PunishSource(int source) {
        this.source = source;
    }

    public int getSource() {
        return source;
    }
}
