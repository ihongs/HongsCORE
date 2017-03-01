
/* global self, eval, Element, encodeURIComponent, decodeURIComponent */

if (typeof(HsAUTH) === "undefined") HsAUTH = {};
if (typeof(HsCONF) === "undefined") HsCONF = {};
if (typeof(HsLANG) === "undefined") HsLANG = {};

/**
 * 快捷方式
 * 说明(首参数以下列字符开头的意义):
 * .    获取配置
 * :    获取语言
 * ?    检查权限
 * /    补全路径
 * &    获取单个参数值, 第二个参数指定参数容器
 * @    获取多个参数值, 第二个参数指定参数容器
 * $    获取/设置会话存储, 第二个参数存在为设置, 第二个参数为null则删除
 * %    获取/设置本地存储, 第二个参数存在为设置, 第二个参数为null则删除
 * @return {Mixed} 根据开头标识返回不同类型的数据
 */
function H$() {
    var a = arguments[0];
    var b = a.charAt (0);
    arguments[0] = a.substring(1);
    switch (b) {
    case '.': return hsGetConf.apply(this, arguments);
    case ':': return hsGetLang.apply(this, arguments);
    case '?': return hsChkUri .apply(this, arguments);
    case '/': return hsFixUri .apply(this, arguments);
    case '&': // 同 #, 但 HTML 中较特殊, 逐步废弃
    case '#':
    case '@':
        if (arguments.length === 1) {
            arguments.length  =  2;
            arguments[1] = location.href;
        }
        if (typeof(arguments[1]) !== "string") {
            if (!jQuery.isArray(arguments[1])) {
                var lb = jQuery(arguments[1]).closest(".loadbox");
                arguments[1] = hsSerialArr(lb);
            }
            if (b === '@')
                return hsGetSerias(arguments[1], arguments[0]);
            else
                return hsGetSeria (arguments[1], arguments[0]);
        } else {
            if (b === '@')
                return hsGetParams(arguments[1], arguments[0]);
            else
                return hsGetParam (arguments[1], arguments[0]);
        }
    case '$':
    case '%':
        var c = b === '%' ? window.localStorage : window.sessionStorage;
        if (typeof c === "undefined") {
            throw "H$: Does not support '"
                + (b === '$' ? 'local' : 'session')
                + "Storage'" ;
        }
        if (arguments.length === 1) {
            return c.getItem(arguments[0]);
        } else
        if (arguments[ 1 ] == null) {
            /**/c.removeItem(arguments[0]);
        } else
        {
            /****/ c.setItem(arguments[0], arguments[1]);
        }
    default:
        throw "H$: Unrecognized identified '" + b + "'" ;
    }
}

/**
 * 标准化返回对象
 * @param {Object|String|XHR} rst JSON对象/JSON文本或错误消息
 * @param {Boolean} qut 不显示消息
 * @param {Boolean} qxt 不进行跳转
 * @return {Object}
 */
function hsResponObj(rst, qut, qxt) {
    if (typeof (rst.responseText) !== "undefined") {
        rst  =  rst.responseText;
    }
    if (typeof (rst) === "string") {
        rst  =  jQuery.trim( rst );
        if (rst.charAt(0) === '{') {
            if (typeof(JSON) !== "undefined") {
                rst  = JSON.parse( rst );
            }
            else {
                rst  = eval('('+rst+')');
            }
        } else
        if (rst.charAt(0) === '<') {
            // 某些时候服务器可能出错, 返回错误消息的页面
            // 需清理其中的超文本代码, 以供输出简洁的消息
            rst = {
                "msg": rst
                    .replace(/<script.*?>.*?<\/script>/gmi, "")  // 清除脚本代码
                    .replace(/<style.*?>.*?<\/style>/gmi, "")    // 清除样式代码
                    .replace(/<!--.*?-->/gm, "")                 // 清除注释
                    .replace(/<[^>]*?>/gm , " ")                 // 清除标签
                    .replace(/&[^&;]*;/gm , " ")                 // 清除符号
                    .replace(/^\s*$/gm, "")                      // 清除空行
                    .replace(/[ \f\t\v]+/g, " ")                 // 清理多余空白
                    .replace(/(^[ \f\t\v]+|[ \f\t\v]+$)/gm, ""), // 清理首尾空白
                "err":  "" ,
                "ern":  "" ,
                "ok" : false
            };
        } else {
            rst = {
                "msg": rst ,
                "err":  "" ,
                "ern":  "" ,
                "ok" : false
            };
        }
    }
    if (typeof (rst) === "object") {
        if (typeof(rst.ok ) === "undefined") {
            rst.ok = true ;
        } else
        if (rst.ok === "0") {
            rst.ok = false;
        }
        if (typeof(rst.ern) === "undefined") {
            rst.ern =  "" ;
        }
        if (typeof(rst.err) === "undefined") {
            rst.err =  "" ;
        }
        if (typeof(rst.msg) === "undefined") {
            rst.msg =  "" ;
        }

        // 服务接口要求跳转 (常为未登录或无权限)
        if (! qxt) {
            if (rst.ern && /^Er(301|302|401|402|403|404)$/.test(rst.ern)) {
                if (rst.msg) {
                    alert(rst.msg); // 严重错误, 直接弹框
                }
                if (rst.err &&/^Goto /i.test(rst.err)) {
                    var url =  rst.err.substring( 5 ) ;
                    if (url && url != '#') {
                        location.assign(hsFixUri(url));
                    } else {
                        location.reload( );
                    }
                } else {
                    var url = hsGetConf(rst.ern + ".redirect");
                    if (url) {
                        location.assign(hsFixUri(url));
                    }
                }
                throw new Error( rst.err );
            }
        }

        // 成功失败消息处理 (失败则总是发出警告)
        if (! qut) {
            if (rst.ok ) {
                if (rst.msg) {
                    jQuery.hsNote(rst.msg, "succ");
                }
            } else {
                if (rst.msg) {
                    jQuery.hsWarn(rst.msg, "warn");
                } else {
                    jQuery.hsWarn(hsGetLang('error.unkwn'), "warn");
                }
            }
        }

        // 针对特定数据结构
        if (typeof(rst['data']) !== "undefined") {
            jQuery.extend(rst , rst['data']);
            delete rst['data'];
        }
    }
    return rst;
}

/**
 * 序列化为数组, 供发往服务器(类似 jQuery.fn.serializeArray)
 * @param {String|Object|Array|Elements} obj
 * @param {Array}
 */
function hsSerialArr(obj) {
    var arr = [];
    var typ = !jQuery.isPlainObject(obj) ? jQuery.type(obj) : "objact";
    switch (typ) {
        case "array" :
            arr = obj;
            break;
        case "objact":
            hsForEach(obj,function(val,key) {
                if (key.length > 0) {
                    key = key.join('.'/**/);
                    arr.push({name: key, value: val});
                }
            });
            break;
        case "string":
            var ar1, ar2, key, val, i = 0;
            ar1 = obj.split('#' , 2);
            if (ar1.length > 1) obj = ar1[0];
            ar1 = obj.split('?' , 2);
            if (ar1.length > 1) obj = ar1[1];
            ar1 = obj.split('&');
            for ( ; i < ar1.length ; i ++ ) {
                ar2 = ar1[i].split('=' , 2);
                if (ar2.length > 1) {
                    key = decodeURIComponent (ar2[0]);
                    val = decodeURIComponent (ar2[1]);
                    arr.push({name: key, value: val});
                }
            }
            break;
        case "object":
            obj = jQuery( obj );
            if (obj.data("href")) {
                arr = [];
                var pos ;
                var url = obj.data("href");
                var dat = obj.data("data");
                pos = url.indexOf("?");
                if (pos != -1) {
                    hsSerialMix(arr, hsSerialArr(url.substring(pos+1)));
                }
                pos = url.indexOf("#");
                if (pos != -1) {
                    hsSerialMix(arr, hsSerialArr(url.substring(pos+1)));
                }
                if (dat) {
                    hsSerialMix(arr, hsSerialArr(dat));
                }
            } else {
                arr = jQuery(obj).serializeArray();
            }
            break;
    }
    return  arr;
}

