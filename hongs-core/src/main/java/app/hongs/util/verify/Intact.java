package app.hongs.util.verify;

/**
 * 原封不动
 * 用于表单配置中无校验时占位
 */
public class Intact extends Rule {
    @Override
    public Object verify(Object value) {
        return value;
    }
}
