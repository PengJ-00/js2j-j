package js2j;

import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.expr.*;
import com.github.javaparser.ast.stmt.*;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import org.mozilla.javascript.Node;
import org.mozilla.javascript.Token;
import org.mozilla.javascript.ast.*;

import java.util.ArrayList;
import java.util.List;

/**
 * JavaScript控制流处理器
 * 负责处理JavaScript的控制流语句（if, for, while, switch等）
 */
public class JSControlFlowProcessor {
    
    private final JSExpressionProcessor expressionProcessor;
    private final JSStatementProcessor statementProcessor;
    
    public JSControlFlowProcessor(JSExpressionProcessor expressionProcessor, JSStatementProcessor statementProcessor) {
        this.expressionProcessor = expressionProcessor;
        this.statementProcessor = statementProcessor;
    }
    
    /**
     * 处理JavaScript的if语句（在类中）
     */
    public void processIfStatement(IfStatement ifStmt, ClassOrInterfaceDeclaration classDecl) {
        AstNode condition = ifStmt.getCondition();
        Expression conditionExpr = expressionProcessor.createExpressionFromJSNode(condition);
        
        BlockStmt thenBlock = new BlockStmt();
        AstNode thenPart = ifStmt.getThenPart();
        
        if (thenPart instanceof Block) {
            List<AstNode> thenStatements = statementProcessor.getBlockStatements((Block) thenPart);
            for (AstNode node : thenStatements) {
                processNodeInBlock(node, thenBlock);
            }
        } else if (thenPart != null) {
            processNodeInBlock(thenPart, thenBlock);
        }
        
        IfStmt ifStatement = new IfStmt();
        ifStatement.setCondition(conditionExpr);
        ifStatement.setThenStmt(thenBlock);
        
        AstNode elsePart = ifStmt.getElsePart();
        if (elsePart != null) {
            BlockStmt elseBlock = new BlockStmt();
            
            if (elsePart instanceof Block) {
                List<AstNode> elseStatements = statementProcessor.getBlockStatements((Block) elsePart);
                for (AstNode node : elseStatements) {
                    processNodeInBlock(node, elseBlock);
                }
            } else {
                processNodeInBlock(elsePart, elseBlock);
            }
            
            ifStatement.setElseStmt(elseBlock);
        }
        
        BlockStmt staticBlock = statementProcessor.ensureStaticBlock(classDecl);
        staticBlock.getStatements().add(ifStatement);
    }
    
    /**
     * 处理JavaScript的for循环（在类中）
     */
    public void processForLoop(ForLoop forLoop, ClassOrInterfaceDeclaration classDecl) {
        // 创建静态块
        BlockStmt staticBlock = statementProcessor.ensureStaticBlock(classDecl);
        
        // 使用通用的processForLoop方法处理for循环并添加到静态块中
        processForLoop(forLoop, staticBlock);
    }
    
    /**
     * 处理条件部分
     */
    private Expression processCondition(AstNode condition) {
        // 特殊处理常见模式，如 i < array.length
        if (condition instanceof InfixExpression) {
            InfixExpression infix = (InfixExpression) condition;
            
            // 处理数组长度比较
            if ((infix.getOperator() == Token.LT || infix.getOperator() == Token.LE) &&
                infix.getRight() instanceof PropertyGet) {
                
                PropertyGet propGet = (PropertyGet) infix.getRight();
                if (propGet.getProperty().getIdentifier().equals("length")) {
                    // 找到 array.length 模式
                    Expression leftExpr = expressionProcessor.createExpressionFromJSNode(infix.getLeft());
                    
                    // 使用 array.size() 替代
                    MethodCallExpr sizeExpr = new MethodCallExpr();
                    sizeExpr.setScope(expressionProcessor.createExpressionFromJSNode(propGet.getTarget()));
                    sizeExpr.setName("size");
                    
                    // 创建比较表达式
                    BinaryExpr.Operator operator = (infix.getOperator() == Token.LT) ? 
                        BinaryExpr.Operator.LESS : BinaryExpr.Operator.LESS_EQUALS;
                    
                    return new BinaryExpr(leftExpr, sizeExpr, operator);
                }
            }
        }
        
        // 默认处理
        return expressionProcessor.createExpressionFromJSNode(condition);
    }
    
