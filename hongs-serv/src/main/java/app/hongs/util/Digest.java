package app.hongs.util;

import app.hongs.Core;
import app.hongs.HongsError;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * 消息摘要工具
 *
 * 注意: 非线程安全
 *
 * @author Hongs
 */
public class Digest {

    private static final char[] DIGITS = {
        '0', '1', '2', '3', '4', '5', '6', '7',
        '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'
    };
    private    MessageDigest    DIGEST ;

    /**
     * MD5 算法
     */
    public Digest() {
        this("MD5");
    }

    /**
     * 指定算法
     * @param type MD5,SHA
     */
    public Digest(String type) {
        try {
            DIGEST = MessageDigest.getInstance(type);
        } catch (NoSuchAlgorithmException e) {
            throw  new  HongsError.Common(e);
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
        for (  ; i < j; i ++ ) {
            byte b = a[i];
            char e = DIGITS[ b & 0xf ];
            char c = DIGITS[(b & 0xf0) >> 4];
            s.append(c);
            s.append(e);
        }
        return s.toString();
    }

    public static final String md5(byte[] data) {
        return Core.getInstance(Digest.class).digest(data);
    }

    public static final String md5(String text) {
        return Core.getInstance(Digest.class).digest(text);
    }

    public static final String md5(File file)
    throws IOException {
        return Core.getInstance(Digest.class).digest(file);
    }

}
