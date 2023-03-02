package io.github.ihongs.test;

import io.github.ihongs.util.Dusk;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import junit.framework.TestCase;
import static junit.framework.TestCase.assertEquals;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * 测试 io.github.ihongs.util.Dusk
 * @author Hongs
 */
public class TestUtilDusk extends TestCase {

    public TestUtilDusk() {

    }

    @BeforeClass
    public static void setUpClass() {
    }

    @AfterClass
    public static void tearDownClass() {
    }

    @Before
    @Override
    public void setUp() {
    }

    @After
    @Override
    public void tearDown() {
    }

    @Test
    public void testFormat() {
        long   tm = 1677329056750L;
        String dt = "2023/02/25 20:44:16.750";
        String df = "yyyy/MM/dd HH:mm:ss.SSS";
        assertEquals(dt, Dusk.formatTime(tm, df, Locale.PRC, ZoneId.of("GMT+8")));
        assertEquals(tm, Dusk. parseTime(dt, df, Locale.PRC, ZoneId.of("GMT+8")).toEpochMilli());

        // 解析时间
        DateTimeFormatter tf = DateTimeFormatter.ofPattern("H:m", Locale.US);
        assertEquals(   900, LocalTime.parse("0:15", tf).atDate(LocalDate.EPOCH).atZone(ZoneId.of("UTC"  )).toInstant().toEpochMilli() / 1000);
        assertEquals(   900, Dusk. parseTime("0:15", "H:m", Locale.US, ZoneId.of("UTC"  )).toEpochMilli() / 1000);
        assertEquals(-27900, LocalTime.parse("0:15", tf).atDate(LocalDate.EPOCH).atZone(ZoneId.of("GMT+8")).toInstant().toEpochMilli() / 1000);
        assertEquals(-27900, Dusk. parseTime("0:15", "H:m", Locale.US, ZoneId.of("GMT+8")).toEpochMilli() / 1000);

        // 解析日期
        tf = DateTimeFormatter.ofPattern("yyyy/MM/dd");
        assertEquals(1677283200, LocalDate.parse("2023/02/25", tf).atTime(LocalTime.MIDNIGHT).atZone(ZoneId.of("UTC"  )).toInstant().toEpochMilli() / 1000);
        assertEquals(1677283200, Dusk. parseTime("2023/2/25" , "yyyy/MM/dd", Locale.US, ZoneId.of("UTC"  )).toEpochMilli() / 1000);
        assertEquals(1677254400, LocalDate.parse("2023/02/25", tf).atTime(LocalTime.MIDNIGHT).atZone(ZoneId.of("GMT+8")).toInstant().toEpochMilli() / 1000);
        assertEquals(1677254400, Dusk. parseTime("2023/2/25" , "yyyy/MM/dd", Locale.US, ZoneId.of("GMT+8")).toEpochMilli() / 1000);
    }

}
