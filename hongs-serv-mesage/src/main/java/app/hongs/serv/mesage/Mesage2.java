package app.hongs.serv.mesage;

import java.io.Serializable;
import java.util.Set;

/**
 * 再分发消息
 *
 * @author Hongs
 */
public class Mesage2 implements Serializable {

    public final Mesage      message;
    public final Set<String> userIds;

    /**
     *
     * @param message 消息结构体
     * @param userIds 在线用户ID(Note)或接收用户ID(Push)
     */
    public Mesage2(Mesage message, Set<String> userIds) {
        this.message = message;
        this.userIds = userIds;
    }

}
