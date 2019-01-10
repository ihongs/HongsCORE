package io.github.ihongs.util.verify;

import io.github.ihongs.HongsException;
import java.util.Map;

/**
 * 校验接口
 * @author Hongs
 */
public interface Veri {

    public boolean isUpdate();
    public boolean isPrompt();
    /**
     * @param update 为 true 则不存在的值跳过而不去校验
     */
    public void isUpdate(boolean update);
    /**
     * @param prompt 为 true 则第一个错误发生时退出校验
     */
    public void isPrompt(boolean prompt);

    /**
     * 校验数据
     * 返回干净的数据, 校验失败则抛出 Wrongs 异常
     * @param values
     * @return
     * @throws Wrongs
     * @throws HongsException
     */
    public Map verify(Map values) throws Wrongs, HongsException;

}
