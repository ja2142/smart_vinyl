package tk.letsplaybol.smart_vinyl;

import java.io.Closeable;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class CacheDb implements Closeable {
    private static final Logger LOGGER = LogManager.getLogger();

    Connection connection;

    public CacheDb() {
        this(SmartVinyl.MOD_ID + "/cache.db");
    }

    public CacheDb(String dbFilename) {
        try {
            Class.forName("org.sqlite.JDBC");
            connection = DriverManager.getConnection("jdbc:sqlite:" + dbFilename);
        } catch (ClassNotFoundException | SQLException e) {
            LOGGER.error("sqlite connection failed", e);
        }
        createDb();
    }

    public boolean has(String songName) {
        try (PreparedStatement statement = connection
                .prepareStatement("select count(id) from streams where song_name=?");) {
            // omfg why is it counted from 1
            statement.setString(1, songName);
            statement.execute();
            if (statement.getResultSet().getInt(1) == 0) {
                return false;
            }
        } catch (SQLException e) {
            LOGGER.error("retrieving song failed", e);
        }
        updateLastUsed(songName);
        return true;
    }

    public void updateLastUsed(String songName) {
        try (PreparedStatement statement = connection
                .prepareStatement("update streams set last_used=strftime('%s','now') where song_name=?");) {
            statement.setString(1, songName);
            statement.execute();
        } catch (SQLException e) {
            LOGGER.error("updating access time failed", e);
        }
    }

    public List<String> shrinkToFit(int sizeBytes) {
        List<String> toRemove = new ArrayList<String>();
        while (getSize() > sizeBytes) {
            toRemove.add(removeLeastRecentlyUsed());
        }
        return toRemove;
    }

    // size of all recorded cached streams
    public long getSize() {
        try (Statement statement = connection.createStatement();) {
            statement.execute("select sum(size) from streams");
            return statement.getResultSet().getLong(1);
        } catch (SQLException e) {
            LOGGER.error("reading size failed", e);
        }
        return 0;
    }

    public String removeLeastRecentlyUsed() {
        String lruRecord = "";
        try (Statement statement = connection.createStatement();) {
            statement.execute("begin");
            statement.execute("select song_name from streams order by last_used limit 1");
            lruRecord = statement.getResultSet().getString(1);
            statement.execute("delete from streams where id = (select id from streams order by last_used limit 1)");
            statement.execute("commit");
        } catch (SQLException e) {
            LOGGER.error("updating access time failed", e);
        }
        return lruRecord;
    }

    public void add(String songName, int size) {
        try (PreparedStatement statement = connection
                .prepareStatement("insert into streams (song_name, last_used, size) values (?, strftime('%s','now'), ?)");) {
            statement.setString(1, songName);
            statement.setInt(2, size);
            statement.execute();
        } catch (SQLException e) {
            LOGGER.error("adding entry failed", e);
        }
    }

    private void createDb() {
        try (Statement createStatement = connection.createStatement()) {
            createStatement.execute(
                    "create table if not exists streams(id integer primary key autoincrement, song_name text, last_used int, size int)");
        } catch (SQLException e) {
            LOGGER.error("creating db failed", e);
        }
    }

    @Override
    public void close() throws IOException {
        try {
            connection.close();
        } catch (SQLException e) {
            throw new IOException(e);
        }
    }
}
