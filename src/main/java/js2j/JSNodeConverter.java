package js2j;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.StringLiteralExpr;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.ExpressionStmt;
import com.github.javaparser.ast.stmt.ReturnStmt;
import com.github.javaparser.ast.stmt.Statement;
import org.mozilla.javascript.ast.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Collections;

/**
 * JavaScript节点转换器
 * 负责将JavaScript AST节点转换为等效的Java代码
 */
public class JSNodeConverter {

    /**
     * 将JavaScript函数体转换为Java方法体
     * 
     * @param functionNode JavaScript函数节点
     * @return Java方法体
     */
    public BlockStmt convertFunctionBody(FunctionNode functionNode) {
        BlockStmt body = new BlockStmt();
        
        // 简化函数体处理
        // 直接返回一个包含返回null的方法体
        body.addStatement(new ReturnStmt().setExpression(null));
        
        // 添加一条注释，表示这里简化了函数体转换
        System.out.println("警告：简化了函数体转换，可能需要手动编写函数体实现。");
        
        return body;
    }
    
    /**
     * 将JavaScript语句转换为Java语句
     * 
     * @param node JavaScript语句节点
     * @return Java语句
     */
    public Statement convertStatement(AstNode node) {
        if (node instanceof ReturnStatement) {
            return convertReturnStatement((ReturnStatement) node);
        } else if (node instanceof ExpressionStatement) {
            return convertExpressionStatement((ExpressionStatement) node);
        } else if (node instanceof IfStatement) {
            return convertIfStatement((IfStatement) node);
        } else if (node instanceof WhileLoop) {
            return convertWhileLoop((WhileLoop) node);
        } else if (node instanceof ForLoop) {
            return convertForLoop((ForLoop) node);
        } else if (node instanceof VariableDeclaration) {
            return convertVariableDeclaration((VariableDeclaration) node);
        }
        
        // 对于不支持的语句类型，返回注释
        MethodCallExpr methodCall = new MethodCallExpr();
        methodCall.setScope(new NameExpr("System.out"));
        methodCall.setName(new com.github.javaparser.ast.expr.SimpleName("println"));
        methodCall.addArgument(new StringLiteralExpr("不支持的JavaScript语句类型: " + node.getClass().getSimpleName()));
        return new ExpressionStmt(methodCall);
    }
    
    /**
     * 转换JavaScript return语句
     */
    private ReturnStmt convertReturnStatement(ReturnStatement node) {
        AstNode returnValue = node.getReturnValue();
        
        if (returnValue == null) {
            // 没有返回值的return语句
            return new ReturnStmt();
        } else {
            // 有返回值的return语句
            Expression javaExpr = convertExpression(returnValue);
            return new ReturnStmt(javaExpr);
        }
    }
    
    /**
     * 转换JavaScript表达式语句
     */
    private ExpressionStmt convertExpressionStatement(ExpressionStatement node) {
        Expression javaExpr = convertExpression(node.getExpression());
        return new ExpressionStmt(javaExpr);
    }
    
    /**
     * 转换JavaScript if语句
     */
    private Statement convertIfStatement(IfStatement node) {
        // 简化实现，返回一个注释
        MethodCallExpr methodCall = new MethodCallExpr();
        methodCall.setScope(new NameExpr("System.out"));
        methodCall.setName(new com.github.javaparser.ast.expr.SimpleName("println"));
        methodCall.addArgument(new StringLiteralExpr("JavaScript if语句转换未实现"));
        return new ExpressionStmt(methodCall);
    }
    
    /**
     * 转换JavaScript while循环
     */
    private Statement convertWhileLoop(WhileLoop node) {
        // 简化实现，返回一个注释
        MethodCallExpr methodCall = new MethodCallExpr();
        methodCall.setScope(new NameExpr("System.out"));
        methodCall.setName(new com.github.javaparser.ast.expr.SimpleName("println"));
        methodCall.addArgument(new StringLiteralExpr("JavaScript while循环转换未实现"));
        return new ExpressionStmt(methodCall);
    }
    
    /**
     * 转换JavaScript for循环
     */
    private Statement convertForLoop(ForLoop node) {
        // 简化实现，返回一个注释
        MethodCallExpr methodCall = new MethodCallExpr();
        methodCall.setScope(new NameExpr("System.out"));
        methodCall.setName(new com.github.javaparser.ast.expr.SimpleName("println"));
        methodCall.addArgument(new StringLiteralExpr("JavaScript for循环转换未实现"));
        return new ExpressionStmt(methodCall);
    }
    
