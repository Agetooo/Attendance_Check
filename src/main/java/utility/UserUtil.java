package utility;

import Model.User;
import dao.UserDao;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

public class UserUtil {

    public static ObservableList<User> loadUserData() {
        return FXCollections.observableArrayList(UserDao.getAllUsers());
    }
}
