package Controller;

import dao.ConnectionProvider;
import javafx.fxml.FXML;

import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import utility.DialogUtil;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class UserRegistrationController {
    @FXML
    private ImageView viewImage;
    @FXML
    private TextField txtName;
    @FXML
    private TextField txtEmail;
    @FXML
    private TextField txtContact;
    @FXML
    private TextField txtAddress;

    @FXML
    private Button btnChoosePicture;

    @FXML
    private RadioButton radioMale;
    @FXML
    private RadioButton radioFemale;

    private String avatarPath = null;

    private File file = null;

    @FXML
    private Button btnRegister;


    @FXML
    public void handleRegister() {
        try {
            String name = txtName.getText();

            String email = txtEmail.getText();
            String emailRegex = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$";
            if (!email.matches(emailRegex)) {
                DialogUtil.showError("Invalid","Invalid Email");
                return;
            }
            String contact = txtContact.getText();
            String contactRegex = "^\\d{10,11}$";
            if (!contact.matches(contactRegex)) {
                DialogUtil.showError("Invalid","Invalid Contact");
                return;
            }


            String address = txtAddress.getText();
            if (address.isEmpty() || contact.isEmpty() || name.isEmpty()){
                DialogUtil.showError("Not Enough Information","Please enter all the requested fields");
            }
            String gender = radioMale.isSelected() ? "Male" : "Female";
            if(!radioFemale.isSelected()&&!radioMale.isSelected()){
                DialogUtil.showError("Invalid Gender","Please choose a gender");
                return;
            }
            if (file!=null) {
                File dir = new File("resources/images/");
                if (!dir.exists()) dir.mkdirs();

                // Copy file vào thư mục images
                Path destination = Paths.get("resources/images/" + file.getName());
                Files.copy(file.toPath(), destination, StandardCopyOption.REPLACE_EXISTING);

                avatarPath = destination.toString(); // Lưu đường dẫn tương đối để lưu DB
            }else{
                avatarPath = "resources/images/GuestRender.png";
            }

            try {
                Connection connection= ConnectionProvider.getCon();
                String insertQuery = "INSERT INTO userdetails (name, gender, email, contact, address, avatar_path) VALUES (?,?,?,?,?,?)";
                PreparedStatement preparedStatement = null;
                preparedStatement = connection.prepareStatement(insertQuery);
                preparedStatement.setString(1, name);
                preparedStatement.setString(2, gender);
                preparedStatement.setString(3, email);
                preparedStatement.setString(4, contact);
                preparedStatement.setString(5, address);
                preparedStatement.setString(6, avatarPath);
                preparedStatement.executeUpdate();

                finishAdd();

                DialogUtil.showNotification("Success","Registration Successful");
                Stage currentStage = (Stage) btnRegister.getScene().getWindow();
                currentStage.close();
            } catch (SQLException e) {
                e.printStackTrace();
                DialogUtil.showError("Error","Registration Failed");
            }





        } catch (IOException e) {
            e.printStackTrace();
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
            Image image = new Image(file.toURI().toString());
            viewImage.setImage(image);
        }else {
            Image image = new Image(getClass().getResource("/images/GuestRender.png").toExternalForm());
            viewImage.setImage(image);
        }


    }
    public void finishAdd() {
        ViewUserController parent = UserRegistrationController.getParentController();

        if (parent != null) {
            parent.reloadUserTable();
        }
    }
    private static ViewUserController parentController;

    public static void setParentController(ViewUserController controller) {
        parentController = controller;
    }

    public static ViewUserController getParentController() {
        return parentController;
    }

}
