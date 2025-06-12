package com.aireader.backend.util;

import org.neo4j.driver.AuthTokens;
import org.neo4j.driver.Driver;
import org.neo4j.driver.GraphDatabase;
import org.neo4j.driver.Session;
import org.neo4j.driver.Result;
import org.neo4j.driver.Transaction;

public class Neo4jConnectionTest {

    public static void main(String[] args) {
        // Neo4j连接参数
        String uri = "bolt://localhost:7687";
        String username = "neo4j";
        String password = "ZZQ123456zzq";

        // 创建驱动
        try (Driver driver = GraphDatabase.driver(uri, AuthTokens.basic(username, password))) {
            // 验证连接
            driver.verifyConnectivity();
            System.out.println("成功连接到Neo4j数据库!");

            // 执行简单查询
            try (Session session = driver.session()) {
                Result result = session.run("MATCH (n) RETURN count(n) AS count");
                System.out.println("数据库中节点数量: " + result.single().get("count").asInt());
            }
        } catch (Exception e) {
            System.err.println("Neo4j连接失败: " + e.getMessage());
            e.printStackTrace();
        }
    }
} 