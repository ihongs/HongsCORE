package app.hongs.util.verify;

/**
 * 校验函数
 *
 * Java8 中利用 Verfiy.Func 使用函数式, 可简化代码, 如:
 * <pre>
 *  values = new Verify()
 *      .addRule("f1", (v, r)->{
 *          return v != null ? v : BLANK;
 *      })
 *      .addRule("f2", (v, r)->{
 *          return v != null ? v : EMPTY;
 *      })
 *      .verify(values);
 * </pre>
 * 
 * @author Hongs
 */
public interface Rulx {
    
    public Object verify(Object value, Rule rule);

}
