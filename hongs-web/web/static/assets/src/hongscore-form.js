
/* global self, FormData */

/**
 * 表单组件
 * @param {Object} opts 选项
 * @param {Element} context 容器
 */
function HsForm(opts, context) {
    context = jQuery (context);
    context.data("HsForm", this);
    context.addClass( "HsForm" );

    var loadBox  = context.closest(".loadbox");
    var formBox  = context.find   ( "form"   );
    var loadUrl  = hsGetValue(opts, "loadUrl");
    var saveUrl  = hsGetValue(opts, "saveUrl");
    var loadDat  = hsGetValue(opts, "loadData");
    var initDat  = hsGetValue(opts, "initData");
    var idKey    = hsGetValue(opts, "idKey", "id"); // id参数名, 用于判断编辑还是创建
    var mdKey    = hsGetValue(opts, "mdKey", "md"); // md参数名, 用于判断是否要枚举表

    if (!formBox.length) formBox = context;

    this.context = context;
    this.loadBox = loadBox;
    this.formBox = formBox;
    this._url  = "";
    this._data = [];

    if (opts) for ( var k in opts ) {
        if ('_'===k.substring(0,1 )
        ||  this[k] !== undefined ) {
            this[k]  =  opts[k];
        } else
        if ('@'===k.substring(0,1)) {
            var n = k.substring(1);
            this.rules["[name="+n+"],[data-fn="+n+"]"] = opts[k];
        } else
        if ('$'===k.substring(0,1)) {
            this.rules[k.substring(1)] = opts[k];
        } else
        if (':'===k.substring(0,1)) {
            this.rmsgs[k.substring(1)] = opts[k];
        }
    }

    var loadArr = hsSerialArr(loadBox);
    if (loadUrl) {
        loadUrl = hsFixPms(loadUrl, loadArr);
    }
    if (saveUrl) {
        saveUrl = hsFixPms(saveUrl, loadArr);
    }

    /**
     * 使用初始化数据填充表单
     * 在打开表单窗口时, 可能指定一些参数(如父ID, 初始选中项等)
     * 这时有必要将这些参数值填写入对应的表单项, 方便初始化过程
     */
    initDat = initDat ? hsSerialArr(initDat)
          : ( loadDat ? hsSerialArr(loadDat)
          : ( loadArr ) );
    for(var i = 0; i < initDat.length; i ++) {
        var n = initDat[i].name ;
        var v = initDat[i].value;
        if (n === idKey && v === "0" ) continue ;
        formBox.find("[data-pn='"+n+"']").val(v);
        formBox.find("[data-fn='"+n+"']").val(v);
        formBox.find("[name='"+n+"']" ).not(".form-ignored").val(v);
    }

    /**
     * 如果存在 id 或 md 则进行数据加载
     * 否则调用 loadBack 进行选项初始化
     */
    if (loadUrl
    && (hsGetParam(loadUrl, idKey)
    ||  hsGetParam(loadUrl, mdKey))) {
        this.load (loadUrl, loadDat || loadArr );
    } else {
        this.loadBack({ });
    }

    this.valiInit(/*****/);
    this.saveInit(saveUrl);
}
HsForm.prototype = {
    load     : function(url, data) {
        if (url ) this._url  = url;
        if (data) this._data = hsSerialArr(data);
        jQuery.hsAjax({
            "url"       : this._url ,
            "data"      : this._data,
            "type"      : "POST",
            "dataType"  : "json",
            "funcName"  : "load",
            "async"     : false,
            "cache"     : false,
            "context"   : this,
            "success"   : this.loadBack
        });
    },
    loadBack : function(rst) {
        rst = hsResponObj(rst);
        if (rst["ok"] === false) return;

        this.formBox.trigger("loadBack", [rst, this]);

        if (rst["enum"]) this.fillEnum(rst["enum"]);
        if (rst["info"]) this.fillInfo(rst["info"]);
        else if (rst.list && rst.list[0]) {
            this.fillInfo (  rst.list[0]);  // 可能仅提供 list
        } else {
            this.fillInfo ( {} );           // 空也要执行 fill
        }

        this.formBox.trigger("loadOver", [rst, this]);
    },
    fillEnum : function(envm) {
        var fds, fns, fts, i, n, t, v;
        fds = this.formBox.find("[data-fn],select[name]")
                          .not ('.form-ignored');
        fns = {}; fts = {};
        for(i = 0; i < fds.length; i ++) {
            v = jQuery(fds[i]);
            n = v.attr("name");
            if (! n) {
                n  = v.attr("data-fn");
            }
            if (! n) {
                continue;
            }
            fns[n] = n;
            fts[n] = v.attr("data-ft");
        }

        this._enum =  envm;
        for (n in fns) {
                v  =  hsGetValue(envm, n);
            if (v === undefined) {
                v  =  envm[n];
            }

                i = this.formBox.find('[name="'   +n+'"]').not(".form-ignored");
            if (i.length === 0 ) {
                i = this.formBox.find('[data-fn="'+n+'"]').not(".form-ignored");
            }

            // 填充
            t = fts[n];
            if (n && this["_fill_"+n] !== undefined) {
                v  = this["_fill_"+n].call(this, i, v, n, "enum");
            } else
            if (t && this["_fill_"+t] !== undefined) {
                v  = this["_fill_"+t].call(this, i, v, n, "enum");
            }
            // 无值不理会
            if (!v && v !== 0 && v !== "") {
                continue;
            }

            if (i.is("select")) {
                this._fill__select(i, v, n, "enum");
            } else {
                this._fill__review(i, v, n, "enum");
            }
        }
        delete this._enum;
    },
    fillInfo : function(info) {
        var fds, fns, fts, fvs, fls, i, n, t, v, f;
        fds = this.formBox.find("[data-fn],input[name],select[name],textarea[name]")
                          .not ('.form-ignored');
        fns = {}; fts = {}; fvs = {}; fls = {};
        for(i = 0 ; i < fds.length ; i ++ ) {
            v = jQuery(fds[i]);
            n = v.attr("name");
            if (! n) {
                n  = v.attr("data-fn");
            }
            if (! n) {
                continue;
            }
            fns[n] = n;
            fts[n] = v.attr("data-ft");
            fvs[n] = v.data("fv");
            /**/f  = v.data("fl");

            // 解析填充方法
            if (f && typeof f != "function") {
                try {
                    f = eval('(function(form, v, n){return '+f+';})');
                } catch (e) {
                    throw new Error("Parse list data-fl error: " + e);
                }
                v.data("fl",f);
            }
            if (f) {
                fls [ n ] = f ;
            }
        }

        this._info =  info;
        for (n in fns) {
                v  =  hsGetValue(info, n);
            if (v === undefined) {
                v  =  info[n];
            }
            if (!  v  ) {
                v  =  fvs [n];
            }

                i = this.formBox.find('[name="'   +n+'"]').not(".form-ignored");
            if (i.length === 0 ) {
                i = this.formBox.find('[data-fn="'+n+'"]').not(".form-ignored");
                // 仅给了 data-fn 则此处可能仅需显示
                if (fts[n] === undefined) {
                    fts[n]  =  "_review";
                }
            }

            // 填充
            t = fts[n];
            f = fls[n];
            if (f) {
                v  = f.call(i, this, v, n);
            } else
            if (n && this["_fill_"+n] !== undefined) {
                v  = this["_fill_"+n].call(this, i, v, n, "info");
            } else
            if (t && this["_fill_"+t] !== undefined) {
                v  = this["_fill_"+t].call(this, i, v, n, "info");
            }
            // 无值不理会
            if (!v && v !== 0 && v !== "") {
                continue;
            }

            if (i.attr("type") == "checkbox"
            ||  i.attr("type") == "radio") {
                jQuery.each(! jQuery.isArray(v) ? [v] : v ,
                function(j, u) {
                    i.filter ("[value='"+u+"']")
                     .prop   ("checked" , true )
                     .change ( );
                });
            } else
            if (i.attr("type") == "file" ) {
                i.attr("data-value", v).change();
            } else
            if (i.is  ("input,select,textarea")) {
                i.not (":file").val( v).change();
            } else {
                i.text(v);
            }
        }
        delete this._info;
    },

    saveInit : function(act) {
        var url  = this.formBox.attr("action" ) || act;
        var type = this.formBox.attr("method" ) || "POST";
        var enct = this.formBox.attr("enctype") || "application/x-www-form-urlencoded; charset=UTF-8";
        var mult = /^multipart\/.*/i.test(enct);
        var data = this.formBox;
        var that = this;

        data.attr("action", hsFixUri( url ) );

        if ( mult && FormData === undefined ) {
            if (  ! data.attr(  "target"  ) ) {
                var name  = "_" + ( (new Date()).getTime() % 86400000 ) + "_" + Math.floor( Math.random( ) * 1000 );
                var style = "width:0; height:0; border:0; margin:0; padding:0; overflow:hidden; visibility:hidden;";
                var frame = jQuery('<iframe src="about:blank" name="' + name + '" style="' + style + '"></iframe>');
                data.append('<input type="hidden" name=".ajax" value="1"/>'); // 显式告知遵循 AJAX 方式
                data.attr("target", name).before(frame);
                frame.on ("load", function( ) {
                    var doc = frame[0].contentDocument || frame[0].contentWindow.document;
                    if (doc.location.href == "about:blank") return;
                    var rst = doc.body.innerHTML.replace( /(^<PRE.*?>|<\/PRE>$)/igm, '' );
                    that.saveBack(rst);
                });
            }
        } else {
            data.on("submit", function( evt ) {
                if (evt.isDefaultPrevented()) {
                    return;
                }
                evt.preventDefault();
                jQuery.hsAjax({
                    "url"         : url ,
                    "type"        : type,
                    "data"        : FormData === undefined || !mult ? data.serialize() : new FormData(data[0]),
                    "contentType" : FormData === undefined || !mult ? enct : false,
                    "processData" : FormData === undefined || !mult ? true : false,
                    "dataType"    : "json",
                    "funcName"    : "save",
                    "async"       : false,
                    "cache"       : false,
                    "global"      : false,
                    "context"     : that,
                    "complete"    : that.saveBack,
                    "error"       : function() { return false; }
                });
            });
        }
    },
    saveBack : function(rst) {
        rst = hsResponObj(rst, true);
        if (rst.ok) {
            var evt = jQuery.Event("saveBack");
            this.formBox.trigger(evt, [rst, this]);
            if (evt.isDefaultPrevented( )) return ;

            // 关闭窗口
            this.loadBox.hsClose(  );

            // 完成提示
            if ( rst.msg) {
                this.note(rst.msg, "succ");
            }
        } else {
            var evt = jQuery.Event("saveFail");
            this.formBox.trigger(evt, [rst, this]);
            if (evt.isDefaultPrevented( )) return ;

            // 错误提示
            if (rst.errs) {
                this.seterror ( rst.errs );
            } else
            if (!rst.msg) {
                 rst.msg= hsGetLang('error.unkwn');
            }
            if ( rst.msg) {
                this.warn(rst.msg, "warn");
            }
        }
    },

    note : function() {
        jQuery.hsNote.apply(self, arguments);
    },
    warn : function() {
        jQuery.hsWarn.apply(self, arguments);
    },

    _fill__review : function(inp, v, n, t) {
        // 枚举
        if (t === "enum" ) {
            inp.data("enum", v );
            return v;
        }
        var a = inp.data("enum");
        if (a) {
            var k = inp.attr("data-vk"); if (! k) k = 0;
            var t = inp.attr("data-tk"); if (! t) t = 1;
            var i, c, e, m = { };
            var x = inp.is("ul") ? '<li></li>' : '<span></span>';
            inp.empty().removeData("enum");
            if (! jQuery.isArray(v)) {
                v =  [v];
            }
            for(i = 0; i < a.length; i ++) {
                e = a[i];
                m[e[k]]=e[t];
            }
            for(i = 0; i < v.length; i ++) {
                c = v[i];
                e = m[v[i] ];
                inp.append(jQuery(x).text(e)).attr("data-code", c);
            }
            return;
        }

        // 标签
        if (inp.is("ul")) {
            var v = inp.attr("data-vk"); if (! k) k = 0;
            var t = inp.attr("data-tk"); if (! t) t = 1;
            var i, c, e;
            inp.empty();
            var x = '<li></li>';
            for(i = 0; i < v.length; i ++) {
                c = i;
                e = v[i];
                if ( jQuery.isPlainObject(e)
                ||   jQuery.isArray(e)) {
                    c = e[k];
                    e = e[t];
                }
                inp.append(jQuery(x).text(e)).attr("data-code", c);
            }
            return;
        }

        // 链接,图片,多媒体
        if (inp.is("img,audio,video")) {
            inp.attr("src" , v);
            return;
        }
        if (inp.is("object")) {
            inp.attr("data", v);
            return;
        }
        if (inp.is("a")) {
            inp.attr("href", v);
            return;
        }

        return v;
    },
    _fill__select : function(inp, v, n, t) {
        if (t !== "enum")  return v;
        var vk = inp.attr("data-vk"); if(!vk) vk = 0;
        var tk = inp.attr("data-tk"); if(!tk) tk = 1;
        for(var i = 0; i < v.length; i ++) {
            var k = hsGetValue(v[i], vk);
            if (k == '*') continue ; // * 总为其他
            var t = hsGetValue(v[i], tk);
            var opt = jQuery('<option></option>');
            opt.val(k).text(t).data("data", v[i]);
            inp.append(opt);
        }
        inp.change().click(); // multiple 必须触发 click 才初始化
    },
    _fill__radio : function(inp, v, n, t) {
        if (t !== "enum") return v;
        var vk = inp.attr("data-vk"); if(!vk) vk = 0;
        var tk = inp.attr("data-tk"); if(!tk) tk = 1;
        for(var i = 0; i < v.length; i ++) {
            var lab = jQuery('<label><input type="radio"/><span></span></label>');
            lab.find("input").attr("name", n).data(v[i])
                             .val (hsGetValue(v[i], vk));
            lab.find("span" ).text(hsGetValue(v[i], tk));
            inp.append(lab);
        }
        inp.find(":radio").first().change();
    },
    _fill__check : function(inp, v, n, t) {
        if (t !== "enum") return v;
        var vk = inp.attr("data-vk"); if(!vk) vk = 0;
        var tk = inp.attr("data-tk"); if(!tk) tk = 1;
        for(var i = 0; i < v.length; i ++) {
            var lab = jQuery('<label><input type="checkbox"/><span></span></label>');
            lab.find("input").attr("name", n).data(v[i])
                             .val (hsGetValue(v[i], vk));
            lab.find("span" ).text(hsGetValue(v[i], tk));
            inp.append(lab);
        }
        inp.find(":checkbox").first().change();
    },
    _fill__checkset : function(inp, v, n, t) {
        if (t !== "enum") return v;

        var vk = inp.attr("data-vk"); if(!vk) vk = 0;
        var tk = inp.attr("data-tk"); if(!tk) tk = 1;
        var vl = inp.attr("data-vl"); if(!vl) vl = 0; // Value List
        var tl = inp.attr("data-tl"); if(!tl) tl = 1; // Title Line
        var bc = this._fill__checkset_body_class || "checkbox";
        var ic = this._fill__checkset_item_class || "col-md-6";

        if (v !== undefined) {
        for(var i = 0; i < v.length; i ++) {
            var u = v[ i ][vl];
            var s = v[ i ][tl];
            var set = jQuery('<fieldset>'
                            +'<legend class="dropdown-toggle">'
                            +'<input type="checkbox" class="checkall dropdown-deny"/>'
                            +'&nbsp;<span></span><b class="caret"></b>'
                            +'</legend>'
                            +'<div class="dropdown-body '+bc+'"></div>'
                            +'</fieldset>');
            set.find("span").first().text(s);
            inp.append(set );
            set = set.find ( "div" );

            for(var j = 0; j < u.length; j ++) {
                var w = u[ j ];
                var lab = jQuery('<label class="'+ic+'"><input type="checkbox"/>'
                                +'<span></span></label>');
                lab.find("input").attr("name", n).data(w)
                                 .val (hsGetValue(w, vk));
                lab.find("span" ).text(hsGetValue(w, tk));
                set.append(lab);
            }
        }}

        inp.find(":checkbox").first().change();
        inp.hsReady();
    },

    _fill__htime : function(td, v, n) {
        if (v === undefined) return v;
        var d1  =  new Date ();
        var d2  =  hsPrsDate(v, hsGetLang("datetime.format"));
        if (d1.getFullYear() == d2.getFullYear()
        &&  d1.getMonth() == d2.getMonth()
        &&  d1.getDate( ) == d2.getDate()) {
            return hsGetLang("time.today", [
                   hsFmtDate(v, hsGetLang("time.format"))
            ]);
        }
        else {
            return hsFmtDate(v, hsGetLang("date.format"));
        }
    },
    _fill__hdate : function(td, v, n) {
        if (v === undefined) return v;
        var d1  =  new Date ();
        var d2  =  hsPrsDate(v, hsGetLang("date.format"));
        if (d1.getFullYear() == d2.getFullYear()
        &&  d1.getMonth() == d2.getMonth()
        &&  d1.getDate( ) == d2.getDate()) {
            return hsGetLang("date.today");
        }
        else {
            return hsFmtDate(v, hsGetLang("date.format"));
        }
    },
    _fill__datetime : function(td, v, n) {
        if (v === undefined) return v;
        return hsFmtDate(v, hsGetLang("datetime.format"));
    },
    _fill__date : function(td, v, n) {
        if (v === undefined) return v;
        return hsFmtDate(v, hsGetLang("date.format"));
    },
    _fill__time : function(td, v, n) {
        if (v === undefined) return v;
        return hsFmtDate(v, hsGetLang("time.format"));
    },
    _fill__html : function(td, v, n) {
        if (v === undefined) return v;
        td.html(v); return false;
    },

    valiInit : function() {
        var that = this;
        this.formBox.attr("novalidate", "novalidate");
        this.formBox.on("reset" , function(evt) {
            if (evt.isDefaultPrevented()) {
                return;
            }
            return that.verified( );
        });
        this.formBox.on("submit", function(evt) {
            if (evt.isDefaultPrevented()) {
                return;
            }
            return that.verifies( );
        });
        this.formBox.on("change","input,select,textarea,[data-fn]",
        function() {
            var item = jQuery(this);
            var name = item.attr( "name") || item.attr( "data-fn");
            if (name) {
                that.validate(name);
            }
        });
    },
    verified : function() {
        this.formBox.find(".form-group").removeClass( "has-error");
        this.formBox.find(".help-block").   addClass( "invisible");
        return true;
    },
    verifies : function() {
        this.verified();
        var vali = true;
        var inps = {  };
        this.formBox.find("input,select,textarea,[data-fn]")
                    .not (".form-ignored")
                    .each( function( ) {
            var inp = jQuery(this);
            var nam = inp.attr("name")||inp.attr("data-fn" );
            if (!nam) return true ;
            if (inps[nam] === undefined) {
                inps[nam] = inp;
            } else {
                inps[nam] = inps[nam].add(inp);
            }
        });
        for(var nam in inps ) {
            var val = this.validate(inps[nam]);
            if (val == false) {
                vali = false;
            }
        }
        if ( !  vali ) {
           this.warn(hsGetLang('form.invalid'), "warn");
        }
        return  vali;
    },
    validate : function(inp) {
        if (inp === undefined) {
            return this.verifies();
        }

        inp = this.getinput( inp );

        for(var key in this.rules) {
            if (!inp.is(key)) {
                continue;
            }
            var val  =  inp.val( );
            var err  =  this.rules[key].call(this, val, inp);
            if (err !== true) {
                err  =  err || hsGetLang ( "form.haserror" );
                this.seterror(inp, err);
                return false;
            } else {
                this.seterror(inp);
            }
        }
        return  true;
    },
    seterror : function(inp, err) {
        if (err === undefined && jQuery.isPlainObject(inp)) {
            this.verified();
            for (var n in inp) {
                 var e  = inp[n];
                this.seterror(n, e);
            }
            return;
        }

        inp = this.getinput(inp);

        var grp = inp.closest(".form-group");
        var blk = grp.find   (".help-block");
        if (blk.size() == 0) {
            blk = jQuery('<p class="help-block"></p>').appendTo(grp);
        }
        if (err===undefined) {
            grp.removeClass("has-error");
            blk.   addClass("invisible");
        } else {
            grp.   addClass("has-error");
            blk.removeClass("invisible");
            blk.text(err);
        }
    },
    geterror : function(inp, err, rep) {
        var msg = err.replace(/^form\./, "").replace(/\./g, "-");
            msg = inp.attr("data-" + msg + "-error" )
               || inp.attr("data-error");
        if (msg) {
            err = msg;
        }
        if (this.rmsgs[err]) {
            err = this.rmsgs[err];
        }

        // 放入字段标签
        var lab = inp.attr("data-label");
        if (lab == null) {
            lab = inp.closest(".form-group")
                     .find(".control-label")
                     .text();
        }
        if (lab) {
            if(jQuery.isArray(rep)) {
                var rap = {};
                for(var i = 0; i < rep.length; i ++) {
                    rap[i + "" ] = rep[i];
                }
                rep = rap;
            } else
            if (rep == null) {
                rep = { };
            }
            rep._ = hsGetLang(lab);
        }

        return hsGetLang(err, rep);
    },
    getinput : function(inp) {
        if (typeof inp != "string") {
            return jQuery(inp);
        }

        do {
            var sel = inp;

            inp = this.formBox.find('[name="'+sel+'"],[data-fn="'+sel+'"]');
            if (inp.size()) {
                break;
            }

            /**
             * 从字段名里提取数组下标
             * 并使用下标来查找表单项
             * 还找不到则尝试按集合名
             * 如: name. 和 name[]
             * 此查找方法主要为解决显示服务端多值字段校验的错误消息
             */

            var grp = /^(.*)\[(\d+)\]$/.exec(sel);
            if (grp == null) {
                break;
            }
            var idx = grp[2];
                sel = grp[1];

            inp = this.formBox.find('[name="'+sel+'"],[data-fn="'+sel+'"]');
            if (inp.size()) {
                return inp.eq(parseInt(idx)); // 精确位置, 直接返回
            }

            inp = this.formBox.find('[name="'+sel+'."],[data-fn="'+sel+'."]');
            if (inp.size()) {
                break;
            }

            inp = this.formBox.find('[name="'+sel+'[]"],[data-fn="'+sel+'[]"]');
            if (inp.size()) {
                break;
            }
        } while (false);

        return inp.not(".form-ignored");
    },
    rmsgs : {
    },
    rules : {
        "[required],[data-required]" : function(val, inp) {
            if (inp.is(":radio,:checkbox")) {
                if (!inp.filter(":checked").length) {
                    return this.geterror(inp, "form.requires");
                }
            } else if (inp.is(".checkbox")) {
                if (!inp. find (":checked").length) {
                    return this.geterror(inp, "form.requires");
                }
            } else if (inp.is(  "ul,div" )) {
                if (!inp. find (":hidden" ).length) {
                    return this.geterror(inp, "form.requires");
                }
            } else if (inp.is(  ":file"  )) {
                if (!val && !inp.data( "value" )  ) {
                    return this.geterror(inp, "form.requires");
                }
                // 选择区没有
                var  ipx = inp.filter(":hidden");
                var  box = inp.parent().closest(".file-input");
                if (!val && !ipx.size() && box.size()) {
                    return this.geterror(inp, "form.requires");
                }
            } else if (inp.is(  "select" )) {
                if (!val) {
                    return this.geterror(inp, "form.requires");
                }
            } else {
                if (!val) {
                    return this.geterror(inp, "form.required");
                }
            }
            return true;
        },
        "[pattern],[data-pattern]" : function(val, inp) {
            var pn = inp.attr("pattern") || inp.attr("data-pattern");
            var pm = /^\/(.*)\/([gim])?$/.exec(pn);
            if (pm) {
                pn = new RegExp(pm[1], pm[2]);
            } else {
                pn = new RegExp(pn);
            }
            if (! pn.test(val)) {
               return this.geterror(inp, "form.is.not.match");
            }
            return true;
        },
        "[maxlength],[data-maxlength]" : function(val, inp) {
            var max = inp.attr("maxlength") || inp.attr("data-maxlength");
            if (val.length > max) {
                return this.geterror(inp, "form.gt.maxlength", [max]);
            }
            return true;
        },
        "[minlength],[data-minlength]" : function(val, inp) {
            var min = inp.attr("minlength") || inp.attr("data-minlength");
            if (val.length < min) {
                return this.geterror(inp, "form.lt.minlength", [min]);
            }
            return true;
        },
        "[max],[data-max]" : function(val, inp) {
            var err =  this.rules["[type=number],.input-number"].call(this, val, inp);
            if (err!== true) {
                return err ;
            }
            var max = inp.attr( "max" ) || inp.attr("data-max");
            if (parseFloat(val) > parseFloat(max)) {
                return this.geterror(inp, "form.gt.max", [max]);
            }
            return true;
        },
        "[min],[data-min]" : function(val, inp) {
            var err =  this.rules["[type=number],.input-number"].call(this, val, inp);
            if (err!== true) {
                return err ;
            }
            var min = inp.attr( "min" ) || inp.attr("data-min");
            if (parseFloat(val) < parseFloat(min)) {
                return this.geterror(inp, "form.lt.min", [min]);
            }
            return true;
        },
        "[type=number],.input-number" : function(val, inp) {
            // 修复 webkit 下 number 中输入非数字时
            // 由于获取不到值从而导致无法校验的问题
            if (!val && inp[0] && inp[0].validity && inp[0].validity.badInput) {
                return this.geterror(inp, "form.is.not.number");
            }
            if (!/^-?[0-9]*(\.[0-9]+)?$/.test(val)) {
                return this.geterror(inp, "form.is.not.number");
            }
            return true;
        },
        "[type=email],.input-email" : function(val, inp) {
            if (!val) return true; // 规避非 required 的情况
            if (!/^([a-z0-9_\.\-\+]+)@([\da-z\.\-]+)\.([a-z\.]{2,6})$/i.test(val)) {
                return this.geterror(inp, "form.is.not.email");
            }
            return true;
        },
        "[type=url],.input-url" : function(val, inp) {
            if (!val) return true; // 规避非 required 的情况
            if (!/^(https?:\/\/)?[\da-z\.\-]+\.[a-z\.]{2,6}(:\d+)?(\/[^\s]*)?$/i.test(val)) {
                return this.geterror(inp, "form.is.not.url");
            }
            return true;
        },
        "[type=tel],.input-tel" : function(val, inp) {
            if (!val) return true; // 规避非 required 的情况
            if (!/^(\+\d{1,3})?\d{3,}$/i.test(val)) {
                return this.geterror(inp, "form.is.not.tel");
            }
            return true;
        },
        "[data-validate]" : function(val, inp) {
            var fn = inp.attr("data-validate");
            try {
                if (inp.data(fn)) {
                    return inp.data(fn).call(this, val, inp);
                } else
                if ( window [fn]) {
                    return  window [fn].call(this, val, inp);
                } else
                if ( window.console ) {
                    if (window.console.error) {
                        window.console.error(fn+" not found!");
                    } else {
                        window.console.log  (fn+" not found!");
                    }
                }
                return false;
            } catch (ex) {
                if ( window.console ) {
                    if (window.console.error) {
                        window.console.error(fn+" run error: "+ex, val, inp);
                    } else {
                        window.console.log  (fn+" run error: "+ex, val, inp);
                    }
                }
                return false;
            }
        },
        "[data-verify]" : function(val, inp, url) {
            if (! val) return true;
            var ret = true;
            var obj = this.formBox;
            var data = {
                "n" : inp.attr("name"),
                "v" : val
            };
            if (! url) url = inp.attr("data-verify");
            url = url.replace(/\$\{(.*?)\}/g,function(x, n) {
                return obj.find("[name='"+n+"']")
                          .not ( ".form-ignored" )
                          .val ( ) || "";
            });
            jQuery.hsAjax({
                "url": url,
                "data": data,
                "type": "POST",
                "dataType": "json",
                "funcName": "vali",
                "async": false,
                "cache": false,
                "context": this,
                "success": function(rst) {
                    if (rst["list"] !== undefined) {
                        ret = rst["list"].length > 0;
                    } else
                    if (rst["info"] !== undefined) {
                        ret = !jQuery.isEmptyObject(rst["info"]);
                    } else
                    if (rst["rows"] !== undefined) {
                        ret = rst["rows"] > 0 ? true :
                            ( rst["msg" ] ? rst["msg"] : false );
                    } else {
                        ret = rst[ "ok" ] ? true :
                            ( rst["msg" ] ? rst["msg"] : false );
                    }
                }
            });
            return ret;
        },
        "[data-unique]" : function(val, inp) {
            var ret = this.rules["[data-verify]"].call(this, val, inp, inp.attr("data-unique"));
            if (typeof ret === "string") {
                return ret;
            } else if (! ret) {
                return this.geterror(inp, "form.is.not.unique");
            }
            return true;
        },
        "[data-exists]" : function(val, inp) {
            var ret = this.rules["[data-verify]"].call(this, val, inp, inp.attr("data-exists"));
            if (typeof ret === "string") {
                return ret;
            } else if (! ret) {
                return this.geterror(inp, "form.is.not.exists");
            }
            return true;
        },
        "[data-repeat]" : function(val, inp) {
            var fn = inp.attr("data-repeat");
            var fd = this.formBox.find("[name="+fn+"],[data-fn="+fn+"]").not(".form-ignored");
            if (fd.val( ) != val) {
                return this.geterror(inp, "form.is.not.repeat");
            }
            return true;
        },
        "[data-relate]" : function(val, inp) {
            var fn = inp.attr("data-relate");
            var fd = this.formBox.find("[name="+fn+"],[data-fn="+fn+"]").not(".form-ignored");
            if (fd.val( ) != "" ) {
                this.validate(fn);
            }
            return true;
        }
    }
};

