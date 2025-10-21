package Controller;

import Model.User;
import dao.ConnectionProvider;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class UserUpdateController {

    @FXML
    private ImageView viewImage;

    @FXML
    private TextField txtName;

    @FXML
    private TextField txtContact;

    @FXML
    private TextField txtAddress;

    @FXML
    private RadioButton radioMale;

    @FXML
    private RadioButton radioFemale;

    @FXML
    private Button btnChoosePicture;

    @FXML
    private Button btnUpdate;

    private static ViewUserController parentController;

    private File file;
    private User currentUser;


    public void setUserData(User user) {
        this.currentUser = user;

        txtName.setText(user.getName());
        txtContact.setText(user.getContact());
        txtAddress.setText(user.getAddress());

        if ("Male".equalsIgnoreCase(user.getGender())) {
            radioMale.setSelected(true);
        } else {
            radioFemale.setSelected(true);
        }

        // Load avatar
        String avatarPath = user.getAvatarPath();
        if (avatarPath != null) {
            File avatarFile = new File(avatarPath);
            if (avatarFile.exists()) {
                viewImage.setImage(new Image(avatarFile.toURI().toString()));
            } else {
                viewImage.setImage(new Image(getClass().getResource("/images/GuestRender.png").toExternalForm()));
            }
        }
    }

    @FXML
    public void handleChoosePicture() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Chọn hình ảnh");
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg", "*.gif")
        );

        file = fileChooser.showOpenDialog(null);
        if (file != null) {
            viewImage.setImage(new Image(file.toURI().toString()));
        }
    }

    @FXML
    public void handleUpdate() {
        if (currentUser == null) return; // Phòng lỗi

        String name = txtName.getText();
        String contact = txtContact.getText();
        String address = txtAddress.getText();
        String gender = radioMale.isSelected() ? "Male" : "Female";
        String avatarPath = currentUser.getAvatarPath(); // giữ ảnh cũ nếu không chọn

        if (file != null) {
            try {
                File dir = new File("images");
                if (!dir.exists()) dir.mkdirs();

                Path destination = Paths.get("images/" + file.getName());
                Files.copy(file.toPath(), destination, StandardCopyOption.REPLACE_EXISTING);

                avatarPath = destination.toString();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        try {
            Connection connection = ConnectionProvider.getCon();
            String updateQuery = "UPDATE userdetails " +
                    "SET name=?, contact=?, gender=?, address=?, avatar_path=? WHERE email=?";
            PreparedStatement ps = connection.prepareStatement(updateQuery);
            ps.setString(1, name);
            ps.setString(2, contact);
            ps.setString(3, gender);
            ps.setString(4, address);
            ps.setString(5, avatarPath);
            ps.setString(6, currentUser.getEmail());
            ps.executeUpdate();

            if (UserUpdateController.getParentController() != null) {
                UserUpdateController.getParentController().reloadUserTable();
            }


            Stage currentStage = (Stage) btnUpdate.getScene().getWindow();
            currentStage.close();


        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    public void finishUpdate() {
        ViewUserController parent = ViewUserController.getParentController();
        if (parent != null) {
            parent.reloadUserTable();
        }
    }
    public static void setParentController(ViewUserController controller) {
        parentController = controller;
    }

    public static ViewUserController getParentController() {
        return parentController;
    }

}
