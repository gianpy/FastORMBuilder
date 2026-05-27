# FastORM Builder

⚡ A powerful, modern JetBrains IDE plugin for rapid ORM code generation from database schemas.

## Supported ORM Frameworks

### Java
- **MyBatis** — Full MyBatis Generator integration with mapper XML, model classes, and DAO interfaces
- **JPA** — Entity generation with `@Entity`, `@Table`, `@Column` annotations and relationship mapping
- **Hibernate** — Entity generation with HBM XML or annotation-based mapping and `hibernate.cfg.xml`
- **YORM** — Java Record generation with convention-based mapping (zero annotations)

### JavaScript / TypeScript
- **Sequelize** — Model definitions with DataTypes, timestamps, field mapping
- **Knex.js** — Migration files with createTable/dropTable
- **Prisma** — Complete schema.prisma with models, datasource, generator
- **TypeORM** — Entity classes with decorators (@Entity, @Column, @PrimaryGeneratedColumn)
- **Bookshelf.js** — Model definitions with tableName and idAttribute
- **Waterline** — Model definitions with identity, attributes, and types
- **Objection.js** — Model classes extending Model with jsonSchema validation
- **MikroORM** — Entity classes with @Entity, @PrimaryKey, @Property decorators

## Features

- **Modern web-based UI** — Fully embedded JCEF interface, no legacy Swing dialogs
- **Language selector** — Switch between Java and JavaScript ORMs with a single click
- **JS/TS toggle** — Generate JavaScript or TypeScript output for all JS ORM frameworks
- **One-click generation** from database tables and views to complete ORM artifacts
- **Multi-database support**: MySQL, PostgreSQL, MariaDB, Oracle, SQLite, DuckDB
- **View support** — Generate ORM code from database views, shown with a distinct icon (👁) in the table list
- **Multi-IDE support**: IntelliJ IDEA, WebStorm, PhpStorm, PyCharm, and all JetBrains IDEs (2024.1+)
- **Overwrite protection** — Warns before overwriting existing generated files
- **Relationship detection** — Auto-generates `@ManyToOne` / `@OneToMany` from foreign keys
- **Lombok integration** with `@Data` annotation support
- **Smart merge** — Preserves custom code in existing MyBatis mapper XML
- **Generation history** — Full audit trail with reproduce capability
- **Copy as Executable SQL** from MyBatis log output
- **Run MyBatis Generator** with official XML configuration files (Java IDEs only)

## Installation

Install from JetBrains Marketplace or build from source:

```bash
./gradlew buildPlugin
```

The plugin ZIP will be in `build/distributions/`.

## Usage

1. Open the **FastORM Builder** tool window (left sidebar)
2. Add a database connection → Test → Connect
3. Select **Java** or **JS** and choose your ORM framework from the dropdown
4. Browse schemas and select tables or views
5. Click **⚡ Generate** — done!

All configuration (connections, defaults, generation settings) is managed directly in the integrated web UI.

## Requirements

- Any JetBrains IDE 2024.1+ (IntelliJ IDEA, WebStorm, PhpStorm, PyCharm, etc.)
- Java 17+ (for running the plugin itself)
- JetBrains Runtime with JCEF support (default in all standard JetBrains distributions)

## License

See [LICENSE](LICENSE) file.
