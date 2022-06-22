package io.github.ihongs.dh.lucene.conn;

/**
 * 仓库连接工厂
 * @author Hongs
 */
public interface ConnGetter {
    
    public Conn get(String dbpath, String dbname);
    
}
