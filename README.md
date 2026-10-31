# Catalyst

Catalyst is a community aid management system developed as a university software design project.

## Technology Stack

- Java 23
- Maven
- PostgreSQL
- JDBC
- Flyway Database Migrations

## Prerequisites

- JDK 23
- Maven
- Access to the shared PostgreSQL database

## Setup

1. Clone the repository.
2. Open the project in IntelliJ IDEA.
3. Configure the required environment variables:
    - `DB_HOST`
    - `DB_NAME`
    - `DB_USER`
    - `DB_PASSWORD`
4. Reload Maven dependencies.
5. Run `DatabaseConnectionTest` to verify database connectivity.
6. Run `DatabaseMigrationRunner` to apply pending database migrations.

## Database

The project uses PostgreSQL hosted on Neon.

Database schema changes are managed through Flyway migrations located in:

`src/main/resources/db/migration/`

Do not modify an already-applied migration. Create a new versioned migration for subsequent schema changes.

