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
 * 但 id 可以并推荐作为主键字段.
 * </p>
 *
 * @author Hongs
 */
public final class Cnst {

    public static final int    RN_DEF =  20 ; // 默认每页行数

    @Deprecated
    public static final int    PN_DEF =   1 ;

    @Deprecated
    public static final int    QN_DEF =   0 ;

    //** 查询参数 **/

    public static final String ID_KEY = "id"; // 编号

    public static final String WD_KEY = "wd"; // 查询      (Word)

    public static final String CB_KEY = "cb"; // 回调名称  (Callback)

    public static final String RN_KEY = "rn"; // 每页行数  (Rows num)

    public static final String PN_KEY = "pn"; // 页码编号  (Page num)

    public static final String QN_KEY = "qn"; // 探测页数  (Quiz num)

    public static final String OB_KEY = "ob"; // 排序字段  (Order by)

    public static final String RB_KEY = "rb"; // 应答字段  (Reply with)

    public static final String AB_KEY = "ab"; // 应用约束  (Apply with)

    public static final String OR_KEY = "or"; // 或条件    (Or )

    public static final String AR_KEY = "ar"; // 与条件    (And)

    public static final String NR_KEY = "nr"; // 否条件    (Not)

    public static final String SR_KEY = "sr"; // 优先条件  (Should, Lucene 加权)

    //** 关系符号 **/

    public static final String EQ_REL = "eq"; // 等于

    public static final String NE_REL = "ne"; // 不等于

    public static final String LT_REL = "lt"; // 小于

    public static final String LE_REL = "le"; // 小于或等于

    public static final String GT_REL = "gt"; // 大于

    public static final String GE_REL = "ge"; // 大于或等于

    public static final String AT_REL = "at"; // 区间

    public static final String IS_REL = "is"; // 是否为空  (取值: none, not-none, null, not-null, empty, not-empty)

    public static final String IN_REL = "in"; // 包含

    public static final String NO_REL = "no"; // 不包含

    public static final String ON_REL = "on"; // 全包含

    public static final String SA_REL = "sa"; // 搜索参数  (格式: "mode,a1,a2,a3", mode 总在首位, 后面参数顺序无关)

    public static final String SE_REL = "se"; // 搜索匹配

    public static final String NS_REL = "ns"; // 搜索排除

    public static final String UP_REL = "up"; // 加权

    //** 配置扩展 **/

    public static final String  ACT_EXT = ".act";  // 动作后缀

    public static final String  API_EXT = ".api";  // 接口后缀

    public static final String   DB_EXT = ".db"; // 数据库配置

    public static final String   DF_EXT = ".df"; // 表字段缓存

    public static final String FORM_EXT = ".form"; // 表单配置

    public static final String NAVI_EXT = ".navi"; // 导航配置

    public static final String PROP_EXT = ".prop"; // 属性配置

    //** 会话参数 **/

    public static final String LANG_DEF = "zh_CN"; // 默认语言

    public static final String ZONE_DEF = "UTC+8"; // 默认时区

    public static final String LANG_KEY =  "lang"; // 语言代号

    public static final String ZONE_KEY =  "zone"; // 时区代号

    public static final String  SID_KEY =  "sid";  // 会话代号

    public static final String  UID_SES =  "uid";  // 用户代号

    public static final String  UST_SES =  "ust";  // 登录时间

    public static final String  USK_SES =  "usk";  // 登录模式

    public static final String  ADM_UID =  "1";    // 超级用户

    public static final String  GST_UID =  "0";    // 访客用户

    public static final String  TOP_GID =  "0";    // 顶层用户组

    //** 请求属性 **/

    public static final String ACTION_ATTR = "__ACTION_NAME__"; // 动作名称

    public static final String SERVER_ATTR = "__SERVER_HREF__"; // 服务前缀

    public static final String CLIENT_ATTR = "__CLIENT_ADDR__"; // 客户地址

    public static final String UPDATE_ATTR = "__UPDATE_TIME__"; // 更新时间(当会话或属性改变时将被设置)

    public static final String UPLOAD_ATTR = "__UPLOAD_PART__"; // 上传参数(用于存放哪些参数是文件上传)

    public static final String REQUES_ATTR = "__REQUES_DATA__"; // 请求数据

    public static final String RESPON_ATTR = "__RESPON_DATA__"; // 响应数据

    //** 全局模式 **/

    public static final String REFLUX_MODE = "__REFLUX_MODE__"; // 事务模式

    public static final String UPDATE_MODE = "__UPDATE_MODE__"; // 增补模式

    //** 系统路径 **/

    public static final String CONF_PACK = "io/github/ihongs/config";

    public static final String  LOG_NAME = "hongs.log";

    public static final String  OUT_NAME = "hongs.out";

}
