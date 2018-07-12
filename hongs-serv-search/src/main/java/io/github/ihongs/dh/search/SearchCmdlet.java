package io.github.ihongs.dh.search;

import io.github.ihongs.Core;
import io.github.ihongs.HongsException;
import io.github.ihongs.action.ActionHelper;
import io.github.ihongs.action.VerifyHelper;
import io.github.ihongs.cmdlet.CmdletHelper;
import io.github.ihongs.cmdlet.anno.Cmdlet;
import io.github.ihongs.dh.lucene.LuceneRecord;
import io.github.ihongs.util.Synt;

import java.util.List;
import java.util.Map;

/**
 * 索引命令
 * @author Hongs
 */
@Cmdlet()
public class SearchCmdlet {

    @Cmdlet("search")
    public void search(String[] args) throws HongsException {
        Map opts = CmdletHelper.getOpts(args, new String[ ] {
            "conf=s",
            "name=s",
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
        ActionHelper ah = Core.getInstance(ActionHelper.class);
        LuceneRecord so = LuceneRecord.getInstance(conf, name);
        Map req = ah.getRequestData();
        req.putAll(opts);
        Map rsp = so.search (req);
        CmdletHelper.preview(rsp);
    }

    @Cmdlet("update")
    public void update(String[] args) throws HongsException {
        Map opts = CmdletHelper.getOpts(args, new String[ ] {
            "conf=s",
            "name=s",
            "id*s",
        });

        String conf = Synt.asString(opts.remove("conf"));
        String name = Synt.asString(opts.remove("name"));
        List<String> ds = Synt.asList(opts.remove("id"));
        ActionHelper ah = Core.getInstance(ActionHelper.class);
        LuceneRecord so = LuceneRecord.getInstance(conf, name);
        Map  rd = ah.getRequestData();

        if (!rd.isEmpty()) {
            // 有数据则校验数据
            VerifyHelper vh = new VerifyHelper();
            vh.addRulesByForm(conf, name);
            rd = vh.verify(rd);

            try {
                so.begin ( );
                for (String id  : ds) {
                    so.set( id  , rd);
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
        else {
            // 不给内容即为删除
            try {
                so.begin( );
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
    }

}
