package io.github.ihongs.util;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Date;
import java.util.Collection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.regex.Pattern;

/**
 * 极简模板引擎
 *
 * 兼容最基础的 Jinja 语法
 * @see https://jinja.flask.org.cn/en/3.1.x/templates/#base-template
 * 支持 {{}},{%%},{##} 三种标签
 * 支持 if/elif/else/endif, for/else/endfor, set, include 指令
 * 其中 include 支持指定的上下文，如 {%include "sub.temp" with subContext%} subContext 为传递给子模板的上下文变量
 *
 * 范例:
 * <code>
 * # {{title}}
 * {%for item in list%}
 * - {{item.name}}: {{item.msg}}
 *   {%if item.age > 18%}
 *   Adult
 *   {%else%}
 *   Child
 *   {%endif%}
 * {%else%}
 * **Empty!**
 * {%endfor%}
 * </code>
 *
 * @author Hongs
 */
public class Template {

    private final List<Block> blocks;
    private final Map<String, Function<Object[], Object>> functions;
    private final static Pattern TEMP_LINE = Pattern.compile("^\\s*(\\{%(?!\\s*include\\s+).*?%\\}|\\{#.*?#\\})\\s*$");

    private Template(List<Block> blocks) {
        this.blocks = blocks;
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
     * 模板勾在
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
     */
    public void regist(String name, Function<Object[], Object> function ) {
        this.functions.put(name, function);
    }

    /**
     * 注册函数
     * @param functions
     */
    public void regist(Map<String, Function<Object[], Object>> functions) {
        this.functions.putAll( functions );
    }

    public void render(Map<String, Object> context, Writer writer) throws IOException {
        context.put("__FUNC__", functions);
        for (Block block : blocks) {
            block.render(context, writer);
        }
    }

    public String render(Map<String, Object> context) {
        StringWriter writer = new StringWriter();
        try {
            render(context, writer);
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
                int p0  = baseTemp.lastIndexOf("\n", bPos + end);
                int p1  = baseTemp.    indexOf("\n", bPos + end);
                if (p0 != -1 && p1 != -1) {
                    String line = baseTemp.substring(p0, p1);
                    if (TEMP_LINE.matcher(line).matches()) {
                        end = p0 - bPos;
                    }
                }

                String text = template.substring(index, end);
                blocks.add(new TxtBlock(text));
            }
                break;
            }

            // Add plain text before the directive
            if (nextStart > index) {
                // 清理独行指令空白
                int end = nextStart;
                int p0  = baseTemp.lastIndexOf("\n", bPos + end);
                int p1  = baseTemp.    indexOf("\n", bPos + end);
                if (p0 != -1 && p1 != -1) {
                    String line = baseTemp.substring(p0, p1);
                    if (TEMP_LINE.matcher(line).matches()) {
                        end = p0 - bPos;
                    }
                }

                String text = template.substring(index, end);
                blocks.add(new TxtBlock(text));
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
                    int currentLine = countLines(baseTemp.substring(0, bPos + dirStart)) + 1;
                    throw new IllegalArgumentException("Line " + currentLine + ": Unclosed directive");
                }
                end += 2;

                // Extract the directive content
                String directive = template.substring(dirStart + 2, end - 2).trim();

                if (directive.startsWith("if ")) {
                    // If block
                    String condition = directive.substring(3).trim();
                    // Find matching endif
                    int endifStart = findMatchingEnd(template, end, "endif");
                    if (endifStart == -1) {
                        int currentLine = countLines(baseTemp.substring(0, bPos + dirStart)) + 1;
                        throw new IllegalArgumentException("Line " + currentLine + ": Unclosed if statement");
                    }
                    int endifEnd = template.indexOf("%}", endifStart);
                    if (endifEnd == -1) {
                        int currentLine = countLines(baseTemp.substring(0, bPos + dirStart)) + 1;
                        throw new IllegalArgumentException("Line " + currentLine + ": Unclosed if statement");
                    }
                    endifEnd += 2;

                    List<IfBlock.InBlock> conditionalBlocks = new ArrayList<>();
                    List<Block> elseBlocks = new ArrayList<>();

                    // Add the first condition
                    conditionalBlocks.add(new IfBlock.InBlock(condition, new ArrayList<>()));

                    int currentStart = end;
                    int currentPos = end;

                    while (currentPos < endifStart) {
                        // Find the next else or elif directive
                        int nextDirectiveStart = template.indexOf("{%", currentPos);
                        if (nextDirectiveStart == -1 || nextDirectiveStart >= endifStart) {
                            // No more directives, process the last condition
                            //String content = template.substring(currentStart, endifStart);
                            List<Block> innerBlocks = parseTemplate(baseTemp, basePath, bPos + currentStart, bPos + endifStart);
                            conditionalBlocks.get(conditionalBlocks.size() - 1).setBlocks(innerBlocks);
                            break;
                        }

                        // Check if this is an else or elif directive
                        int directiveEnd = template.indexOf("%}", nextDirectiveStart);
                        if (directiveEnd == -1) {
                            int currentLine = countLines(baseTemp.substring(0, bPos + currentPos)) + 1;
                            throw new IllegalArgumentException("Line " + currentLine + ": Unclosed directive");
                        }
                        String nextDirective = template.substring(nextDirectiveStart + 2, directiveEnd).trim();

                        if (nextDirective.equals("else")) {
                            // Process the current condition's content
                            //String content = template.substring(currentStart, nextDirectiveStart);
                            List<Block> innerBlocks = parseTemplate(baseTemp, basePath, bPos + currentStart, bPos + nextDirectiveStart);
                            conditionalBlocks.get(conditionalBlocks.size() - 1).setBlocks(innerBlocks);

                            // Process else block
                            //String elseContent = template.substring(directiveEnd + 2, endifStart);
                            elseBlocks = parseTemplate(baseTemp, basePath, bPos + directiveEnd + 2, bPos + endifStart);
                            break;
                        } else
                        if (nextDirective.startsWith("elif ")) {
                            // Process the current condition's content
                            //String content = template.substring(currentStart, nextDirectiveStart);
                            List<Block> innerBlocks = parseTemplate(baseTemp, basePath, bPos + currentStart, bPos + nextDirectiveStart);
                            conditionalBlocks.get(conditionalBlocks.size() - 1).setBlocks(innerBlocks);

                            // Add a new conditional block for elif
                            String elifCondition = nextDirective.substring(5).trim();
                            conditionalBlocks.add(new IfBlock.InBlock(elifCondition, new ArrayList<>()));
                            currentStart = directiveEnd + 2;
                            currentPos = currentStart;
                        } else {
                            // Not an else/elif, continue searching
                            currentPos = directiveEnd + 2;
                        }
                    }

                    // Create the IfBlock with all conditional blocks and else blocks
                    blocks.add(new IfBlock(conditionalBlocks, elseBlocks));
                    index = endifEnd;
                } else
                if (directive.startsWith("for ")) {
                    // For block
                    String[] parts = directive.substring(4).trim().split("\\s+in\\s+");
                    if (parts.length != 2) {
                        int currentLine = countLines(baseTemp.substring(0, bPos + dirStart)) + 1;
                        throw new IllegalArgumentException("Line " + currentLine + ": Invalid for statement: " + directive);
                    }
                    String variableName = parts[0].trim();
                    String collectionName = parts[1].trim();
                    // Find matching endfor
                    int endforStart = findMatchingEnd(template, end, "endfor");
                    if (endforStart == -1) {
                        int currentLine = countLines(baseTemp.substring(0, bPos + dirStart)) + 1;
                        throw new IllegalArgumentException("Line " + currentLine + ": Unclosed for statement");
                    }
                    int endforEnd = template.indexOf("%}", endforStart);
                    if (endforEnd == -1) {
                        int currentLine = countLines(baseTemp.substring(0, bPos + dirStart)) + 1;
                        throw new IllegalArgumentException("Line " + currentLine + ": Unclosed for statement");
                    }
                    endforEnd += 2;

                    // Check for else clause in for loop
                    int elseStart = -1;
                    int currentPos = end;
                    int nestedForCount = 0;
                    int nestedIfCount = 0;

                    while (currentPos < endforStart) {
                        int nestedDirStart = template.indexOf("{%", currentPos);
                        if (nestedDirStart == -1 || nestedDirStart >= endforStart) {
                            break;
                        }

                        // Find the correct closing %} by scanning character by character
                        int nestedDirEnd = nestedDirStart + 2;
                        while (nestedDirEnd < template.length()) {
                            if (template.charAt(nestedDirEnd) == '%' && nestedDirEnd + 1 < template.length() && template.charAt(nestedDirEnd + 1) == '}') {
                                // Found valid closing %}
                                break;
                            }
                            nestedDirEnd++;
                        }
                        if (nestedDirEnd >= template.length() || nestedDirEnd >= endforStart) {
                            break;
                        }
                        nestedDirEnd += 2;

                        String nestedDirective = template.substring(nestedDirStart + 2, nestedDirEnd - 2).trim();

                        if (nestedDirective.startsWith("for ")) {
                            nestedForCount++;
                        } else
                        if (nestedDirective.equals("endfor")) {
                            if (nestedForCount > 0) {
                                nestedForCount--;
                            }
                        } else
                        if (nestedDirective.startsWith("if ")) {
                            nestedIfCount++;
                        } else
                        if (nestedDirective.equals("endif")) {
                            if (nestedIfCount > 0) {
                                nestedIfCount--;
                            }
                        } else if (nestedDirective.equals("else") && nestedForCount == 0 && nestedIfCount == 0) {
                            // Found else directive for this for loop
                            elseStart = nestedDirStart;
                            break;
                        }

                        currentPos = nestedDirEnd;
                    }

                    List<Block> innerBlocks;
                    List<Block> elseBlocks = new ArrayList<>();

                    if (elseStart != -1 && elseStart < endforStart) {
                        // Found else clause
                        //String loopContent = template.substring(end, elseStart);
                        innerBlocks = parseTemplate(baseTemp, basePath, bPos + end, bPos + elseStart);

                        // Find the end of else directive
                        int elseEnd = template.indexOf("%}", elseStart);
                        if (elseEnd == -1) {
                            int currentLine = countLines(baseTemp.substring(0, bPos + currentPos)) + 1;
                            throw new IllegalArgumentException("Line " + currentLine + ": Unclosed else directive");
                        }
                        elseEnd += 2;

                        //String elseContent = template.substring(elseEnd, endforStart);
                        elseBlocks = parseTemplate(baseTemp, basePath, bPos + elseStart, bPos + endforStart);
                    } else {
                        // No else clause
                        //String content = template.substring(end, endforStart);
                        innerBlocks = parseTemplate(baseTemp, basePath, bPos + end, bPos + endforStart);
                    }

                    blocks.add(new ForBlock(variableName, collectionName, innerBlocks, elseBlocks));
                    index = endforEnd;
                } else
                if (directive.startsWith("set ")) {
                    // Set block
                    String[] parts = directive.substring(4).trim().split("\\s*=\\s*");
                    if (parts.length != 2) {
                        int currentLine = countLines(baseTemp.substring(0, bPos + dirStart)) + 1;
                        throw new IllegalArgumentException("Line " + currentLine + ": Invalid set statement: " + directive);
                    }
                    String variableName = parts[0].trim();
                    String valueExpression = parts[1].trim();
                    blocks.add(new SetBlock(variableName, valueExpression));
                    index = end;
                } else
                if (directive.startsWith("include ")) {
                    // Include block
                    String includePart = directive.substring(8).trim();
                    String includeName = null;
                    String subContext = null;

                    // Find the first quote
                    int firstQuote = includePart.indexOf('"');
                    if (firstQuote == -1) {
                        int currentLine = countLines(baseTemp.substring(0, bPos + dirStart)) + 1;
                        throw new IllegalArgumentException("Line " + currentLine + ": Include template name must be quoted");
                    }

                    // Find the matching closing quote
                    int lastQuote = includePart.indexOf('"', firstQuote + 1);
                    if (lastQuote == -1) {
                        int currentLine = countLines(baseTemp.substring(0, bPos + dirStart)) + 1;
                        throw new IllegalArgumentException("Line " + currentLine + ": Include template name quote unclosed");
                    }

                    // Extract the template name
                    includeName = includePart.substring(firstQuote + 1, lastQuote);

                    // Check for with clause after the closing quote
                    String remaining = includePart.substring(lastQuote + 1).trim();
                    if (remaining.startsWith("with ")) {
                        subContext = remaining.substring(5).trim();
                    }

                    blocks.add(new SubBlock(basePath, includeName, subContext));
                    index = end;
                } else
                if (directive.equals("else")
                ||  directive.equals("endif")
                ||  directive.equals("endfor")) {
                    // Else directive - should only be found by the if or for block parser
                    // End directive - should only be found by findMatchingEnd
                    int currentLine = countLines(baseTemp.substring(0, bPos + dirStart)) + 1;
                    throw new IllegalArgumentException("Line " + currentLine + ": Unexpected directive: " + directive);
                } else {
                    // Unknown directive
                    int currentLine = countLines(baseTemp.substring(0, bPos + dirStart)) + 1;
                    throw new IllegalArgumentException("Line " + currentLine + ": Unknown directive: " + directive);
                }
            } else
            if (nextStart == varStart) {
                // Variable block
                int end = template.indexOf("}}", varStart);
                if (end == -1) {
                    int currentLine = countLines(baseTemp.substring(0, bPos + varStart)) + 1;
                    throw new IllegalArgumentException("Line " + currentLine + ": Unclosed variable");
                }
                String variableName = template.substring(varStart + 2, end).trim();
                blocks.add(new VarBlock(variableName));
                index = end + 2;
            } else
            if (nextStart == comStart) {
                // Comment block - skip
                int end = template.indexOf("#}", comStart);
                if (end == -1) {
                    int currentLine = countLines(baseTemp.substring(0, bPos + comStart)) + 1;
                    throw new IllegalArgumentException("Line " + currentLine + ": Unclosed comments");
                }
                index = end + 2;
            }
        }

        return blocks;
    }

    private static int findMatchingEnd(String template, int start, String endDirective) {
        int count = 1;
        int index = start;

        while (index < template.length()) {
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

            String directive = template.substring(dirStart + 2, dirEnd - 2).trim();

            if (directive.startsWith("if ")) {
                // Nested if
                if (endDirective.equals("endif")) {
                    count++;
                }
            } else
            if (directive.startsWith("for ")) {
                // Nested for
                if (endDirective.equals("endfor")) {
                    count++;
                }
            } else
            if (directive.equals("endif")) {
                // Endif directive
                if (endDirective.equals("endif")) {
                    count--;
                    if (count == 0) {
                        return dirStart;
                    }
                }
            } else
            if (directive.equals("endfor")) {
                // Endfor directive
                if (endDirective.equals("endfor")) {
                    count--;
                    if (count == 0) {
                        return dirStart;
                    }
                }
            }

            index = dirEnd;
        }

        return -1;
    }

    private static int countLines(String text) {
        if (text == null || text.isEmpty()) {
            return 0;
        }
        int count = 0;
        for (int i = 0; i < text.length(); i++) {
            if (text.charAt(i) == '\n') {
                count++;
            }
        }
        return count;
    }

    // Block interface
    private interface Block {
        void render(Map<String, Object> context, Writer writer) throws IOException;
    }

    // Plain text block
    private static class TxtBlock implements Block {
        private String text;

        public TxtBlock(String text) {
            this.text = text;
        }

        @Override
        public void render(Map<String, Object> context, Writer writer) throws IOException {
            writer.write(text);
        }
    }

    // Variable block
    private static class VarBlock implements Block {
        private final String name;

        public VarBlock(String variableName) {
            this.name = variableName;
        }

        @Override
        public void render(Map<String, Object> context, Writer writer) throws IOException {
            Object value = getValue(name, context);
            if (value != null) {
                writer.write(value.toString());
            }
        }
    }

    // If block
    private static class IfBlock implements Block {
        private final List<InBlock> innerBlocks;
        private final List<Block> elseBlocks;

        public IfBlock(List<InBlock> innerBlocks, List<Block> elseBlocks) {
            this.innerBlocks = innerBlocks;
            this.elseBlocks = elseBlocks;
        }

        @Override
        public void render(Map<String, Object> context, Writer writer) throws IOException {
            for (InBlock block : innerBlocks) {
                if (evaluateCondition(block.condition, context)) {
                    for (Block innerBlock : block.blocks) {
                        innerBlock.render(context, writer);
                    }
                    return;
                }
            }
            // If none of the conditions are true, render the else blocks
            for (Block block : elseBlocks) {
                block.render(context, writer);
            }
        }

        private boolean evaluateCondition(String condition, Map<String, Object> context) {
            // Parse and evaluate complex expression
            condition = condition.trim();
            Object result = getValue(condition, context);

            // Convert result to boolean
            if (result == null) {
                return false;
            } else if (result instanceof Boolean) {
                return (Boolean) result;
            } else if (result instanceof String) {
                return !((String) result).isEmpty();
            } else if (result instanceof Number) {
                return ((Number) result).doubleValue() != 0;
            } else {
                return true;
            }
        }

        static class InBlock {
            private final String condition;
            private List<Block> blocks;

            public InBlock(String condition, List<Block> blocks) {
                this.condition = condition;
                this.blocks = blocks;
            }

            public void setBlocks(List<Block> blocks) {
                this.blocks = blocks;
            }
        }

    }

    // For block
    private static class ForBlock implements Block {
        private final String variableName;
        private final String collectionName;
        private final List<Block> innerBlocks;
        private final List<Block> elseBlocks;

        public ForBlock(String variableName, String collectionName, List<Block> innerBlocks, List<Block> elseBlocks) {
            this.variableName = variableName;
            this.collectionName = collectionName;
            this.innerBlocks = innerBlocks;
            this.elseBlocks = elseBlocks;
        }

        @Override
        public void render(Map<String, Object> context, Writer writer) throws IOException {
            Object collection = getValue(collectionName, context);
            boolean hasItems = false;

            if (collection instanceof List<?>) {
                List<?> list = (List<?>) collection;
                hasItems = !list.isEmpty();
                for (Object item : list) {
                    Map<String, Object> loopContext = new HashMap<>(context);
                    loopContext.put(variableName, item);
                    for (Block block : innerBlocks) {
                        block.render(loopContext, writer);
                    }
                }
            } else if (collection instanceof Map<?, ?>) {
                Map<?, ?> map = (Map<?, ?>) collection;
                hasItems = !map.isEmpty();
                for (Map.Entry<?, ?> entry : map.entrySet()) {
                    Map<String, Object> loopContext = new HashMap<>(context);
                    loopContext.put(variableName, entry);
                    for (Block block : innerBlocks) {
                        block.render(loopContext, writer);
                    }
                }
            }

            // If collection is null or empty, render else blocks
            if (!hasItems) {
                for (Block block : elseBlocks) {
                    block.render(context, writer);
                }
            }
        }
    }

    // Set block
    private static class SetBlock implements Block {
        private final String variableName;
        private final String valueExpression;

        public SetBlock(String variableName, String valueExpression) {
            this.variableName = variableName;
            this.valueExpression = valueExpression;
        }

        @Override
        public void render(Map<String, Object> context, Writer writer) throws IOException {
            Object value = getValue(valueExpression, context);
            context.put(variableName, value);
        }
    }

    // Include block
    private static class SubBlock implements Block {
        private final List<Block> subBlocks;
        private final String subContext;

        public SubBlock(Path basePath, String subPath, String subContext) {
            if (basePath == null) {
                throw new UnsupportedOperationException("Include directive requires a basePath to be set");
            }

            try {
                // Load the included template file
                Path curPath = Path.of(basePath.toString(), subPath);
                if (!curPath.toFile().exists()) {
                    throw new IOException("Template file not found: " + curPath.toAbsolutePath());
                }

                this.subBlocks = parseTemplate(Files.readString(curPath), curPath.getParent());
                this.subContext = subContext;
            }
            catch (IOException ex) {
                throw new UnsupportedOperationException(ex);
            }
        }

        @Override
        public void render(Map<String, Object> context, Writer writer) throws IOException {
            // Determine which context to use
            Map<String, Object> renderContext = context;
            if (subContext != null) {
                Object subValue = getValue(subContext, context);
                if (subValue instanceof Map<?, ?>) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> subMap = (Map<String, Object>) subValue ;
                    renderContext = new HashMap(subMap);
                } else {
                    renderContext = new HashMap();
                }
                renderContext.put("__FUNC__", context.get("__FUNC__"));
            }

            // Render the included template
            for (Block block : subBlocks) {
                block.render(renderContext, writer);
            }
        }
    }

    // Shared expression evaluation methods
    private static Object getValue(String expression, Map<String, Object> context) {
        // Trim whitespace
        expression = expression.trim();

        // Check if it's a boolean literal
        if (expression.equals("true")) {
            return true;
        } else
        if (expression.equals("false")) {
            return false;
        }
        // Check if it's a string literal
        if (expression.startsWith("\"")) {
            return expression.substring(1, expression.length() - 1);
        }
        // Check if it's a JSON literal (array or object)
        if (expression.startsWith("[")
        ||  expression.startsWith("{")) {
            try {
                return parseJson(expression, context);
            } catch (Exception ex) {
                // Not a valid JSON literal, try expression evaluation
            }
        }
        // Try to evaluate as arithmetic expression
        try {
            return new Expression(expression).evaluate(context);
        } catch (Exception e) {
            // Not an expression, try to get from context
            return fetchVari(expression, context);
        }
    }

    private static Object fetchVari(String path, Map<String, Object> context) {
        String[] parts = path.split("\\.");
        Object value = context.get(parts[0]);

        for (int i = 1; i < parts.length && value != null; i++) {
            String part = parts[i];
            if (value instanceof Map.Entry<?, ?>) {
                if ("key".equals(part)) {
                    value = ((Map.Entry<?, ?>) value).getKey();
                } else
                if ("value".equals(part)) {
                    value = ((Map.Entry<?, ?>) value).getValue();
                } else {
                    value = null;
                }
            } else if (value instanceof Map<?, ?>) {
                value = ((Map<?, ?>) value).get(part);
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
                    } else if (element.matches("\\d+")) {
                        value = Integer.parseInt(element);
                    } else if (element.matches("\\d+\\.\\d+")) {
                        value = Double.parseDouble(element);
                    } else {
                        value = fetchVari(element, context);
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
                        } else if (textVal.matches("\\d+")) {
                            value = Integer.parseInt(textVal);
                        } else if (textVal.matches("\\d+\\.\\d+")) {
                            value = Double.parseDouble(textVal);
                        } else {
                            value = fetchVari(textVal, context);
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
            return parseLogical(context);
        }

        private void skipWhitespace() {
            while (pos < expression.length() && Character.isWhitespace(expression.charAt(pos))) {
                pos++;
            }
        }

        private Object parseLogical(Map<String, Object> context) {
            Object left = parseCompare(context);
            while (pos < expression.length()) {
                skipWhitespace();
                if (pos >= expression.length()) break;
                String op = parseOperator();
                if (!op.equals("&&") && !op.equals("||")) {
                    // Not a logical operator, put the operator back
                    pos -= op.length();
                    break;
                }
                Object right = parseCompare(context);
                left = evaluateLogicalOp(left, op, right);
            }
            return left;
        }

        private Object parseCompare(Map<String, Object> context) {
            Object left = parseAddSubs(context);
            while (pos < expression.length()) {
                skipWhitespace();
                if (pos >= expression.length()) break;
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
                if (op != '*' && op != '/') break;
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
                return !toBoolean(value);
            } else if (c == '(') {
                pos++;
                Object result = parseLogical(context);
                skipWhitespace();
                if (pos >= expression.length() || expression.charAt(pos) != ')') {
                    throw new IllegalArgumentException("Missing closing parenthesis");
                }
                pos++;
                return result;
            } else if (c == '-') {
                pos++;
                Object value = parsePrimary(context);
                if (value instanceof Number) {
                    return -((Number) value).doubleValue();
                } else if (value instanceof String) {
                    try {
                        return -Double.parseDouble((String) value);
                    } catch (NumberFormatException e) {
                        throw new IllegalArgumentException("Cannot apply negative operator to non-number: " + value);
                    }
                } else {
                    throw new IllegalArgumentException("Cannot apply negative operator to: " + value);
                }
            } else if (Character.isDigit(c) || c == '.') {
                return parseNumber();
            } else if (c == '"') {
                return parseString();
            } else if (c == '[' || c == '{') {
                return parseJson(context);
            } else {
                return parseVari(context);
            }
        }

        private String parseOperator() {
            StringBuilder op = new StringBuilder();
            // Check for 'in' operator first
            if (pos + 2 < expression.length() &&
                expression.charAt(pos    ) == 'i' &&
                expression.charAt(pos + 1) == 'n' &&
                Character.isWhitespace(expression.charAt(pos + 2))) {
                pos += 2;
                return "in";
            }

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
            while (pos < expression.length() && (Character.isDigit(expression.charAt(pos)) || expression.charAt(pos) == '.')) {
                pos++;
            }
            String numStr = expression.substring(start, pos);
            try {
                if (numStr.contains(".")) {
                    return Double.parseDouble(numStr);
                } else {
                    return Integer.parseInt(numStr);
                }
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Invalid number: " + numStr);
            }
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

        private Object parseVari(Map<String, Object> context) {
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
            return fetchVari(name, context);
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

        private Object evaluateLogicalOp(Object left, String op, Object right) {
            boolean leftBool = toBoolean(left);
            boolean rightBool = toBoolean(right);

            switch (op) {
                case "&&":
                    return leftBool && rightBool;
                case "||":
                    return leftBool || rightBool;
                default:
                    throw new IllegalArgumentException("Unknown logical operator: " + op);
            }
        }

        private Object evaluateNumericOp(Object left, char op, Object right) {
            double leftVal = toNumber(left);
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
                case ">":
                    return compare(left, right) >  0;
                case "<":
                    return compare(left, right) <  0;
                case ">=":
                    return compare(left, right) >= 0;
                case "<=":
                    return compare(left, right) <= 0;
                case "in":
                    return  within(left, right);
                default:
                    throw new IllegalArgumentException("Unknown comparison operator: " + op);
            }
        }

        private boolean isCompareOp(String op) {
            return op.equals("==") || op.equals("!=") ||
                   op.equals(">=") || op.equals("<=") ||
                   op.equals(">" ) || op.equals("<" ) ||
                   op.equals("in");
        }

        private int compare(Object left, Object right) {
            if (left == null || right == null) {
                if (left == null && right == null) {
                    return 0;
                } else if (left == null) {
                    return -1;
                } else {
                    return 1;
                }
            }

            if (left instanceof Number && right instanceof Number) {
                double leftValue = ((Number) left).doubleValue();
                double rightValue = ((Number) right).doubleValue();
                return Double.compare(leftValue, rightValue);
            }

            throw new IllegalArgumentException("Cannot compare non-numeric values: " + left.getClass() + " and " + right.getClass());
        }

        private boolean equals(Object left, Object right) {
            if (left == null && right == null) {
                return true;
            }
            if (left == null || right == null) {
                return false;
            }
            return left.equals(right);
        }

        private boolean within(Object left, Object right) {
            if (right == null) {
                return false;
            }
            if (right instanceof java.util.Collection) {
                return ((java.util.Collection<?>) right).contains(left);
            } else if (right instanceof java.util.Map) {
                return ((java.util.Map<?, ?>) right ).containsKey(left);
            }
            return false;
        }

        private boolean toBoolean(Object obj) {
            if (obj == null) {
                return false;
            } else if (obj instanceof Boolean) {
                return (Boolean) obj;
            } else if (obj instanceof String) {
                return ((String) obj).isEmpty() == false;
            } else if (obj instanceof Number) {
                return ((Number) obj).doubleValue() != 0;
            } else {
                return true;
            }
        }

        private double toNumber(Object obj) {
            if (obj instanceof Number) {
                return ((Number) obj).doubleValue();
            } else
            if (obj instanceof String) {
                try {
                    return Double.parseDouble((String) obj);
                } catch (NumberFormatException e) {
                    throw new IllegalArgumentException("Cannot convert to number: " + obj);
                }
            } else {
                throw new IllegalArgumentException("Cannot convert to number: " + obj);
            }
        }
    }

    /**
     * 默认模板函数
     * 默认: default(变量1, 变量2...) 跳过空值、空串和数字 0
     * 缩进: indent(文本, 缩进几格) 或 indent(文本) 缩进两格
     * 连词: concat(列表, 连词符号) 或 indent(列表) 逗号连接
     * 格式: format(格式, 变量1, 变量2...)
     * 日期格式: date_format(时间, 格式) 时间变量可以是 Date/Instant 或时间戳(毫秒)
     * 文本清理: strip(文本, 模式) 模式取值:
     *   trim 清除首尾
     *   tags 清除 html 标签
     *   cros 清除 html 脚本
     *   html 等同 cros,tags,trim
     *   gaps 清除空行
     *   ends 清除换行
     *   unis 统一换行
     *   可逗号分隔多个模式, 省略模式等同 trim
     */
    public static final Map<String, Function<Object[], Object>> FUNCTIONS = new HashMap();
    static {
        FUNCTIONS.put("default", args -> {
            for (Object arg : args) {
                if (arg != null) {
                    if (arg instanceof String str ) {
                        if (! "".equals(str)) {
                            return arg;
                        }
                    } else
                    if (arg instanceof Number num ) {
                        if (0 != num.doubleValue()) {
                            return arg;
                        }
                    }
                }
            }
            return null;
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
    }

}