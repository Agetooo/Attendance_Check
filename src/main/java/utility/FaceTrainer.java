package utility;

import org.bytedeco.javacpp.IntPointer;
import org.bytedeco.opencv.opencv_core.Mat;
import org.bytedeco.opencv.opencv_core.MatVector;
import org.bytedeco.opencv.opencv_face.FaceRecognizer;
import org.bytedeco.opencv.opencv_face.LBPHFaceRecognizer;

import java.io.File;
import java.io.FilenameFilter;
import java.nio.IntBuffer;

import static org.bytedeco.opencv.global.opencv_core.CV_32SC1;
import static org.bytedeco.opencv.global.opencv_imgcodecs.IMREAD_GRAYSCALE;
import static org.bytedeco.opencv.global.opencv_imgcodecs.imread;


public class FaceTrainer {

    public static boolean trainModel() {
        System.out.println("Bắt đầu quá trình huấn luyện...");

        File datasetDir = new File("dataset");

        FilenameFilter imgFilter = (dir, name) -> name.toLowerCase().endsWith(".jpg");

        File[] imageFiles = datasetDir.listFiles(imgFilter);

        if (imageFiles == null || imageFiles.length == 0) {
            System.err.println("Không tìm thấy ảnh nào trong thư mục 'dataset'.");
            return false;
        }

        try {
            MatVector images = new MatVector(imageFiles.length);
            Mat labels = new Mat(imageFiles.length, 1, CV_32SC1);
            IntBuffer labelsBuffer = labels.createBuffer();

            int counter = 0;
            for (File image : imageFiles) {
                Mat img = imread(image.getAbsolutePath(), IMREAD_GRAYSCALE);
                int label = Integer.parseInt(image.getName().split("\\.")[1]);
                images.put(counter, img);
                labelsBuffer.put(counter, label);
                counter++;
            }

            FaceRecognizer recognizer = LBPHFaceRecognizer.create();
            System.out.println("Đang huấn luyện mô hình (có thể mất vài giây)...");
            recognizer.train(images, labels);

            recognizer.save("model.yml");

            System.out.println("Đã huấn luyện xong và lưu mô hình tại 'model.yml'!");
            return true;

        } catch (Exception e) {
            System.err.println("Lỗi nghiêm trọng trong quá trình huấn luyện: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
}