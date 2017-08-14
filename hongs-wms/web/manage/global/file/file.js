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
    js          : "javascript",
    ls          : "livescript",
    properties  : "properties",
    html        : "htmlmixed",
    htm         : "htmlmixed",
    jsp         : "htmlembedded",
    ejs         : "htmlembedded"
};

// 代码模式
function getModeByName(mod) {
    if (mod) {
        return EXTN_TO_MODE[mod.replace(/.*\./,'')];
    }
}

// 字符解码
function decodeUnicode(str) {
    // 注意: 如果出现 \\\u1234 就歇菜了
    str = str.replace (/\\u([0-9a-f]{4})/g,
    function ( a, n ) {
        return String.fromCharCode(parseInt(n, 16));
    });
    return str;
}

// 字符转码
function encodeUnicode(str) {
    var res = [];
    for(var i = 0; i < str.length; i ++ ) {
        var c = str.charCodeAt(i);
        if (c > 0x7F) {
            res[i] = "\\u"+c.toString(16);
        } else {
            res[i] = str[i];
        }
    }
    return res.join("");
}
