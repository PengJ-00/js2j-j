package js2j;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

/**
 * JavaScript源代码预处理器
 * 用于对JavaScript源码进行预处理，处理Rhino解析器不支持的语法
 */
public class JSSourcePreprocessor {
    
    /**
     * 预处理源代码，移除Rhino不支持的ES6语法
     */
    public String preProcessSource(String source) {
        // 移除import语句
        source = source.replaceAll("import\\s+.*?;", "// import statement removed");
        
        // 移除export语句(普通函数导出)
        source = source.replaceAll("export\\s+function", "function");
        
        // 移除export default语句
        source = source.replaceAll("export\\s+default\\s+function", "function");
        
        // 移除export const/let/var
        source = source.replaceAll("export\\s+(const|let|var)", "$1");
        
        // 处理ES6的模板字符串 (反引号字符串)
        source = handleTemplateLiterals(source);
        
        // 将class替换为构造函数形式
        source = replaceClassWithConstructor(source);
        
        // 将const和let替换为var (Rhino不完全支持)
        source = source.replaceAll("(const|let)\\s+", "var ");
        
        // 处理for...of循环
        source = replaceForOfLoops(source);
        
        // 处理箭头函数
        source = handleArrowFunctions(source);
        
        return source;
    }
    
    /**
     * 处理ES6的模板字符串
     */
    private String handleTemplateLiterals(String source) {
        StringBuilder result = new StringBuilder();
        int lastPos = 0;
        
        // 简单的状态机来扫描模板字符串
        boolean inSingleQuotes = false;
        boolean inDoubleQuotes = false;
        boolean inBackticks = false;
        boolean escapeNext = false;
        
        for (int i = 0; i < source.length(); i++) {
            char c = source.charAt(i);
            
            if (escapeNext) {
                escapeNext = false;
                continue;
            }
            
            if (c == '\\') {
                escapeNext = true;
                continue;
            }
            
            if (!inSingleQuotes && !inDoubleQuotes && !inBackticks) {
                if (c == '\'') {
                    inSingleQuotes = true;
                } else if (c == '"') {
                    inDoubleQuotes = true;
                } else if (c == '`') {
                    // 找到一个模板字符串的开始
                    inBackticks = true;
                    
                    // 添加之前的内容
                    result.append(source.substring(lastPos, i));
                    
                    // 开始一个新的字符串
                    result.append("\"");
                    lastPos = i + 1;
                }
            } else if (inSingleQuotes && c == '\'') {
                inSingleQuotes = false;
            } else if (inDoubleQuotes && c == '"') {
                inDoubleQuotes = false;
            } else if (inBackticks && c == '`') {
                // 找到模板字符串的结束
                inBackticks = false;
                
                // 添加内容，但需要处理 ${...} 表达式
                String template = source.substring(lastPos, i);
                template = convertTemplateContent(template);
                result.append(template).append("\"");
                
                lastPos = i + 1;
            } else if (inBackticks && c == '$' && i < source.length() - 1 && source.charAt(i + 1) == '{') {
                // 找到了 ${...} 表达式
                result.append(source.substring(lastPos, i).replace("\"", "\\\"").replace("\n", "\\n"));
                result.append("\" + ");
                
                // 跳过 ${
                lastPos = i + 2;
                i++; // 跳过 {
                
                // 查找闭合的 }
                int braceLevel = 1;
                int j = lastPos;
                for (; j < source.length(); j++) {
                    if (source.charAt(j) == '{') braceLevel++;
                    else if (source.charAt(j) == '}') braceLevel--;
                    
                    if (braceLevel == 0) break;
                }
                
                if (j < source.length()) {
                    // 找到了匹配的 }
                    String expr = source.substring(lastPos, j);
                    result.append(expr).append(" + \"");
                    lastPos = j + 1;
                    i = j; // 跳过已处理的内容
                }
            }
        }
        
        // 添加剩余内容
        if (lastPos < source.length()) {
            result.append(source.substring(lastPos));
        }
        
        return result.toString();
    }
    
    /**
     * 转换模板字符串内容，处理转义字符
     */
    private String convertTemplateContent(String template) {
        return template.replace("\\", "\\\\")
                      .replace("\"", "\\\"")
                      .replace("\n", "\\n")
                      .replace("\r", "\\r");
    }
    
    /**
     * 处理for...of循环
     */
    private String replaceForOfLoops(String source) {
        // 匹配 for (let x of array) 或 for(var y of items) 等形式
        StringBuilder result = new StringBuilder();
        int lastPos = 0;
        
        // 简单的正则匹配
        Pattern pattern = Pattern.compile(
            "for\\s*\\(\\s*(var|let|const)?\\s*(\\w+)\\s+of\\s+(\\w+)\\s*\\)\\s*\\{");
        Matcher matcher = pattern.matcher(source);
        
        while (matcher.find()) {
            String varType = matcher.group(1) != null ? matcher.group(1) : "var";
            String varName = matcher.group(2);
            String arrayName = matcher.group(3);
            
            result.append(source.substring(lastPos, matcher.start()));
            result.append("for (var " + varName + "_index = 0; " + varName + "_index < " + arrayName + ".length; " + varName + "_index++) {\n");
            result.append("    var " + varName + " = " + arrayName + "[" + varName + "_index];\n");
            
            lastPos = matcher.end();
        }
        
        // 添加剩余内容
        if (lastPos < source.length()) {
            result.append(source.substring(lastPos));
        }
        
        return result.toString();
    }
    
