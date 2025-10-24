package Controller;

import dao.ConnectionProvider;
import javafx.application.Platform;
import javafx.embed.swing.SwingFXUtils;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.Stage;
import org.bytedeco.javacpp.DoublePointer;
import org.bytedeco.javacpp.IntPointer;
import org.bytedeco.javacv.*;
import org.bytedeco.opencv.opencv_core.*;
import org.bytedeco.opencv.opencv_face.LBPHFaceRecognizer; // Sửa 1: Đổi từ FaceRecognizer
import org.bytedeco.opencv.opencv_objdetect.CascadeClassifier;
import utility.DialogUtil;

import java.awt.image.BufferedImage; // Sửa 2: Thêm import này
import java.io.File;
import java.net.URL;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ResourceBundle;

import static org.bytedeco.opencv.global.opencv_imgproc.*;

public class FaceVerifyController implements Initializable {

    @FXML
    private ImageView camView;
    @FXML
    private Label lbStatus;

    private OpenCVFrameGrabber grabber;
    private CascadeClassifier faceDetector;
    private LBPHFaceRecognizer recognizer;
    private OpenCVFrameConverter.ToMat converterToMat = new OpenCVFrameConverter.ToMat();
    private Java2DFrameConverter converterToImage = new Java2DFrameConverter();
    private volatile boolean isCameraActive = false;
    private boolean isModelLoaded = false;
    private final JavaFXFrameConverter fxConverter = new JavaFXFrameConverter();

    private int userId;
    private String userName;
    private String userEmail;

    private MarkAttendanceController mainController; // Tham chiếu tới controller chính

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        try {
            String cascadePath = new File(getClass().getResource("/haarcascade_frontalface_default.xml").toURI()).getAbsolutePath();
            faceDetector = new CascadeClassifier(cascadePath);

            recognizer = LBPHFaceRecognizer.create();
            recognizer.read("model.yml");
            isModelLoaded=true;
        } catch (Exception e) {
            e.printStackTrace();
            lbStatus.setText("Lỗi: Không thể tải model!");
            isModelLoaded=false;
        }
    }

    public void initData(int id, String name, String email, MarkAttendanceController mainController) {
        this.userId = id;
        this.userName = name;
        this.userEmail = email;
        this.mainController = mainController;

        lbStatus.setText("Xác thực: " + name);
        startFaceCamera();
    }

    private void startFaceCamera() {
        if (!isModelLoaded) {
            Platform.runLater(() -> {
                DialogUtil.showError("Lỗi Model", "Không thể xác thực. Model.yml bị lỗi hoặc không tồn tại.");
                closeWindow();
            });
            return;
        }
        if (isCameraActive) return;

        new Thread(() -> {
            try {
                grabber = new OpenCVFrameGrabber(0);
                grabber.start();
                isCameraActive = true;
                int verificationAttempts = 0;

                while (isCameraActive) {
                    Frame frame = grabber.grab();
                    if (frame == null) break;
                    Platform.runLater(() -> {
                        camView.setImage(fxConverter.convert(frame));
                    });

                    Mat colorImage = converterToMat.convert(frame);
                    Mat grayImage = new Mat();
                    cvtColor(colorImage, grayImage, COLOR_BGR2GRAY);

                    RectVector faces = new RectVector();
                    faceDetector.detectMultiScale(grayImage, faces);


                    boolean foundMatchInFrame = false;
                    boolean foundWrongPersonInFrame = false;

                    for (int i = 0; i < faces.size(); i++) {
                        Rect faceRect = faces.get(i);
                        Mat faceToRecognize = new Mat(grayImage, faceRect);
                        resize(faceToRecognize, faceToRecognize, new Size(200, 200));

                        IntPointer label = new IntPointer(1);
                        DoublePointer confidence = new DoublePointer(1);
                        recognizer.predict(faceToRecognize, label, confidence);

                        int predictedId = label.get(0);
                        double conf = confidence.get(0);

                        if (predictedId == this.userId && conf < 55) {

                            isCameraActive = false;
                            foundMatchInFrame = true;
                            Platform.runLater(() -> lbStatus.setText("Successfully"));
                            handleAttendance();
                            Thread.sleep(10000);
                            Platform.runLater(this::closeWindow);
                            break;

                        } else if (predictedId != this.userId && conf < 55) {
                            foundWrongPersonInFrame = true;

                        } else {
                        }
                    }

                    if (foundMatchInFrame) {
                        break;
                    }


                    if (foundWrongPersonInFrame) {
                        verificationAttempts++;
                        final int currentAttempts = verificationAttempts;
                        Platform.runLater(() -> lbStatus.setText("Wrong! (" + currentAttempts + "/5)"));

                    } else if (faces.size() == 0) {
                        Platform.runLater(() -> lbStatus.setText("Put your face here!"));

                    } else {
                        Platform.runLater(() -> lbStatus.setText("Recognizing"));
                    }

                    if (verificationAttempts > 5) {
                        isCameraActive = false;
                        Platform.runLater(() -> DialogUtil.showError("Error", "Failed"));
                        Thread.sleep(1000);
                        Platform.runLater(this::closeWindow);
                        break;
                    }


                    Thread.sleep(66);

                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                stopCameraInternal();
            }
        }).start();
    }


    private void handleAttendance() {
        LocalDate today = LocalDate.now();
        String checkQuery = "SELECT checkin, checkout FROM userattendance WHERE userid = ? AND date = ?";

        try (Connection connection = ConnectionProvider.getCon();
             PreparedStatement checkStmt = connection.prepareStatement(checkQuery)) {

            checkStmt.setInt(1, this.userId);
            checkStmt.setDate(2, java.sql.Date.valueOf(today));

            try (ResultSet rs = checkStmt.executeQuery()) {
                if (!rs.next()) {
                    String insertQuery = "INSERT INTO userattendance (userid, date, checkin) VALUES (?, ?, ?)";
                    try (PreparedStatement insertStmt = connection.prepareStatement(insertQuery)) {
                        insertStmt.setInt(1, this.userId);
                        insertStmt.setDate(2, java.sql.Date.valueOf(today));
                        insertStmt.setTimestamp(3, Timestamp.valueOf(LocalDateTime.now()));
                        insertStmt.executeUpdate();
                    }
                    Platform.runLater(() -> DialogUtil.showNotification("Success", "User " + this.userName +" checked in"));
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
                            updateStmt.setString(2, workDurationText);
                            updateStmt.setInt(3, this.userId);
                            updateStmt.setDate(4, java.sql.Date.valueOf(today));
                            updateStmt.executeUpdate();
                        }
                        Platform.runLater(() -> DialogUtil.showNotification("Thành công",
                                "User " + this.userName +" checked out" + "\n Duration: " + workDurationText));
                    } else {
                        Platform.runLater(() -> DialogUtil.showNotification( "Success",
                                "User " + this.userName + " đã chấm công xong hôm nay!"));
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            Platform.runLater(() -> DialogUtil.showError("Lỗi CSDL", "Có lỗi xảy ra khi chấm công: " + e.getMessage()));
        }
    }

    private Image matToImage(Mat mat) {
        Frame frame = converterToMat.convert(mat);
        BufferedImage bufferedImage = converterToImage.convert(frame);
        return SwingFXUtils.toFXImage(bufferedImage, null);
    }

    private void stopCameraInternal() {
        if (grabber != null) {
            try {
                grabber.stop();
                grabber.release();
            } catch (Exception e) { e.printStackTrace(); }
        }
        grabber = null;
    }

    private void closeWindow() {
        isCameraActive = false;
        Stage stage = (Stage) camView.getScene().getWindow();
        stage.close();
    }
}