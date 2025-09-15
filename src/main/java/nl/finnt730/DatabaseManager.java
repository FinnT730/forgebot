package nl.finnt730;

import java.sql.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Database manager for handling SQLite operations with caching
 */
public class DatabaseManager {
    private static final String DB_URL = "jdbc:sqlite:prod.db";
    private static final String CREATE_COMMANDS_TABLE = """
        CREATE TABLE IF NOT EXISTS commands (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            name TEXT UNIQUE NOT NULL,
            description TEXT,
            data TEXT NOT NULL,
            created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
            updated_at DATETIME DEFAULT CURRENT_TIMESTAMP
        )
        """;
    
    private static final String CREATE_ALIASES_TABLE = """
        CREATE TABLE IF NOT EXISTS aliases (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            command_id INTEGER NOT NULL,
            alias TEXT UNIQUE NOT NULL,
            created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
            FOREIGN KEY (command_id) REFERENCES commands (id) ON DELETE CASCADE
        )
        """;
    
    private static final String CREATE_USERS_TABLE = """
        CREATE TABLE IF NOT EXISTS users (
            user_id TEXT PRIMARY KEY,
            paste_site TEXT DEFAULT 'mclogs',
            created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
            updated_at DATETIME DEFAULT CURRENT_TIMESTAMP
        )
        """;
    
    private static final String CREATE_INDEXES = """
        CREATE INDEX IF NOT EXISTS idx_commands_name ON commands(name);
        CREATE INDEX IF NOT EXISTS idx_aliases_alias ON aliases(alias);
        CREATE INDEX IF NOT EXISTS idx_aliases_command_id ON aliases(command_id);
        CREATE INDEX IF NOT EXISTS idx_users_user_id ON users(user_id);
        """;
    
    private static DatabaseManager instance;
    private final Map<String, CommandData> commandCache = new ConcurrentHashMap<>();
    private final Map<String, String> aliasToCommandCache = new ConcurrentHashMap<>();
    private final Set<String> realCommands = ConcurrentHashMap.newKeySet();
    private final Set<String> observedAliases = ConcurrentHashMap.newKeySet();
    
    private DatabaseManager() {
        initializeDatabase();
        loadAllCommandsIntoCache();
    }
    
    public static synchronized DatabaseManager getInstance() {
        if (instance == null) {
            instance = new DatabaseManager();
        }
        return instance;
    }
    
