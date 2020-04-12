/* global H$, HsLANG, CodeMirror */

//** 资源扩展功能 **/

H$.inst = function() {
    var context = $(".HsList,.HsForm").filter(":visible:first");
    return context.data("HsList")
        || context.data("HsForm");
};
H$.src  = function() {
    return (H$.inst()._url || location.pathname)
    //  .replace(/\#.*/, '')
        .replace(/\?.*/, '')
        .replace(/\/[^\/]+$/, '');
};
H$.load = function(req) {
    var mod = H$.inst();
    mod.load(undefined, hsSerialMix(mod._data, req));
};
H$.send = function(url, req) {
    var rzt;
    $.hsAjax({
        url     : hsFixUri   (url),
        data    : hsSerialArr(req),
        complete: function   (rst) {
            rzt = hsResponse (rst , 3);
        },
        type    : "post",
        dataType: "json",
        async   : false ,
        cache   : false ,
        global  : false
    });
    return  rzt ;
};
H$["search"] = function(req) {
    var url = H$.src() + "/search.act";
    return H$.send(url, req);
};
H$["create"] = function(req) {
    var url = H$.src() + "/create.act";
    return H$.send(url, req);
};
H$["update"] = function(req) {
    var url = H$.src() + "/update.act";
    return H$.send(url, req);
};
H$["delete"] = function(req) {
    var url = H$.src() + "/delete.act";
    return H$.send(url, req);
};

//** 初始化表单项 **/

function setFormItems(formbox, loadbox) {
    formbox.on("loadOver", function(evt, rst) {
        var inp;

        // 选择器
        inp = formbox.find("[data-toggle=dateinput],[data-toggle=datetimepicker]");
        if (inp.size()) forDateInput(   );
        inp = formbox.find("[data-toggle=suggest]");
        if (inp.size()) forSuggInput(   );
        inp = formbox.find("[data-toggle=fileinput]");
        if (inp.size()) setFileInput(inp);
        inp = formbox.find("[data-toggle=tagsinput]");
        if (inp.size()) setTagsInput(inp);

        // 编辑器
        inp = formbox.find("textarea[data-type=html]");
        if (inp.size()) setEditor(inp);
        inp = formbox.find("textarea[data-type=code]");
        if (inp.size()) setMirror(inp);
    });
    formbox.on("willSave", function(evt, dat) {
        synEditor(formbox.find("textarea[data-type]"));
        // 将同步后的结果加入到待保存数据
        if ( dat )formbox.find("textarea[data-type]")
            .each(function( ) {
                dat.set( $(this).attr("name"),
                         $(this).val (   )  );
            });
    });
    loadbox.on("hsClose" , function(evt, und) {
        desEditor(formbox.find("textarea[data-type]"));
    });
}

function setInfoItems(formbox, loadbox) {
    formbox.on("loadOver", function(evt, rst) {
        var inp;

        // 编辑器
        inp = formbox.find("textarea[data-type=code]");
        if (inp.size()) setMirror(inp);
        inp = formbox.find(/**/ "pre[data-type=code]");
        if (inp.size()) setMirror(inp);
    });
    loadbox.on("hsClose" , function(evt, und) {
        desEditor(formbox.find("textarea[data-type]"));
    });
}

//** 一般输入组件 **/

function forDateInput(func) {
    hsRequires([
        "static/addons/bootstrap-datetimepicker/css/datetimepicker.min.css",
        "static/addons/bootstrap-datetimepicker/datetimepicker.min.js"
    ] , func);
}

function forSuggInput(func) {
    hsRequires([
        "static/addons/bootstrap-suggest/suggest.min.js"
    ] , func);
}

function forFileInput(func) {
    hsRequires([
        "static/addons/bootstrap-fileinput/css/fileinput.min.css",
        "static/addons/bootstrap-fileinput/fileinput.min.js"
    ] , func);
}

function setFileInput(node, func) {
    forFileInput(function() {
        node.hsFileInput ();
        func && func.call(node);
    });
}

function forTagsInput(func) {
    hsRequires([
        "static/addons/bootstrap-tagsinput/css/tagsinput.min.css",
        "static/addons/bootstrap-tagsinput/tagsinput.min.js"
    ] , func);
}

function setTagsInput(node, func) {
    forTagsInput(function() {
        node.hsTagsInput ();
        func && func.call(node);
    });
}

//** 可视化编辑器 **/

function synEditor(node) {
    node.each(function() {
        if ($(this).data("synchro")) {
            $(this).data("synchro")();
        }
    });
}

function desEditor(node) {
    node.each(function() {
        if ($(this).data("destroy")) {
            $(this).data("destroy")();
        }
    });
}

