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
package org.apache.ibatis.binding;

import org.apache.ibatis.builder.annotation.MapperAnnotationBuilder;
import org.apache.ibatis.io.ResolverUtil;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.SqlSession;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * @author Clinton Begin
 * @author Eduardo Macarron
 * @author Lasse Voss
 */
public class MapperRegistry {

  private final Configuration config;
  //knownMappers是MapperRegistry的主要字段，维护了Mapper接口和代理类的映射关系,key是mapper接口类，value是MapperProxyFactory
  private final Map<Class<?>, MapperProxyFactory<?>> knownMappers = new HashMap<Class<?>, MapperProxyFactory<?>>();

  public MapperRegistry(Configuration config) {
    this.config = config;
  }

  /**
   * 可以看到我们是从 Configuration对象中的MapperRegistry对象通过类对象作为key获取MapperProxyFactory
   * 然后通过jdk的动态代理生成代理对象 （这里也就解释了为什么我们要创建一个Mapper接口而不是实体类）
   * 里面的addMapper()方法是不是似曾相识。
   *
   *
   * MapperRegistry又将创建代理的任务委托给MapperProxyFactory，
   * MapperProxyFactory首先为Mapper接口创建了一个实现了InvocationHandler方法调用处理器接口的代理类MapperProxy，
   * 并实现invoke接口（其中为mapper各方法执行sql的具体逻辑），
   * 最后才调用JDK的java.lang.reflect.Proxy为Mapper接口创建动态代理类并返回
   *
   * 这样当我们应用层执行List users = mapper.getUser2(293);的时候，JVM会首先调用MapperProxy.invoke
   */
  @SuppressWarnings("unchecked")
  public <T> T getMapper(Class<T> type, SqlSession sqlSession) {
    final MapperProxyFactory<T> mapperProxyFactory = (MapperProxyFactory<T>) knownMappers.get(type);
    if (mapperProxyFactory == null) {
      throw new BindingException("Type " + type + " is not known to the MapperRegistry.");
    }
    try {
      //创建一个代理对象
      return mapperProxyFactory.newInstance(sqlSession);
    } catch (Exception e) {
      throw new BindingException("Error getting mapper instance. Cause: " + e, e);
    }
  }
  
  public <T> boolean hasMapper(Class<T> type) {
    return knownMappers.containsKey(type);
  }

  public <T> void addMapper(Class<T> type) {
    // 对于mybatis mapper接口文件，必须是interface，不能是class
    if (type.isInterface()) {
      // 判重，确保只会加载一次不会被覆盖
      if (hasMapper(type)) {
        throw new BindingException("Type " + type + " is already known to the MapperRegistry.");
      }
      boolean loadCompleted = false;
      try {
        // 为mapper接口创建一个MapperProxyFactory代理
        // private final Map<Class<?>, MapperProxyFactory<?>> knownMappers = new HashMap<>();
        // 最终这里是把mapper类对象作为key，MapperProxyFactory对象作为value加入到map里面，方
        // 便后面获取通过动态代理生成Mapper的代理对象
        knownMappers.put(type, new MapperProxyFactory<T>(type));
        // It's important that the type is added before the parser is run
        // otherwise the binding may automatically be attempted by the
        // mapper parser. If the type is already known, it won't try.
        MapperAnnotationBuilder parser = new MapperAnnotationBuilder(config, type);
        parser.parse();
        loadCompleted = true;
      } finally {
        //剔除解析出现异常的接口
        if (!loadCompleted) {
          knownMappers.remove(type);
        }
      }
    }
  }

  /**
   * @since 3.2.2
   */
  public Collection<Class<?>> getMappers() {
    return Collections.unmodifiableCollection(knownMappers.keySet());
  }

  /**
   * @since 3.2.2
   */
  public void addMappers(String packageName, Class<?> superType) {
    // mybatis框架提供的搜索classpath下指定package以及子package中符合条件(注解或者继承于某个类/接口)的类，
    // 默认使用Thread.currentThread().getContextClassLoader()返回的加载器,和spring的工具类殊途同归。
    ResolverUtil<Class<?>> resolverUtil = new ResolverUtil<Class<?>>();
    // 无条件的加载所有的类,因为调用方传递了Object.class作为父类,这也给以后的指定mapper接口预留了余地
    resolverUtil.find(new ResolverUtil.IsA(superType), packageName);
    // 所有匹配的calss都被存储在ResolverUtil.matches字段中
    Set<Class<? extends Class<?>>> mapperSet = resolverUtil.getClasses();
    for (Class<?> mapperClass : mapperSet) {
      //调用addMapper方法进行具体的mapper类/接口解析
      addMapper(mapperClass);
    }
  }

  /**
   * @since 3.2.2
   */
  public void addMappers(String packageName) {
    addMappers(packageName, Object.class);
  }
  
}
