package js2j;

import org.mozilla.javascript.CompilerEnvirons;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Parser;
import org.mozilla.javascript.ast.AstRoot;

/**
 * JavaScript解析器
 * 使用Rhino引擎解析JavaScript代码并生成抽象语法树(AST)
 */
public class JSParser {
    
    private final CompilerEnvirons compilerEnvirons;
    
    /**
     * 构造函数，初始化解析器环境
     */
    public JSParser() {
        this.compilerEnvirons = new CompilerEnvirons();
        this.compilerEnvirons.setLanguageVersion(Context.VERSION_ES6);
        this.compilerEnvirons.setRecordingComments(true);
        this.compilerEnvirons.setRecordingLocalJsDocComments(true);
    }
    
    /**
     * 解析JavaScript代码并返回AST
     * 
     * @param jsCode JavaScript代码字符串
     * @return 解析后的AST根节点
     */
    public AstRoot parse(String jsCode) {
        Parser parser = new Parser(compilerEnvirons);
        return parser.parse(jsCode, null, 1);
    }
    
    /**
     * 解析JavaScript代码并返回AST，指定源文件名
     * 
     * @param jsCode JavaScript代码字符串
     * @param sourceFileName 源文件名
     * @return 解析后的AST根节点
     */
    public AstRoot parse(String jsCode, String sourceFileName) {
        Parser parser = new Parser(compilerEnvirons);
        return parser.parse(jsCode, sourceFileName, 1);
    }
} 