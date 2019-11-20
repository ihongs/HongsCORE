//** 演示配置 **/
if(!window.HsCONF)window.HsCONF={};$.extend(window.HsCONF,{
	"DEBUG":3,
	"SERVER_ID":"0",
	"BASE_HREF":".",
	"pn.key":"pn",
	"gn.key":"gn",
	"rn.key":"rn",
	"ob.key":"ob",
	"rb.key":"rb",
	"pugs.for.page":5,
	"rows.per.page":20,
	"tree.root.id":"0",
	"tree.pid.key":"pid",
	"tree.bid.key":"bid",
	"":""
});

//** 演示语言 **/
if(!window.HsLANG)window.HsLANG={};$.extend(window.HsLANG,{
	"lang":"zh_CN",
	"zone":"GMT-8",
	"error.label":"错误($0)",
	"error.unkwn":"未知错误",
	"datetime.format":"yyyy/MM/dd HH:mm:ss",
	"date.format":"yyyy/MM/dd",
	"time.format":"HH:mm:ss",

	"loading":"加载中...",
	"opening":"打开中...",
	"ensure":"确定",
	"cancel":"取消",
	"commit":"提交",
	"create":"创建",
	"update":"修改",
	"delete":"删除",
	"search":"查找",
	"select":"确定选择",
	"create.title":"创建${0}",
	"update.title":"修改${0}",
	"select.title":"选择${0}",
	"create.success":"创建${0}成功！",
	"update.success":"成功更新了${1}个${0}！",
	"delete.success":"成功删除了${1}个${0}！",
	"delete.confirm":"您确定要删除这个(些){$0}？",

	"list.title":"${0}列表",
	"list.empty":"列表为空, 请添加数据或改变条件",
	"list.above":"分页错误, 即将转到第一页",
	"list.get.all":"请选择一行或多行",
	"list.get.one":"请选择一行",
	"list.page.info": "共 ${count} 条, 分 ${pages} 页",
	"list.page.unfo": "超 ${count} 条, 分 ${pages} 页",
	"list.prev.pagi":"上一页, 双击回首页",
	"list.next.pagi":"下一页, 双击有惊喜",
	"list.prev.page":"上一页",
	"list.next.page":"下一页",
	"tree.root.name":"根节点",
	"tree.root.note":"根节点",

	"form.create":"创建",
	"form.update":"修改",
	"form.sending":"保存中...",
	"form.invalid":"操作失败, 请查看错误信息并修正后重试",
	"form.haserror":"输入错误",
	"form.required":"必须填写",
	"form.requires":"必须选择",
	"form.repeated":"必须为数组",
	"form.gt.max":"不得大于$0",
	"form.lt.min":"不得小于$0",
	"form.gt.maxdate":"不得在$0之后",
	"form.lt.mindate":"不得在$0之前",
	"form.gt.maxlength":"长度不得大于$0",
	"form.lt.minlength":"长度不得小于$0",
	"form.gt.maxrepeat":"数量不得大于$0",
	"form.lt.minrepeat":"数量不得小于$0",
	"form.is.not.int":"不是整型",
	"form.is.not.long":"不是长整型",
	"form.is.not.float":"不是浮点型",
	"form.is.not.double":"不是双精度",
	"form.is.not.number":"必须是数字",
	"form.is.not.date":"请使用格式 年/月/日",
	"form.is.not.time":"请使用格式 时:分:秒",
	"form.is.not.datetime":"请使用格式 年/月/日 时:分:秒",
	"form.is.not.url":"不是正确的链接地址",
	"form.is.not.tel":"不是正确的电话号码",
	"form.is.not.email":"不是正确的Email",
	"form.is.not.match":"输入验证错误",
	"form.not.in.enum":"值不在选择列表中",
	"form.is.not.repeat":"请重复填写",
	"form.is.not.exists":"记录不存在",
	"form.is.not.unique":"记录已存在",
	"fork.unpicked":"您什么也没选",
	"pick.unpicked":"您什么也没选",

	"date.LM":["一月","二月","三月","四月","五月","六月","七月","八月","九月","十月","十一月", "十二月"],
	"date.SM":["一","二","三","四","五","六","七","八","九","十","十一","十二"],
	"date.LE":["星期日","星期一","星期二","星期三","星期四","星期五","星期六"],
	"date.SE":["日","一","二","三","四","五","六"],
	"time.La":["上午","下午"],
	"time.Sa":["AM","PM"],
	"date.today":"今天",
	"time.today":"${0}",
	"week.start":0,

	"file.browse":"选择...",
	"file.upload":"上传",
	"file.cancel":"取消",
	"file.remove":"删除",
	"file.single":"文件",
	"file.plural":"文件",
	"file.upload.title":"上传文件",
	"file.cancel.title":"取消上传",
	"file.remove.title":"删除文件",
	"file.drop.to.here":"请将文件拖拽到此处...",
	"file.invalid.type":"不支持 {name} 的类型, 仅能上传类型为 <b>{types}</b> 的文件",
	"file.invalid.extn":"不支持 {name} 的类型, 仅能上传类型为 <b>{extensions}</b> 的文件",
	"file.invalid.size":"文件 {name} (<b>{size} KB</b>) 太大, 不得超过 <b>{maxSize} KB</b>",

	"welcome0":"您好!",
	"welcome1":"欢迎来到",
	"welcome2":"管理信息系统, 请登录...",

	"":""
});

//** 演示处理 **/
(function($) {
    var j = 0;
    $(document).on("loadBack", ".HsTree", function(evt, rst) {
        for(var i = 0; i < rst.list.length; i ++) {
            rst.list[i].id = j ++;
        }
    });
    $(document).on("loadBack", ".HsForm", function(evt, rst) {
        var box = $( this ).closest (".loadbox" );
        if (! hsGetSeria(hsSerialArr(box), "id")) {
            delete rst.list;
            rst.info = {  };
        }
    });

    // 规避 POST 一个 json 演示数据文件而失败
    var _hsAjax = $.hsAjax ;
    $.hsAjax = function(url, settings) {
        if (typeof(url) ===  "object") {
            settings = url;
            if (typeof(url["url"]) !== "undefined") {
                url  = url["url"];
            }
        }
        if (settings && url.indexOf(".json") != -1) {
            settings.type = "GET";
        }
        return _hsAjax (url, settings);
    };
})(jQuery);
