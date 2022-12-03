package io.github.ihongs.util.verify;

/**
 * 可选约束
 * @author Hongs
 */
public class Optional extends Rule {
    @Override
    public Object verify(Value watch) throws Wrong {
        // 未给值则跳过此项目
        if (watch.isDefined()) {
            return  PASS;
        } else {
            return  NONE;
        }
    }
 }
