package js2j;

import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.expr.*;
import org.mozilla.javascript.Token;
import org.mozilla.javascript.ast.*;

import java.util.ArrayList;
import java.util.List;

/**
 * JavaScript对象处理器
 * 负责处理JavaScript对象字面量和数组字面量
 */
public class JSObjectProcessor {
    
    /**
     * 处理对象字面量，转换为Java代码字符串
     */
    public String processObjectLiteral(ObjectLiteral node) {
        StringBuilder result = new StringBuilder();
        result.append("new HashMap<String, Object>() {{");
        
        List<ObjectProperty> properties = node.getElements();
        for (int i = 0; i < properties.size(); i++) {
            ObjectProperty prop = properties.get(i);
            
            String key = getPropertyKey(prop);
            String value = getPropertyValue(prop);
            
            // 忽略函数属性，它们在后面单独处理
            if (!value.startsWith("(Function<")) {
                result.append("\n    put(\"").append(key).append("\", ").append(value).append(");");
            }
        }
        
        // 单独处理函数属性
        for (ObjectProperty prop : properties) {
            String key = getPropertyKey(prop);
            String value = getPropertyValue(prop);
            
            if (value.startsWith("(Function<")) {
                result.append("\n    put(\"").append(key).append("\", ").append(value).append(");");
            }
        }
        
        result.append("\n}}");
        return result.toString();
    }
    
    /**
     * 处理对象字面量，转换为Java表达式
     */
    public Expression createObjectLiteralExpression(ObjectLiteral node) {
        // 简化实现，使用NameExpr创建对象字面量表达式
        return new NameExpr(processObjectLiteral(node));
    }
    
    /**
     * 处理数组字面量，转换为Java代码字符串
     */
    public String processArrayLiteral(ArrayLiteral node) {
        List<AstNode> elements = node.getElements();
        
        if (elements.isEmpty()) {
            return "new ArrayList<>()";
        }
        
        StringBuilder result = new StringBuilder();
        result.append("Arrays.asList(");
        
        for (int i = 0; i < elements.size(); i++) {
            AstNode element = elements.get(i);
            result.append(getElementValue(element));
            
            if (i < elements.size() - 1) {
                result.append(", ");
            }
        }
        
        result.append(")");
        return result.toString();
    }
    
    /**
     * 获取属性键
     */
    private String getPropertyKey(ObjectProperty prop) {
        if (prop.getLeft() instanceof org.mozilla.javascript.ast.Name) {
            return ((org.mozilla.javascript.ast.Name) prop.getLeft()).getIdentifier();
        } else if (prop.getLeft() instanceof StringLiteral) {
            return ((StringLiteral) prop.getLeft()).getValue();
        } else {
            return "key_" + prop.getLeft().hashCode();
        }
    }
    
    /**
     * 获取属性值字符串
     */
    private String getPropertyValue(ObjectProperty prop) {
        if (prop.getRight() instanceof StringLiteral) {
            return "\"" + ((StringLiteral) prop.getRight()).getValue() + "\"";
        } else if (prop.getRight() instanceof NumberLiteral) {
            return ((NumberLiteral) prop.getRight()).getValue();
        } else if (prop.getRight() instanceof KeywordLiteral) {
            KeywordLiteral keyword = (KeywordLiteral) prop.getRight();
            if (keyword.getType() == Token.TRUE) {
                return "true";
            } else if (keyword.getType() == Token.FALSE) {
                return "false";
            } else if (keyword.getType() == Token.NULL) {
                return "null";
            }
        } else if (prop.getRight() instanceof ObjectLiteral) {
            return processObjectLiteral((ObjectLiteral) prop.getRight());
        } else if (prop.getRight() instanceof ArrayLiteral) {
            return processArrayLiteral((ArrayLiteral) prop.getRight());
        } else if (prop.getRight() instanceof FunctionNode) {
            // 处理函数
            return processFunctionProperty((FunctionNode) prop.getRight());
        }
        
        return "/* 不支持的对象属性值类型 */";
    }
    
