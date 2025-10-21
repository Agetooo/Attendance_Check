package Controller;

import Model.HoSoChamCong;
import com.google.gson.Gson;
import com.google.zxing.BinaryBitmap;
import com.google.zxing.LuminanceSource;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.Result;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.common.HybridBinarizer;
import dao.ConnectionProvider;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.embed.swing.SwingFXUtils;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.util.Duration;

import java.awt.image.BufferedImage;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.*;

import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.github.sarxos.webcam.Webcam;
import utility.DialogUtil;

public class MarkAttendanceController implements Initializable {
    @FXML
    private Label lbTime;
    @FXML
    private ImageView camView;
    @FXML
    private Button btnResume;
    @FXML
    private Button btnStopCam;
    @FXML
    private TableView<HoSoChamCong> attendanceTable;
    @FXML
    private TableColumn<HoSoChamCong, Integer> colId;
    @FXML
    private TableColumn<HoSoChamCong, String> colName;
    @FXML
    private TableColumn<HoSoChamCong, String> colEmail;
    @FXML
    private TableColumn<HoSoChamCong, String> colCheckin;
    @FXML
    private TableColumn<HoSoChamCong, String> colCheckout;

    @FXML
    private Button btnStartSession;
    @FXML
    private Button btnEndSession;

    private Webcam webcam;
    private ExecutorService executor;
    private boolean isCameraActive = false;

    private boolean isSessionActive = false;


    private Integer id;
    private String name;
    private String email;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        startClock();
        Image image = new Image(getClass().getResource("/images/camOff.png").toExternalForm());
        camView.setImage(image);
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colName.setCellValueFactory(new PropertyValueFactory<>("ten"));
        colEmail.setCellValueFactory(new PropertyValueFactory<>("email"));
        colCheckin.setCellValueFactory(new PropertyValueFactory<>("checkin"));
        colCheckout.setCellValueFactory(new PropertyValueFactory<>("checkout"));