jQuery.fn.hsForm = function(opts) {
    return this._hsModule(HsForm, opts);
};

(function($) {
    $(document)
    .on("submit", "form",
    function(evt) {
        if (evt.isDefaultPrevented()) {
            return;
        }
        var btn = $(this).find( ":submit" );
        btn.prop("disabled", true );
        btn.data("txt", btn.text());
        btn.text(hsGetLang("form.sending"));
    })
    .on("saveBack saveFail", "form",
    function() {
        var btn = $(this).find( ":submit" );
        var txt = btn.data( "txt" );
        if (txt)  btn.text(  txt  );
        btn.prop("disabled", false);
    })
    .on("change", "fieldset .checkall",
    function(evt) {
        this.indeterminate = false;
        var box = $(this).closest("fieldset");
        var ckd = $(this).prop   ("checked" );
        box.find(":checkbox:not(.checkall)").prop("checked", ckd).trigger("change");
    })
    .on("change", "fieldset :checkbox:not(.checkall)",
    function(evt) {
        var box = $(this).closest("fieldset");
        var siz = box.find(":checkbox:not(.checkall)").length;
        var len = box.find(":checkbox:not(.checkall):checked").length;
        var ckd = siz && siz == len ? true : (len && siz != len ? null : false);
        box.find(".checkall").prop("choosed", ckd);
    })
    .on("click", "[data-toggle=hsEdit]",
    function(evt) {
        var that = $(this).closest(".HsForm").data("HsForm");
        var func = function() {
            $(this).on("saveBack", function(evt, rst, rel) {
                var ext = jQuery.Event("saveBack");
                ext.relatedTarget = evt.target;
                ext.relatedHsInst = rel /****/;
                that.formBox.trigger(ext, [rst, that]);
                if (ext.isDefaultPrevented( )) return ;

                that.load( );
            });
        };

        var box = $(this).attr("data-target");
        var url = $(this).attr("data-href");
            url = hsFixPms(url, this);
        if (box) {
            box = $(this).hsFind(box);
            box.hsOpen(url, func);
        } else {
              $.hsOpen(url, func);
        }
        evt.stopPropagation();
    });
})(jQuery);