/**
 * 序列化为字典, 供快速地查找(直接使用object-key获取数据)
 * @param {String|Object|Array|Elements} obj
 * @return {Object}
 */
function hsSerialDic(obj) {
    var arr = hsSerialArr(obj);
    var reg = /(\.\.|\.$)/;
    obj = {};
    for(var i = 0 ; i < arr.length ; i ++) {
        var k = arr[i].name ;
        var v = arr[i].value;
        if (k.length == 0) continue;
        k = k.replace(/\]\[/g, ".")
             .replace(/\[/   , ".")
             .replace(/\]/   , "" );
        if (reg.test( k )) { // a.b. 或 a..b 都是数组
            if (obj[k]===undefined) {
                obj[k]=[ ];
            }
            obj[k].push(v);
        } else {
            obj[k]    = v ;
        }
    }
    return  obj;
}

/**
 * 序列化为对象, 供进一步操作(可以使用hsGetValue获取数据)
 * @param {String|Object|Array|Elements} obj
 * @return {Object}
 */
function hsSerialObj(obj) {
    var arr = hsSerialArr(obj);
    obj = {};
    for(var i = 0; i < arr.length; i ++) {
        hsSetValue(obj, arr[i].name, arr[i].value);
    }
    return obj;
}

/**
 * 将 ar2 并入 arr 中, arr 和 ar2 必须都是 serializeArray 结构
 * @param {Array} arr
 * @param {Array} ar2
 */
function hsSerialMix(arr, ar2) {
    var map = {};
    for(var i =  0, j = ar2.length  ; i < j; i ++) {
        map[ar2[i].name] = 1 ;
    }
    for(var i = -1, j = arr.length-1; i < j; j --) {
        if (map[arr[j].name]) {
            arr.splice(j , 1);
        }
    }
    return jQuery.merge(arr, ar2);
}

/**
 * 获取多个序列值
 * @param {Array} arr 使用 hsSerialArr 获得
 * @param {String} name
 * @return {Array}
 */
function hsGetSerias(arr, name) {
    var val = [];
    for(var i = 0; i < arr.length; i ++) {
        if (arr[i]["name"] === name ) {
            val.push(arr[i]["value"]);
        }
    }
    return val;
}

/**
 * 设置多个序列值
 * @param {Array} arr 使用 hsSerialArr 获得
 * @param {String} name
 * @param {Array} value
 */
function hsSetSerias(arr, name, value) {
    for(var j = arr.length-1; j > -1; j --) {
        if (arr[j]["name"] === name) {
            arr.splice(j, 1);
        }
    }
    for(var i = 0 ; i < value.length; i ++) {
        arr.push({name: name, value: value[i]});
    }
}

/**
 * 获取单个序列值
 * @param {Array} arr 使用 hsSerialArr 获得
 * @param {String} name
 * @return {String}
 */
function hsGetSeria (arr, name) {
    var val = hsGetSerias(arr, name);
    if (val.length) return val.pop();
    else            return "";
}

/**
 * 设置单个序列值
 * @param {Array} arr 使用 hsSerialArr 获得
 * @param {String} name
 * @param {Array} value
 */
function hsSetSeria (arr, name, value) {
    hsSetSerias(arr, name, value != undefined && value != null ? [value] : []);
}

/**
 * 获取多个参数值
 * @param {String} url
 * @param {String} name
 * @return {Array}
 */
function hsGetParams(url, name) {
    name = encodeURIComponent(name).replace('.', '\\.');
    var reg = new RegExp("[\\?&]"+name+"=([^&]*)", "g");
    var arr = null;
    var val = [];
    while (true) {
        arr = reg.exec(url);
        if ( arr === null ) break;
        val.push(decodeURIComponent(arr[1]));
    }
    return val;
}

/**
 * 设置多个参数值
 * @param {String} url
 * @param {String} name
 * @param {Array} value
 */
function hsSetParams(url, name, value) {
    name = encodeURIComponent(name).replace('.', '\\.');
    var reg = new RegExp("[\\?&]"+name+"=([^&]*)", "g");
    url = url.replace(reg, "");
    for (var i = 0; i < value.length; i ++)
    {
        url += "&"+name+"="+encodeURIComponent(value[i]);
    }
    if (url.indexOf("?") < 0 ) {
        url  = url.replace("&", "?");
    }
    return url;
}

/**
 * 获取单个参数值
 * @param {String} url
 * @param {String} name
 * @return {String}
 */
function hsGetParam (url, name) {
    var val = hsGetParams(url, name);
    if (val.length) return val.pop();
    else            return "";
}

/**
 * 设置单个参数值
 * @param {String} url
 * @param {String} name
 * @param {String} value
 */
function hsSetParam (url, name, value) {
    return hsSetParams(url, name, value != undefined && value != null ? [value] : []);
}

/**
 * 从树对象获取值
 * @param {Object|Array} obj
 * @param {Array|String} path ['a','b'] 或 a.b
 * @param def 默认值
 * @return 获取到的值, 如果没有则取默认值
 */
function hsGetValue (obj, path, def) {
    /**
     需要注意的键:
     a[1]   数字将作为字符串对待
     a[][k] 空键将作为字符串对待, 但放在末尾会直接忽略
     */
    if (jQuery.isArray(path)) {
        return _hsGetPoint(obj, path, def);
    } else
    if (typeof(path) === "number") {
        var keys = [ path ];
        return _hsGetPoint(obj, keys, def);
    } else
    if (typeof(path) === "string") {
        var keys = _hsGetPkeys( path );
        return _hsGetPoint(obj, keys, def);
    } else {
        throw "hsGetValue: 'path' must be an array or string";
    }
}

/**
 * 从树对象获取值(hsGetValue的底层方法)
 * @param {Object|Array} obj
 * @param {Array} keys ['a','b']
 * @param def 默认值
 * @return 获取到的值, 如果没有则取默认值
 */
function _hsGetPoint(obj, keys, def) {
    if (!obj) {
        return def;
    }
    if (!jQuery.isArray(obj ) && !jQuery.isPlainObject(obj)) {
        throw "_hsGetPoint: 'obj' must be an array or object";
    }
    if (!jQuery.isArray(keys)) {
        throw "_hsGetPoint: 'keys' must be an array";
    }
    if (!keys.length) {
        throw "_hsGetPoint: 'keys' can not be empty";
    }
    return _hsGetDepth(obj, keys, def, 0);
}