        // 2. Gọi luồng mới để tải dữ liệu
        loadAttendanceDataAsync();
    }

    public void startCamera() {
        if (executor != null && !executor.isShutdown()) {
            executor.shutdownNow();
        }
        executor = Executors.newSingleThreadExecutor();

        if (webcam == null) {
            webcam = Webcam.getDefault();
            if (webcam == null) {
                Platform.runLater(() -> DialogUtil.showError("Lỗi Camera", "Không tìm thấy webcam."));
                return;
            }
        }

        if (!webcam.isOpen()) {
            webcam.open();
        }

        isCameraActive = true;

        executor.submit(() -> {
            try {

                while (isCameraActive && webcam.isOpen()) {
                    BufferedImage bufferedImage = webcam.getImage();
                    if (bufferedImage == null) {
                        Thread.sleep(10);
                        continue;
                    }

                    Image fxImage = SwingFXUtils.toFXImage(bufferedImage, null);
                    Platform.runLater(() -> camView.setImage(fxImage));

                    if (isSessionActive) {
                        try {
                            LuminanceSource source = new BufferedImageLuminanceSource(bufferedImage);
                            BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(source));
                            Result result = new MultiFormatReader().decode(bitmap);

                            if (result != null) {
                                isCameraActive = false;

                                String qrText = result.getText();
                                Gson gson = new Gson();
                                Map<String, String> data = gson.fromJson(qrText, Map.class);


                                String idAsString = data.get("id");
                                this.id = Integer.parseInt(idAsString);
                                this.name = data.get("name");
                                this.email = data.get("email");

                                Platform.runLater(() -> {
                                    setCamoff();
                                    handleAttendance();
                                });

                                break;
                            }
                        } catch (Exception e) {
                        }
                    }
                    Thread.sleep(10);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                if (webcam != null && webcam.isOpen()) {
                    webcam.close();
                    Platform.runLater(() -> System.out.println("Webcam đã đóng an toàn."));
                }
            }
        });
    }

    public void startClock() {
        Timeline clock = new Timeline(
                new KeyFrame(Duration.ZERO, e -> {
                    SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                    lbTime.setText(simpleDateFormat.format(new Date()));
                }),

                new KeyFrame(Duration.seconds(1))
        );
        clock.setCycleCount(Animation.INDEFINITE);
        clock.play();
    }


    public void stopCamera() {
        isCameraActive = false;
        if (executor != null && !executor.isShutdown()) {
            executor.shutdownNow();
        }
        if (webcam != null && webcam.isOpen()) {
            webcam.close();
        }
    }

    public void handleResume() {

        if (!isCameraActive && (webcam == null || !webcam.isOpen())) {
            startCamera();
        }
    }

    public void handleStopCam() {
        stopCamera();
        setCamoff();
    }

    public void setCamoff() {
        try {
            Image image = new Image(getClass().getResource("/images/camOff.png").toExternalForm());
            camView.setImage(image);
        } catch (Exception e) {
            System.err.println("Không thể tải ảnh camOff.png: " + e.getMessage());
        }
    }


    public void handleAttendance() {
        if (this.id == null) {
            DialogUtil.showError("Lỗi", "Không quét được ID người dùng.");
            return;
        }

        LocalDate today = LocalDate.now();
        String checkQuery = "SELECT checkin, checkout FROM userattendance WHERE userid = ? AND date = ?";


        try (Connection connection = ConnectionProvider.getCon();
             PreparedStatement checkStmt = connection.prepareStatement(checkQuery)) {

            checkStmt.setInt(1, this.id);
            checkStmt.setDate(2, java.sql.Date.valueOf(today));

            try (ResultSet rs = checkStmt.executeQuery()) {
                if (!rs.next()) {

                    String insertQuery = "INSERT INTO userattendance (userid, date, checkin) VALUES (?, ?, ?)";
                    try (PreparedStatement insertStmt = connection.prepareStatement(insertQuery)) {
                        insertStmt.setInt(1, this.id);
                        insertStmt.setDate(2, java.sql.Date.valueOf(today));
                        insertStmt.setTimestamp(3, Timestamp.valueOf(LocalDateTime.now()));
                        insertStmt.executeUpdate();
                    }
                    loadAttendanceDataAsync();
                    DialogUtil.showNotification("Success",
                            "User " + this.name +" checked in");

                } else {

                    Timestamp checkinTime = rs.getTimestamp("checkin");
                    Timestamp checkoutTime = rs.getTimestamp("checkout");

                    if (checkoutTime == null) {
                        LocalDateTime checkoutNow = LocalDateTime.now();
                        LocalDateTime checkinLDT = checkinTime.toLocalDateTime();


                        java.time.Duration duration = java.time.Duration.between(checkinLDT, checkoutNow);
                        long hours = duration.toHours();
                        long minutes = duration.toMinutes() % 60;
                        String workDurationText = hours + " giờ " + minutes + " phút";

                        String updateQuery = "UPDATE userattendance SET checkout = ?, workduration = ? " +
                                "WHERE userid = ? AND date = ?";

                        try (PreparedStatement updateStmt = connection.prepareStatement(updateQuery)) {
                            updateStmt.setTimestamp(1, Timestamp.valueOf(checkoutNow));
                            updateStmt.setString(2, workDurationText); // Dùng setString
                            updateStmt.setInt(3, this.id);
                            updateStmt.setDate(4, java.sql.Date.valueOf(today));
                            updateStmt.executeUpdate();
                        }
                        loadAttendanceDataAsync();

                        DialogUtil.showNotification("Thành công",
                                "User " + this.name +" checked out" +
                                        "\n Duration: " + workDurationText);
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            DialogUtil.showError("Lỗi CSDL", "Có lỗi xảy ra khi chấm công: " + e.getMessage());
        }
    }

    public void loadAttendanceDataAsync() {

        // 1. Tạo Task (định nghĩa việc cần làm)
        Task<ObservableList<HoSoChamCong>> loadDataTask = new Task<>() {
            @Override
            protected ObservableList<HoSoChamCong> call() throws Exception {

                ObservableList<HoSoChamCong> list = FXCollections.observableArrayList();

                String sql = "SELECT u.id, u.name, u.email, a.checkin, a.checkout " +
                        "FROM userdetails u " +
                        "LEFT JOIN userattendance a ON u.id = a.userid AND a.date = CURRENT_DATE";


                try (Connection conn = ConnectionProvider.getCon();
                     PreparedStatement ps = conn.prepareStatement(sql);
                     ResultSet rs = ps.executeQuery()) {

                    while (rs.next()) {
                        int id = rs.getInt("id");
                        String name = rs.getString("name");
                        String email = rs.getString("email");
                        Timestamp checkin = rs.getTimestamp("checkin");
                        Timestamp checkout = rs.getTimestamp("checkout");


                        list.add(new HoSoChamCong(id, name, email, checkin, checkout));
                    }
                } catch (SQLException e) {
                    e.printStackTrace();
                    throw e;
                }
                return list;
            }
        };


        loadDataTask.setOnSucceeded(e -> {
            ObservableList<HoSoChamCong> resultList = loadDataTask.getValue();
            attendanceTable.setItems(resultList);
            System.out.println("Tải dữ liệu bảng thành công!");
        });

        loadDataTask.setOnFailed(e -> {
            Throwable error = loadDataTask.getException();
            System.err.println("Lỗi khi chạy Task: " + error.getMessage());
            error.printStackTrace();
        });
        new Thread(loadDataTask).start();
    }

    public void handleStartSession(){
        if(!isSessionActive){
            isSessionActive = true;
            startCamera();
        }
    }
    public void handleEndSession(){
        if(isSessionActive){
            isSessionActive = false;
            generateReportAndEndSessionAsync();
        }
    }
    private void generateReportAndEndSessionAsync() {
        Task<Void> endSessionTask = new Task<>() {
            @Override
            protected Void call() throws Exception {
                StringBuilder reportContent = new StringBuilder();
                String sql = "SELECT u.id, u.name, u.email, a.checkin, a.checkout, a.workduration " +
                        "FROM userdetails u " +
                        "LEFT JOIN userattendance a ON u.id = a.userid AND a.date = CURRENT_DATE " +
                        "ORDER BY u.name";

                DateTimeFormatter dtf = DateTimeFormatter.ofPattern("HH:mm:ss");

                reportContent.append("BÁO CÁO CHẤM CÔNG NGÀY: ")
                        .append(LocalDate.now().toString())
                        .append("\n============================================\n\n");

                try (Connection conn = ConnectionProvider.getCon();
                     PreparedStatement ps = conn.prepareStatement(sql);
                     ResultSet rs = ps.executeQuery()) {

                    while (rs.next()) {
                        String name = rs.getString("name");
                        Timestamp checkin = rs.getTimestamp("checkin");
                        Timestamp checkout = rs.getTimestamp("checkout");
                        String workDuration = rs.getString("workduration");

                        reportContent.append(String.format("Nhân viên: %s\n", name));
                        if (checkin != null) {
                            reportContent.append(String.format("\t- Giờ vào: %s\n", checkin.toLocalDateTime().format(dtf)));
                            reportContent.append(String.format("\t- Giờ ra: %s\n", (checkout != null) ? checkout.toLocalDateTime().format(dtf) : "Chưa check-out"));
                            reportContent.append(String.format("\t- Tổng giờ: %s\n", (workDuration != null) ? workDuration : "N/A"));
                        } else {
                            reportContent.append("\t- Trạng thái: VẮNG MẶT\n");
                        }
                        reportContent.append("\n");
                    }
                }

                String userHome = System.getProperty("user.home");
                Path directory = Paths.get(userHome, "BaoCaoTongHop");
                if (!Files.exists(directory)) {
                    Files.createDirectories(directory);
                }
                String fileName = "BaoCao_" + LocalDate.now().toString() + ".txt";
                Path filePath = directory.resolve(fileName);
                Files.writeString(filePath, reportContent.toString());

                updateMessage("Đã xuất báo cáo thành công tại: " + filePath);


                String deleteSql = "DELETE FROM userattendance WHERE date = CURRENT_DATE";
                try (Connection conn = ConnectionProvider.getCon();
                     PreparedStatement ps = conn.prepareStatement(deleteSql)) {
                    ps.executeUpdate();
                }

                return null;
            }
        };


        endSessionTask.setOnSucceeded(e -> {
            DialogUtil.showNotification( "Hoàn tất", "Đã kết thúc session và xuất báo cáo thành công!");
            loadAttendanceDataAsync();
        });

        endSessionTask.setOnFailed(e -> {
            Throwable error = endSessionTask.getException();
            error.printStackTrace();
            DialogUtil.showError("Lỗi nghiêm trọng", "Không thể kết thúc session: " + error.getMessage());
        });

        new Thread(endSessionTask).start();
    }


}