function forEditor(func) {
    hsRequires([
        "static/addons/summernote/summernote.min.css",
        "static/addons/summernote/summernote.min.js"
    ] , function() {
        var lang  = HsLANG[ 'lang' ];
        if (lang && lang != 'en_US') {
            lang  = lang.replace('_', '-');
        } else {
            lang  = null;
        }
        if (lang ) {
            hsRequires([
                "static/addons/summernote/lang/summernote-"+lang+".js"
            ] , func );
        } else {
                func();
        }
    });
}

function setEditor(node, func) {
    forMirror(function() {
    forEditor(function() {
        node.each(function() {
            var that =   this ;
            var data = $(this).hsData() || {};
            var lang = HsLANG['lang'].replace('_', '-');
            var conf = {
                toolbar : [
                    ['font', ['color', 'bold'     , 'italic'  ]],
                    ['inst', ['table', 'picture'  , 'link'    ]],
                    ['para', ['style', 'paragraph', 'ul', 'ol']],
                    ['misc', ['clean', 'codeview']]
                ],
                buttons : {
                    "clean": function() {
                        return $.summernote.ui.button({
                            contents: '<i class="note-icon-eraser"></i>',
                            tooltip : $.summernote.lang[lang].font.clear,
                            click : function() {
                                $.hsMask({
                                    title: "富文本格式化工具",
                                    html : '<form onsubmit="return false">'
+ '<div class="form-group row">'
+ '<label class="col-xs-4 text-right control-label form-control-static">字体</label>'
+ '<div class="col-xs-6"><input class="form-control" type="text"   name="font-family" value="" data-unit="" list="hs-font-list">'
+ '<datalist id="hs-font-list">'
+ '<option value="宋体,STSong"  style="font-family: 宋体,STSong" >宋体</option>'
+ '<option value="黑体,STHeiti" style="font-family: 黑体,STHeiti">黑体</option>'
+ '<option value="楷体,STKaiti" style="font-family: 楷体,STKaiti">楷体</option>'
+ '</datalist></div>'
+ '</div>'
+ '<div class="form-group row">'
+ '<label class="col-xs-4 text-right control-label form-control-static">字号</label>'
+ '<div class="col-xs-4"><input class="form-control" type="number" name="font-size"   value="" data-unit="px" step="1"   min="0" max="36"></div>'
+ '<div class="col-xs-2 form-control-static">(px)</div>'
+ '</div>'
+ '<div class="form-group row">'
+ '<label class="col-xs-4 text-right control-label form-control-static">行高</label>'
+ '<div class="col-xs-4"><input class="form-control" type="number" name="line-height" value="" data-unit="em" step="0.1" min="0" max="8" ></div>'
+ '<div class="col-xs-2 form-control-static">(em)</div>'
+ '</div>'
+ '<div class="form-group row">'
+ '<label class="col-xs-4 text-right control-label form-control-static">缩进</label>'
+ '<div class="col-xs-4"><input class="form-control" type="number" name="text-indent" value="" data-unit="em" step="0.5" min="0" max="8" ></div>'
+ '<div class="col-xs-2 form-control-static">(em)</div>'
+ '</div>'
+ '<div class="form-group row">'
+ '<label class="col-xs-4 text-right control-label form-control-static">段尾间距</label>'
+ '<div class="col-xs-4"><input class="form-control" type="number" name="margin-bottom" value="" data-unit="px" step="1" min="0" max="80"></div>'
+ '<div class="col-xs-2 form-control-static">(px)</div>'
+ '</div>'
+ '<div class="form-group row">'
+ '<label class="col-xs-4 text-right control-label form-control-static">文本对齐</label>'
+ '<div class="col-xs-4"><select class="form-control" name="text-align" data-unit="">'
+ '<option value=""></option>'
+ '<option value="center" > 居中对齐</option>'
+ '<option value="justify"> 两端对齐</option>'
+ '</select>'
+ '</div></div>'
+ '<div class="form-group row">'
+ '<label class="col-xs-4 text-right control-label form-control-static">图片样式</label>'
+ '<div class="col-xs-8 radio checkbox">'
+ '<label><input type="checkbox" name="pics" data-float="" data-margin="0 auto" data-display="block"> 居中</label>'
+ '<label><input type="checkbox" name="picw" data-width="100%"> 宽 100%</label>'
+ '</div></div>'
+ '<div class="form-group row">'
+ '<label class="col-xs-4 text-right control-label form-control-static">应用范围</label>'
+ '<div class="col-xs-8 radio">'
+ '<label><input type="radio" name="area" value="range" checked="checked"> 选段</label>'
+ '<label><input type="radio" name="area" value="whole"> 全部</label>'
+ '<label><input type="radio" name="area" value="clean"> 清理</label>'
+ '</div></div>'
+ '</form>'
                                } , {
                                    label: "应用",
                                    glass: "btn-primary",
                                    click: function() {
                                        var form = $(this).closest(".modal-content").find("form");
                                        var area = form.find ("[name=area]:checked").val ( /**/ );
                                        var pics = {width:"", margin:"", display:""};
                                        var opts = {};
                                        var list ;
                                        // 清理内容数据
                                        switch (area) {
                                            case 'range':
                                                list = $(that).summernote("getLastRange").nodes();
                                                break;
                                            case 'whole':
                                                list = $(that).data("summernote").layoutInfo.editable;
                                                list = $(list).find("p,pre,div,ul,ol,h1,h2,h3,h4,h5,h6,table");
                                                break;
                                            case 'clean':
                                                var code ;
                                                code = $(that).summernote("code");
                                                if ( ! /<\w+[^>]+>/.test ( code )) {
                                                    code = code.replace(/^/g, '<p>');
                                                    code = code.replace(/$/g,'</p>');
                                                } else {
                                                    code = code.replace(/<!--[\S\s]*?-->/mg, '');
                                                    code = code.replace(/(<font(\s[^>]*)?>|<\/font>)/mg, '');
                                                    code = code.replace(/(<span(\s[^>]*)?>|<\/span>)/mg, '');
                                                    code = code.replace(/\s(style|class|align|color)=\"[\S\s]*?\"/mg, '');
                                                    // Office 特殊代码
                                                    code = code.replace('<o:p></o:p>' , '<br/>');
                                                }
                                                $(that).summernote("code", code );
                                                // 清理后设样式
                                                list = $(that).data("summernote").layoutInfo.editable;
                                                list = $(list).find("p,pre,div,ul,ol,h1,h2,h3,h4,h5,h6,table");
                                                break;
                                        }
                                        // 获取图片样式
                                        form.find("[name^=pic]").each(function() {
                                            if ($(this).prop("checked")) {
                                                $.extend( pics, $(this).data() );
                                            }
                                        });
                                        // 获取基本样式
                                        form.find("[data-unit]").each(function() {
                                            var v = $(this).val ( /**/ );
                                            var u = $(this).data("unit");
                                            var n = $(this).attr("name");
                                            if (v === "" || v === "0") {
                                                v = u = "" ;
                                            }
                                            opts[n] = v + u;
                                        });
                                        // 设置段落样式
                                        for(var i = 0; i < list.length; i ++) {
                                            var p = $(list[i]).closest("p,pre,div,h1,h2,h3,h4,h5,h6,ul,ol,table,blockquote,.note-editable");
                                            if (p.is(".note-editable")) {
                                                continue;
                                            }
                                            p.css(opts);
                                            var m = p.find("img");
                                            if (m.size( ) === 0 ) {
                                                continue;
                                            }
                                            m.css(pics);
                                            // 段首缩进不可用于标题/列表/表格
                                            if (opts ["text-indent"] && p.is("h1,h2,h3,h4,h5,h6,ul,ol,table")) {
                                                p.css("text-indent", "");
                                            }
                                        }
                                    }
                                } , {
                                    label: "取消",
                                    glass: "btn-default"
                                } );
                            }
                        }).render();
                    }
                },
                callbacks: {
                    onImageUpload: function(files) {
                        var data  = new FormData();
                        for(var i = 0 ; i < files.length ; i ++) {
                            data.append("file.",files[i]);
                        }
                        $.ajax({
                            url     : hsFixUri(ADD_IMG_HREF),
                            data    :  data ,
                            type    : "POST",
                            dataType: "json",
                            cache      : false,
                            contentType: false,
                            processData: false,
                            success    : function(data) {
                                data = hsResponse(data);
                                if ( ! data.ok ) return;
                                for(var i = 0 ; i < data.file.length ; i ++) {
                                    $(that).summernote("insertImage" , data.file[i]);
                                }
                            }
                        });
                    }
                },
                codemirror : {
                    mode   : "text/html",
                    htmlMode     : true ,
                    lineNumbers  : true ,
                    lineWrapping : true
                },
                styleTags  : ['p', 'h6','h5', 'h4','h3', 'h2','h1', 'pre','blockquote'],
                colorButton: {foreColor: '#FFFFFF', backColor: '#474949'},
                placeholder: $(this).attr("placeholder") ||"",
                minHeight  : $(this).height() * 1 ,
                maxHeight  : $(this).height() * 2 ,
                lang: lang
            };
            $(this).summernote( $.extend(conf, data) );

            // 默认无背景色
            $(this).siblings(".note-editor" )
                   .find(".note-current-color-button")
                   .attr( "data-backcolor", "inherit")
                   .find( "i" )
                   .css("background-color", "inherit");

            // 关闭时需销毁
            $(this).data("destroy",function() {
                $(that).summernote("destroy");
            });
        });
        func && func.call(node);
    });
    } , 'xml');
}

function forMirror(func, mode) {
    hsRequires([
        "static/addons/codemirror/codemirror.min.css",
        "static/addons/codemirror/codemirror.min.js"
    ] , function() {
        if (! $.isArray(mode)) {
            mode  = [mode];
        }
        var deps  = [/**/];
        for(var i = 0; i < mode.length; i ++) {
            deps.push("static/addons/codemirror/mode/"+mode[i]+"/"+mode[i]+".js");
        }

        if (deps.length) {
            hsRequires(
                deps  ,
                func );
        } else {
                func();
        }
    });
}

function setMirror(node, func) {
    var mods = [];
    node.each(function() {
        var mode = $(this).data ("mode")
              ||   $(this).data ("type");
            mode = getModeByName( mode );
        mods.push(mode[0]);
    });

    forMirror(function() {
        node.each(function() {
            var that = $(this);
            var read = $(this).prop ("readonly") && "nocursor";
            var mode = $(this).data ("mode")
                  ||   $(this).data ("type");
                mode = getModeByName( mode );

            if (that.is("textarea")) {
                var cm = CodeMirror.fromTextArea(that[0], {
                    mode        : mode[1],
                    lineNumbers : true,
                    readOnly    : read
                });

                var cw = $(cm.getWrapperElement());
                cw.addClass("form-control");
                cw.height  ( that.height());

                that.addClass ("invisible");
                that.data("CM", cm);
                that.data("destroy", function() {
                    cm.toTextArea();
                });
                that.data("synchro", function() {
                    cm.save(      );
                });
            } else {
                var cm = CodeMirror(that.parent()[0], {
                    mode        : mode[1],
                    lineNumbers : true,
                    readOnly    : "nocursor",
                    value       : that.text()
                });

                var cw = $(cm.getWrapperElement());
                cw.addClass("form-control-static");
                cw.height  ("auto");

                that.data("CM", cm);
                that.text( "" );
                that.hide(    );
            }
        });
        func && func.call(node);
    } , mods);
}

function getModeByName(name) {
    if (name) {
        name = name.replace(/.*\./, '');
        return EXTN_TO_MODE[name]
            || [name , name];
    } else {
        return [name , name];
    }
}

var ADD_IMG_HREF = hsChkUri('centra') ?
    "centra/data/upload/image/create.act" :
    "centre/data/upload/image/create.act" ;

var EXTN_TO_MODE = {
    /* C   家族 */
    h           : ["clike", "text/x-c"],
    c           : ["clike", "text/x-c"],
    cpp         : ["clike", "text/x-c++src"],
    cs          : ["clike", "text/x-csharp"],
    mm          : ["clike", "text/x-objectivec"],
    m           : ["clike", "text/x-objectivec"],
    /* JVM 家族 */
    java        : ["clike", "text/x-java"],
    scala       : ["clike", "text/x-scala"],
    kotlin      : ["clike", "text/x-kotlin"],
    groovy      : ["groovy", "groovy"],
    gradle      : ["groovy", "groovy"],
    /* JS  家族 */
    json        : ["javascript", "application/json"],
    js          : ["javascript", "text/javascript"],
    ts          : ["javascript", "text/typescrpit"],
    coffee      : ["coffee", "text/coffeescript"],
    jsx         : ["jsx", "jsx"],
    vue         : ["vue", "vue"],
    /* XML 家族 */
    xml         : ["xml", "xml"],
    xsd         : ["xml", "xml"],
    dtd         : ["dtd", "dtd"],
    html        : ["htmlmixed", "text/html"],
    htm         : ["htmlmixed", "text/html"],
    asp         : ["htmlembedded", "text/aspx"],
    jsp         : ["htmlembedded", "text/jsp" ],
    /* 其他程序 */
    go          : ["go", "go"],
    sql         : ["sql", "sql"],
    lua         : ["lua", "lua"],
    php         : ["php", "php"],
    rb          : ["ruby", "ruby"],
    py          : ["python", "python"],
    pl          : ["perl", "perl"],
    pm          : ["perl", "perl"],
    sh          : ["shell", "shell"],
    bat         : ["shell", "shell"],
    cmd         : ["shell", "shell"],
    ps1         : ["powershell", "powershell"],
    /* 其他代码 */
    css         : ["css", "css"],
    scss        : ["css", "text/x-scss"],
    less        : ["css", "text/x-less"],
    sass        : ["sass", "text/x-sass"],
    md          : ["markdown", "markdown"],
    proto       : ["protobuf", "protobuf"],
    ini         : ["properties", "properties"],
    cnf         : ["properties", "properties"],
    conf        : ["properties", "properties"],
    properties  : ["properties", "properties"]
};