    /**
     * 处理ES6的箭头函数
     */
    private String handleArrowFunctions(String source) {
        // 处理单参数箭头函数 a => expr
        source = source.replaceAll("(\\w+)\\s*=>\\s*([^{][^;]*)(;|$)", "function($1) { return $2; }$3");
        
        // 处理带括号的单参数或多参数箭头函数 (a) => expr 或 (a, b) => expr
        source = source.replaceAll("\\((.*?)\\)\\s*=>\\s*([^{][^;]*)(;|$)", "function($1) { return $2; }$3");
        
        // 处理带函数体的箭头函数 a => { ... }
        source = source.replaceAll("(\\w+)\\s*=>\\s*\\{", "function($1) {");
        
        // 处理带括号的带函数体箭头函数 (a) => { ... } 或 (a, b) => { ... }
        source = source.replaceAll("\\((.*?)\\)\\s*=>\\s*\\{", "function($1) {");
        
        return source;
    }
    
    /**
     * 将class声明替换为构造函数形式
     */
    private String replaceClassWithConstructor(String source) {
        // 这是一个简化的处理，不能处理复杂的class定义
        StringBuilder result = new StringBuilder();
        int lastPos = 0;
        
        // 匹配简单的class定义
        Pattern pattern = Pattern.compile(
            "class\\s+(\\w+)\\s*\\{");
        Matcher matcher = pattern.matcher(source);
        
        while (matcher.find()) {
            String className = matcher.group(1);
            result.append(source.substring(lastPos, matcher.start()));
            
            // 替换为构造函数
            result.append("function " + className);
            
            // 找到constructor方法
            int classStart = matcher.end();
            int braceLevel = 1;
            int classEnd = classStart;
            
            while (braceLevel > 0 && classEnd < source.length()) {
                char c = source.charAt(classEnd);
                if (c == '{') braceLevel++;
                else if (c == '}') braceLevel--;
                classEnd++;
            }
            
            if (classEnd <= source.length()) {
                String classBody = source.substring(classStart, classEnd - 1);
                
                // 查找构造函数
                Pattern ctorPattern = Pattern.compile(
                    "constructor\\s*\\((.*?)\\)\\s*\\{([^}]*?)\\}");
                Matcher ctorMatcher = ctorPattern.matcher(classBody);
                
                if (ctorMatcher.find()) {
                    // 有构造函数
                    String params = ctorMatcher.group(1);
                    String body = ctorMatcher.group(2);
                    
                    result.append("(" + params + ") {" + body + "}");
                } else {
                    // 没有构造函数
                    result.append("() {}");
                }
                
                // 查找类方法
                Pattern methodPattern = Pattern.compile(
                    "(\\w+)\\s*\\((.*?)\\)\\s*\\{([^}]*?)\\}");
                Matcher methodMatcher = methodPattern.matcher(classBody);
                
                while (methodMatcher.find()) {
                    String methodName = methodMatcher.group(1);
                    String methodParams = methodMatcher.group(2);
                    String methodBody = methodMatcher.group(3);
                    
                    // 跳过构造函数
                    if (!methodName.equals("constructor")) {
                        // 添加原型方法
                        result.append("\n\n" + className + ".prototype." + methodName + 
                                    " = function(" + methodParams + ") {" + methodBody + "};");
                    }
                }
                
                lastPos = classEnd;
            } else {
                // 无法找到类结束
                lastPos = matcher.end();
            }
        }
        
        // 添加剩余内容
        if (lastPos < source.length()) {
            result.append(source.substring(lastPos));
        }
        
        return result.toString();
    }
    
    /**
     * 将源代码分割为可能的逻辑块
     */
    public List<String> splitIntoBlocks(String source) {
        List<String> blocks = new ArrayList<>();
        
        // 简单的启发式分割：按照函数定义、var/let/const 声明以及语句块分割
        String[] lines = source.split("\n");
        StringBuilder currentBlock = new StringBuilder();
        int braceLevel = 0;
        boolean inFunction = false;
        
        for (String line : lines) {
            // 计算行中的花括号数量
            for (char c : line.toCharArray()) {
                if (c == '{') braceLevel++;
                else if (c == '}') braceLevel--;
            }
            
            // 添加当前行到当前块
            currentBlock.append(line).append("\n");
            
            // 检查是否开始一个函数定义
            if (line.matches(".*function\\s+\\w+\\s*\\(.*\\)\\s*\\{.*") && !inFunction) {
                inFunction = true;
            }
            
            // 检查是否结束一个块或函数
            if (braceLevel == 0) {
                if (inFunction || line.trim().endsWith("}")) {
                    // 结束一个完整的块
                    blocks.add(currentBlock.toString());
                    currentBlock = new StringBuilder();
                    inFunction = false;
                } else if (line.trim().endsWith(";") && currentBlock.length() > 0) {
                    // 结束一个语句
                    blocks.add(currentBlock.toString());
                    currentBlock = new StringBuilder();
                }
            }
        }
        
        // 添加最后一个块（如果有）
        if (currentBlock.length() > 0) {
            blocks.add(currentBlock.toString());
        }
        
        return blocks;
    }
} 