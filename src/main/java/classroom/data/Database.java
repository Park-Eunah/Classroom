package classroom.data;

import classroom.app.ClassroomRentalApp;
import classroom.model.Application;
import classroom.model.Room;
import classroom.model.RoomManager;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/** PostgreSQL persistence for rental applications and room-manager assignments. */
public final class Database {
    private static final String DATABASE_URL = System.getenv("DATABASE_URL");
    private static final boolean ENABLED = DATABASE_URL != null && !DATABASE_URL.isBlank();

    private Database() { }

    public static boolean isEnabled() { return ENABLED; }

    public static void initialize() {
        if (!ENABLED) {
            System.out.println("DATABASE_URL이 없어 메모리 저장 모드로 실행합니다.");
            return;
        }
        try (Connection connection = connect(); Statement statement = connection.createStatement()) {
            statement.executeUpdate("CREATE TABLE IF NOT EXISTS rental_applications ("
                    + "id INTEGER PRIMARY KEY, applicant_name TEXT NOT NULL, student_id TEXT NOT NULL, "
                    + "room_id INTEGER NOT NULL, room_name TEXT NOT NULL, rental_date DATE NOT NULL, "
                    + "start_time TIME NOT NULL, end_time TIME NOT NULL, purpose TEXT NOT NULL, "
                    + "details TEXT NOT NULL, status TEXT NOT NULL, reason TEXT NOT NULL DEFAULT '')");
            statement.executeUpdate("CREATE TABLE IF NOT EXISTS room_managers ("
                    + "id INTEGER PRIMARY KEY, room_id INTEGER NOT NULL, manager_name TEXT NOT NULL, "
                    + "UNIQUE(room_id, manager_name))");
            statement.executeUpdate("CREATE TABLE IF NOT EXISTS app_settings (setting_key TEXT PRIMARY KEY, setting_value TEXT NOT NULL)");
            seedRoomManagersIfEmpty(connection);
            loadIntoMemory(connection);
            System.out.println("PostgreSQL 연결 및 데이터 로드를 완료했습니다.");
        } catch (Exception e) {
            throw new IllegalStateException("PostgreSQL 초기화에 실패했습니다. DATABASE_URL과 데이터베이스 상태를 확인해 주세요.", e);
        }
    }

    private static void seedRoomManagersIfEmpty(Connection connection) throws SQLException {
        try (PreparedStatement query = connection.prepareStatement(
                "SELECT setting_value FROM app_settings WHERE setting_key = 'default_managers_seeded'" );
             ResultSet result = query.executeQuery()) {
            if (result.next()) return;
        }
        boolean autoCommit = connection.getAutoCommit();
        connection.setAutoCommit(false);
        try {
            try (PreparedStatement insert = connection.prepareStatement(
                    "INSERT INTO room_managers (id, room_id, manager_name) VALUES (?, ?, ?)");
                 PreparedStatement setting = connection.prepareStatement(
                         "INSERT INTO app_settings (setting_key, setting_value) VALUES ('default_managers_seeded', 'true')")) {
                int id = 1;
                for (Room room : ClassroomRentalApp.ROOMS) {
                    insert.setInt(1, id++);
                    insert.setInt(2, room.id);
                    insert.setString(3, "관리자1");
                    insert.addBatch();
                }
                insert.executeBatch();
                setting.executeUpdate();
            }
            connection.commit();
        } catch (SQLException e) {
            connection.rollback();
            throw e;
        } finally {
            connection.setAutoCommit(autoCommit);
        }
    }

    private static void loadIntoMemory(Connection connection) throws SQLException {
        ClassroomRentalApp.APPLICATIONS.clear();
        try (Statement statement = connection.createStatement();
             ResultSet rows = statement.executeQuery("SELECT id, applicant_name, student_id, room_id, room_name, "
                     + "rental_date, start_time, end_time, purpose, details, status, reason "
                     + "FROM rental_applications ORDER BY id")) {
            while (rows.next()) {
                Application application = new Application(rows.getInt("id"), rows.getString("applicant_name"),
                        rows.getString("student_id"), rows.getInt("room_id"), rows.getString("room_name"),
                        rows.getDate("rental_date").toLocalDate().toString(),
                        rows.getTime("start_time").toLocalTime(), rows.getTime("end_time").toLocalTime(),
                        rows.getString("purpose"), rows.getString("details"));
                application.status = rows.getString("status");
                application.reason = rows.getString("reason");
                ClassroomRentalApp.APPLICATIONS.add(application);
                ClassroomRentalApp.nextId = Math.max(ClassroomRentalApp.nextId, application.id + 1);
            }
        }

        for (List<RoomManager> managers : ClassroomRentalApp.ROOM_MANAGERS.values()) managers.clear();
        try (Statement statement = connection.createStatement();
             ResultSet rows = statement.executeQuery("SELECT id, room_id, manager_name FROM room_managers ORDER BY id")) {
            while (rows.next()) {
                int roomId = rows.getInt("room_id");
                List<RoomManager> managers = ClassroomRentalApp.ROOM_MANAGERS.get(roomId);
                if (managers != null) managers.add(new RoomManager(roomId, rows.getString("manager_name"), rows.getInt("id")));
                ClassroomRentalApp.nextRoomManagerId = Math.max(ClassroomRentalApp.nextRoomManagerId, rows.getInt("id") + 1);
            }
        }
    }

