package io.github.ihongs.util;

import io.github.ihongs.CruxExemption;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import javax.crypto.spec.IvParameterSpec;

/**
 * 加解密工具集
 * @author Hongs
 */
public class Crypto {

    protected final          String ct;
    protected final   SecretKeySpec ks;
    protected final IvParameterSpec ps;

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

    /**
     * 是否要加密
     * @return
     */
    public boolean valid() {
        return ct != null;
    }

    private Encrypt ec = null;

    /**
     * 获取解密器
     * @return
     */
    public Crypt encrypt() {
        if (ec == null) {
            ec = new Encrypt(ct, ks, ps, encoder());
        }
        return ec;
    }

    private Decrypt dc = null;

    /**
     * 获取加密器
     * @return
     */
    public Crypt decrypt() {
        if (dc == null) {
            dc = new Decrypt(ct, ks, ps, decoder());
        }
        return dc;
    }

    /**
     * 字符编码器
     * @return
     */
    public Codec encoder() {
        return (byte[] bs) -> Base64.getEncoder().encode(bs);
    }

    /**
     * 字符解码器
     * @return
     */
    public Codec decoder() {
        return (byte[] bs) -> Base64.getDecoder().decode(bs);
    }

    public static interface Crypt {

        public Cipher getCipher();
        public boolean valid();
        public byte[] apply(byte[] bs);
        public String apply(String ds);

    }

    private static abstract class Crypts implements Crypt {

        protected final Cipher ci;
        protected final Codec  co;

        protected Crypts (int md, String ct, SecretKeySpec ks, IvParameterSpec ps, Codec co) {
           this.co  = co;
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
                throw new CruxExemption(ex);
            }
        }

        @Override
        public Cipher getCipher() {
            return ci;
        }

        @Override
        public boolean valid() {
            return ci != null;
        }

    }

    /**
     * 加密器
     */
    private static class Encrypt extends Crypts {

        private Encrypt (String ct, SecretKeySpec ks, IvParameterSpec ps, Codec co) {
            super(Cipher.ENCRYPT_MODE, ct, ks, ps, co);
        }

        @Override
        public byte[] apply(byte[] bs) {
            if (bs != null && ci != null) try {
                bs = ci.doFinal (bs);
            } catch (GeneralSecurityException ex) {
                throw new CruxExemption(ex, "@normal:core.encrypt.failed");
            }
            return bs;
        }

        @Override
        public String apply(String ds) {
            if (ds != null && ci != null) try {
                byte [] db;
                db = ds.getBytes(/**/StandardCharsets.UTF_8);
                db = ci.doFinal (db);
                db = co.  apply (db);
                ds = new String (db, StandardCharsets.UTF_8);
            } catch (GeneralSecurityException ex) {
                throw new CruxExemption(ex, "@normal:core.encrypt.failed");
            }
            return ds;
        }

    }

    /**
     * 解密器
     */
    private static class Decrypt extends Crypts {

        private Decrypt (String ct, SecretKeySpec ks, IvParameterSpec ps, Codec co) {
            super(Cipher.DECRYPT_MODE, ct, ks, ps, co);
        }

        @Override
        public byte[] apply(byte[] bs) {
            if (bs != null && ci != null) try {
                bs = ci.doFinal (bs);
            } catch (GeneralSecurityException ex) {
                throw new CruxExemption(ex, "@normal:core.decrypt.failed");
            }
            return bs;
        }

        @Override
        public String apply(String ds) {
            if (ci != null) try {
                byte [] db;
                db = ds.getBytes(/**/StandardCharsets.UTF_8);
                db = co.  apply (db);
                db = ci.doFinal (db);
                ds = new String (db, StandardCharsets.UTF_8);
            } catch (GeneralSecurityException ex) {
                throw new CruxExemption(ex, "@normal:core.decrypt.failed");
            } catch (IllegalArgumentException ex) {
                throw new CruxExemption(ex, "@normal:core.decrypt.invalid", ds);
            }
            return ds;
        }

    }

    public static interface Codec {

        public byte[] apply(byte[] bs);

    }

    /**
     * 同 Base64.getUrlEncoder, 可换 padding 符号
     */
    public static class Encoder implements Codec {

        private final  byte pad;

        public Encoder(byte pad) {
            this.pad = pad;
        }

        @Override
        public byte[] apply(byte[] bs) {
            bs = Base64.getUrlEncoder().encode(bs);
            if ( pad != '=' && bs.length > 3 )
            for(int i = bs.length - 1; i > bs.length - 4; i --) {
                if (bs[i] == '=') {
                    bs[i] =  pad;
                } else {
                    break;
                }
            }
            return  bs;
        }

    }

    /**
     * 同 Base64.getUrlDecoder, 可换 padding 符号
     */
    public static class Decoder implements Codec {

        private final  byte pad;

        public Decoder(byte pad) {
            this.pad = pad;
        }

        @Override
        public byte[] apply(byte[] bs) {
            if ( pad != '=' && bs.length > 3 )
            for(int i = bs.length - 1; i > bs.length - 4; i --) {
                if (bs[i] == pad) {
                    bs[i] =  '=';
                } else {
                    break;
                }
            }
            bs = Base64.getUrlDecoder().decode(bs);
            return  bs;
        }

    }

}
