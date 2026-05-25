# Project dependencies

- MyBatis Spring Boot starter (Spring Boot 3.x)
  ````xml
  <!-- maven -->
  <dependency>
    <groupId>org.mybatis.spring.boot</groupId>
    <artifactId>mybatis-spring-boot-starter</artifactId>
    <version>3.0.5</version>
  </dependency>
  ````

  ````groovy
  // gradle
  implementation "org.mybatis.spring.boot:mybatis-spring-boot-starter:3.0.5"
   ````

- MyBatis Spring Boot starter (Spring Boot 4.x)
  ````xml
  <!-- maven -->
  <dependency>
    <groupId>org.mybatis.spring.boot</groupId>
    <artifactId>mybatis-spring-boot-starter</artifactId>
    <version>4.0.1</version>
  </dependency>
  ````

  ````groovy
  // gradle
  implementation "org.mybatis.spring.boot:mybatis-spring-boot-starter:4.0.1"
   ````

- MyBatis standalone
  ````xml
  <!-- maven -->
  <dependency>
    <groupId>org.mybatis</groupId>
    <artifactId>mybatis</artifactId>
    <version>3.5.16</version>
  </dependency>
  ````

  ```groovy
  // gradle
  implementation "org.mybatis:mybatis:3.5.16"
  ```

- MyBatis Dynamic SQL
  ````xml
  <!-- maven -->
  <dependency>
    <groupId>org.mybatis.dynamic-sql</groupId>
    <artifactId>mybatis-dynamic-sql</artifactId>
    <version>2.0.0</version>
  </dependency>
  ````

  ```groovy
  // gradle
  implementation "org.mybatis.dynamic-sql:mybatis-dynamic-sql:2.0.0"
  ```

  > **Note:** mybatis-dynamic-sql 2.0.0 requires Java 17+. For Java 8 projects, use the 1.x line.

- MySQL driver
  ```xml
  <!-- maven -->
  <dependency>
    <groupId>com.mysql</groupId>
    <artifactId>mysql-connector-j</artifactId>
    <version>9.7.0</version>
  </dependency>
  ```

  ```groovy
  // gradle
  implementation "com.mysql:mysql-connector-j:9.7.0"
  ```
