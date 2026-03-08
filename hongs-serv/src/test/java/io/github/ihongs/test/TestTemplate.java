package io.github.ihongs.test;

import io.github.ihongs.util.Template;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.Test;

/**
 * 模板测试
 * @author Hongs
 */
public class TestTemplate {
    
    @Test
    public void test() throws IOException {
        Map<String, Object> context = new HashMap<>();
        context.put("title", "My Template");
        context.put("showWelcome", true);
        context.put("showItems", true);
        context.put("user", Map.of("name", "John"));
        context.put("items", List.of("Item 1", "Item 2", "Item 3"));
        context.put("count", 5);
        context.put("price", 9.99);
        context.put("isActive", true);
        context.put("footerText", "Copyright 2026");

        // include files
        String basePath = "target/var/test-template";
        Path dir = Path.of(basePath);
        Files.createDirectories(dir);
        FileWriter writer;
        writer = new FileWriter(new File(dir.toString(), "header.html"));
        writer.write("""
        <header>
            <h1>{{title}}</h1>
            <p>Welcome to my website!</p>
        </header>
        """);
        writer.close();
        writer = new FileWriter(new File(dir.toString(), "subtemplate.html"));
        writer.write("""
        <div class="subtemplate">
            <h3>{{subtitle}}</h3>
            <p>{{message}}</p>
        </div>
        """);
        writer.close();
        writer = new FileWriter(new File(dir.toString(), "template with test.html"));
        writer.write("""
        <div class="template with test">
            <h3>{{subtitle}}</h3>
            <p>{{message}}</p>
        </div>
        """);
        writer.close();
        
        // Test 1: Basic template rendering
        String template1 = """
        <h1>{{title}}</h1>
          {%set greeting = "Hello"%}
          {%if showWelcome%}
          <p>{{greeting}}, {{user.name}}!</p>
          {%endif%}
        <p>Count: {{count}}</p>
        <p>Price: {{price}}</p>
        <p>Is Active: {{isActive}}</p>
        <h2>Items:</h2>
        <ul>
        {%for item in items%}
          <li>{{item}}</li>
        {%endfor%}
        </ul>
        <h2>Map Items:</h2>
        <ul>
        {%for entry in user%}
          {%set key = entry.key%}
          {%set value = entry.value%}
          <li>{{key}}: {{value}}</li>
        {%endfor%}
        </ul>
        <footer>{{footerText}}</footer>
        """;

        System.out.println("Test 1: Basic template rendering");
        Template engine1 = Template.compile(template1);
        String result1 = engine1.render(context);
        System.out.println(result1);

        // Test 2: Include directive with basePath
        String template2 = """
        {%include "header.html"%}
        <h1>{{title}}</h1>
        {%set greeting = "Hello"%}
        {%if showWelcome%}
          <p>{{greeting}}, {{user.name}}!</p>
        {%endif%}
        <footer>{{footerText}}</footer>
        """;

        try {
            System.out.println("\nTest 2: Include directive with basePath");
            Template engine2 = Template.compile(template2, Path.of(basePath));
            String result2 = engine2.render(context);
            System.out.println(result2);
        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
        }

        // Test 3: Include directive without basePath (should throw exception)
        String template3 = "{%include \"header.html\"%}";

        try {
            System.out.println("\nTest 3: Include directive without basePath");
            Template engine3 = Template.compile(template3);
            String result3 = engine3.render(context);
            System.out.println(result3);
        } catch (Exception e) {
            System.out.println("Expected error: " + e.getMessage());
        }

        // Test 4: If nested with for
        String template4 = """
        <h2>If nested with for</h2>
        {%if showItems%}
          <ul>
          {%for item in items%}
            <li>{{item}}</li>
          {%endfor%}
          </ul>
        {%endif%}
        """;

        System.out.println("\nTest 4: If nested with for");
        Template engine4 = Template.compile(template4);
        String result4 = engine4.render(context);
        System.out.println(result4);

        // Test 5: For nested with if
        String template5 = """
        <h2>For nested with if</h2>
        <ul>
        {%for item in items%}
          {%if item != "Item 2"%}
            <li>{{item}}</li>
          {%endif%}
        {%endfor%}
        </ul>
        """;

        System.out.println("\nTest 5: For nested with if");
        Template engine5 = Template.compile(template5);
        String result5 = engine5.render(context);
        System.out.println(result5);

        // Test 6: For nested with for
        List<List<String>> nestedItems = List.of(
            List.of("A1", "A2", "A3"),
            List.of("B1", "B2", "B3"),
            List.of("C1", "C2", "C3")
        );
        context.put("nestedItems", nestedItems);

        String template6 = """
        <h2>For nested with for</h2>
        <ul>
        {%for group in nestedItems%}
          <li>Group:
            <ul>
            {%for item in group%}
              <li>{{item}}</li>
            {%endfor%}
            </ul>
          </li>
        {%endfor%}
        </ul>
        """;

        System.out.println("\nTest 6: For nested with for");
        Template engine6 = Template.compile(template6);
        String result6 = engine6.render(context);
        System.out.println(result6);

        // Test 7: Include with subContext
        Map<String, Object> subContext = new HashMap<>();
        subContext.put("subtitle", "Sub Template");
        subContext.put("message", "Hello from subcontext!");
        context.put("subContext", subContext);

        String template7 = """
        <h2>Include with subContext</h2>
        {%include "subtemplate.html" with subContext%}
        """;

        try {
            System.out.println("\nTest 7: Include with subContext");
            Template engine7 = Template.compile(template7, Path.of(basePath));
            String result7 = engine7.render(context);
            System.out.println(result7);
        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
        }

        // Test 8: Include with template name containing "with"
        String template8 = """
        <h2>Include with template name containing 'with'</h2>
        {%include "template with test.html" with subContext%}
        """;

        try {
            System.out.println("\nTest 8: Include with template name containing 'with'");
            Template engine8 = Template.compile(template8, Path.of(basePath));
            String result8 = engine8.render(context);
            System.out.println(result8);
        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
        }

        // Test 9: Complex if expressions
        String template9 = """
        <h2>Complex if expressions</h2>
        {%if (count > 3 && price < 10) || (showWelcome && user.name == "John")%}
            <p>Complex condition is true!</p>
        {%endif%}

        {%if !(count < 3) && (price >= 5 && price <= 15)%}
            <p>Negated condition is true!</p>
        {%endif%}
        """;

        try {
            System.out.println("\nTest 9: Complex if expressions");
            Template engine9 = Template.compile(template9);
            String result9 = engine9.render(context);
            System.out.println(result9);
        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
        }

        // Test 10: Multi-level variable access
        String template10 = """
        <h2>Multi-level variable access</h2>
        <p>User name: {{user.name}}</p>
        <p>User age: {{user.age}}</p>

        {%if user.age >= 18%}
            <p>User is adult</p>
        {%else%}
            <p>User is minor</p>
        {%endif%}
        """;

        try {
            System.out.println("\nTest 10: Multi-level variable access");
            Template engine10 = Template.compile(template10);
            String result10 = engine10.render(context);
            System.out.println(result10);
        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
        }

        // Test 11: JSON literals in for loop
        String template11 = """
        <h2>JSON literals in for loop</h2>
        <h3>Array literal:</h3>
        <ul>
        {%for item in ["a", "b", "c"]%}
            <li>{{item}}</li>
        {%endfor%}
        </ul>

        <h3>Object literal:</h3>
        <ul>
        {%for entry in {"name": "John", "age": 30, "city": "New York"}%}
            <li>{{entry.key}}: {{entry.value}}</li>
        {%endfor%}
        </ul>
        """;

        try {
            System.out.println("\nTest 11: JSON literals in for loop");
            Template engine11 = Template.compile(template11);
            String result11 = engine11.render(context);
            System.out.println(result11);
        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
        }

        // Test 12: JSON literals in set directive
        String template12 = """
        <h2>JSON literals in set directive</h2>
        {%set fruits = ["apple", "banana", "orange"]%}
        <ul>
        {%for fruit in fruits%}
            <li>{{fruit}}</li>
        {%endfor%}
        </ul>

        {%set person = {"name": "Alice", "age": 25}%}
        <p>Name: {{person.name}}</p>
        <p>Age: {{person.age}}</p>
        """;

        try {
            System.out.println("\nTest 12: JSON literals in set directive");
            Template engine12 = Template.compile(template12);
            String result12 = engine12.render(context);
            System.out.println(result12);
        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
        }

        // Test 13: in operator
        String template13 = """
        <h2>in operator</h2>
        {%set fruits = ["apple", "banana", "orange"]%}
        {%set person = {"name": "Alice", "age": 25}%}

        {%if "apple" in fruits%}
            <p>Apple is in fruits</p>
        {%endif%}

        {%if "grape" in fruits%}
            <p>Grape is in fruits</p>
        {%else%}
            <p>Grape is not in fruits</p>
        {%endif%}

        {%if "name" in person%}
            <p>name is in person</p>
        {%endif%}

        {%if "address" in person%}
            <p>address is in person</p>
        {%else%}
            <p>address is not in person</p>
        {%endif%}

        // Test in with JSON literals
        {%if "a" in ["a", "b", "c"]%}
            <p>a is in ["a", "b", "c"]</p>
        {%endif%}

        {%if "d" in ["a", "b", "c"]%}
            <p>d is in ["a", "b", "c"]</p>
        {%else%}
            <p>d is not in ["a", "b", "c"]</p>
        {%endif%}

        {%if "name" in {"name": "Alice", "age": 25}%}
            <p>name is in {"name": "Alice", "age": 25}</p>
        {%endif%}

        // Test negated in operator
        {%if !("a" in ["a", "b", "c"])%}
            <p>!("a" in ["a", "b", "c"]) is true</p>
        {%else%}
            <p>!("a" in ["a", "b", "c"]) is false</p>
        {%endif%}

        {%if !("d" in ["a", "b", "c"])%}
            <p>!("d" in ["a", "b", "c"]) is true</p>
        {%else%}
            <p>!("d" in ["a", "b", "c"]) is false</p>
        {%endif%}

        // Test elif directive
        {%set score = 85%}
        {%if score >= 90%}
            <p>Excellent</p>
        {%elif score >= 80%}
            <p>Good</p>
        {%elif score >= 70%}
            <p>Average</p>
        {%else%}
            <p>Needs improvement</p>
        {%endif%}
        """;

        try {
            System.out.println("\nTest 13: in operator");
            Template engine13 = Template.compile(template13);
            String result13 = engine13.render(context);
            System.out.println(result13);
        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
        }

        // Test 14: for else directive
        String template14 = """
        <h2>for else directive</h2>

        // Test with non-empty collection
        <h3>Non-empty collection:</h3>
        {%for item in ["a", "b", "c"]%}
            <p>Item: {{item}}</p>
        {%else%}
            <p>No items found</p>
        {%endfor%}

        // Test with empty collection
        <h3>Empty collection:</h3>
        {%for item in []%}
            <p>Item: {{item}}</p>
        {%else%}
            <p>No items found</p>
        {%endfor%}

        // Test with null collection
        <h3>Null collection:</h3>
        {%for item in nullCollection%}
            <p>Item: {{item}}</p>
        {%else%}
            <p>No items found</p>
        {%endfor%}

        // Test nested for else
        <h3>Nested for else:</h3>
        {%for group in groups%}
            <p>Group: {{group.name}}</p>
            {%for item in group.items%}
                <p>  Item: {{item}}</p>
            {%else%}
                <p>  No items in this group</p>
            {%endfor%}
        {%else%}
            <p>No groups found</p>
        {%endfor%}
        """;

        try {
            System.out.println("\nTest 14: for else directive");
            Template engine14 = Template.compile(template14);
            // Add test data for nested for else
            Map<String, Object> group1 = new HashMap<>();
            group1.put("name", "Group 1");
            List<String> items1 = new ArrayList<>();
            items1.add("A1");
            items1.add("A2");
            items1.add("A3");
            group1.put("items", items1);

            Map<String, Object> group2 = new HashMap<>();
            group2.put("name", "Group 2");
            group2.put("items", new ArrayList<>()); // Empty items

            Map<String, Object> testContext = new HashMap<>(context);
            List<Map<String, Object>> groups = new ArrayList<>();
            groups.add(group1);
            groups.add(group2);
            testContext.put("groups", groups);
            testContext.put("nullCollection", null);

            String result14 = engine14.render(testContext);
            System.out.println(result14);
        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
        }

        // Test 15: multiple spaces in directives
        String template15 = """
        <h2>Multiple spaces in directives</h2>

        // Test for with multiple spaces
        <h3>For with multiple spaces:</h3>
        {%for  item  in  ["a", "b", "c"]  %}
            <p>Item: {{item}}</p>
        {%endfor%}

        // Test if with multiple spaces
        <h3>If with multiple spaces:</h3>
        {%if  count  >  3  %}
            <p>Count is greater than 3</p>
        {%else%}
            <p>Count is not greater than 3</p>
        {%endif%}

        // Test set with multiple spaces
        <h3>Set with multiple spaces:</h3>
        {%set  testVar  =  "test value"  %}
        <p>Test variable: {{testVar}}</p>

        // Test in operator with multiple spaces
        <h3>In operator with multiple spaces:</h3>
        {%if  "apple"  in  fruits  %}
            <p>Apple is in fruits</p>
        {%endif%}
        """;

        try {
            System.out.println("\nTest 15: multiple spaces in directives");
            Template engine15 = Template.compile(template15);
            String result15 = engine15.render(context);
            System.out.println(result15);
        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
        }

        // Test 16: Arithmetic operations in set directive
        String template16 = """
        <h2>Arithmetic operations in set directive</h2>

        // Test basic arithmetic operations
        {%set sum = 1 + 2%}
        <p>1 + 2 = {{sum}}</p>

        {%set difference = 5 - 3%}
        <p>5 - 3 = {{difference}}</p>

        {%set product = 4 * 6%}
        <p>4 * 6 = {{product}}</p>

        {%set quotient = 10 / 2%}
        <p>10 / 2 = {{quotient}}</p>

        // Test operations with variables
        {%set countPlusPrice = count + price%}
        <p>count + price = {{countPlusPrice}}</p>

        // Test operations with parentheses
        {%set complex = (1 + 2) * (3 + 4)%}
        <p>(1 + 2) * (3 + 4) = {{complex}}</p>

        // Test mixed operations
        {%set mixed = (10 + 5) * 2 - 8 / 4%}
        <p>(10 + 5) * 2 - 8 / 4 = {{mixed}}</p>
        """;

        try {
            System.out.println("\nTest 16: Arithmetic operations in set directive");
            Template engine16 = Template.compile(template16);
            String result16 = engine16.render(context);
            System.out.println(result16);
        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
        }
        
        
        // Test 17: Function calls
        String template17 = """
        <h2>Function calls</h2>

        // Test abs function
        <p>abs(-5) = {{abs(-5)}}</p>

        // Test concat function
        <p>concat("Hello ", "World") = {{concat("Hello ", "World")}}</p>

        // Test max function
        <p>max(1, 5, 3, 9, 2) = {{max(1, 5, 3, 9, 2)}}</p>

        // Test function with variables
        <p>abs(count - 10) = {{abs(count - 10)}}</p>

        // Test nested function calls
        <p>abs(max(-5, -3, -9)) = {{abs(max(-5, -3, -9))}}</p>

        // Test multiple functions in set directive
        {%set absCount = abs(count - 20)%}
        <p>set absCount = abs(count - 20) = {{absCount}}</p>

        {%set fullName = concat("Mr. ", user.name)%}
        <p>set fullName = concat("Mr. ", user.name) = {{fullName}}</p>

        {%set maxValue = max(count, price, 10)%}
        <p>set maxValue = max(count, price, 10) = {{maxValue}}</p>

        // Test multiple functions in if directive
        {%if abs(count - 10) > 3 && max(count, price) < 20%}
            <p>abs(count - 10) > 3 && max(count, price) < 20 is true</p>
        {%else%}
            <p>abs(count - 10) > 3 && max(count, price) < 20 is false</p>
        {%endif%}

        // Test multiple functions in for directive
        {%for i in [1, 2, 3, 4, 5]%}
            <p>abs(i - 3) = {{abs(i - 3)}}, max(i, 3) = {{max(i, 3)}}</p>
        {%endfor%}

        // Test complex nested functions
        {%set complex = abs(max(count - 10, price - 5))%}
        <p>set complex = abs(max(count - 10, price - 5)) = {{complex}}</p>

        // Test inner function call
        {%set num = 199.543%}
        {{format("Price: %.2f", num)}}
                            
        // Test inline if
        - {%if true%}abc{%endif%}
        - syz
        """;

        System.out.println("\nTest 17: Function calls");
        Template engine17 = Template.compile(template17);
        
        // Register test functions
        engine17.regist("abs", params -> {
            if (params.length != 1) throw new IllegalArgumentException("abs requires exactly one argument");
            return Math.abs(Double.parseDouble(params[0].toString()));
        });
        engine17.regist("concat", params -> {
            StringBuilder sb = new StringBuilder();
            for (Object arg : params) {
                sb.append(arg);
            }
            return sb.toString();
        });
        engine17.regist("max", params -> {
            if (params.length < 1) throw new IllegalArgumentException("max requires at least one argument");
            double max = Double.parseDouble(params[0].toString());
            for (int i = 1; i < params.length; i++) {
                double val = Double.parseDouble(params[i].toString());
                if (val > max) max = val;
            }
            return max;
        });
        
        String result17 = engine17.render(context);
        System.out.println(result17);
    }

}
