
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

function getModeByName(name) {
    if (name) {
        return EXTN_TO_MODE[name.replace(/.*\./,'')];
    }
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

function forWriter(func) {
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

function setWriter(node, func) {
    forWriter(function() {
        node.each(function() {
            var ro = $(this).attr ("readonly");
                ro = !! ro;
            var md = $(this).data (  "mode"  )
                  || $(this).data (  "type"  );
                md = getModeByName( md ) || md;
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
            } else {
                cw.find( ".CodeMirror-scroll")
                  .height("auto");
                cw.height("auto");
            }
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