    private void initializeDatabase() {
        try (Connection conn = DriverManager.getConnection(DB_URL)) {
            try (Statement stmt = conn.createStatement()) {
                stmt.execute(CREATE_COMMANDS_TABLE);
                stmt.execute(CREATE_ALIASES_TABLE);
                stmt.execute(CREATE_USERS_TABLE);
                stmt.execute(CREATE_INDEXES);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to initialize database", e);
        }
    }
    
    private void loadAllCommandsIntoCache() {
        try (Connection conn = DriverManager.getConnection(DB_URL)) {
            String sql = """
                SELECT c.id, c.name, c.description, c.data, 
                       GROUP_CONCAT(a.alias) as aliases
                FROM commands c
                LEFT JOIN aliases a ON c.id = a.command_id
                GROUP BY c.id, c.name, c.description, c.data
                """;
            
            try (PreparedStatement stmt = conn.prepareStatement(sql);
                 ResultSet rs = stmt.executeQuery()) {
                
                while (rs.next()) {
                    String name = rs.getString("name");
                    String description = rs.getString("description");
                    String data = rs.getString("data");
                    String aliasesStr = rs.getString("aliases");
                    
                    List<String> aliases = new ArrayList<>();
                    if (aliasesStr != null && !aliasesStr.isEmpty()) {
                        aliases.addAll(Arrays.asList(aliasesStr.split(",")));
                    }
                    
                    CommandData cmdData = new CommandData(name, description, data, aliases);
                    commandCache.put(name, cmdData);
                    realCommands.add(name);
                    
                    // Cache aliases
                    for (String alias : aliases) {
                        aliasToCommandCache.put(alias, name);
                        observedAliases.add(alias);
                    }
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to load commands into cache", e);
        }
    }
    
    public boolean commandExists(String commandName) {
        return realCommands.contains(commandName) || commandCache.containsKey(commandName);
    }
    
    public boolean aliasExists(String alias) {
        return observedAliases.contains(alias) || aliasToCommandCache.containsKey(alias);
    }
    
    public boolean isTakenAlias(String alias) {
        return aliasExists(alias);
    }
    
    public Optional<CommandData> getCommand(String commandName) {
        return Optional.ofNullable(commandCache.get(commandName));
    }
    
    public Optional<CommandData> getCommandByAlias(String alias) {
        String commandName = aliasToCommandCache.get(alias);
        if (commandName != null) {
            return getCommand(commandName);
        }
        return Optional.empty();
    }
    
    public void createCommand(String name, String description, String data, List<String> aliases) {
        try (Connection conn = DriverManager.getConnection(DB_URL)) {
            conn.setAutoCommit(false);
            
            try {
                // Insert command
                String insertCommandSql = "INSERT INTO commands (name, description, data) VALUES (?, ?, ?)";
                try (PreparedStatement stmt = conn.prepareStatement(insertCommandSql)) {
                    stmt.setString(1, name);
                    stmt.setString(2, description);
                    stmt.setString(3, data);
                    stmt.executeUpdate();
                }
                
                // Get the command ID using last_insert_rowid() - SQLite specific
                long commandId;
                String getCommandIdSql = "SELECT last_insert_rowid()";
                try (PreparedStatement stmt = conn.prepareStatement(getCommandIdSql);
                     ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        commandId = rs.getLong(1);
                    } else {
                        throw new SQLException("Failed to get command ID");
                    }
                }
                
                // Insert aliases
                if (!aliases.isEmpty()) {
                    String insertAliasSql = "INSERT INTO aliases (command_id, alias) VALUES (?, ?)";
                    try (PreparedStatement aliasStmt = conn.prepareStatement(insertAliasSql)) {
                        for (String alias : aliases) {
                            aliasStmt.setLong(1, commandId);
                            aliasStmt.setString(2, alias);
                            aliasStmt.addBatch();
                        }
                        aliasStmt.executeBatch();
                    }
                }
                
                conn.commit();
                
                // Update cache
                CommandData cmdData = new CommandData(name, description, data, new ArrayList<>(aliases));
                commandCache.put(name, cmdData);
                realCommands.add(name);
                
                for (String alias : aliases) {
                    aliasToCommandCache.put(alias, name);
                    observedAliases.add(alias);
                }
                
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (SQLException e) {
            System.err.println("SQL Error creating command '" + name + "': " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Failed to create command", e);
        }
    }
    
    public void updateCommand(String name, String description, String data, List<String> aliases) {
        try (Connection conn = DriverManager.getConnection(DB_URL)) {
            conn.setAutoCommit(false);
            
            try {
                // Update command
                String updateCommandSql = "UPDATE commands SET description = ?, data = ?, updated_at = CURRENT_TIMESTAMP WHERE name = ?";
                try (PreparedStatement stmt = conn.prepareStatement(updateCommandSql)) {
                    stmt.setString(1, description);
                    stmt.setString(2, data);
                    stmt.setString(3, name);
                    stmt.executeUpdate();
                }
                
                // Get command ID
                long commandId;
                String getCommandIdSql = "SELECT id FROM commands WHERE name = ?";
                try (PreparedStatement stmt = conn.prepareStatement(getCommandIdSql)) {
                    stmt.setString(1, name);
                    try (ResultSet rs = stmt.executeQuery()) {
                        if (rs.next()) {
                            commandId = rs.getLong("id");
                        } else {
                            throw new SQLException("Command not found");
                        }
                    }
                }
                
                // Delete existing aliases
                String deleteAliasesSql = "DELETE FROM aliases WHERE command_id = ?";
                try (PreparedStatement stmt = conn.prepareStatement(deleteAliasesSql)) {
                    stmt.setLong(1, commandId);
                    stmt.executeUpdate();
                }
                
                // Insert new aliases
                if (!aliases.isEmpty()) {
                    String insertAliasSql = "INSERT INTO aliases (command_id, alias) VALUES (?, ?)";
                    try (PreparedStatement stmt = conn.prepareStatement(insertAliasSql)) {
                        for (String alias : aliases) {
                            stmt.setLong(1, commandId);
                            stmt.setString(2, alias);
                            stmt.addBatch();
                        }
                        stmt.executeBatch();
                    }
                }
                
                conn.commit();
                
                // Update cache
                CommandData cmdData = new CommandData(name, description, data, new ArrayList<>(aliases));
                commandCache.put(name, cmdData);
                
                // Remove old aliases from cache
                CommandData oldData = commandCache.get(name);
                if (oldData != null) {
                    for (String oldAlias : oldData.aliases()) {
                        aliasToCommandCache.remove(oldAlias);
                        observedAliases.remove(oldAlias);
                    }
                }
                
                // Add new aliases to cache
                for (String alias : aliases) {
                    aliasToCommandCache.put(alias, name);
                    observedAliases.add(alias);
                }
                
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to update command", e);
        }
    }
    
    public void deleteCommand(String name) {
        try (Connection conn = DriverManager.getConnection(DB_URL)) {
            // Get command data first to clean up cache
            CommandData cmdData = commandCache.get(name);
            
            String deleteCommandSql = "DELETE FROM commands WHERE name = ?";
            try (PreparedStatement stmt = conn.prepareStatement(deleteCommandSql)) {
                stmt.setString(1, name);
                int rowsAffected = stmt.executeUpdate();
                
                if (rowsAffected > 0) {
                    // Clean up cache
                    commandCache.remove(name);
                    realCommands.remove(name);
                    
                    if (cmdData != null) {
                        for (String alias : cmdData.aliases()) {
                            aliasToCommandCache.remove(alias);
                            observedAliases.remove(alias);
                        }
                    }
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to delete command", e);
        }
    }
    
    public void addAliases(String commandName, List<String> newAliases) {
        CommandData existingData = commandCache.get(commandName);
        if (existingData == null) {
            throw new IllegalArgumentException("Command not found: " + commandName);
        }
        
        List<String> updatedAliases = new ArrayList<>(existingData.aliases());
        updatedAliases.addAll(newAliases);
        
        updateCommand(commandName, existingData.description(), existingData.data(), updatedAliases);
    }
    
    public void invalidateCache(String commandName) {
        commandCache.remove(commandName);
        realCommands.remove(commandName);
        observedAliases.remove(commandName);
        aliasToCommandCache.remove(commandName);
    }
    
    public void addObservedAlias(String alias) {
        observedAliases.add(alias);
    }
    
    public Set<String> getAllCommandNames() {
        return new HashSet<>(realCommands);
    }
    
    public Set<String> getAllAliases() {
        return new HashSet<>(observedAliases);
    }
    
    // User management methods
    public String getUserPasteSite(String userId) {
        try (Connection conn = DriverManager.getConnection(DB_URL)) {
            String sql = "SELECT paste_site FROM users WHERE user_id = ?";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, userId);
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        return rs.getString("paste_site");
                    }
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to get user paste site", e);
        }
        return "mclogs"; // default
    }
    
    public void setUserPasteSite(String userId, String pasteSite) {
        try (Connection conn = DriverManager.getConnection(DB_URL)) {
            String sql = """
                INSERT INTO users (user_id, paste_site) VALUES (?, ?)
                ON CONFLICT(user_id) DO UPDATE SET 
                    paste_site = excluded.paste_site,
                    updated_at = CURRENT_TIMESTAMP
                """;
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, userId);
                stmt.setString(2, pasteSite);
                stmt.executeUpdate();
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to set user paste site", e);
        }
    }
    
    /**
     * Data class representing a command
     */
    public record CommandData(String name, String description, String data, List<String> aliases) {}
}
