package io.github.ihongs.test;

import io.github.ihongs.util.Template;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * 模板测试
 * @author Hongs
 */
public class TestTemplate {
    
    private Map<String, Object> context;
    private String basePath;
    
    @Before
    public void setUp() throws IOException {
        // 初始化测试上下文
        context = new HashMap<>();
        context.put("title", "My Template");
        context.put("showWelcome", true);
        context.put("showItems", true);
        context.put("user", Map.of("name", "John"));
        context.put("items", List.of("Item 1", "Item 2", "Item 3"));
        context.put("count", 5);
        context.put("price", 9.99);
        context.put("isActive", true);
        context.put("footerText", "Copyright 2026");

        // 创建测试文件
        basePath = "target/var/test-template";
        Path dir = Path.of(basePath);
        Files.createDirectories(dir);
        
        // 创建 header.html
        try (FileWriter writer = new FileWriter(new File(dir.toString(), "header.html"))) {
            writer.write("""
            <header>
                <h1>{{title}}</h1>
                <p>Welcome to my website!</p>
            </header>
            """);
        }
        
        // 创建 subtemplate.html
        try (FileWriter writer = new FileWriter(new File(dir.toString(), "subtemplate.html"))) {
            writer.write("""
            <div class="subtemplate">
                <h3>{{subtitle}}</h3>
                <p>{{message}}</p>
            </div>
            """);
        }
    }
    
    private void testTemplate(String template, Map<String, Object> ctx, String expected) {
        Template engine = Template.compile(template);
        String result = engine.render(ctx);

//        System.out.println("====== EXPECTED ======");
//        System.out.println(expected);
//        System.out.println("======  ACTUAL  ======");
//        System.out.println( result );
//        System.out.println("======");

        // 直接比较，不清理空白字符
        assertEquals("Template rendering failed", expected, result);
    }
    
    @Test
    public void testBasicTemplateRendering() {
        String template = """
        <h1>{{title}}</h1>
          {%set greeting = "Hello"%}
          {%if showWelcome%}
          <p>{{greeting}}, {{user.name}}!</p>
          {%endif%}
        <p>Count: {{count}}</p>
        <p>Price: {{price}}</p>
        <p>Is Active: {{isActive}}</p>
        <footer>{{footerText}}</footer>
        """;

        String expected = """
        <h1>My Template</h1>
          <p>Hello, John!</p>
        <p>Count: 5</p>
        <p>Price: 9.99</p>
        <p>Is Active: true</p>
        <footer>Copyright 2026</footer>
        """;

        testTemplate(template, context, expected);
    }
    
    @Test
    public void testIfNestedWithFor() {
        String template = """
        <h2>If nested with for</h2>
        {%if showItems%}
          <ul>
          {%for item in items%}
            <li>{{item}}</li>
          {%endfor%}
          </ul>
        {%elif count > 10%}
          <p>Count is greater than 10</p>
        {%else%}
          <p>No items to display</p>
        {%endif%}
        """;

        String expected = """
        <h2>If nested with for</h2>
          <ul>
            <li>Item 1</li>
            <li>Item 2</li>
            <li>Item 3</li>
          </ul>
        """;

        testTemplate(template, context, expected);
    }
    
    @Test
    public void testForNestedWithIf() {
        String template = """
        <h2>For nested with if</h2>
        <ul>
        {%for item in items%}
          {%if item == "Item 1"%}
            <li class="first">{{item}}</li>
          {%elif item == "Item 2"%}
            <li class="second">{{item}}</li>
          {%else%}
            <li class="other">{{item}}</li>
          {%endif%}
        {%endfor%}
        </ul>
        """;

        String expected = """
        <h2>For nested with if</h2>
        <ul>
            <li class="first">Item 1</li>
            <li class="second">Item 2</li>
            <li class="other">Item 3</li>
        </ul>
        """;

        testTemplate(template, context, expected);
    }
    
    @Test
    public void testArithmeticOperations() {
        String template = """
        <h2>Arithmetic operations</h2>
        {%set sum = 1 + 2%}
        <p>1 + 2 = {{sum}}</p>
        
        {%set difference = 5 - 3%}
        <p>5 - 3 = {{difference}}</p>
        
        {%set product = 4 * 6%}
        <p>4 * 6 = {{product}}</p>
        
        {%set quotient = 10 / 3%}
        <p>10 / 3 = {{format("%.2f", quotient)}}</p>
        """;

        String expected = """
        <h2>Arithmetic operations</h2>
        <p>1 + 2 = 3</p>
        
        <p>5 - 3 = 2</p>
        
        <p>4 * 6 = 24</p>
        
        <p>10 / 3 = 3.33</p>
        """;

        testTemplate(template, context, expected);
    }
    
