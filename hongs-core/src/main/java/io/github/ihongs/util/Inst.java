package io.github.ihongs.util;

import io.github.ihongs.Core;

import java.util.Date;
import java.util.Locale;
import java.time.ZoneId;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.ChronoField;
import java.time.temporal.TemporalField;
import java.time.temporal.TemporalAccessor;

/**
 * 时间格式工具
 *
 * @author Hongs
 */
public final class Inst
{

  private Inst() {}

  /**
   * 格式化时长
   * @param time 毫秒数
   * @return 天时分秒单位分别为 d,h,m,s
   */
  public static String phrase(long time)
  {
    return phrase(time, "d", "h", "m", "s");
  }

  /**
   * 格式化时长
   * @param time 毫秒数
   * @param d 天
   * @param h 时
   * @param m 分
   * @param s 秒
   * @return
   */
  public static String phrase(long time, String d, String h, String m, String s)
  {
    StringBuilder sb = new StringBuilder();
    int item;

    item = (int) Math.floor(time / 86400000);
    if (item > 0) {  time = time % 86400000 ;
        sb.append(item).append(d);
    } else
    if (time > 0 && 0 < sb.length()) {
        sb.append("0" ).append(d);
    }

    item = (int) Math.floor(time / 3600000);
    if (item > 0) {  time = time % 3600000 ;
        sb.append(item).append(h);
    } else
    if (time > 0 && 0 < sb.length()) {
        sb.append("0" ).append(h);
    }

    item = (int) Math.floor(time / 60000);
    if (item > 0) {  time = time % 60000 ;
        sb.append(item).append(m);
    } else
    if (time > 0 && 0 < sb.length()) {
        sb.append("0" ).append(m);
    }

    float last = (float) time / 1000 ;
    if (last > 0 || 0== sb.length()) {
        sb.append(last).append(s);
    }

    return sb.toString();
  }

  /**
   * 格式化时间
   * 用 Core 中当前时区和区域
   * 且 Core 中将暂存格式对象
   * @param time 时间戳(毫秒)
   * @param patt
   * @return
   * @throws IllegalArgumentException 格式错误
   */
  public static String format(long time, String patt) {
    Instant          inst = Instant.ofEpochMilli(time);
    ZoneId            zid = Core.getZoneId();
    ZonedDateTime     zdt = inst.atZone(zid);
    DateTimeFormatter dtf = getDateTimeFormatter(patt);
    return dtf.format(zdt);
  }

  /**
   * 格式化时间
   * @param time 时间戳(毫秒)
   * @param patt
   * @param loc
   * @param zid
   * @return
   * @throws IllegalArgumentException 格式错误
   */
  public static String format(long time, String patt, Locale loc, ZoneId zid) {
    Instant          inst = Instant.ofEpochMilli(time);
    ZonedDateTime     zdt = inst.atZone(zid);
    DateTimeFormatter dtf = Inst.newDateTimeFormatter(patt,loc);
    return dtf.format(zdt);
  }

  /**
   * 格式化时间
   * 用 Core 中当前时区和区域
   * 且 Core 中将暂存格式对象
   * @param time 旧版日期对象
   * @param patt
   * @return
   * @throws IllegalArgumentException 格式错误
   */
  public static String format(Date time, String patt) {
    Instant          inst = time.toInstant();
    ZoneId            zid = Core.getZoneId();
    ZonedDateTime     zdt = inst.atZone(zid);
    DateTimeFormatter dtf = getDateTimeFormatter(patt);
    return dtf.format(zdt);
  }

  /**
   * 格式化时间
   * @param time 旧版日期对象
   * @param patt
   * @param loc
   * @param zid
   * @return
   * @throws IllegalArgumentException 格式错误
   */
  public static String format(Date time, String patt, Locale loc, ZoneId zid) {
    Instant          inst = time.toInstant();
    ZonedDateTime     zdt = inst.atZone(zid);
    DateTimeFormatter dtf = Inst.newDateTimeFormatter(patt,loc);
    return dtf.format(zdt);
  }

  /**
   * 格式化时间
   * 用 Core 中当前时区和区域
   * 且 Core 中将暂存格式对象
   * @param time 新版时刻对象
   * @param patt
   * @return
   * @throws IllegalArgumentException 格式错误
   */
  public static String format(Instant time, String patt) {
    ZoneId            zid = Core.getZoneId();
    ZonedDateTime     zdt = time.atZone(zid);
    DateTimeFormatter dtf = getDateTimeFormatter(patt);
    return dtf.format(zdt);
  }

