package app.hongs.db.util;

import app.hongs.util.Synt;

/**
 * 可单独提取条件的查询结构
 * Model 中用于构建复合条件
 * 其他仅需条件的场合亦可用
 * 注意: getFilter,getHaving 等方法的返回均不包含关联数据
 * @author Hongs
 */
public class FetchCast extends FetchCase {

    public FetchCast(FetchCase caze) {
        super();

        if (!Synt.asserts("IN_PASS", false)) {
        if (!Synt.asserts("IN_PACK", false)) {
            this.wheres  = caze.wheres ;
            this.havins  = caze.havins ;
        }
            this.wparams = caze.wparams;
            this.vparams = caze.vparams;
        }
            this.options = caze.options;
    }

    public FetchCast() {
        super();
    }

    public String getFilter() {
        return this.wheres.length() > 0 ? this.wheres.substring(5) : "";
    }

    public String getFilterSql() {
        return delSQLTbls(pw.matcher(wheres).replaceFirst(""));
    }

    public Object[] getFilterArr() {
        return wparams.toArray();
    }

    public String getHaving() {
        return this.havins.length() > 0 ? this.wheres.substring(5) : "";
    }

    public String getHavingSql() {
        return delSQLTbls(pw.matcher(havins).replaceFirst(""));
    }

    public Object[] getHavingArr() {
        return vparams.toArray();
    }

}
