package io.github.ihongs.serv.matrix;

import io.github.ihongs.Core;
import io.github.ihongs.CoreConfig;
import io.github.ihongs.HongsException;
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

    public DataTable (DB db, Map conf) throws HongsException {
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
                CoreConfig cc = CoreConfig.getInstance("matrix");
                return new Crypto(
                    cc.getProperty("core.matrix.data.crypto.type"),
                    cc.getProperty("core.matrix.data.crypto.sk"),
                    cc.getProperty("core.matrix.data.crypto.iv")
                );
            });
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
                        &&  fv.startsWith("{")) {
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

}