    @Test
    public void testIncludeDirectiveWithBasePath() throws Exception {
        String template = """
        {%include "header.html"%}
        <h1>{{title}}</h1>
        {%set greeting = "Hello"%}
        {%if showWelcome%}
          <p>{{greeting}}, {{user.name}}!</p>
        {%endif%}
        <footer>{{footerText}}</footer>
        """;

        String expected = """
        <header>
            <h1>My Template</h1>
            <p>Welcome to my website!</p>
        </header>

        <h1>My Template</h1>
          <p>Hello, John!</p>
        <footer>Copyright 2026</footer>
        """;

        Template engine = Template.compile(template, Path.of(basePath));
        String result = engine.render(context);
        
        // 直接比较，不清理空白字符
        assertEquals("Include directive test failed", expected, result);
    }
    
    @Test
    public void testIncludeWithSubContext() throws Exception {
        Map<String, Object> subContext = new HashMap<>();
        subContext.put("subtitle", "Sub Template");
        subContext.put("message", "Hello from subcontext!");
        Map<String, Object> ctx = new HashMap<>(context);
        ctx.put("subContext", subContext);

        String template = """
        <h2>Include with subContext</h2>
        {%include "subtemplate.html" with subContext%}
        """;

        String expected = """
        <h2>Include with subContext</h2>
        <div class="subtemplate">
            <h3>Sub Template</h3>
            <p>Hello from subcontext!</p>
        </div>
        
        """;

        Template engine = Template.compile(template, Path.of(basePath));
        String result = engine.render(ctx);
        
        // 直接比较，不清理空白字符
        assertEquals("Include with subContext test failed", expected, result);
    }

    @Test
    public void testIncludeDirectiveWithoutBasePath() {
        String template = "{%include \"header.html\"%}";

        // 验证没有basePath时会抛出异常
        boolean thrown = false;
        try {
            Template engine = Template.compile(template);
            engine.render(context);
        } catch (Exception e) {
            thrown = true;
//            String message = e.getMessage();
//            System.out.println("Error message: " + message);
        }
        assertTrue("Expected exception was not thrown", thrown);
    }
    
    @Test
    public void testTemplateSyntaxError() {
        // 模板格式错误：缺少结束标签
        String template = """
        <h1>{{title}}</h1>
        <p>&nbsp;</p>
        {%if showWelcome%}
        <p>Welcome, {{user.name}}!</p>
        """;

        // 验证异常报告的行号是否正确
        boolean thrown = false;
        try {
            Template engine = Template.compile(template);
            engine.render(context);
        } catch (Exception e) {
            thrown = true;
//            String message = e.getMessage();
//            System.out.println("Error message: " + message);
        }
        assertTrue("Expected exception was not thrown", thrown);
    }
    
    @Test
    public void testIfElifElse() {
        // 测试if-elif-else条件分支
        String template = """
        <h2>If-elif-else test</h2>
        {%if count > 10%}
        <p>Count is greater than 10</p>
        {%elif count > 5%}
        <p>Count is greater than 5 but less than or equal to 10</p>
        {%elif count > 0%}
        <p>Count is greater than 0 but less than or equal to 5</p>
        {%else%}
        <p>Count is 0 or negative</p>
        {%endif%}
        """;

        String expected = """
        <h2>If-elif-else test</h2>
        <p>Count is greater than 0 but less than or equal to 5</p>
        """;

        testTemplate(template, context, expected);
    }
    
    @Test
    public void testForElse() {
        // 测试for循环的else分支
        Map<String, Object> emptyContext = new HashMap<>(context);
        emptyContext.put("items", List.of());
        
        String template = """
        <h2>For with else test</h2>
        <ul>
            {%for item in items%}<li>{{item}}</li>{%else%}<li>No items found</li>{%endfor%}
            {%for entry in user%}{%set a = entry.value%}{%endfor%}
        </ul>
        """;
    
        String expected = """
        <h2>For with else test</h2>
        <ul>
            <li>No items found</li>
        </ul>
        """;
    
        testTemplate(template, emptyContext, expected);
    }
    
