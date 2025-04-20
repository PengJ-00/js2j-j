package js2j;

import org.mozilla.javascript.ast.*;
import org.mozilla.javascript.Token;
import com.github.javaparser.ast.Modifier;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.ConstructorDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.ExpressionStmt;
import com.github.javaparser.ast.stmt.ReturnStmt;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.ast.type.VoidType;

import java.util.ArrayList;
import java.util.List;

/**
 * JavaScript类处理器
 * 负责处理ES6类语法，转换为Java类
 */
public class JSClassProcessor {
    
    private final JSExpressionProcessor expressionProcessor;
    private final JSStatementProcessor statementProcessor;
    private final JSFunctionProcessor functionProcessor;
    
    /**
     * 构造函数
     */
    public JSClassProcessor() {
        JSObjectProcessor objectProcessor = new JSObjectProcessor();
        this.expressionProcessor = new JSExpressionProcessor(objectProcessor);
        this.statementProcessor = new JSStatementProcessor(expressionProcessor, objectProcessor);
        this.functionProcessor = new JSFunctionProcessor(expressionProcessor, statementProcessor, 
                                                       new JSControlFlowProcessor(expressionProcessor, statementProcessor));
    }
    
    /**
     * 构造函数（使用依赖注入）
     */
    public JSClassProcessor(JSExpressionProcessor expressionProcessor, 
                           JSStatementProcessor statementProcessor,
                           JSFunctionProcessor functionProcessor) {
        this.expressionProcessor = expressionProcessor;
        this.statementProcessor = statementProcessor;
        this.functionProcessor = functionProcessor;
    }
    
    /**
     * 处理JavaScript类定义，转换为Java类
     * @param node 类节点
     * @return Java类定义字符串
     */
    public String processClass(AstNode node) {
        if (!(node instanceof ObjectLiteral)) {
            return "// 不支持的类定义格式";
        }
        
        ObjectLiteral classNode = (ObjectLiteral) node;
        String className = getClassName(classNode);
        
        StringBuilder result = new StringBuilder();
        result.append("public class ").append(className).append(" {\n");
        
        // 添加字段
        List<String> fields = extractFields(classNode);
        for (String field : fields) {
            result.append("    ").append(field).append("\n");
        }
        
        // 添加构造函数
        String constructor = extractConstructor(classNode);
        if (!constructor.isEmpty()) {
            result.append("\n    ").append(constructor).append("\n");
        }
        
        // 添加方法
        List<String> methods = extractMethods(classNode);
        for (String method : methods) {
            result.append("\n    ").append(method).append("\n");
        }
        
        result.append("}\n");
        return result.toString();
    }
    
    /**
     * 从类节点中获取类名
     */
    private String getClassName(ObjectLiteral classNode) {
        // 尝试从上下文中获取类名
        // 这里简化实现，返回默认类名
        return "GeneratedClass";
    }
    
    /**
     * 从类中提取字段
     */
    private List<String> extractFields(ObjectLiteral classNode) {
        List<String> fields = new ArrayList<>();
        
        for (ObjectProperty prop : classNode.getElements()) {
            if (prop.getRight() instanceof FunctionNode) {
                FunctionNode funcNode = (FunctionNode) prop.getRight();
                if (funcNode.getFunctionName() != null && 
                    "constructor".equals(funcNode.getFunctionName().getIdentifier())) {
                    // 从构造函数中提取字段（this.xxx = yyy）
                    List<String> fieldsFromConstructor = extractFieldsFromConstructor(funcNode);
                    fields.addAll(fieldsFromConstructor);
                }
            }
        }
        
        return fields;
    }
    
