package io.github.ihongs;

/**
 * 常量
 *
 * <p>
 * 命名规律:
 *
 * 特殊查询参数都是使用两个字符;
 * 因模型中会将参数作为字段过滤,
 * 故请避免将字段取名为两个字符.
 * 但 id 可以并推荐作为主键字段,
 * 而 wd 亦可按需作专用搜索字段.
 * </p>
 *
 * @author Hongs
 */
public final class Cnst {

    //** 默认数值 **/

    public static final int    GN_DEF =    5 ; // 分页默认数量

    public static final int    RN_DEF =   20 ; // 每页默认行数

    public static final int    CL_DEF =   -1 ; // 默认生命周期(秒, Cookie)

    //** 查询参数 **/

    public static final String ID_KEY =  "id"; // 编号

    public static final String WD_KEY =  "wd"; // 查询      (Word)

    public static final String PN_KEY =  "pn"; // 页码编号  (Page num)

    public static final String GN_KEY =  "gn"; // 最多页数  (Pugs num)

    public static final String RN_KEY =  "rn"; // 每页行数  (Rows num)

    public static final String OB_KEY =  "ob"; // 排序字段  (Order by)

    public static final String RB_KEY =  "rb"; // 应答字段  (Reply with)

    public static final String AB_KEY =  "ab"; // 应用约束  (Apply with)

    public static final String CB_KEY =  "cb"; // 回调名称  (Callback)

    public static final String OR_KEY =  "or"; // 或条件    (Or )

    public static final String AR_KEY =  "ar"; // 与条件    (And)

    public static final String SR_KEY =  "sr"; // 可条件    (Lucene 特有)

    //** 关系符号 **/

    public static final String IS_REL = ":is"; // 是否为空  (NULL , WELL)

    public static final String EQ_REL = ":eq"; // 等于

    public static final String NE_REL = ":ne"; // 不等于

    public static final String CQ_REL = ":cq"; // 匹配

    public static final String NC_REL = ":nc"; // 不匹配

    public static final String LT_REL = ":lt"; // 小于

    public static final String LE_REL = ":le"; // 小于或等于

    public static final String GT_REL = ":gt"; // 大于

    public static final String GE_REL = ":ge"; // 大于或等于

    public static final String RN_REL = ":rn"; // 区间

    public static final String ON_REL = ":on"; // 多区间

    public static final String IN_REL = ":in"; // 包含

    public static final String NI_REL = ":ni"; // 不包含

    public static final String AI_REL = ":ai"; // 全包含    (Lucene 特有, all in)

    public static final String SI_REL = ":si"; // 可包含    (Lucene 特有, may in)

    public static final String SE_REL = ":se"; // 可等于    (Lucene 特有, may be)

    public static final String SC_REL = ":sc"; // 可匹配    (Lucene 特有, may be)

    public static final String WT_REL = ":wt"; // 权重      (Lucene 特有, weight)

    //** 配置扩展 **/

    public static final String DB_EXT = ".db"; // 数据库配置

    public static final String DF_EXT = ".df"; // 表字段缓存

    public static final String FORM_EXT = ".form"; // 表单配置

    public static final String NAVI_EXT = ".navi"; // 导航配置

    public static final String PROP_EXT = ".prop"; // 属性配置

    public static final String  ACT_EXT = ".act";

    public static final String  API_EXT = ".api";

    //** 会话参数 **/

    public static final String  UID_SES =  "uid"; // 用户代号

    public static final String  UST_SES =  "ust"; // 登录时间

    public static final String  ADM_UID =  "1";   // 超级管理员

    public static final String  ADM_GID =  "0";   // 顶级管理组

    //** 请求属性 **/

    public static final String ACTION_ATTR = "__ACTION_NAME__"; // 动作名称

    public static final String ORIGIN_ATTR = "__ORIGIN_NAME__"; // 起源动作

    public static final String SCHEME_ATTR = "__SCHEME_HOST__"; // 服务前缀

    public static final String CLIENT_ATTR = "__CLIENT_ADDR__"; // 客户地址

    public static final String UPDATE_ATTR = "__UPDATE_TIME__"; // 更新时间(当会话或属性改变时将被设置)

    public static final String UPLOAD_ATTR = "__UPLOAD_PART__"; // 上传参数(用于存放哪些参数是文件上传)

    public static final String REQUES_ATTR = "__REQUES_DATA__"; // 请求数据

    public static final String RESPON_ATTR = "__RESPON_DATA__"; // 响应数据

    //** 数据模式 **/

    public static final String OBJECT_MODE = "__OBJECT_MODE__"; // 对象模式

    public static final String TRNSCT_MODE = "__TRNSCT_MODE__"; // 事务模式

    public static final String UPDATE_MODE = "__UPDATE_MODE__"; // 增补模式

    //** 系统路径 **/

    public static final String CONF_PACK = "io/github/ihongs/config";

}
