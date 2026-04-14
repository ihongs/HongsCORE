
/**
 * 填充数据、渲染元素
 * TODO: 写一份说明
 * @param {Element|jQuery} element 
 * @param {Object} data 
 */
function hsFill(element, data) {
    if (window.jQuery && element instanceof jQuery) {
        element = element[0];
    }

    // 内部有 set 和 for 变量, 需避免污染外部数据
    var env = {};
    for(var key in data) {
        env[key] = data[key];
    }
    data =  env ;

    // 内部渲染函数
    function render(element, data, inFor = false) {
        if (!element || !data) return;
        
        // 处理注释节点
        if (element.nodeType === 8) {
            renderComment(element, data);
            return;
        }
        
        // 处理文本节点
        if (element.nodeType === 3) {
            renderText(element, data);
            return;
        }
        
        // 处理元素节点
        if (element.nodeType === 1) {
            // 排除 for-item 已渲染元素
            if (element.hasAttribute('data-for-id')
            && !element.hasAttribute('data-for')
            && !inFor ) {
                return;
            }

            // 处理属性
            renderAttr(element, data);
            
            // 处理 if 指令
            if (element.hasAttribute('data-if' )) {
                renderIf (element, data);
                return;
            }
            
            // 处理 for 指令
            if (element.hasAttribute('data-for')) {
                renderFor(element, data);
                return;
            }
            
            // 递归处理子元素
            var children = element.childNodes;
            for (var i = 0; i < children.length; i++) {
                render(children[i], data);
            }
        }
    }

    // 处理注释
    function renderComment(comment, data) {
        var comment = comment.nodeValue.trim();
        var mat = comment.match(/^set\s+(\w+)\s*=\s*(.*)$/);
        if (mat) {
            var name = mat[1];
            var expr = mat[2];
            var val  = evaluate(expr, data);
            data[name] = val ;
        }
    }

    // 处理文本
    function renderText(element, data) {
        var temp ;
        if (element.tempValue !== undefined) {
            temp = element.tempValue;
        } else {
            temp = element.nodeValue;
        }
        var text = inscribe(temp, data);
        if (element.nodeValue !== text) {
            element.nodeValue = text;
        if (element.tempValue === undefined) {
            element.tempValue = temp;
        }}
    }

    // 处理属性
    function renderAttr(element, data) {
        var temps = element.tempAttrs || {};
        var attrs = element.attributes;
        var i = 0, j = 0;
        for(; i < attrs.length; i++) {
            var attr = attrs[i];
            var temp ;
            if (temps[attr.name] !== undefined) {
                temp = temps[attr.name];
            } else {
                temp = attr.value;
            }
            var text = inscribe(temp, data);
            if (element.getAttribute(attr.name) !== text) {
                element.setAttribute(attr.name, text);
            if (temps[attr.name] === undefined) {
                temps[attr.name] = temp;
                j ++;
            }}
        }
        // 有变更才需要更新记录
        if (j > 0) {
            element.tempAttrs = temps;
        }
    }

    // 处理 if 指令
    function renderIf(element, data) {
        var condition = element.getAttribute('data-if');
        var evaluated = evaluate(condition, data);
        
        // 记录原始的 display 值
        var oldDisplay = element.getAttribute('data-display');
        if (! oldDisplay) {
            oldDisplay = element.style.display || getComputedStyle(element).display || '-';
            element.setAttribute('data-display', oldDisplay );
        }
        if (oldDisplay === '-') {
            oldDisplay = '';
        }

        if (evaluated) {
            // 条件为真，显示元素
            element.style.display = oldDisplay;
            // 渲染子元素
            var children = element.childNodes;
            for (var i = 0; i < children.length; i++) {
                render(children[i], data);
            }
        } else {
            // 条件为假，隐藏元素
            element.style.display = 'none';
        }
    }

    // 处理 for 指令
    function renderFor(element, data) {
        var forExpr = element.getAttribute('data-for');
        var match = forExpr.match(/(\w+)\s+in\s+(.+)/);
        if (!match) return;
        
        var itemName = match[1];
        var listExpr = match[2];
        var list = evaluate(listExpr, data);
        
        if (!list) return;
        
        // 生成随机的 for-id
        var forId = element.getAttribute('data-for-id');
        if (! forId) {
            forId = 'for-' + Math.random().toString(36).substring(2);
            element.setAttribute('data-for-id' , forId);
        }
        
        // 记录原始的 display 值
        var oldDisplay = element.getAttribute('data-display');
        if (! oldDisplay) {
            oldDisplay = element.style.display || getComputedStyle(element).display || '-';
            element.setAttribute('data-display', oldDisplay );
        }
        if (oldDisplay === '-') {
            oldDisplay = '';
        }

        // 隐藏 data-for 节点
        element.style.display = 'none';

        var reserve = element.hasAttribute("data-for-reserve");
        var reverse = element.hasAttribute("data-for-reverse");
        
        // 插入定位
        var endElement = element;
        var parElement = element.parentNode;
        if (! reserve) {
            // 非保留模式, 清理已渲染的节点
            var curr = element.nextSibling;
            var next = curr;
            while (next) {
                curr = next;
                next = curr.nextSibling;
                if (curr.nodeType === 1 && curr.getAttribute('data-for-id') === forId) {
                    parElement.removeChild(curr);
                }
            }
        } else
        if (! reverse) {
            // 保留但非逆序模式, 定位到最后
            var curr = element.nextSibling;
            var next = curr;
            while (next) {
                curr = next;
                next = curr.nextSibling;
                if (curr.nodeType === 1 && curr.getAttribute('data-for-id') === forId) {
                    endElement = curr;
                }
            }
        }
        if (reverse) {
            // 逆序必须从下个节点开始
            // 否则 render 遍历到下个还是它
            // 这样会导致无限循环渲染
            endElement = element.nextSibling;
        }
        
        // 保存原始元素作为模板
        var template = element.cloneNode(true);

        // 处理数组
        if (Array.isArray(list)) {
            for(var i = 0; i < list.length; i ++) {
                data.$index = i; // 隐含索引
                data[itemName] = list[i];
                
                // 创建新元素
                var newElement = template.cloneNode(true);
                newElement.removeAttribute('data-for');
                newElement.setAttribute('data-for-id' , forId);
                newElement.style.display = oldDisplay ;

                // 渲染新元素
                render(newElement, data, true);

                // 添加新元素
                parElement.insertBefore(newElement, reverse ? endElement : endElement.nextSibling);
                endElement = newElement;
            }
        }
        // 处理对象
        else if (typeof list === 'object') {
            var keys = Object.keys( list );
            for(var i = 0; i < keys.length; i ++) {
                var k = keys[i];
                var v = list[k];
                data.$index = i; // 隐含索引
                data.$value = v; // 隐含取值
                data[itemName] = k;
                
                // 创建新元素
                var newElement = template.cloneNode(true);
                newElement.removeAttribute('data-for');
                newElement.setAttribute('data-for-id' , forId);
                newElement.style.display = oldDisplay ;

                // 渲染新元素
                render(newElement, data, true);

                // 添加新元素, 在最后一个元素后面
                element.parentNode.insertBefore(newElement, reverse ? endElement : endElement.nextSibling);
                endElement = newElement;
            }
        }
    }

    // 插值处理
    function inscribe(text, data) {
        return text.replace(/\{\{\s*(.*?)\s*\}\}/g, function (mat, expr) {
            var value = evaluate(expr, data);
            return value !== undefined ? value : expr;
        });
    }

    // 表达式值
    function evaluate(expr, data) {
        try {
            // 创建一个函数，将 data 中的所有属性作为变量引入
            var keys = Object.keys(data);
            var values = keys.map(function(key) { return data[key]; });
            var func = new Function(keys, 'return (' + expr + ')');
            return func.apply(null, values);
        } catch (e) {
            console.error('Wrong expression:', e.message, '|', expr, data);
            return undefined;
        }
    }
    
    // 开始渲染
    render(element, data);
}

/**
 * 视图组件
 * @param {Element|jQuery} context 
 */
function HsView(context) {
    this.context = context;
}
HsView.prototype = {
    data: {},
    fill: function (data) {
        this.data = this.mix(this.data, data);
        hsFill(this.context, this.data);
    },
    mix : function (target, source) {
        for (var key in source) {
            if (source.hasOwnProperty(key)) {
                if (source[key] && typeof source[key] === 'object' && ! Array.isArray(source[key])) {
                    if (!target[key]) target[key] = {};
                    this.mix(target[key], source[key]);
                } else {
                    target[key] = source[key];
                }
            }
        }
        return target;
    }
};
