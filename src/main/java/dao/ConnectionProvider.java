package dao;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class ConnectionProvider {
    private static final String LOCAL_DB_NAME = "detention";
    private static final String LOCAL_USER = "postgres";
    private static final String LOCAL_PASSWORD = "123456";
    private static final String LOCAL_HOST_URL =
            "jdbc:postgresql://localhost:5432/" + LOCAL_DB_NAME + "?sslmode=disable";


    public static Connection getCon() {
        Connection con = null;
        try {
            Class.forName("org.postgresql.Driver");
            con = DriverManager.getConnection(LOCAL_HOST_URL, LOCAL_USER, LOCAL_PASSWORD);
            return con;

        } catch (ClassNotFoundException | SQLException ex) {
            System.err.println("Lỗi nghiêm trọng khi kết nối CSDL Local!");
            ex.printStackTrace();
            return null;
        }
    }
}