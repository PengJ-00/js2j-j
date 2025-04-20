package js2j;

import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.expr.*;
import com.github.javaparser.ast.stmt.ExpressionStmt;
import org.mozilla.javascript.Token;
import org.mozilla.javascript.ast.*;

import java.util.ArrayList;
import java.util.List;

/**
 * JavaScript表达式处理器
 * 负责将JavaScript表达式转换为Java表达式
 */
public class JSExpressionProcessor {
    
    private final JSObjectProcessor objectProcessor;
    
    /**
     * 构造函数
     */
    public JSExpressionProcessor() {
        this.objectProcessor = new JSObjectProcessor();
    }
    
    /**
     * 构造函数（使用依赖注入）
     */
    public JSExpressionProcessor(JSObjectProcessor objectProcessor) {
        this.objectProcessor = objectProcessor;
    }
    
    /**
     * 处理表达式语句，输出Java代码字符串
     */
    public String processExpressionStatement(org.mozilla.javascript.ast.ExpressionStatement node) {
        Expression expr = createExpressionFromJSNode(node.getExpression());
        return expr.toString() + ";\n";
    }
    
    /**
     * 处理表达式语句，输出Java代码字符串
     * 这个重载方法用于处理JavaParser的ExpressionStmt
     */
    public String processExpressionStatement(ExpressionStmt node) {
        // 由于JavaParser的Expression不能转为AstNode，我们只返回表达式的字符串形式
        return node.getExpression().toString() + ";\n";
    }
    
