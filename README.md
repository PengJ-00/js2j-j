# JavaScript到Java转换器 (JS2J)

这个工具可以将JavaScript代码转换为等效的Java代码。它通过解析JavaScript代码的抽象语法树(AST)，然后生成对应的Java代码实现转换。

## 特性

- 支持基础JavaScript语法转换为Java
- 支持ES6类语法转换为Java类
- 支持对象字面量转换为Java对象
- 支持控制流语句（if-else、for循环、while循环、switch）
- 支持函数和方法的转换及参数处理
- 递归处理目录中的所有JavaScript文件
- 保持原始目录结构
- 提供可扩展的转换框架
- 预处理和后处理流程以提高转换质量

## 系统要求

- Java 8或更高版本
- Maven 3.6.0或更高版本

## 依赖库

- **Rhino** (1.7.14): Mozilla的JavaScript引擎，用于解析JavaScript代码生成AST
- **Nashorn** (15.3): OpenJDK的JavaScript引擎，提供额外的JavaScript解析支持
- **JavaParser** (3.15.0): 用于生成和操作Java代码

## 编译

使用Maven编译项目：

```bash
mvn clean package
```

编译成功后，将在`target`目录下生成可执行JAR文件。

## 使用方法

```bash
java -jar target/js2j-j-1.0-SNAPSHOT.jar <JavaScript文件/目录> [输出目录]
```

### 参数说明

- `<JavaScript文件/目录>`: 要转换的JavaScript文件路径或包含JavaScript文件的目录
- `[输出目录]`: （可选）生成的Java文件的输出目录，默认为"java-output"

### 示例

转换单个文件：
```bash
java -jar target/js2j-j-1.0-SNAPSHOT.jar example.js
```

转换整个目录：
```bash
java -jar target/js2j-j-1.0-SNAPSHOT.jar src/js out/java
```

## 支持的转换

当前版本支持以下JavaScript结构转换为Java：

- **基本数据类型**：字符串、数字、布尔值、null等
- **变量声明**：var/let/const变量声明和初始化
- **函数声明**：函数定义、参数、返回值
- **ES6类语法**：类声明、构造函数、方法、属性
- **对象字面量**：对象创建及属性初始化
- **数组操作**：数组创建、访问、修改
- **条件语句**：if-else、三元运算符
- **循环语句**：for、while、do-while
- **逻辑和算术表达式**：+、-、*、/、%、&&、||等
- **属性访问**：对象属性和方法调用
- **函数调用**：普通函数和方法调用

## 转换限制

当前版本存在以下限制：

- 不支持高级JavaScript特性，如箭头函数、解构赋值、async/await等
- 不支持闭包等JavaScript特有特性的完全等效转换
- 不支持JavaScript内置对象的完全映射（部分支持console、Math等）
- 不支持DOM操作和浏览器API的直接转换
- 不支持JavaScript动态类型的完全等效模拟
- 生成的Java代码可能需要手动调整以适应特定需求
- 不保证生成的代码能够直接编译和运行

## 架构设计

项目采用模块化设计，主要包含以下组件：

- **Main**: 程序入口，处理命令行参数
- **JSParser**: 使用Rhino解析JavaScript代码生成AST
- **JavaScriptToJavaConverter**: 主转换器，协调整个转换过程
- **各专用处理器**:
  - JSFunctionProcessor: 处理函数定义和调用
  - JSClassProcessor: 处理类定义和方法
  - JSObjectProcessor: 处理对象字面量
  - JSExpressionProcessor: 处理表达式
  - JSStatementProcessor: 处理语句
  - JSControlFlowProcessor: 处理控制流结构
  - JSSourcePreprocessor: 源代码预处理
  - JSTypeUtils: 类型转换工具

## 扩展

该项目设计为可扩展的。如需添加对更多JavaScript特性的支持，可以扩展以下类：

- `JSNodeConverter`: 实现更多节点类型的转换
- `JavaScriptToJavaConverter`: 添加更多转换策略
- 创建专用处理器处理特定JavaScript特性

## 示例转换

以下简单示例展示JS2J的转换效果：

JavaScript:
```javascript
function calculateSum(numbers) {
    var sum = 0;
    for (var i = 0; i < numbers.length; i++) {
        sum += numbers[i];
    }
    return sum;
}
```

转换后的Java:
```java
public static int calculateSum(int[] numbers) {
    int sum = 0;
    for (int i = 0; i < numbers.length; i++) {
        sum += numbers[i];
    }
    return sum;
}
```
