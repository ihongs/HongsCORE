package io.github.ihongs.util;

import io.github.ihongs.Core;
import io.github.ihongs.CoreConfig;
import io.github.ihongs.HongsExemption;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.Base64;
import java.util.Base64.Decoder;
import java.util.Base64.Encoder;
import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import javax.crypto.spec.IvParameterSpec;

/**
 * 加解密工具集
 *
 * <pre>
 * 默认配置(default.properties):
 *  core.crypto.default.type=默认加密形式, 如 AES/ECB/PKCS5Padding
 *  core.crypto.default.sk=默认密钥, 需为 16/24/32 位
 *  core.crypto.default.iv=加密向量, 需为 16 位
 * 默认未设, 即不加密
 * </pre>
 *
 * @author Hongs
 */
public class Crypto {

    private final          String ct;
    private final   SecretKeySpec ks;
    private final IvParameterSpec ps;
    protected Encrypt ec = null;
    protected Decrypt dc = null;

    /**
     * 加密组件
     * @param type 加密形式
     * @param sk 密钥
     * @param iv 向量
     */
    public Crypto (String type, byte[] sk, byte[] iv) {
        if (type != null && ! type.isEmpty()) {
            if (sk != null && sk.length > 0 ) {
                int p = type.indexOf("/");
                if (p == -1) {
                    p = type.length (   );
                }
                ks  = new SecretKeySpec(sk, type.substring(0, p));
            } else {
                ks  = null;
            }
            if (iv != null && iv.length > 0 ) {
                ps  = new IvParameterSpec(iv);
            } else {
                ps  = null;
            }
            ct  = type;
        } else {
            ct  = null;
            ks  = null;
            ps  = null;
        }
    }

    /**
     * 加密组件
     * @param type 加密形式
     * @param sk 密钥
     * @param iv 向量
     */
    public Crypto (String type, String sk, String iv) {
        this(type,
            (byte[]) (sk != null ? sk.getBytes() : null),
            (byte[]) (iv != null ? iv.getBytes() : null)
        );
    }

    /**
     * 加密组件
     * @param type 加密形式
     * @param sk 密钥
     */
    public Crypto (String type, byte[] sk) {
        this(type, sk, null);
    }

    /**
     * 加密组件
     * @param type 加密形式
     * @param sk 密钥
     */
    public Crypto (String type, String sk) {
        this(type, sk, null);
    }

    public static interface Crypt {

        public boolean valid();
        public byte[] apply(byte[] bs);
        public String apply(String ds);

    }

    private static abstract class Crypts implements Crypt {

        protected final Cipher ci;

        protected Crypts (int md, String ct, SecretKeySpec ks, IvParameterSpec ps) {
            if (ct == null) {
                ci  = null;
                return;
            }
            try {
                ci = Cipher.getInstance(ct);
                if (ps != null) {
                    ci.init(md, ks, ps);
                } else {
                    ci.init(md, ks);
                }
            } catch (GeneralSecurityException ex) {
                throw new HongsExemption (ex);
            }
        }

        @Override
        public boolean valid() {
            return ci != null;
        }

        @Override
        public byte[] apply(byte[] bs) {
            if (ci != null) try {
                bs = ci.doFinal (bs);
            } catch (GeneralSecurityException ex) {
                throw new HongsExemption (ex);
            }
            return bs;
        }

    }

    /**
     * 加密器
     */
    private static class Encrypt extends Crypts {

        private Encrypt (String ct, SecretKeySpec ks, IvParameterSpec ps) {
            super(Cipher.ENCRYPT_MODE, ct, ks, ps);
        }

        @Override
        public String apply(String ds) {
            if (ci != null) try {
                byte [] db;
                Encoder ba;
                ba = Base64.getEncoder();
                db = ds.getBytes(/**/StandardCharsets.UTF_8);
                db = ci.doFinal (db);
                db = ba. encode (db);
                ds = new String (db, StandardCharsets.UTF_8);
            } catch (GeneralSecurityException ex) {
                throw new HongsExemption (ex);
            }
            return ds;
        }

    }

    /**
     * 解密器
     */
    private static class Decrypt extends Crypts {

        private Decrypt (String ct, SecretKeySpec ks, IvParameterSpec ps) {
            super(Cipher.DECRYPT_MODE, ct, ks, ps);
        }

        @Override
        public String apply(String ds) {
            if (ci != null) try {
                byte [] db;
                Decoder ba;
                ba = Base64.getDecoder();
                db = ds.getBytes(/**/StandardCharsets.UTF_8);
                db = ba. decode (db);
                db = ci.doFinal (db);
                ds = new String (db, StandardCharsets.UTF_8);
            } catch (GeneralSecurityException ex) {
                throw new HongsExemption (ex);
            }
            return ds;
        }

    }

    /**
     * 是否要加密
     * @return
     */
    public boolean valid() {
        return ct != null;
    }

    /**
     * 获取解密器
     * @return
     */
    public Crypt encrypt() {
        if (ec == null) {
            ec = new Encrypt(ct, ks, ps);
        }
        return ec;
    }

    /**
     * 获取加密器
     * @return
     */
    public Crypt decrypt() {
        if (dc == null) {
            dc = new Decrypt(ct, ks, ps);
        }
        return dc;
    }

    /**
     * 获取默认加解密实例
     * @return
     */
    public static Crypto getInstance() {
        return Core.getInstance().got(
            Crypto.class.getName( ),
            ( ) -> {
                CoreConfig cc = CoreConfig.getInstance();
                return new Crypto (
                    cc.getProperty("core.crypto.default.type"),
                    cc.getProperty("core.crypto.default.sk"),
                    cc.getProperty("core.crypto.default.iv")
                );
            }
        );
    }

}
