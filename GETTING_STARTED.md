# Getting Started with FastORM Builder

FastORM Builder is an IntelliJ IDEA plugin designed to streamline your development workflow by automatically generating MyBatis ORM code from your database schema. It provides a user-friendly interface to configure and run the MyBatis Generator, creating Java models, mapper interfaces, and XML mapping files directly within your project.

## How to Use

1.  **Open the Tool Window**:
    Navigate to `View -> Tool Windows -> FastORM Builder` to open the main plugin panel.

2.  **Configure Database Connection**:
    In the FastORM Builder panel, set up your database connection:
    *   Select your database vendor (e.g., PostgreSQL, MySQL, Oracle, etc.).
    *   Enter the database URL, username, and password.
    *   The necessary JDBC drivers are already included with the plugin.

3.  **Configure Generation Settings**:
    Specify the details for the code generation:
    *   **Project and Module**: Select the target project and module where the files will be created.
    *   **Package Names**: Define the destination packages for your Java Models, Java Mappers (Interfaces), and XML Mappers.
    *   **Table Selection**: Choose the specific database tables for which you want to generate code.

4.  **Generate Code**:
    Click the **Generate** button. The plugin will connect to your database, process the selected tables, and generate the corresponding files. Progress and logs will be displayed in the output panel.

5.  **Review Your Files**:
    Once the process is complete, the newly generated files (e.g., `User.java`, `UserMapper.java`, and `UserMapper.xml`) will appear in the specified packages within your project's source tree. You can now integrate these ORM classes into your application.
