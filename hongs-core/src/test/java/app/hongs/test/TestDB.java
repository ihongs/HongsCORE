package app.hongs.test;

import app.hongs.HongsException;
import app.hongs.db.util.FetchCase;
import static junit.framework.Assert.assertEquals;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 *
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
        FetchCase caze = new FetchCase();
        FetchCase copy ;
        String    vali ;

        caze.from   ("t1")
            .select ("f1,`f2`,.`f3` , t1.f4, t1.`f5`, `t1`.f6, `t1`.`f7`")
            .select ("CONCAT(f1, `f2`, .f3, t1.f4, tf.`f5`, 'abc') AS c3")
            .select ("COUNT(!*) AS c1, COUNT(DISTINCT *) c2 ")
            .select ("'abc' AS x1, 'def' x2")
            .join   ("t2")
            .by     ( FetchCase.INNER )
            .on     ("`t1_id` = :`id`")
            .in     ("") // 追加别名
            .select ("CONCAT(f1, f2, f3) AS v0, 'abc.def 0.15 .xy' AS x0")
            .select ("(1.2 + f1) v1, 3.4 * f2 AS `v2`, 5.6e10 / .f3 AS !`v3`")
            .filter ("f4 = ? AND .f5 IN (?) AND `f6` like ?", 123, 456, "abc")
            .orderBy("`f1`, f2 DESC, !v1 ASC, !`v2` DESC");
        vali = "SELECT `t1`.f1,`t1`.`f2`,`t1`.`f3` , t1.f4, t1.`f5`, `t1`.f6, `t1`.`f7`, CONCAT(`t1`.f1, `t1`.`f2`, `t1`.f3, t1.f4, tf.`f5`, 'abc') AS c3, COUNT(*) AS c1, COUNT(DISTINCT `t1`.*) c2 , 'abc' AS x1, 'def' x2 , CONCAT(`t2`.f1, `t2`.f2, `t2`.f3) AS `t2.v0`, 'abc.def 0.15 .xy' AS `t2.x0`, (1.2 + `t2`.f1) `t2.v1`, 3.4 * `t2`.f2 AS `t2.v2`, 5.6e10 / `t2`.f3 AS `t2.v3` FROM `t1` INNER JOIN `t2` ON `t2`.`t1_id` = `t1`.`id` WHERE `t2`.f4 = 123 AND `t2`.f5 IN (456) AND `t2`.`f6` like 'abc' ORDER BY `t2`.`f1`, `t2`.f2 DESC, v1 ASC, `v2` DESC";
        System.out.println(vali);
        System.out.println(caze.toString());
        assertEquals(vali, caze.toString());

        copy = caze.clone();
        copy.gotJoin("t2" ).in( null    );
        vali = "SELECT `t1`.f1,`t1`.`f2`,`t1`.`f3` , t1.f4, t1.`f5`, `t1`.f6, `t1`.`f7`, CONCAT(`t1`.f1, `t1`.`f2`, `t1`.f3, t1.f4, tf.`f5`, 'abc') AS c3, COUNT(*) AS c1, COUNT(DISTINCT `t1`.*) c2 , 'abc' AS x1, 'def' x2 , CONCAT(`t2`.f1, `t2`.f2, `t2`.f3) AS v0, 'abc.def 0.15 .xy' AS x0, (1.2 + `t2`.f1) v1, 3.4 * `t2`.f2 AS `v2`, 5.6e10 / `t2`.f3 AS `v3` FROM `t1` INNER JOIN `t2` ON `t2`.`t1_id` = `t1`.`id` WHERE `t2`.f4 = 123 AND `t2`.f5 IN (456) AND `t2`.`f6` like 'abc' ORDER BY `t2`.`f1`, `t2`.f2 DESC, v1 ASC, `v2` DESC";
        System.out.println(vali);
        System.out.println(copy.toString());
        assertEquals(vali, copy.toString());

        copy = caze.clone();
        copy.gotJoin("t2" ).by((byte) 0 );
        vali = "SELECT `t1`.f1,`t1`.`f2`,`t1`.`f3` , t1.f4, t1.`f5`, `t1`.f6, `t1`.`f7`, CONCAT(`t1`.f1, `t1`.`f2`, `t1`.f3, t1.f4, tf.`f5`, 'abc') AS c3, COUNT(*) AS c1, COUNT(DISTINCT `t1`.*) c2 , 'abc' AS x1, 'def' x2 FROM `t1`";
        System.out.println(vali);
        System.out.println(copy.toString());
        assertEquals(vali, copy.toString());

        copy = caze.gotJoin("t2").clone();
        vali = "SELECT CONCAT(f1, f2, f3) AS v0, 'abc.def 0.15 .xy' AS x0, (1.2 + f1) v1, 3.4 * f2 AS `v2`, 5.6e10 / f3 AS `v3` FROM `t2` WHERE f4 = 123 AND f5 IN (456) AND `f6` like 'abc' ORDER BY `f1`, f2 DESC, v1 ASC, `v2` DESC";
        System.out.println(vali);
        System.out.println(copy.toString());
        assertEquals(vali, copy.toString());

        caze = new FetchCase();
        caze.from   ("a_member_user", "user")
            .filter ("state != 1 AND dept_id IN (?)", 1)
            .join   ("a_member_user_dept", "depts")
            .on     ("user_id = :user_id")
            .by     (FetchCase.LEFT)
            .filter ("dept_id IN (?)", 10);
        vali = "SELECT `user`.* FROM `a_member_user` AS `user` LEFT JOIN `a_member_user_dept` AS `depts` ON `depts`.user_id = `user`.user_id WHERE `user`.state != 1 AND `user`.dept_id IN (1) AND `depts`.dept_id IN (10)";
        System.out.println(vali);
        System.out.println(caze.toString());
        assertEquals(vali, caze.toString());
    }

}
