package js2j;

import com.github.javaparser.ast.Modifier;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.ReturnStmt;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import org.mozilla.javascript.ast.AstNode;
import org.mozilla.javascript.ast.Block;
import org.mozilla.javascript.ast.FunctionNode;
import org.mozilla.javascript.ast.Name;

import java.util.List;

/**
 * JavaScript函数处理器
 * 负责处理JavaScript函数节点，转换为Java方法
 */
public class JSFunctionProcessor {
    
    private final JSExpressionProcessor expressionProcessor;
    private final JSStatementProcessor statementProcessor;
    private final JSControlFlowProcessor controlFlowProcessor;
    
    /**
     * 构造函数
     */
    public JSFunctionProcessor() {
        this.expressionProcessor = new JSExpressionProcessor();
        this.statementProcessor = new JSStatementProcessor();
        this.controlFlowProcessor = new JSControlFlowProcessor(this.expressionProcessor, this.statementProcessor);
    }
    
    /**
     * 构造函数（使用依赖注入）
     */
    public JSFunctionProcessor(JSExpressionProcessor expressionProcessor, 
                             JSStatementProcessor statementProcessor,
                             JSControlFlowProcessor controlFlowProcessor) {
        this.expressionProcessor = expressionProcessor;
        this.statementProcessor = statementProcessor;
        this.controlFlowProcessor = controlFlowProcessor;
    }

    /**
     * 处理函数声明，转换为Java方法字符串
     */
    public String processFunctionNode(FunctionNode node) {
        String methodName = node.getName();
        String functionBody = node.getBody().toSource();
        
        // 根据函数名和内容推断返回类型
        String returnType = JSTypeUtils.determineReturnTypeFromMethodName(methodName, functionBody);
        
        // 创建方法
        StringBuilder methodBuilder = new StringBuilder();
        
        // 添加方法签名
        methodBuilder.append(returnType).append(" ").append(methodName).append("(");
        
        // 添加参数
        List<AstNode> params = node.getParams();
        for (int i = 0; i < params.size(); i++) {
            AstNode param = params.get(i);
            if (param instanceof Name) {
                String paramName = ((Name) param).getIdentifier();
                String paramType = JSTypeUtils.determineParameterType(paramName);
                
                methodBuilder.append(paramType).append(" ").append(paramName);
                if (i < params.size() - 1) {
                    methodBuilder.append(", ");
                }
            }
        }
        methodBuilder.append(") {\n");
        
        // 处理函数体
        methodBuilder.append(processFunctionBody(node.getBody()));
        
        // 关闭方法
        methodBuilder.append("}\n");
        
        return methodBuilder.toString();
    }
    
    /**
     * 将函数节点添加到Java类中
     */
    public void processFunctionNode(FunctionNode node, ClassOrInterfaceDeclaration classDecl) {
        String methodName = node.getName();
        String functionBody = node.getBody().toSource();
        
        // 根据函数名和内容推断返回类型
        String returnType = JSTypeUtils.determineReturnTypeFromMethodName(methodName, functionBody);
        
        // 创建方法
        MethodDeclaration method = classDecl.addMethod(methodName, Modifier.Keyword.PUBLIC, Modifier.Keyword.STATIC);
        
        // 设置返回类型
        method.setType(returnType);
        
        // 添加参数
        for (AstNode param : node.getParams()) {
            if (param instanceof Name) {
                String paramName = ((Name) param).getIdentifier();
                String paramType = JSTypeUtils.determineParameterType(paramName);
                
                method.addParameter(paramType, paramName);
            }
        }
        
        // 创建方法体
        BlockStmt body = new BlockStmt();
        method.setBody(body);
        
        // 获取函数体并处理语句
        AstNode functionBodyNode = node.getBody();
        processMethodBody(functionBodyNode, body);
    }
    
    /**
     * 处理函数体，转换为Java语句
     */
    private String processFunctionBody(AstNode bodyNode) {
        StringBuilder bodyBuilder = new StringBuilder();
        
        if (bodyNode instanceof Block) {
            Block block = (Block) bodyNode;
            List<AstNode> statements = statementProcessor.getBlockStatements(block);
            
            for (AstNode statement : statements) {
                if (statement instanceof org.mozilla.javascript.ast.ReturnStatement) {
                    // 处理返回语句
                    org.mozilla.javascript.ast.ReturnStatement returnStmt = 
                        (org.mozilla.javascript.ast.ReturnStatement) statement;
                    AstNode returnValue = returnStmt.getReturnValue();
                    
                    bodyBuilder.append("    return ");
                    if (returnValue != null) {
                        bodyBuilder.append(expressionProcessor.createExpressionString(returnValue));
                    }
                    bodyBuilder.append(";\n");
                } else {
                    // 处理其他语句
                    bodyBuilder.append(processStatement(statement));
                }
            }
        }
        
        return bodyBuilder.toString();
    }
    
