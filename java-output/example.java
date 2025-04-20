String greeting = "Hello, World!";

int count = 10;

boolean isActive = true;

String sayHello(String name) {
    return greeting + " My name is " + name + ".";
}

int calculateSum(int[] numbers) {
    int sum = 0;
    {
        for (int i = 0;i < numbers.length; i++) {
            sum += numbers[i];
        }
    }
    return sum;
}

Map<String, Object> person = new HashMap<String, Object>() {put("firstName", "John");
put("lastName", "Doe");
put("age", 30);
put("getFullName", (Function<Object[], Object>) (args) -> {
    //  函数体
    // 使用args[0]访问map自身，相当于this
    Map<String, Object> self = (Map<String, Object>)args[0];
    return self.get("firstName") + " " + self.get("lastName");
}};

String message = sayHello("Alice");

System.out.println(message);

int[] numbers = {1, 2, 3, 4, 5};

int total = calculateSum(numbers);

System.out.println("Sum: " + total);

if (count > 5) {
    System.out.println("Count is greater than 5");
} else {
    System.out.println("Count is not greater than 5");
}

for (int i = 0;i < 3; i++) {
    System.out.println("Iteration " + i);
}

System.out.println(person.getFullName() + " is " + person.age + " years old.");
