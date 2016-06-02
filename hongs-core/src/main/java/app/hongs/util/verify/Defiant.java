package app.hongs.util.verify;

import app.hongs.util.Synt;
import java.util.Set;

/**
 * 弃用占位
 *
 * 字段如果可重复, 需要清空取值时,
 * 如果一个也不传, 什么也不会改变.
 * 比较简单的方法是传递一个占位值，
 * 可用此校验器设置要抛弃的占位值.
 *
 * @author Hongs
 */
public class Defiant extends Rule {
    @Override
    public Object verify(Object value) {
        Set def  =  Synt.asTerms(params.get("defiant"));
        if (null == value  ||  def.contains(  value  )) {
            return  BLANK;
        }
        return value;
    }
}
