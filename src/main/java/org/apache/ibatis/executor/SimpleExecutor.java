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
package org.apache.ibatis.executor;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Collections;
import java.util.List;

import org.apache.ibatis.executor.statement.StatementHandler;
import org.apache.ibatis.logging.Log;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.RowBounds;
import org.apache.ibatis.transaction.Transaction;

/**
 * @author Clinton Begin
 */
public class SimpleExecutor extends BaseExecutor {

  public SimpleExecutor(Configuration configuration, Transaction transaction) {
    super(configuration, transaction);
  }

  /**
   * 其中的逻辑可以发现，和selectList的实现非常相似，
   * 先创建语句处理器，
   * 然后创建Statement实例，
   * 最后调用语句处理的update，
   * 语句处理器里面调用jdbc对应update的方法execute()。
   *
   * 和selectList的不同之处在于：
   *
   * 在创建语句处理器期间，会根据需要调用KeyGenerator.processBefore生成前置id；
   * 在执行完成execute()方法后，会根据需要调用KeyGenerator.processAfter生成后置id；
   * 通过分析delete/insert，我们会发现他们内部都委托给update实现了，所以我们就不做重复的分析了。
   *
   * @param ms
   * @param parameter
   * @return
   * @throws SQLException
   */
  @Override
  public int doUpdate(MappedStatement ms, Object parameter) throws SQLException {
    // 终于看到原生jdbc的Statement
    Statement stmt = null;
    try {
      Configuration configuration = ms.getConfiguration();
      StatementHandler handler = configuration.newStatementHandler(this, ms, parameter, RowBounds.DEFAULT, null, null);
      //获取stmt对象
      stmt = prepareStatement(handler, ms.getStatementLog());
      return handler.update(stmt);
    } finally {
      closeStatement(stmt);
    }
  }

  @Override
  public <E> List<E> doQuery(MappedStatement ms, Object parameter, RowBounds rowBounds, ResultHandler resultHandler, BoundSql boundSql) throws SQLException {
    Statement stmt = null;
    try {
      Configuration configuration = ms.getConfiguration();
      // 根据上下文参数和具体的执行器new一个StatementHandler,
      // 其中包含了所有必要的信息,比如结果处理器、参数处理器、执行器等等,
      // 主要有三种类型的语句处理器UNPREPARE、PREPARE、CALLABLE。
      // 默认是PREPARE类型，通过mapper语句上的statementType属性进行设置,一般除了存储过程外不应该设置
      StatementHandler handler = configuration.newStatementHandler(wrapper, ms, parameter, rowBounds, resultHandler, boundSql);
      // 这一步是真正和JDBC打交道
      stmt = prepareStatement(handler, ms.getStatementLog());
      //创建了Statement具体实现的实例后，调用SimpleExecutor.query进行具体的查询
      return handler.<E>query(stmt, resultHandler);
    } finally {
      closeStatement(stmt);
    }
  }

  @Override
  public List<BatchResult> doFlushStatements(boolean isRollback) throws SQLException {
    return Collections.emptyList();
  }

  private Statement prepareStatement(StatementHandler handler, Log statementLog) throws SQLException {
    Statement stmt;
    // 获取链接信息
    // 获取JDBC连接
    Connection connection = getConnection(statementLog);
    //创建一个PreparedStatement返回
    // 调用语句处理器的prepare方法
    stmt = handler.prepare(connection);
    // 设置参数
    handler.parameterize(stmt);
    return stmt;
  }

}
