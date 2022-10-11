/**
 *    Copyright 2009-2015 the original author or authors.
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */
package org.apache.ibatis.mapping;

/**
 * Represents the content of a mapped statement read from an XML file or an annotation. 
 * It creates the SQL that will be passed to the database out of the input parameter received from the user.
 *
 * @author Clinton Begin
 *
 * mybatis提供了两种类型的实现org.apache.ibatis.scripting.xmltags.DynamicSqlSource和org.apache.ibatis.scripting.defaults.RawSqlSource。
 *
 * SqlSource是XML文件或者注解方法中映射语句的实现时表示，
 * 通过SqlSourceBuilder.parse()方法创建，
 * SqlSourceBuilder中符号解析器将mybatis中的查询参数#{}转换为?，并记录了参数的顺序。
 * 它只有一个方法getBoundSql用于获取映射语句对象的各个组成部分
 *
 * 代表从XML文件或者注解读取的映射语句的内容,它创建的SQL会被传递给数据库。
 *
 *
 * 根据SQL语句的类型不同，mybatis提供了多种SqlSource的具体实现
 * StaticSqlSource：最终静态SQL语句的封装,其他类型的SqlSource最终都委托给StaticSqlSource。
 * RawSqlSource：原始静态SQL语句的封装,在加载时就已经确定了SQL语句,没有、等动态标签和${} SQL拼接,比动态SQL语句要快,因为不需要运行时解析SQL节点。
 * DynamicSqlSource：动态SQL语句的封装，在运行时需要根据参数处理、等标签或者${} SQL拼接之后才能生成最后要执行的静态SQL语句。
 * ProviderSqlSource：当SQL语句通过指定的类和方法获取时(使用@XXXProvider注解)，需要使用本类，它会通过反射调用相应的方法得到SQL语句。
 */
public interface SqlSource {

  BoundSql getBoundSql(Object parameterObject);

}