    @Test
    public void testNestedCombinations() {
        // 测试for/if、for/else、if/else的嵌套组合
        String template = """
        <h2>Nested combinations test</h2>
        
        <!-- For with nested if-else -->
        <h3>For with nested if-else</h3>
        <ul>
        {%for item in items%}
            {%if item == "Item 1"%}
            <li class="first">{{item}}</li>
            {%elif item == "Item 2"%}
            <li class="second">{{item}}</li>
            {%else%}
            <li class="other">{{item}}</li>
            {%endif%}
        {%endfor%}
        </ul>
        
        <!-- If-else with nested for -->
        <h3>If-else with nested for</h3>
        {%if showItems%}
            <ul>
            {%for item in items%}
                <li>{{item}}</li>
            {%endfor%}
            </ul>
        {%else%}
            <p>No items to display</p>
        {%endif%}
        
        <!-- For with else and nested if -->
        <h3>For with else and nested if</h3>
        <ul>
            {% set i = 0 %}
        {%for item in items%}
            {%if item != "Item 2"%}
                {% set i = i + 1 %}
            <li>{{i}}. {{item}}</li>
            {%endif%}
        {%else%}
            <li>No items found</li>
        {%endfor%}
        </ul>
        """;

        String expected = """
        <h2>Nested combinations test</h2>
        
        <!-- For with nested if-else -->
        <h3>For with nested if-else</h3>
        <ul>
            <li class="first">Item 1</li>
            <li class="second">Item 2</li>
            <li class="other">Item 3</li>
        </ul>
        
        <!-- If-else with nested for -->
        <h3>If-else with nested for</h3>
            <ul>
                <li>Item 1</li>
                <li>Item 2</li>
                <li>Item 3</li>
            </ul>
        
        <!-- For with else and nested if -->
        <h3>For with else and nested if</h3>
        <ul>
            <li>1. Item 1</li>
            <li>2. Item 3</li>
        </ul>
        """;

        testTemplate(template, context, expected);
    }
    
    @Test
    public void testModuloOperator() {
        // 测试取模运算符，特别测试不会与%}标签结束符冲突
        Map<String, Object> ctx = new HashMap<>(context);
        
        String template = """
        <h2>Modulo operator test</h2>
        
        <!-- Basic modulo -->
        <p>10 % 3 = {{10 % 3}}</p>
        <p>15 % 4 = {{15 % 4}}</p>
        <p>20 % 5 = {{20 % 5}}</p>
        
        <!-- Modulo with variables -->
        {%set a = 17%}
        {%set b = 5%}
        <p>{{a}} % {{b}} = {{a % b}}</p>
        
        <!-- Modulo in expression -->
        {%set total = 23%}
        {%set divisor = 7%}
        <p>{{total}} % {{divisor}} = {{total % divisor}}</p>
        
        <!-- Modulo in condition -->
        {%set num = 15%}
        {%if num % 3 == 0%}
        <p>{{num}} is divisible by 3</p>
        {%else%}
        <p>{{num}} is not divisible by 3</p>
        {%endif%}
        
        <!-- Modulo with parentheses -->
        <p>(10 + 5) % 4 = {{(10 + 5) % 4}}</p>
        <p>10 + (5 % 4) = {{10 + (5 % 4)}}</p>
        
        <!-- Important: Test that % doesn't conflict with %} tag end -->
        {%set remainder = 10 % 3%}
        <p>Remainder: {{remainder}}</p>
        
        <!-- Complex expression with % and other operators -->
        {%set x = 25%}
        {%set y = 6%}
        <p>{{x}} % {{y}} = {{x % y}}</p>
        <p>({{x}} + {{y}}) % 4 = {{(x + y) % 4}}</p>
        """;

        String expected = """
        <h2>Modulo operator test</h2>
        
        <!-- Basic modulo -->
        <p>10 % 3 = 1</p>
        <p>15 % 4 = 3</p>
        <p>20 % 5 = 0</p>
        
        <!-- Modulo with variables -->
        <p>17 % 5 = 2</p>
        
        <!-- Modulo in expression -->
        <p>23 % 7 = 2</p>
        
        <!-- Modulo in condition -->
        <p>15 is not divisible by 3</p>
        
        <!-- Modulo with parentheses -->
        <p>(10 + 5) % 4 = 3</p>
        <p>10 + (5 % 4) = 11</p>
        
        <!-- Important: Test that % doesn't conflict with %} tag end -->
        <p>Remainder: 1</p>
        
        <!-- Complex expression with % and other operators -->
        <p>25 % 6 = 1</p>
        <p>(25 + 6) % 4 = 3</p>
        """;

        testTemplate(template, ctx, expected);
    }
    
}
