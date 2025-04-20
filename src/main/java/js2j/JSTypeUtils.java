package js2j;

import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.IntegerLiteralExpr;
import com.github.javaparser.ast.expr.DoubleLiteralExpr;
import com.github.javaparser.ast.expr.StringLiteralExpr;
import com.github.javaparser.ast.expr.BooleanLiteralExpr;
import com.github.javaparser.ast.expr.NullLiteralExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.MethodCallExpr;

/**
 * JavaScript类型工具类
 * 负责处理JavaScript与Java之间的类型转换
 */
public class JSTypeUtils {

    /**
     * 根据变量名推断类型
     */
    public static String determineTypeFromName(String varName) {
        if (varName == null || varName.isEmpty()) {
            return "Object";
        }
        
        // 字符串相关变量名
        if (varName.toLowerCase().contains("message") || 
            varName.equals("greeting") ||
            varName.startsWith("name") ||
            varName.endsWith("Name") ||
            varName.toLowerCase().contains("text") ||
            varName.toLowerCase().contains("str")) {
            return "String";
        }
        
        // 数值相关变量名
        if (varName.equals("count") || 
            varName.equals("sum") || 
            varName.equals("total") ||
            varName.startsWith("num") ||
            varName.endsWith("Count") ||
            varName.endsWith("Index") ||
            varName.startsWith("i") && varName.length() < 3) {
            return "int";
        }
        
        // 布尔相关变量名
        if (varName.startsWith("is") ||
            varName.startsWith("has") ||
            varName.startsWith("can") ||
            varName.startsWith("should") ||
            varName.equals("enabled") ||
            varName.equals("visible")) {
            return "boolean";
        }
        
        // 数组或集合相关变量名
        if (varName.endsWith("List") ||
            varName.endsWith("Array") ||
            varName.endsWith("Collection") ||
            varName.endsWith("Set") ||
            varName.equals("items") ||
            varName.equals("elements")) {
            return "List<Object>";
        }
        
        // 默认类型
        return "Object";
    }
    
    /**
     * 根据表达式确定Java类型
     */
    public static String determineType(Expression expr) {
        if (expr instanceof StringLiteralExpr) {
            return "String";
        } else if (expr instanceof IntegerLiteralExpr) {
            return "int";
        } else if (expr instanceof DoubleLiteralExpr) {
            return "double";
        } else if (expr instanceof BooleanLiteralExpr) {
            return "boolean";
        } else if (expr instanceof NullLiteralExpr) {
            return "Object";
        } else if (expr instanceof NameExpr) {
            String name = ((NameExpr) expr).getNameAsString();
            if (name.equals("Arrays")) {
                return "Arrays";
            } else if (name.endsWith("List")) {
                return "List<Object>";
            }
        } else if (expr instanceof MethodCallExpr) {
            String methodName = ((MethodCallExpr) expr).getNameAsString();
            if (methodName.equals("asList")) {
                return "List<Integer>";
            } else if (methodName.equals("println") || methodName.equals("print")) {
                return "void";
            } else if (methodName.startsWith("get") && !methodName.equals("get")) {
                String propertyName = methodName.substring(3);
                if (propertyName.equals("Name") || propertyName.equals("Text") || 
                    propertyName.equals("Message") || propertyName.equals("Description")) {
                    return "String";
                } else if (propertyName.equals("Count") || propertyName.equals("Size") || 
                           propertyName.equals("Index") || propertyName.equals("Length")) {
                    return "int";
                } else if (propertyName.equals("Enabled") || propertyName.equals("Visible") || 
                           propertyName.equals("Valid")) {
                    return "boolean";
                }
            }
            return "Object"; // 函数调用默认返回Object
        }
        return "Object"; // 默认类型
    }
    
    /**
     * 根据方法名推断返回类型
     */
    public static String determineReturnTypeFromMethodName(String methodName, String functionBody) {
        if (methodName == null || methodName.isEmpty()) {
            return "Object";
        }
        
        // 1. 检查方法名
        if (methodName.startsWith("get") && !methodName.equals("get")) {
            String propertyName = methodName.substring(3);
            if (propertyName.equals("Name") || propertyName.equals("Text") || 
                propertyName.equals("Message") || propertyName.equals("Description")) {
                return "String";
            } else if (propertyName.equals("Count") || propertyName.equals("Size") || 
                       propertyName.equals("Index") || propertyName.equals("Length")) {
                return "int";
            } else if (propertyName.equals("Enabled") || propertyName.equals("Visible") || 
                       propertyName.equals("Valid")) {
                return "boolean";
            }
        }
        
        if (methodName.startsWith("is") || methodName.startsWith("has") || 
            methodName.startsWith("can") || methodName.startsWith("should")) {
            return "boolean";
        }
        
        if (methodName.startsWith("calculate") || 
            methodName.startsWith("compute") || 
            methodName.startsWith("count") ||
            methodName.equals("calculateSum") || 
            methodName.equals("add") ||
            methodName.equals("subtract")) {
            return "int";
        }
        
        if (methodName.startsWith("say") || 
            methodName.startsWith("format") || 
            methodName.startsWith("convert") ||
            methodName.contains("Name") || 
            methodName.contains("Text") || 
            methodName.contains("Message")) {
            return "String";
        }
        
        // 2. 分析函数体
        if (functionBody != null) {
            if (functionBody.contains("return") && functionBody.contains("\"")) {
                return "String";
            } else if (functionBody.contains("return") && functionBody.contains("true") || 
                       functionBody.contains("return") && functionBody.contains("false")) {
                return "boolean";
            } else if (functionBody.contains("return") && 
                     (functionBody.contains("sum") || functionBody.contains("count") || 
                      functionBody.contains("index") || functionBody.contains(" + ") || 
                      functionBody.contains(" - "))) {
                return "int";
            }
        }
        
        return "Object";
    }
    
    /**
     * 根据参数名推断参数类型
     */
    public static String determineParameterType(String paramName) {
        if (paramName == null || paramName.isEmpty()) {
            return "Object";
        }
        
        // 使用变量名推断逻辑
        return determineTypeFromName(paramName);
    }
    
    /**
     * 判断是否是简单类型（Java原生类型或String）
     */
    public static boolean isSimpleType(String typeName) {
        return "int".equals(typeName) || 
               "long".equals(typeName) || 
               "float".equals(typeName) || 
               "double".equals(typeName) || 
               "boolean".equals(typeName) || 
               "char".equals(typeName) || 
               "byte".equals(typeName) || 
               "short".equals(typeName) || 
               "String".equals(typeName);
    }
} 