    public static void insertApplication(Application application) {
        if (!ENABLED) return;
        execute("INSERT INTO rental_applications (id, applicant_name, student_id, room_id, room_name, rental_date, "
                + "start_time, end_time, purpose, details, status, reason) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                statement -> {
                    statement.setInt(1, application.id);
                    statement.setString(2, application.name);
                    statement.setString(3, application.studentId);
                    statement.setInt(4, application.roomId);
                    statement.setString(5, application.roomName);
                    statement.setObject(6, java.time.LocalDate.parse(application.date));
                    statement.setObject(7, application.start);
                    statement.setObject(8, application.end);
                    statement.setString(9, application.purpose);
                    statement.setString(10, application.details);
                    statement.setString(11, application.status);
                    statement.setString(12, application.reason);
                });
    }

    public static void updateApplication(int id, String status, String reason) {
        if (!ENABLED) return;
        execute("UPDATE rental_applications SET status = ?, reason = ? WHERE id = ?", statement -> {
            statement.setString(1, status);
            statement.setString(2, reason);
            statement.setInt(3, id);
        });
    }

    public static void insertRoomManagers(List<RoomManager> managers) {
        if (!ENABLED) return;
        try (Connection connection = connect(); PreparedStatement statement = connection.prepareStatement(
                "INSERT INTO room_managers (id, room_id, manager_name) VALUES (?, ?, ?)")) {
            connection.setAutoCommit(false);
            try {
                for (RoomManager manager : managers) {
                    statement.setInt(1, manager.id);
                    statement.setInt(2, manager.roomId);
                    statement.setString(3, manager.name);
                    statement.addBatch();
                }
                statement.executeBatch();
                connection.commit();
            } catch (SQLException e) {
                connection.rollback();
                throw e;
            }
        } catch (SQLException e) {
            throw databaseError(e);
        }
    }

    public static void deleteRoomManager(int roomId, int managerId) {
        if (!ENABLED) return;
        execute("DELETE FROM room_managers WHERE room_id = ? AND id = ?", statement -> {
            statement.setInt(1, roomId);
            statement.setInt(2, managerId);
        });
    }

    public static void deleteRoomManagers(String managerName, List<RoomManager> assignments) {
        if (!ENABLED) return;
        Integer[] ids = assignments.stream().map(assignment -> assignment.id).toArray(Integer[]::new);
        execute("DELETE FROM room_managers WHERE manager_name = ? AND id = ANY (?)", statement -> {
            statement.setString(1, managerName);
            statement.setArray(2, statement.getConnection().createArrayOf("integer", ids));
        });
    }

    private static void execute(String sql, SqlBinder binder) {
        try (Connection connection = connect(); PreparedStatement statement = connection.prepareStatement(sql)) {
            binder.bind(statement);
            statement.executeUpdate();
        } catch (SQLException e) {
            throw databaseError(e);
        }
    }

    private static Connection connect() throws SQLException {
        try {
            URI uri = URI.create(DATABASE_URL);
            String scheme = uri.getScheme();
            String jdbcUrl;
            Properties properties = new Properties();
            if (scheme != null && scheme.startsWith("jdbc:")) {
                jdbcUrl = DATABASE_URL;
                String user = System.getenv("DB_USER");
                String password = System.getenv("DB_PASSWORD");
                if (user != null) properties.setProperty("user", user);
                if (password != null) properties.setProperty("password", password);
            } else {
                if (!"postgres".equals(scheme) && !"postgresql".equals(scheme))
                    throw new IllegalArgumentException("DATABASE_URL은 PostgreSQL 주소여야 합니다.");
                String host = uri.getHost();
                if (host == null) throw new IllegalArgumentException("DATABASE_URL의 호스트가 없습니다.");
                jdbcUrl = "jdbc:postgresql://" + host + (uri.getPort() < 0 ? "" : ":" + uri.getPort())
                        + (uri.getRawPath() == null ? "" : uri.getRawPath());
                String query = uri.getRawQuery();
                boolean hasSslMode = query != null && query.toLowerCase().contains("sslmode=");
                jdbcUrl += "?" + (query == null || query.isBlank() ? "sslmode=require"
                        : hasSslMode ? query : query + "&sslmode=require");
                String userInfo = uri.getRawUserInfo();
                if (userInfo != null) {
                    String[] parts = userInfo.split(":", 2);
                    properties.setProperty("user", decodeUserInfo(parts[0]));
                    if (parts.length == 2) properties.setProperty("password", decodeUserInfo(parts[1]));
                }
            }
            return DriverManager.getConnection(jdbcUrl, properties);
        } catch (RuntimeException e) {
            throw new SQLException("DATABASE_URL 형식이 올바르지 않습니다.", e);
        }
    }

    private static String decodeUserInfo(String value) {
        return URLDecoder.decode(value.replace("+", "%2B"), StandardCharsets.UTF_8);
    }

    private static IllegalStateException databaseError(SQLException cause) {
        return new IllegalStateException("데이터베이스 저장에 실패했습니다. 잠시 후 다시 시도해 주세요.", cause);
    }

    @FunctionalInterface
    private interface SqlBinder { void bind(PreparedStatement statement) throws SQLException; }
}
