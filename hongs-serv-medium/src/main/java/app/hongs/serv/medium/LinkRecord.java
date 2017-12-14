package app.hongs.serv.medium;

import app.hongs.HongsException;
import app.hongs.db.Model;
import app.hongs.db.Table;
import app.hongs.db.util.FetchCase;
import app.hongs.util.Synt;
import java.util.Map;

/**
 * 链表模型
 * 必须含有 link_id,link 字段, 类似 comment,dissent,endorse,visitor
 * @author Hongs
 */
public class LinkRecord extends Model {

    private String link   = null;
    private String linkId = null;

    public LinkRecord(Table table) throws HongsException {
        super(table);
    }

    public void setLink(String link) {
        this.link = link;
    }

    public String getLink() {
        return this.link;
    }

    public void setLinkId(String linkId) {
        this.linkId=linkId;
    }

    public String getLinkId() {
        return this.linkId;
    }

    @Override
    public int add(String id, Map rd) throws HongsException {
        if (null != linkId) {
            rd.put("link_id", linkId);
        }
        if (null != link) {
            rd.put("link", link);
        }
        return super.add(id, rd);
    }

    @Override
    public int put(String id, Map rd) throws HongsException {
        if (null != linkId) {
            rd.put("link_id", linkId);
        }
        if (null != link) {
            rd.put("link", link);
        }
        return super.put(id, rd);
    }

    @Override
    protected void filter(FetchCase fc, Map rd) throws HongsException {
        super.filter(fc, rd);
        String tn = Synt.defoult(fc.getName(), table.name);
        if (linkId != null) {
            fc.filter("`"+tn+"`.`link_id` = ?", linkId);
        } else
        if ( ! rd.containsKey ( "link_id" )) {
            fc.filter("`"+tn+"`.`link_id` IS NULL");
        }
        if (link != null) {
            fc.filter("`"+tn+"`.`link` = ?", link );
        } else
        if ( ! rd.containsKey ( "link" )) {
            fc.filter("`"+tn+"`.`link` IS NULL");
        }
    }

}
