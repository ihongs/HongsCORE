var MODE_MAP = {
    jsx         : "jsx",
    css         : "css",
    xml         : "xml",
    xsd         : "xml",
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
    mod = mod.replace(/.*\./, '');
    return MODE_MAP[mod];
}

// 字符解码
function decodeUnicode(str) {
    return JSON.parse ('"'+str+'"')
               .substr(  1  , -1  );
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
