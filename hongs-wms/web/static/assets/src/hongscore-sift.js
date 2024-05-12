/* global jQuery, Element, HsForm, HsList, echarts */

function HsSift(context, opts) {
    context = $(context);

    /**
     * 类:
     * sift-unit    分组单元
     * sift-list    组内列表
     * sift-item    筛查条目
     * sift-hand    拖拽把手
     * sift-fn      字段名
     * sift-fr      条件名
     * sift-fv      取值名
     * erase        删除
     * ensue        输入确认
     * value        输入控件
     *
     * 属性
     * data-sift=fn 字段选择
     * data-sift=fr 条件选择
     * data-sift=fv 取值
     * data-sift=lr 列表关系
     */

    var  that    = this ;
    var  siftBox = context.find(".siftbox");
    var  findBox = context.find(".findbox");
    var  formBox = siftBox.find( "form"   );

    if (!formBox.size()) {
         formBox = siftBox;
    }

    this.context = context;
    this.siftBox = siftBox;
    this.findBox = findBox;
    this.formBox = formBox;
    this._url  = opts._url || opts.loadUrl ;

    this.itemTmp = siftBox.find(".sift-item.template").detach();
    this.listTmp = siftBox.find(".sift-unit.template").detach();
    this.itemTmp.removeClass("template");
    this.listTmp.removeClass("template");

    if ( opts ) for (var k in opts) {
        if ('_'===k.substring(0,1 )
        ||  this[k] !== undefined ) {
            this[k]  =  opts[k];
        }
    }

        this.init(  );
    if (opts.loadUrl) {
        this.load(  );
    }
}
HsSift.prototype = {
    init: function() {
        var that = this;
        var siftBox = this.siftBox;
        var fns = siftBox.find("[data-sift=fn]");
        var frs = siftBox.find("[data-sift=fr]");
        var fvx = siftBox.find("[data-sift=fv]");

        // 选中
        siftBox.on("click", ".sift-unit" , function(ev) {
            var cur  = $(this).closest(  ".sift-unit"  );
            var act  = siftBox.find(".sift-unit.active");
            act.not(cur.addClass("active")).removeClass("active");
            ev.stopPropagation();
            return false;
        });

        // 移除
        siftBox.on("click", ".sift-unit .erase,.sift-item .erase", function(ev) {
            var cur  = $(this).closest( ".sift-unit,.sift-item" );
            var lst  = cur.parent();
                       cur.remove();
            lst.trigger("change", ["del", cur]);
            ev.stopPropagation();
            return false;
        });

        // 拖拽
        hsRequires("static/assets/jquery-ui.min.js", function() {
            var rootUnits = siftBox.find(".sift-root");
            var dropEvent = function(ev, ui) {
                var sub = $(ui.draggable);
                var unt = $(ev.target);
                var lst = unt.find(".sift-list:first");
                var un2 = lst.find(".sift-unit:first");
                if (lst[0] === sub.parent()[0]) {
                    return; // 位置没变
                }
                if (sub. is(".sift-unit")
                ||  un2.size() === 0 ) {
                    lst.append(sub);
                } else {
                    un2.before(sub);
                }
                that. fixItem (sub); // 修正字段名前缀
            };
            rootUnits.on("change", function(ev, act, sub) {
                if (act !== "add") return;
                sub.draggable({
                    zIndex : 100 ,
                    revert : true,
                    revertDuration: 0,
                    handle : sub.find(".sift-hand")
                });
                if (! sub.is(".sift-unit") ) return ;
                sub.droppable({
                    accept : ".sift-unit,.sift-item",
                    drop   :   dropEvent
                });
            });
            rootUnits.droppable({
                    accept : ".sift-unit,.sift-item",
                    drop   :   dropEvent
                });
        });

        // 选择字段
        fns.on("change", function() {
            var fno = that.getOpt(fns);
            var rls = fno.data("rels");

            // 将支持的条件转为字典类结构
            // 以便下方快速判断是否需隐藏
            if (typeof rls === "string") {
              var a = $.trim(rls).split(/\s*,\s*/);
                rls = { };
                for(var i = 0; i < a.length; i ++) {
                    rls [a[i]] = true;
                }
                fno.data("rels", rls);
            } else
            if (! rls ) {
                rls = { };
                fno.data("rels", rls);
            }

            // 登记首个选项, 处理选项隐藏
            var vls = [ ] ;
            frs.children().each(function() {
                var v = $(this).val();console.log(v)
                var x = rls[v];
                if (x && !vls.length) {
                    vls  = [v];
                }
                $(this).prop("hidden", !x);
            });
            console.log(rls, vls)

            frs.val(vls);
            frs.change();
        });

        // 选择条件
        frs.on("change", function() {
            var fno = that.getOpt(fns);
            var fro = that.getOpt(frs);
            var fk  = fno.data("kind");
            var ft  = fno.data("type");
            var ln  = fno.data("ln");
            var fn  = fno.val();
            var fr  = fro.val();
            var fv  ;

            // 按名称/别名/类别依次查找取值组件
            do {
                if (fr ==  "" ) {
                    fv  = fvx.children("[data-kind='no']");
                }
                if (fr == "is") {
                    fv  = fvx.children("[data-kind='is']");
                    break;
                }
                if (fn) {
                    fv  = fvx.children("[data-name='"+fn+"']");
                    if (fv.size()) break;
                }
                if (ln) {
                    fv  = fvx.children("[data-name='"+ln+"']");
                    if (fv.size()) break;
                }
                if (ft) {
                    fv  = fvx.children("[data-type='"+ft+"']");
                    if (fv.size()) break;
                }
                if (fk) {
                    fv  = fvx.children("[data-kind='"+fk+"']");
                    if (fv.size()) break;
                }
                    // 默认选项
                    fv  = fvx.children("[data-kind='is']");
            }
            while (false);

            fv.addClass("active").siblings().removeClass("active");
        });

        // 选项初始化
        fns.trigger("change");

        //** 类型扩展事件 **/

        // 输入类
        var inp = function( ) {
            var fvi = $(this).closest(".sift-input" ).find(".value");
            var fno = that.getOpt(fns);
            var fro = that.getOpt(frs);
            var val = fvi .val();
            if (val === "") {
                that.note(that._empty_value_error,"warning");
                return;
            }
            fvi .val ( "" );
            that.addItem(that.getList(), fno, fro, val, val);
        };
        var ent = function(e) {
            // 回车即确认
            if ( 13 === e.keyCode ) {
                inp . call ( this );
                e.stopPropagation();
                return false;
            }
        };
        fvx.on("click"  , ">.sift-input .ensue", inp);
        fvx.on("keydown", ">.sift-input"       , ent);

        // 选项类
        var sel = function( ) {
            var fvs = $(this).closest(".sift-select").find(".value");
            var fno = that.getOpt(fns);
            var fro = that.getOpt(frs);
            var fvo = that.getOpt(fvs);
            var val = fvo .val ();
            var txt = fvo .text();
            if (val === "") {
                that.note(that._empty_value_error,"warning");
                return;
            }
            fvs .val ([""]);
            that.addItem(that.getList(), fno, fro, val, txt);
        };
        fvx.on("click" , ">.sift-select .ensue", sel);
        fvx.on("change", ">.sift-select"       , sel);

        // 定制类
        fvx.on("click", ">[data-kind=sift] .btn", function() {
            var fno = that.getOpt(fns);
            var fro = that.getOpt(frs);
            var fn  = fno.val();

            if (that["_sift_"+fn]) {
                that["_sift_"+fn](fno, fro);
            }
        });

        // 关联
        fvx.on("click", ">[data-kind=fork] .btn", function() {
            var fno = that.getOpt(fns);
            var fro = that.getOpt(frs);
            var ln  = fno.data("ln") || "";
            var vk  = fno.data("vk") || "id";
            var tk  = fno.data("tk") || "name";
            var url = fno.data( "href" ) || "";
            var bin = fno.data("target") || "";
            var box = jQuery('<div></div>').attr({"data-ln": ln, "data-vk": vk, "data-tk": tk});
            var btn = $(this);

            btn.hsPick( url, bin, box, function (btn, v) {
                for(var val in v) {
                    var arr  = v[val];
                    var txt  = arr[0];
                    if (! val) {
                        that.note(that._empty_value_error,"warning");
                        return;
                    }
                    that.addItem(that.getList(), fno, fro, val, txt);
                }
            });
        });

        // 日期
        fvx.on("click", ">[data-kind=date] .btn", function() {
            var fno = that.getOpt(fns);
            var fro = that.getOpt(frs);
            var typ = fno.data("type");
            var fmt = fno.data("format");
            var fst = fno.data("offset");
            var mul = 1;

            // 转换预置格式
            switch (fmt) {
                case "time":
                    fmt = hsGetLang("time.format");
                    break;
                case "date":
                    fmt = hsGetLang("date.format");
                    break;
                case "datetime" : case "" : case null :
                    fmt = hsGetLang("datetime.format");
            }
            switch (typ) {
                case "timestamp":
                case "datestamp":
                    mul = 1000;
            }

            var inp = $('<input type="hidden" data-topple="hsDate"/>');
            inp.attr("data-type"  , typ);
            inp.attr("data-format", fmt);
            inp.attr("data-offset", fst);

            that.mask(
                fno.text()+" "+fro.text(),
                inp,
                function() {
                    var val = parseInt (inp.val ( ) );
                    var txt = hsFmtDate(val*mul, fmt);
                    if (isNaN(val) || "" == val) {
                        this.note(this._empty_value_error,"warning");
                        return;
                    }
                    this.addItem(this.getList(), fno, fro, val, txt);
                }
            ).hsReady();
        });
    },

    load: function() {
        var that = this;
        $.hsAjax({
            url     : this._url,
            cache   : true,
            async   : true,
            type    : "get" ,
            dataType: "json",
            success : function(rst) {
                rst = hsResponse(rst);
                if (rst && rst.ok ) {
                    that.fill(rst.enfo || {});
                }
            }
        });
    },
    fill: function(enfo) {
        HsForm.prototype.fillEnfo.call(this, enfo);
    },
    _feed__datalist: HsForm.prototype._feed__datalist,
    _feed__select  : HsForm.prototype._feed__select  ,
    _feed__radio   : HsForm.prototype._feed__radio   ,
    _feed__check   : HsForm.prototype._feed__check   ,
    _feed__checkset: HsForm.prototype._feed__checkset,

    note: function(msg, typ) {
        // 拆分标题和说明
        var txt  ;
        var pos  = msg.indexOf( "\r\n" );
        if (pos !== -1) {
            txt  = msg.substring(1+ pos);
            msg  = msg.substring(0, pos);
        }

        // 转换对话框样式
        var cls  = "alert-" + typ ;

        return $.hsMask({
            mode : "note",
            text : txt,
            title: msg,
            glass: cls,
            count:  3 , // 停留三秒钟
            focus: -1 , // 聚焦到关闭
            position: "top" // 放顶部
        });
    },
    mask: function(tit, inp, back) {
        var that = this;
        var mask = $.hsMask({
            title: tit,
            html : '<form onsubmit="return false">'
                 + '<div class="invisible"><button type="submit"></button></div>'
                 + '</form>'
        }, {
            label: "确定",
            glass: "btn-primary",
            click: function() {
                form.find(":submit").click();
            }
        }, {
            label: "取消",
            glass: "btn-link"
        });
        var form = mask.find("form");
        form.append(inp);
        form.find( "input,select,textarea" ).not(":hidden")
            .first ().focus ();
        form.submit(function() {
            back.call(that, form);
            mask.hsClose();
            return false;
        });
        return mask;
    },

    /**
     * 获取选中选项
     * @returns {jQuery}
     */
    getOpt : function(sel, val) {
        val  = val || sel. val();
        return sel.children().filter(function() {
            return $(this).val() === val;
        });
    },

    /**
     * 获取活动列表
     * @returns {jQuery}
     */
    getList: function(nam) {
        if (nam) {
          return  this.siftBox.find(".sift-list[data-name='"+nam+"'");
        }

        var lst = this.siftBox.find(".sift-unit.active").find(".sift-list:first");
        if ( ! lst.size() ) {
            lst = this.siftBox.find(".sift-unit:first" ).find(".sift-list:first");
        }
        return lst;
    },

    /**
     * 添加列表
     * @param {jQuery} lst 活动列表
     * @param {string} rel 关系标识: ar/or/nr
     * @param {string} txt 分组标题
     * @returns {jQuery} 新的列表
     */
    addList: function(lst, rel, txt) {
        var sub = this.listTmp.clone();
        var pre = lst.data("name") || "ar";
        var cnt = lst.data("cnt" ) ||  0  ;
            cnt = parseInt( cnt  );
        var nam = pre +"."+ cnt +"."+ rel ;

        sub.find(".sift-lr").text(txt);
        sub.find(".sift-list").first()
           .attr( "data-name" , nam  );
        lst.data( "cnt", cnt + 1 );
        lst.append(sub);

        // 触发变更事件
        lst.trigger("change", ["add", sub]);

        return sub;
    },

    /**
     * 添加条目
     * @param {jQuery} lst 活动列表
     * @param {jQuery|array} fno 字段选项,或[代号,文本]
     * @param {jQuery|array} fro 条件选项,或[符号,文本]
     * @param {string} val 取值
     * @param {string} txt 标签
     * @returns {jQuery} 新的条目
     */
    addItem: function(lst, fno, fro, val, txt) {
        if (! $.isArray(fno)) {
            fno = [
                fno.data("name") || fno.val (),
                fno.data("text") || fno.text()
            ];
        }
        if (! $.isArray(fro)) {
            fro = [
                fro.data("name") || fro.val (),
                fro.data("text") || fro.text()
            ];
        }

        var sub = this.itemTmp.clone();
        var pre = lst.data("name") || "ar";
        var cnt = lst.data("cnt" ) ||  0  ;
            cnt = parseInt( cnt  );
        var nam = pre +"."+ cnt +"."+ fno[0] +"."+ fro[0];
        var inp = $('<input type="hidden"/>');

        sub.find(".sift-fn").text(fno[1]);
        sub.find(".sift-fr").text(fro[1]);
        sub.find(".sift-fv").text(txt);
        inp.attr("name", nam).val(val);
        lst.data( "cnt", cnt + 1 );
        sub.append(inp);

        var ls2 = lst.children(".sift-unit:first");
        if (ls2.size()) {
            ls2.before(sub);
        } else {
            lst.append(sub);
        }

        // 触发变更事件
        lst.trigger("change", ["add", sub]);
        console.log(lst, sub);

        return sub;
    },

    /**
     * 重排列表索引
     * @param {jQuery} lst 列表
     */
    fixList: function(lst) {
        var that = this;
        var cnt  = 0x0 ;
        var pre  = lst.data( "name" ) || "ar" ;

        // 列表下移
        lst.append(lst.children(".sift-unit"));

        // 逐个改名
        lst.children().each(function () {
            var sub = $(this);
            var inp ;
            var nam ;

            if (! sub.is(".sift-unit")) {
                inp = sub.find("input:hidden").first();
                nam = inp.attr("name") || "" ;
            } else {
                inp = sub.find( ".sift-list" ).first();
                nam = inp.data("name") || "" ;
            }

            // 提取标准名称 xxxx.xx
            var mat = /[^\.]+\.[^\.]{2}\.?$/.exec(nam);
            if (mat) {
                nam = mat[0];
            }

            nam = pre +"."+ cnt +"."+ nam ;
            if (! sub.is(".sift-unit")) {
                inp.attr(/**/ "name", nam);
            } else {
                inp.attr("data-name", nam);
                inp.removeData("name");
                that . fixList( inp  ); // 递归
            }

            cnt ++;
        });
        lst.data("cnt", cnt);
        return cnt;
    },

    /**
     * 修正条目索引
     * @param {type} sub
     * @returns {Number}
     */
    fixItem: function(sub) {
        var lst = sub.parent();
        var pre = lst.data("name") || "ar";
        var cnt = lst.data("cnt" ) ||  0  ;
            cnt = parseInt( cnt  );
        var inp ;
        var nam ;

        if (! sub.is(".sift-unit")) {
            inp = sub.find("input:hidden").first();
            nam = inp.attr("name") || "" ;
        } else {
            inp = sub.find( ".sift-list" ).first();
            nam = inp.data("name") || "" ;
        }

        // 提取标准名称 xxxx.xx
        var mat = /[^\.]+\.[^\.]{2}\.?$/.exec(nam);
        if (mat) {
            nam = mat[0];
        }

        nam = pre +"."+ cnt +"."+ nam ;
        if (! sub.is(".sift-unit")) {
            inp.attr(/**/ "name", nam);
        } else {
            inp.attr("data-name", nam);
            inp.removeData("name");
            this . fixList( inp  ); // 递归
        }

            cnt ++;
        lst.data("cnt", cnt);
        return cnt;
    },

    /**
     * 添加条目
     * @param {string} key 参数名称
     * @param {string} val 参数取值
     * @param {string|array} txt 值对应的显示文本, 或 [字段文本,条件文本,取值文本]
     * @returns {HsSift.prototype@call;addItem}
     */
    addTerm: function(key, val, txt) {
        var mat = /^(?:(.*)\.[^\.]*\.)?([^\.]+)\.([^\.]{2})\.?$/.exec(key);
        if (! mat) {
            throw new Error("HsSift.addTerm: Can not parse key '"+key+"', must like 'xxx.eq' or 'xx.0.xxx.eq'");
        }
        var pn  = mat[1] || "ar";
        var fn  = mat[2];
        var fr  = mat[3];

        var lst = this.getList(pn);
        if (! lst.size()) {
            throw new Error("HsSift.addTerm: Can not find list for '"+pre+"'");
        }

        var fno, fro;
        if (! $.isArray(txt)) {
            var fx = this.siftBox.find("[data-sfit=fr]").val() || "" ;
            fno = this.siftBox.find("[data-sfit=fn][value='"+fn+"']");
            if (! fno.size()) {
                throw new Error("HsSift.addTerm: Can not find FN opt for '"+fn+"'");
            }
            this.siftBox.find("[data-sfit=fn]").val([fn]).first().trigger("change"); // 切到对应字段, 以便下方读取
            fro = this.siftBox.find("[data-sfit=fr][value='"+fr+"']");
            if (! fno.size()) {
                throw new Error("HsSift.addTerm: Can not find FR opt for '"+fn+"'");
            }
            this.siftBox.find("[data-sfit=fn]").val([fx]).first().trigger("change"); // 切回初始状态, 避免干扰操作
        } else {
            fno = [fn, txt[0]];
            fro = [fr, txt[1]];
            txt =      txt[2] ;
        }

        return this.addItem(lst, fno, fro, val, txt);
    },

    _empty_field_error: '字段为空\r\n请先选择字段, 然后再取值',
    _empty_value_error: '不可为空\r\n如需筛查空值, 请使用条件"为"'
};
jQuery.fn.hsSift = function(opts) {
    return this.hsBind(HsSift, opts);
};

