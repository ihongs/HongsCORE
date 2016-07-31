package app.hongs.util.verify;

import app.hongs.Cnst;
import app.hongs.HongsException;
import app.hongs.action.SocketHelper;
import app.hongs.db.DB;
import java.util.Map;
import javax.servlet.http.HttpSession;
import javax.websocket.Session;

/**
 * 消息区域ID
 * @author Hongs
 */
public class MesageTopic extends Rule {

    @Override
    public Object verify(Object value) throws Wrong, Wrongs, HongsException {
        /**/Session   sess = (/**/Session)    values.get    (/**/Session.class.getName());
        SocketHelper  hepr = SocketHelper.getInstance(sess );
        HttpSession    ses = (HttpSession) hepr.getAttribute(HttpSession.class.getName());
        Object         uid = hepr.getAttribute(Cnst.UID_SES);
        String         tid = hepr.getParameter("tid");
        Object         gid = ses.getId(  );

        values.put("uid", uid);
        values.put("tid", tid);
        values.put("gid", gid);

        if (uid == null || "".equals(uid)) {
            throw new Wrong("core.uid.cannot.be.empty").setLocalizedSection("mesage");
        }
        if (tid == null || "".equals(tid)) {
            throw new Wrong("core.tid.cannot.be.empty").setLocalizedSection("mesage");
        }

        DB  db = DB.getInstance("mesage");
        Map ro;

        ro = db.fetchCase()
            .from  (db.getTable("room").tableName)
            .where ("id = ? AND state > 0" , tid )
            .select("id")
            .one   ();
        if (ro == null || ro.isEmpty()) {
            throw new Wrong("core.room.not.exists" ).setLocalizedSection("mesage");
        }

        ro = db.fetchCase()
            .from  (db.getTable("room_mate").tableName)
            .where ("rid = ? AND uid = ? AND state > 0", tid, uid)
            .select("rid")
            .one   ();
        if (ro == null || ro.isEmpty()) {
            throw new Wrong("core.user.not.in.room").setLocalizedSection("mesage");
        }

        return tid;
    }

}
