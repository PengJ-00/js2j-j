package js2j;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;
import org.mozilla.javascript.ast.AstNode;  
import java.util.Collections;

/**
 * JavaScript到Java转换器的主入口类
 */
public class Main { 

    public static void main(String[] args) {
        if (args.length < 1) {
            System.out.println("用法: java -jar js2j.jar <JavaScript文件/目录> [输出目录]");
            System.out.println("如果未指定输出目录，将使用当前目录下的'java-output'");
            return;
        }

        String inputPath = args[0];
        String outputDir = args.length > 1 ? args[1] : "java-output";

        try {
            File inputFile = new File(inputPath);
            if (!inputFile.exists()) {
                System.err.println("错误: 输入文件或目录不存在 - " + inputPath);
                return;
            }

            // 确保输出目录存在
            File outDir = new File(outputDir);
            if (!outDir.exists() && !outDir.mkdirs()) {
                System.err.println("错误: 无法创建输出目录 - " + outputDir);
                return;
            }

            JavaScriptToJavaConverter converter = new JavaScriptToJavaConverter();

            if (inputFile.isDirectory()) {
                // 处理目录中的所有.js文件
                processDirectory(inputFile, outDir, converter);
            } else {
                // 处理单个文件
                if (inputFile.getName().endsWith(".js")) {
                    System.out.println("转换文件: " + inputFile.getPath());
                    converter.convertFile(inputFile, outDir);
                } else {
                    System.err.println("警告: 跳过非JavaScript文件 - " + inputFile.getPath());
                }
            }

            System.out.println("转换完成! 输出目录: " + outputDir);

        } catch (Exception e) {
            System.err.println("转换过程中发生错误:");
            e.printStackTrace();
        }
    }

    /**
     * 处理目录中的所有JavaScript文件
     * 
     * @param inputDir 输入目录
     * @param outputDir 输出目录
     * @param converter 转换器实例
     */
    public static void processDirectory(File inputDir, File outputDir, JavaScriptToJavaConverter converter) {
        try {
            // 递归获取所有.js文件
            List<Path> jsFiles = Files.walk(inputDir.toPath())
                .filter(Files::isRegularFile)
                .filter(p -> p.toString().endsWith(".js"))
                .collect(Collectors.toList());
            
            System.out.println("找到 " + jsFiles.size() + " 个JavaScript文件需要转换");
            
            for (Path jsFile : jsFiles) {
                // 计算相对路径，保持目录结构
                Path relativePath = inputDir.toPath().relativize(jsFile);
                
                // 确定输出目录(保持原始目录结构)
                Path outputPath = outputDir.toPath().resolve(relativePath).getParent();
                if (outputPath != null && !Files.exists(outputPath)) {
                    Files.createDirectories(outputPath);
                }
                
                // 转换文件
                System.out.println("转换文件: " + jsFile);
                File outputSubDir = outputPath != null ? outputPath.toFile() : outputDir;
                converter.convertFile(jsFile.toFile(), outputSubDir);
            }
        } catch (IOException e) {
            System.err.println("处理目录时发生错误: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 打印使用帮助
     */
    private static void printHelp() {
        System.out.println("JavaScript到Java转换器");
        System.out.println("---------------------");
        System.out.println("此工具将JavaScript代码转换为等效的Java代码");
        System.out.println();
        System.out.println("用法:");
        System.out.println("  java -jar js2j.jar <JavaScript文件/目录> [输出目录]");
        System.out.println();
        System.out.println("选项:");
        System.out.println("  <JavaScript文件/目录>  要转换的JavaScript文件或包含JavaScript文件的目录");
        System.out.println("  [输出目录]            转换后的Java文件的输出目录（默认为'java-output'）");
    }
} 