package app.hongs.util.verify;

import app.hongs.HongsException;
import java.util.List;
import java.util.Map;

/**
 * 校验接口
 * @author Hongs
 */
public interface Veri {

    public boolean  isUpdate();
    public boolean  isPrompt();
    /**
     * @param update 为 true 则不存在的值跳过而不去校验
     */
    public void     isUpdate(boolean update);
    /**
     * @param prompt 为 true 则第一个错误发生时退出校验
     */
    public void     isPrompt(boolean prompt);

    public Map<String,List<Rule>> getRules();
    public Veri     setRule (String name, Rule... rule);
    public Veri     addRule (String name, Rule... rule);

    public Map      verify  (Map values) throws Wrongs, HongsException;

}
