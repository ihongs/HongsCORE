package io.github.ihongs.util;

import io.github.ihongs.Core;
import io.github.ihongs.CruxExemption;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * 消息摘要工具
 * @author Hongs
 */
public final class Digest {

    private static final char[] DIGITS = {
        '0', '1', '2', '3', '4', '5', '6', '7',
        '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'
    };
    private    MessageDigest    DIGEST ;

    /**
     * 指定算法
     * @param type MD5,SHA-1,SHA-256
     */
    public Digest(String type) {
        try {
            DIGEST = MessageDigest.getInstance(type);
        } catch (NoSuchAlgorithmException e) {
            throw new CruxExemption(e);
        }
    }

    public String digest(byte[] data) {
        return toHex(DIGEST.digest(data));
    }

    public String digest(String text) {
        byte[] data = text.getBytes();
        return toHex(DIGEST.digest(data));
    }

    public String digest( File  file)
    throws IOException {
        try (
            FileInputStream  in = new FileInputStream(file);
            FileChannel      fc = in.getChannel();
        ) {
            MappedByteBuffer bb = fc.map(
                         FileChannel.MapMode.READ_ONLY,
                         0, file.length());
                         DIGEST.update(bb);
            return toHex(DIGEST.digest( ));
        }
    }

    private static String toHex(byte[] a) {
        int  i = 0;
        int  j = a.length;
        StringBuilder s = new StringBuilder(2 * j);
        for (  ; i < j; i ++  ) {
            byte b = a[ i ];
            char d = DIGITS[b & 0xf];
            char c = DIGITS[b >>> 4 & 0xf];
            s.append(c);
            s.append(d);
        }
        return s.toString();
    }

    /**
     * 摘要实例
     * @param type MD5,SHA-1,SHA-256
     * @return
     */
    public static Digest getInstance(String type) {
        return  Core.getInstance( ).got(
            Digest.class.getName( ) + ":" + type,
            () -> new Digest (type)
        );
    }

    /**
     * MD5 实例
     * @return 
     */
    public static Digest md5() {
        return  Core.getInstance( ).got(
            Digest.class.getName( ) + ":MD5",
            () -> new Digest("MD5")
        );
    }

    public static final String md5(byte[] data) {
        return md5().digest(data);
    }

    public static final String md5(String text) {
        return md5().digest(text);
    }

    public static final String md5( File  file)
    throws IOException {
        return md5().digest(file);
    }

}
