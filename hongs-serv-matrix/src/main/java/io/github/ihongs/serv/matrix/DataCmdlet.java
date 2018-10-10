package io.github.ihongs.serv.matrix;

import io.github.ihongs.Cnst;
import io.github.ihongs.Core;
import io.github.ihongs.HongsException;
import io.github.ihongs.action.ActionHelper;
import io.github.ihongs.action.VerifyHelper;
import io.github.ihongs.cmdlet.CmdletHelper;
import io.github.ihongs.cmdlet.anno.Cmdlet;
import java.util.Map;

/**
 * 数据导入命令
 * @author hong
 */
@Cmdlet("matrix/data")
public class DataCmdlet {

    @Cmdlet("save")
    public static void save(String[] args) throws HongsException {
        Map opts = CmdletHelper.getOpts(args, new String[] {
            "conf=s",
            "form=s",
            "user:s"
        });

        String conf = (String) opts.get("conf");
        String form = (String) opts.get("form");
        String user = (String) opts.get("user");
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
            dr.save (dt, id, data);
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
