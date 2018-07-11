package foo.hongs.util.verify;

import foo.hongs.util.Synt;

/**
 * 数字校验
 * <pre>
 * 校验参数:
 *  type    数字类型, 取值: byte,short,int,long,float,double, 默认 double
 *  min     最小值
 *  max     最大值
 * </pre>
 * @author Hongs
 */
public class IsNumber extends Rule {
    @Override
    public Object verify(Object value) throws Wrong {
        // 类型转换
        String type = Synt.declare(params.get("type"), "");
        Number  num ;
        try {
            if ( "byte".equals(type)) {
                num = Synt.declare(value, ( byte) 0);
            } else
            if ("short".equals(type)) {
                num = Synt.declare(value, (short) 0);
            } else
            if (  "int".equals(type)) {
                num = Synt.declare(value, 0 );
            } else
            if ( "long".equals(type)) {
                num = Synt.declare(value, 0L);
            } else
            if ("float".equals(type)) {
                num = Synt.declare(value, 0F);
            } else {
               type = "double";
                num = Synt.declare(value, 0D);
            }
        } catch (ClassCastException er) {
            throw new Wrong("fore.form.conv.to."+type+".failed");
        }

        // 取值范围
        Double m;
        m = Synt.asDouble(params.get("min"));
        if (m != null && m > num.doubleValue()) {
            throw new Wrong("fore.form.lt.min", Double.toString(m));
        }
        m = Synt.asDouble(params.get("max"));
        if (m != null && m < num.doubleValue()) {
            throw new Wrong("fore.form.lt.max", Double.toString(m));
        }

        // 小数位数
        Short  s;
        s = Synt.asShort(params.get("scale"));
        if (s != null && s > 0) {
            double n = num.doubleValue() - num.longValue();
            n  = n * Math.pow ( 10 , s );
            if ( n > (long) n ) {
                throw new Wrong("fore.form.gt.scale", s.toString());
            }
        }

        return num;
    }
}
