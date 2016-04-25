package app.hongs.action.anno;

import java.lang.annotation.Target;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * 操作成功才提交数据更改
 * @author Hongs
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Filter(CommitInvoker.class)
public @interface CommitSuccess { }
