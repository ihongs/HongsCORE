var EXTN_TO_MODE = {
    html        : "htmlmixed",
    htm         : "htmlmixed",
    jsp         : "text/jsp",
    json        : "application/json",
    js          : "text/javascript",
    ts          : "text/typescript",
    jsx         : "jsx",
    vue         : "vue",
    css         : "css",
    xml         : "xml",
    xsd         : "xml",
    tld         : "xml",
    sql         : "sql",
    php         : "php",
    py          : "python",
    rb          : "ruby",
    pl          : "perl",
    sh          : "shell",
    bat         : "shell",
    cmd         : "shell",
    ps1         : "powershell",
    md          : "markdown",
    proto       : "protobuf",
    ini         : "properties",
    cnf         : "properties",
    properties  : "properties"
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
            res[i] = str [i];
        }
    }
    return res.join("");
}

function humanSize(size)
{
    var n, a = [];

    n = Math.floor(size / 1073741824);
    if (n > 0) {
        size = size % 1073741824;
        a.push(n + "G");
    }
    n = Math.floor(size / 1048576);
    if (n > 0) {
        size = size % 1048576;
        a.push(n + "M");
    }
    n = Math.floor(size / 1024);
    if (n > 0) {
        size = size % 1024;
        a.push(n + "K");
    }
    n = size;
    if (n > 0 || a.length == 0) {
        a.push(n);
    }

    return a.join (" ");
}