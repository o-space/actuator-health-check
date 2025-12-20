# Flyway Database Migration

This project uses [Flyway](https://flywaydb.org/) for database schema version control and migration management.

## Configuration

Flyway is configured in `application.yaml`:

```yaml
spring:
  flyway:
    enabled: true
    locations: classpath:db/migration
    baseline-on-migrate: true
```

- **enabled**: Flyway is enabled for the main application
- **locations**: Migration scripts are located in `src/main/resources/db/migration`
- **baseline-on-migrate**: Automatically creates a baseline for existing databases

## Migration Scripts

### V1__create_health_check_records_table.sql

This initial migration creates the `health_check_records` table with the following structure:

- **id**: BIGSERIAL PRIMARY KEY
- **service_name**: VARCHAR(255) NOT NULL
- **status**: VARCHAR(255) NOT NULL
- **details**: VARCHAR(1000)
- **checked_at**: TIMESTAMP NOT NULL
- **response_time_ms**: BIGINT NOT NULL

The migration also creates indexes for better query performance:

- Index on `service_name`
- Index on `status`
- Index on `checked_at` (DESC)
- Composite index on `service_name` and `status`

## Migration Naming Convention

Flyway migration files follow the naming pattern:

```
V<version>__<description>.sql
```

Example: `V1__create_health_check_records_table.sql`

- **V**: Versioned migration prefix
- **1**: Version number (sequential)
- **__**: Separator (double underscore)
- **Create_health_check_records_table**: Description

## Running Migrations

### Automatic Migration

Migrations run automatically when the Spring Boot application starts, before the application context is fully
initialized.

### Manual Migration

You can also run migrations manually using the Flyway CLI or Maven/Gradle plugins.

## Test Configuration

For unit tests that use H2 in-memory database with `ddl-auto=create-drop`, Flyway is disabled:

```java
@TestPropertySource(properties = {
    "spring.flyway.enabled=false"
})
```

This allows tests to use Hibernate's automatic schema creation instead of Flyway migrations.

## Adding New Migrations

To add a new migration:

1. Create a new SQL file in `src/main/resources/db/migration/`
2. Follow the naming convention: `V<next_version>__<description>.sql`
3. Write the SQL statements for your schema changes
4. The migration will run automatically on the next application startup

### Example: Adding a New Column

Create `V2__Add_created_by_column.sql`:

```sql
ALTER TABLE health_check_records
    ADD COLUMN created_by VARCHAR(255);
```

## Best Practices

1. **Never modify existing migrations**: Once a migration has been applied to a database, do not modify it. Create a new
   migration instead.

2. **Use transactions**: Flyway wraps each migration in a transaction by default (for databases that support it).

3. **Test migrations**: Always test migrations on a copy of production data before applying to production.

4. **Version numbers**: Use sequential version numbers. Flyway will apply migrations in order.

5. **Descriptive names**: Use clear, descriptive names for migration files to understand what each migration does.

## Migration History

Flyway maintains a `flyway_schema_history` table in your database to track which migrations have been applied. This
ensures:

- Migrations are only applied once
- Migrations are applied in the correct order
- You can see the migration history

## Troubleshooting

### Migration Fails on Startup

If a migration fails:

1. Check the error message in the application logs
2. Fix the SQL in the migration file
3. For databases that support it, Flyway will roll back the transaction
4. Fix the issue and restart the application

### Baseline Existing Database

If you have an existing database without Flyway:

- Set `baseline-on-migrate: true` (already configured)
- Flyway will create a baseline and only apply new migrations

### Check Migration Status

You can check which migrations have been applied by querying the `flyway_schema_history` table:

```sql
SELECT *
FROM flyway_schema_history
ORDER BY installed_rank;
```
