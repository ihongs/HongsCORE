package app.hongs.serv.mesage;

import java.io.Serializable;

/**
 * 消息结构体
 *
 * @author Hongs
 */
public class Mesage implements Serializable {

    public final String id;
    public final String userId;
    public final String roomId;
    public final String kind;
    public final String data;
    public final long   stime;

    /**
     *
     * @param id 消息ID
     * @param userId 发送者ID
     * @param roomId 接收区ID
     * @param kind 消息类型
     * @param data 消息数据
     * @param stime 发送时间
     */
    public Mesage(String id, String userId, String roomId, String kind, String data, long stime) {
        this.id     = id;
        this.userId = userId;
        this.roomId = roomId;
        this.kind   = kind;
        this.data   = data;
        this.stime  = stime;
    }

}
