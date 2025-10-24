package Controller;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.stage.Stage;

import java.awt.event.ActionEvent;
import java.io.IOException;

public class DashboardController {
    @FXML
    private Button btnRegisterUser;
    @FXML
    private Button btnViewUser;

    @FXML
    public void handleUserRegister() throws IOException {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/forms/UserRegistration.fxml"));
            Stage stage = new Stage();
            stage.setScene(new Scene(root));
            stage.setTitle("User Registration");
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    public void handleViewUser() throws IOException {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/forms/ViewUser.fxml"));
            Stage stage = new Stage();
            stage.setScene(new Scene(root));
            stage.setTitle("View User");
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    public void handleUpdateUser() throws IOException{
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/forms/UserBox.fxml"));
            Stage stage = new Stage();
            stage.setScene(new Scene(root));
            stage.setTitle("Find User to Update");
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    public void handleGenerateQR() throws IOException{
        try{
            Parent root = FXMLLoader.load(getClass().getResource("/forms/GenerateQR.fxml"));
            Stage stage = new Stage();
            stage.setScene(new Scene(root));
            stage.setTitle("Generate QR");
            stage.show();
        }catch (Exception e){
            e.printStackTrace();
        }
    }
    public void handleMarkAttendance() throws IOException{
        try{
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/forms/MarkAttendance.fxml"));
            Parent root = loader.load();
            Stage stage = new Stage();
            stage.setScene(new Scene(root));
            MarkAttendanceController controller = loader.getController();

            stage.setTitle("Mark Attendance");
            stage.show();
            stage.setOnCloseRequest(event -> controller.stopCamera());
        }catch (Exception e){
            e.printStackTrace();
        }
    }
    public void handleEnrollment() throws IOException{
        try{
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/forms/Enrollment.fxml"));
            Parent root = loader.load();
            Stage stage = new Stage();
            stage.setScene(new Scene(root));
            EnrollmentController controller = loader.getController();

            stage.setTitle("Enrollment");
            stage.show();
            stage.setOnCloseRequest(event -> controller.stopCamera());
        }catch (Exception e){
            e.printStackTrace();
        }
    }
}
