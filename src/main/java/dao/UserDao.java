package dao;

import Model.User;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class UserDao {

    public static List<User> getAllUsers() {
        List<User> users = new ArrayList<>();

        try (Connection con = ConnectionProvider.getCon();
             Statement stmt = con.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT * FROM userdetails")) {

            while (rs.next()) {
                users.add(new User(
                        rs.getInt("id"),
                        rs.getString("name"),
                        rs.getString("gender"),
                        rs.getString("email"),
                        rs.getString("contact"),
                        rs.getString("address"),
                        rs.getString("avatar_path")
                ));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return users;
    }
    public User findUserByEmail(String email) {
        String query = "SELECT * FROM userdetails WHERE email = ?";
        try (Connection conn = ConnectionProvider.getCon();
             PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setString(1, email);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                return new User(
                        rs.getInt("id"),
                        rs.getString("name"),
                        rs.getString("gender"),
                        rs.getString("email"),
                        rs.getString("contact"),
                        rs.getString("address"),
                        rs.getString("avatar_path")
                );
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}
