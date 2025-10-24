package Controller;

import Model.HoSoChamCong;
import com.google.gson.Gson;
import com.google.zxing.BinaryBitmap;
import com.google.zxing.LuminanceSource;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.NotFoundException;
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
import javafx.fxml.FXMLLoader; // Thêm import
import javafx.fxml.Initializable;
import javafx.scene.Parent; // Thêm import
import javafx.scene.Scene; // Thêm import
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.Modality; // Thêm import
import javafx.stage.Stage; // Thêm import
import javafx.stage.StageStyle; // Thêm import
import javafx.util.Duration;

import java.awt.image.BufferedImage;
import java.io.IOException; // Thêm import
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.*;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.util.Map;
import java.util.ResourceBundle;

import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.Java2DFrameConverter;
import org.bytedeco.javacv.OpenCVFrameGrabber;
import utility.DialogUtil;

// Phiên bản này chỉ quét QR và mở popup xác thực
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

    private OpenCVFrameGrabber grabber;
    private volatile boolean isCameraActive = false;
    private Java2DFrameConverter converter = new Java2DFrameConverter();
    private boolean isSessionActive = false;

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

        loadAttendanceDataAsync();
    }

    /**
     * Hàm này bây giờ CHỈ quét mã QR
     */
    public void startQRCamera() {
        if (isCameraActive) return;

        new Thread(() -> {
            try {
                if (grabber == null) {
                    grabber = new OpenCVFrameGrabber(0);
                }
                grabber.start();
                isCameraActive = true;

                while (isCameraActive) {
                    Frame frame = grabber.grab();
                    if (frame == null) break;

                    BufferedImage bufferedImage = converter.convert(frame);
                    if (bufferedImage == null) continue;

                    Image fxImage = SwingFXUtils.toFXImage(bufferedImage, null);
                    Platform.runLater(() -> camView.setImage(fxImage));

                    if (isSessionActive) {
                        try {
                            LuminanceSource source = new BufferedImageLuminanceSource(bufferedImage);
                            BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(source));
                            Result result = new MultiFormatReader().decode(bitmap);

                            if (result != null) {
                                isCameraActive = false; // Dừng quét QR
                                String qrText = result.getText();

                                Platform.runLater(() -> {
                                    setCamoff();
                                    try {
                                        Gson gson = new Gson();
                                        Map<String, String> data = gson.fromJson(qrText, Map.class);
                                        int id = Integer.parseInt(data.get("id"));
                                        String name = data.get("name");
                                        String email = data.get("email");

                                        // Mở cửa sổ xác thực khuôn mặt
                                        openFaceVerificationWindow(id, name, email);


                                    } catch (Exception e) {
                                        // Hiển thị chi tiết lỗi
                                        String errorMsg = "Lỗi: " + e.getMessage();
                                        System.err.println("Chi tiết lỗi QR: " + qrText); // In ra chuỗi QR
                                        e.printStackTrace(); // In ra lỗi đầy đủ

                                        DialogUtil.showError("Lỗi QR", errorMsg); // Hiển thị lỗi thật
                                        if (isSessionActive) startQRCamera();
                                    }
                                });
                                break;
                            }
                        } catch (NotFoundException e) {
                            // Bỏ qua, không tìm thấy QR
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                stopCameraInternal();
            }
        }).start();
    }

    /**
     * Hàm MỚI: Mở cửa sổ popup để xác thực khuôn mặt
     */
    private void openFaceVerificationWindow(int id, String name, String email) {
        try {
            // Đảm bảo đường dẫn này khớp với cấu trúc project của bạn
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/forms/FaceVerify.fxml"));
            Parent root = loader.load();

            FaceVerifyController faceVerifyController = loader.getController();
            // Truyền dữ liệu (ID, Tên) và chính Controller này qua
            faceVerifyController.initData(id, name, email, this);

            Stage popupStage = new Stage();
            popupStage.initModality(Modality.APPLICATION_MODAL); // Chặn cửa sổ chính
            popupStage.initStyle(StageStyle.UNDECORATED); // Không viền
            popupStage.setScene(new Scene(root));

            popupStage.showAndWait(); // Hiển thị và chờ cho đến khi popup đóng

            // SAU KHI POPUP ĐÓNG:
            loadAttendanceDataAsync(); // Tải lại bảng

            if (isSessionActive) {
                startQRCamera(); // Bật lại camera QR để chờ người tiếp theo
            }

        } catch (IOException e) {
            e.printStackTrace();
            DialogUtil.showError("Lỗi FXML", "Không thể mở cửa sổ xác thực khuôn mặt. Kiểm tra đường dẫn /View/FaceVerify.fxml");
        }
    }

    // --- Các hàm điều khiển camera (giữ nguyên) ---
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

    private void stopCameraInternal() {
        if (grabber != null) {
            try {
                grabber.stop();
                grabber.release();
            } catch (Exception e) { e.printStackTrace(); }
        }
        grabber = null;
        Platform.runLater(() -> setCamoff());
    }

    public void stopCamera() {
        isCameraActive = false;
    }

    public void handleResume() {
        if (!isCameraActive && isSessionActive) {
            startQRCamera();
        }
    }

    public void handleStopCam() {
        stopCamera();
    }

    public void setCamoff() {
        try {
            Image image = new Image(getClass().getResource("/images/camOff.png").toExternalForm());
            camView.setImage(image);
        } catch (Exception e) {
            System.err.println("Không thể tải ảnh camOff.png: " + e.getMessage());
        }
    }

    // --- Các hàm nghiệp vụ (hàm handleAttendance ĐÃ BỊ XÓA) ---

    // Hàm này phải PUBLIC để FaceVerifyController gọi
    public void loadAttendanceDataAsync() {
        Task<ObservableList<HoSoChamCong>> loadDataTask = new Task<>() {
            @Override
            protected ObservableList<HoSoChamCong> call() throws Exception {
                // (Code bên trong giữ nguyên)
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
                    e.printStackTrace(); throw e;
                }
                return list;
            }
        };
        loadDataTask.setOnSucceeded(e -> {
            attendanceTable.setItems(loadDataTask.getValue());
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
            startQRCamera(); // Sửa thành startQRCamera()
            btnStartSession.setDisable(true);
            btnEndSession.setDisable(false);
        }
    }

    public void handleEndSession(){
        if(isSessionActive){
            isSessionActive = false;
            stopCamera();
            generateReportAndEndSessionAsync();
            btnStartSession.setDisable(false);
            btnEndSession.setDisable(true);
        }
    }

    private void generateReportAndEndSessionAsync() {
        // (Code hàm này giữ nguyên)
        Task<Void> endSessionTask = new Task<>() {
            @Override
            protected Void call() throws Exception {
                // ... (Toàn bộ code của hàm này giữ nguyên)
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