package app.hongs.util.crypto;

import app.hongs.HongsError;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * 消息摘要
 * @author Hongs
 */
public class Digest {

    private static final char[] DIGITS = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'};
    private MessageDigest       DIGEST ;

    public Digest(String type) {
        try {
            DIGEST = MessageDigest.getInstance(type);
        } catch (NoSuchAlgorithmException e) {
            throw  new  HongsError.Common(e);
        }
    }

    public Digest() {
        this("MD5");
    }

    public String digest(byte[] data) {
        return bufferToHex(DIGEST.digest(data));
    }

    public String digest(String text) {
        byte[] data = text.getBytes();
        return bufferToHex(DIGEST.digest(data));
    }

    public String digest( File  file) throws FileNotFoundException, IOException {
        FileInputStream  in = new FileInputStream(file);
        FileChannel      ch = in.getChannel();
        long             fl = file.length(  );
        MappedByteBuffer bb = ch.map(FileChannel.MapMode.READ_ONLY, 0, fl);
                            DIGEST.update(bb);
        return bufferToHex( DIGEST.digest() );
    }

    private static String bufferToHex(byte[] bs) {
        return bufferToHex(bs, 0, bs.length);
    }

    private static String bufferToHex(byte[] bs, int m, int n ) {
        StringBuffer stringbuffer = new StringBuffer(2 * n);
        int k = m + n;
        for (int l = m; l < k; l++) {
            appendHexPair(bs[l], stringbuffer);
        }
        return stringbuffer.toString();
    }

    private static void appendHexPair(byte bt, StringBuffer sb) {
        char c0 = DIGITS[(bt & 0xf0) >> 4];
        char c1 = DIGITS[ bt & 0xf ];
        sb.append(c0);
        sb.append(c1);
    }

}
