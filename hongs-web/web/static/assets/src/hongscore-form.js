
/**
 * 表单组件
 * @param {Object} opts 选项
 * @param {Element} context 容器
 */
function HsForm(context, opts) {
    context = jQuery (context);

    var loadBox  = context.closest(".loadbox");
    var pageBox  = context.find   (".pagebox");
    var formBox  = context.find   ( "form"   );
    var loadUrl  = hsGetValue(opts, "loadUrl");
    var saveUrl  = hsGetValue(opts, "saveUrl");
    var loadDat  = hsGetValue(opts, "loadData");
    var initDat  = hsGetValue(opts, "initData");
    var idKey    = hsGetValue(opts, "idKey", "id"); // id参数名, 用于判断编辑还是创建
    var abKey    = hsGetValue(opts, "abKey", "ab"); // ab参数名, 用于判断是否要枚举表

    if (!formBox.length) formBox = context;

    this.context = context;
    this.loadBox = loadBox;
    this.pageBox = pageBox;
    this.formBox = formBox;
    this._url  = "";
    this._data = [];
    this._opt_group_start = "#"; // 选项分组起始符

    if (opts) for ( var k in opts ) {
        if ('_'===k.substring(0,1 )
        ||  this[k] !== undefined ) {
            this[k]  =  opts[k];
        } else
        if ('@'===k.substring(0,1)) {
            var n = k.substring(1);
            this.rules["[name="+n+"]"] = opts[k];
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
        this.formBox.attr("action", saveUrl);
    }

    /**
     * 没有指定 id 尝试从加载参数中提取
     */
    if (! loadDat) {
        var id = hsGetParam(loadUrl , idKey);
        if (! id ) {
            id = hsGetSeria(loadArr , idKey);
            loadDat        = {};
            loadDat[idKey] = id;
        }
    }

    /**
     * 表单初始数据可以从加载参数中提取
     */
    if (initDat) {
        initDat = hsSerialDic(initDat);
    } else {
        initDat = hsSerialDic(  [ ]  );
        for(var i = 0; i < loadArr.length; i ++ ) {
            var n = loadArr [i];
            if (n.name && (n.value||n.value===0)) {
                var v = n.value;
                var k = n.name ;
                k = _hsGetDkeys (k);
                _hsSetPoint(initDat, k,v);
            }
        }
    }
    this._init = initDat;

    this.valiInit();
    this.saveInit();

    /**
     * 如果存在 id 或 ab 则进行数据加载
     * 否则调用 loadBack 进行选项初始化
     */
    if (loadUrl
    && (hsGetParam(loadUrl, idKey)
    ||  hsGetParam(loadUrl, abKey))) {
        this.load (loadUrl, loadDat);
    } else if (! this._url) {
        this.loadBack( {} );
    }
}
HsForm.prototype = {
    load     : function(url, data) {
        data =  hsSerialObj (data) ;
        if (url ) this._url  = url ;
        if (data) this._data = data;
        this.ajax({
            "url"      : this._url ,
            "data"     : this._data,
            "type"     : "POST",
            "dataType" : "json",
            "funcName" : "load",
            "cache"    : false,
            "global"   : false,
            "context"  : this,
            "complete" : this.loadBack
        });
    },
    loadBack : function(rst) {
        rst = hsResponse(rst);
        if (rst["ok"] === false) return;

        this.formBox.trigger("loadBack", [rst, this]);

        var page = rst["page"] || {};
        var enfo = rst["enfo"] || {};
        var info = rst["info"] ||
                 ( rst["list"] && rst.list[0] ) || {};

        // 填充预置数据
        if (this._init) {
            var d = hsSerialDic(this._init);
            for(var k in d ) {
                var v  = d [k];
            k = k.replace(/(\[\]|\.)$/, ''); // 规避数组键影响
                hsSetValue ( info , k , v );
            }
        }

        this.fillEnfo(enfo);
        this.fillInfo(info);
        this.fillPage(page);

        this.formBox.trigger("loadOver", [rst, this]);
    },
    fillEnfo : function(enfo) {
        var fds, fns, fts, fls, i, n, t, v, f;
        fds = this.formBox.find("[data-fn],input[name],select[name],textarea[name]")
                          .not ('.form-ignored');
        fns = { }; fts = { }; fls = { };
        for(i = 0; i < fds.length; i ++) {
            v = jQuery(fds[i]);
            n = v.data( "fn" ) || v.attr("name");
            t = v.data( "ft" );
            f = v.data( "dl" );
            if (! n || fns[n]) {
                continue;
            }
            fns[n] = n;
            fts[n] = t;

            // 解析填充方法
            if (f && typeof f != "function") {
                try {
                    f = eval('(null||function(form,v,n){return '+f+';})');
                } catch (e) {
                    throw new Error("Parse form data-dl error: "+e);
                }
                v.data("dl",f);
            }
            if (f) {
                fls [ n ] = f ;
            }
        }

        this._enfo =  enfo;
        for(n in fns) {
            t  = fts[n];
            f  = fls[n];
            v  = hsGetValue(enfo, n.replace(/(\[\]|\.)$/, '')); // 规避数组键影响
            if (v === undefined) {
                v  =  enfo [ n ];
            }

            // 获取当前待填充的节点
            // 非表单项可能仅需显示
                i = this.formBox.find('[data-fn="'+n+'"]').not(".form-ignored");
            if (i.length === 0 ) {
                i = this.formBox.find('[name="'   +n+'"]').not(".form-ignored");
            } else
            if (t === undefined) {
                t  =  "_review";
            }

            // 调节
            if (f) {
                v  = f.call(i[0], this, v, n);
            }
            // 填充
            if (n && this["_doll_"+n] !== undefined) {
                v  = this["_doll_"+n].call(this, i, v, n);
            } else
            if (t && this["_doll_"+t] !== undefined) {
                v  = this["_doll_"+t].call(this, i, v, n);
            }
            // 无值不理会
            if (!v && v !== 0 && v !== "") {
                continue;
            }

            if (i.is("select")) {
                this._doll__select(i, v, n);
            } else {
                this._doll__review(i, v, n);
            }
        }
        delete this._enfo;
    },
    fillInfo : function(info) {
        var fds, fns, fts, fls, i, n, t, v, f;
        fds = this.formBox.find("[data-fn],input[name],select[name],textarea[name]")
                          .not ('.form-ignored');
        fns = { }; fts = { }; fls = { };
        for(i = 0; i < fds.length; i ++) {
            v = jQuery(fds[i]);
            n = v.data( "fn" ) || v.attr("name");
            t = v.data( "ft" );
            f = v.data( "fl" );
            if (! n || fns[n]) {
                continue;
            }
            fns[n] = n;
            fts[n] = t;

            // 解析填充方法
            if (f && typeof f != "function") {
                try {
                    f = eval('(null||function(form,v,n){return '+f+';})');
                } catch (e) {
                    throw new Error("Parse form data-fl error: "+e);
                }
                v.data("fl",f);
            }
            if (f) {
                fls [ n ] = f ;
            }
        }

        this._info =  info;
        for(n in fns) {
            t  = fts[n];
            f  = fls[n];
            v  = hsGetValue(info, n.replace(/(\[\]|\.)$/, '')); // 规避数组键影响
            if (v === undefined) {
                v  =  info [ n ];
            }

            // 获取当前待填充的节点
            // 非表单项可能仅需显示
                i = this.formBox.find('[data-fn="'+n+'"]').not(".form-ignored");
            if (i.length === 0 ) {
                i = this.formBox.find('[name="'   +n+'"]').not(".form-ignored");
            } else
            if (t === undefined) {
                t  =  "_review";
            }

            // 调节
            if (f) {
                v  = f.call(i[0], this, v, n);
            }
            // 填充
            if (n && this["_fill_"+n] !== undefined) {
                v  = this["_fill_"+n].call(this, i, v, n);
            } else
            if (t && this["_fill_"+t] !== undefined) {
                v  = this["_fill_"+t].call(this, i, v, n);
            }
            // 无值不理会
            if (!v && v !== 0 && v !== "") {
                continue;
            }

            if (i.attr("type") === "file") {
                i.attr("data-value", v).change();
            } else
            if (i.is  ("input,select,textarea")) {
                i.val (v).change();
            } else {
                i.text(v);
            }
        }
        delete this._info;
    },
    fillPage : function(page) {
        var formBox = this.formBox;
        var pageBox = this.pageBox;
        if (page.state === undefined
        ||  page.state === null
        ||  page.state > 0) {
            formBox.show( );
            pageBox.hide( );
        }  else
        if (pageBox.size()) {
            formBox.hide( );
            pageBox.show( );

            // 显示警示框
            if (page.count && page.count > 0) {
                pageBox.empty().append('<div class="alert alert-warning" style="width: 100%;">'
                           + (this._above_err || hsGetLang('info.above')) + '</div>');
            } else {
                pageBox.empty().append('<div class="alert alert-warning" style="width: 100%;">'
                           + (this._empty_err || hsGetLang('info.empty')) + '</div>');
            }
        } else {
            if (page.count && page.count > 0) {
                jQuery.hsWarn(this._above_err || hsGetLang('info.above') , "warning");
            } else {
                jQuery.hsWarn(this._empty_err || hsGetLang('info.empty') , "warning");
            }
        }
    },

    saveInit : function() {
        var that = this;
        this.formBox.submit(function(evt) {
            if (evt.isDefaultPrevented()) {
                return ;
            }

            // 防止重复提交, 在 save/swap 的回调中移除
            if (that._waiting) {
                that.note (hsGetLang("form.waiting"));
                evt.preventDefault();
                return ;
            }   that._waiting = true;

            var data = that.formBox ;
            var url  = data.attr("action" ) ;
            var type = data.attr("method" ) || "POST";
            var enct = data.attr("enctype") || "None";

            if (/^.*\/json($|;| )/i.test( enct ) ) {
                evt.preventDefault();

                var dat = hsAsFormData( data [0] );
                var ext = jQuery.Event("willSave");
                data.trigger(ext, [ dat , that ] );
                if (ext.isDefaultPrevented()) {
                    delete that._waiting;
                    return;
                }

                that.save(url, dat, type, "json" );
            } else
            if (! /^multipart\/.*/i.test( enct ) ) {
                evt.preventDefault();

                var dat = hsAsFormData( data [0] );
                var ext = jQuery.Event("willSave");
                data.trigger(ext, [ dat , that ] );
                if (ext.isDefaultPrevented()) {
                    delete that._waiting;
                    return;
                }

                that.save(url, dat, type, "form" );
            } else
            if (window.FormData ) {
                evt.preventDefault();

                var dat = hsToFormData( data [0] );
                var ext = jQuery.Event("willSave");
                data.trigger(ext, [ dat , that ] );
                if (ext.isDefaultPrevented()) {
                    delete that._waiting;
                    return;
                }

                that.save(url, dat, type, "part" );
            } else
            {
                var dat = hsBeFormData( data [0] );
                var ext = jQuery.Event("willSave");
                data.trigger(ext, [ dat , that ] );
                if (ext.isDefaultPrevented()) {
                    evt.preventDefault();
                    delete that._waiting;
                    return;
                }

                that.swap(url, data);
            }
        });
    },
    saveBack : function(rst) {
        rst = hsResponse(rst, 1);
        if (rst.ok) {
            var evt = jQuery.Event("saveBack");
            this.formBox.trigger(evt, [rst, this]);
            if (evt.isDefaultPrevented( )) return ;

            // 关闭窗口
            this.loadBox.hsClose(  );

            // 完成提示
            if ( rst.msg ) {
                this.note(rst.msg, "success" );
            }
        } else {
            var evt = jQuery.Event("saveFail");
            this.formBox.trigger(evt, [rst, this]);
            if (evt.isDefaultPrevented( )) return ;

            // 字段校验
            this.seterror(rst.errs || { });

            // 错误提示
            if ( rst.msg ) {
                this.warn(rst.msg, "warning" );
            } else
            if (!rst.errs) {
                this.warn(hsGetLang('error.undef'));
            }
        }
    },

    swap : function(url, data) {
        if (! data.attr("target")) {
            var that  = this;
            var href  = hsSetParam (url, ".ajax", "1"); // 显式申明 AJAX 方式
            var name  = "_" + ( (new Date()).getTime() % 86400000 ) + "_" + Math.floor( Math.random( ) * 1000 );
            var style = "width:0; height:0; border:0; margin:0; padding:0; overflow:hidden; visibility:hidden;";
            var frame = jQuery('<iframe src="about:blank" name="' + name + '" style="' + style + '"></iframe>');

            frame.insertBefore( data );
            data.attr("target", name );
            data.attr("action", href );

            frame.on ( "load" , function() {
                var doc = frame[0].contentDocument || frame[0].contentWindow.document;
                if (doc.location.href==="about:blank") return ;
                var rst = doc.body.innerHTML.replace( /(^<PRE.*?>|<\/PRE>$)/igm, "" );
                delete that._waiting;
                that.saveBack( rst );
            } );
        }
    },
    save : function(url, data, type, kind) {
        this.ajax( {
            "url"       : url ,
            "data"      : data,
            "type"      : type,
            "dataKind"  : kind,
            "dataType"  : "json",
            "funcName"  : "save",
            "async"     : false,
            "cache"     : false,
            "global"    : false,
            "context"   : this,
            "trigger"   : this.formBox,
            "complete"  : function(rst) {
                delete this._waiting;
                this.saveBack( rst );
            }
        } );
    },

    ajax : function() {
        return jQuery.hsAjax.apply(window, arguments);
    },
    note : function() {
        return jQuery.hsNote.apply(window, arguments);
    },
    warn : function() {
        return jQuery.hsWarn.apply(window, arguments);
    },

    _doll__review : function(inp, v, n) {
        inp.data("data" , v);
    },
    _fill__review : function(inp, v, n) {
        if (v === undefined
        ||  v === null) {
            return;
        }

        // 枚举,列表,选项,标签
        if (inp.is("ul,ol,.repeated,.multiple")) {
            var a = inp.data("data") || [];
            var k = inp.attr("data-vk") || 0;
            var t = inp.attr("data-tk") || 1;
            var x = jQuery( inp.is("ul,ol") ? '<li></li>' : '<div></div>' );
            var m = { };
            var i, c, e;

            x.attr("class", inp.attr("data-item-class"));
            x.attr("style", inp.attr("data-item-style"));
            inp.empty( );

            if (! jQuery.isArray(v)) {
                v =  [v];
            }

            // 构建枚举映射
            for(i = 0; i < a.length; i ++) {
                c = a[i];
                m [ c[t] ]  =  c[k];
            }

            // 写入列表选项
            for(i = 0; i < v.length; i ++) {
                c = v[i];
                if (jQuery.isPlainObject (c)
                ||  jQuery.isArray (c) ) {
                    e = c[t];
                    c = c[k];
                } else {
                    e = m[c];
                    if (e === undefined) {
                        e = c;
                    }
                }

                x.text(/* label */  e);
                x.attr(  "title"  , e);
                x.attr("data-code", c);
                inp.append(x.clone( ));
            }

            return;
        }

        // 链接,图片,视频,音频
        if (inp.is("a,img,video,audio")) {
            var u = ! v ? v : hsFixUri(  v );
            inp.filter("a:empty").text(  v );
            inp.filter("a").attr("href", u );
            inp.filter("a.a-email").attr("href", "mailto:"+v);
            inp.filter("a.a-tel").attr("href", "tel:"+v);
            inp.filter("a.a-sms").attr("href", "sms:"+v);
            inp.filter("img,video,audio").attr("src", u);
            return;
        }

        if (! inp.is("input,select,textarea")) {
            v = this._fill__format(inp, v, n);
            if (jQuery.isArray(v)) {
                v = v.join( ", ");
            }
        }

        return  v ;
    },
    _fill__format : function(inp, v, n) {
        if (v === undefined) return v;
        var f = inp.data("format");
        if (f === undefined) return v;
        var a = [f] ;
        if (! jQuery.isArray (v) ) {
            v = [v] ;
        }
        Array.prototype.push.apply(a, v);
        return hsFormat.apply(window, a);
    },

    _doll__select : function(inp, v, n) {
        if (v === undefined) return ;
        var vk = inp.attr("data-vk"); if(!vk) vk = 0;
        var tk = inp.attr("data-tk"); if(!tk) tk = 1;
        for(var i = 0; i < v.length; i ++) {
            var k = hsGetValue(v[i], vk);
            var t = hsGetValue(v[i], tk);
            // 选项分组
            if (k && this._opt_group_start && k[0] == this._opt_group_start) {
                if (inp.is("optgroup")) {
                    inp = inp.parent();
                    if (! t) continue ;
                }
                inp = jQuery('<optgroup></optgroup>').appendTo(inp);
                inp.attr(     "label", t)
                   .attr("data-value", k);
                continue;
            }
            var opt = jQuery('<option></option>');
            opt.val(k).text(t).data("data", v[i]);
            inp.append(opt);
        }
        if (inp.closest(".form-group").hasClass("has-error")) {
            inp.change();
        }
    },

    _doll__radio : function(inp, v, n) {
        if (v === undefined) return ;
        var vk = inp.attr("data-vk"); if(!vk) vk = 0;
        var tk = inp.attr("data-tk"); if(!tk) tk = 1;
        for(var i = 0; i < v.length; i ++) {
            var k = hsGetValue(v[i], vk);
            var t = hsGetValue(v[i], tk);
            // 选项分组
            if (k && this._opt_group_start && k[0] == this._opt_group_start) {
                if (inp.is("fieldset")) {
                    inp = inp.parent();
                    if (! t) continue ;
                }
                var leg;
                inp = jQuery('<fieldset></fieldset>').appendTo(inp);
                leg = jQuery(  '<legend></legend>'  ).appendTo(inp);
                leg.text(              t);
                inp.attr("data-value", k);
                continue;
            }
            var lab = jQuery('<label><input type="radio"   /><span></span></label>');
            lab.find("input"). val (k).attr("name", n).data("data", v[i]);
            lab.find("span" ).text (t);
            inp.append(lab);
        }
        if (inp.closest(".form-group").hasClass("has-error")) {
            inp.find("input").first( ).change();
        }
    },
    _fill__radio : function(inp, v, n) {
        if (v ===  undefined  ) {
            return ;
        }
        if (!jQuery.isArray(v)) {
            v = [v];
        }
        inp.find(":radio"   ).val(v).first().change();
    },

    _doll__check : function(inp, v, n) {
        if (v === undefined) return ;
        var vk = inp.attr("data-vk"); if(!vk) vk = 0;
        var tk = inp.attr("data-tk"); if(!tk) tk = 1;
        for(var i = 0; i < v.length; i ++) {
            var k = hsGetValue(v[i], vk);
            var t = hsGetValue(v[i], tk);
            // 选项分组
            if (k && this._opt_group_start && k[0] == this._opt_group_start) {
                if (inp.is("fieldset")) {
                    inp = inp.parent();
                    if (! t) continue ;
                }
                var leg;
                inp = jQuery('<fieldset></fieldset>').appendTo(inp);
                leg = jQuery(  '<legend></legend>'  ).appendTo(inp);
                leg.text(              t);
                inp.attr("data-value", k);
                continue;
            }
            var lab = jQuery('<label><input type="checkbox"/><span></span></label>');
            lab.find("input"). val (k).attr("name", n).data("data", v[i]);
            lab.find("span" ).text (t);
            inp.append(lab);
        }
        if (inp.closest(".form-group").hasClass("has-error")) {
            inp.find("input").first( ).change();
        }
    },
    _fill__check : function(inp, v, n) {
        if (v ===  undefined  ) {
            return ;
        }
        if (!jQuery.isArray(v)) {
            v = [v];
        }
        inp.find(":checkbox").val(v).first().change();
    },

    _doll__checkset : function(inp, v, n) {
        if (v === undefined) return ;
        var vk = inp.attr("data-vk"); if(!vk) vk = 0;
        var tk = inp.attr("data-tk"); if(!tk) tk = 1;
        var vl = inp.attr("data-vl"); if(!vl) vl = 0; // Value List
        var tl = inp.attr("data-tl"); if(!tl) tl = 1; // Title Line
        var bc = inp.attr("data-body-class") || "checkbox";
        var ic = inp.attr("data-item-class") || "col-xs-6";

        for(var i = 0; i < v.length; i ++) {
            var u = v[ i ][vl];
            var s = v[ i ][tl];
            var set = jQuery('<fieldset>'
                            +'<legend data-toggle="hsDrop">'
                            +'<input  type="checkbox" class="checkall"/>'
                            +'<b></b> <i class="caret"></i>'
                            +'</legend>'
                            +'<div class="dropdown-body clearfix"></div>'
                            +'</fieldset>');
            inp.append(set);
            set.find("b").text(s);
            set = set.find("div")
                . addClass( bc  );

            for(var j = 0; j < u.length; j ++) {
                var w = u[ j ];
                var lab = jQuery('<label class="'+ ic +'">'
                                +'<input type="checkbox"/>'
                                +'<span></span></label>');
                lab.find("input").attr("name", n).data(w)
                                 .val (hsGetValue(w, vk));
                lab.find("span" ).text(hsGetValue(w, tk));
                set.append(lab);
            }
        }

        if (inp.closest(".form-group").hasClass("has-error")) {
            inp.find("input").first( ).change();
        }

        inp.hsReady();
    },
    _fill__checkset : function(inp, v, n) {
        if (v === undefined ) return;
        inp.find(":checkbox"). not(".checkall").val( v );
        inp.find( "fieldset").each( function( ) {
            var box = $(this);
            var siz = box.find(":checkbox:not(.checkall)"        ).length;
            var len = box.find(":checkbox:not(.checkall):checked").length;
            var ckd = siz && siz === len ? true
                  : ( len && siz !== len ? null : false);
            box.find(".checkall").prop( "choosed", ckd );
        });
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
        return hsFmtDate(v, td.data("format") || hsGetLang("datetime.format"));
    },
    _fill__date : function(td, v, n) {
        if (v === undefined) return v;
        return hsFmtDate(v, td.data("format") || hsGetLang("date.format"));
    },
    _fill__time : function(td, v, n) {
        if (v === undefined) return v;
        return hsFmtDate(v, td.data("format") || hsGetLang("time.format"));
    },
    _fill__html : function(td, v, n) {
        if (v === undefined) return v;
        jQuery(td).html( v );return false;
    },
    _fill__text : function(td, v, n) {
        if (v === undefined) return v;
        jQuery(td).text( v );return false;
    },

    valiInit : function() {
        var that = this;
        this.formBox.attr("novalidate", "novalidate");
//      this.formBox.prop("novalidate", true); // 无效
        this.formBox.on("reset" , function(e) {
            if (e.isDefaultPrevented()) {
                return;
            }
            return that.verified( );
        });
        this.formBox.on("submit", function(e) {
            if (e.isDefaultPrevented()) {
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
        this.formBox.find(".form-group")
              .removeClass( "has-error")
                    .find(".text-error")
                    .empty();
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
            var nam = inp.attr("name")||inp.attr("data-fn");
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
           this.warn(hsGetLang('form.invalid'), "warning");
        }
        return  vali;
    },
    validate : function(inp) {
        if (inp === undefined) {
            return this.verifies();
        }

        var val;
        inp = this.getinput( inp );
        val = this.getvalue( inp );

        for(var key in this.rules) {
            if (!inp.is(key)) {
                continue;
            }
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
        var blk = grp.find(   ".text-error");
        var leb = grp.find(".control-label");
        if (blk.size() == 0) {
            blk = jQuery('<p class="text-error help-block"></p>').appendTo(grp);
            if (leb.hasClass(".form-control-static")) {
                blk.addClass( "form-control-static" );
            }
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
        var msg = err.replace(/^form\./, "")
                     .replace( /\./g , "-" );
            msg = inp.attr("data-" + msg + "-error")
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
        }
        while ( false );

        return inp.not(".form-ignored");
    },
    getvalue : function(inp) {
        if (inp.is("input,select,textarea") === false) {
            inp = inp.find("input");
        }

        if (inp.is(":checkbox")) {
            var val = [];
            inp.filter(":checked")
               .each(function( ) {
                val.push($(this).val( ) );
            });
            return val;
        }
        if (inp.is(":radio"   )) {
            return inp.filter(":checked")
                      .last()
                      .val ();
        }

        // 以 . 结尾, 含有 .. 或 [] 均表示为多个值
        if (/(\[\]|\.\.|\.$)/.test(inp.attr("name"))) {
            var val = [];
            inp.each(function( ) {
                val.push($(this).val( ) );
            });
            return val;
        } else {
            return inp.last()
                      .val ();
        }
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
            } else if (inp.is("ul,div")) {
                if (!inp. find ( "input" ) .length) {
                    return this.geterror(inp, "form.requires");
                }
            } else if (inp.is(":file" )) {
                if (!val && !inp.data( "value" )) {
                    return this.geterror(inp, "form.requires");
                }
                // 选择区没有
                var  ipx = inp.filter(":hidden");
                var  box = inp.parent().closest(".file-input");
                if (!val && !ipx.size() && box.size()) {
                    return this.geterror(inp, "form.requires");
                }
            } else if (inp.is("select")) {
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
            if (max < val.length) {
                return this.geterror(inp, "form.gt.maxlength", [max]);
            }
            return true;
        },
        "[minlength],[data-minlength]" : function(val, inp) {
            var min = inp.attr("minlength") || inp.attr("data-minlength");
            if (min > val.length) {
                return this.geterror(inp, "form.lt.minlength", [min]);
            }
            return true;
        },
        "[maxrepeat],[data-maxrepeat]" : function(val, inp) {
            var max = inp.attr("maxrepeat") || inp.attr("data-maxrepeat");
            if (max < inp.find("[type=hidden],:checked,:selected,:file,:text").size()) {
                return this.geterror(inp, "form.gt.maxrepeat", [max]);
            }
            return true;
        },
        "[minrepeat],[data-minrepeat]" : function(val, inp) {
            var min = inp.attr("minrepeat") || inp.attr("data-minrepeat");
            if (min > inp.find("[type=hidden],:checked,:selected,:file,:text").size()) {
                return this.geterror(inp, "form.lt.minrepeat", [min]);
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
        "[type=color],.input-color" : function(val, inp) {
            if (!val) return true; // 规避非 required 的情况
            if (!/^#[0-9A-Fa-f]{6}$/.test(val)) {
                return this.geterror(inp, "form.is.not.color");
            }
            return true;
        },
        "[type=email],.input-email" : function(val, inp) {
            if (!val) return true; // 规避非 required 的情况
            if (!/^\w+([-.]\w+)*@\w+([-.]\w+)*$/.test(val)) {
                return this.geterror(inp, "form.is.not.email");
            }
            return true;
        },
        "[type=url],.input-url" : function(val, inp) {
            if (!val) return true; // 规避非 required 的情况
            if (!/^([a-z]+:)?\/\/[^\s]+$/.test(val)) {
                return this.geterror(inp, "form.is.not.url");
            }
            return true;
        },
        "[type=tel],.input-tel" : function(val, inp) {
            if (!val) return true; // 规避非 required 的情况
            if (!/^\+?[0-9][\d\-]+[0-9]$/.test(val)) {
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
            if (! val) return true ;
            var ret  = true;
            var obj  = this.formBox;
            var key  = inp.attr("name") || inp.attr("data-fn");

            // 清理多值名称
            key = key.replace(".." , ".")
                     .replace("[]" , "" )
                     .replace(/\.$/, "" );

            // 补齐请求参数
            if (! url) url = inp.attr("data-verify");
            url = url.replace(/\$\{(.*?)\}/g, function( x, n ) {
                return encodeURIComponent(
                       obj.find("[name='"+n+"']")
                          .not (".form-ignored" )
                          .val () || "" );
            });

            this.ajax({
                "url"     : url ,
                "data"    : { n : key , v : val },
                "type"    : "POST",
                "dataType": "json",
                "funcName": "veri",
                "async"   : false,
                "cache"   : false,
                "global"  : false,
                "context" : this,
                "complete": function(rst) {
                    rst = hsResponse(rst);
                    if (rst["list"] !== undefined) {
                        ret = inp.attr("data-unique")
                            ? rst["list"].length == 0
                            : rst["list"].length != 0;
                    } else
                    if (rst["page"] !== undefined) {
                        ret = inp.attr("data-unique")
                            ? rst["page"].count  == 0
                            : rst["page"].count  != 0;
                    } else
                    if (rst[ "rn" ] !== undefined) {
                        rst[ "rn" ]  =  parseInt( rst[ "rn" ] );
                        ret = rst["rn" ] ? true
                          : ( rst["msg"] ? rst["msg"] : false );
                    } else {
                        ret = rst["ok" ] ? true
                          : ( rst["msg"] ? rst["msg"] : false );
                    }
                },
                "error" : function() {
                    ret = false;
                }
            });
            return  ret;
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
            if (val != this.getvalue(this.getinput(fn))) {
                return this.geterror(inp, "form.is.not.repeat");
            }
            return true;
        },
        "[data-relate]" : function(val, inp) {
            var fn = inp.attr("data-relate");
            if (this.getvalue(this.getinput(fn))) {
                this.validate(fn);
            }
            return true;
        }
    }
};

jQuery.fn.hsForm = function(opts) {
    return this.hsBind(HsForm, opts);
};

(function($) {
    $(document)
    .on("change", "fieldset .checkall",
    function(evt) {
        this.indeterminate = false;
        var box = $(this).closest("fieldset");
        var ckd = $(this).prop   ("checked" );
        box.find(":checkbox"    )
           .not (".checkall"    )
           .prop( "checked", ckd)
           .trigger(  "change"  );
    })
    .on("change", "fieldset :checkbox:not(.checkall)",
    function(evt) {
        var box = $(this).closest("fieldset");
        var siz = box.find(":checkbox:not(.checkall)"        ).length;
        var len = box.find(":checkbox:not(.checkall):checked").length;
        var ckd = siz && siz === len ? true
              : ( len && siz !== len ? null : false);
        box.find(".checkall"    )
           .prop( "choosed", ckd);
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
        var url = $(this).attr("data-href"  );
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
