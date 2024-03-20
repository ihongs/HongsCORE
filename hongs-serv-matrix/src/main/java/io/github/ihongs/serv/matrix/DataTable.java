package io.github.ihongs.serv.matrix;

import io.github.ihongs.Core;
import io.github.ihongs.CoreConfig;
import io.github.ihongs.CruxException;
import io.github.ihongs.CruxExemption;
import io.github.ihongs.db.DB;
import io.github.ihongs.db.PrivTable;
import io.github.ihongs.util.Crypto;
import io.github.ihongs.util.Synt;
import java.util.Map;
import java.util.function.Consumer;

/**
 * 加密数据表
 *
 * 针对数据字段值进行加密,
 * 无需加密则不必设置此类.
 *
 * @author Hongs
 */
public class DataTable extends PrivTable {

    private static final String DATA_FIELD = "data";

    private Crypto   crypto   = null;
    private Consumer<Map> enc = null;
    private Consumer<Map> dec = null;

    public DataTable (DB db, Map conf) throws CruxException {
        super(db, conf);
    }

    @Override
    public void setCrypto(Crypto crypto) {
        this.crypto = crypto;
    }

    @Override
    public Crypto getCrypto() {
        return crypto != null ? crypto : Core.getInstance().got(
            Crypto.class.getName() + ":matrix.data" , () -> {
                CoreConfig cc = CoreConfig.getInstance();
                return new Crypto(
                    cc.getProperty("core.matrix.data.crypto.type"),
                    cc.getProperty("core.matrix.data.crypto.sk"),
                    cc.getProperty("core.matrix.data.crypto.iv")
                );
            });
    }

    /**
     * 加密, 仅用于 data 字段
     * @param dv
     * @return
     */
    public String encrypt(String dv) {
        // 加密
        Crypto.Crypt enx = getCrypto().encrypt();
        if (enx.valid()
        &&  dv != null
        && !dv.isEmpty()
        && (dv.startsWith("{")
        ||  dv.startsWith("["))) {
            dv  = enx.apply(dv);
        }

        return dv;
    }

    /**
     * 解密, 仅用于 data 字段
     * @param dv
     * @return
     */
    public String decrypt(String dv) {
        // 解密
        Crypto.Crypt dex = getCrypto().decrypt();
        if (dex.valid()
        &&  dv != null
        && !dv.isEmpty()
        && !dv.startsWith("{")
        && !dv.startsWith("[")) {
            dv  = dex.apply(dv);
        }

        return dv;
    }

    @Override
    public Consumer<Map> encrypt() {
        if (enc == null) {
            final Crypto.Crypt enx = getCrypto().encrypt();
            if (enx.valid()) {
                enc = new Consumer<Map>() {
                    @Override
                    public void accept(Map values) {
                        String fv = Synt.asString(values.get(DATA_FIELD));
                        if (fv != null
                        && !fv.isEmpty()
                        && (fv.startsWith("{")
                        ||  fv.startsWith("}"))) {
                            fv  = enx.apply(fv);
                            values.put(DATA_FIELD, fv);
                        }
                    }
                };
            } else {
                enc = new Consumer<Map>() {
                    @Override
                    public void accept(Map values) {
                        // Pass
                    }
                };
            }
        }
        return enc;
    }

    @Override
    public Consumer<Map> decrypt() {
        if (dec == null) {
            final Crypto.Crypt dex = getCrypto().decrypt();
            if (dex.valid()) {
                dec = new Consumer<Map>() {
                    @Override
                    public void accept(Map values) {
                        String fv = Synt.asString(values.get(DATA_FIELD));
                        if (fv != null
                        && !fv.isEmpty()
                        && !fv.startsWith("{")
                        && !fv.startsWith("[")) {
                            fv  = dex.apply(fv);
                            values.put(DATA_FIELD, fv);
                        }
                    }
                };
            } else {
                dec = new Consumer<Map>() {
                    @Override
                    public void accept(Map values) {
                        // Pass
                    }
                };
            }
        }
        return dec;
    }

    /**
     * 测试解密
     * @return
     */
    @Override
    public Consumer<Map> becrypt() {
        Consumer<Map> dex = decrypt();
        return new Consumer<Map>() {
            @Override
            public void accept(Map values) {
                dex.accept(values);
                String fv = Synt.asString(values.get(DATA_FIELD));
                if (fv != null
                && !fv.isEmpty()
                && !fv.startsWith("{")
                && !fv.startsWith("[")) {
                    throw new CruxExemption(DATA_FIELD+" not decrypt");
                }
            }
        };
    }

}
