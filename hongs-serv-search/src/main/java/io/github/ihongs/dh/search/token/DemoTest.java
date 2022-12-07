package io.github.ihongs.dh.search.token;

import java.io.IOException;
import java.io.StringReader;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.custom.CustomAnalyzer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.tokenattributes.OffsetAttribute;

/**
 * 分析器测试
 * 已经逐步针对中文特点构建自己的分析器
 * @author Hongs
 */
public class DemoTest {

    public static void main(String[] args) throws IOException {
        Analyzer az = CustomAnalyzer.builder()
            //.withTokenizer("Standard")
            .withTokenizer("Name")
            .addTokenFilter("EdgeNGram", "minGramSize", "1", "maxGramSize", "20")
            //.addTokenFilter("ICUTransform", "id", "Han-Latin;NFD;[[:NonspacingMark:][:Space:]] Remove")
            //.addTokenFilter("EdgeNGram", "minGramSize", "1", "maxGramSize", "20")
            .build();

        StringReader      sr = new StringReader(args[0]);
        TokenStream       ts = az.tokenStream  ("" , sr);
        OffsetAttribute   oa = ts.addAttribute (OffsetAttribute.class);
        CharTermAttribute ta = ts.addAttribute (CharTermAttribute.class);

        try {
            ts.reset(); // Resets this stream to the beginning. (Required)
            while (ts.incrementToken()) {
                System.out.println(ta.toString() + "|" + ta.length()
                        + "[" + oa.startOffset() + "," + oa.endOffset() + "]");
            }
            ts.end(  ); // Perform end-of-stream operations, e.g. set the final offset.
        } finally {
            ts.close(); // Release resources associated with this stream.
        }

    }

}
