package com.memory.xiaohua.utils;

/**
 * @description:
 * @author: xiaohua
 * @date: 2025/2/18,17:27
 */

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.generator.FastAutoGenerator;

import java.util.List;


/**
 * @author mijiupro
 */
public class CodeGenerator {
    public static void main(String[] args) {
        getCode();
    }

    public static void getCode() {
        //TODO 修改为自己的数据库信息
        String url = "jdbc:mysql://localhost:3306/memory_space?useUnicode=true&characterEncoding=UTF-8&useSSL=false&serverTimezone=Asia/Shanghai";
        String username = "root";
        String password = "123456";
        //TODO 修改为自己的表名
        List<String> tables = List.of("similar_picture");//需要生成对应代码的表名的集合

        FastAutoGenerator.create(url, username, password)
                //全局配置----------------------------------------------------------------------------------------
                .globalConfig(builder -> {
                    builder
                            .author("xiaohua")//TODO 修改为自己名称
                            .outputDir(System.getProperty("user.dir") + "\\src\\main\\java")// 输出路径(写到java目录)
                            .enableSwagger() //开启swagger，会自动添加swagger相关注解
                            .commentDate("yyyy-MM-dd");//日期格式


                })

                //包名配置--------------------------------------------------------------------------------------------
                .packageConfig(builder ->
                        builder.parent("com.memory.xiaohua")//TODO 修改为自己项目的路径
                                // .moduleName("practice")//模块名，设置该项会在输出路径上增加一层模块名目录
                                .entity("model.entity")
                                .service("service").serviceImpl("service.impl")
                                .controller("controller")
                                .mapper("mapper")
                                .xml("mapper/xml")
                )

                //策略配置-----------------------------------------------------------------------------------------
                .strategyConfig(builder -> {
                    builder
                            .addInclude(tables)// 需要生成代码对应的表，若需要生成全部表则注释该行解放下一行
                            // .addInclude("all")//生成全部表
                            // .addTablePrefix("p_")//表前缀过滤，例如“p_”开头的表不会生成对应代码

                            //实体策略配置
                            .entityBuilder()
                            .enableFileOverride()// TODO 开启覆盖已生成的entity文件,关闭则注释本行
                            .enableLombok()// 自动添加lombok注解@Getter @Setter
                            .logicDeleteColumnName("deleted")// 指定逻辑删除字段名自动为其添加逻辑删除字段注解
                            .enableTableFieldAnnotation()//启用表字段注解@TableField
//                          .disable()

                            //Mapper策略配置
                            .mapperBuilder()
                            .enableBaseResultMap() // 生成通用的resultMap
                            .superClass(BaseMapper.class)
                            .formatMapperFileName("%sMapper")//mapper文件后缀,如UserMapper
                            // .enableFileOverride()// TODO 开启覆盖已生成的mapper文件,关闭则注释本行
                            .formatXmlFileName("%sMapper")


                            //xml文件后缀,如UserMapper.xml


                            //Service策略配置
                            .serviceBuilder()
                            // .enableFileOverride()//TODO 开启覆盖已生成的service文件,关闭则注释本行
                            .formatServiceFileName("%sService")
                            .formatServiceImplFileName("%sServiceImpl")
                        .disableService()
                          .disableServiceImpl()


                            //Controller策略配置
                            .controllerBuilder()
                            .enableHyphenStyle() // 映射路径使用连字符格式
                            .formatFileName("%sController")
                            // .enableFileOverride()// TODO 开启覆盖已生成的controller文件,关闭则注释本行
                            .enableRestStyle()
                           .disable();//启用rest风格自动添加@RestController


                }).execute();
    }


}