package js2j;

import com.github.javaparser.ast.Modifier;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.InitializerDeclaration;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.*;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.ExpressionStmt;
import com.github.javaparser.ast.stmt.ReturnStmt;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import org.mozilla.javascript.Node;
import org.mozilla.javascript.Token;
import org.mozilla.javascript.ast.*;

import java.util.ArrayList;
import java.util.List;

/**
 * JavaScript语句处理器
 * 负责处理JavaScript语句并转换为Java语句
 */
public class JSStatementProcessor {
    
    private final JSExpressionProcessor expressionProcessor;
    private final JSObjectProcessor objectProcessor;
    
    /**
     * 构造函数
     */
    public JSStatementProcessor() {
        this.objectProcessor = new JSObjectProcessor();
        this.expressionProcessor = new JSExpressionProcessor(objectProcessor);
    }
    
    /**
     * 构造函数（使用依赖注入）
     */
    public JSStatementProcessor(JSExpressionProcessor expressionProcessor, JSObjectProcessor objectProcessor) {
        this.expressionProcessor = expressionProcessor;
        this.objectProcessor = objectProcessor;
    }
    
    /**
     * 处理变量声明，添加到Java块中
     */
    public void processVariableDeclarationInBlock(VariableDeclaration node, BlockStmt javaBlock) {
        for (VariableInitializer init : node.getVariables()) {
            if (init.getTarget() instanceof org.mozilla.javascript.ast.Name) {
                String varName = ((org.mozilla.javascript.ast.Name) init.getTarget()).getIdentifier();
                AstNode valueNode = init.getInitializer();
                
                // 确定变量类型
                String varType = "Object"; // 默认类型
                
                if (valueNode != null) {
                    Expression valueExpr = expressionProcessor.createExpressionFromJSNode(valueNode);
                    varType = JSTypeUtils.determineType(valueExpr);
                    
                    // 创建变量声明表达式
                    VariableDeclarationExpr varDecl = new VariableDeclarationExpr();
                    
                    // 创建变量声明器
                    VariableDeclarator var = new VariableDeclarator()
                        .setType(varType)
                        .setName(varName)
                        .setInitializer(valueExpr);
                    
                    varDecl.addVariable(var);
                    
                    // 添加到块
                    javaBlock.addStatement(new ExpressionStmt(varDecl));
                } else {
                    // 处理没有初始化器的变量声明
                    VariableDeclarationExpr varDecl = new VariableDeclarationExpr();
                    
                    // 根据变量名推断类型
                    varType = JSTypeUtils.determineTypeFromName(varName);
                    
                    // 创建变量声明器
                    VariableDeclarator var = new VariableDeclarator()
                        .setType(varType)
                        .setName(varName);
                    
                    varDecl.addVariable(var);
                    
                    // 添加到块
                    javaBlock.addStatement(new ExpressionStmt(varDecl));
                }
            }
        }
    }
    
    /**
     * 处理变量声明，返回Java代码字符串
     */
    public String processVariableDeclaration(VariableDeclaration node) {
        StringBuilder result = new StringBuilder();
        
        for (VariableInitializer init : node.getVariables()) {
            if (init.getTarget() instanceof org.mozilla.javascript.ast.Name) {
                String varName = ((org.mozilla.javascript.ast.Name) init.getTarget()).getIdentifier();
                AstNode valueNode = init.getInitializer();
                
                // 确定变量类型
                String varType = "Object"; // 默认类型
                
                if (valueNode != null) {
                    // 先将AstNode转换为Java表达式，然后获取字符串表示
                    Expression valueExpr = expressionProcessor.createExpressionFromJSNode(valueNode);
                    String valueExprStr = valueExpr.toString();
                    varType = JSTypeUtils.determineTypeFromName(varName);
                    
                    // 创建变量声明表达式
                    result.append("    ").append(varType).append(" ")
                          .append(varName).append(" = ")
                          .append(valueExprStr).append(";\n");
                } else {
                    // 处理没有初始化器的变量声明
                    varType = JSTypeUtils.determineTypeFromName(varName);
                    result.append("    ").append(varType).append(" ")
                          .append(varName).append(";\n");
                }
            }
        }
        
        return result.toString();
    }
    
