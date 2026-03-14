package io.github.ihongs.util;

import io.github.ihongs.Core;
import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Date;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Collection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 极简模板引擎
 *
 * 由 AI 辅助编写, 类似 Jinja 语法
 * @see https://jinja.flask.org.cn/en/3.1.x/templates/#base-template
 *
 * ==================== 基本语法 ====================
 *
 * 三种标签类型:
 * - {{}}  : 变量输出，如 {{title}}, {{user.name}}
 * - {%%}  : 控制指令，如 {%if condition%}, {%for item in items%}
 * - {##}  : 注释，如 {# This is a comment #}
 *
 * ==================== 变量输出 ====================
 *
 * 简单变量: {{title}}
 * 字典访问: {{user.name}}
 * 数组访问: {{items.1}}
 * 嵌套访问: {{users.0.name}}
 *
 * ==================== 条件语句 ====================
 *
 * if 语句:
 * {%if condition%}
 *   Content when true
 * {%endif%}
 *
 * if-elif-else:
 * {%if count > 10%}
 *   Count > 10
 * {%elif count > 5%}
 *   Count > 5
 * {%else%}
 *   Count <= 5
 * {%endif%}
 *
 * ==================== 循环语句 ====================
 *
 * for 循环:
 * {%for item in items%}
 *   {{item}}
 * {%endfor%}
 *
 * for-else (循环为空时执行else):
 * {%for item in items%}
 *   {{item}}
 * {%else%}
 *   No items found
 * {%endfor%}
 *
 * 遍历字典:
 * {%for entry in config%}
 *   {{entry.key}}: {{entry.value}}
 * {%endfor%}
 *
 * ==================== 变量设置 ====================
 *
 * 设置变量:
 * {%set greeting = "Hello"%}
 * {%set total = count * price%}
 * {%set items = ["A", "B", "C"]%}
 *
 * ==================== 模板包含 ====================
 *
 * 包含子模板:
 * {%include "header.html"%}
 *
 * 使用指定上下文:
 * {%include "sub.temp" with subContext%}
 *
 * ==================== 表达式 ====================
 *
 * 算术运算: +, -, *, /, %
 * {%set sum = a + b%}
 * {%set avg = total / count%}
 *
 * 比较运算: ==, !=, >, <, >=, <=
 * {%if count > 10%}
 * {%if name == "admin"%}
 *
 * 逻辑运算: &&, ||, !
 * {%if a && b%}
 * {%if !isAdmin%}
 * 类似 JS 的 &&, ||
 * {{abc || def}}
 * 如过 abc 为空则用 def
 *
 * 三元操作:
 * {{a && b ? c : d}}
 * {%set x = a ? b : (c ? d : e)%}
 *
 * 括号分组:
 * {%if (a && b) || (c && d)%}
 * {%set result = (a + b) * (c - d)%}
 *
 * 简单JSON:
 * {%set result = ["abc", "def"]%}
 * {%set result = {"key": "val"}%}
 * {%include "xxx.html" with {"list": sub_list}%}
 *
 * ==================== 内置变量 ====================
 *
 * SERVER_PATH - 服务路径, 如 /sample
 * SERVER_HREF - 服务域名, 如 http://localhost
 * BASE_HREF - 服务链接, 如 http://localhost/sample
 *
 * ==================== 内置函数 ====================
 *
 * contains(集合, 选项) - 包含, 也可用于字典或字串
 * {%if contains(list, name)%}
 *
 * matches(文本, 正则) - 正则匹配
 * {%if matches(name, "[Tt]ony")}}
 *
 * substr(文本, 起始, 长度) - 文本截取, 起始为负反向截取
 * {{substr(text, 4, 2)}}
 * {{substr(text, 4)}}
 *
 * indent(文本, 缩进几格) - 缩进文本
 * {{indent(text, 4)}}
 * {{indent(text)}}  # 默认缩进两格
 *
 * concat(列表, 连词符号) - 连接多个
 * {{concat(list, "|")}}
 * {{concat(list)}}  # 默认逗号连接
 *
 * format(格式, 变量1, 变量2...) - 格式化字符串
 * {{format("Hello %s", name)}}
 * {{format("%.2f", price)}}
 *
 * date_format(时间, 格式) - 日期格式化, 支持时间戳(毫秒), Date, Instant
 * {{date_format(timestamp, "HH:mm:ss")}}
 * {{date_format(date, "yyyy-MM-dd")}}
 *
 * range(开始, 结束, 步长) - 范围迭代, 步长默认 1
 * {%for i in range(0, 10, 2)%}
 * {%for i in range(0, 10)%}
 *
 * count(变量) - 获取长度
 * {{count(text)}}  # 字符串长度
 * {{count(list)}}  # 列表长度
 * {{count(dict)}}  # 字典大小
 *
 * strip(文本, 模式) - 文本清理
 * {{strip(text, "trim")}}  # 清除首尾空格
 * {{strip(html, "tags")}}  # 清除HTML标签
 * {{strip(html, "html")}}  # 清除脚本、标签、首尾空格
 * {{strip(text, "gaps")}}  # 清除空行
 * {{strip(text, "ends")}}  # 清除换行
 * {{strip(text, "unis")}}  # 统一换行符
 * {{strip(text, "trim,tags")}}  # 多个模式
 *
 * ==================== 注意事项 ====================
 *
 * - 变量名需区分大小写
 * - 支持条件和循环嵌套
 * - 支持表达式括号嵌套
 * - 不支持对象属性访问
 * - 不支持对象方法调用
 * - 不支持字符串内转义，如：\n,\t,\"，如特殊，请从 render context 传入
 * - 支持 JSON 值为变量，如：{"a": b}，但不支持嵌套：{"abc": [1, 2, 3]}
 * - include 指令需指定 basePath
 * - 独占一行的指令标签 {%%} 和注释 {##} 不空行，不影响 markdown 的段落
 *
 * ==================== 完整示例 ====================
 *
 * <code>
 * # {{title}}
 * {%set greeting = "Hello"%}
 * {%if showWelcome%}
 * {{greeting}}, {{user.name}}!
 * {%endif%}
 *
 * {%set i = 0%}
 * {%for item in items%}
 * {%set i = i + 1%}
 * - {{i}}. {{item}}
 * {%endfor%}
 *
 * Total: {{count(items)}} items
 * Price: {{format("%.2f", price)}}
 * </code>
 *
 * @author Hongs
 */
public class Template {

    private final List<Block> blocks;
    private final Map<String, Object> variables;
    private final Map<String, Function<Object[], Object>> functions;
    private final static Pattern DIR_LINE = Pattern.compile("(\\{%(?!\\s*include\\s+).*?%\\}|\\{#.*?#\\})+");
    private static enum END {IF, FOR};

    private Template(List<Block> blocks) {
        this.blocks    =  blocks;
        this.variables = new HashMap<>();
        this.functions = new HashMap<>(FUNCTIONS);
    }

    /**
     * 模板构造
     * @param template 模板文本, include 指令会抛异常
     * @return
     */
    public static Template compile(String template) {
        List<Block> blocks = parseTemplate(template, null);
        return new Template(blocks);
    }

    /**
     * 模板构造
     * @param template 模板文本
     * @param basePath 基准目录, include 时相对此目录
     * @return
     */
    public static Template compile(String template, Path basePath) {
        List<Block> blocks = parseTemplate(template, basePath);
        return new Template(blocks);
    }

    /**
     * 模板构造
     * @param path 模板路径
     * @return
     * @throws IOException
     */
    public static Template compile(Path path) throws IOException {
        List<Block> blocks = parseTemplate(Files.readString(path), path.getParent());
        return new Template(blocks);
    }

    /**
     * 注册函数
     * @param name
     * @param function
     * @return this
     */
    public Template regist(String name, Function<Object[], Object> function) {
        this.functions.put(name, function);
        return this;
    }

    /**
     * 登记变量
     * @param name
     * @param supplier
     * @return this
     */
    public Template assign(String name, Supplier<Object> supplier) {
        this.variables.put(name, supplier);
        return this;
    }

    /**
     * 登记变量
     * @param name
     * @param variable
     * @return this
     */
    public Template assign(String name, Object variable) {
        this.variables.put(name, variable);
        return this;
    }

    /**
     * 模板渲染
     * @param context
     * @param writer
     * @throws IOException
     */
    public void render(Map<String, Object> context, Writer writer) throws IOException {
        Map<String, Object> newContext = new HashMap<>();
        newContext.put("__FUNC__", functions);
        newContext.put("__VARS__", variables);
        newContext.putAll(context);
        for (Block block : blocks) {
             block.render(newContext, writer);
        }
    }

    /**
     * 模板渲染
     * @param writer
     * @throws IOException
     */
    public void render(Writer writer) throws IOException {
        Map<String, Object> newContext = new HashMap<>();
        newContext.put("__FUNC__", functions);
        newContext.put("__VARS__", variables);
        for (Block block : blocks) {
             block.render(newContext, writer);
        }
    }

    /**
     * 模板渲染
     * @param context
     * @return
     */
    public String render(Map<String, Object> context) {
        StringWriter writer = new StringWriter();
        try {
            render(context, writer);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return writer.toString();
    }

    /**
     * 模板渲染
     * @return
     */
    public String render() {
        StringWriter writer = new StringWriter();
        try {
            render(writer);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return writer.toString();
    }

    private static List<Block> parseTemplate(String template, Path basePath) {
        // 为规避多余的空行影响格式
        // 清理独行指令标签前后空白及末尾换行
        // 方法简单但对字符串有修改会造成拷贝
        //template = template.replaceAll("(?<=\n)[ \t]*(\\{%.*?%\\}|\\{#.*?#\\})[ \t]*\n", "$1");

        return parseTemplate(template, basePath, 0, template.length());
    }

    private static List<Block> parseTemplate(String baseTemp, Path basePath, int bPos, int ePos) {
        String template = baseTemp.substring(bPos, ePos);
        List<Block> blocks = new ArrayList<>();
        int index = 0;

        while (index < template.length()) {
            int varStart = template.indexOf("{{", index);
            int dirStart = template.indexOf("{%", index);
            int comStart = template.indexOf("{#", index);

            // Find the next directive or variable
            int nextStart = -1;
            if (varStart != -1) nextStart = varStart;
            if (dirStart != -1 && (nextStart == -1 || dirStart < nextStart)) nextStart = dirStart;
            if (comStart != -1 && (nextStart == -1 || comStart < nextStart)) nextStart = comStart;

            // No more directives, add remaining text
            if (nextStart == -1) {
            if (index < template.length()) {
                // 清理独行指令空白
                int end = template.length();
                    end = findTxtEnd(baseTemp, bPos, end);
                if (end > index) {
                    String text = template.substring(index, end);
                    blocks.add(new TxtBlock(text));
                }
            }
                break;
            }

            // Add plain text before the directive
            if (nextStart > index) {
                // 清理独行指令空白
                int end = nextStart;
                    end = findTxtEnd(baseTemp, bPos, end);
                if (end > index) {
                    String text = template.substring(index, end);
                    blocks.add(new TxtBlock(text));
                }
            }

            if (nextStart == dirStart) {
                // Directive block
                // Find the end of the directive by scanning for %}
                int end = dirStart + 2;
                while (end < template.length()) {
                    if (template.charAt(end) == '%' && end + 1 < template.length() && template.charAt(end + 1) == '}') {
                        // Found the end of the directive
                        break;
                    }
                    end++;
                }
                if (end >= template.length()) {
                    int currentLine = countLines(baseTemp, bPos + dirStart);
                    throw new IllegalArgumentException("Line " + currentLine + ": Unclosed directive");
                }
                end += 2;

                // Extract the directive content
                String directive = template.substring(dirStart + 2, end - 2).trim();

                if (directive.startsWith("if ")) {
                    // Find matching endif
                    int endifStart = findDirEnd(template, end, template.length());
                    if (endifStart == -1) {
                        int currentLine = countLines(baseTemp, bPos + dirStart);
                        throw new IllegalArgumentException("Line " + currentLine + ": Unclosed if statement");
                    }
                    int endifEnd = template.indexOf("%}", endifStart);
                    if (endifEnd == -1) {
                        int currentLine = countLines(baseTemp, bPos + dirStart);
                        throw new IllegalArgumentException("Line " + currentLine + ": Unclosed if statement");
                    }
                    endifEnd += 2;

                    List<Block> elifBlocks ;
                    List<Block> elseBlocks = new ArrayList<>();
                    List<IfBlock.Group> elifGroups = new ArrayList<>();

                    int elifStart;
                    int elifEnd = end;
                    String condition = directive.substring(3).trim();

                    do {
                        elifStart = findDirEls(template, elifEnd, endifStart);
                        if (elifStart == -1) {
                            elifBlocks = parseTemplate(baseTemp, basePath, bPos + elifEnd, bPos + endifStart);
                            elifGroups.add(new IfBlock.Group(condition, elifBlocks));

                            elifEnd = endifEnd;
                        } else {
                            elifBlocks = parseTemplate(baseTemp, basePath, bPos + elifEnd, bPos + elifStart );
                            elifGroups.add(new IfBlock.Group(condition, elifBlocks));

                            // else or elif directive
                            int elseEnd = template.indexOf("%}", elifStart);
                            if (elseEnd == -1) {
                                int currentLine = countLines(baseTemp, bPos + elifStart);
                                throw new IllegalArgumentException("Line " + currentLine + ": Unclosed statement");
                            }
                            elseEnd += 2;

                            String dir = template.substring (elifStart + 2, elseEnd - 2).trim();
                            if (dir.startsWith("elif ")) {
                                condition  = dir.substring(5).trim(); // 下一个的条件
                            } else
                            if (dir.equals("else")) {
                                elseBlocks = parseTemplate(baseTemp, basePath, bPos + elseEnd, bPos + endifStart );
                                break;
                            } else {
                                int currentLine = countLines(baseTemp, bPos + elifStart);
                                throw new IllegalArgumentException("Line " + currentLine + ": Invalid statement in if: " + dir);
                            }

                            elifEnd = elseEnd;
                        }
                    }
                    while ( elifEnd < endifStart );

                    blocks.add(new IfBlock(elifGroups, elseBlocks));
                    index = endifEnd;
                } else
                if (directive.startsWith("for ")) {
                    // Find matching endfor
                    int endforStart = findDirEnd(template, end, template.length());
                    if (endforStart == -1) {
                        int currentLine = countLines(baseTemp, bPos + dirStart);
                        throw new IllegalArgumentException("Line " + currentLine + ": Unclosed for statement");
                    }
                    int endforEnd = template.indexOf("%}", endforStart);
                    if (endforEnd == -1) {
                        int currentLine = countLines(baseTemp, bPos + dirStart);
                        throw new IllegalArgumentException("Line " + currentLine + ": Unclosed for statement");
                    }
                    endforEnd += 2;

                    String[] parts = directive.substring(4).trim().split("\\s+in\\s+");
                    if (parts.length != 2) {
                        int currentLine = countLines(baseTemp, bPos + dirStart);
                        throw new IllegalArgumentException("Line " + currentLine + ": Invalid for statement: " + directive);
                    }
                    String variableName = parts[0].trim();
                    String iterableName = parts[1].trim();

                    List<Block> baseBlocks ;
                    List<Block> elseBlocks = new ArrayList<>();

                    // 寻找同级的 else
                    int elseStart = findDirEls(template, end, endforStart);
                    if (elseStart == -1) {
                        baseBlocks = parseTemplate(baseTemp, basePath, bPos + end, bPos + endforStart);
                    } else {
                        baseBlocks = parseTemplate(baseTemp, basePath, bPos + end, bPos + elseStart  );

                        // else directive
                        int elseEnd = template.indexOf("%}", elseStart);
                        if (elseEnd == -1) {
                            int currentLine = countLines(baseTemp, bPos + elseStart);
                            throw new IllegalArgumentException("Line " + currentLine + ": Unclosed statement");
                        }
                        elseEnd += 2;

                        String dir = template.substring (elseStart + 2, elseEnd - 2).trim();
                        if (dir.equals("else")) {
                            elseBlocks = parseTemplate(baseTemp, basePath, bPos + elseEnd, bPos + endforStart);
                        } else {
                            int currentLine = countLines(baseTemp, bPos + elseStart);
                            throw new IllegalArgumentException("Line " + currentLine + ": Invalid statement in for: " + dir);
                        }
                    }

                    blocks.add(new ForBlock(variableName, iterableName, baseBlocks, elseBlocks));
                    index = endforEnd;
                } else
                if (directive.startsWith("set ")) {
                    // Set block
                    String[] parts = directive.substring(4).trim().split("\\s*=\\s*");
                    if (parts.length != 2) {
                        int currentLine = countLines(baseTemp, bPos + dirStart);
                        throw new IllegalArgumentException("Line " + currentLine + ": Invalid set statement: " + directive);
                    }
                    String variableName = parts[0].trim();
                    String variableExpr = parts[1].trim();
                    blocks.add(new SetBlock(variableName, variableExpr));
                    index = end;
                } else
                if (directive.startsWith("include ")) {
                    // Include block
                    String includePart = directive.substring(8).trim();
                    String includeName = null;
                    String contentExpr = null;

                    // Find the first quote
                    int firstQuote = includePart.indexOf('"');
                    if (firstQuote == -1) {
                        int currentLine = countLines(baseTemp, bPos + dirStart);
                        throw new IllegalArgumentException("Line " + currentLine + ": Include template name must be quoted");
                    }

                    // Find the matching closing quote
                    int lastQuote = includePart.indexOf('"', firstQuote + 1);
                    if (lastQuote == -1) {
                        int currentLine = countLines(baseTemp, bPos + dirStart);
                        throw new IllegalArgumentException("Line " + currentLine + ": Include template name quote unclosed");
                    }

                    // Extract the template name
                    includeName = includePart.substring(firstQuote + 1, lastQuote);

                    // Check for with clause after the closing quote
                    String remaining = includePart.substring(lastQuote + 1).trim();
                    if (remaining.startsWith("with ")) {
                        contentExpr = remaining.substring(5).trim();
                    }

                    blocks.add(new InBlock(basePath, includeName, contentExpr));
                    index = end;
                } else
                if (directive.equals("else")
                ||  directive.equals("endif")
                ||  directive.equals("endfor")) {
                    // Else directive - should only be found by the if or for block parser
                    // End directive - should only be found by findMatchingEnd
                    int currentLine = countLines(baseTemp, bPos + dirStart);
                    throw new IllegalArgumentException("Line " + currentLine + ": Unexpected directive: " + directive);
                } else {
                    // Unknown directive
                    int currentLine = countLines(baseTemp, bPos + dirStart);
                    throw new IllegalArgumentException("Line " + currentLine + ": Unknown directive: " + directive);
                }
            } else
            if (nextStart == varStart) {
                // Variable block
                int end = template.indexOf("}}", varStart);
                if (end == -1) {
                    int currentLine = countLines(baseTemp, bPos + varStart);
                    throw new IllegalArgumentException("Line " + currentLine + ": Unclosed variable");
                }
                String variableExpr = template.substring(varStart + 2, end).trim();
                blocks.add(new VarBlock(variableExpr));
                index = end + 2;
            } else
            if (nextStart == comStart) {
                // Comment block - skip
                int end = template.indexOf("#}", comStart);
                if (end == -1) {
                    int currentLine = countLines(baseTemp, bPos + comStart);
                    throw new IllegalArgumentException("Line " + currentLine + ": Unclosed comments");
                }
                index = end + 2;
            }
        }

        return blocks;
    }

    private static int findDirEnd(String template, int start, int end) {
        int count = 1;
        int index = start;

        while (index < end) {
            int dirStart = template.indexOf("{%", index);
            if (dirStart == -1) {
                break;
            }

            // Find the correct closing %} by scanning character by character
            int dirEnd = dirStart + 2;
            while (dirEnd < template.length()) {
                if (template.charAt(dirEnd) == '%' && dirEnd + 1 < template.length() && template.charAt(dirEnd + 1) == '}') {
                    // Found valid closing %}
                    break;
                }
                dirEnd++;
            }
            if (dirEnd >= template.length()) {
                break;
            }
            dirEnd += 2;

            String dir = template.substring(dirStart + 2, dirEnd - 2).trim();
            int pos = dir. indexOf ( " " );
            if (pos > 0) {
                dir = dir.substring(0,pos);
            }

            switch (dir) {
                case "if"    :
                    count ++ ;
                    break;
                case "for"   :
                    count ++ ;
                    break;
                case "endif" :
                    count -- ;
                    if (count == 0) {
                        return dirStart;
                    }   break;
                case "endfor":
                    count -- ;
                    if (count == 0) {
                        return dirStart;
                    }   break;
            }

            index = dirEnd;
        }

        return -1;
    }

    private static int findDirEls(String template, int start, int end) {
        int count = 1;
        int index = start;

        while (index < end) {
            int dirStart = template.indexOf("{%", index);
            if (dirStart == -1) {
                break;
            }

            // Find the correct closing %} by scanning character by character
            int dirEnd = dirStart + 2;
            while (dirEnd < template.length()) {
                if (template.charAt(dirEnd) == '%' && dirEnd + 1 < template.length() && template.charAt(dirEnd + 1) == '}') {
                    // Found valid closing %}
                    break;
                }
                dirEnd++;
            }
            if (dirEnd >= template.length()) {
                break;
            }
            dirEnd += 2;

            String dir = template.substring(dirStart + 2, dirEnd - 2).trim();
            int pos = dir. indexOf ( " " );
            if (pos > 0) {
                dir = dir.substring(0,pos);
            }

            switch (dir) {
                case "if"    :
                    count ++ ;
                    break;
                case "for"   :
                    count ++ ;
                    break;
                case "endif" :
                    count -- ;
                    break;
                case "endfor":
                    count -- ;
                    break;
                case "elif"  :
                    // 只找同层的 elif
                    if (count == 1) {
                        return dirStart;
                    }   break;
                case "else"  :
                    // 只找同层的 else
                    if (count == 1) {
                        return dirStart;
                    }   break;
            }

            index = dirEnd;
        }

        return -1;
    }

    private static int findTxtEnd(String template, int start, int end) {
        int p0  = template.lastIndexOf("\n", start + end);
        int p1  = template.    indexOf("\n", start + end);
        if (p0 != -1 && p1 != -1) {
            String line = template.substring(p0, p1).trim();
            Matcher mat = DIR_LINE.matcher(line);
            if (mat.find() && mat.start() == 0 && mat.end() == line.length()) {
                return p0 - start;
            }
        }
        return  end;
    }

    private static int countLines(String template, int length) {
        if (template == null || template.isEmpty()) {
            return 0;
        }
        int cnt  = 1; // first line
        if (length > template.length()) {
            length = template.length();
        }
        for (int i = 0; i < length; i++) {
            if (template.charAt(i) == '\n') {
                cnt ++;
            }
        }
        return  cnt;
    }

    private static boolean decide(Object obj) {
        if (obj == null) {
            return false;
        } else if (obj instanceof Boolean) {
            return (Boolean) obj;
        } else if (obj instanceof Number ) {
            return ((Number) obj).doubleValue() != 0;
        } else if (obj instanceof String ) {
            return !((String    ) obj).isEmpty();
        } else if (obj instanceof Collection) {
            return !((Collection) obj).isEmpty();
        } else if (obj instanceof Map) {
            return !((Map       ) obj).isEmpty();
        } else {
            return true;
        }
    }

    // Block interface
    private interface Block {
        void render(Map<String, Object> context, Writer writer) throws IOException;
    }

    // Plain text block
    private static class TxtBlock implements Block {
        private final String text;

        public TxtBlock(String text) {
            this. text = text ;
        }

        @Override
        public void render(Map<String, Object> context, Writer writer) throws IOException {
            writer.write(text);
        }
    }

    // Variable block
    private static class VarBlock implements Block {
        private final String variableExpr;

        public VarBlock(String variableExpr) {
            this.variableExpr = variableExpr;
        }

        @Override
        public void render(Map<String, Object> context, Writer writer) throws IOException {
            Object value = getValue(variableExpr, context);
            if (value != null) {
                writer.write(Synt.asString(value));
                //writer.write(value.toString());
            }
        }
    }

    // Set variable block
    private static class SetBlock implements Block {
        private final String variableName;
        private final String variableExpr;

        public SetBlock(String variableName, String variableExpr) {
            this.variableName = variableName;
            this.variableExpr = variableExpr;
        }

        @Override
        public void render(Map<String, Object> context, Writer writer) throws IOException {
            Object value = getValue(variableExpr, context);
            context.put(variableName, value);
        }
    }

    // For block
    private static class ForBlock implements Block {
        private final String variableName;
        private final String iterableExpr;
        private final List<Block> baseBlocks;
        private final List<Block> elseBlocks;

        public ForBlock(String variableName, String iterableExpr, List<Block> baseBlocks, List<Block> elseBlocks) {
            this.variableName = variableName;
            this.iterableExpr = iterableExpr;
            this.baseBlocks = baseBlocks;
            this.elseBlocks = elseBlocks;
        }

        @Override
        public void render(Map<String, Object> context, Writer writer) throws IOException {
            Object value = getValue(iterableExpr, context);
            boolean hasItems = false;

            if (value instanceof Collection<?>) {
                Collection<?> list = (Collection<?>) value;
                hasItems = !list.isEmpty();
                for (Object entry : list ) {
                    context.put(variableName, entry );
                    for (Block block : baseBlocks) {
                        block.render(context, writer);
                    }
                }
            } else if (value instanceof Map<?,?>) {
                Map<?,?> dict = (Map<?,?>) value;
                hasItems = !dict.isEmpty();
                for (Map.Entry<?,?> entry : dict.entrySet()) {
                    context.put(variableName, entry );
                    for (Block block : baseBlocks) {
                        block.render(context, writer);
                    }
                }
            } else if (value instanceof Object[]) {
                Object[] list = (Object[]) value;
                hasItems = list.length > 0;
                for (Object entry : list ) {
                    context.put(variableName, entry );
                    for (Block block : baseBlocks) {
                        block.render(context, writer);
                    }
                }
            } else if (value instanceof Iterator) {
                Iterator iter = (Iterator) value;
                while (iter.hasNext()) {
                    Object entry = iter.next();
                    context.put(variableName, entry );
                    for (Block block : baseBlocks) {
                        block.render(context, writer);
                    }
                    if (! hasItems) {
                        hasItems = true;
                    }
                }
            } else if (value instanceof Iterable) {
                Iterator iter = ((Iterable) value).iterator();
                while (iter.hasNext()) {
                    Object entry = iter.next();
                    context.put(variableName, entry );
                    for (Block block : baseBlocks) {
                        block.render(context, writer);
                    }
                    if (! hasItems) {
                        hasItems = true;
                    }
                }
            } else if (value != null) {
                throw new UnsupportedOperationException("Non-iterable type for `"+iterableExpr+"`: "+value.getClass().getName());
            }

            // If collection is null or empty, render else blocks
            if (!hasItems) {
                for (Block block : elseBlocks) {
                    block.render(context, writer);
                }
            }
        }
    }

    // If block
    private static class IfBlock implements Block {
        private final List<Group> elifGroups;
        private final List<Block> elseBlocks;

        public IfBlock(List<Group> elifGroups, List<Block> elseBlocks) {
            this.elifGroups = elifGroups;
            this.elseBlocks = elseBlocks;
        }

        @Override
        public void render(Map<String, Object> context, Writer writer) throws IOException {
            for (Group group : elifGroups) {
                // Parse and evaluate complex expression
                String condition = group.condition;
                Object value = getValue(condition, context);
                if (decide(value)) {
                    List<Block> elifBlocks = group.blocks;
                    if (elifBlocks != null) {
                        for (Block block : elifBlocks) {
                            block.render(context, writer);
                        }
                    }
                    return;
                }
            }
            // If none of the conditions are true, render the else blocks
            for (Block block : elseBlocks) {
                block.render(context, writer);
            }
        }

        static class Group {
            private final String condition;
            private List <Block> blocks;

            public Group (String condition, List<Block> blocks) {
                this.condition = condition;
                this.blocks    = blocks;
            }
        }

    }

    // Include block
    private static class InBlock implements Block {
        private final List<Block> innerBlocks;
        private final String contextExpr;

        public InBlock(Path basePath, String includePath, String contextExpr) {
            if (basePath == null) {
                throw new UnsupportedOperationException("Include directive requires a basePath to be set");
            }

            try {
                // Load the included template file
                Path currPath = Path.of(basePath.toString(), includePath);
                if (!currPath.toFile().exists()) {
                    throw new IOException("Template file not found: "+currPath.toAbsolutePath());
                }

                this.innerBlocks = parseTemplate(Files.readString(currPath), currPath.getParent());
                this.contextExpr = contextExpr;
            }
            catch (IOException ex) {
                throw new UnsupportedOperationException(ex);
            }
        }

        @Override
        public void render(Map<String, Object> context, Writer writer) throws IOException {
            // Determine which context to use
            Map<String, Object> renderContext = context;
            if (contextExpr != null) {
                Object value = getValue(contextExpr, context);
                if (value instanceof Map<?, ?>) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> subMap = (Map<String, Object>) value;
                    renderContext = new HashMap(subMap);
                } else {
                    renderContext = new HashMap();
                }
                renderContext.put("__FUNC__", context.get("__FUNC__"));
                renderContext.put("__VARS__", context.get("__VARS__"));
            }

            // Render the included template
            for (Block block : innerBlocks) {
                block.render(renderContext, writer);
            }
        }
    }

    // Shared expression evaluation methods
    private static Object getValue(String expression, Map<String, Object> context) {
        // Trim whitespace
        expression = expression.trim();

        // Check if it's a null or boolean literal
        if (expression.equals("null")) {
            return null;
        } else
        if (expression.equals("true")) {
            return true;
        } else
        if (expression.equals("false")) {
            return false;
        }

        /**
         * "abc" == def && def != "xyz" 会被解析成一个字符串,
         * 两头为 JSON 也导致同样的问题.
         * 虽可用 contains 速判边界中断,
         * 但这也需要遍历字符串.
        if (expression.length() > 1) {
            // Check if it's a pure string literal (no operators)
            if (expression.startsWith("\"") && expression.endsWith("\"")) {
                return expression.substring(1, expression.length() - 1);
            }

            // Check if it's a JSON literal (array or object)
            if (expression.startsWith("[") && expression.endsWith("]")) {
                try {
                    return parseJson(expression, context);
                } catch (Exception ex) {
                    // Not a valid JSON literal, try expression evaluation
                }
            }
            if (expression.startsWith("{") && expression.endsWith("}")) {
                try {
                    return parseJson(expression, context);
                } catch (Exception ex) {
                    // Not a valid JSON literal, try expression evaluation
                }
            }
        }
        */

        // Try to evaluate as arithmetic expression
        try {
            return new Expression(expression).evaluate(context);
        } catch (Exception ex) {
            throw  new IllegalArgumentException(ex.getMessage()+" EXPR: "+expression, ex);
        }
    }

    private static Object fetchData(String path, Map<String, Object> context) {
        String[] parts = path.split("\\.");
        Object value = context.get(parts[0]);

        for (int i = 1; i < parts.length && value != null; i++) {
            String part = parts[i];
            if (value instanceof Map.Entry) {
                if ( "key" .equals(part)) {
                    value = ((Map.Entry) value).getKey(  );
                } else
                if ("value".equals(part)) {
                    value = ((Map.Entry) value).getValue();
                } else {
                    value = null;
                }
            } else if (value instanceof Map ) {
                value = ((Map) value).get(part);
            } else if (value instanceof List) {
                int j = Integer.parseInt (part);
               List a = (List) value;
                if (j < a.size()) {
                    value = a.get(j);
                } else {
                    value = null;
                }
            } else if (value instanceof Object[]) {
                int j = Integer.parseInt (part);
               Object[] a = (Object[]) value;
                if (j < a.length) {
                    value = a[j];
                } else {
                    value = null;
                }
            } else {
                value = null;
            }
        }

        return value;
    }

    private static Object parseJson(String json, Map<String, Object> context) {
        // Simple JSON literal parser for arrays and objects
        if (json.startsWith("[")) {
            // Parse array
            List<Object> list = new ArrayList<>();
            String content = json.substring(1, json.length() - 1).trim();
            if (!content.isEmpty()) {
                String[] elements = content.split(",");
                for (String element : elements) {
                    element = element.trim();
                    Object value;
                    if (element.startsWith("\"")) {
                        value = element.substring(1, element.length() - 1);
                    } else if (element.matches("-?\\d+")) {
                        value = Integer.parseInt(element);
                    } else if (element.matches("-?\\d+\\.\\d+")) {
                        value = Double.parseDouble(element);
                    } else {
                        value = fetchData(element, context);
                    }
                    list.add(value);
                }
            }
            return list;
        } else if (json.startsWith("{")) {
            // Parse object
            Map<String, Object> map = new HashMap<>();
            String content = json.substring(1, json.length() - 1).trim();
            if (!content.isEmpty()) {
                String[] elements = content.split(",");
                for (String element : elements) {
                    element = element.trim();
                    String[] elemVal = element.split(":");
                    if (elemVal.length == 2) {
                        String elemKey = elemVal[0].trim().substring(1, elemVal[0].length() - 1);
                        String textVal = elemVal[1].trim();
                        Object value;
                        if (textVal.startsWith("\"")) {
                            value = textVal.substring(1, textVal.length() - 1);
                        } else if (textVal.matches("-?\\d+")) {
                            value = Integer.parseInt(textVal);
                        } else if (textVal.matches("-?\\d+\\.\\d+")) {
                            value = Double.parseDouble(textVal);
                        } else {
                            value = fetchData(textVal, context);
                        }
                        map.put(elemKey, value);
                    }
                }
            }
            return map;
        }
        return null;
    }

    // Expression parser class
    private static class Expression {
        private final String expression;
        private int pos;

        public Expression(String expression) {
            this.expression = expression.trim();
        }

        public Object evaluate(Map<String, Object> context) {
            this.pos = 0;
            return parseTernary(context);
        }

        private void skipWhitespace() {
            while (pos < expression.length() && Character.isWhitespace(expression.charAt(pos))) {
                pos++;
            }
        }

        private Object parseTernary(Map<String, Object> context) {
            Object cond = parseLogical(context);
            skipWhitespace();
            if (pos < expression.length() && expression.charAt(pos) == '?') {
                pos++;
                Object left = parseTernary(context);
                skipWhitespace();
                if (pos < expression.length() && expression.charAt(pos) == ':') {
                    pos++;
                } else {
                    throw new IllegalArgumentException("Missing colon in ternary operator");
                }
                //Object right = parseTernary(context);
                //return decide(cond) ? left : right;

                // 惰性求值, 满足条件则不再处理右边
                if (decide(cond)) {
                    return left;
                }
                return parseTernary(context);
            }
            return cond;
        }

        private Object parseLogical(Map<String, Object> context) {
            Object left = parseCompare(context);
            while (pos < expression.length()) {
                skipWhitespace();
                if (pos >= expression.length()) break;
                // 检查是否是三元操作符的开始
                if (expression.charAt(pos) == '?') {
                    break;
                }
                String op = parseOperator();
                if (!isLogicalOp(op)) {
                    // Not a logical operator, put the operator back
                    pos -= op.length();
                    break;
                }
                //Object right = parseCompare(context);
                //left = evaluateLogicalOp(left, op, right);

                // 惰性求值，左边满足条件则不再继续
                if ("&&".equals(op)) {
                    if (! decide(left)) {
                        return left;
                    }
                } else
                if ("||".equals(op)) {
                    if (decide(left)) {
                        return left;
                    }
                }
                left = parseCompare(context);
            }
            return left;
        }

        private Object parseCompare(Map<String, Object> context) {
            Object left = parseAddSubs(context);
            while (pos < expression.length()) {
                skipWhitespace();
                if (pos >= expression.length()) break;
                // 检查是否是三元操作符的开始
                if (expression.charAt(pos) == '?') {
                    break;
                }
                String op = parseOperator();
                if (!isCompareOp(op)) {
                    // Not a comparison operator, put the operator back
                    pos -= op.length();
                    break;
                }
                Object right = parseAddSubs(context);
                left = evaluateCompareOp(left, op, right);
            }
            return left;
        }

        private Object parseAddSubs(Map<String, Object> context) {
            Object left = parseMulDivs(context);
            while (pos < expression.length()) {
                skipWhitespace();
                if (pos >= expression.length()) break;
                char op = expression.charAt(pos);
                if (op != '+' && op != '-') break;
                pos++;
                Object right = parseMulDivs(context);
                left = evaluateNumericOp(left, op, right);
            }
            return left;
        }

        private Object parseMulDivs(Map<String, Object> context) {
            Object left = parsePrimary(context);
            while (pos < expression.length()) {
                skipWhitespace();
                if (pos >= expression.length()) break;
                char op = expression.charAt(pos);
                if (op != '*' && op != '/' && op != '%') break;
                pos++;
                Object right = parsePrimary(context);
                left = evaluateNumericOp(left, op, right);
            }
            return left;
        }

        private Object parsePrimary(Map<String, Object> context) {
            skipWhitespace();
            if (pos >= expression.length()) {
                throw new IllegalArgumentException("Unexpected end of expression");
            }

            char c = expression.charAt(pos);
            if (c == '!') {
                pos++;
                Object value = parsePrimary(context);
                return ! decide(value);
            } else if (c == '(') {
                pos++;
                Object value = parseTernary(context);
                skipWhitespace();
                if (pos >= expression.length() || expression.charAt(pos) != ')') {
                    throw new IllegalArgumentException("Missing closing parenthesis");
                }
                pos++;
                return value;
            } else if (Character.isDigit(c) || c == '-' || c == '+') {
                return parseNumber();
            } else if (c == '"') {
                return parseString();
            } else if (c == '[' || c == '{') {
                return parseJson(context);
            } else {
                return parseFunc(context);
            }
        }

        private String parseOperator() {
            StringBuilder op = new StringBuilder();

            // Parse other operators
            while (pos < expression.length() &&
                !Character.isWhitespace(expression.charAt(pos)) &&
               (expression.charAt(pos) == '=' || expression.charAt(pos) == '!' ||
                expression.charAt(pos) == '>' || expression.charAt(pos) == '<' ||
                expression.charAt(pos) == '&' || expression.charAt(pos) == '|')) {
                op.append(expression.charAt(pos));
                pos++;
            }
            return op.toString();
        }

        private Object parseNumber() {
            int start = pos;
            // 正负号
            if (pos < expression.length() && (expression.charAt(pos) == '-' || expression.charAt(pos) == '+')) {
                pos++;
            }
            while (pos < expression.length() && (Character.isDigit(expression.charAt(pos)) || expression.charAt(pos) == '.')) {
                pos++;
            }
            String numStr = expression.substring(start, pos);
            return toNumber(numStr);
        }

        private String parseString() {
            pos++;
            int start = pos;
            while (pos < expression.length() && expression.charAt(pos) != '"') {
                pos++;
            }
            if (pos >= expression.length()) {
                throw new IllegalArgumentException("Unclosed string literal");
            }
            String result = expression.substring(start, pos);
            pos++;
            return result;
        }

        private Object parseJson(Map context) {
            int start = pos;
            char openingChar = expression.charAt(pos);
            int depth = 1;
            pos++;

            while (pos < expression.length() && depth > 0) {
                char c = expression.charAt(pos);
                if (c == '"') {
                    // Skip string content
                    pos++;
                    while (pos < expression.length() && expression.charAt(pos) != '"') {
                        if (expression.charAt(pos) == '\\') {
                            pos++;
                        }
                        pos++;
                    }
                    if (pos < expression.length()) {
                        pos++;
                    }
                } else if (c == openingChar) {
                    depth++;
                    pos++;
                } else if (c == (openingChar == '[' ? ']' : '}')) {
                    depth--;
                    pos++;
                } else {
                    pos++;
                }
            }

            if (depth != 0) {
                throw new IllegalArgumentException("Unclosed JSON literal");
            }

            String jsonStr = expression.substring(start, pos);
            return Template.parseJson(jsonStr, context);
        }

        private Object parseFunc(Map<String, Object> context) {
            int start = pos;
            while (pos < expression.length() && (Character.isLetterOrDigit(expression.charAt(pos)) || expression.charAt(pos) == '.' || expression.charAt(pos) == '_')) {
                pos++;
            }
            String name = expression.substring(start, pos);

            // Check if it's a function call
            skipWhitespace();
            if (pos < expression.length() && expression.charAt(pos) == '(') {
                pos++;
                List<Object> args = parseArgs(context);
                skipWhitespace();
                if (pos >= expression.length() || expression.charAt(pos) != ')') {
                    throw new IllegalArgumentException("Missing closing parenthesis for function call");
                }
                pos++;

                // Call function
                Map<String, Function<Object[], Object>> functions = (Map) context.get("__FUNC__");
                if (functions == null) {
                    throw new IllegalArgumentException("Unknown function! " + name);
                }
                Function<Object[], Object> function = functions.get(name);
                if (function == null) {
                    throw new IllegalArgumentException("Unknown function: " + name);
                }

                return function.apply(args.toArray());
            }

            // Otherwise, it's a variable
            if (! name.isEmpty()) {
                Object  val = fetchData(name, context);

                // Get global variable
                if (val == null) {
                    Map<String,Object> globals = (Map) context.get("__VARS__");
                    if (globals != null) {
                        val = fetchData(name, globals);
                    if (val instanceof Supplier) {
                        val = ( (Supplier) val ).get();
                    }}
                }

                return val;
            }
            return null;
        }

        private List<Object> parseArgs(Map<String, Object> context) {
            List<Object> args = new ArrayList<>();
            skipWhitespace();

            if (pos < expression.length() && expression.charAt(pos) == ')') {
                return args; // No arguments
            }

            do {
                args.add(parseLogical(context));
                skipWhitespace();
            } while (pos < expression.length() && expression.charAt(pos) == ',' && (pos++ > 0));

            return args;
        }

        private Object evaluateNumericOp(Object left, char op, Object right) {
            // 一边是字符串则可连接
            if (op == '+'
            && (left instanceof String
            || right instanceof String)) {
                return toString(left) + toString(right);
            }

            double leftVal  = toNumber(left );
            double rightVal = toNumber(right);

            try {
                switch (op) {
                    case '+':
                        return leftVal + rightVal;
                    case '-':
                        return leftVal - rightVal;
                    case '*':
                        return leftVal * rightVal;
                    case '/':
                        return leftVal / rightVal;
                    case '%':
                        return leftVal % rightVal;
                    default :
                        throw new IllegalArgumentException("Unknown operator: " + op);
                }
            }
            catch (ArithmeticException ex) {
                throw new IllegalArgumentException(ex);
            }
        }

        private Object evaluateCompareOp(Object left, String op, Object right) {
            switch (op) {
                case "==":
                    return  equals(left, right);
                case "!=":
                    return !equals(left, right);
                case ">" :
                    return compare(left, right) >  0;
                case "<" :
                    return compare(left, right) <  0;
                case ">=":
                    return compare(left, right) >= 0;
                case "<=":
                    return compare(left, right) <= 0;
                default:
                    throw new IllegalArgumentException("Unknown compare operator: " + op);
            }
        }

        private Object evaluateLogicalOp(Object left, String op, Object right) {
            switch (op) {
                case "&&":
                    if (! decide(left)) {
                        return left;
                    }
                    return right;
                case "||":
                    if (decide(left)) {
                        return left;
                    }
                    return right;
                default:
                    throw new IllegalArgumentException("Unknown logical operator: " + op);
            }
        }

        private boolean isLogicalOp(String op) {
            return op.equals("&&") || op.equals("||");
        }

        private boolean isCompareOp(String op) {
            return op.equals("==") || op.equals("!=") ||
                   op.equals(">=") || op.equals("<=") ||
                   op.equals(">" ) || op.equals("<" );
        }

        private boolean equals(Object left, Object right) {
            if (left == null && right == null) {
                return true;
            }
            if (left == null || right == null) {
                return false;
            }

            // 一边是数字则比对数值
            if (left  instanceof Number
            ||  right instanceof Number) {
                return compare(left, right) == 0;
            }

            return left.equals(right);
        }

        private int compare(Object left, Object right) {
            if (left == null || right == null) {
                if (left == null && right == null) {
                    return  0;
                } else if (left == null) {
                    return -1;
                } else {
                    return  1;
                }
            }

            double leftVal  = toNumber(left );
            double rightVal = toNumber(right);
            return Double.compare(leftVal, rightVal);
        }

        private double toNumber(Object obj) {
            Number n = Synt.asDouble(obj);
            if (n == null) {
                throw new ClassCastException("Null can not be cast to number");
            }
            return n.doubleValue();
        }

        private String toString(Object obj) {
            String s = Synt.asString(obj);
            if (s == null) {
                return "";
            }
            return s;
        }

    }

    public static final Map<String, Supplier<Object>> VARIABLES = new HashMap();
    static {
        VARIABLES.put("SERVER_PATH", () -> Core.SERVER_PATH.get());
        VARIABLES.put("SERVER_HREF", () -> Core.SERVER_HREF.get());
        VARIABLES.put("BASE_HREF", () -> Core.SERVER_HREF.get() + Core.SERVER_PATH.get());
    }

    public static final Map<String, Function<Object[], Object>> FUNCTIONS = new HashMap();
    static {
        FUNCTIONS.put("contains", args -> {
            Object a = args[1];
            Object o = args[0];
            if (a instanceof String) {
                return ((String) a).contains((String) o);
            } else
            if (a instanceof Collection) {
                return ((Collection) a).contains(o);
            } else
            if (a instanceof Map) {
                return ((Map) a).containsKey(o);
            }
            return false;
        });

        FUNCTIONS.put("matches", args -> {
            String s = Synt.asString(args[0]);
            String r = Synt.asString(args[1]);
            return s.matches(r);
        });

        FUNCTIONS.put("substr", args -> {
            String s = Synt.asString(args[0]);
               int b = Synt.asInt(args[1]);
            if (args.length > 2) {
               int d = Synt.asInt(args[2]);
               return Syno.substr(s, b, d);
            } else {
               return Syno.substr(s, b);
            }
        });

        FUNCTIONS.put("indent", args -> {
            String s = Synt.asString(args[0]);
            if (s == null || "".equals(s)) {
                return "";
            }
            int n = args.length > 1 ? Synt.declare(args[1], 2) : 2;
            return Syno.indent(s, " ".repeat(n)).trim();
        });

        FUNCTIONS.put("concat", args -> {
            Collection o = Synt.asColl(args[0]);
            if (o == null) {
                return "";
            }
            String s = args.length > 1 ? Synt.declare(args[1], ", ") : ", ";
            return Syno.concat(s, o).trim();
        });

        FUNCTIONS.put("format", args -> {
            String s = Synt.asString(args[0]);
            Object[] newArgs = Arrays.copyOfRange(args, 1, args.length);
            return String.format(s, newArgs);
        });

        FUNCTIONS.put("date_format", args -> {
            Object d = args[0];
            String f = Synt.asString(args[1]);
            if (d == null || "".equals(d)) {
                return "";
            } else
            if (d instanceof Date) {
                return Inst.format((Date) d, f);
            } else
            if (d instanceof Instant) {
                return Inst.format((Instant) d, f);
            } else
            {
                return Inst.format(Synt.asLong(d), f);
            }
        });

        FUNCTIONS.put("strip", args -> {
            String st = Synt.declare(args[0], "");
            Set    sa = Synt. toSet (args[1]);
            if (sa == null) {
                sa = Synt.setOf("trim"); // 默认清除首尾空字符
            }
            if (! sa.isEmpty()) {
                if (sa.contains("cros") || sa.contains("html")) {
                    st = Syno.stripCros(st); // 清除脚本
                }
                if (sa.contains("tags") || sa.contains("html")) {
                    st = Syno.stripTags(st); // 清除标签
                }
                if (sa.contains("trim") || sa.contains("html")) {
                    st = Syno.strip    (st); // 清理首尾
                }
                if (sa.contains("gaps")) {
                    st = Syno.stripGaps(st); // 清除空行
                }
                if (sa.contains("ends")) {
                    st = Syno.stripEnds(st); // 清除换行
                }
                if (sa.contains("unis")) {
                    st = Syno.unifyEnds(st); // 统一换行
                }
            }
            return  st;
        });

        FUNCTIONS.put("count", args -> {
            Object o = args[0];
            if (o instanceof Map m) {
                return m.size();
            }
            if (o instanceof Collection c) {
                return c.size();
            }
            if (o instanceof Object [ ] a) {
                return a.length;
            }
            if (o instanceof String s) {
                return s.length();
            }
            return 0;
        });

        FUNCTIONS.put("range", args -> {
            final int s = Synt.declare(args[0], 0); // 开始
            final int e = Synt.declare(args[1], 0); // 结束
            final int d = Synt.declare(args.length > 2 ? args[2] : 1, 1); // 步长
            if (d == 0) {
                throw new IllegalArgumentException("Range step can not be 0");
            }
            if (d <  0) {
                return new Iterator () {
                    int i = s;

                    @Override
                    public boolean hasNext() {
                        return i > e && i <= s;
                    }

                    @Override
                    public Object next() {
                        int j = i;
                        i = i + d;
                        return  j;
                    }
                };
            } else {
                return new Iterator () {
                    int i = s;

                    @Override
                    public boolean hasNext() {
                        return i < e && i >= s;
                    }

                    @Override
                    public Object next() {
                        int j = i;
                        i = i + d;
                        return  j;
                    }
                };
            }
        });
    }

}