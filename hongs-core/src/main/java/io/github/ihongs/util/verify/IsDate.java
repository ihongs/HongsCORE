package io.github.ihongs.util.verify;

import io.github.ihongs.Core;
import io.github.ihongs.CoreLocale;
import io.github.ihongs.HongsExemption;
import io.github.ihongs.util.Syno;
import io.github.ihongs.util.Synt;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 日期校验
 * <pre>
 * 规则参数:
 *  type    日期类型, date(Date对象),time(毫秒数),datestamp(Date对象,精确到秒),timestamp(时间戳,精确到秒)
 *  format  日期格式, 同 java 的 DateTimeFormatter , 默认从语言资源提取
 *  offset  偏移时间, 毫秒时间戳, 可用 +-, 与 format 配合来解决日期精度
 *  min     最小时间, 毫秒时间戳, 可用 +- 前缀表示当前时间偏移
 *  max     最大时间, 毫秒时间戳, 可用 +- 前缀表示当前时间偏移
 * </pre>
 * @author Hongs
 */
public class IsDate extends Rule {
    @Override
    public Object verify(Value watch) throws Wrong {
        // 跳过空值和空串
        Object value = watch.get();
        if (value  ==  null ) {
            return PASS;
        }
        if (value.equals("")) {
            return null;
        }

        String typa = Synt.declare(getParam("__type__"), "");
        String type = Synt.declare(getParam(  "type"  ), "");

        // 日期格式
        int     off = Synt.declare(getParam( "offset" ), 0 );
        String  fmt = Synt.declare(getParam( "format" ), "");
        String  fwt =  fmt ;
        if ( "".equals(fmt)) {
            fwt = CoreLocale.getInstance( ).getProperty("core.default." +typa+ ".format");
            if (fwt == null) {
                throw new HongsExemption("Can not recognize date type '"+typa+"'.");
            }
        }

        Date day = getDate(value, typa, type, fwt, off);

        // 区间校验
        String  min = Synt.declare(getParam(  "min"   ), "");
        String  max = Synt.declare(getParam(  "max"   ), "");
        long now = new Date().getTime();
        if (!"".equals(min)) {
            long tim = getTime(min, now);
            if ( tim > day.getTime( ) ) {
                throw new Wrong("@fore.form.lt.mindate", Syno.formatTime(tim, fwt));
            }
        }
        if (!"".equals(max)) {
            long tim = getTime(max, now);
            if ( tim < day.getTime( ) ) {
                throw new Wrong("@fore.form.gt.maxdate", Syno.formatTime(tim, fwt));
            }
        }

        if ("date".equals(type)) {
            return day;
        }
        if ("time".equals(type)) {
            return day.getTime();
        }
        if ("datestamp".equals(type)) {
            return day;
        }
        if ("timestamp".equals(type)) {
            return day.getTime() / 1000;
        }
        if (! "".equals(fmt)) {
            return Syno.formatTime(day , fmt);
        }

        return value;
    }

    private Date getDate(Object val, String typa, String type, String fwt, int off) throws Wrong {
        if (val instanceof Date) {
            return  (Date) val ;
        }

        if (val instanceof Number) {
            long num = Synt.declare(val,0L);
            if ("timestamp".equals(type)
            ||  "datestamp".equals(type)) {
                return new Date(num * 1000);
            } else {
                return new Date(num /* */ );
            }
        }

        if (val instanceof String) {
            String str = ( String) val;

            try {
                long num = Synt.declare(val,0L);
                if ("timestamp".equals(type)
                ||  "datestamp".equals(type)) {
                    return new Date(num * 1000);
                } else {
                    return new Date(num /* */ );
                }
            } catch (ClassCastException e) {
                // Nothing to do.
            }

            // Web 的 datetime-local 等
            // 时间精确到分需要补充零秒
            if (str.matches("(.*\\D)?\\d+:\\d+") ) {
                str += ":00";
            }

            // 按指定格式解析日期字符串
            // 要精确时间的可以使用偏移
            return Date.from(Syno.parseTime(str, fwt, Core.getZoneId(), Core.getLocale()).plusMillis(off));
        }

        throw new Wrong("@fore.form.is.not."+ typa);
    }

    private long getTime(String tim, long now) {
        Matcher mat = Pattern.compile("^([+\\-])?(\\d+)$").matcher ( tim );
        if (!mat.matches()) {
            throw new HongsExemption ("Can not recognize time '"+tim+"'.");
        }
        long    msc = Synt.declare(mat.group(2), 0L);
        String  sym = mat.group(1);
        if ("+".equals(sym)) {
            return now + msc;
        } else
        if ("-".equals(sym)) {
            return now - msc;
        } else {
            return msc;
        }
    }
}
