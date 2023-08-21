package io.github.ihongs.serv.matrix;

import io.github.ihongs.Core;
import io.github.ihongs.CoreConfig;
import io.github.ihongs.HongsExemption;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

/**
 * 数据加密解密
 *
 * 配置项(default.properties)：
 *  core.matrix.data.secret.key=密钥, 需为 16/24/32 位
 *  core.matrix.data.crypto=加密方式, 默认 AES/ECB/PKCS5Padding
 * 密钥默认不设, 即不加密
 *
 * @author Hongs
 */
public class Cryp {

   private final SecretKeySpec ks;
   private final String tr ;
   private       Cipher ec = null;
   private       Cipher dc = null;

    public static Cryp getInstance() {
        return Core.getInstance().got(
            Cryp.class.getName( ),
            ( ) -> new Cryp (
                CoreConfig.getInstance().getProperty("core.matrix.data.secret.key"),
                CoreConfig.getInstance().getProperty("core.matrix.data.crypto", "AES/ECB/PKCS5Padding")
            )
        );
    }

    /**
     * 加密组件
     * @param sk 密钥
     * @param tr 转换形式
     */
    public Cryp (String sk, String tr) {
        if (sk != null && !sk.isEmpty()) {
            this.ks = new SecretKeySpec(sk.getBytes(), tr.substring(0, tr.indexOf("/")));
            this.tr =  tr ;
        } else {
            this.ks = null;
            this.tr = null;
        }
    }

    /**
     * 加密
     * @param ds 明文
     * @return
     */
    public String encrypt(String ds) {
        if (ks != null && ds != null && !ds.isEmpty()) {
            try {
                if (ec == null) {
                    ec = Cipher.getInstance(tr);
                    ec.init ( Cipher.ENCRYPT_MODE , ks );
                }
                Base64.Encoder  ba = Base64.getEncoder();

                byte [] db;
                db = ds.getBytes   (StandardCharsets.UTF_8);
                db = ec.doFinal(db);
                db = ba.encode (db);
                ds = new String(db, StandardCharsets.UTF_8);
            } catch (GeneralSecurityException ex) {
                throw new HongsExemption (ex);
            }
        }
        return  ds;
    }

    /**
     * 解密
     * @param ds 密文
     * @return
     */
    public String decrypt(String ds) {
        if (ks != null && ds != null && !ds.isEmpty()) {
            try {
                if (dc == null) {
                    dc = Cipher.getInstance(tr);
                    dc.init ( Cipher.DECRYPT_MODE , ks );
                }
                Base64.Decoder  ba = Base64.getDecoder();

                byte [] db;
                db = ds.getBytes   (StandardCharsets.UTF_8);
                db = ba.decode (db);
                db = dc.doFinal(db);
                ds = new String(db, StandardCharsets.UTF_8);
            } catch (GeneralSecurityException ex) {
                throw new HongsExemption (ex);
            }
        }
        return  ds;
    }

}