    /**
     * 处理ForLoop节点 (针对BlockStmt版本)
     */
    public void processForLoop(ForLoop forLoop, BlockStmt block) {
        ForStmt forStmt = new ForStmt();
        
        // 处理初始化部分
        if (forLoop.getInitializer() != null) {
            if (forLoop.getInitializer() instanceof VariableDeclaration) {
                VariableDeclaration varDecl = (VariableDeclaration) forLoop.getInitializer();
                NodeList<Expression> initializations = new NodeList<>();
                
                for (VariableInitializer init : varDecl.getVariables()) {
                    // 获取变量名
                    String varName = "";
                    if (init.getTarget() instanceof org.mozilla.javascript.ast.Name) {
                        varName = ((org.mozilla.javascript.ast.Name) init.getTarget()).getIdentifier();
                    } else {
                        varName = "var_" + init.hashCode();
                    }
                    
                    // 为循环变量推断类型
                    String varType = "int"; // 循环变量默认为int类型
                    if (varName.equals("i") || varName.equals("j") || varName.equals("k") || 
                        varName.equals("index") || varName.endsWith("Index")) {
                        varType = "int";
                    }
                    
                    Expression initializer = init.getInitializer() != null 
                        ? expressionProcessor.createExpressionFromJSNode(init.getInitializer()) 
                        : null;
                        
                    // 创建变量类型
                    ClassOrInterfaceType type = new ClassOrInterfaceType();
                    type.setName(varType);
                    
                    // 创建变量声明表达式
                    VariableDeclarationExpr varDeclExpr = new VariableDeclarationExpr();
                    com.github.javaparser.ast.body.VariableDeclarator varDeclarator = new com.github.javaparser.ast.body.VariableDeclarator();
                    varDeclarator.setType(type);
                    varDeclarator.setName(varName);
                    if (initializer != null) {
                        varDeclarator.setInitializer(initializer);
                    }
                    
                    varDeclExpr.addVariable(varDeclarator);
                    varDeclExpr.setFinal(false);
                    
                    initializations.add(varDeclExpr);
                }
                
                forStmt.setInitialization(initializations);
            } else {
                // 非变量声明的初始化表达式，例如赋值表达式
                NodeList<Expression> initializations = new NodeList<>();
                initializations.add(expressionProcessor.createExpressionFromJSNode(forLoop.getInitializer()));
                forStmt.setInitialization(initializations);
            }
        } else {
            // 没有初始化部分
            forStmt.setInitialization(new NodeList<>());
        }
        
        // 处理条件部分
        if (forLoop.getCondition() != null) {
            forStmt.setCompare(processCondition(forLoop.getCondition()));
        }
        
        // 处理更新部分
        if (forLoop.getIncrement() != null) {
            NodeList<Expression> updates = new NodeList<>();
            AstNode increment = forLoop.getIncrement();
            
            // 处理不同类型的增量表达式
            Expression updateExpr = expressionProcessor.createExpressionFromJSNode(increment);
            updates.add(updateExpr);
            
            forStmt.setUpdate(updates);
        } else {
            // 没有更新部分
            forStmt.setUpdate(new NodeList<>());
        }
        
        // 处理循环体
        BlockStmt loopBody = new BlockStmt();
        AstNode body = forLoop.getBody();
        
        if (body instanceof Block) {
            List<AstNode> bodyStatements = statementProcessor.getBlockStatements((Block) body);
            for (AstNode node : bodyStatements) {
                processNodeInBlock(node, loopBody);
            }
        } else if (body != null) {
            processNodeInBlock(body, loopBody);
        }
        
        forStmt.setBody(loopBody);
        
        // 直接添加循环语句，不要额外包一层块
        block.addStatement(forStmt);
    }
    
