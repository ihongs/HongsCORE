package app.hongs;

/**
 * 常量
 * @author Hongs
 */
public class Cnst {

    //** 默认数值 **/

    public static final int    GN_DEF =    5 ; // 分页默认数量

    public static final int    RN_DEF =   20 ; // 每页默认行数

    public static final int    CL_DEF =   -1 ; // 默认生命周期(秒, Cookie)

    //** 查询参数 **/

    public static final String ID_KEY =  "id"; // 编号参数

    public static final String MD_KEY =  "md"; // 模式参数  (Mode)

    public static final String UD_KEY =  "ud"; // 常用参数  (Used)

    public static final String WD_KEY =  "wd"; // 关键词    (Word)

    public static final String WH_KEY =  "wh"; // 变更约束  (Where)

    public static final String PN_KEY =  "pn"; // 页码编号  (Page num)

    public static final String GN_KEY =  "gn"; // 分页数量  (Pags num)

    public static final String RN_KEY =  "rn"; // 每页行数  (Rows num)

    public static final String OB_KEY =  "ob"; // 排序字段  (Order By)

    public static final String RB_KEY =  "rb"; // 应答字段  (Reply By)

    public static final String OR_KEY =  "or"; // 或关系    (Or)

    public static final String AR_KEY =  "ar"; // 与或      (And Or)

    public static final String SR_KEY =  "sr"; // 可或      (Lucene 特有)

    //** 关系符号 **/

    public static final String EQ_REL = "!eq"; // 等于

    public static final String NE_REL = "!ne"; // 不等于

    public static final String LT_REL = "!lt"; // 小于

    public static final String LE_REL = "!le"; // 小于或等于

    public static final String GT_REL = "!gt"; // 大于

    public static final String GE_REL = "!ge"; // 大于或等于

    public static final String IN_REL = "!in"; // 包含

    public static final String NI_REL = "!ni"; // 不包含    (Not in)

    public static final String AI_REL = "!ai"; // 全包含    (All in, Lucene 特有)

    public static final String OI_REL = "!oi"; // 或包含    (Or  in, Lucene 特有)

    public static final String OR_REL = "!or"; // 或等于    (Or    , Lucene 特有)

    public static final String WT_REL = "!wt"; // 权重      (        Lucene 特有)

    //** 配置扩展 **/

    public static final String DB_EXT = ".db"; // 数据库配置

    public static final String DF_EXT = ".df"; // 表字段缓存

    public static final String FORM_EXT = ".form"; // 表单配置

    public static final String NAVI_EXT = ".navi"; // 导航配置

    public static final String PROP_EXT = ".prop"; // 属性配置
    
    //** 会话参数 **/

    public static final String  UID_SES =  "uid";

    public static final String  UST_SES =  "ust"; // 最后访问时间

    public static final String  USL_SES =  "usl"; // 已登录的区域

    public static final String  ADM_UID =  "1";

    public static final String  ADM_GID =  "0";

//  public static final String CSID_KEY =  "_";

//  public static final String PSID_KEY =  "_";

    //** 请求属性 **/

    public static final String CORE_ATTR = "__HONGS_CORE__"; // 核心对象

    public static final String PATH_ATTR = "__HONGS_PATH__"; // 请求路径

    public static final String RESP_ATTR = "__HONGS_RESP__"; // 返回数据

    public static final String BACK_ATTR = "__HONGS_BACK__"; // 回调名称

    public static final String RUNNER_ATTR = "__RUNNER__"; // 动作执行器

    public static final String UPLOAD_ATTR = "__UPLOAD__"; // 上传参数键

    public static final String UPDATE_ATTR = "__UPDATE__"; // 更新时间戳(当会话、属性改变时设置)

    //** 数据模式 **/

    public static final String OBJECT_MODE = "__IN_OBJECT_MODE__"; // 对象模式

    public static final String TRNSCT_MODE = "__IN_TRNSCT_MODE__"; // 事务模式

}
