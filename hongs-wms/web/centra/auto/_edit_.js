
/* global HsLANG, CodeMirror */

function setFormItems(formbox, loadbox) {
    formbox.on("loadOver", function(evt, rst) {
        var inp;

        // 选择器
        inp = formbox.find("[data-toggle=datetimepicker]");
        if (inp.size()) forDateInput(inp);
        inp = formbox.find("[data-toggle=suggest]");
        if (inp.size()) forSuggInput(inp);
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
                dat.set($(this).attr("name"),
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
    ],function() {
    if (HsLANG['lang'] != 'en_US')
    hsRequires([
        "static/addons/summernote/lang/summernote-"+HsLANG['lang'].replace('_', '-')+".js"
    ] , func );
    else
        func();
    });
}

function setEditor(node, func) {
    forEditor(function() {
        node.each(function() {
            $(this).summernote({
                toolbar: [
                    ['base', ['bold' , 'italic' , 'underline' , 'strikethrough']],
                    ['font', ['color', 'height' , 'fontsize']],
                    ['para', ['paragraph', 'ul' , 'ol'  ]],
                    ['list', ['table', 'picture', 'link']]
                ],
                height: $(this).height(),
                lang: HsLANG['lang'].replace('_', '-')
            });

            // 默认无背景色
            $(this).siblings(".note-editor")
                   .find(".note-current-color-button")
                   .attr( "data-backcolor", "inherit")
                   .find( "i" )
                   .css("background-color", "inherit");

            // 关闭时需销毁
            var that = this;
            $(this).data("destroy",function() {
                $(that).summernote("destroy");
            });
        });
        func && func.call(node);
    });
}

function forMirror(func) {
    hsRequires([
        "static/addons/codemirror/codemirror.min.js",
        "static/addons/codemirror/codemirror.min.css"
    ],function() {
    hsRequires([
        "static/addons/codemirror/mode/css/css.js",
        "static/addons/codemirror/mode/jsx/jsx.js",
        "static/addons/codemirror/mode/xml/xml.js",
        "static/addons/codemirror/mode/sql/sql.js",
        "static/addons/codemirror/mode/lua/lua.js",
        "static/addons/codemirror/mode/shell/shell.js",
        "static/addons/codemirror/mode/python/python.js",
        "static/addons/codemirror/mode/groovy/groovy.js",
        "static/addons/codemirror/mode/markdown/markdown.js",
        "static/addons/codemirror/mode/protobuf/protobuf.js",
        "static/addons/codemirror/mode/properties/properties.js",
        "static/addons/codemirror/mode/javascript/javascript.js",
        "static/addons/codemirror/mode/livescript/livescript.js",
        "static/addons/codemirror/mode/multiplex/multiplex.js",
        "static/addons/codemirror/mode/htmlmixed/htmlmixed.js",
        "static/addons/codemirror/mode/htmlembedded/htmlembedded.js"
    ] , func );
    });
}

function setMirror(node, func) {
    forMirror(function() {
        node.each(function() {
            var cm ;
            var md = $(this).data ("mode")
                  || $(this).data ("type");
                md = getModeByName(  md  );
            if ($( this ).is("textarea") ) {
                cm = CodeMirror.fromTextArea(this, {
                     mode       : md,
                     lineNumbers: true,
                     readOnly   : $(this).prop("readonly") && "nocursor"
                });

                var cw = $(cm.getWrapperElement());
                cw.addClass( "form-control" );
                cw.height  ($(this).height());

                $(this).addClass("invisible");
                $(this).data("CM", cm);
                $(this).data("destroy", function() {
                    cm .toTextArea(  );
                });
                $(this).data("synchro", function() {
                    cm .save(  );
                });
            } else {
                cm = CodeMirror($(this).parent()[0], {
                     mode       : md,
                     lineNumbers: true,
                     readOnly   : "nocursor",
                     value      : $(this).text()
                });

                var cw = $(cm.getWrapperElement());
                cw.addClass("form-control-static");
                cw.height  ("auto");

                $( this ).remove( );
            }
        });
        func && func.call(node);
    });
}

function getModeByName(name) {
    if (name) {
        name = name.replace(/.*\./ , '' );
        return EXTN_TO_MODE[name] || name;
    } else {
        return name;
    }
}

var EXTN_TO_MODE = {
    jsx         : "jsx",
    css         : "css",
    xml         : "xml",
    xsd         : "xml",
    tld         : "xml",
    sql         : "sql",
    lua         : "lua",
    sh          : "shell",
    py          : "python",
    groovy      : "groovy",
    md          : "markdown",
    protobuf    : "protobuf",
    json        : "javascript",
    js          : "javascript",
    ls          : "livescript",
    properties  : "properties",
    html        : "htmlmixed",
    htm         : "htmlmixed",
    jsp         : "htmlembedded",
    ejs         : "htmlembedded"
};
