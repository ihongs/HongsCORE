package io.github.ihongs.action;

import io.github.ihongs.Cnst;
import io.github.ihongs.Core;
import io.github.ihongs.CoreRoster;
import io.github.ihongs.CoreRoster.Mathod;
import io.github.ihongs.HongsException;
import io.github.ihongs.HongsExemption;
import io.github.ihongs.action.anno.Action;
import io.github.ihongs.action.anno.Assign;
import io.github.ihongs.action.anno.Filter;
import io.github.ihongs.action.anno.FilterInvoker;
import io.github.ihongs.dh.IActing;
import io.github.ihongs.util.Dawn;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.InvocationTargetException;
import java.util.Map;

/**
 * 动作执行器
 *
 * <h3>异常代码</h3>
 * <pre>
 * 区间: 0x1100~0x110f
 * 0x1100 错误请求
 * 0x1101 尚未登陆
 * 0x1102 区域错误
 * 0x1103 无权访问
 * 0x1104 无此动作
 * 0x1105 非法请求
 * 0x110e 内部错误
 * 0x110f 无法执行, 禁止访问或参数错误
 * </pre>
 *
 * @author Hong
 */
public class ActionRunner {
    private final String   action;
    private final Object   object;
    private final Method   method;
    private final Class<?> mclass;
    private final ActionHelper helper;
    private final Annotation[] annarr;

    private final int len ;
    private final int low = -1;
    private       int idx = -1;

    public ActionRunner(ActionHelper helper, Object object, String method)
    throws HongsException {
        this.helper = helper;
        this.object = object;
        this.mclass = object.getClass();

        // 从类里面获取方法
        try {
            this.method = this.mclass.getMethod(method, ActionHelper.class);
            this.annarr = this.method.getAnnotations();
            this.len    = this.annarr.length;
        } catch (NoSuchMethodException ex) {
            throw new HongsException(0x1104, "Can not find action '"+ mclass.getName() +"."+ method +"'");
        } catch (    SecurityException ex) {
            throw new HongsException(0x1104, "Can not exec action '"+ mclass.getName() +"."+ method +"'");
        }

        // 从注解中提取动作
        Action a; String c, e;
        a = this.mclass.getAnnotation(Action.class);
        if (null != a) {
            c = a.value();
        } else {
            c = this.mclass.getName();
            c = c.replace( '.', '/' );
        }
        a = this.method.getAnnotation(Action.class);
        if (null != a) {
            e = a.value();
        } else {
            e = this.method.getName();
        //  e = e.replace( '.', '/' );
        }
        this.action = c + "/" + e;
    }

    public ActionRunner(ActionHelper helper, String action)
    throws HongsException {
        Mathod mt = getActions().get(action);
        if ( null == mt ) {
            throw new HongsException(0x1104, "Can not find action '"+ action +"'");
        }

        this.action = action;
        this.helper = helper;
        this.mclass = mt.getMclass();
        this.method = mt.getMethod();
        this.object = Core.getInstance(mclass);
        this.annarr = method.getAnnotations( );
        this.len    = annarr.length ;
    }

    /**
     * 构建 AcitonRunner 对象
     *
     * 尝试寻找正确的动作路径
     * 并从其末尾提取请求参数
     *
     * @param ah
     * @param at
     * @return
     * @throws HongsException
     */
    public static ActionRunner newInstance(ActionHelper ah, String at)
    throws HongsException {
        // 查询结构
        String ap = null; // 虚拟动作路径, 作为目标路径
        String aq = null; // 关联请求参数, 转为请求数据
        Map rd;           // 关联请求数据
        Map ad;           // 全部动作集合
        int ps;

        ps = at.indexOf('?');
        if (ps > -1) {
            aq = at.substring(1 + ps).trim();
            at = at.substring(0 , ps).trim();
        }
        ps = at.indexOf('!');
        if (ps > -1) {
            ap = at.substring(1 + ps).trim();
            at = at.substring(0 , ps).trim();
        }

        // 请求参数
        if (null != aq && !"".equals(aq)) {
            if (aq.startsWith("{" )
            &&  aq.  endsWith("}")) {
                rd = ( Map ) Dawn. toObject ( aq );
            } else {
                rd = ActionHelper.parseQuery( aq );
            }
            ah.getRequestData( /**/ ).putAll( rd );
        }

        // 虚拟路径
        ad = ActionRunner.getActions(  );
        if (null != ap && !"".equals(ap)) {
            ah.setAttribute(Cnst.ACTION_ATTR, ap + Cnst.ACT_EXT);

            /**
             * 目标动作已经存在
             * 直接使用不走代理
             */
            if ( ad.containsKey(ap)) {
                 at = ap;
            }
        } else {
            ah.setAttribute(Cnst.ACTION_ATTR, at + Cnst.ACT_EXT);

            /**
             * 当前动作并不存在
             * 尝试逐级向上查找
             */
            if (!ad.containsKey(at)) {
                String   mt; // 方法
                String   ot; // 资源
                    ot = at;
                if  (  0 < (ps = ot.lastIndexOf("/"))) {
                    mt = ot.substring(0+ps);
                    ot = ot.substring(0,ps);
                while (0 < (ps = ot.lastIndexOf("/"))) {
                    ot = ot.substring(0,ps);
                         ap = ot  +  mt;
                    if ( ad.containsKey(ap)) {
                         at = ap; break;
                    }
                }}
            }
        }

        return new ActionRunner(ah, at);
    }

