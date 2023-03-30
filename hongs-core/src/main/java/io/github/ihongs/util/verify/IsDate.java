package io.github.ihongs.util.verify;

import io.github.ihongs.CoreLocale;
import io.github.ihongs.HongsExemption;
import io.github.ihongs.util.Inst;
import io.github.ihongs.util.Synt;
import java.time.format.DateTimeParseException;
import java.time.Instant;
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
                throw new Wrong("@fore.form.lt.mindate", Inst.format(tim, fwt));
            }
        }
        if (!"".equals(max)) {
            long tim = getTime(max, now);
            if ( tim < day.getTime( ) ) {
                throw new Wrong("@fore.form.gt.maxdate", Inst.format(tim, fwt));
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
            return day.getTime() / 1000 ;
        }
        if (! "".equals(fmt)) {
            return Inst.format(day, fmt);
        }

        return value;
    }

    private Date getDate(Object val, String typa, String type, String fwt, int off) throws Wrong {
        if (val instanceof Date) {
            return  (Date) val ;
        }
        if (val instanceof Instant) {
            return Date.from((Instant) val);
        }

        try {
            long num = Synt.asLong(val);
            if ("timestamp".equals(type)
            ||  "datestamp".equals(type)) {
                 num = num * 1000;
            }
            return new Date (num);
        } catch (ClassCastException e ) {
            if ("timestamp".equals(type)
            ||  "datestamp".equals(type)) {
                 off = off * 1000;
            }
        }

        String str = Synt.asString(val);
        if (str == null || str.isEmpty()) {
            throw new Wrong("@fore.form.is.not."+ typa);
        }

        // 按指定格式解析日期字符串
        // 要精确时间的可以使用偏移
        try {
            return Date.from(Inst.parse(str, fwt).plusMillis(off));
        } catch (DateTimeParseException e) {
            // Nothing to do.
        }

        /*
        // 识别 ISO 或 datetime-local,date,time 格式
        String fmt ;
        fmt = "yyyy-MM-ddTH:m:s";
        if (!fmt.equals(fwt)) try {
            return Date.from(Inst.parse(str, fmt).plusMillis(off));
        } catch (DateTimeParseException e) {
            // Nothing to do.
        }
        fmt = "yyyy-MM-ddTH:m";
        if (!fmt.equals(fwt)) try {
            return Date.from(Inst.parse(str, fmt).plusMillis(off));
        } catch (DateTimeParseException e) {
            // Nothing to do.
        }
        fmt = "yyyy-MM-dd";
        if (!fmt.equals(fwt)) try {
            return Date.from(Inst.parse(str, fmt).plusMillis(off));
        } catch (DateTimeParseException e) {
            // Nothing to do.
        }
        fmt = "H:m:s";
        if (!fmt.equals(fwt)) try {
            return Date.from(Inst.parse(str, fmt).plusMillis(off));
        } catch (DateTimeParseException e) {
            // Nothing to do.
        }
        fmt = "H:m";
        if (!fmt.equals(fwt)) try {
            return Date.from(Inst.parse(str, fmt).plusMillis(off));
        } catch (DateTimeParseException e) {
            // Nothing to do.
        }
        */

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
