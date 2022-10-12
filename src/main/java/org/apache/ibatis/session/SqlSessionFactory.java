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
package org.apache.ibatis.session;

import java.sql.Connection;

/**
 * Creates an {@link SqlSesion} out of a connection or a DataSource
 * 
 * @author Clinton Begin
 *
 * 其核心思想就是把config.xml和所有的mapper.xml解析  然后封装到Configuration对象中
 *
 *
 * mybatis通过xml或注解的方式将要执行的各种statement配置起来，
 * 并通过java对象和statement中sql的动态参数进行映射生成最终执行的sql语句，
 * 最后由mybatis框架执行sql并将结果映射为java对象并返回。
 *
 *
 * 主要有多种形式的重载，除了使用默认设置外，可以指定自动提交模式、特定的jdbc连接、事务隔离级别，以及指定的执行器类型。
 * 关于执行器类型，mybatis提供了三种执行器类型：SIMPLE, REUSE, BATCH。
 * 后面我们会详细分析每种类型的执行器的差别以及各自的适用场景。
 * 我们以最简单的无参方法切入（按照一般的套路，如果定义了多个重载的方法或者构造器，内部实现一定是设置作者认为最合适的默认值，然后调用次多参数的方法，直到最后），
 */
public interface SqlSessionFactory {

  SqlSession openSession();

  SqlSession openSession(boolean autoCommit);
  SqlSession openSession(Connection connection);
  SqlSession openSession(TransactionIsolationLevel level);

  SqlSession openSession(ExecutorType execType);
  SqlSession openSession(ExecutorType execType, boolean autoCommit);
  SqlSession openSession(ExecutorType execType, TransactionIsolationLevel level);
  SqlSession openSession(ExecutorType execType, Connection connection);

  Configuration getConfiguration();

}
