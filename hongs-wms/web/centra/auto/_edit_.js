
/* global UE, CodeMirror */

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
        inp = formbox.find("textarea[data-type=mark]");
        if (inp.size()) setMarker(inp);
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
    loadbox.on("hsClose" , function(evt, dat) {
        desEditor(formbox.find("textarea[data-type]"));
    });
}

//** 一般输入组件 **/

function forDateInput(func) {
    hsRequires([
        "static/addons/bootstrap-datetimepicker/datetimepicker.min.js",
        "static/addons/bootstrap-datetimepicker/css/datetimepicker.min.css"
    ] , func);
}

function forSuggInput(func) {
    hsRequires([
        "static/addons/bootstrap-suggest/suggest.min.js"
    ] , func);
}

function forFileInput(func) {
    hsRequires([
        "static/addons/bootstrap-fileinput/fileinput.min.js",
        "static/addons/bootstrap-fileinput/css/fileinput.min.css"
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
        "static/addons/bootstrap-tagsinput/tagsinput.min.js",
        "static/addons/bootstrap-tagsinput/css/tagsinput.min.css"
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
    hsRequired([
        "centra/editor/config.js",
        "static/addons/ueditor/ueditor.all.min.js"
    ] , func);
}

function setEditor(node, func) {
    forEditor(function() {
        node.each(function() {
            var id = $(this).attr("id");
            var ue = UE.getEditor( id );
            $(this).data("UE", ue);
            $(this).data("destroy", function() {
                ue .destroy(  );
            });
        });
        func && func.call(node);
    });
}

function forMarker(func) {

}

function setMarker(node, func) {

}

function forMirror(func) {
    hsRequires([
        "static/addons/codemirror/codemirror.js",
        "static/addons/codemirror/codemirror.css"
    ] , function() {
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
            "static/addons/codemirror/mode/htmlmixed/htmlmixed.js",
            "static/addons/codemirror/mode/multiplex/multiplex.js",
            "static/addons/codemirror/mode/htmlembedded/htmlembedded.js"
        ] , func);
    });
}

function setMirror(node, func) {
    forMirror(function() {
        node.each(function() {
            var ro = $(this).attr ("readonly");
                ro = !! ro;
            var md = $(this).data (  "mode"  )
                  || $(this).data (  "type"  );
                md = getModeByName(    md    );
            var cm = CodeMirror.fromTextArea(this, {
                lineNumbers : true,
                readOnly : ro,
                mode : md
            });

            // 容器高度
            var cw = $(cm.getWrapperElement());
            var ht = $(this).height();
            if (ht) {
                cw.height(  ht  );
            } else  {
                cw.find( ".CodeMirror-scroll")
                  .height("auto");
                cw.height("auto");
            }
            cw.css( {
                border  : "1px solid #d4d4d4",
               "border-radius": "4px"
            });

            $(this).css({
                height  : "0",
                border  : "0",
                margin  : "0",
                padding : "0",
                overflow: "hidden"
            });
            $(this).data("CM", cm);
            $(this).data("destroy", function() {
                cm .toTextArea(  );
            });
            $(this).data("synchro", function() {
                cm .save(  );
            });
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
