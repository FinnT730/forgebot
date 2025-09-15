# Database Migration Guide

This document describes the migration from JSON-based command storage to SQLite database storage.

## What Changed

### Before (JSON-based)
- Commands stored in individual JSON files in `commands/` directory
- User data stored in `userdb.json`
- Custom JSON library (`JsonStructureLib`) used for parsing
- No caching - files read on every command execution

### After (SQLite-based)
- All data stored in `prod.db` SQLite database
- Built-in caching system for improved performance
- Standard SQLite JDBC driver
- Automatic migration from existing JSON files

## Database Schema

### Commands Table
```sql
CREATE TABLE commands (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    name TEXT UNIQUE NOT NULL,
    description TEXT,
    data TEXT NOT NULL,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP
);
```

### Aliases Table
```sql
CREATE TABLE aliases (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    command_id INTEGER NOT NULL,
    alias TEXT UNIQUE NOT NULL,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (command_id) REFERENCES commands (id) ON DELETE CASCADE
);
```

### Users Table
```sql
CREATE TABLE users (
    user_id TEXT PRIMARY KEY,
    paste_site TEXT DEFAULT 'mclogs',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP
);
```

## Migration Process

1. **Automatic Migration**: On first startup, the bot will automatically:
   - Create the SQLite database (`prod.db`)
   - Migrate all existing commands from JSON files
   - Migrate user data from `userdb.json`
   - Load everything into memory cache

2. **Backup**: Original JSON files are preserved (not automatically backed up, but can be manually backed up)

## Performance Improvements

- **Caching**: All commands and aliases are loaded into memory on startup
- **Database Queries**: Optimized SQL queries with proper indexing
- **Reduced I/O**: No more file system reads on every command execution

## Dependencies Added

- `org.xerial:sqlite-jdbc:3.44.1.0` - SQLite JDBC driver
- `com.google.code.gson:gson:2.10.1` - For JSON migration (temporary)

## Files Modified

### Core Database Layer
- `DatabaseManager.java` - New database management class with caching
- `CommandMigrator.java` - Migration utility for JSON to database

### Updated Command Classes
- `CommandCache.java` - Now uses database instead of JSON files
- `RegisterNewCommand.java` - Creates commands in database
- `AliasCommand.java` - Manages aliases in database
- `DeleteCommand.java` - Deletes commands from database
- `DescriptionCommand.java` - Updates command descriptions in database
- `UserDB.java` - Now uses database for user data

### Configuration
- `build.gradle` - Added SQLite and Gson dependencies
- `Main.java` - Added migration initialization

## Usage

The bot will work exactly the same as before from a user perspective. All existing commands will continue to work:

- `!register <name> <content>` - Register new commands
- `!alias <command> <alias1> <alias2>...` - Add aliases to commands
- `!delete <command>` - Delete commands
- `!description <command> "<description>"` - Update command descriptions

## Database File

The database file `prod.db` will be created in the project root directory. This file contains all command and user data and should be backed up regularly.

## Rollback

If you need to rollback to JSON-based storage:
1. Stop the bot
2. Delete `prod.db`
3. Restore JSON files from backup
4. Revert the code changes
5. Restart the bot

## Maintenance

- The database is self-contained in `prod.db`
- No external database server required
- Automatic indexing for optimal performance
- Foreign key constraints ensure data integrity
