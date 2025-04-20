// 简单的JavaScript示例

// 变量声明
var greeting = "Hello, World!";
var count = 10;
var isActive = true;

// 函数声明
function sayHello(name) {
    return greeting + " My name is " + name + ".";
}

// 计算函数
function calculateSum(numbers) {
    var sum = 0;
    for (var i = 0; i < numbers.length; i++) {
        sum += numbers[i];
    }
    return sum;
}

// 对象定义
var person = {
    firstName: "John",
    lastName: "Doe",
    age: 30,
    getFullName: function() {
        return this.firstName + " " + this.lastName;
    }
};

// 调用函数
var message = sayHello("Alice");
console.log(message);

// 数组操作
var numbers = [1, 2, 3, 4, 5];
var total = calculateSum(numbers);
console.log("Sum: " + total);

// 条件语句
if (count > 5) {
    console.log("Count is greater than 5");
} else {
    console.log("Count is not greater than 5");
}

// 循环
for (var i = 0; i < 3; i++) {
    console.log("Iteration " + i);
}

// 使用对象
console.log(person.getFullName() + " is " + person.age + " years old."); 