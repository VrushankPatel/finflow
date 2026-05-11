package org.finflow.statement;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication
@EntityScan("org.finflow.statement.domain")
@EnableJpaRepositories("org.finflow.statement.repository")
public class StatementApplication {
    public static void main(String[] args) {
        SpringApplication.run(StatementApplication.class, args);
    }
}