    /**
     * 处理WhileLoop节点 (在类中)
     */
    public void processWhileLoop(WhileLoop whileLoop, ClassOrInterfaceDeclaration classDecl) {
        // 在块中处理while循环
        AstNode condition = whileLoop.getCondition();
        AstNode body = whileLoop.getBody();
        
        // 创建while语句
        WhileStmt whileStmt = new WhileStmt();
        whileStmt.setCondition(expressionProcessor.createExpressionFromJSNode(condition));
        
        // 处理循环体
        BlockStmt loopBody = new BlockStmt();
        if (body instanceof Block) {
            Block jsBlock = (Block) body;
            for (Node node : jsBlock) {
                if (node instanceof AstNode) {
                    processNodeInBlock((AstNode) node, loopBody);
                }
            }
        } else if (body instanceof AstNode) {
            processNodeInBlock((AstNode) body, loopBody);
        }
        
        whileStmt.setBody(loopBody);
        BlockStmt staticBlock = statementProcessor.ensureStaticBlock(classDecl);
        staticBlock.addStatement(whileStmt);
    }
    
    /**
     * 处理SwitchStatement节点 (在类中)
     */
    public void processSwitchStatement(SwitchStatement switchStmt, ClassOrInterfaceDeclaration classDecl) {
        // 在块中处理switch语句
        AstNode selector = switchStmt.getExpression();
        Expression selectorExpr = expressionProcessor.createExpressionFromJSNode(selector);
        
        // 创建switch语句
        SwitchStmt javaSwitchStmt = new SwitchStmt();
        javaSwitchStmt.setSelector(selectorExpr);
        
        // 处理case语句
        NodeList<SwitchEntry> entries = new NodeList<>();
        for (SwitchCase jsCase : switchStmt.getCases()) {
            SwitchEntry switchEntry = new SwitchEntry();
            
            // 设置case表达式
            if (jsCase.getExpression() != null) {
                switchEntry.getLabels().add(expressionProcessor.createExpressionFromJSNode(jsCase.getExpression()));
            } else {
                // default case
                switchEntry.getLabels().clear();
            }
            
            // 处理case中的语句
            BlockStmt caseBlock = new BlockStmt();
            for (AstNode statement : jsCase.getStatements()) {
                if (statement instanceof BreakStatement) {
                    // 忽略break语句，因为Java中的switch case自动break
                    continue;
                }
                
                if (statement instanceof Block) {
                    Block block = (Block) statement;
                    for (Node node : block) {
                        if (node instanceof AstNode) {
                            processNodeInBlock((AstNode) node, caseBlock);
                        }
                    }
                } else {
                    processNodeInBlock(statement, caseBlock);
                }
            }
            
            // 将case块的所有语句添加到switch entry中
            for (Statement stmt : caseBlock.getStatements()) {
                switchEntry.getStatements().add(stmt);
            }
            
            entries.add(switchEntry);
        }
        
        javaSwitchStmt.setEntries(entries);
        BlockStmt staticBlock = statementProcessor.ensureStaticBlock(classDecl);
        staticBlock.addStatement(javaSwitchStmt);
    }
    
    /**
     * 处理WhileLoop节点 (在块中)
     */
    public void processWhileLoopInBlock(WhileLoop whileLoop, BlockStmt parentBlock) {
        // 创建Java的while语句
        WhileStmt javaWhileStmt = new WhileStmt();
        
        // 设置条件
        javaWhileStmt.setCondition(expressionProcessor.createExpressionFromJSNode(whileLoop.getCondition()));
        
        // 处理循环体
        BlockStmt loopBodyBlock = new BlockStmt();
        AstNode body = whileLoop.getBody();
        
        if (body instanceof Block) {
            List<AstNode> statements = statementProcessor.getBlockStatements((Block) body);
            for (AstNode statement : statements) {
                processNodeInBlock(statement, loopBodyBlock);
            }
        } else if (body != null) {
            processNodeInBlock(body, loopBodyBlock);
        }
        
        javaWhileStmt.setBody(loopBodyBlock);
        
        // 直接添加while语句到父块，不要创建额外的大括号
        parentBlock.addStatement(javaWhileStmt);
    }
    
