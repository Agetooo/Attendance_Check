package Controller;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.PasswordField;

public class LoginController {

    @FXML
    private Button btnLogin;
    @FXML
    private TextField txtmail;
    @FXML
    private TextField txtpass;

    @FXML
    public void handlelogin() {
        String mail = txtmail.getText();
        String pass = txtpass.getText();

        // Kiểm tra tạm thời (sau này bạn thêm database vào)
        if (mail.equalsIgnoreCase("admin") && pass.equalsIgnoreCase("123")) {
            try {
                Stage currentStage = (Stage) btnLogin.getScene().getWindow();
                currentStage.close();

                Parent root = FXMLLoader.load(getClass().getResource("/forms/Dashboard.fxml"));
                Stage stage = new Stage();
                stage.setScene(new Scene(root));
                stage.setTitle("Dashboard");
                stage.show();

            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            System.out.println("Sai tài khoản hoặc mật khẩu!");
        }
    }
}