/**
 * 筛查枚举数据整理
 * 清理
 * @param   {Array} v 枚举数据
 * @param {boolean} a 是否加空
 * @returns {Array}   枚举数据
 */
function hsSiftFeedEnum(v, a) {
    if (!v || !v.length) {
        return v;
    }

    // 补充空值, 供显式筛选用
    var w = a ? [["", ""]] : [];

    // 清理空值, 杠为未知其他
    for(var i = 0; i < v.length; i ++) {
        var u = v [i];
        if (! u[1] || ! u[0] || u[0] == "-") {
            continue;
        }
        w.push(u);
    }

    return w;
}

/**
 * 列表统计筛选组件
 * @param {jQuery|Element} context
 * @param {Object} opts
 * @return {HsStat}
 */
function HsStat(context, opts) {
    context = $(context);

    var  statBox = context.find(".statbox");
    var  findBox = context.find(".findbox");

    this.context = context;
    this.statBox = statBox;
    this.findBox = findBox;
    this._url  = opts._url || opts.loadUrl ;

    var  that  = this;

    //** 条件改变时重载图表 **/

    statBox.data("changed", statBox.is(".invisible"));

    context.on("saveBack sendBack", function() {
        if (statBox.is(".invisible")) {
            statBox.data("changed", true );
        } else {
            statBox.data("changed", false);
            setTimeout(function() {
                that.load();
            }, 1000);
        }
    });

    findBox.on( "submit" , function() {
        if (statBox.is(".invisible")) {
            statBox.data("changed", true );
        } else {
            statBox.data("changed", false);
            setTimeout(function() {
                that.load();
            }, 1000);
        }
    });

    statBox.on( "change" , "input,select", function() {
        if ($(this).is(".checkall2")) {
            $(this).closest(".checkbox").find(".checkone2").prop("checked", false);
        } else
        if ($(this).is(".checkone2")) {
            $(this).closest(".checkbox").find(".checkall2").prop("checked", false);
        }
        findBox.find(":submit").first().click();
    });
}
HsStat.prototype = {
    load: function() {
        var that    = this;
        var statBox = this.statBox;

        hsRequires("static/addons/echarts/echarts.min.js", function() {
            statBox.find(".stat-group")
                   .each(  function  () {
                var sta =  $(  this  );
                if(!sta.data("echart")) {
                    var box = sta.find(".chartbox");
                    var obj = echarts.init(box[0] );
                    sta.data("echart", obj);
                }
            });

            that.fill(statBox.find(".stat-group[data-name]"));
        });
    },

    fill: function(itemBox) {
        if ( 0 === itemBox.size() ) return;

        var that    = this;
        var context = this.context;
        var statBox = this.statBox;
        var findBox = this.findBox;
        var url     = this._url;

        var rb  = [];
        var eb  = [];

        itemBox.each(function() {
            if ($(this).data("name")) rb.push($(this).data("name"));
            if ($(this).data("type")) eb.push($(this).data("name")+".rb="+$(this).data("type"));
            if ($(this).data("prms")) eb.push($(this).data("prms"));

            $(this).find(".checkbox").hide();
            $(this).find(".chartbox").hide();
            $(this).find(".alertbox").show()
             .children().text($(this).data("text")+" 统计中...");
        });

        // 筛选数据
        var dat;
        var mod;
        do {
            mod = context.data("HsList");
            if (mod) {
                dat = mod._data;
                break;
            }

            mod = context.data("HsTree");
            if (mod) {
                dat = mod._data;
                break;
            }

            dat = findBox;
        }
        while (false);

        // 重建数据, 删除可能冲突的参数
        url = hsSetParam (    url, "rb", rb.join(","));
        dat = hsSerialMix({}, dat, "?" + eb.join("&"));
        delete dat["rb" ];
        delete dat["rb."];
        delete dat["ob" ];
        delete dat["ob."];
        delete dat["ab" ];
        delete dat["ab."];
        delete dat["rn" ];
        delete dat["pn" ];

        $.hsAjax({
            url : url,
            data: dat,
            type: "post",
            dataType : "json",
            cache  : true,
            success: function(rst) {
                rst  = rst.enfo || {};
                for(var k  in rst) {
                    var d  =  rst [k];
                    var n  =  statBox.find("[data-name='"+k+"']");
                    var t  =  n.attr("data-type");

                    if (n.size() === 0) {
                        continue;
                    }
                    if (d.length === 0) {
                        continue;
                    }

                    n.find(".alertbox").hide();
                    n.find(".checkbox").show();
                    n.find(".chartbox").show();

                    if (t === "range"
                    ||  t === "tally") {
                        that.setAmountCheck(n , d);
                        that.setAcountChart(n , d);
                    } else
                    if (t === "total") {
                        that.setAmountCheck(n , d);
                        that.setAmountChart(n , d);
                    } else
                    {
                        that.setAcountCheck(n , d);
                        that.setAcountChart(n , d);
                    }

                    k = n.find(".checkone2").attr( "name" );
                    k = _hsGetDkeys(k) [ 0 ]; // 规避数组结尾
                    if (dat && dat [k] ) {
                        n.find(".checkone2").val ( dat[k] );
                    } else {
                        n.find(".checkall2").prop("checked", true);
                    }
                }

                itemBox.each(function() {
                    $(this).find(".alertbox:visible")
                     .children().text($(this).data("text")+" 无统计值!");
                });
            }
        });
    },

    copy: function(itemBox) {
        var context = this.context;
        var findBox = this.findBox;
        var listBox = itemBox.find(".checkbox");

        // 检查接口
        if (! $.hsCanCopy()) {
            $.hsWarn("浏览器无法拷贝\r\n请使用较新版的 Chrome/Firefox/Safari/Edge, 或某些双核/多核浏览器的极速模式.", "warning");
            return;
        }

        // 复制提示
        var set = [{
            mode : "warn",
            glass: "alert-default",
            text : "为了降低网络延迟, 图表按从多到少排, 仅取部分; 这将会调取完整统计数据, 以便导入 Office/WPS 等应用中.",
            title: "正在获取完整统计数据, 请稍等...",
            topic: "已经取得完整统计数据, 请拷贝吧!",
            note : "拷贝成功, 去粘贴吧!"
        }, {
            glass: "btn-primary copies",
            label: "拷贝"
        }, {
            glass: "btn-default",
            label: "取消"
        }];
        var msk = $.hsMask.apply( $, set );
        var mok = function() {
            msk.removeClass ("alert-info")
               .addClass ("alert-success");
            msk.find(".alert-footer .btn")
               .prop( "disabled" , false );
            msk.find(".alert-title" /**/ )
               .text( set[0].topic );
            return msk;
        };
        msk.find(".alert-footer button").prop("disabled", true );
        msk.find(".alert-footer button.copies").click(function() {
            var div = listBox.children( "div" );
            var tab = /**/div.children("table");
            div.show(  );
            tab.hsCopy();
            div.hide(  );
            $.hsNote( set[0].note , "success" );
        });

        // 重复拷贝
        var div = listBox.children( "div" );
        if (div.size( )) {
            mok( );
            return;
        }

        var fn  = itemBox.attr("data-name");
        var fp  = itemBox.attr("data-prms");
        var fx  = itemBox.attr("data-text");
        var ft  = itemBox.attr("data-type");
        var url = this._url;

        // 筛选数据
        var dat;
        var mod;
        do {
            mod = context.data("HsList");
            if (mod) {
                dat = mod._data;
                break;
            }

            mod = context.data("HsTree");
            if (mod) {
                dat = mod._data;
                break;
            }

            dat = findBox;
        }
        while (false);

        // 重建数据, 删除可能冲突的参数
        url = hsSetParam (    url, "rn", 0 );
        url = hsSetParam (    url, "rb", fn);
        dat = hsSerialMix({}, dat, "?" + fp);
        delete dat[ fn + ".rn" ];
        delete dat["rb" ];
        delete dat["rb."];
        delete dat["ob" ];
        delete dat["ob."];
        delete dat["ab" ];
        delete dat["ab."];
        delete dat["rn" ];
        delete dat["pn" ];

        // 读取全部
        $.hsAjax({
            url : url,
            data: dat,
            type: "post",
            dataType : "json",
            cache  : true,
            success: function(rst) {
                // 构建表格
                var div = $('<div></div>').appendTo(listBox);
                var tab = $('<table></table>').appendTo(div);
                var thd = $('<thead></thead>').appendTo(tab);
                var tbd = $('<tbody></tbody>').appendTo(tab);
                var tr  = $('<tr></tr>');
                var th  = $('<td></th>');
                var td  = $('<td></td>');
                div.attr("style", "height:1px; display:none; overflow:auto;");
                tab.attr("style", "margin:1px;")
                   .attr("class", "table table-bordered");

                // 表头
                var tr2 = tr.clone().appendTo ( thd );
                    th.clone().appendTo(tr2).text(fx);
                    th.clone().appendTo(tr2).text("数量");
                if (ft === "amount") {
                    th.clone().appendTo(tr2).text("求和");
                    th.clone().appendTo(tr2).text("最小");
                    th.clone().appendTo(tr2).text("最大");
                }

                // 内容
                if (rst.enfo && rst.enfo[fn]) {
                        var a = rst.enfo[fn];
                    for(var i = 0; i < a.length; i ++) {
                        var b = a [i];
                        if (b[1]===null) b[1] = '#'+b[0];
                        tr2 = tr.clone().appendTo( tbd );
                    for(var j = 1; j < b.length; j ++) {
                        var c = b [j];
                        td.clone().appendTo(tr2).text(c);
                    }}
                }

                mok ();
            }
        });
    },

    setAmountCheck: function(box, data) {
        var name  = box.data("name");
        var text  = box.data("text");
        var box2  = box.find(".checkbox").empty();

        var label = $('<label></label>');
        var check = $('<input type="checkbox" class="checkall2"/>');
        var title = $('<span></span>')
                .text("全部" + text);
            label.append(check).append(title).appendTo(box2);

        for(var i = 0; i < data.length; i ++) {
            var v = data[i];
            if (v[0] === "" )  continue;
            if (v[1] == null)  v[1] = "#"+ v[0] ;
            label = $('<label'+(!v[2] ? ' class="text-muted"' : '')+'></label>')
                .attr("title", v[1] +" ("+ v[2] + (v[3] ? ","+ v[3] : "") + ")");
            check = $('<input type="checkbox" class="checkone2"/>')
                .attr("name" , name + ".at.")
                .attr("value", v[0]);
            title = $('<span></span>')
                .text(v[1]);
            label.append(check).append(title).appendTo(box2);
        }
    },

    setAcountCheck: function(box, data) {
        var name  = box.data("name");
        var text  = box.data("text");
        var box2  = box.find(".checkbox").empty();

        var label = $('<label></label>');
        var check = $('<input type="checkbox" class="checkall2"/>');
        var title = $('<span></span>')
                .text("全部" + text);
            label.append(check).append(title).appendTo(box2);

        for(var i = 0; i < data.length; i ++) {
            var v = data[i];
            if (v[0] === "" )  continue;
            if (v[1] == null)  v[1] = "#"+ v[0] ;
            label = $('<label'+(!v[2] ? ' class="text-muted"' : '')+'></label>')
                .attr("title", v[1] +" ("+ v[2] + ")");
            check = $('<input type="checkbox" class="checkone2"/>')
                .attr("name" , name + ".in.")
                .attr("value", v[0]);
            title = $('<span></span>')
                .text(v[1]);
            label.append(check).append(title).appendTo(box2);
        }
    },

    setAcountChart: function(box, data) {
        var that  = this;
        var chart = box.data("echart");
        var xData = [];
        var bData = [];
        var pData = [];
        for(var i = 0; i < data.length; i ++) {
            var v = data[i];
            if (v[0] === "") continue ;
            xData.push(v[1]);
            bData.push(v[2]);
            pData.push({
                value: v[2],
                name : v[1]
            });
        }

        var pieOpts = {
            series: [{
                data: pData,
                type: "pie"
            }],
            xAxis : [{
                show: false
            }],
            yAxis : [{
                show: false
            }]
        };
        var barOpts = {
            series: [{
                data: bData,
                type: "bar",
                barMaxWidth: 13,
                itemStyle: { normal: {
                    barBorderRadius: [4, 4, 0, 0]
                } }
            }],
            xAxis : [{
                data: xData,
                show: true,
                type: "category"
            }],
            yAxis : [{
                show: true,
                type: "value"
            }]
        };
        var opts = {
            grid : {
                top: 30,
                left: 15,
                right: 15,
                bottom: 0,
                containLabel: true
            },
            tooltip: {},
            toolbox: {
                show: true,
                feature: {
                    show: true,
                    myBar: {
                        show: true,
                        icon: 'M85.312 938.688H1024V1024H0V0h85.312v938.688zM256 341.312h85.312V768H256V341.312zM512 128h85.312v640H512V128z m256 213.312h85.312V768H768V341.312z',
                        iconStyle: {borderWidth: 0, color: '#666'},
                        title: '柱状图',
                        onclick: function() {
                            box.data("plot", "bar");
                            chart.setOption(chart._barOpts);
                        }
                    },
                    myPie: {
                        show: true,
                        icon: 'M85.312 512a426.688 426.688 0 0 0 844.8 85.312H426.688V93.888A426.816 426.816 0 0 0 85.312 512zM512 0v512h512a512 512 0 1 1-512-512z m506.816 438.848H585.152V5.184a512.32 512.32 0 0 1 433.664 433.664z',
                        iconStyle: {borderWidth: 0, color: '#666'},
                        title: '饼视图',
                        onclick: function() {
                            box.data("plot", "pie");
                            chart.setOption(chart._pieOpts);
                        }
                    },
                    myTab: {
                        show: true,
                        icon: 'M512 1024A512 512 0 1 1 512 0a512 512 0 0 1 0 1024z m0-85.312A426.688 426.688 0 1 0 512 85.312a426.688 426.688 0 0 0 0 853.376zM320 576a64 64 0 1 1 0-128 64 64 0 0 1 0 128z m192 0a64 64 0 1 1 0-128 64 64 0 0 1 0 128z m192 0a64 64 0 1 1 0-128 64 64 0 0 1 0 128z',
                        iconStyle: {borderWidth: 0, color: '#666'},
                        title: '导出',
                        onclick: function() {
                            that.copy(box );
                        }
                    }
                }
            }
        };

        chart._pieOpts = pieOpts;
        chart._barOpts = barOpts;

        if (box.data("plot") == "pie") {
            $.extend( opts , pieOpts );
        } else {
            $.extend( opts , barOpts );
        }

        chart.resize();
        chart.setOption(opts);
    },

    setAmountChart: function(box, data) {
        var that  = this;
        var chart = box.data("echart");
        var xData = [];
        var bData1 = [];
        var bData2 = [];
        var pData1 = []; // Count
        var pData2 = []; // Sum
        for(var i = 0; i < data.length; i ++) {
            var v = data[i];
            if (v[0] === "") continue ;
            xData.push(v[1]);
            bData1.push(v[2]);
            bData2.push(v[3]);
            pData1.push({
                value: v[2],
                name : v[1]
            });
            pData2.push({
                value: v[3],
                name : v[1]
            });
        }

        var pieOpts = {
            series: [{
                radius : [ 0, 50],
                data: pData1,
                type: "pie"
            }, {
                radius : [60, 80],
                data: pData2,
                type: "pie"
            }],
            xAxis : [{
                show: false
            }],
            yAxis : [{
                show: false
            }, {
                show: false
            }]
        };
        var barOpts = {
            series: [{
                data: bData1,
                type: "bar",
                barMaxWidth: 13,
                itemStyle: { normal: {
                    barBorderRadius: [4, 4, 0, 0]
                } }
            }, {
                data: bData2,
                type: "bar",
                barMaxWidth: 13,
                itemStyle: { normal: {
                    barBorderRadius: [4, 4, 0, 0]
                } }
            }],
            xAxis : [{
                data: xData,
                show: true,
                type: "category"
            }],
            yAxis : [{
                show: true,
                type: "value"
            }, {
                show: true,
                type: "value",
                position: "right"
            }]
        };
        var opts = {
            grid : {
                top: 30,
                left: 15,
                right: 15,
                bottom: 0,
                containLabel: true
            },
            tooltip: {},
            toolbox: {
                show: true,
                feature: {
                    show: true,
                    myBar: {
                        show: true,
                        icon: 'M85.312 938.688H1024V1024H0V0h85.312v938.688zM256 341.312h85.312V768H256V341.312zM512 128h85.312v640H512V128z m256 213.312h85.312V768H768V341.312z',
                        iconStyle: {borderWidth: 0, color: '#666'},
                        title: '柱状图',
                        onclick: function() {
                            box.data("plot", "bar");
                            chart.setOption(chart._barOpts);
                        }
                    },
                    myPie: {
                        show: true,
                        icon: 'M85.312 512a426.688 426.688 0 0 0 844.8 85.312H426.688V93.888A426.816 426.816 0 0 0 85.312 512zM512 0v512h512a512 512 0 1 1-512-512z m506.816 438.848H585.152V5.184a512.32 512.32 0 0 1 433.664 433.664z',
                        iconStyle: {borderWidth: 0, color: '#666'},
                        title: '饼视图',
                        onclick: function() {
                            box.data("plot", "pie");
                            chart.setOption(chart._pieOpts);
                        }
                    },
                    myTab: {
                        show: true,
                        icon: 'M512 1024A512 512 0 1 1 512 0a512 512 0 0 1 0 1024z m0-85.312A426.688 426.688 0 1 0 512 85.312a426.688 426.688 0 0 0 0 853.376zM320 576a64 64 0 1 1 0-128 64 64 0 0 1 0 128z m192 0a64 64 0 1 1 0-128 64 64 0 0 1 0 128z m192 0a64 64 0 1 1 0-128 64 64 0 0 1 0 128z',
                        iconStyle: {borderWidth: 0, color: '#666'},
                        title: '导出',
                        onclick: function() {
                            that.copy(box );
                        }
                    }
                }
            }
        };

        chart._pieOpts = pieOpts;
        chart._barOpts = barOpts;

        if (box.data("plot") == "pie") {
            $.extend( opts , pieOpts );
        } else {
            $.extend( opts , barOpts );
        }

        chart.resize();
        chart.setOption(opts);
    }
};
jQuery.fn.hsStat = function(opts) {
    return this.hsBind(HsStat, opts);
};