    /**
     * 获取当前 ActionHelper
     * @return
     */
    public ActionHelper getHelper() {
        return helper;
    }

    /**
     * 获得当前 action 对象
     * @return
     */
    public Object getObject() {
        return object;
    }

    /**
     * 获得当前动作方法对象
     * @return
     */
    public Method getMethod() {
        return method;
    }

    /**
     * 执行初始方法
     * 会执行 acting 方法, doAction,doInvoke 内已调
     * @throws HongsException
     */
    public void doActing() throws HongsException {
        // Reset
        idx = 0;

        // Regist the runner
        helper.setAttribute(ActionRunner.class.getName(), this);

        // Initialize action
        if (object instanceof IActing) {
           (  (  IActing  )  object  ).acting(  helper  , this);
        }
    }

    /**
     * 执行动作方法
     * 会执行 action 方法上 annotation 指定的过滤器
     * @throws HongsException
     */
    public void doAction() throws HongsException {
        // 如果正处于链头, 则作初始化
        if ( idx == low ) {
            doActing();
        //  return;
        }

        // 如果已到达链尾, 则执行动作
        if ( idx == len ) {
            doInvoke();
            return;
        }

        Annotation anno = annarr[idx ++];
        Filter     actw ;
        if (anno instanceof Filter) {
            actw = ( Filter ) anno;
        } else {
            actw = anno.annotationType()
            .getAnnotation(Filter.class);
        }

        // 如果不是动作链, 则跳过注解
        if (actw == null) {
            doAction();
            return;
        }

        // 执行注解过滤器
        Class <? extends FilterInvoker> classo = actw.value();
        Core.getInstance(classo).invoke(helper , this, anno );
    }

    /**
     * 执行动作方法
     * 不执行 action 方法上 annotation 指定的过滤器
     * @throws HongsException
     */
    public void doInvoke() throws HongsException {
        if (idx == low) {
            doActing( );
        } else {
            idx  =  0  ;
        }

        try {
            method.invoke(object, helper);
        } catch (   IllegalAccessException e) {
            throw new HongsException(0x110f, "Illegal access for method '"+mclass.getName()+"."+method.getName()+"(ActionHelper).");
        } catch ( IllegalArgumentException e) {
            throw new HongsException(0x110f, "Illegal params for method '"+mclass.getName()+"."+method.getName()+"(ActionHelper).");
        } catch (InvocationTargetException e) {
            Throwable  ex = e.getCause( );
            if (ex instanceof HongsExemption) {
                throw (HongsExemption) ex;
            } else
            if (ex instanceof HongsException) {
                throw (HongsException) ex;
            } else {
                throw new HongsException(0x110e, ex);
            }
        }
    }

    //** 更方便的获取模块、实体、操作的方式 **/

    private String mod = null;
    private String ent = null;
    private String met = null;

    public void setModule(String name) {
        mod = name;
    }
    public void setEntity(String name) {
        ent = name;
    }
    public void setHandle(String name) {
        met = name;
    }

    /**
     * 获取待操作的模块/配置
     * @return
     */
    public String getModule() {
        if (null != mod) {
            return  mod;
        }

        // 从注解提取模块名称
        Assign ing;
        ing = method.getAnnotation(Assign.class);
        if (null != ing) {
            return  ing.conf();
        }
        ing = mclass.getAnnotation(Assign.class);
        if (null != ing) {
            return  ing.conf();
        }

        int pos;
        mod  = getAction();
        pos  = mod.lastIndexOf('/');
        mod  = mod.substring(0,pos); // 去掉动作
        pos  = mod.lastIndexOf('/');
        mod  = mod.substring(0,pos); // 去掉实体
        return mod;
    }

    /**
     * 获取待操作的实体/表单
     * @return
     */
    public String getEntity() {
        if (null != ent) {
            return  ent;
        }

        // 从注解提取实体名称
        Assign ing;
        ing = method.getAnnotation(Assign.class);
        if (null != ing) {
            return  ing.name();
        }
        ing = mclass.getAnnotation(Assign.class);
        if (null != ing) {
            return  ing.name();
        }

        int pos;
        ent  = getAction();
        pos  = ent.lastIndexOf('/');
        ent  = ent.substring(0,pos); // 去掉动作
        pos  = ent.lastIndexOf('/');
        ent  = ent.substring(1+pos); // 得到实体
        return ent;
    }

    /**
     * 获取待操作的动作/方法
     * @return
     */
    public String getHandle() {
        if (null != met) {
            return  met;
        }

        int pos;
        met  = getAction();
        pos  = met.lastIndexOf('/');
        met  = met.substring(1+pos); // 得到动作
        return met;
    }

    /**
     * 获取动作名
     * 设置请使用 getHelper().setAttribute(Cnst.ACTION_ATTR, "x/y/z.act")
     * @return
     */
    public String getAction() {
        // 去除路径中的根目录和扩展名
        String  act = (String) helper.getAttribute(Cnst.ACTION_ATTR);
        if (act != null) {
            int pos = act.lastIndexOf('.');
            if (pos > 0) {
                act = act.substring(0,pos);
            }
            return act;
        }
        return  action;
    }

    //** 动作方法 **/

    public static Map<String, Mathod> getActions() {
        return CoreRoster.getActions();
    }

}
