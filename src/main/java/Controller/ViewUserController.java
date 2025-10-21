package Controller;

import Model.User;
import dao.ConnectionProvider;
import dao.UserDao;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.Stage;
import utility.DialogUtil;
import utility.UserUtil;

import java.io.File;
import java.net.URL;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.ResourceBundle;

public class ViewUserController implements Initializable {

    @FXML
    private TableView<User> tableView;

    @FXML
    private TableColumn<User, Integer> colId;

    @FXML
    private TableColumn<User, String> colName;

    @FXML
    private TableColumn<User, String> colGender;

    @FXML
    private TableColumn<User, String> colEmail;

    @FXML
    private TableColumn<User, String> colContact;

    @FXML
    private TableColumn<User, String> colAddress;

    @FXML
    private ImageView viewImage;
    @FXML
    private TextField txtSearch;

    @FXML
    private Button btnAddUser;

    @FXML
    private Button btnDeleteUser;

    @FXML
    private Button btnUpdateUser;

    private ObservableList<User> userList;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // 1. Gán dữ liệu cho các cột
        colId.setCellValueFactory(new PropertyValueFactory<>("displayId"));
        colName.setCellValueFactory(new PropertyValueFactory<>("name"));
        colGender.setCellValueFactory(new PropertyValueFactory<>("gender"));
        colEmail.setCellValueFactory(new PropertyValueFactory<>("email"));
        colContact.setCellValueFactory(new PropertyValueFactory<>("contact"));
        colAddress.setCellValueFactory(new PropertyValueFactory<>("address"));


        userList = FXCollections.observableArrayList(UserDao.getAllUsers());

        FilteredList<User> filteredData = new FilteredList<>(userList, p -> true);

        tableView.setItems(filteredData);

        // Lắng nghe thay đổi ở ô tìm kiếm
        txtSearch.textProperty().addListener((observable, oldValue, newValue) -> {
            filteredData.setPredicate(user -> {
                // Nếu ô trống -> hiện tất cả
                if (newValue == null || newValue.isEmpty()) {
                    return true;
                }

                String keyword = newValue.toLowerCase();
                return user.getName().toLowerCase().contains(keyword);
            });
        });

        // Đưa FilteredList vào TableView
        tableView.setItems(filteredData);


        // 3. Set ảnh mặc định
        try {
            Image defaultImage = new Image(getClass().getResourceAsStream("/images/GuestRender.png"));
            viewImage.setImage(defaultImage);
        } catch (Exception e) {
            System.out.println("Không tìm thấy ảnh mặc định!");
        }

        tableView.getSelectionModel().selectedItemProperty().addListener((obs, oldUser, selectedUser) -> {
            if (selectedUser != null) {
                try {
                    String path = selectedUser.getAvatarPath(); // hoặc getImagePath()

                    File file = new File(path);
                    if (file.exists()) {
                        Image userImage = new Image(file.toURI().toString());
                        viewImage.setImage(userImage);
                    } else { // Nếu không tìm thấy → ảnh mặc định
                        Image defaultImage = new Image(getClass().getResourceAsStream("/images/GuestRender.png"));
                        viewImage.setImage(defaultImage);
                    }

                } catch (Exception e) {
                    try {
                        Image defaultImage = new Image(getClass().getResourceAsStream("/images/GuestRender.png"));
                        viewImage.setImage(defaultImage);
                    } catch (Exception ex) {
                        System.out.println("Không tìm thấy ảnh mặc định!");
                    }
                }
            }
        });

    }

    @FXML
    private void handleUpdateUser() {
        User selectedUser = tableView.getSelectionModel().getSelectedItem();
        if (selectedUser == null) {
            System.out.println("Vui lòng chọn user!");
            return;
        }

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/forms/UserUpdate.fxml"));
            Parent root = loader.load();

            UserUpdateController updateController = loader.getController();

            updateController.setUserData(selectedUser);

            UserUpdateController.setParentController(this);


            Stage stage = new Stage();
            stage.setTitle("User Update");
            stage.setScene(new Scene(root));
            stage.show();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    public void handleAddUser() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/forms/UserRegistration.fxml"));
            Parent root = loader.load();

            UserRegistrationController.setParentController(this);


            Stage stage = new Stage();
            stage.setTitle("User Adder");
            stage.setScene(new Scene(root));
            stage.show();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    public void handleDeleteUser() {
        User selectedUser = tableView.getSelectionModel().getSelectedItem();
        if (selectedUser == null) {
            System.out.println("Vui lòng chọn user!");
            return;
        }
        String selectedEmail = selectedUser.getEmail();
        if (DialogUtil.showConfirmation("Xác nhận", "Bạn có chắc muốn xóa không?")) {
            try{
                Connection connection = ConnectionProvider.getCon();
                String deleteuser = "DELETE FROM userdetails WHERE email =?";
                PreparedStatement ps = connection.prepareStatement(deleteuser);
                ps.setString(1, selectedEmail);
                ps.executeUpdate();
                reloadUserTable();
            }catch(Exception e){
                e.printStackTrace();
            }
        } else {
        }

    }
    public void reloadUserTable() {
        ObservableList<User> users = FXCollections.observableArrayList(UserDao.getAllUsers());
        tableView.setItems(users);
        tableView.refresh();
    }

    private static ViewUserController parentController;

    public static void setParentController(ViewUserController controller) {
        parentController = controller;
        }
        public static ViewUserController getParentController() {
            return parentController;
    }



}
