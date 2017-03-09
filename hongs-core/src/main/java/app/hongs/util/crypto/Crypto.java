package app.hongs.util.crypto;

import app.hongs.CoreConfig;
import app.hongs.HongsException;
import java.io.UnsupportedEncodingException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.SecretKeySpec;

/**
 * 加密解密
 * @author Hongs
 */
public class Crypto {

    public static String encrypt(String content)
    throws HongsException {
        try {
            return new String(docrypt(content.getBytes(), null, null, 0, false), "UTF-8");
        } catch (UnsupportedEncodingException ex) {
            throw new HongsException.Common ( ex);
        }
    }

    public static String decrypt(String content)
    throws HongsException {
        try {
            return new String(docrypt(content.getBytes(), null, null, 0, true ), "UTF-8");
        } catch (UnsupportedEncodingException ex) {
            throw new HongsException.Common ( ex);
        }
    }

    public static String encrypt(String content, String key)
    throws HongsException {
        try {
            return new String(docrypt(content.getBytes(), key , null, 0, false), "UTF-8");
        } catch (UnsupportedEncodingException ex) {
            throw new HongsException.Common ( ex);
        }
    }

    public static String decrypt(String content, String key)
    throws HongsException {
        try {
            return new String(docrypt(content.getBytes(), key , null, 0, true ), "UTF-8");
        } catch (UnsupportedEncodingException ex) {
            throw new HongsException.Common ( ex);
        }
    }

    public static byte[] encrypt(byte[] content)
    throws HongsException {
        return docrypt(content, null, null, 0, false);
    }

    public static byte[] decrypt(byte[] content)
    throws HongsException {
        return docrypt(content, null, null, 0, true );
    }

    public static byte[] encrypt(byte[] content, String key)
    throws HongsException {
        return docrypt(content, key , null, 0, false);
    }

    public static byte[] decrypt(byte[] content, String key)
    throws HongsException {
        return docrypt(content, key , null, 0, true );
    }

    /**
     * 加解密
     * @param content 待加解密内容
     * @param key 秘钥
     * @param enc 算法
     * @param len 长度
     * @param dec 解密 true, 加密 false
     * @return
     * @throws HongsException 
     */
    public static byte[] docrypt(byte[] content, String key, String enc, int len, boolean dec)
    throws HongsException {
        CoreConfig conf = CoreConfig.getInstance();
        if (key == null) {
            key  = conf.getProperty("core.crypto.seckey", "HSC");
        }
        if (enc == null) {
            enc  = conf.getProperty("core.crypto.method", "AES");
        }
        if (len ==  0  ) {
            len  = conf.getProperty("core.crypto.length",  128 );
        }

        try {
            SecretKeySpec kspc;
            KeyGenerator  kgen;
            Cipher        cphr;

            kgen = KeyGenerator.getInstance(enc);
            cphr =       Cipher.getInstance(enc);

            kgen.init(len, new SecureRandom(key.getBytes()));
            kspc = new SecretKeySpec(kgen.generateKey().getEncoded(), enc);
            cphr.init(dec? Cipher.DECRYPT_MODE: Cipher.ENCRYPT_MODE, kspc);

            return cphr.doFinal(content);
        } catch (NoSuchAlgorithmException e) {
            throw new HongsException.Common(e);
        } catch (NoSuchPaddingException e) {
            throw new HongsException.Common(e);
        } catch (InvalidKeyException e) {
            throw new HongsException.Common(e);
        } catch (IllegalBlockSizeException e) {
            throw new HongsException.Common(e);
        } catch (BadPaddingException e) {
            throw new HongsException.Common(e);
        }
    }

}