    /**
     * 处理函数属性
     */
    private String processFunctionProperty(FunctionNode functionNode) {
        StringBuilder result = new StringBuilder();
        result.append("(Function<Object[], Object>) (args) -> {\n");
        result.append("    //  函数体\n");
        result.append("    // 使用args[0]访问map自身，相当于this\n");
        result.append("    Map<String, Object> self = (Map<String, Object>)args[0];\n");
        
        // 获取函数体
        String functionBody = functionNode.getBody().toSource();
        
        // 解析函数体以获取return语句
        if (functionBody.contains("return")) {
            // 分析return语句
            int returnIndex = functionBody.indexOf("return ");
            if (returnIndex != -1) {
                int startIndex = returnIndex + "return ".length();
                int endIndex = functionBody.indexOf(";", startIndex);
                
                if (endIndex > startIndex) {
                    String returnValue = functionBody.substring(startIndex, endIndex).trim();
                    
                    // 处理字符串拼接和this引用
                    returnValue = processFunctionReturnValue(returnValue);
                    
                    // 添加转换后的返回语句
                    result.append("    return ").append(returnValue).append(";\n");
                } else {
                    // 没有找到分号，使用通用返回值
                    result.append("    return null;\n");
                }
            } else {
                // 未能正确解析return语句
                result.append("    return null;\n");
            }
        } else {
            // 没有return语句
            result.append("    return null;\n");
        }
        
        result.append("}");
        return result.toString();
    }
    
    /**
     * 处理函数返回值
     * 特别处理字符串拼接和this引用
     */
    private String processFunctionReturnValue(String returnValue) {
        // 如果返回值包含this.xxx的引用，则替换为map的get方法调用
        if (returnValue.contains("this.")) {
            // 例如: this.firstName + " " + this.lastName
            // 转换为: self.get("firstName") + " " + self.get("lastName")
            
            StringBuilder processedValue = new StringBuilder();
            int currentPos = 0;
            
            // 查找所有this.xxx模式并替换
            java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("this\\.(\\w+)");
            java.util.regex.Matcher matcher = pattern.matcher(returnValue);
            
            while (matcher.find()) {
                // 添加this引用之前的部分
                processedValue.append(returnValue.substring(currentPos, matcher.start()));
                
                // 替换this.xxx为self.get("xxx")
                String property = matcher.group(1);
                processedValue.append("self.get(\"").append(property).append("\")");
                
                // 更新当前位置
                currentPos = matcher.end();
            }
            
            // 添加剩余部分
            if (currentPos < returnValue.length()) {
                processedValue.append(returnValue.substring(currentPos));
            }
            
            // 清理返回值中可能出现的多余括号和分号
            String processed = processedValue.toString();
            // 移除末尾多余的右括号和分号
            processed = processed.replaceAll("\\);$", "");
            
            return processed;
        }
        
        // 清理返回值中可能出现的多余括号和分号
        return returnValue.replaceAll("\\);$", "");
    }
    
    /**
     * 获取数组元素值
     */
    private String getElementValue(AstNode element) {
        if (element instanceof StringLiteral) {
            return "\"" + ((StringLiteral) element).getValue() + "\"";
        } else if (element instanceof NumberLiteral) {
            return ((NumberLiteral) element).getValue();
        } else if (element instanceof KeywordLiteral) {
            KeywordLiteral keyword = (KeywordLiteral) element;
            if (keyword.getType() == Token.TRUE) {
                return "true";
            } else if (keyword.getType() == Token.FALSE) {
                return "false";
            } else if (keyword.getType() == Token.NULL) {
                return "null";
            }
        } else if (element instanceof ObjectLiteral) {
            return processObjectLiteral((ObjectLiteral) element);
        } else if (element instanceof ArrayLiteral) {
            return processArrayLiteral((ArrayLiteral) element);
        }
        
        return "/* 不支持的数组元素类型 */";
    }
    
    /**
     * 创建方法调用表达式
     */
    public MethodCallExpr createMethodCallExpression(String target, String methodName, List<Expression> args) {
        MethodCallExpr methodCall = new MethodCallExpr();
        methodCall.setScope(new NameExpr(target));
        methodCall.setName(methodName);
        
        for (Expression arg : args) {
            methodCall.addArgument(arg);
        }
        
        return methodCall;
    }
} 