function _hsGetDepth(obj, keys, def, pos) {
    var key = keys[pos];
    if (obj == null) {
        return def;
    }

    // 按键类型来决定容器类型
    if (key == null) {
        if (keys.length == pos + 1) {
            return obj;
        } else {
            return _hsGetDapth(obj, keys, def, pos + 1);
        }
    } else
    if (typeof(key) == "number") {
        // 如果列表长度不够, 则直接返回默认值
        if (obj.length  <= key ) {
            return def;
        }

        if (keys.length == pos + 1) {
            return obj[key] || def;
        } else {
            return _hsGetDepth(obj[key], keys, def, pos + 1);
        }
    } else {
        if (keys.length == pos + 1) {
            return obj[key] || def;
        } else {
            return _hsGetDepth(obj[key], keys, def, pos + 1);
        }
    }
}

function _hsGetDapth(lst, keys, def, pos) {
    var col = [];
    for(var i = 0; i < lst.length; i ++) {
        var obj  = _hsGetDepth(lst[i], keys, def, pos);
        if (obj !=  null) {
            col.push(obj);
        }
    }
    if (!jQuery.isEmptyObject(col)) {
        return col;
    } else {
        return def;
    }
}

function _hsGetPkeys(path) {
    path = path.replace(/\]\[/g, ".")
               .replace(/\[/   , ".")
               .replace(/\]/   , "" )
               .split  (/\./ );
    var i , keys = [];
    for(i = 0; i < path.length; i ++) {
        var keyn = path[i];
        /*
        if (keyn.substr(0, 1) == '#') {
            keys.push(parseInt(keyn.substr(1)));
        } else
        */
        if (keyn.length == 0 && i!=0) {
            keys.push(null);
        } else
        {
            keys.push(keyn);
        }
    }
    return  keys;
}

/**
 * 向树对象设置值
 * @param {Object|Array} obj
 * @param {Array|String} path ['a','b'] 或 a.b
 * @param val 将设置的值
 */
function hsSetValue (obj, path, val) {
    /**
     需要注意的键:
     a[1]   数字将作为字符串对待
     a[][k] 空键将作为字符串对待, 但放在末尾可表示push
     */
    if (jQuery.isArray(path)) {
        _hsSetPoint( obj, path, val);
    } else
    if (typeof(path) === "number") {
        var keys = [path];
        _hsSetPoint( obj, keys, val);
    } else
    if (typeof(path) === "string") {
        var keys = _hsGetPkeys(path);
        _hsSetPoint( obj, keys, val);
    } else {
        throw "hsSetValue: 'path' must be an array or string";
    }
}

/**
 * 向树对象设置值(hsSetValue的底层方法)
 * @param {Object|Array} obj
 * @param {Array} keys ['a','b']
 * @param val
 */
function _hsSetPoint(obj, keys, val) {
    if (!obj) {
        return;
    }
    if (!jQuery.isPlainObject(obj)) {
        throw "_hsSetPoint: 'obj' must be an object";
    }
    if (!jQuery.isArray(keys)) {
        throw "_hsSetPoint: 'keys' must be an array";
    }
    if (!keys.length) {
        throw "_hsSetPoint: 'keys' can not be empty";
    }
    _hsSetDepth(obj, keys, val, 0);
}

function _hsSetDepth(obj, keys, val, pos) {
    var key = keys[pos];

    // 按键类型来决定容器类型
    if (key == null) {
        if (obj == null) {
            obj =  [];
        }

        if (keys.length == pos + 1) {
            obj.push(val);
        } else {
            obj.push(_hsSetDepth(null, keys, val, pos + 1));
        }

        return obj;
    } else
    if (typeof(key) == "number") {
        if (obj == null) {
            obj =  [];
        }

        // 如果列表长度不够, 填充到索引的长度
        if (obj.length <= key) {
            for(var i = 0; i <= key; i++) {
                obj.push(null);
            }
        }

        if (keys.length == pos + 1) {
            obj[key] = val;
        } else {
            obj[key] = _hsSetDepth(obj[key], keys, val, pos + 1);
        }

        return obj;
    } else {
        if (obj == null) {
            obj =  {};
        }

        if (keys.length == pos + 1) {
            obj[key] = val;
        } else {
            obj[key] = _hsSetDepth(obj[key], keys, val, pos + 1);
        }

        return obj;
    }
}

/**
 * 遍历对象或数组的全部叶子节点
 * @param {Object,Array} data
 * @param {Function} func
 */
function hsForEach(data, func) {
    var path = [];
    if (arguments.length>2) {
        path = arguments[2];
    }
    if (jQuery.isPlainObject(data)) {
        for (var k in data) {
            hsForEach(data[k], func, path.concat([k]));
        }
    }
    else if (jQuery.isArray (data)) {
        for (var i = 0; i < data.length; i ++) {
            hsForEach(data[i], func, path.concat([i]));
        }
    }
    else if (path.length > 0) {
        func(data, path);
    }
}

/**
 * 获取配置
 * @param {String} key
 * @param {String} def 默认值
 * @return {String} 获取到的配置, 如果没有则取默认值
 */
function hsGetConf(key, def) {
    if (typeof(HsCONF[key]) !== "undefined") {
        return HsCONF[key];
    }
    else {
        return def;
    }
}

/**
 * 获取语言
 * @param {String} key
 * @param {Object|Array} rep 替换参数, {a:1,b:2} 或 [1,2]
 * @return {String} 获取到的语言, 其中的 $a或$0 可被 rep 替换
 */
function hsGetLang(key, rep) {
    if (typeof(HsLANG[key]) !== "undefined") {
        key  = HsLANG[key];
    }

    if (rep instanceof Array ) {
        var i, x = {};
        for(i in rep) {
            x[i + ""] = rep[i];
        }
        rep = x;
    }

    if (rep instanceof Object) {
        key = key.replace( /\$(\w+|\{.+?\})/gm, function(w) {
            if (w.substring(0 , 2) == "${") {
                w = w.substring(2, w.length - 1);
            }
            else {
                w = w.substring(1);
            }
            if (typeof(rep[w]) !== "undefined") {
                return rep[w];
            }
            else {
                return "";
            }
        });
    }

    return key;
}

/**
 * 检查URI是否有权访问
 * @param {String} uri
 * @return {Boolean} 是(true)否(false)有权访问
 */
function hsChkUri(uri) {
    if (typeof(HsAUTH[uri]) !== "undefined") {
        return HsAUTH[uri];
    }
    else {
        return true;
    }
}

/**
 * 补全URI为其增加前缀
 * @param {String} uri
 * @return {String} 完整的URI
 */
function hsFixUri(uri) {
    if (/^(\w+:\/\/|\/|\.\/|\.\.\/)/.test(uri) === false) {
        var pre  = HsCONF["BASE_HREF"];
        if (pre == undefined) {
            pre  = jQuery("base").attr("href");
        if (pre != undefined) {
            pre  = pre.replace(  /\/$/  ,  '');
            HsCONF["BASE_HREF"] = pre;
        }
        }
        return pre +"/"+ uri;
    }
    else {
        return uri;
    }
}

/**
 * 补全URI为其设置参数
 * 注意: 参数必须是单个的, 对多个参数如 &a[]=$a&a[]=$a 只会设置两个一样的值
 * @param {String} uri
 * @param {Object} pms 可以是 hsSerialArr 或 .loadbox 节点
 * @returns {String} 完整的URI
 */
function hsFixPms(uri, pms) {
    if (pms instanceof Element || pms instanceof jQuery) {
        pms = jQuery(pms).closest(".loadbox");
        pms = hsSerialArr(pms);
    }   pms = hsSerialObj(pms);
    return uri.replace(/\$(\w+|\{.+?\})/gm , function(w) {
        if (w.substring(0 , 2) === "${") {
            w = w.substring(2, w.length -1);
        }
        else {
            w = w.substring(1);
        }
        w = hsGetValue(pms, w);
        return  w || "";
    });
};

