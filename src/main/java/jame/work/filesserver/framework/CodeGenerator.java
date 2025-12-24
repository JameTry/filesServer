//package jame.work.filesserver.framework;
//
//
//import com.baomidou.mybatisplus.generator.FastAutoGenerator;
//import com.baomidou.mybatisplus.generator.engine.VelocityTemplateEngine;
//
//import java.nio.file.Paths;
//
//// 演示例子，执行 main 方法控制台输入模块表名回车自动生成对应项目目录中
//public class CodeGenerator {
//
//    public static void main(String[] args) {
//        FastAutoGenerator.create("jdbc:mysql://127.0.0.1:3306/files", "root", "root")
//                .globalConfig(builder -> builder
//                        .author("Jame")
//                        .outputDir(Paths.get(System.getProperty("user.dir")) + "/src/main/java")
//                        .commentDate("yyyy-MM-dd")
//                )
//                .packageConfig(builder -> builder
//                        .parent("jame.work.filesserver")
//                        .entity("entity")
//                        .mapper("mapper")
//                        .service("service")
//                        .serviceImpl("service.impl")
//                        .xml("mapper.xml")
//                )
//                .strategyConfig(builder -> builder
//                        .entityBuilder()
//                        .enableLombok()
//                )
//                .templateEngine(new VelocityTemplateEngine())
//                .execute();
//    }
//}