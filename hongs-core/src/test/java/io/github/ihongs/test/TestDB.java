package io.github.ihongs.test;

import io.github.ihongs.HongsException;
import io.github.ihongs.db.util.FetchCase;
import static junit.framework.Assert.fail;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * 测试数据库工具
 * @author Hongs
 */
public class TestDB {

    public TestDB() {
    }

    @BeforeClass
    public static void setUpClass() {
    }

    @AfterClass
    public static void tearDownClass() {
    }

    @Before
    public void setUp() {
    }

    @After
    public void tearDown() {
    }

    @Test
    public void testFetchCase() throws HongsException {
        FetchCase caze = new FetchCase(FetchCase.CLEVER);
        FetchCase copy ;
        String    vali ;

        caze.from   ("table1", "t1")
            .select ("f1, `f2` , t1.f3, t1.`f4` , `t1`.f5, `t1`.`f6`")
            .select ("CONCAT(f1, `f2`, t1.f3, 12.3, 'ab.c', COUNT(*), COUNT(DISTINCT f1)) AS c1")
            .select ("'xyz' AS a1, 45.6 AS !a2, 78.9 AS !`a3`")
            .filter ("f1 = ? AND f2 IN (?) AND `f3` LIKE ?", 123, 456, "abc")
            .assort ("`f1`, f2 DESC, !a2 ASC, !`a3` DESC")
            .join   ("table2", "t2").on("`t1_id` = :`id`")
            .select ("f1, `f2` , t2.f3, t2.`f4` , `t2`.f5, `t2`.`f6`")
            .select ("CONCAT(f1, `f2`, t2.f3, 12.3, 'ab.c', COUNT(*), COUNT(DISTINCT f1)) AS c1")
            .select ("'xyz' AS a1, 45.6 AS !a2, 78.9 AS !`a3`")
            .filter ("f1 = ? AND f2 IN (?) AND `f3` LIKE ?", 123, 456, "abc")
            .assort ("`f1`, f2 DESC, !a2 ASC, !`a3` DESC");
        vali = "SELECT `t1`.f1, `t1`.`f2` , t1.f3, t1.`f4` , `t1`.f5, `t1`.`f6`, CONCAT(`t1`.f1, `t1`.`f2`, t1.f3, 12.3, 'ab.c', COUNT(*), COUNT(DISTINCT `t1`.f1)) AS c1, 'xyz' AS a1, 45.6 AS a2, 78.9 AS `a3` , `t2`.`f1` AS `t2.f1`, `t2`.`f2` AS `t2.f2` , t2.`f3` AS `t2.f3`, t2.`f4` AS `t2.f4` , `t2`.`f5` AS `t2.f5`, `t2`.`f6` AS `t2.f6`, CONCAT(`t2`.f1, `t2`.`f2`, t2.f3, 12.3, 'ab.c', COUNT(*), COUNT(DISTINCT `t2`.f1)) AS `t2.c1`, 'xyz' AS `t2.a1`, 45.6 AS a2, 78.9 AS `a3` FROM `table1` AS `t1` INNER JOIN `table2` AS `t2` ON `t2`.`t1_id` = `t1`.`id` WHERE `t1`.f1 = 123 AND `t1`.f2 IN (456) AND `t1`.`f3` LIKE 'abc' AND `t2`.f1 = 123 AND `t2`.f2 IN (456) AND `t2`.`f3` LIKE 'abc' ORDER BY `t1`.`f1`, `t1`.f2 DESC, a2 ASC, `a3` DESC , `t2`.`f1`, `t2`.f2 DESC, a2 ASC, `a3` DESC";
        if (! vali.equals(caze.toString())) {
            fail("构建查询语句错误\r\n\t目标: "+vali+"\r\n\t实际: "+caze.toString());
        }

        copy = caze.clone();
        copy.gotJoin("t2" ).in(null);
        vali = "SELECT `t1`.f1, `t1`.`f2` , t1.f3, t1.`f4` , `t1`.f5, `t1`.`f6`, CONCAT(`t1`.f1, `t1`.`f2`, t1.f3, 12.3, 'ab.c', COUNT(*), COUNT(DISTINCT `t1`.f1)) AS c1, 'xyz' AS a1, 45.6 AS a2, 78.9 AS `a3` , `t2`.f1, `t2`.`f2` , t2.f3, t2.`f4` , `t2`.f5, `t2`.`f6`, CONCAT(`t2`.f1, `t2`.`f2`, t2.f3, 12.3, 'ab.c', COUNT(*), COUNT(DISTINCT `t2`.f1)) AS c1, 'xyz' AS a1, 45.6 AS a2, 78.9 AS `a3` FROM `table1` AS `t1` INNER JOIN `table2` AS `t2` ON `t2`.`t1_id` = `t1`.`id` WHERE `t1`.f1 = 123 AND `t1`.f2 IN (456) AND `t1`.`f3` LIKE 'abc' AND `t2`.f1 = 123 AND `t2`.f2 IN (456) AND `t2`.`f3` LIKE 'abc' ORDER BY `t1`.`f1`, `t1`.f2 DESC, a2 ASC, `a3` DESC , `t2`.`f1`, `t2`.f2 DESC, a2 ASC, `a3` DESC";
        if (! vali.equals(copy.toString())) {
            fail("取消查询层名异常\r\n\t目标: "+vali+"\r\n\t实际: "+caze.toString());
        }

        copy = caze.clone();
        copy.gotJoin("t2" ).by(FetchCase.NONE);
        vali = "SELECT `t1`.f1, `t1`.`f2` , t1.f3, t1.`f4` , `t1`.f5, `t1`.`f6`, CONCAT(`t1`.f1, `t1`.`f2`, t1.f3, 12.3, 'ab.c', COUNT(*), COUNT(DISTINCT `t1`.f1)) AS c1, 'xyz' AS a1, 45.6 AS a2, 78.9 AS `a3` FROM `table1` AS `t1` WHERE `t1`.f1 = 123 AND `t1`.f2 IN (456) AND `t1`.`f3` LIKE 'abc' ORDER BY `t1`.`f1`, `t1`.f2 DESC, a2 ASC, `a3` DESC";
        if (! vali.equals(copy.toString())) {
            fail("移除查询层级异常\r\n\t目标: "+vali+"\r\n\t实际: "+caze.toString());
        }

        copy = caze.gotJoin("t2").clone();
        vali = "SELECT f1, `f2` , t2.f3, t2.`f4` , `t2`.f5, `t2`.`f6`, CONCAT(f1, `f2`, t2.f3, 12.3, 'ab.c', COUNT(*), COUNT(DISTINCT f1)) AS c1, 'xyz' AS a1, 45.6 AS a2, 78.9 AS `a3` FROM `table2` AS `t2` WHERE f1 = 123 AND f2 IN (456) AND `f3` LIKE 'abc' ORDER BY `f1`, f2 DESC, a2 ASC, `a3` DESC";
        if (! vali.equals(copy.toString())) {
            fail("提取下级查询异常\r\n\t目标: "+vali+"\r\n\t实际: "+caze.toString());
        }
        
        caze = new FetchCase(FetchCase.CLEVER);
        caze.from   ("xxx", "x")
            .join   ("yyy", "y", "xxx_id = :id")
            .filter ("name = \"Hong's\" AND age = '30'");
        vali = "SELECT `x`.* FROM `xxx` AS `x` INNER JOIN `yyy` AS `y` ON `y`.xxx_id = `x`.id WHERE `x`.name = \"Hong's\" AND `x`.age = '30'";
        if (! vali.equals(caze.toString())) {
            fail("智能模式构建异常\r\n\t目标: "+vali+"\r\n\t实际: "+caze.toString());
        }
        
        caze = new FetchCase(FetchCase.CLEVER);
        caze.from   ("a_master_user", "user")
            .filter ("state != 1 AND dept_id IN (?)", 1)
            .join   ("a_master_dept_user", "depts")
            .on     ("user_id = :user_id")
            .by     (FetchCase.LEFT)
            .filter ("dept_id IN (?)", 10);
        vali = "SELECT `user`.* FROM `a_master_user` AS `user` LEFT JOIN `a_master_dept_user` AS `depts` ON `depts`.user_id = `user`.user_id WHERE `user`.state != 1 AND `user`.dept_id IN (1) AND `depts`.dept_id IN (10)";
        if (! vali.equals(caze.toString())) {
            fail("实例复合查询异常\r\n\t目标: "+vali+"\r\n\t实际: "+caze.toString());
        }
    }

}
