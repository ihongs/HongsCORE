package io.github.ihongs.db;

import io.github.ihongs.HongsException;
import io.github.ihongs.HongsExemption;
import io.github.ihongs.db.link.Loop;
import io.github.ihongs.db.util.AssocMore;
import io.github.ihongs.db.util.FetchCase;
import io.github.ihongs.util.Crypto;
import java.util.Map;
import java.util.function.Consumer;

/**
 * 加密表
 * @author Hongs
 */
public abstract class PrivTable extends Table implements Cloneable {

    public PrivTable(DB db, Map conf) throws HongsException {
        super(db, conf);
    }

    @Override
    public PrivTable clone() {
        try {
            return (PrivTable) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new HongsExemption( e );
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
    throws HongsException {
        // 加密
        encrypt().accept(values);

        return super.insert(values);
    }

    @Override
    public int update(Map<String, Object> values, String where, Object... params)
    throws HongsException {
        // 加密
        encrypt().accept(values);

        return super.update(values, where, params);
    }

    @Override
    public FetchCase fetchCase()
    throws HongsException {
        FetchCase  fc = new PCase(this)
              .use(db).from(tableName, name);
        AssocMore.checkCase(fc, getParams());
        return     fc ;
    }

    private static class PCase extends FetchCase {

        private final PrivTable table;

        public PCase (PrivTable table) {
            super ( (byte) 0 );
            this.table = table;
        }

        @Override
        public int insert(Map<String, Object> values) throws HongsException {
            // 加密
            table.encrypt().accept(values);

            return super.insert(values);
        }

        @Override
        public int update(Map<String, Object> values) throws HongsException {
            // 加密
            table.encrypt().accept(values);

            return super.update(values);
        }

        @Override
        public Loop select() throws HongsException {
            return new PLoop(table, super.select());
        }

    }

    private static class PLoop extends Loop {

        private final Consumer<Map> dc;

        public PLoop (PrivTable table, Loop loop)
        throws HongsException {
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
