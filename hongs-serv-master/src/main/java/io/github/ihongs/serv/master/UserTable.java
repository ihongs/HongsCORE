package io.github.ihongs.serv.master;

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
 * 加密用户表
 *
 * 针对电话和邮箱进行加密,
 * 无需加密则不必设置此类.
 *
 * @author HuangHong
 */
public class UserTable extends PrivTable {

    private static final String[] CRYPTO_FIELDS = new String[] {"email", "phone"};
    private static final String   CRYPTO_PREFIX = "=";
    private static final int      CRYPTO_STARTS =  1 ;

    private Crypto   crypto   = null;
    private Consumer<Map> enc = null;
    private Consumer<Map> dec = null;

    public UserTable (DB db, Map conf) throws CruxException {
        super(db, conf);
    }

    @Override
    public void setCrypto(Crypto crypto) {
        this.crypto = crypto;
    }
    
    @Override
    public Crypto getCrypto() {
        return crypto != null ? crypto : Core.getInstance().got(
            Crypto.class.getName() + ":master.user" , () -> {
                CoreConfig cc = CoreConfig.getInstance();
                return new Crypto(
                    cc.getProperty("core.master.user.crypto.type"),
                    cc.getProperty("core.master.user.crypto.sk"),
                    cc.getProperty("core.master.user.crypto.iv")
                );
            });
    }

    /**
     * 加密, 以便作为查询参数
     * @param val
     * @return
     */
    public String encrypt(String val) {
        // 加密
        Crypto.Crypt enx = getCrypto().encrypt();
        if (enx.valid()
        &&  val != null
        && !val.isEmpty()
        && !val.startsWith(CRYPTO_PREFIX)) {
            val = CRYPTO_PREFIX + enx.apply(val);
        }

        return val;
    }

    /**
     * 解密, 以便特殊情况使用
     * @param val
     * @return
     */
    public String decrypt(String val) {
        // 加密
        Crypto.Crypt enx = getCrypto().decrypt();
        if (enx.valid()
        &&  val != null
        && !val.isEmpty()
        &&  val.startsWith(CRYPTO_PREFIX)) {
            val = enx.apply ( val.substring(1) );
        }

        return val;
    }

    @Override
    public Consumer<Map> encrypt() {
        if (enc == null) {
            final Crypto.Crypt enx = getCrypto().encrypt();
            if (enx.valid()) {
                enc = new Consumer<Map>() {
                    @Override
                    public void accept(Map values) {
                        for(String fn : CRYPTO_FIELDS) {
                            String fv = Synt.asString(values.get(fn));
                            if (fv != null
                            && !fv.isEmpty()
                            && !fv.startsWith(CRYPTO_PREFIX)) {
                                fv  = CRYPTO_PREFIX + enx.apply( fv );
                                values.put(fn , fv);
                            }
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
                        for(String fn : CRYPTO_FIELDS) {
                            String fv = Synt.asString(values.get(fn));
                            if (fv != null
                            && !fv.isEmpty()
                            &&  fv.startsWith(CRYPTO_PREFIX)) {
                                fv  = dex.apply(fv.substring(CRYPTO_STARTS));
                                values.put(fn , fv);
                            }
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
                for(String fn : CRYPTO_FIELDS) {
                    String fv = Synt.asString(values.get(fn));
                    if (fv != null
                    && !fv.isEmpty()
                    &&  fv.startsWith(CRYPTO_PREFIX)) {
                        throw new CruxExemption(fn+" not decrypt");
                    }
                }
            }
        };
    }

}
