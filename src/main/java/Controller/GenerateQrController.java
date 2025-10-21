package Controller;

import Model.User;
import dao.UserDao;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

import javafx.stage.FileChooser;
import javafx.stage.Stage;
import net.glxn.qrgen.QRCode;
import net.glxn.qrgen.image.ImageType;

import com.google.gson.Gson;
import utility.DialogUtil;


import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

import java.io.File;
import java.io.FileOutputStream;
import java.net.URL;

import java.util.HashMap;
import java.util.Map;
import java.util.ResourceBundle;

public class GenerateQrController implements Initializable {
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
    private ImageView viewQR;
    @FXML
    private Button btnSaveQR;

    private ObservableList<User> userList;
    private byte[] qrImageData;

    @Override
    public void initialize(URL location, ResourceBundle resources) {

        colId.setCellValueFactory(new PropertyValueFactory<>("displayId"));
        colName.setCellValueFactory(new PropertyValueFactory<>("name"));
        colGender.setCellValueFactory(new PropertyValueFactory<>("gender"));
        colEmail.setCellValueFactory(new PropertyValueFactory<>("email"));
        colContact.setCellValueFactory(new PropertyValueFactory<>("contact"));
        colAddress.setCellValueFactory(new PropertyValueFactory<>("address"));

        userList = FXCollections.observableArrayList(UserDao.getAllUsers());

        FilteredList<User> filteredData = new FilteredList<>(userList, p -> true);
        tableView.setItems(filteredData);

        tableView.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
            if (newSelection != null) {
                generateQRCode(newSelection);
            }
        });
    }
    public void handleSaveQR(){
        if (qrImageData == null) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Chưa có mã QR");
            alert.setHeaderText(null);
            alert.setContentText("Vui lòng chọn một người dùng để tạo mã QR trước khi lưu.");
            alert.showAndWait();
            return;
        }


        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Lưu mã QR");
        fileChooser.setInitialFileName("qrcode.png");


        FileChooser.ExtensionFilter extFilter = new FileChooser.ExtensionFilter("PNG files (*.png)", "*.png");
        fileChooser.getExtensionFilters().add(extFilter);


        Stage stage = (Stage) btnSaveQR.getScene().getWindow();
        File file = fileChooser.showSaveDialog(stage);


        if (file != null) {
            try (FileOutputStream fos = new FileOutputStream(file)) {

                fos.write(qrImageData);


                DialogUtil.showNotification("Success","Qr Downloaded Successfully!");

            } catch (Exception e) {
                e.printStackTrace();
                DialogUtil.showError("Error","File cannot be saved");
            }
        }
    }
    public void handleSaveQRAt(){}
    private void generateQRCode(User user) {
        String id = String.valueOf(user.getId());
        String name = user.getName();
        String email = user.getEmail();




        Map<String, String> data = new HashMap<>();

        data.put("id", id);
        data.put("name", name);
        data.put("email", email);


        Gson gson = new Gson();
        String jsonData = gson.toJson(data);

        try {
            ByteArrayOutputStream out = QRCode.from(jsonData)
                    .withSize(322, 286)
                    .to(ImageType.PNG)
                    .stream();

            this.qrImageData = out.toByteArray();


            ByteArrayInputStream bis = new ByteArrayInputStream(this.qrImageData);
            viewQR.setImage(new Image(bis));
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}