    /**
     * 处理函数体中的单个语句
     */
    private String processStatement(AstNode node) {
        if (node instanceof org.mozilla.javascript.ast.ExpressionStatement) {
            org.mozilla.javascript.ast.ExpressionStatement exprStmt = 
                (org.mozilla.javascript.ast.ExpressionStatement) node;
            return "    " + expressionProcessor.createExpressionString(exprStmt.getExpression()) + ";\n";
        } else if (node instanceof org.mozilla.javascript.ast.VariableDeclaration) {
            return statementProcessor.processVariableDeclaration((org.mozilla.javascript.ast.VariableDeclaration) node);
        } else if (node instanceof org.mozilla.javascript.ast.IfStatement) {
            // 创建一个临时BlockStmt来处理IfStatement
            BlockStmt tempBlock = new BlockStmt();
            controlFlowProcessor.processIfStatementInBlock((org.mozilla.javascript.ast.IfStatement) node, tempBlock);
            return "    " + tempBlock.toString() + "\n";
        } else if (node instanceof org.mozilla.javascript.ast.ForLoop) {
            // 创建一个临时BlockStmt来处理ForLoop
            BlockStmt tempBlock = new BlockStmt();
            controlFlowProcessor.processForLoop((org.mozilla.javascript.ast.ForLoop) node, tempBlock);
            return "    " + tempBlock.toString() + "\n";
        } else if (node instanceof org.mozilla.javascript.ast.WhileLoop) {
            // 创建一个临时BlockStmt来处理WhileLoop
            BlockStmt tempBlock = new BlockStmt();
            controlFlowProcessor.processWhileLoopInBlock((org.mozilla.javascript.ast.WhileLoop) node, tempBlock);
            return "    " + tempBlock.toString() + "\n";
        } else if (node instanceof org.mozilla.javascript.ast.SwitchStatement) {
            // 创建一个临时BlockStmt来处理SwitchStatement
            BlockStmt tempBlock = new BlockStmt();
            controlFlowProcessor.processSwitchStatementInBlock((org.mozilla.javascript.ast.SwitchStatement) node, tempBlock);
            return "    " + tempBlock.toString() + "\n";
        } else {
            return "    // 不支持的节点类型: " + node.getClass().getSimpleName() + "\n";
        }
    }
    
    /**
     * 处理方法体
     */
    private void processMethodBody(AstNode functionBodyNode, BlockStmt body) {
        if (functionBodyNode instanceof Block) {
            Block block = (Block) functionBodyNode;
            List<AstNode> statements = statementProcessor.getBlockStatements(block);
            
            for (AstNode statement : statements) {
                if (statement instanceof org.mozilla.javascript.ast.ReturnStatement) {
                    // 处理返回语句
                    org.mozilla.javascript.ast.ReturnStatement returnStmt = 
                        (org.mozilla.javascript.ast.ReturnStatement) statement;
                    AstNode returnValue = returnStmt.getReturnValue();
                    
                    if (returnValue != null) {
                        body.addStatement(new ReturnStmt(
                            expressionProcessor.createExpressionFromJSNode(returnValue)));
                    } else {
                        body.addStatement(new ReturnStmt());
                    }
                } else {
                    // 处理其他语句
                    processStatementInBlock(statement, body);
                }
            }
        }
    }
    
    /**
     * 在块中处理语句
     */
    private void processStatementInBlock(AstNode node, BlockStmt body) {
        if (node instanceof org.mozilla.javascript.ast.ExpressionStatement) {
            statementProcessor.processExpressionStatementInBlock(
                (org.mozilla.javascript.ast.ExpressionStatement) node, body);
        } else if (node instanceof org.mozilla.javascript.ast.VariableDeclaration) {
            statementProcessor.processVariableDeclarationInBlock(
                (org.mozilla.javascript.ast.VariableDeclaration) node, body);
        } else if (node instanceof org.mozilla.javascript.ast.IfStatement) {
            controlFlowProcessor.processIfStatementInBlock(
                (org.mozilla.javascript.ast.IfStatement) node, body);
        } else if (node instanceof org.mozilla.javascript.ast.ForLoop) {
            // 使用正确的处理方法
            controlFlowProcessor.processForLoop(
                (org.mozilla.javascript.ast.ForLoop) node, body);
        } else if (node instanceof org.mozilla.javascript.ast.WhileLoop) {
            controlFlowProcessor.processWhileLoopInBlock(
                (org.mozilla.javascript.ast.WhileLoop) node, body);
        } else if (node instanceof org.mozilla.javascript.ast.SwitchStatement) {
            controlFlowProcessor.processSwitchStatementInBlock(
                (org.mozilla.javascript.ast.SwitchStatement) node, body);
        }
    }
} 