package org.apache.ibatis.demo;



import org.apache.log4j.Logger;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

/**
 *  JDBC开发示例代码
 */
public class JDBCTest {
    private static Logger logger = Logger.getLogger(JDBCTest.class);

    public static void main(String[] args) throws Exception {
        // 1、注册驱动
        DriverManager.registerDriver(new com.mysql.jdbc.Driver());
        // 2、建立连接
        Connection con = DriverManager.getConnection("jdbc:mysql://localhost:3306/mybatis_indepth?useUnicode=true&characterEncoding=utf-8&serverTimezone=GMT", "root", "root");
        // 3、编写sql，进行预编译
        String sql = " select * from user;";
        PreparedStatement ps = con.prepareStatement(sql);
        // 4、执行查询，得到结果集
        ResultSet rs = ps.executeQuery();
        while (rs.next()) {
            int id = rs.getInt("id");
            String name = rs.getString("name");

            logger.info("====> id=" + id + "\tname=" + name);

        }
        //5、关闭事务
        rs.close();
        ps.close();
        con.close();
    }
}