    /**
     * 转换JavaScript变量声明
     */
    private Statement convertVariableDeclaration(VariableDeclaration node) {
        // 简化实现，返回一个注释
        MethodCallExpr methodCall = new MethodCallExpr();
        methodCall.setScope(new NameExpr("System.out"));
        methodCall.setName(new com.github.javaparser.ast.expr.SimpleName("println"));
        methodCall.addArgument(new StringLiteralExpr("JavaScript变量声明转换未实现"));
        return new ExpressionStmt(methodCall);
    }
    
    /**
     * 将JavaScript表达式转换为Java表达式
     * 
     * @param node JavaScript表达式节点
     * @return Java表达式
     */
    public Expression convertExpression(AstNode node) {
        if (node instanceof StringLiteral) {
            // 字符串字面量
            return new StringLiteralExpr(((StringLiteral) node).getValue());
        } else if (node instanceof NumberLiteral) {
            // 数字字面量
            try {
                String value = ((NumberLiteral) node).getValue();
                return StaticJavaParser.parseExpression(value);
            } catch (Exception e) {
                return new StringLiteralExpr("无效的数字: " + ((NumberLiteral) node).getValue());
            }
        } else if (node instanceof Name) {
            // 变量引用
            return new NameExpr(((Name) node).getIdentifier());
        } else if (node instanceof FunctionCall) {
            // 函数调用
            return convertFunctionCall((FunctionCall) node);
        } else if (node instanceof PropertyGet) {
            // 属性访问
            return convertPropertyGet((PropertyGet) node);
        } else if (node instanceof InfixExpression) {
            // 中缀表达式（如 a + b, x == y 等）
            return convertInfixExpression((InfixExpression) node);
        }
        
        // 对于不支持的表达式类型，返回一个字符串字面量
        return new StringLiteralExpr("不支持的JavaScript表达式类型: " + node.getClass().getSimpleName());
    }
    
    /**
     * 转换JavaScript函数调用
     */
    private Expression convertFunctionCall(FunctionCall node) {
        AstNode target = node.getTarget();
        List<AstNode> arguments = node.getArguments();
        
        // 目标可能是简单名称或属性访问
        Expression methodExpr;
        if (target instanceof Name) {
            // 简单函数调用，如 foo()
            String methodName = ((Name) target).getIdentifier();
            methodExpr = new NameExpr(methodName);
        } else if (target instanceof PropertyGet) {
            // 方法调用，如 obj.method()
            methodExpr = convertPropertyGet((PropertyGet) target);
        } else {
            // 不支持的函数调用目标类型
            return new StringLiteralExpr("不支持的函数调用目标类型: " + target.getClass().getSimpleName());
        }
        
        // 转换参数
        List<Expression> javaArgs = new ArrayList<>();
        for (AstNode arg : arguments) {
            javaArgs.add(convertExpression(arg));
        }
        
        // 创建方法调用表达式
        if (methodExpr instanceof NameExpr) {
            // 简单方法调用
            MethodCallExpr call = new MethodCallExpr();
            call.setName(new com.github.javaparser.ast.expr.SimpleName(((NameExpr) methodExpr).getNameAsString()));
            for (Expression arg : javaArgs) {
                call.addArgument(arg);
            }
            return call;
        } else {
            // 对象方法调用
            MethodCallExpr call = new MethodCallExpr();
            call.setScope(methodExpr);
            call.setName(new com.github.javaparser.ast.expr.SimpleName("invoke"));
            for (Expression arg : javaArgs) {
                call.addArgument(arg);
            }
            return call;
        }
    }
    
    /**
     * 转换JavaScript属性访问
     */
    private Expression convertPropertyGet(PropertyGet node) {
        // 简化实现，返回一个字符串表达式
        return new StringLiteralExpr("属性访问: " + node.toSource());
    }
    
    /**
     * 转换JavaScript中缀表达式
     */
    private Expression convertInfixExpression(InfixExpression node) {
        // 简化实现，返回一个字符串表达式
        return new StringLiteralExpr("中缀表达式: " + node.toSource());
    }
} 