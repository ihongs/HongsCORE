package io.github.ihongs.dh.search.token;

import java.io.IOException;
import org.apache.lucene.analysis.CharacterUtils;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.tokenattributes.OffsetAttribute;

/**
 * 分词器
 * 专用于处理名字类的关键词
 * 按从左往右逐字增加的方式
 * 如 "Lucene 分词器" 会被解析为:
 * l lu luc luce lucen lucene 分 分词 分词器
 * @author Hongs
 */
public class NameTokenizer extends Tokenizer {

    public NameTokenizer() {
        super();
    }

    private final CharacterUtils.CharacterBuffer buffer = CharacterUtils.newCharacterBuffer(4096);
    private final CharTermAttribute termAttr = addAttribute(CharTermAttribute.class);
    private final   OffsetAttribute ofstAttr = addAttribute(  OffsetAttribute.class);
    private int bufferIndex = 0,
                bufferShift = 0,
                offsetShift = 0,
                offset = 0,
                endset = 0;

    @Override
    public boolean incrementToken() throws IOException {
        clearAttributes();
        char[] buf = termAttr.buffer();
        int    bgn, end, len, chr, cnt, bgx;

        while (true) {
            // 判断是否结束
            if (bufferIndex >= bufferShift) {
                CharacterUtils.fill(buffer , input);
                offsetShift += bufferShift ;
                bufferShift  = buffer.getLength();
                bufferIndex  = 0;
                if (bufferShift == 0) {
                    endset = correctOffset(offsetShift);
                    offset =  0 ;
                    return false;
                }
            }

            bgn = bufferIndex + offsetShift - offset;

            chr = Character.codePointAt(buffer.getBuffer(), bufferIndex);
            cnt = Character.charCount(chr);
            bufferIndex += cnt;

            chr = filterToken(chr);
            if (chr == 0x0) {
                buf = termAttr.buffer();
                offset = 0;
                continue;
            }

            len = Character.toChars(chr, buf, offset);
            end = bgn + len;

            termAttr.setLength(len + offset);
            bgx    = correctOffset(bgn);
            endset = correctOffset(end);
            ofstAttr.setOffset(bgx , endset);

            offset += cnt;
            return  true;
        }
    }

    @Override
    public void reset() throws IOException {
          super.reset();
         buffer.reset(); // make sure to reset the IO buf!!
        bufferShift = 0;
        bufferIndex = 0;
        offsetShift = 0;
        offset = 0;
        endset = 0;
    }

    @Override
    public void end() throws IOException {
          super.end();
        // set final offsetShift
        ofstAttr.setOffset(endset, endset);
    }

    /**
     * 以符号作为分隔
     * @param chr
     * @return
     */
    public int filterToken(int chr) {
        if ((chr >= 0x0  && chr <= 0x2f)
        ||  (chr >= 0x3a && chr <= 0x40)
        ||  (chr >= 0x5b && chr <= 0x60)
        ||  (chr >= 0x7b && chr <= 0x7f)) {
            return  0x0;
        }
        return chr;
    }

}
