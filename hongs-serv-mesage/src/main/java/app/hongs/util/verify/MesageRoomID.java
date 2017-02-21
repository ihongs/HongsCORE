package app.hongs.util.verify;

import app.hongs.Cnst;
import app.hongs.HongsException;
import app.hongs.db.DB;
import java.util.Map;
import javax.websocket.Session;
import javax.servlet.http.HttpSession;

/**
 * 消息区域ID
 * @author Hongs
 */
public class MesageRoomID extends Rule {

    @Override
    public Object verify(Object value) throws Wrong, Wrongs, HongsException {
        /**/Session  sess = (/**/Session) values.get(/**/Session.class.getName());
        /**/Map    propes = sess.getUserProperties();
        HttpSession  hses = (HttpSession) propes.get(HttpSession.class.getName());
        String rid = sess.getPathParameters().get("rid");
        Object uid = hses.getAttribute(Cnst.UID_SES);
        Object gid = hses.getId();

        values.put("rid", rid);
        values.put("uid", uid);
        values.put("gid", gid);

        if (rid == null || "".equals(rid)) {
            throw new Wrong("core.rid.cannot.be.empty").setLocalizedSection("mesage");
        }
        if (uid == null || "".equals(uid)) {
            throw new Wrong("core.uid.cannot.be.empty").setLocalizedSection("mesage");
        }

        DB  db = DB.getInstance("mesage");
        Map ro;

        ro = db.fetchCase()
            .from  (db.getTable("room").tableName)
            .filter("id = ? AND state > 0" , rid )
            .select("id")
            .one   ();
        if (ro == null || ro.isEmpty()) {
            throw new Wrong("core.room.not.exists" ).setLocalizedSection("mesage");
        }

        ro = db.fetchCase()
            .from  (db.getTable("room_mate").tableName)
            .filter("room_id = ? AND user_id = ? AND state > 0", rid, uid)
            .select("room_id")
            .one   ();
        if (ro == null || ro.isEmpty()) {
            throw new Wrong("core.user.not.in.room").setLocalizedSection("mesage");
        }

        return rid;
    }

}
