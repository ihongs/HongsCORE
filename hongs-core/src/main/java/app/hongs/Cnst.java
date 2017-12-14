package app.hongs;

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
 *
 * 查询关系符号为叹号加两个字符,
 * 叹号在这里表示特别而不是否定.
 *
 * 用叹号而不是 .: 等符号, 因为:
 * URL 里只有 .!_-~*() 无需转义.
 * 符号 . 被用作键分隔符,
 * 符号 : 按规定需要转义,
 * 其他符号都有其他用途或不好看 ^_^
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

    public static final String WD_KEY =  "wd"; // 关键词    (Word)

    public static final String PN_KEY =  "pn"; // 页码编号  (Page num)

    public static final String GN_KEY =  "gn"; // 分页数量  (Pags num)

    public static final String RN_KEY =  "rn"; // 每页行数  (Rows num)

    public static final String OB_KEY =  "ob"; // 排序字段  (Order By)

    public static final String RB_KEY =  "rb"; // 应答字段  (Reply By)

    public static final String AB_KEY =  "ab"; // 应用约束  (Apply By)

    public static final String CB_KEY =  "cb"; // 回调名称  (Callback)

    public static final String WR_KEY =  "wr"; // 附加约束  (Where)

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

    public static final String RG_REL = "!rg"; // 区间

    public static final String IR_REL = "!ir"; // 区间集

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

    public static final String ACT_EXT  = ".act";

    public static final String API_EXT  = ".api";

    //** 会话参数 **/

    public static final String  UID_SES =  "uid";

    public static final String  UST_SES =  "ust"; // 最后访问时间

    public static final String  USL_SES =  "usl"; // 已登录的区域

    public static final String  ADM_UID =  "1";

    public static final String  ADM_GID =  "0";

    //** 启动配置 **/

    public static final String INIT_NAME = "defines"; // 启动配置名称

    //** 请求属性 **/

    public static final String DATA_ATTR = "__HONGS_DATA__"; // 请求数据

    public static final String RESP_ATTR = "__HONGS_RESP__"; // 响应数据

    public static final String ACTION_ATTR = "__ACTION_NAME__"; // 动作名称

    public static final String ORIGIN_ATTR = "__ORIGIN_NAME__"; // 起源动作

    public static final String CLIENT_ATTR = "__CLIENT_ADDR__"; // 客户地址

    public static final String UPDATE_ATTR = "__UPDATE_TIME__"; // 更新时间(当会话或属性改变时将被设置)

    public static final String UPLOAD_ATTR = "__UPLOAD_PART__"; // 上传参数(用于存放哪些参数是文件上传)

    //** 数据模式 **/

    public static final String OBJECT_MODE = "__OBJECT_MODE__"; // 对象模式

    public static final String TRNSCT_MODE = "__TRNSCT_MODE__"; // 事务模式

}