    /**
     * 从JavaScript节点创建Java表达式
     */
    public Expression createExpressionFromJSNode(AstNode node) {
        if (node == null) {
            return null;
        }
        
        switch (node.getType()) {
            case Token.STRING:
            return new StringLiteralExpr(((StringLiteral) node).getValue());
                
            case Token.NUMBER:
            String numValue = ((NumberLiteral) node).getValue();
            if (numValue.contains(".")) {
                return new DoubleLiteralExpr(numValue);
            } else {
                return new IntegerLiteralExpr(numValue);
            }
                
            case Token.TRUE:
                return new BooleanLiteralExpr(true);
                
            case Token.FALSE:
                return new BooleanLiteralExpr(false);
                
            case Token.NULL:
            case Token.VOID:
                return new NullLiteralExpr();
                
            case Token.NAME:
                if (node instanceof org.mozilla.javascript.ast.Name) {
                    String name = ((org.mozilla.javascript.ast.Name) node).getIdentifier();
                    return new NameExpr(name);
                }
                break;
                
            case Token.CALL:
                FunctionCall callNode = (FunctionCall) node;
                AstNode callTargetNode = callNode.getTarget();
                
                // 检查是否是console.log调用
                if (callTargetNode instanceof PropertyGet) {
                    PropertyGet propGet = (PropertyGet) callTargetNode;
                    AstNode targetObj = propGet.getTarget();
                    AstNode method = propGet.getProperty();
                    
                    if (targetObj instanceof org.mozilla.javascript.ast.Name && 
                        method instanceof org.mozilla.javascript.ast.Name) {
                        String objName = ((org.mozilla.javascript.ast.Name) targetObj).getIdentifier();
                        String methodName = ((org.mozilla.javascript.ast.Name) method).getIdentifier();
                        
                        if (objName.equals("console") && methodName.equals("log")) {
                            // 转换为System.out.println
                            MethodCallExpr printlnCall = new MethodCallExpr();
                            printlnCall.setScope(new NameExpr("System.out"));
                            printlnCall.setName("println");
                            
                            // 添加参数
                            for (AstNode arg : callNode.getArguments()) {
                                printlnCall.addArgument(createExpressionFromJSNode(arg));
                            }
                            
                            return printlnCall;
                        }
                    }
                }
                
                // 处理其他函数调用
                MethodCallExpr methodCall = new MethodCallExpr();
                
                // 处理不同类型的函数调用
                if (callTargetNode instanceof org.mozilla.javascript.ast.Name) {
                    // 简单函数调用，如: foo()
                    org.mozilla.javascript.ast.Name name = (org.mozilla.javascript.ast.Name) callTargetNode;
                    methodCall.setName(name.getIdentifier());
                } else if (callTargetNode instanceof PropertyGet) {
                    // 对象方法调用，如: obj.method()
                    PropertyGet propGet = (PropertyGet) callTargetNode;
                    Expression scopeExpr = createExpressionFromJSNode(propGet.getTarget());
                    methodCall.setScope(scopeExpr);
                    
                    AstNode propertyNode = propGet.getProperty();
                    if (propertyNode instanceof org.mozilla.javascript.ast.Name) {
                        String methodName = ((org.mozilla.javascript.ast.Name) propertyNode).getIdentifier();
                        methodCall.setName(methodName);
                    } else {
                        methodCall.setName(propertyNode.toString());
                    }
                } else {
                    // 其他类型的函数调用
                    Expression targetExpr = createExpressionFromJSNode(callTargetNode);
                    methodCall.setScope(targetExpr);
                    methodCall.setName("call"); // 对于复杂的函数表达式，使用通用名称
                }
                
                // 添加参数
                for (AstNode arg : callNode.getArguments()) {
                    methodCall.addArgument(createExpressionFromJSNode(arg));
                }
                
                return methodCall;
                
            case Token.OBJECTLIT:
                ObjectLiteral objLit = (ObjectLiteral) node;
                return new NameExpr(objectProcessor.processObjectLiteral(objLit));
                
            case Token.ARRAYLIT:
                ArrayLiteral arrayLit = (ArrayLiteral) node;
                return new NameExpr(objectProcessor.processArrayLiteral(arrayLit));
                
            case Token.ADD:
            case Token.SUB:
            case Token.MUL:
            case Token.DIV:
            case Token.MOD:
            case Token.BITOR:
            case Token.BITAND:
            case Token.BITXOR:
                InfixExpression infix = (InfixExpression) node;
                BinaryExpr.Operator operator = getBinaryOperator(infix.getOperator());
                
                Expression leftExpr = createExpressionFromJSNode(infix.getLeft());
                Expression rightExpr = createExpressionFromJSNode(infix.getRight());
                
                return new BinaryExpr(leftExpr, rightExpr, operator);
                
            case Token.EQ:
            case Token.NE:
            case Token.LT:
            case Token.LE:
            case Token.GT:
            case Token.GE:
                return processComparisonExpression((InfixExpression) node);
                
            case Token.AND:
            case Token.OR:
                return processLogicalExpression((InfixExpression) node);
                
            case Token.ASSIGN:
            case Token.ASSIGN_ADD:
            case Token.ASSIGN_SUB:
            case Token.ASSIGN_MUL:
            case Token.ASSIGN_DIV:
                return processAssignmentExpression((Assignment) node);
                
            case Token.INC:
            case Token.DEC:
                org.mozilla.javascript.ast.UnaryExpression unary = (org.mozilla.javascript.ast.UnaryExpression) node;
                Expression expr = createExpressionFromJSNode(unary.getOperand());
                
                // 判断是前缀还是后缀操作符
                int position = unary.getPosition();
                int operandPosition = unary.getOperand().getPosition();
                
                // 如果操作符位置在操作数之后，则为后缀
                boolean isPostfix = position > operandPosition;
                
                if (isPostfix) {
                    if (unary.getType() == Token.INC) {
                        return new UnaryExpr(expr, UnaryExpr.Operator.POSTFIX_INCREMENT);
                    } else {
                        return new UnaryExpr(expr, UnaryExpr.Operator.POSTFIX_DECREMENT);
                    }
                } else {
                    if (unary.getType() == Token.INC) {
                        return new UnaryExpr(expr, UnaryExpr.Operator.PREFIX_INCREMENT);
                    } else {
                        return new UnaryExpr(expr, UnaryExpr.Operator.PREFIX_DECREMENT);
                    }
                }
                
            case Token.NEG:
            case Token.POS:
            case Token.NOT:
            case Token.BITNOT:
                return processUnaryExpression((org.mozilla.javascript.ast.UnaryExpression) node);
                
            case Token.GETPROP:
                // 处理属性访问表达式，如obj.property
                if (node instanceof PropertyGet) {
                    PropertyGet propGet = (PropertyGet) node;
                    AstNode targetNode = propGet.getTarget();
                    AstNode propertyNode = propGet.getProperty();
                    
                    // 创建字段访问表达式
                    Expression scopeExpr = createExpressionFromJSNode(targetNode);
                    String propName = "";
                    
                    if (propertyNode instanceof org.mozilla.javascript.ast.Name) {
                        propName = ((org.mozilla.javascript.ast.Name) propertyNode).getIdentifier();
                    } else {
                        propName = propertyNode.toString();
                    }
                    
                    return new FieldAccessExpr(scopeExpr, propName);
                }
                break;
                
            case Token.GETELEM:
                // 处理数组元素访问，如array[index]
                if (node instanceof ElementGet) {
                    ElementGet elemGet = (ElementGet) node;
                    Expression arrayExpr = createExpressionFromJSNode(elemGet.getTarget());
                    Expression indexExpr = createExpressionFromJSNode(elemGet.getElement());
                    
                    return new ArrayAccessExpr(arrayExpr, indexExpr);
                }
                break;
        }
        
        // 不支持的表达式类型
        return new StringLiteralExpr("/* 不支持的表达式类型: " + node.getClass().getSimpleName() + " */");
    }
    
