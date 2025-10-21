package Controller;

import Model.User;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import dao.UserDao;
import javafx.stage.Stage;

public class UserBoxController {
    @FXML
    private TextField txtFindUser;

    @FXML
    private Button btnFindUser;

    private UserDao userDao=new UserDao();
    protected String name;
    protected String address;
    protected String gender;
    protected String contact;
    protected String email;


    public void handlefindUser() {
        String findingGmail = txtFindUser.getText();
        User foundUser = userDao.findUserByEmail(findingGmail);


        try{
            Stage currentStage = (Stage) btnFindUser.getScene().getWindow();
            currentStage.close();

            FXMLLoader loader = new FXMLLoader(getClass().getResource("/forms/UserUpdate.fxml"));
            Parent root = loader.load();


            UserUpdateController updateController = loader.getController();


            updateController.setUserData(foundUser);

            Stage stage = new Stage();
            stage.setScene(new Scene(root));
            stage.setTitle("User Update");
            stage.show();
        }catch(Exception e){
            e.printStackTrace();
        }


    }


}
