import java.util.Arrays;
import java.util.List;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

String greeting = "Hello, World!";
int count = 10;
boolean isActive = true;
public static String sayHello(String name) {
    return greeting + " My name is " + name + ".";
}

public static int calculateSum(String numbers) {
    int sum = 0
    for (int i = 0; i < numbers.size(); i++) {
        {
            sum += numbers.get(i);
        }
    }
    return sum;
}

Map<String, Object> person = new HashMap<String, Object>() {{
    HashMap<String, Object> temp = this;
    temp.put("firstName", "John");
    temp.put("lastName", "Doe");
    temp.put("age", 30);
    temp.put("getFullName", /* ERROR: Unsupported expression type: FunctionExpression */);
}};
Object message = sayHello("Alice");
System.out.println(message);
List<Integer> numbers = Arrays.asList(1, 2, 3, 4, 5);
Object total = calculateSum(numbers);
System.out.println("Sum: " + total);
if (count > 5) {
    {
        System.out.println("Count is greater than 5");
    }
    } else {
        {
            System.out.println("Count is not greater than 5");
        }
    }
    for (int i = 0; i < 3; i++) {
        {
            System.out.println("Iteration " + i);
        }
    }
    System.out.println(person.getFullName() + " is " + person.age + " years old.");