/* global HsLANG, CodeMirror */

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
    forEditor(function() {
        node.each(function() {
            var that =   this ;
            var data = $(this).hsData() || {};
            var lang = HsLANG['lang'].replace('_', '-');
            var conf = {
                toolbar : [
                    ['font', ['color', 'bold' , 'italic', 'fontsize']],
                    ['para', ['style', 'paragraph', 'ul', 'ol']],
                    ['inst', ['table', 'picture', 'link']],
                    ['misc', ['clean', 'codeview' ]]
                ],
                buttons : {
                    "clean": function() {
                        return $.summernote.ui.button({
                            contents: '<i class="note-icon-eraser"></i>',
                            tooltip : $.summernote.lang[lang].font.clear,
                            click : function() {
                                var code ;
                                code = $(that).summernote("code");
                                if ( ! /<\w+[^>]+>/.test ( code )) {
                                    code = code.replace(/^/g, '<p>');
                                    code = code.replace(/$/g,'</p>');
                                } else {
                                    code = code.replace(/<!--[\S\s]*?-->/mg, '');
                                    code = code.replace(/ (style|class)=\"[\S\s]*?\"/mg, '');
                                }
                                $(that).summernote("code", code );
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
                                    $(that).summernote("insertImage" , data.file[i] );
                                }
                            }
                        });
                    }
                },
                styleTags  : [ 'p' , 'h1', 'h2', 'h3', 'h4', 'h5', 'h6' ],
                colorButton: {foreColor: '#FFFFFF', backColor: '#474949'},
                placeholder: $(this).attr("placeholder") || "",
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