  /**
   * 格式化时间
   * @param time 新版时刻对象
   * @param patt
   * @param loc
   * @param zid
   * @return
   * @throws IllegalArgumentException 格式错误
   */
  public static String format(Instant time, String patt, Locale loc, ZoneId zid) {
    ZonedDateTime     zdt = time.atZone(zid);
    DateTimeFormatter dtf = Inst.newDateTimeFormatter(patt,loc);
    return dtf.format(zdt);
  }

  /**
   * 解析时间
   * 用 Core 中当前时区和区域
   * 且 Core 中将暂存格式对象
   * @param time
   * @param patt
   * @return
   * @throws IllegalArgumentException 格式错误
   * @throws java.time.format.DateTimeParseException 解析错误
   */
  public static Instant parse(String  time, String patt) {
    DateTimeFormatter dtf = getDateTimeFormatter(patt);
    TemporalAccessor  tar = dtf .parse(time);
    ZoneId            zid = Core.getZoneId();
    return fixZonedDateTime(tar , 1970, 1, 1, 0, 0, 0, 0, zid).toInstant();
  }

  /**
   * 解析时间
   * @param time
   * @param patt
   * @param loc
   * @param zid
   * @return
   * @throws IllegalArgumentException 格式错误
   * @throws java.time.format.DateTimeParseException 解析错误
   */
  public static Instant parse(String time, String patt, Locale loc, ZoneId zid) {
    DateTimeFormatter dtf = Inst.newDateTimeFormatter(patt,loc);
    TemporalAccessor  tar = dtf .parse(time);
    return fixZonedDateTime(tar , 1970, 1, 1, 0, 0, 0, 0, zid).toInstant();
  }

  /**
   * 获取格式
   * 用 Core 中当前时区和区域
   * 且 Core 中将暂存格式对象
   * @param patt
   * @return
   */
  public static DateTimeFormatter getDateTimeFormatter(String patt) {
    return Core.getInstance().got(
        DateTimeFormatter.class.getName() + ":" + patt,
        () -> newDateTimeFormatter(patt, Core.getLocale(), Core.getZoneId())
    );
  }

  /**
   * 构建格式
   * 解析宽松模式, 不区分大小写
   * @param patt
   * @param loc
   * @return
   */
  public static DateTimeFormatter newDateTimeFormatter(String patt, Locale loc) {
    return new DateTimeFormatterBuilder()
        .parseCaseInsensitive()
        .parseLenient  ( /**/ )
        .appendPattern ( patt )
        .toFormatter   ( loc  );
  }

  /**
   * 构建格式
   * 解析宽松模式, 不区分大小写
   * @param patt
   * @param loc
   * @param zid
   * @return
   */
  public static DateTimeFormatter newDateTimeFormatter(String patt, Locale loc, ZoneId zid) {
    return new DateTimeFormatterBuilder()
        .parseCaseInsensitive()
        .parseLenient  ( /**/ )
        .appendPattern ( patt )
        .toFormatter   ( loc  )
        .withZone      ( zid  );
  }

  /**
   * 补全数值
   * 
   * 不建议用, 如格式明确推荐用:
   * <code>
   * DateTimeFormatter dtf = DateTimeFormatterBuilder()
   *    .parseDefaulting(ChronoField.YEAR_OF_ERA, 1970)
   *    .parseDefaulting(ChronoField.MONTH_OF_YEAR, 1 )
   *    .parseDefaulting(ChronoField.DAY_OF_MONTH , 1 )
   *    .appendPattern("HH:mm:ss")
   *    .toFormatter(Locale.getDefault())
   *    . withZone  (ZoneId.of("UTC+8" ));
   * ZonedDateTime.parse("12:30:15", dtf)
   * </code>
   * 
   * @param ta
   * @param y
   * @param M
   * @param d
   * @param H
   * @param m
   * @param s
   * @param n
   * @param zi
   * @return 
   */
  private static ZonedDateTime fixZonedDateTime(TemporalAccessor ta, int y, int M, int d, int H, int m, int s, int n, ZoneId zi) {
    int[] ps = new int [] {y, M, d, H, m, s, n};
    for (int i = 0 ; i < 7 ; i ++ ) {
        if (ta.isSupported(TFS[i])) {
            ps[i] = ta.get(TFS[i]);
        }
    }
    return ZonedDateTime.of(ps[0], ps[1], ps[2], ps[3], ps[4], ps[5], ps[6], zi);
  }

  private static final TemporalField[] TFS = new TemporalField[] {
    ChronoField.  YEAR_OF_ERA   ,
    ChronoField. MONTH_OF_YEAR  ,
    ChronoField.   DAY_OF_MONTH ,
    ChronoField.  HOUR_OF_DAY   ,
    ChronoField.MINUTE_OF_HOUR  ,
    ChronoField.SECOND_OF_MINUTE,
    ChronoField.  NANO_OF_SECOND
  };

}