    /**
     * 获取二元操作符
     */
    private BinaryExpr.Operator getBinaryOperator(int jsOperator) {
        switch (jsOperator) {
            case Token.ADD: return BinaryExpr.Operator.PLUS;
            case Token.SUB: return BinaryExpr.Operator.MINUS;
            case Token.MUL: return BinaryExpr.Operator.MULTIPLY;
            case Token.DIV: return BinaryExpr.Operator.DIVIDE;
            case Token.MOD: return BinaryExpr.Operator.REMAINDER;
            case Token.EQ: return BinaryExpr.Operator.EQUALS;
            case Token.NE: return BinaryExpr.Operator.NOT_EQUALS;
            case Token.LT: return BinaryExpr.Operator.LESS;
            case Token.LE: return BinaryExpr.Operator.LESS_EQUALS;
            case Token.GT: return BinaryExpr.Operator.GREATER;
            case Token.GE: return BinaryExpr.Operator.GREATER_EQUALS;
            case Token.AND: return BinaryExpr.Operator.AND;
            case Token.OR: return BinaryExpr.Operator.OR;
            case Token.BITOR: return BinaryExpr.Operator.BINARY_OR;
            case Token.BITAND: return BinaryExpr.Operator.BINARY_AND;
            case Token.BITXOR: return BinaryExpr.Operator.XOR;
            default: return BinaryExpr.Operator.PLUS; // 默认
        }
    }
    
    /**
     * 处理比较表达式
     */
    private Expression processComparisonExpression(InfixExpression node) {
        BinaryExpr.Operator operator = getBinaryOperator(node.getOperator());
        
        Expression leftExpr = createExpressionFromJSNode(node.getLeft());
        Expression rightExpr = createExpressionFromJSNode(node.getRight());
        
        return new BinaryExpr(leftExpr, rightExpr, operator);
    }
    
    /**
     * 处理逻辑表达式
     */
    private Expression processLogicalExpression(InfixExpression node) {
        BinaryExpr.Operator operator = getBinaryOperator(node.getOperator());
        
        Expression leftExpr = createExpressionFromJSNode(node.getLeft());
        Expression rightExpr = createExpressionFromJSNode(node.getRight());
        
        return new BinaryExpr(leftExpr, rightExpr, operator);
    }
    
    /**
     * 处理赋值表达式
     */
    private Expression processAssignmentExpression(Assignment node) {
        AssignExpr.Operator operator;
        
        switch (node.getOperator()) {
            case Token.ASSIGN_ADD: operator = AssignExpr.Operator.PLUS; break;
            case Token.ASSIGN_SUB: operator = AssignExpr.Operator.MINUS; break;
            case Token.ASSIGN_MUL: operator = AssignExpr.Operator.MULTIPLY; break;
            case Token.ASSIGN_DIV: operator = AssignExpr.Operator.DIVIDE; break;
            default: operator = AssignExpr.Operator.ASSIGN;
        }
        
        Expression target = createExpressionFromJSNode(node.getLeft());
        Expression value = createExpressionFromJSNode(node.getRight());
        
        return new AssignExpr(target, value, operator);
    }
    
    /**
     * 处理一元表达式
     */
    private Expression processUnaryExpression(org.mozilla.javascript.ast.UnaryExpression node) {
        Expression expr = createExpressionFromJSNode(node.getOperand());
        UnaryExpr.Operator operator;
        
        switch (node.getOperator()) {
            case Token.NEG: operator = UnaryExpr.Operator.MINUS; break;
            case Token.POS: operator = UnaryExpr.Operator.PLUS; break;
            case Token.NOT: operator = UnaryExpr.Operator.LOGICAL_COMPLEMENT; break;
            case Token.BITNOT: operator = UnaryExpr.Operator.BITWISE_COMPLEMENT; break;
            case Token.INC:
                // 处理++操作符
                int position = node.getPosition();
                int operandPosition = node.getOperand().getPosition();
                boolean isPostfix = position > operandPosition;
                return new UnaryExpr(expr, isPostfix ? 
                    UnaryExpr.Operator.POSTFIX_INCREMENT : 
                    UnaryExpr.Operator.PREFIX_INCREMENT);
            case Token.DEC:
                // 处理--操作符
                position = node.getPosition();
                operandPosition = node.getOperand().getPosition();
                isPostfix = position > operandPosition;
                return new UnaryExpr(expr, isPostfix ? 
                    UnaryExpr.Operator.POSTFIX_DECREMENT : 
                    UnaryExpr.Operator.PREFIX_DECREMENT);
            default: operator = UnaryExpr.Operator.PLUS; // 默认
        }
        
        return new UnaryExpr(expr, operator);
    }
    
    /**
     * 将JavaScript表达式转换为Java表达式字符串
     */
    public String createExpressionString(AstNode node) {
        Expression expr = createExpressionFromJSNode(node);
        return expr.toString();
    }
} 