    /**
     * 从构造函数中提取字段
     */
    private List<String> extractFieldsFromConstructor(FunctionNode constructorNode) {
        List<String> fields = new ArrayList<>();
        org.mozilla.javascript.ast.AstNode body = constructorNode.getBody();
        
        if (body instanceof Block) {
            Block blockBody = (Block) body;
            List<AstNode> statements = statementProcessor.getBlockStatements(blockBody);
            for (AstNode statement : statements) {
                if (statement instanceof ExpressionStatement) {
                    ExpressionStatement exprStmt = (ExpressionStatement) statement;
                    AstNode expr = exprStmt.getExpression();
                    
                    if (expr instanceof Assignment) {
                        Assignment assignment = (Assignment) expr;
                        AstNode left = assignment.getLeft();
                        
                        if (left instanceof PropertyGet) {
                            PropertyGet propGet = (PropertyGet) left;
                            AstNode target = propGet.getTarget();
                            
                            if (target instanceof org.mozilla.javascript.ast.Name) {
                                org.mozilla.javascript.ast.Name name = (org.mozilla.javascript.ast.Name) target;
                                if (name.getIdentifier().equals("this")) {
                                    // 这是this.xxx = yyy的形式
                                    if (propGet.getProperty() instanceof org.mozilla.javascript.ast.Name) {
                                        String fieldName = ((org.mozilla.javascript.ast.Name) propGet.getProperty()).getIdentifier();
                                        
                                        // 确定字段类型（简化实现，默认使用Object）
                                        String fieldType = inferFieldType(assignment.getRight());
                                        
                                        // 生成字段声明
                                        fields.add("private " + fieldType + " " + fieldName + ";");
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        
        return fields;
    }
    
    /**
     * 推断字段类型
     */
    private String inferFieldType(AstNode valueNode) {
        if (valueNode instanceof NumberLiteral) {
            // 检查是否包含小数点
            String numStr = ((NumberLiteral) valueNode).getValue();
            if (numStr.contains(".")) {
                return "double";
            } else {
                return "int";
            }
        } else if (valueNode instanceof StringLiteral) {
            return "String";
        } else if (valueNode instanceof KeywordLiteral) {
            KeywordLiteral keyword = (KeywordLiteral) valueNode;
            if (keyword.getType() == Token.TRUE || 
                keyword.getType() == Token.FALSE) {
                return "boolean";
            } else if (keyword.getType() == Token.NULL) {
                return "Object";
            }
        } else if (valueNode instanceof ArrayLiteral) {
            return "List<Object>";
        } else if (valueNode instanceof ObjectLiteral) {
            return "Map<String, Object>";
        }
        
        return "Object";
    }
    
    /**
     * 提取构造函数
     */
    private String extractConstructor(ObjectLiteral classNode) {
        for (ObjectProperty prop : classNode.getElements()) {
            if (prop.getRight() instanceof FunctionNode) {
                FunctionNode funcNode = (FunctionNode) prop.getRight();
                if (funcNode.getFunctionName() != null && 
                    "constructor".equals(funcNode.getFunctionName().getIdentifier())) {
                    return processConstructor(funcNode);
                }
            }
        }
        
        return "";
    }
    
    /**
     * 处理构造函数
     */
    private String processConstructor(FunctionNode constructorNode) {
        StringBuilder result = new StringBuilder();
        String className = "GeneratedClass"; // 应该和类名匹配
        
        // 处理参数
        List<AstNode> params = constructorNode.getParams();
        StringBuilder paramList = new StringBuilder();
        
        for (int i = 0; i < params.size(); i++) {
            AstNode param = params.get(i);
            if (param instanceof org.mozilla.javascript.ast.Name) {
                String paramName = ((org.mozilla.javascript.ast.Name) param).getIdentifier();
                String paramType = "Object"; // 简化实现，默认使用Object
                
                paramList.append(paramType).append(" ").append(paramName);
                
                if (i < params.size() - 1) {
                    paramList.append(", ");
                }
            }
        }
        
        result.append("public ").append(className).append("(").append(paramList).append(") {");
        
        // 处理构造函数体
        AstNode body = constructorNode.getBody();
        if (body instanceof Block) {
            Block blockBody = (Block) body;
            List<AstNode> statements = statementProcessor.getBlockStatements(blockBody);
            for (AstNode statement : statements) {
                if (statement instanceof ExpressionStatement) {
                    ExpressionStatement exprStmt = (ExpressionStatement) statement;
                    AstNode expr = exprStmt.getExpression();
                    
                    if (expr instanceof Assignment) {
                        Assignment assignment = (Assignment) expr;
                        AstNode left = assignment.getLeft();
                        
                        if (left instanceof PropertyGet) {
                            PropertyGet propGet = (PropertyGet) left;
                            AstNode target = propGet.getTarget();
                            
                            if (target instanceof org.mozilla.javascript.ast.Name) {
                                org.mozilla.javascript.ast.Name name = (org.mozilla.javascript.ast.Name) target;
                                if (name.getIdentifier().equals("this")) {
                                    // 这是this.xxx = yyy的形式
                                    if (propGet.getProperty() instanceof org.mozilla.javascript.ast.Name) {
                                        String fieldName = ((org.mozilla.javascript.ast.Name) propGet.getProperty()).getIdentifier();
                                        String fieldValue = expressionProcessor.createExpressionFromJSNode(assignment.getRight()).toString();
                                        
                                        // 添加字段赋值
                                        result.append("\n        this.").append(fieldName).append(" = ").append(fieldValue).append(";");
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        
        result.append("\n    }");
        return result.toString();
    }
    
    /**
     * 提取方法
     */
    private List<String> extractMethods(ObjectLiteral classNode) {
        List<String> methods = new ArrayList<>();
        
        for (ObjectProperty prop : classNode.getElements()) {
            if (prop.getRight() instanceof FunctionNode) {
                FunctionNode funcNode = (FunctionNode) prop.getRight();
                String methodName = "";
                
                if (prop.getLeft() instanceof org.mozilla.javascript.ast.Name) {
                    methodName = ((org.mozilla.javascript.ast.Name) prop.getLeft()).getIdentifier();
                } else {
                    methodName = prop.getLeft().toSource();
                }
                
                // 排除构造函数
                if (!"constructor".equals(methodName)) {
                    methods.add(processMethod(funcNode, methodName));
                }
            }
        }
        
        return methods;
    }
    
    /**
     * 处理方法
     */
    private String processMethod(FunctionNode methodNode, String methodName) {
        StringBuilder result = new StringBuilder();
        
        // 处理返回类型和参数
        String returnType = inferReturnType(methodNode);
        List<AstNode> params = methodNode.getParams();
        StringBuilder paramList = new StringBuilder();
        
        for (int i = 0; i < params.size(); i++) {
            AstNode param = params.get(i);
            if (param instanceof org.mozilla.javascript.ast.Name) {
                String paramName = ((org.mozilla.javascript.ast.Name) param).getIdentifier();
                String paramType = "Object"; // 简化实现，默认使用Object
                
                paramList.append(paramType).append(" ").append(paramName);
                
                if (i < params.size() - 1) {
                    paramList.append(", ");
                }
            }
        }
        
        result.append("public ").append(returnType).append(" ").append(methodName).append("(")
              .append(paramList).append(") {");
        
        // 处理方法体
        AstNode body = methodNode.getBody();
        if (body instanceof Block) {
            Block blockBody = (Block) body;
            List<AstNode> statements = statementProcessor.getBlockStatements(blockBody);
            for (AstNode statement : statements) {
                if (statement instanceof ReturnStatement) {
                    ReturnStatement returnStmt = (ReturnStatement) statement;
                    AstNode returnValue = returnStmt.getReturnValue();
                    
                    if (returnValue != null) {
                        String returnExpr = expressionProcessor.createExpressionFromJSNode(returnValue).toString();
                        result.append("\n        return ").append(returnExpr).append(";");
                    } else {
                        result.append("\n        return;");
                    }
                } else {
                    // 处理其他语句，这里简化处理，实际情况需要更多逻辑
                    String statementStr = statement.toSource().trim();
                    result.append("\n        ").append(statementStr).append(";");
                }
            }
        }
        
        result.append("\n    }");
        return result.toString();
    }
    
    /**
     * 推断方法返回类型
     */
    private String inferReturnType(FunctionNode methodNode) {
        // 检查是否有return语句
        AstNode body = methodNode.getBody();
        if (body instanceof Block) {
            Block blockBody = (Block) body;
            List<AstNode> statements = statementProcessor.getBlockStatements(blockBody);
            for (AstNode statement : statements) {
                if (statement instanceof ReturnStatement) {
                    ReturnStatement returnStmt = (ReturnStatement) statement;
                    AstNode returnValue = returnStmt.getReturnValue();
                    
                    if (returnValue != null) {
                        return inferFieldType(returnValue);
                    } else {
                        return "void";
                    }
                }
            }
        }
        
        return "void";
    }
} 