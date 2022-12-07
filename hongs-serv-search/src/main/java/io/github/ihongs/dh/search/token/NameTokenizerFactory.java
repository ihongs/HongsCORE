package io.github.ihongs.dh.search.token;

import java.util.Map;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.util.TokenizerFactory;
import org.apache.lucene.util.AttributeFactory;

/**
 * 分词器工厂
 * @author Hongs
 */
public class NameTokenizerFactory extends TokenizerFactory {

    public NameTokenizerFactory(Map<String, String> args) {
        super(args);
    }
    
    @Override
    public Tokenizer create(AttributeFactory af) {
        return new NameTokenizer();
    }
    
}
