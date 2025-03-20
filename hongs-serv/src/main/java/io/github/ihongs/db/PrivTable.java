package io.github.ihongs.db;

import io.github.ihongs.CruxException;
import io.github.ihongs.CruxExemption;
import io.github.ihongs.db.link.Loop;
import io.github.ihongs.db.util.FetchCase;
import io.github.ihongs.util.Crypto;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * 加密表
 * @author Hongs
 */
public abstract class PrivTable extends Table implements Cloneable {

    public PrivTable(DB db, Map conf) throws CruxException {
        super(db, conf);
    }

    @Override
    public PrivTable clone() {
        try {
            return (PrivTable) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new CruxExemption( e );
        }
    }

    /**
     * 设置加解密组件
     * @param crypto
     */
    public abstract void setCrypto(Crypto crypto);

    /**
     * 获取加解密组件
     * @return
     */
    public abstract Crypto getCrypto();

    /**
     * 加密方法
     * @return
     */
    public abstract Consumer<Map> encrypt();

    /**
     * 解密方法
     * @return
     */
    public abstract Consumer<Map> decrypt();

    /**
     * 测试解密
     *
     * 默认同 decrypt,
     * 抛出异常即无法解密,
     * 特殊格式可重写校验.
     *
     * @deprecated 检测命令专用
     * @return
     */
    public Consumer<Map> becrypt() {
        return decrypt();
    }

    @Override
    public int insert(Map<String, Object> values)
    throws CruxException {
        // 加密
        encrypt().accept(values);

        return super.insert(values);
    }

    @Override
    public int update(Map<String, Object> values, String where, Object... params)
    throws CruxException {
        // 加密
        encrypt().accept(values);

        return super.update(values, where, params);
    }

    @Override
    public List fetchMore(FetchCase caze)
    throws CruxException {
        FetchCase. Doer doer = caze.getDoer (  );
        try {
            if (! (doer instanceof PCase) ) {
                caze.use( new PCase(caze, this));
            }
            return super.fetchMore (caze);
        }
        finally {
            caze.use(doer);
        }
    }

    @Override
    public FetchCase fetchCase() {
        FetchCase caze = super.fetchCase( );
        caze.use( new PCase( caze, this ) );
        return caze;
    }

    private static class PCase extends FetchCase.Doer {

        final  PrivTable table;

        public PCase(FetchCase caze, PrivTable table) {
            super(caze);
            this.table = table;
        }

        @Override
        public int insert(Map values) throws CruxException {
            // 加密
            table.encrypt().accept(values);

            return super.insert(values);
        }

        @Override
        public int update(Map values) throws CruxException {
            // 加密
            table.encrypt().accept(values);

            return super.update(values);
        }

        @Override
        public Loop select() throws CruxException {
            return new PLoop(table, super.select());
        }

    }

    private static class PLoop extends Loop {

        private final Consumer<Map> dc;

        public PLoop (PrivTable table, Loop loop)
        throws CruxException {
            super(loop.getReusltSet(), loop.getStatement());
            this.dc = table.decrypt();
        }

        @Override
        public Map next() {
            Map row = super.next();

            if (row != null) {
                // 解密
                dc.accept(row);
            }

            return row;
        }

    }

}
