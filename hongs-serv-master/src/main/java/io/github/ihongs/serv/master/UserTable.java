package io.github.ihongs.serv.master;

import io.github.ihongs.serv.STable;
import io.github.ihongs.HongsException;
import io.github.ihongs.db.DB;
import io.github.ihongs.util.Crypto;
import java.util.Map;
import java.util.function.Function;

/**
 * 加密用户表
 *
 * 针对电话和邮箱进行加密,
 * 无需加密则不必设置此类.
 *
 * @author HuangHong
 */
public class UserTable extends STable {

    public static final String[] CRYPTO_FIELDS = new String[] {"email", "phone"};
    public static final String   CRYPTO_PREFIX = "=";
    public static final int      CRYPTO_STARTS =  1 ;

    private Function<String, String> enc = null;
    private Function<String, String> dec = null;

    public UserTable (DB db, Map conf) throws HongsException {
        super(db, conf);
    }

    public Crypto getCrypto() {
        return Crypto.getInstance();
    }
    
    @Override
    public String[] getCryptoFields() {
        return CRYPTO_FIELDS;
    }

    @Override
    public Function<String, String> encrypt() {
        if (enc == null) {
            final Crypto.Crypt enx = getCrypto().encrypt();
            if (enx.valid()) {
                enc = new Function<String, String>() {
                    @Override
                    public String apply(String t) {
                        if (t != null
                        && !t.isEmpty()
                        && !t.startsWith(CRYPTO_PREFIX)) {
                            t  = CRYPTO_PREFIX + enx.apply(t);
                        }
                        return t;
                    }
                };
            } else {
                enc = new Function<String, String>() {
                    @Override
                    public String apply(String t) {
                        return t;
                    }
                };
            }
        }
        return enc;
    }

    @Override
    public Function<String, String> decrypt() {
        if (dec == null) {
            final Crypto.Crypt dex = getCrypto().decrypt();
            if (dex.valid()) {
                dec = new Function<String, String>() {
                    @Override
                    public String apply(String t) {
                        if (t != null
                        && !t.isEmpty()
                        &&  t.startsWith(CRYPTO_PREFIX)) {
                            t  = dex.apply(t.substring(CRYPTO_STARTS));
                        }
                        return t;
                    }
                };
            } else {
                enc = new Function<String, String>() {
                    @Override
                    public String apply(String t) {
                        return t;
                    }
                };
            }
        }
        return dec;
    }

}