    /**
     * 处理SwitchStatement节点 (在块中)
     */
    public void processSwitchStatementInBlock(SwitchStatement switchStmt, BlockStmt parentBlock) {
        // 创建Java的switch语句
        SwitchStmt javaSwitchStmt = new SwitchStmt();
        
        // 设置选择器表达式
        javaSwitchStmt.setSelector(expressionProcessor.createExpressionFromJSNode(switchStmt.getExpression()));
        
        // 处理case语句
        NodeList<SwitchEntry> entries = new NodeList<>();
        for (SwitchCase jsCase : switchStmt.getCases()) {
            SwitchEntry entry = new SwitchEntry();
            
            // 设置case标签
            if (jsCase.getExpression() == null) {
                // default标签
                entry.setType(SwitchEntry.Type.STATEMENT_GROUP);
                entry.setLabels(new NodeList<>());
            } else {
                // 普通case标签
                NodeList<Expression> labels = new NodeList<>();
                labels.add(expressionProcessor.createExpressionFromJSNode(jsCase.getExpression()));
                entry.setLabels(labels);
                entry.setType(SwitchEntry.Type.STATEMENT_GROUP);
            }
            
            // 处理case中的语句
            List<AstNode> jsStatements = new ArrayList<>();
            if (jsCase.getStatements() != null) {
                for (Object stmt : jsCase.getStatements()) {
                    if (stmt instanceof AstNode) {
                        jsStatements.add((AstNode) stmt);
                    }
                }
            }
            
            // 创建临时块存储语句
            BlockStmt tempBlock = new BlockStmt();
            for (AstNode stmt : jsStatements) {
                processNodeInBlock(stmt, tempBlock);
            }
            
            // 将临时块中的语句添加到entry中
            NodeList<Statement> statements = new NodeList<>();
            for (Statement stmt : tempBlock.getStatements()) {
                statements.add(stmt);
            }
            entry.setStatements(statements);
            
            entries.add(entry);
        }
        
        javaSwitchStmt.setEntries(entries);
        
        // 直接添加switch语句到父块，不要创建额外的大括号
        parentBlock.addStatement(javaSwitchStmt);
    }
    
