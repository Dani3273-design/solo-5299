package com.example;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class SqliteDemoApplication {

    public static void main(String[] args) {
        SpringApplication.run(SqliteDemoApplication.class, args);
        System.out.println("========================================");
        System.out.println("  SQLite Demo Application Started!");
        System.out.println("========================================");
        System.out.println("Available APIs:");
        System.out.println("  Student API: http://localhost:8080/api/students");
        System.out.println("  Score API:   http://localhost:8080/api/scores");
        System.out.println("========================================");
    }
}
