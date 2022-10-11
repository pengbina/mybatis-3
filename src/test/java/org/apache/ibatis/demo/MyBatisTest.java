package org.apache.ibatis.demo;

import org.apache.ibatis.io.Resources;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;

/**
 * 思考：mybatis为我们做了什么？
 *
 * mybatis如何获取数据源连接信息？
 * mybatis如何获取到需要执行的sql语句？
 * mybatis是如何完成sql执行的？
 * mybatis如何完成参数映射和结果封装？
 *
 * 1).接口层
 * 概述：对应 session 模块。
 * 接口层相对简单，其核心是 SqlSession 接口，该接口中定义了 MyBatis 暴露给应用程序调用的 API，
 * 也就是上层应用与 MyBatis 交互的桥梁。
 * 接口层在接收到调用请求时，会调用核心处理层的相应模块来完成具体的数据库操作。
 *
 * 作用：
 * 使用SqlSession接口和Mapper接口通知调用哪个sql还有关联参数。
 * 可以实现数据的增/删/改/查接口 配置信息维护接口，进行动态的更改配置
 *
 *
 * SqlSession源码分析:
 * 作用：Mybatis工作的主要顶层API，表示和数据库交互的会话，完成必要数据库增删改查功能
 *
 * 2）数据处理核心层
 * 在核心处理层中，实现了 MyBatis 的核心处理流程。
 * 其中包括 MyBatis 的初始化以及完成一次数据库操作的涉及的全部流程 。
 *
 * 2.1) 配置解析
 * 概述： 对应 builder 和 mapping 模块。前者为配置解析过程，后者主要为 SQL 操作解析后的映射。
 * 在 MyBatis 初始化过程中，会加载 mybatis-config.xml 配置文件、映射配置文件以及 Mapper 接口中的注解信息，
 * 解析后的配置信息会形成相应的对象并保存到 Configuration 对象中。
 * 利用该 Configuration 对象创建 SqlSessionFactory对象。
 * 待 MyBatis 初始化之后，开发人员可以通过初始化得到 SqlSessionFactory 创建 SqlSession 对象并完成数据库操作。
 *
 * Configuration概述：是一个所有配置信息的容器对象实战分析：Configuration对象涉及到的配置信息分析
 * 简单的理解：MyBatis初始化的过程，就是创建Configuration对象，加载各种配置信息的过程
 *
 * 2.2) SQL解析（SqlSource）
 * 概述： 对应 scripting 模块。
 * MyBatis 中的 scripting 模块，会根据用户传入的实参，解析映射文件中定义的动态 SQL 节点，并形成数据库可执行的 SQL 语句。
 * 之后会处理 SQL 语句中的占位符，绑定用户传入的实参
 * 负责根据用户传递的parameterObject，动态地生成SQL语句，将信息封装到BoundSql对象中，并返回。
 *
 * 各个实现类分析：
 * RawSqlSource 负责处理静态 SQL 语句，它们最终会把处理后的 SQL 封装 StaticSqlSource 进行返回。
 * StaticSqlSource 处理包含的 SQL 可能含有 “？” 占位符，可以被数据库直接执行。
 * DynamicSqlSource 负责处理动态 SQL 语句。
 * ProviderSqlSource 实现 SqlSource 接口，基于方法上的 `@ProviderXXX` 注解的 SqlSource 实现类。
 *
 * 2.3) SQL执行（Executor）
 * 概述：对应 executor 模块
 * 是MyBatis执行器，是MyBatis 调度的核心，负责SQL语句的生成和查询缓存的维护。
 *
 * SQL 语句的执行涉及多个组件 ，其中比较重要的是 Executor、StatementHandler、ParameterHandler 和 ResultSetHandler 。
 *
 * Executor 主要负责维护一级缓存和二级缓存，并提供事务管理的相关操作，它会将数据库相关操作委托给 StatementHandler完成。
 * StatementHandler 首先通过 ParameterHandler 完成 SQL 语句的实参绑定，
 * 然后通过 java.sql.Statement 对象执行 SQL 语句并得到结果集，
 * 最后通过 ResultSetHandler 完成结果集的映射，得到结果对象并返回。
 *
 * 3）基础支撑层：
 * 概述：基础支持层，包含整个 MyBatis 的基础模块，这些模块为核心处理层的功能提供了良好的支撑。
 *
 * 日志：对应 logging 包
 * 概述：Mybatis提供了详细的日志输出信息，还能够集成多种日志框架，其日志模块的主要功能就是集成第三方日志框架。设计模式分析使用的适配器模式分析
 *
 * 缓存机制：对应 cache 包
 * 一级缓存
 * 概述：Session或Statement作用域级别的缓存，默认是Session，
 * BaseExecutor中根据MappedStatement的Id、SQL、参数值以及rowBound(边界)来构造CacheKey，并使用BaseExecutor中的localCache来维护此缓存。
 * 实战应用场景分析 默认开启的缓存
 *
 * 二级缓存
 * 概述：全局的二级缓存，通过CacheExecutor来实现，其委托TransactionalCacheManager来保存/获取缓存
 * 实战应用场景分析 缓存的效率以及应用场景
 * 注意点：两级缓存与Mybatis以及整个应用是运行在同一个JVM中的，共享同一块内存，
 * 如果这两级缓存中的数据量较大，则可能影响系统中其它功能，需要缓存大量数据时，优先考虑使用Redis、Memcache等缓存产品。
 *
 * 数据源/连接池：对应 datasource 包
 * 概述：Mybatis自身提供了相应的数据源实现，也提供了与第三方数据源集成的接口。
 * 分析：主要实现类是PooledDataSource，包含了最大活动连接数、最大空闲连接数、最长取出时间(避免某个线程过度占用)、连接不够时的等待时间。
 * 实战应用：连接池、检测连接状态等，选择性能优秀的数据源组件，对于提供ORM框架以及整个应用的性能都是非常重要的。
 *
 * 事务管理：对应 transaction 包
 * 概述：Mybatis自身对数据库事务进行了抽象，提供了相应的事务接口和简单实现。
 * 注意点：一般地，Mybatis与Spring框架集成，由Spring框架管理事务。
 *
 * 反射：对应 reflection 包
 * 概述：对Java原生的反射进行了很好的封装，提供了简易的API，方便上层调用，并且对反射操作进行了一系列的优化，提高了反射操作的性能。
 * 实战应用
 * ① 缓存了类的元数据（MetaClass）
 * ② 对象的元数据（MetaObject）
 *
 * IO 模块: 对应 io 包。
 * 资源加载模块，主要是对类加载器进行封装，确定类加载器的使用顺序，并提供了加载类文件以及其他资源文件的功能 。
 *
 * 解析器： 对应 parsing 包
 * 解析器模块，主要提供了两个功能:
 * 1.对 XPath 进行封装，为 MyBatis 初始化时解析 mybatis-config.xml 配置文件以及映射配置文件提供支持。
 * 2.为处理动态 SQL 语句中的占位符提供支持。
 *
 *
 * 3.3 MyBatis的核心配置文件解析原理
 * 在 MyBatis 初始化过程中，会加载 mybatis-config.xml 配置文件、映射配置文件以及 Mapper 接口中的注解信息，
 * 解析后的配置信息会形成相应的对象并保存到 Configuration 对象中
 *
 * 1) 解析的目的
 * 概述：通过资源类Resources读入“SqlMapConfig.xml”文件 使用SqlSessionFactoryBuilder类生成我们需要的SqlSessionFactory类。
 * 目的：
 * mybatis解析配置文件最本质的目的是为了获得Configuration对象;
 * 然后，利用该 Configuration 对象创建 SqlSessionFactory对象。待 MyBatis 初始化之后，
 * 可以通过初始化得到 SqlSessionFactory 创建 SqlSession 对象并完成数据库操作。
 *
 * 2) XML 解析流程
 * MyBatis 的初始化流程的入口是 SqlSessionFactoryBuilder 的 build 方法:
 *
 *
 *
 */

public class MyBatisTest {

    InputStream in;
    SqlSessionFactoryBuilder builder;
    SqlSessionFactory factory;
    SqlSession session;
   // UserMapper userMapper = null;
    @Before
    public void init() throws Exception {
        //1.读取配置文件
        in = Resources.getResourceAsStream("SqlMapConfig.xml");
        //2.创建SqlSessionFactory工厂
        builder = new SqlSessionFactoryBuilder();
        factory = builder.build(in);
        //3.使用工厂生产SqlSession对象
        session = factory.openSession();
        //4.使用SqlSession创建Dao接口的代理对象
       // userMapper = session.getMapper(UserMapper.class);
    }

    @After
    public void after() throws IOException {
        //6.释放资源
        session.close();
        in.close();
    }

    /**
     * 入门案例
     */
    @Test
    public void testFindById() throws Exception {
        //5.使用代理对象执行方法
      //  User user = userMapper.findUserById(1);
      //  System.out.println(user);
    }

}