    /**
     * 处理变量声明，添加到Java类中
     */
    public void processVariableDeclaration(VariableDeclaration node, ClassOrInterfaceDeclaration classDecl) {
        for (VariableInitializer init : node.getVariables()) {
            if (init.getTarget() instanceof org.mozilla.javascript.ast.Name) {
                String varName = ((org.mozilla.javascript.ast.Name) init.getTarget()).getIdentifier();
                AstNode valueNode = init.getInitializer();
                
                // 确定字段类型
                String fieldType = "Object"; // 默认类型
                
                if (valueNode != null) {
                    Expression valueExpr = expressionProcessor.createExpressionFromJSNode(valueNode);
                    fieldType = JSTypeUtils.determineType(valueExpr);
                    
                    // 创建字段声明
                    NodeList<Modifier> modifiers = NodeList.nodeList(Modifier.privateModifier(), Modifier.staticModifier());
                    VariableDeclarator var = new VariableDeclarator()
                        .setType(new ClassOrInterfaceType(null, fieldType))
                        .setName(varName);
                    
                    if (valueNode != null) {
                        var.setInitializer(valueExpr);
                    }
                    
                    // 添加字段到类
                    classDecl.addFieldWithInitializer(fieldType, varName, valueExpr, Modifier.Keyword.PRIVATE, Modifier.Keyword.STATIC);
                } else {
                    // 处理没有初始化器的字段声明
                    fieldType = JSTypeUtils.determineTypeFromName(varName);
                    
                    // 添加字段到类
                    classDecl.addField(fieldType, varName, Modifier.Keyword.PRIVATE, Modifier.Keyword.STATIC);
                }
            }
        }
    }
    
    /**
     * 处理表达式语句，添加到Java块中
     */
    public void processExpressionStatementInBlock(ExpressionStatement node, BlockStmt javaBlock) {
        Expression expr = expressionProcessor.createExpressionFromJSNode(node.getExpression());
        javaBlock.addStatement(new ExpressionStmt(expr));
    }
    
    /**
     * 处理数组初始化
     */
    private void processArrayInitialization(BlockStmt blockStmt, ArrayLiteral array, String varName, 
                                           String[] varTypeRef, ClassOrInterfaceDeclaration classDecl) {
        // 添加注释说明如何转换数组
        blockStmt.getStatements().add(new ExpressionStmt(
            new StringLiteralExpr("// 数组初始化: " + varName)
        ));
        
        // 直接创建ArrayList变量并初始化
        varTypeRef[0] = "List<Object>";
        
        // 创建一个ArrayList初始化
        ObjectCreationExpr arrayInit = new ObjectCreationExpr()
            .setType("ArrayList<Object>")
            .setArguments(NodeList.nodeList());
        
        // 为数组添加元素
        List<AstNode> elements = array.getElements();
        
        if (elements.isEmpty()) {
            // 空数组直接使用初始化表达式
            addVariableInitialization(blockStmt, varName, varTypeRef[0], arrayInit, classDecl);
        } else {
            // 创建变量
            VariableDeclarator tempVar = new VariableDeclarator()
                .setType(varTypeRef[0])
                .setName(varName)
                .setInitializer(arrayInit);
            
            blockStmt.getStatements().add(new ExpressionStmt(
                new VariableDeclarationExpr(tempVar)
            ));
            
            // 更新字段的类型
            final String varType = varTypeRef[0]; // 为lambda创建一个final变量
            classDecl.getFieldByName(varName).ifPresent(f -> {
                f.getVariable(0).setType(varType);
            });
            
            // 添加元素
            for (AstNode element : elements) {
                Expression elementExpr = expressionProcessor.createExpressionFromJSNode(element);
                
                MethodCallExpr addCall = new MethodCallExpr()
                    .setName("add")
                    .setScope(new NameExpr(varName))
                    .addArgument(elementExpr);
                blockStmt.getStatements().add(new ExpressionStmt(addCall));
            }
        }
    }
    
    /**
     * 添加变量初始化语句
     */
    private void addVariableInitialization(BlockStmt blockStmt, String varName, String varType, 
                                          Expression initExpr, ClassOrInterfaceDeclaration classDecl) {
        // 创建变量声明
        VariableDeclarator varDecl = new VariableDeclarator()
            .setType(varType)
            .setName(varName)
            .setInitializer(initExpr);
            
        blockStmt.getStatements().add(new ExpressionStmt(
            new VariableDeclarationExpr(varDecl)
        ));
        
        // 更新字段类型
        classDecl.getFieldByName(varName).ifPresent(f -> {
            f.getVariable(0).setType(varType);
        });
    }
    
    /**
     * 确保类声明中存在静态初始化块
     */
    public BlockStmt ensureStaticBlock(ClassOrInterfaceDeclaration classDecl) {
        // 查找现有的静态初始化块
        for (com.github.javaparser.ast.body.BodyDeclaration<?> member : classDecl.getMembers()) {
            if (member instanceof InitializerDeclaration) {
                InitializerDeclaration init = (InitializerDeclaration) member;
                if (init.isStatic()) {
                    return init.getBody();
                }
            }
        }
        
        // 如果没有找到，创建一个新的静态初始化块
        BlockStmt staticBlock = new BlockStmt();
        classDecl.addMember(new InitializerDeclaration(true, staticBlock));
        return staticBlock;
    }
    
    /**
     * 从Block对象中获取语句列表
     */
    public List<AstNode> getBlockStatements(Block block) {
        List<AstNode> result = new ArrayList<>();
        
        Node current = block.getFirstChild();
        while (current != null) {
            if (current instanceof AstNode) {
                result.add((AstNode) current);
            }
            current = current.getNext();
        }
        
        return result;
    }
} 