/**
 * 列表分类筛选组件
 * @param {jQuery|Element} context
 * @param {Object} opts
 * @return {HsStat}
 */
function HsCate(context, opts) {
    context = $(context);

    var  statBox = context.find(".statbox");
    var  findBox = context.find(".findbox");

    this.context = context;
    this.statBox = statBox;
    this.findBox = findBox;
    this._url  = opts._url || opts.loadUrl ;

    var  that  = this;

    //** 条件改变时重载图表 **/

    statBox.data("changed", statBox.is(".invisible"));

    context.on("saveBack sendBack", function() {
        if (statBox.is(".invisible")) {
            statBox.data("changed", true );
        } else {
            statBox.data("changed", false);
            setTimeout(function() {
                that.load();
            }, 1000);
        }
    });

    findBox.on( "submit" , function() {
        if (statBox.is(".invisible")) {
            statBox.data("changed", true );
        } else {
            statBox.data("changed", false);
            setTimeout(function() {
                that.load();
            }, 1000);
        }
    });

    statBox.on( "change" , "input,select", function() {
        if ($(this).is(".checkall2")) {
            $(this).closest(".checkbox").find(".checkone2").prop("checked", false);
        } else
        if ($(this).is(".checkone2")) {
            $(this).closest(".checkbox").find(".checkall2").prop("checked", false);
        }
        findBox.find(":submit").first().click();
    });
}
HsCate.prototype = {
    load: function() {
        var that    = this;
        var statBox = this.statBox;
        that.fill(statBox.find(".stat-group[data-name]"));
    },

    fill: function(itemBox) {
        if ( 0 === itemBox.size() ) return;

        var that    = this;
        var context = this.context;
        var statBox = this.statBox;
        var findBox = this.findBox;
        var url     = this._url;

        var rb  = [];
        var eb  = [];

        itemBox.each(function() {
            if ($(this).data("name")) rb.push($(this).data("name"));
            if ($(this).data("type")) eb.push($(this).data("name")+".rb="+$(this).data("type"));
            if ($(this).data("prms")) eb.push($(this).data("prms"));

            $(this).empty(); // 清空待写入选项
        });

        // 筛选数据
        var dat;
        var mod;
        do {
            mod = context.data("HsList");
            if (mod) {
                dat = mod._data;
                break;
            }

            mod = context.data("HsTree");
            if (mod) {
                dat = mod._data;
                break;
            }

            dat = findBox;
        }
        while (false);

        // 重建数据, 删除可能冲突的参数
        url = hsSetParam (    url, "rb", rb.join(","));
        dat = hsSerialMix({}, dat, "?" + eb.join("&"));
        delete dat["rb" ];
        delete dat["rb."];
        delete dat["ob" ];
        delete dat["ob."];
        delete dat["ab" ];
        delete dat["ab."];
        delete dat["rn" ];
        delete dat["pn" ];

        $.hsAjax({
            url : url,
            data: dat,
            type: "post",
            dataType : "json",
            cache  : true,
            success: function(rst) {
                rst  = rst.enfo || {};
                for(var k  in rst) {
                    var d  =  rst [k];
                    var n  =  statBox.find("[data-name='"+k+"']");
                    var t  =  n.attr("data-type");

                    if (n.size() === 0 ) {
                        continue;
                    }
                    if (d.length === 0 ) {
                        continue;
                    }

                    if (t === "range"
                    ||  t === "tally"
                    ||  t === "total") {
                        that.setAmountCheck(n , d);
                    } else
                    {
                        that.setAcountCheck(n , d);
                    }

                    k = n.find(".checkone2").attr( "name" );
                    k = _hsGetDkeys(k) [ 0 ]; // 规避数组结尾
                    if (dat && dat[k] ) {
                        n.find(".checkone2").val ( dat[k] );
                    } else {
                        n.find(".checkall2").prop("checked", true);
                    }
                }

                // 隐藏空的统计块
                itemBox.filter(":empty").closest(".stat-group").hide();
            }
        });
    },

    setAmountCheck: function(box, data) {
        var name  = box.data("name");
        var text  = box.data("text");
        var box2  = box.find(".checkbox").empty();

        if (data.length == 0) {
            box.closest(".form-group").hide();
            return;
        } else {
            box.closest(".form-group").show();
        }

        var label = $('<label></label>');
        var check = $('<input type="checkbox" class="checkall2"/>');
        var title = $('<span></span>')
                .text("全部");
            label.append(check).append(title).appendTo(box2);

        for(var i = 0; i < data.length; i ++) {
            var v = data[i];
            if (v[0] === "") continue ;
            label = $('<label'+(!v[2] ? ' class="text-muted"' : '')+'></label>')
                .attr("title", v[1] +" ("+ v[2] + (v[3] ? ","+ v[3] : "") + ")");
            check = $('<input type="checkbox" class="checkone2"/>')
                .attr("name" , name + ".at.")
                .attr("value", v[0]);
            title = $('<span></span>')
                .text(v[1]);
            label.append(check).append(title).appendTo(box2);
        }
    },

    setAcountCheck: function(box, data) {
        var name  = box.data("name");
        var text  = box.data("text");
        var box2  = box.find(".checkbox").empty();

        if (data.length == 0) {
            box.closest(".form-group").hide();
            return;
        } else {
            box.closest(".form-group").show();
        }

        var label = $('<label></label>');
        var check = $('<input type="checkbox" class="checkall2"/>');
        var title = $('<span></span>')
                .text("全部");
            label.append(check).append(title).appendTo(box2);

        for(var i = 0; i < data.length; i ++) {
            var v = data[i];
            if (v[0] === "") continue ;
            label = $('<label'+(!v[2] ? ' class="text-muted"' : '')+'></label>')
                .attr("title", v[1] +" ("+ v[2] + ")");
            check = $('<input type="checkbox" class="checkone2"/>')
                .attr("name" , name + ".in.")
                .attr("value", v[0]);
            title = $('<span></span>')
                .text(v[1]);
            label.append(check).append(title).appendTo(box2);
        }
    }
};
jQuery.fn.hsCate = function(opts) {
    return this.hsBind(HsCate, opts);
};
