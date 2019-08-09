package io.github.ihongs.dh.lucene;

import io.github.ihongs.Cnst;
import io.github.ihongs.HongsException;
import io.github.ihongs.action.ActionHelper;
import io.github.ihongs.cmdlet.CmdletHelper;
import io.github.ihongs.cmdlet.anno.Cmdlet;
import io.github.ihongs.util.Dawn;
import io.github.ihongs.util.Synt;

import java.util.List;
import java.util.Map;

/**
 * Lucene 索引命令
 * @author Hongs
 */
@Cmdlet()
public class LuceneCmdlet {

    @Cmdlet("search")
    public static void search(String[] args) throws HongsException {
        Map opts = CmdletHelper.getOpts(args, new String[ ] {
            "conf=s",
            "form=s",
            "id*s",
            "wd*s",
            "rb*s",
            "ob*s",
            "pn:i",
            "gn:i",
            "rn:i"
        });

        String conf = Synt.asString(opts.remove("conf"));
        String name = Synt.asString(opts.remove("name"));
        LuceneRecord so = LuceneRecord.getInstance(conf, name);
        Map rsp = so.search(opts);
        CmdletHelper.preview(rsp);
    }

    @Cmdlet("delete")
    public static void delete(String[] args) throws HongsException {
        Map opts = CmdletHelper.getOpts(args, new String[ ] {
            "conf=s",
            "form=s",
            "id*s"
        });

        String conf = Synt.asString(opts.remove("conf"));
        String name = Synt.asString(opts.remove("name"));
        List<String> ds = Synt.asList(opts.remove("id"));
        LuceneRecord so = LuceneRecord.getInstance(conf, name);

        try {
            so.begin ( );
            for (String id  : ds) {
                so.del( id );
            }
            so.commit( );
        }
        catch (HongsException ex) {
            so.revert( );
            throw ex;
        }
        finally {
            so.close ( );
        }
    }

    @Cmdlet("update")
    public static void update(String[] args) throws HongsException {
        Map opts = CmdletHelper.getOpts(args, new String[ ] {
            "conf=s",
            "form=s",
            "id*s",
            "!A"
        }); args = (String[]) opts.get("");

        String conf = Synt.asString(opts.remove("conf"));
        String name = Synt.asString(opts.remove("name"));
        List<String> ds = Synt.asList(opts.remove("id"));
        LuceneRecord so = LuceneRecord.getInstance(conf, name);

        Map rd = data(args[0]);

        try {
            so.begin ( );
            for (String id  : ds) {
                so.put( id  , rd);
            }
            so.commit( );
        }
        catch (HongsException ex) {
            so.revert( );
            throw ex;
        }
        finally {
            so.close ( );
        }
    }

    @Cmdlet("create")
    public static void create(String[] args) throws HongsException {
        Map opts = CmdletHelper.getOpts(args, new String[ ] {
            "conf=s",
            "form=s",
            "!A"
        }); args = (String[]) opts.get("");

        String conf = Synt.asString(opts.remove("conf"));
        String name = Synt.asString(opts.remove("name"));
        LuceneRecord so = LuceneRecord.getInstance(conf, name);

        String id;
        Map    rd;

        try {
            so.begin ( );
            for (String rt : args) {
                rd = data(rt);
                id = (String)  rd.get(Cnst.ID_KEY);
                if (id != null && id.length() > 0) {
                    so.set(id, rd);
                } else {
                    so.add(    rd);
                }
            }
            so.commit( );
        }
        catch (HongsException ex) {
            so.revert( );
            throw ex;
        }
        finally {
            so.close ( );
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
            return ( Map ) Dawn.toObject  (text);
        } else {
            return ActionHelper.parseQuery(text);
        }
    }

}