/**
 * 格式化数字
 * @param {Number} num
 * @param {Number} len 总长度(不含小数点)
 * @param {Number} dec 小数位
 * @param {String} sep 千分符
 * @param {String} dot 小数点
 * @return {String}
 */
function hsFmtNum(num, len, dec, sep, dot) {
  if (typeof(len) === "undefined") {
    len = 0;
  }
  if (typeof(dec) === "undefined") {
    dec = 2;
  }
  if (typeof(sep) === "undefined") {
    sep = ",";
  }
  if (typeof(dot) === "undefined") {
    dot = ".";
  }

  // 四舍五入
  var o = parseInt(num);
  if (isNaN(o) ) o = 0 ;
  var p = Math.pow(10, dec);
  num = ( Math.round(o * p) / p ).toString();

  var a = num.split(".", 2);
  if (a.length < 2) {
    a[1] = "0";
  }
  var n = a[0];
  var d = a[1];

  // 右侧补零
  var nl = n.length;
  for (var i = nl; i < len; i ++) {
    n = "0" + n;
  }

  // 左侧补零
  var dl = d.length;
  for (var j = dl; j < dec; j ++) {
    d = d + "0";
  }

  num = "";
  dec = "";

  // 添加分隔符
  if (sep) {
    var k, s = "";
    // 整数部分从右往左每3位分割
    while (n != "") {
      k = n.length - 3;
      s = n.substring(k);
      n = n.substring(0, k);
      num = s + sep + num;
    }
    // 整数部分扔掉最右边一位
    if (num) {
      k = num.length - 1;
      num = num.substring(0, k);
    }
    // 小数部分从左往右每3位分割
    while (d != "") {
      s = d.substring(0, 3);
      d = d.substring(3);
      dec = dec + sep + s;
    }
    // 小数部分扔掉最左边一位
    if (dec) {
      dec = dec.substring(1);
    }
  }
  else {
    num = n;
    dec = d;
  }

  // 组合整数位和小数位
  return num + dot + dec;
}

/**
 * 格式化日期
 * @param {Date} date
 * @param {String} format
 * @return {String}
 */
function hsFmtDate(date, format) {
  if (date === undefined) {
    return "";
  }

  if (typeof(date) === "string") {
    if ( /^\d+$/.test(date)) {
      date = parseInt(date);
    }
    else {
      date = Date.parse(date.replace(/-/g, "/").replace(/\.\d+$/, ""));
    }
  }

  if (typeof(date) === "number") {
    if (date == 0) {
      return "" ;
    }
    if (date <= 2147483647) {
      date = date * 1000 ;
    }
    date = new Date(date);
  }

  var y = date.getFullYear();
  var M = date.getMonth();
  var d = date.getDate();
  var H = date.getHours();
  var k = H + 1;
  var K = H > 11 ? H - 12 : H;
  var h = H > 12 ? H - 12 : H;
  var m = date.getMinutes();
  var s = date.getSeconds();
  var S = date.getMilliseconds();
  var E = date.getDay( );
  var a = H < 12 ? 0 : 1;

  if (K == 12) K = 0;
  if (h == 0) h = 12;

  function _addzero(num, len) {
    num = num ? num : "";
    num = num.toString();
    var gth = num.length;
    for (var i = gth; i < len; i ++) {
      num = "0" + num;
    }
    return num;
  }

  function _replace(mat) {
    var len = mat.length;
    var flg = mat.substring(0, 1);
    switch (flg) {
      case 'y':
        var x = _addzero(y, len);
        if (len <= 2) {
          return x.substring(x.length - len);
        }
        else {
          return x;
        }
      case 'M':
        if (len >= 4) {
          return hsGetLang("date.format.LM")[M];
        }
        else if (len == 3) {
          return hsGetLang("date.format.SM")[M];
        }
        else {
          return _addzero(M + 1, len);
        }
      case 'd':
        return _addzero(d, len);
      case 'H':
        return _addzero(H, len);
      case 'k':
        return _addzero(k, len);
      case 'K':
        return _addzero(K, len);
      case 'h':
        return _addzero(h, len);
      case 'm':
        return _addzero(m, len);
      case 's':
        return _addzero(s, len);
      case 'S':
        return _addzero(S, len);
      case 'E':
        if (len >= 4) {
          return hsGetLang("date.format.LE")[E];
        }
        else {
          return hsGetLang("date.format.SE")[E];
        }
      case 'a':
        if (len >= 4) {
          return hsGetLang("date.format.La")[a];
        }
        else {
          return hsGetLang("date.format.Sa")[a];
        }
    }
  }

  return format.replace(/M+|d+|y+|H+|k+|K+|h+|m+|s+|S+|E+|a+/g, _replace);
}

/**
 * 解析日期
 * @param {String} text
 * @param {String} format
 * @return {Date}
 */
function hsPrsDate(text, format) {
  if (text === undefined) {
    return new Date( 0  );
  }

  if (typeof(text) === "string") {
    if ( /^\d+$/.test(text)) {
      text = parseInt(text);
    }
  }

  if (typeof(text) === "number") {
    if (text <= 2147483647) {
      text = text * 1000 ;
    }
    return new Date(text);
  }

  var a = text.split(/\W+/);
  var b = format.match(/M+|d+|y+|H+|k+|K+|h+|m+|s+|S+|E+|a+/g);

  var i, j;
  var M, d, y, H = 0, m = 0, s = 0, A = 0;

  for (i = 0; i < b.length; i ++) {
    if (a[i] == null) continue;

    var wrd = a[i];
    var len = b[i].length;
    var flg = b[i].substring(0, 1);
    switch (flg) {
      case 'M':
        if (len >= 4) {
          for (j = 0; j < hsGetLang("date.format.LM").length; j ++) {
            if (wrd == hsGetLang("date.format.LM")[j]) {
              M = j;
              break;
            }
          }
        }
        else if (len == 3) {
          for (j = 0; j < hsGetLang("date.format.SM").length; j ++) {
            if (wrd == hsGetLang("date.format.SM")[j]) {
              M = j;
              break;
            }
          }
        }
        else {
          M = parseInt(wrd, 10);
        }
      break;
      case 'd':
        d = parseInt(wrd, 10);
      break;
      case 'y':
        y = parseInt(wrd, 10);
        if (len <= 2) {
          y += y > 29 ? 1900 : 2000;
        }
      break;
      case 'H':
      case 'K':
        H = parseInt(wrd, 10);
      break;
      case 'k':
      case 'h':
        H = parseInt(wrd, 10) - 1;
      break;
      case 'm':
        m = parseInt(wrd, 10);
      break;
      case 's':
        s = parseInt(wrd, 10);
      break;
      case 'a':
        if (len >= 4) {
          for (j = 0; j < hsGetLang("date.format.La").length; j ++) {
            if (wrd == hsGetLang("date.format.La")[j]) {
              A = j;
              break;
            }
          }
        }
        else {
          for (j = 0; j < hsGetLang("date.format.Sa").length; j ++) {
            if (wrd == hsGetLang("date.format.Sa")[j]) {
              A = j;
              break;
            }
          }
        }
      break;
    }
  }

  if (A == 1) {
    H += 12;
  }

  var text2;
  if (typeof(M) !== "undefined"
  &&  typeof(d) !== "undefined"
  &&  typeof(y) !== "undefined") {
    text2 = M+"/"+d+"/"+y+" "+H+":"+m+":"+s;
  }
  else {
    text2 = H+":"+m+":"+s;
  }

  return new Date(Date.parse(text2));
}

