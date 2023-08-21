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
 * @author Hongs
 */
public class Cryp {
    
   private final String sk;
   private final Cipher ec;
   private final Cipher dc;
    
    public Cryp (String sk) {
        this.sk =  sk ;
        if (sk == null) {
            ec  = null;
            dc  = null;
            return;
        }
        
        try {
            byte[] kb;
            SecretKeySpec ks;
            kb = sk.getBytes();
            ks = new SecretKeySpec(kb, "AES");

            ec = Cipher.getInstance("AES/ECB/PKCS5Padding");
            ec.init(Cipher.ENCRYPT_MODE , ks);

            dc = Cipher.getInstance("AES/ECB/PKCS5Padding");
            dc.init(Cipher.DECRYPT_MODE , ks);
        } catch (GeneralSecurityException ex) {
            throw new HongsExemption(ex);
        }
    }
    
    public static Cryp getInstance() {
        return Core.getInstance().got(
            Cryp.class.getName( ),
            ( ) -> new Cryp (
                CoreConfig.getInstance()
                          .getProperty("core.matrix.data.secret.key")
            )
        );
    }
    
    public String encrypt(String ds) {
        if (sk != null && !sk.isEmpty()
        &&  ds != null && !ds.isEmpty()) {
            try {
                byte[] db;
                Base64.Encoder  ba ;
                ba = Base64.getEncoder();
                db = ds. getBytes ( StandardCharsets.UTF_8);
                db = ec.doFinal(db);
                db = ba.encode (db);
                ds = new String(db, StandardCharsets.UTF_8);
            } catch (GeneralSecurityException ex) {
                throw new HongsExemption (ex);
            }
        }
        return  ds;
    }
    
    public String decrypt(String ds) {
        if (sk != null && !sk.isEmpty()
        &&  ds != null && !ds.isEmpty()) {
            try {
                byte[] db;
                Base64.Decoder  ba ;
                ba = Base64.getDecoder();
                db = ds. getBytes ( StandardCharsets.UTF_8);
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
