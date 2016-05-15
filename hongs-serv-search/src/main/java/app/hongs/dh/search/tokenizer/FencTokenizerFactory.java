package app.hongs.dh.search.tokenizer;

import java.util.Map;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.util.TokenizerFactory;
import org.apache.lucene.util.AttributeFactory;

/**
 *
 * @author Hongs
 */
public class FencTokenizerFactory extends TokenizerFactory {

    public FencTokenizerFactory(Map<String, String> args) {
        super(args);
    }
    
    @Override
    public Tokenizer create(AttributeFactory af) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
    
}
