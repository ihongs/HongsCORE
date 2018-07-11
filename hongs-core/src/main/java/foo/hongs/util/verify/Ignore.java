package foo.hongs.util.verify;

/**
 * 忽略取值
 * 用于表单配置中需跳过的字段
 */
public class Ignore extends Rule {
    @Override
    public Object verify(Object value) {
        return BLANK;
    }
}