/**
 * HongsCORE日期格式转Bootstrap日期格式
 * @param {String} format
 * @return {String)
 */
function _hs2bsDF(format) {
  return format.replace(/a/g , 'P')
               .replace(/m/g , 'i')
               .replace(/M/g , 'm')
               // 交换 H h
               .replace(/H/g , 'x')
               .replace(/h/g , 'H')
               .replace(/x/g , 'h');
}

/**
 * Bootstrap日期格式转HongsCORE日期格式
 * @param {String} format
 * @return {String)
 */
function _bs2hsDF(format) {
  return format.replace(/m/g , 'M')
               .replace(/i/g , 'm')
               .replace(/P/gi, 'a')
               // 交换 H h
               .replace(/H/g , 'x')
               .replace(/h/g , 'H')
               .replace(/x/g , 'h');
}

(function($) {

$.jqAjax = $.ajax;
$.hsAjax = function(url, settings) {
    if (typeof(url) ===  "object") {
        settings = url;
        if (typeof(url["url"]) !== "undefined") {
            url  = url["url"];
        }
    }

    // 为明确所送数据类型, 便于服务端正确解析
    // 增加 dataKind, 取值 form,json,xml
    if (settings.dataKind) {
        switch(settings.dataKind.toLowerCase()) {
            case "form":
                if ($.isArray( /**/ settings.data)
                ||  $.isPlainObject(settings.data)
                ||  settings.data instanceof jQuery
                ||  settings.data instanceof Element) {
                    var hsStringprm = jQuery.param  ;
                    settings.data = hsSerialArr(settings.data);
                    settings.data = hsStringprm(settings.data);
                }
                settings.contentType = "application/x-www-form-urlencoded; charset=UTF-8";
                break;
            case "json":
                if ($.isArray( /**/ settings.data)
                ||  $.isPlainObject(settings.data)
                ||  settings.data instanceof jQuery
                ||  settings.data instanceof Element) {
                    var hsStringify = JSON.stringify;
                    settings.data = hsSerialObj(settings.data);
                    settings.data = hsStringify(settings.data);
                }
                settings.contentType = "application/json; charset=UTF-8";
                break;
            case "xml" :
                if (settings.data instanceof jQuery
                ||  settings.data instanceof Element) {
                    settings.data = '<?xml version="1.0"?>' +
                          $(settings.data).prop('outerHTML');
                }
                settings.contentType = "application/xml; charset=UTF-8";
                break;
            default:
                throw new Error("hsAjax: Unrecognized dataKind " + settings.dataKind);
        }
    }

    return $.jqAjax( hsFixUri(url) , settings );
};
$.hsOpen = function(url, data, complete) {
    var div = $('<div class="modal fade in"><div class="modal-dialog">'
              + '<div class="modal-content"><div class="modal-header">'
              + '<button type="button" class="close" data-dismiss="modal">&times;</button>'
              + '<h4  class="modal-title"  >' + hsGetLang( "opening" ) + '</h4>'
              + '</div><div class="modal-body openbox"></div></div></div></div>');
    var box = div.find ( '.openbox' );
    box.hsLoad( url, data, complete );
    div.on("hide.bs.modal",function() {
        div.remove();
    } );
    div.modal();
    return  box;
};
$.hsNote = function(msg, typ, yes, sec) {
    'use strict';
    var div = $('<div class="alert alert-dismissable fade in">'
              + '<button type="button" class="close" data-dismiss="alert">&times;</button>'
              + '<div class="alert-body notebox" ></div></div>');
    div.find(".notebox").text(msg);

    // 参数检查
    if (typeof typ !== "string") {
        sec =  yes ;
        yes =  typ ;
        typ ='info';
    }
    switch (typ) {
        case 'info':
            div.addClass("alert-info"   );
            break;
        case 'succ':
            div.addClass("alert-success");
            break;
        case 'warn':
            div.addClass("alert-warning");
            break;
        case 'erro':
            div.addClass("alert-danger" );
            break;
    }

    // 消息容器
    var ctr = $("#notebox");
    if (ctr.length < 1) {
        ctr = $('<div id="notebox"></div>');
        ctr.prependTo( document.body /**/ );
    }
    ctr.show().prepend(div);

    // 延时关闭
    if (sec === undefined ) {
        sec = 5;
    }
    if (sec > 0) {
        setTimeout(function( ) {
            div.alert("close");
        } , sec * 1000);
    }

    div.on("close.bs.alert", function(evt) {
        if (ctr.children( ).size( ) <= 1 ) {
            ctr.hide( );
        }
        if (yes) {
            yes(evt);
        }
        div.remove();
    });
    div.alert();
    return  div;
};
$.hsWarn = function(msg, typ, yes, not) {
    'use strict';
    var div = $('<div class="alert alert-dismissable fade in">'
              + '<button type="button" class="close" data-dismiss="alert">&times;</button>'
              + '<div class="alert-body warnbox" ></div></div>');
    var mod = $('<div class="modal fade in"></div>').append(div);
    var box = div.find(".warnbox");
    var btt = $('<h4></h4>');
    var btu = $( '<p></p>' );
    var btn = $( '<p></p>' );
    var tit ;

    // 参数检查
    var j   =  2   ;
    if (typeof typ !== "string") {
        j   =  1   ;
        not =  yes ;
        yes =  typ ;
        typ ='info';
    }
    switch (typ) {
        case 'info':
            div.addClass("alert-info"   );
            tit = hsGetLang("info.title");
            break;
        case 'succ':
            div.addClass("alert-success");
            tit = hsGetLang("succ.title");
            break;
        case 'warn':
            div.addClass("alert-warning");
            tit = hsGetLang("warn.title");
            break;
        case 'erro':
            div.addClass("alert-danger" );
            tit = hsGetLang("erro.title");
            break;
        default:
            div.addClass("alert-info"   );
            tit = typ;
            break;
    }
    btt.text(tit);
    box.text(msg);

    // 常规按钮
    if (yes == null || $.isFunction(yes)) {
        arguments[j + 0] = {
            "click": yes,
            "class": "btn-primary",
            "label": hsGetLang("ensure")
        };
    }
    if (not == null || $.isFunction(not)) {
        arguments[j + 1] = {
            "click": not,
            "class": "btn-default",
            "label": hsGetLang("cancel")
        };
    }

    // 操作按钮
    var end = null;
    for(var i = j ; i < arguments.length ; i = i + 1) {
        var v = arguments[i];
        // 没给 label 就是设置警告窗体
        if (v["label"] === undefined) {
            if (v["close"]) {
                end = v["close"];
            }
            if (v["title"]) {
                btt.text(v["title"]);
            }
            if (v["class"]) {
                div.addClass(v["class"]);
            }
            continue;
        }
        if (v["class"] === undefined) {
            v["class"]  =  "btn-default";
        }
        var btm = $('<button type="button" class="btn btn-md"></button>');
            btm.text( v["label"] ).addClass( v["class"] ).appendTo( btn );
        if (v["click"]) {
            btm.on("click" , v["click"]);
        }
    }

    // 确认对话
    var ini = { show : true };
    if (btn.children().size()) {
        ini.keyboard = false ;
        ini.backdrop = "static";

        // 外围包裹
        var wrp = $('<div></div>');
        wrp.append(div.contents());
        div.addClass("warn1");
        wrp.addClass("warn2");
        div.append(wrp);

        // 重新布局
        btu.append(box.contents());
        btu.addClass("warn3");
        btn.addClass("warn4");
        box.append(btt);
        box.append(btu);
        box.append(btn);

        // 隐藏关闭
        div.find(".close").hide( );
        div.removeClass("alert-dismissable");

        // 必须置空, 否则二次打开不会触发下方事件
        $.support.transition  =  undefined  ;

        // 垂直居中
        mod.on("shown.bs.modal", function() {
            var wh =$(window).height();
            var mh = div.outerHeight();
            if (wh > mh) {
                mh = Math.floor((wh - mh) / 2);
                div.css("margin-top", mh+"px");
                mod.css("padding-right","0px"); // 去掉模态框BS设的15像素右补丁
            }
        } );
    }

    $( ":focus" ).blur( );
    btn.on( "click","button", function() {
        mod.modal("hide");
    } );
    div.on( "close.bs.alert", function() {
        mod.modal("hide");
    } );
    mod.on("hidden.bs.modal", function(evt) {
        div.remove();
        mod.remove();
        if (end) {
            end(evt);
        }
    } );
    mod.modal( ini );
    div.alert();
    return  div;
};

$.fn.jqLoad = $.fn.load;
$.fn.hsLoad = function(url, data, complete) {
    if ( $.isFunction(  data  )) {
        complete = data ;
        data = undefined;
    }
    if (!$.isFunction(complete)) {
        complete = function() {};
    }

    var dat = data ? hsSerialArr(data): [];
    this.data( "href", url )
        .data( "data", dat )
        .addClass("loadbox")
        .addClass("loading");

    /**
     * 为了给加载区域内传递参数
     * 通常将参数附加在请求之上
     * 但这样会导致静态页不缓存
     * 故, 如果是 html 且无参数"_=RANDOM"
     * 则不传递任何参数到服务端
     */
    var pos;
    pos = url.indexOf(/***/"#");
    if (pos != -1) {
        url = url.substring(0, pos);
    }
    pos = url.indexOf('.html?');
    if (pos != -1 && !hsGetParam(url, '_') && !hsGetSeria(dat, '_')) {
        url = url.substring(0, pos + 5);
        dat = undefined;
    } else
    if (dat.length == 0) {
        dat = undefined;
    }
    url = hsFixUri(url);

    return $.fn.jqLoad.call(this, url, dat, function() {
        $(this).removeClass("loading").hsReady();
        complete.apply(this,arguments);
    });
};
$.fn.hsOpen = function(url, data, complete) {
    var prt = $(this);
    var box;
    var ref;
    var tab;

    if (prt.is(".panes")) {
        prt = prt.data("tabs");
        prt = prt.hsTadd(url );
        tab = prt[0];
        prt = prt[1];
    } else
    if (prt.is(".tabs" )) {
        prt = prt.hsTadd(url );
        tab = prt[0];
        prt = prt[1];
    } else
    if (prt.parent().is(".panes")) {
        tab = prt.parent().data("tabs" ).children().eq(prt.index());
    } else
    if (prt.parent().is(".tabs" )) {
        tab = prt;
        prt = prt.parent().data("panes").children().eq(tab.index());
    }

    if (tab) {
        ref = tab.parent().children( ).filter(".active");
        tab.show( ).find("a").click( );
        if (tab.find("a b,a span").size()) {
            tab.find("a b,a span").not(".close")
               .text(hsGetLang("loading"));
        } else {
            tab.find("a")
               .text(hsGetLang("loading"));
        }
        // 关闭关联的 tab
        if (prt.children().size( ) ) {
            prt.children().hsCloze();
            prt.empty();
        }
    } else {
        ref = $('<div class="openbak"></div>').hide()
            .append( prt.contents() ).appendTo( prt );
    }

    box = $('<div class="openbox"></div>')
          .appendTo(prt).data("ref", ref );
    box.hsLoad( url, data, complete );
    return box;
};
$.fn.hsReady = function() {
    var box = $(this);

    // 为避免 chrome 等浏览器中显示空白间隔, 清除全部独立的空白文本节点
    box.find("*").contents().filter(function() {
        return this.nodeType === 3 && /^\s+$/.test(this.nodeValue);
    }).remove();

    // 输入类
    box.find("input"/*class*/).each(function() {
        $(this).addClass("input-" + $(this).attr("type"));
    });

    // 折叠栏
    box.find(".dropdown-body").each(function() {
        var u = $(this).parent().is(".dropup");
        $(this).toggleClass("invisible", ! u );
    });

    // 国际化
    box.find(".i18n,[data-i18n]").each(function() {
        $(this).hsI18n();
    });

    // 选项卡
    box.find("[data-toggle=hsTabs]").each(function() {
        $(this).hsTabs();
    });

    // 初始化
    box.find("[data-toggle=hsInit]").each(function() {
        $(this).hsInit();
    });

    // 初始化
    if (! box.children("[data-toggle=hsInit],[data-module=hsInit]").size()) {
        $(this).hsInit();
    }

    // 组件化
    box.find("[data-module]").each( function() {
        var prnt = $(this);
        var opts = $(this).hsData();
        var func = $(this).attr("data-module");
        if (typeof(prnt[func]) === "function") {
            prnt[func]( opts );
        }
    });

    // 在加载前触发事件
    box.trigger("hsReady");

    // 加载、打开、执行
    box.find("[data-load]").each(function() {
        $(this).hsLoad($(this).attr("data-load"), $(this).attr("data-data"));
    });
    box.find("[data-open]").each(function() {
        $(this).hsOpen($(this).attr("data-open"), $(this).attr("data-data"));
    });
    box.find("[data-eval]").each(function() {
        eval('(function(){' + $(this).attr("data-eval") + '})').call( this );
    });

    return box;
};
$.fn.hsClose = function() {
    var prt = $(this).parent();
    var box = $(this);
    var tab;

    if (prt.parent().is(".panes")) {
        tab = prt.parent().data("tabs" ).children().eq(prt.index());
    } else
    if (prt.parent().is(".tabs" )) {
        tab = prt;
        prt = prt.parent().data("panes").children().eq(tab.index());
        box = prt.children( ".openbox" ); // Get the following boxes
    }

    // 触发事件
    box.trigger("hsClose");

    // 联动关闭
    box.hsCloze(/*recur*/);

    // 恢复标签
    if (tab) {
        var idx = box.data("ref") ? box.data("ref").index() : 0;
//      tab.parent().children().eq(idx).find( "a" ).click() ;
        var tbs = tab.parent().children();
        var pns = prt.parent().children();
        var tb2 = tbs.eq(idx);
        var pn2 = pns.eq(idx);
        tbs./**/removeClass("active");
        tb2.show().addClass("active");
        pns.hide();
        pn2.show();
        if (tab.has(".close").size()) {
            tab.remove();
            prt.remove();
        }
        pn2.trigger("hsRecur"); // 触发重现事件
    } else
    // 恢复内容
    if (box.data( "ref" )) {
        var ref  = box.data("ref");
        prt.append(ref.contents());
        box.remove();
        ref.remove();
        prt.trigger("hsRecur"); // 触发重现事件
    } else
    // 关闭浮窗
    if (box.closest(".modal").size()) {
        box.closest(".modal").modal ("hide");
    } else
    // 关闭通知
    if (box.closest(".alert").size()) {
        box.closest(".alert").remove( /**/ );
    }

    return box;
};
$.fn.hsCloze = function() {
    var box = $(this);
    $( document.body).find( ".openbox" ).each(function( ) {
        if (!box.is(this) && box.is($(this).data("rel"))) {
            $(this).hsClose();
        }
    });
    return box;
};

// 选项卡
$.fn.hsTabs = function(rel) {
    var box = $(this);
    if (! rel) {
        rel = box.attr("data-target");
        if (rel) {
            rel = box.hsFind  ( rel );
            /***/ rel.addClass( "panes");
        } else {
            rel = box.siblings(".panes");
        }
    }
    box.addClass( "tabs" );
    rel.data( "tabs", box);
    box.data("panes", rel);

    var act = box.children(".active" );
    if (act.size() === 0 ) {
        act = box.children("li:first");
    }
    act.children("a").click();

    return box;
};
$.fn.hsTadd = function(ref) {
    if (! ref) ref='';
    var box = $(this);
    var tab = box.find("[data-hrel='"+ref+"']").closest("li");
    var pne = $(box.data("tabs")).children().eq(tab.index( ));
    if (! tab.length) {
        tab = $('<li><a href="javascript:;"><span class="title"></span><span class="close">&times;</span></a></li>');
        pne = $('<div></div>').appendTo(box.data("panes"));
        tab.appendTo(box).find('a').attr('data-hrel', ref);
    }
    return [tab, pne];
};
$.fn.hsTdel = function(ref) {
    var box = $(this);
    var tab = box.find("[data-hrel='"+ref+"']").closest("li");
    var pne = $(box.data("tabs")).children().eq(tab.index( ));
    tab.children("a").hsClose( ); // 先关闭
    tab.remove( ); pne.remove( ); // 再删除
    return [tab, pne];
};

// 初始化
$.fn.hsInit = function(cnf) {
    if (cnf ===  undefined) {
        cnf = $(this).hsData();
    }
    var box = $(this);

    // 标题上的设置作用在其容器上
    if (box.is("h1,h2,h3")) {
        box = box.parent();
    }

    // 自动提取标题, 替换编辑文字
    // 如主键不叫id, 打开编辑页面, 则需加上id=0
    var h = box.children("h1,h2,h3").first();
    if (h.length ) {
        cnf.title = h.text();
    }
    if (cnf.title) {
        cnf.title = H$("#id" , box )?
            cnf.title.replace('{DO}', cnf.update || hsGetLang("form.update")):
            cnf.title.replace('{DO}', cnf.create || hsGetLang("form.create"));
    }
    if (h.length ) {
        h.text ( cnf.title );
    }

    if (box.is(".modal-body")) {
        var a = box.closest(".modal");
        for(var k in cnf) {
            var v =  cnf[k];
            switch (k) {
                case "modal":
                    v = "modal-" + v ;
                    a.find(".modal-dialog").addClass(v);
                    break;
                case "title":
                    a.find(".modal-title" ).text/**/(v);
                    break;
            }
        }
        a.modal();
    } else
    if (box.parent(".panes").size()
    ||  box.parent().parent(".panes").size()) {
        var a = box.closest(".panes>*");
            a = box.closest(".panes"  )
                   .data("tabs").children( ).eq(a.index());
        for(var k in cnf) {
            var v =  cnf[k];
            switch (k) {
                case "title":
                    var x = a.find("a");
                    var y = x.find("b,span").not(".close");
                    if (y.size()) {
                        y.text(v);
                    } else {
                        x.text(v);
                    }
                    break;
            }
        }
    }

    return box;
};

// 国际化
$.fn.hsI18n = function(rep) {
    var box = $(this);
    var lng;

    if (box.attr("data-i18n")) {
        lng = box.attr("data-i18n");
        lng = hsGetLang(lng, rep);
        box.text( lng );
    } else
    if ($(this).text()) {
        lng = box.text( );
        lng = hsGetLang(lng, rep);
        box.text( lng );
    }

    if (box.attr("alt")) {
        lng = box.attr("alt");
        lng = hsGetLang(lng, rep);
        box.attr("alt" , lng);
    }
    if (box.attr("title")) {
        lng = box.attr("title");
        lng = hsGetLang(lng, rep);
        box.attr("title" , lng);
    }
    if (box.attr("placeholder")) {
        lng = box.attr("placeholder");
        lng = hsGetLang(lng, rep);
        box.attr("placeholder" , lng);
    }

    return box;
};

$.fn.hsData = function() {
    var that = this.get(0);
    var conf = this.data();
    var nreg = /^data-\d+$/;
    var treg = /-\d+$/;
    var freg = /-\w/g ;
    var frep = function(n) {
        return n.substring(1).toUpperCase();
    };
    var vrep = function(v) {
        if ( /^(\{.*\}|\[.*\])$/.test( v )) {
            v = eval('('+v+')');
        } else if ( /^(\(.*\))$/.test( v )) {
            v = eval(    v    );
        }
        return  v;
    };
    var mrep = function(v) {
        if (!/^(\{.*\})$/.test( v )) {
                v  = '{'+v+'}' ;
        }
        return  eval('('+v+')');
    };
    this.each( function( ) {
        var a = this.attributes;
        if (a === undefined ) {
            return;
        }
        var j = a.length;
        var i = 0;
        for ( ; i < j; i ++ ) {
            var n = a[i].name ;
            if (n.substring(0 , 5) == 'data-') {
                n = n.substring(5);
            } else {
                continue;
            }
            var v = a[i].value;
            if ('data' == n ) {
                v = mrep.call(that, v);
                jQuery.extend(conf, v);
            } else
            if (nreg.test(n)) {
                var o = v.indexOf(':');
                n = v.substring(0 , o);
                v = v.substring(1 + o);
                n = jQuery.trim(n);
                v = jQuery.trim(v);
                v = vrep.call(that, v);
                hsSetValue(conf, n, v);
            } else
            if (treg.test(n)) {
                n = n.replace(treg,  ''  );
                n = n.replace(freg, frep );
                if (conf[n] === undefined) {
                    conf[n] = [];
                }
                v = vrep.call(that, v);
                 conf[n].push(v);
            } else {
                n = n.replace(freg, frep );
                v = vrep.call(that, v);
                 conf[n]  =   v ;
            }
        }
    });
    return  conf;
};
$.fn._hsConfig = $.fn.hsData; // 兼容旧版命名

$.fn.hsFind = function(selr) {
    var elem = this;
    selr = $.trim(selr);
    var flag = selr.charAt(0);
    var salr = selr.substr(1);
    salr = $.trim(salr);
    switch (flag) {
        case '!':
            return $(this).closest("."+salr).data(salr); // 快捷提取模块对象
        case '@':
            do {
                var x;
                x = elem.closest( ".panes" );
                if (x.size()) { elem = x; break; }
                x = elem.closest(".openbox");
                if (x.size()) { elem = x; break; }
                x = elem.closest(".loadbox");
                if (x.size()) { elem = x; break; }
                elem = $(document);
            } while (false);
            return salr ? $(salr, elem) : elem;
        case '%':
            do {
                var x;
                x = elem.closest(".loadbox");
                if (x.size()) { elem = x; break; }
                x = elem.closest(".openbox");
                if (x.size()) { elem = x; break; }
                elem = $(document);
            } while (false);
            return salr ? $(salr, elem) : elem;
        case '^':
            elem = elem.parent();
            var a = salr.split(';' , 2);
            if (! a[0]) elem = elem.closest(a[0]);
            if (! a[1]) elem = elem.hsFind (a[1]);
            return elem;
        case '>':
            return elem.children(salr);
        case '~':
            return elem.siblings(salr);
        case '+':
            return elem.next(salr);
        case '-':
            return elem.prev(salr);
        case '?':
            return elem.find(salr);
        case '*':
            return $(salr);
        case '#':
            return $(selr);
        default : // .:[
            return elem.find(selr);
    }
};
$.fn._hsTarget = $.fn.hsFind; // 兼容旧版命名

$.fn._hsModule = function(func, opts) {
    var elem = this;
    var name = func.name || /^function\s+(\w+)/.exec(func.toString())[1];
    var inst = elem.data(name);
    if (! inst) {
        inst = new func(opts, elem);
        elem.data(name, inst);
        elem.addClass ( name);
    }
    if (opts) for ( var k in opts ) {
        if ('_'===k.substring(0, 1)
        ||  inst[k] !== undefined ) {
            inst[k]  =  opts[k];
        }
    }
    return  inst;
};

// 三态选择
// indeterminate 有三个值: true 选中, null 半选, false 未选
$.propHooks.choosed = {
    get : function(elem) {
        return elem.checked ? true : (elem.indeterminate ?  null : false);
    },
    set : function(elem, stat) {
        if (stat === null) {
            elem.checked = false ;
            elem.indeterminate = true ;
        } else {
            elem.checked = !!stat;
            elem.indeterminate = false;
        }
    }
};

// 字串长度
String.prototype.baseSize = function() {
    var len = 0;
    for(var i = 0; i < this.length; i ++) {
        var c = this.charCodeAt(i);
        if (c > 128) {
            len += 2;
        } else {
            len += 1;
        }
    }
    return  len;
};
String.prototype.byteSize = function() {
    var len = 0;
    for(var i = 0; i < this.length; i ++) {
        var c = this.charCodeAt(i);
        if (c > 128) {
            len += 3;
        } else {
            len += 1;
        }
    }
    return  len;
};

//** Global Events **/

$(document).ajaxError(function(evt, xhr, cnf) {
    var rst = hsResponObj(xhr);
    if (typeof(cnf.funcName) === "undefined") {
        return;
    }
    if (typeof(cnf.trigger ) !== "undefined") {
        var btn = $(cnf.trigger);
        btn.trigger(cnf.funcName+"Error", evt, rst);
    }
    if (typeof(cnf.context ) !== "undefined") {
        var box;
        if ( typeof(cnf.context.context) !== "undefined") {
            box = $(cnf.context.context);
        } else {
            box = $(cnf.context);
        }
        box.trigger(cnf.funcName+"Error", evt, rst);
    }
});

$(document)
.on("click", "[data-toggle=hsOpen]",
function() {
    var btn = $(this);
    var box = btn.data("target");
    var url = btn.data("href");
    var dat = btn.data("data");
    var das = btn.data();
    var evs = { };

    /**
     * 从数据属性中提取出事件句柄
     * 绑定在打开的区域上
     * 因打开的区域会在关闭后销毁
     * 故可在打开重复绑定
     */
    for(var n in das) {
        var f  = das[n];
        if (! /^on[A-Z]\w*/.test( n )) {
            continue;
        }
        if (typeof(f) !== "function" ) {
            f = eval('(function(event) {'+ f +'})');
        }
        n = n.substring(2, 3).toLowerCase() + n.substring(3);
        evs[n] = f;
    }

    url = hsFixPms(url, this);
    if (box) {
        box = btn.hsFind(box)
                 .hsOpen(url, dat, evs.hsReady);
    } else {
        box =   $.hsOpen(url, dat, evs.hsReady);
    }

    for(var n in evs) {
        var f  = evs[n];
        if (n === "hsReady") {
            continue;
        }
        box.on(n, function() {
            f.apply(btn, arguments);
        });
    }
})
.on("click", "[data-toggle=hsClose],.close,.cloze,.cancel",
function() {
    var box;
    var ths = $(this);
    do {
        // 云标签
        if (ths.is("li>.close")) {
            return;
        }
        // 选项卡
        if (ths.is("li .close")) {
            box = ths.closest("a");
            break ;
        }
        // 标题栏
        box = ths.closest(".modal-header").next();
        if (box.is( ".openbox")) {
            box = box.closest(".modal");
            break ;
        }
        // 警示框
        box = ths.closest( ".notebox, .warnbox" );
        if (box.is( ".alert-body")) {
            box = box.closest(".alert");
            break ;
        }
        // 模态框
        box = ths.closest( ".openbox" );
        if (box.is( ".modal-body")) {
            box = box.closest(".modal");
            break ;
        }
        if (box.size()
        &&  box.closest(".alert,.modal").size()) {
            break ;
        }
        if (ths.closest(".alert,.modal").size()) {
            return;
        }
    } while(false);
    // 禁止关闭
    if(ths.closest(".dont-close",box[0]).size()) {
            return;
    }
    box.hsClose( );
})
.on("click", ".tabs > li > a",
function() {
    var ths = $(this);
    var tab = ths.parent();
    var nav = tab.parent();
    var pns = nav.data("panes");
    var pne = pns ? pns.children().eq(tab.index()) : $();
    if (tab.is(".dont-close")
    ||  tab.is(".back-crumb")) {
        return;
    }
    // 联动关闭
    if (nav.is(".breadcrumb")
    && !tab.is(".hold-crumb")) {
        tab.nextAll( ).find("a").each( function() {
            $(this).hsClose();
        });
    }
    // 延迟加载
    if (ths.is("[data-href]")) {
        var ref;
        ref = ths.attr("data-href");
        ths.removeAttr("data-href");
        pne.hsOpen( ref );
    }
    pne.siblings().hide();
    tab.siblings()
           .removeClass("active");
    tab.show().addClass("active");
    pne.show().trigger("hsRecur");
})
.on("click", ".dropdown-toggle",
function(evt) {
    if ($(evt.target).is(".dropdown-deny")) return;
    var body = $(this).siblings(".dropdown-body" );
    if (body.size() == 0) return;
    var cont = $(this).parent( );
    cont.toggleClass( "dropup" );
    body.toggleClass("invisible", !cont.is(".dropup"));
})
.on("click", "select[multiple]",
function(evt) {
    if (evt.shiftKey || evt.ctrlKey || evt.altKey) {
        return;
    }
    var vals = $(this).data("vals") || [];
    var valz = $(this).val();
    if (!valz || valz.length === 0) {
        vals = [];
    } else {
        $.each(valz, function(x, v) {
            var i = $.inArray(v, vals);
            if (i >= 0) {
                vals.splice(i,1);
            } else {
                vals.push  ( v );
            }
        });
    }
    $(this).data("vals", vals);
    $(this).val ( vals );
})
.on("click", ".back-crumb a",
function() {
    var nav = $(this).closest ('.breadcrumb');
    nav.find('li:last a').hsClose();
    nav.find('li:last a').  click();
})
.on("hsReady hsRecur", ".backable.panes",
function() {
    var nav = $(this).siblings('.breadcrumb') || $(this).data("tabs");
    if (nav.children().not('.back-crumb,.home-crumb').size( )) {
        nav.show().removeClass( "invisible" )
                  .removeClass(  "hide"  );
    } else {
        nav.hide();
    }
    if (nav.children(".active").is('.home-crumb,.hold-crumb')) {
        nav.children('.back-crumb').hide();
    } else {
        nav.children('.back-crumb').show();
    }
});

$(function() {
    $(document).hsReady();
});

})(jQuery);
