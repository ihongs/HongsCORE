package io.github.ihongs.serv.matrix.sync;

import java.io.Serializable;

/**
 * 事件条目, 以供通知
 * @author Hongs
 */
public class SyncRecord implements Serializable {

    public final boolean mod;
    public final String conf;
    public final String form;
    public final String id;
    public final String servId;

    /**
     * @param mod    true 更新, false 删除
     * @param conf   配置
     * @param form   表达
     * @param id     数据ID
     * @param servId 服务ID
     */
    public SyncRecord(boolean mod, String conf, String form, String id, String servId) {
        this.mod    =  mod;
        this.conf   = conf;
        this.form   = form;
        this.id     = id;
        this.servId = servId;
    }
    
    @Override
    public String toString() {
        return (mod ? "UPDATE" : "DELETE") + "|" + conf + ":" + form + "|" + id + "|" + servId;
    }

}
