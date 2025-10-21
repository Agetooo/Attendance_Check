package dao;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;

public class ConnectionProvider {
    private static final String DB_PASSWORD_ENV_VAR = "AIVEN_DB_PASSWORD";

    private static final String AIVEN_PASSWORD = null;

    private static final String TARGET_DB_NAME = "defaultdb";
    private static final String AIVEN_USER = "avnadmin";


    private static final String AIVEN_HOST_URL =
            "jdbc:postgresql://detention-detention2025.g.aivencloud.com:12359/defaultdb?sslmode=require";

    public static Connection getCon() {
        if (AIVEN_PASSWORD == null || AIVEN_PASSWORD.isEmpty()) {
            System.err.println("Lỗi: Không tìm thấy mật khẩu. Vui lòng thiết lập biến môi trường " + DB_PASSWORD_ENV_VAR);
            return null;
        }

        String fullAivenUrl = AIVEN_HOST_URL + "&user=" + AIVEN_USER + "&password=" + AIVEN_PASSWORD;

        Connection con = null;
        try {
            Class.forName("org.postgresql.Driver");

            con = DriverManager.getConnection(fullAivenUrl);



            String finalUrl = fullAivenUrl.replace("/defaultdb", "/" + TARGET_DB_NAME);
            if (con != null) {
                con.close();
            }
            con = DriverManager.getConnection(finalUrl);

            return con;
        } catch (Exception ex) {
            ex.printStackTrace();
            return null;
        }
    }

    private static boolean databaseExists(Connection con, String databaseName) throws Exception {
        Statement stmt = con.createStatement();
        String query = "SELECT 1 FROM pg_database WHERE datname = '" + databaseName + "'";
        return stmt.executeQuery(query).next();
    }

    private static void createDatabase(Connection con, String databaseName) throws Exception {
        Statement stmt = con.createStatement();
        String query = "CREATE DATABASE " + databaseName;
        stmt.executeUpdate(query);
    }
}