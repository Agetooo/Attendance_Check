package Controller;

import javafx.application.Platform;
import javafx.embed.swing.SwingFXUtils;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import org.bytedeco.javacv.*;
import org.bytedeco.opencv.opencv_core.*;
import org.bytedeco.opencv.opencv_objdetect.CascadeClassifier;
import javafx.concurrent.Task;
import java.awt.image.BufferedImage;
import java.io.File;
import java.net.URL;
import java.nio.file.Paths;
import java.util.ResourceBundle;

import static org.bytedeco.opencv.global.opencv_imgcodecs.imwrite;
import static org.bytedeco.opencv.global.opencv_imgproc.*;

public class EnrollmentController implements Initializable {

    @FXML
    private ImageView camView;
    @FXML
    private TextField tfUserId;
    @FXML
    private Button btnCapture;
    @FXML
    private Label lbCount;

    private OpenCVFrameGrabber grabber;
    private CascadeClassifier faceDetector;
    private volatile boolean stopThread = false;
    private boolean isCapturing = false;
    private int captureCount = 0;
    private int userId = 0;
    private final int MAX_SAMPLES = 100;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        try {
            String cascadePath = new File(getClass().getResource("/haarcascade_frontalface_default.xml").toURI()).getAbsolutePath();
            faceDetector = new CascadeClassifier(cascadePath);
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Lỗi nghiêm trọng: Không thể tải file Haar Cascade.");
            return;
        }

        File datasetDir = new File("dataset");
        if (!datasetDir.exists()) {
            datasetDir.mkdirs();
        }

        startCamera();
    }

    private void startCamera() {
        new Thread(() -> {
            try {
                grabber = new OpenCVFrameGrabber(0);
                grabber.start();

                OpenCVFrameConverter.ToMat converterToMat = new OpenCVFrameConverter.ToMat();

                while (!stopThread) {
                    Frame frame = grabber.grab();
                    if (frame == null) break;

                    Mat colorImage = converterToMat.convert(frame);
                    Mat grayImage = new Mat();

                    cvtColor(colorImage, grayImage, COLOR_BGR2GRAY);

                    RectVector faces = new RectVector();
                    faceDetector.detectMultiScale(grayImage, faces);

                    for (int i = 0; i < faces.size(); i++) {
                        Rect faceRect = faces.get(i);

                        rectangle(colorImage, faceRect, new Scalar(0, 255, 0, 1));

                        if (isCapturing && captureCount < MAX_SAMPLES) {
                            Mat faceToSave = new Mat(grayImage, faceRect);
                            resize(faceToSave, faceToSave, new Size(200, 200));
                            String savePath = "dataset/user." + userId + "." + captureCount + ".jpg";
                            imwrite(savePath, faceToSave);

                            captureCount++;
                            Platform.runLater(() -> lbCount.setText("Số ảnh đã chụp: " + captureCount));
                        }
                    }

                    if (captureCount >= MAX_SAMPLES) {
                        isCapturing = false;

                        Platform.runLater(() -> {
                            lbCount.setText("Đã chụp xong! Đang huấn luyện mô hình...");
                            btnCapture.setDisable(true);
                            btnCapture.setText("Đang huấn luyện...");
                        });

                        Task<Boolean> trainingTask = new Task<>() {
                            @Override
                            protected Boolean call() throws Exception {
                                return utility.FaceTrainer.trainModel();
                            }
                        };

                        trainingTask.setOnSucceeded(e -> {
                            boolean success = trainingTask.getValue();
                            if (success) {
                                lbCount.setText("Huấn luyện thành công! Sẵn sàng.");
                            } else {
                                lbCount.setText("Lỗi! Không thể huấn luyện mô hình.");
                            }
                            btnCapture.setDisable(false);
                            btnCapture.setText("Bắt đầu Chụp (Giữ 100 ảnh)");
                        });

                        trainingTask.setOnFailed(e -> {
                            lbCount.setText("Lỗi nghiêm trọng khi chạy Task huấn luyện.");
                            btnCapture.setDisable(false);
                            btnCapture.setText("Bắt đầu Chụp (Giữ 30 ảnh)");
                        });

                        new Thread(trainingTask).start();

                        captureCount = 0;
                    }

                    Image fxImage = matToImage(colorImage);
                    Platform.runLater(() -> camView.setImage(fxImage));
                }
            } catch (FrameGrabber.Exception e) {
                e.printStackTrace();
            } finally {
                stopCamera();
            }
        }).start();
    }


    @FXML
    private void handleCaptureButton() {
        String idText = tfUserId.getText();
        if (idText.isEmpty() || !idText.matches("\\d+")) {
            System.err.println("Vui lòng nhập ID hợp lệ (chỉ số).");
            return;
        }

        this.userId = Integer.parseInt(idText);
        this.isCapturing = true;
        this.captureCount = 0;
        btnCapture.setDisable(true);
        btnCapture.setText("Capturing...");
    }

    private Image matToImage(Mat mat) {
        OpenCVFrameConverter.ToMat converter = new OpenCVFrameConverter.ToMat();
        Frame frame = converter.convert(mat);
        BufferedImage bufferedImage = new Java2DFrameConverter().convert(frame);
        return SwingFXUtils.toFXImage(bufferedImage, null);
    }

    public void stopCamera() {
        stopThread = true;
        if (grabber != null) {
            try {
                grabber.stop();
                grabber.release();
            } catch (FrameGrabber.Exception e) {
                e.printStackTrace();
            }
        }
    }
}