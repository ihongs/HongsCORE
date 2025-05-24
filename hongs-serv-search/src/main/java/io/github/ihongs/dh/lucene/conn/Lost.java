package io.github.ihongs.dh.lucene.conn;

import io.github.ihongs.CruxExemption;
import java.io.IOException;
import org.apache.lucene.store.AlreadyClosedException;

/**
 * 连接中断
 * @author Hongs
 */
public class Lost extends CruxExemption {
    
    public Lost (AlreadyClosedException cause, String error) {
        super(cause, 1031, error);
    }

    public Lost (IOException cause, String error) {
        super(cause, 1031, error);
    }

}
