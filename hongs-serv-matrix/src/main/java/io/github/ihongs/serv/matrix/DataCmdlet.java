package io.github.ihongs.serv.matrix;

import io.github.ihongs.Cnst;
import io.github.ihongs.Core;
import io.github.ihongs.HongsException;
import io.github.ihongs.action.ActionHelper;
import io.github.ihongs.action.VerifyHelper;
import io.github.ihongs.cmdlet.CmdletHelper;
import io.github.ihongs.cmdlet.anno.Cmdlet;
import io.github.ihongs.util.Synt;
import java.util.HashMap;
import java.util.Map;

/**
 * 数据导入命令
 * @author hong
 */
@Cmdlet("matrix.data")
public class DataCmdlet {

    @Cmdlet("import")
    public static void impart(String[] args) throws HongsException {
        Map opts = CmdletHelper.getOpts(args, new String[] {
            "conf=s",
            "form=s",
            "user:s",
            "memo:s",
            "?Usage: import --conf CONF_NAME --form FORM_NAME DATA DATA ..."
        });

        String conf = (String) opts.get("conf");
        String form = (String) opts.get("form");
        String user = (String) opts.get("user");
        String memo = (String) opts.get("memo");
        long dt = Core.ACTION_TIME .get(      );
        Data dr = Data.getInstance( conf,form );
        VerifyHelper vh = new VerifyHelper (  );
             vh . addRulesByForm  ( conf,form );
        if (user == null) {
            user  = Cnst.ADM_UID;
        }

        String[] dats = (String[]) opts.get("");
        for(String text : dats) {
            String id ;
            Map  data = data(text);
            data = vh.verify(data);
            id = (String) data.get(Cnst.ID_KEY);
            data.put("form_id", form);
            data.put("user_id", user);
            data.put("memo", memo);
            dr.save (dt, id, data);
        }
    }

    @Cmdlet("update")
    public static void update(String[] args) throws HongsException {
        Map opts = CmdletHelper.getOpts(args, new String[] {
            "conf=s",
            "form=s",
            "user:s",
            "memo:s",
            "?Usage: update --conf CONF_NAME --form FORM_NAME FIND DATA"
        });

        String conf = (String) opts.get("conf");
        String form = (String) opts.get("form");
        String user = (String) opts.get("user");
        String memo = (String) opts.get("memo");
        long dt = Core.ACTION_TIME .get(      );
        Data dr = Data.getInstance( conf,form );
        VerifyHelper vh = new VerifyHelper (  );
             vh . addRulesByForm  ( conf,form );
        if (user == null) {
            user  = Cnst.ADM_UID;
        }

        String[] dats = (String[]) opts.get("");
        if (dats.length < 2) {
            CmdletHelper.println("Need FIND DATA");
            return;
        }

        Map rd = data(dats[0]);
        Map sd = data(dats[1]);
            sd = vh.verify(sd);
        sd.put("form_id",form);
        sd.put("user_id",user);
        sd.put("memo"   ,memo);
        rd.put(Cnst.RB_KEY , Synt.setOf(Cnst.ID_KEY));

        for(Map od : dr.search(rd, 0, 0)) {
            String id = (String) od.get(Cnst.ID_KEY) ;
            dr.save (dt,id,sd);
        }
    }

    @Cmdlet("delete")
    public static void delete(String[] args) throws HongsException {
        Map opts = CmdletHelper.getOpts(args, new String[] {
            "conf=s",
            "form=s",
            "user:s",
            "memo:s",
            "?Usage: delete --conf CONF_NAME --form FORM_NAME FIND_TERM"
        });

        String conf = (String) opts.get("conf");
        String form = (String) opts.get("form");
        String user = (String) opts.get("user");
        String memo = (String) opts.get("memo");
        long dt = Core.ACTION_TIME .get(      );
        Data dr = Data.getInstance( conf,form );
        VerifyHelper vh = new VerifyHelper (  );
             vh . addRulesByForm  ( conf,form );
        if (user == null) {
            user  = Cnst.ADM_UID;
        }

        String[] dats = (String[]) opts.get("");
        if (dats.length < 1) {
            CmdletHelper.println("Need FIND_TERM.");
            return;
        }

        Map rd = data(dats[0]);
        Map sd = new HashMap();
        //  sd = vh.verify(sd);
        sd.put("form_id",form);
        sd.put("user_id",user);
        sd.put("memo"   ,memo);
        rd.put(Cnst.RB_KEY , Synt.setOf(Cnst.ID_KEY));

        for(Map od : dr.search(rd, 0, 0)) {
            String id = (String) od.get(Cnst.ID_KEY) ;
            dr.drop (dt,id,sd);
        }
    }

    @Cmdlet("search")
    public static void search(String[] args) throws HongsException {
        Map opts = CmdletHelper.getOpts(args, new String[] {
            "conf=s",
            "form=s",
            "?Usage: search --conf CONF_NAME --form FORM_NAME FIND_TERM"
        });

        String conf = (String) opts.get("conf");
        String form = (String) opts.get("form");
        Data dr = Data.getInstance( conf,form );
        VerifyHelper vh = new VerifyHelper (  );
             vh . addRulesByForm  ( conf,form );

        String[] dats = (String[]) opts.get("");
        if (dats.length < 1) {
            CmdletHelper.println("Need FIND_TERM.");
            return;
        }

        Map rd = data(dats[0]);

        for(Map od : dr.search(rd, 0, 0)) {
            CmdletHelper.preview(od);
        }
    }

    private static Map data(String text) {
        text = text.trim();
        if (text.startsWith("<") && text.endsWith(">")) {
            throw  new UnsupportedOperationException("Unsupported html: "+ text);
        } else
        if (text.startsWith("[") && text.endsWith("]")) {
            throw  new UnsupportedOperationException("Unsupported list: "+ text);
        } else
        if (text.startsWith("{") && text.endsWith("}")) {
            return (Map) io.github.ihongs.util.Data.toObject(text);
        } else {
            return ActionHelper.parseQuery(text);
        }
    }

}