    /**
     * 在块中处理节点
     */
    private void processNodeInBlock(AstNode node, BlockStmt block) {
        if (node instanceof ExpressionStatement) {
            ExpressionStatement exprStmt = (ExpressionStatement) node;
            AstNode expression = exprStmt.getExpression();
            
            // 检查是否是赋值表达式
            if (expression instanceof Assignment) {
                Assignment assignment = (Assignment) expression;
                // 检查是否是复合赋值操作 (+=, -=, 等)
                int operator = assignment.getOperator();
                
                if (operator == Token.ASSIGN_ADD) {
                    // 处理 += 操作符
                    AstNode left = assignment.getLeft();
                    AstNode right = assignment.getRight();
                    
                    Expression leftExpr = expressionProcessor.createExpressionFromJSNode(left);
                    Expression rightExpr = expressionProcessor.createExpressionFromJSNode(right);
                    
                    // 创建正确的赋值表达式
                    AssignExpr assignExpr = new AssignExpr(
                        leftExpr, 
                        rightExpr, 
                        AssignExpr.Operator.PLUS
                    );
                    
                    block.addStatement(new ExpressionStmt(assignExpr));
                    return;
                }
            }
            
            // 检查表达式是否可能是隐式赋值（如 sum + array[i]）
            if (expression instanceof InfixExpression) {
                InfixExpression infix = (InfixExpression) expression;
                if (infix.getOperator() == Token.ADD) {
                    AstNode left = infix.getLeft();
                    AstNode right = infix.getRight();
                    
                    // 检查是否是 sum + array[i] 模式（左侧是简单变量，右侧是数组访问）
                    if (left instanceof org.mozilla.javascript.ast.Name && 
                        (right instanceof ElementGet || right instanceof PropertyGet)) {
                        String varName = ((org.mozilla.javascript.ast.Name) left).getIdentifier();
                        // 如果变量名暗示它是累加器
                        if (varName.equals("sum") || varName.equals("total") || 
                            varName.equals("count") || varName.equals("result")) {
                            // 创建 sum += array[i] 表达式
                            AssignExpr assignExpr = new AssignExpr(
                                new NameExpr(varName),
                                expressionProcessor.createExpressionFromJSNode(right),
                                AssignExpr.Operator.PLUS
                            );
                            block.addStatement(new ExpressionStmt(assignExpr));
                            return;
                        }
                    }
                }
            }
            
            // 默认处理
            block.addStatement(new ExpressionStmt(expressionProcessor.createExpressionFromJSNode(expression)));
        } else if (node instanceof ReturnStatement) {
            // 返回语句
            ReturnStatement returnStmt = (ReturnStatement) node;
            AstNode returnValue = returnStmt.getReturnValue();
            
            if (returnValue != null) {
                block.addStatement(new ReturnStmt(expressionProcessor.createExpressionFromJSNode(returnValue)));
            } else {
                block.addStatement(new ReturnStmt());
            }
        } else if (node instanceof IfStatement) {
            // 处理嵌套if语句
            processIfStatementInBlock((org.mozilla.javascript.ast.IfStatement) node, block);
        } else if (node instanceof ForLoop) {
            // 处理for循环
            processForLoop((ForLoop) node, block);
        } else if (node instanceof WhileLoop) {
            // 处理while循环
            processWhileLoopInBlock((WhileLoop) node, block);
        } else if (node instanceof SwitchStatement) {
            // 处理switch语句
            processSwitchStatementInBlock((SwitchStatement) node, block);
        } else if (node instanceof VariableDeclaration) {
            // 处理变量声明
            statementProcessor.processVariableDeclarationInBlock((VariableDeclaration) node, block);
        } else if (node instanceof Block) {
            // 处理代码块 - 直接处理内部语句，不要创建新的块
            Block jsBlock = (Block) node;
            
            for (org.mozilla.javascript.Node n : jsBlock) {
                if (n instanceof AstNode) {
                    processNodeInBlock((AstNode) n, block);
                }
            }
        } else if (node instanceof Scope) {
            // 处理作用域 - 直接处理内部语句，不要创建新的块
            Scope scope = (Scope) node;
            
            for (org.mozilla.javascript.Node n : scope) {
                if (n instanceof AstNode) {
                    processNodeInBlock((AstNode) n, block);
                }
            }
        } else {
            // 尝试作为表达式处理
            Expression expr = expressionProcessor.createExpressionFromJSNode(node);
            if (!(expr instanceof StringLiteralExpr) || !((StringLiteralExpr)expr).getValue().startsWith("/*")) {
                // 只有当结果不是一个注释字符串时才添加
                block.addStatement(new ExpressionStmt(expr));
            } else {
                // 对于未识别的节点类型，添加一个注释
                block.addStatement(new ExpressionStmt(new StringLiteralExpr(
                    "// 跳过未支持的节点: " + node.getClass().getSimpleName()
                )));
            }
        }
    }
    
    /**
     * 处理if语句 (在块中)
     */
    public void processIfStatementInBlock(org.mozilla.javascript.ast.IfStatement ifStmt, BlockStmt block) {
        // 处理条件
        Expression conditionExpr = expressionProcessor.createExpressionFromJSNode(ifStmt.getCondition());
        
        // 创建Java的if语句
        IfStmt ifStatement = new IfStmt();
        ifStatement.setCondition(conditionExpr);
        
        // 处理then部分
        BlockStmt thenBlock = new BlockStmt();
        AstNode thenPart = ifStmt.getThenPart();
        
        if (thenPart instanceof Block) {
            List<AstNode> thenStatements = statementProcessor.getBlockStatements((Block) thenPart);
            for (AstNode node : thenStatements) {
                processNodeInBlock(node, thenBlock);
            }
        } else if (thenPart != null) {
            processNodeInBlock(thenPart, thenBlock);
        }
        
        ifStatement.setThenStmt(thenBlock);
        
        // 处理else部分
        AstNode elsePart = ifStmt.getElsePart();
        if (elsePart != null) {
            BlockStmt elseBlock = new BlockStmt();
            
            if (elsePart instanceof Block) {
                List<AstNode> elseStatements = statementProcessor.getBlockStatements((Block) elsePart);
                for (AstNode node : elseStatements) {
                    processNodeInBlock(node, elseBlock);
                }
            } else {
                processNodeInBlock(elsePart, elseBlock);
            }
            
            ifStatement.setElseStmt(elseBlock);
        }
        
        // 直接添加if语句，不要额外包一层块
        block.addStatement(ifStatement);
    }
} 