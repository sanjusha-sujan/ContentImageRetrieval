import java.io.*;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.videoio.VideoCapture;

/*
Reading the video file, creating color histogram bins.
 */
public class readVideo {

    public static void main(String[] args) throws IOException {
        readVideo rVideo=new readVideo();
    }

    static{ System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
        System.loadLibrary("opencv_videoio_ffmpeg420_64");
    }

    double intensityBins[] = new double[25];
    double intensityMatrix[][] = new double[4000][25];

    public readVideo() throws IOException {

        int count=0;
        Mat frame=new Mat();
        VideoCapture camera= new VideoCapture("20020924_juve_dk_02a.mpg");
        System.out.println("Starting the intensity calculation");
        while(true){
            if(camera.read(frame)){
                if(count > 999 && count < 5000) {

                    if(count == 1864) {
                        Imgcodecs imageCodecs = new Imgcodecs();
                        imageCodecs.imwrite("saved.jpg", frame);
                    }

                    for (int i = 0; i < 25; i++) {
                        intensityBins[i] = 0;
                    }
                    getIntensity(frame);
                    for (int j = 0; j < 25; j++) {
                        intensityMatrix[count-1000][j] = intensityBins[j];
                    }
                }
                count++;
            }

            if(count >= 5000){
                break;
            }
        }
        System.out.println("Completed the intensity calculation..Writing to the file.");
        writeIntensity();
        System.out.println("Successfully written to the intensity file");
    }

    /*
    intensity method gets the intensity value for each pixel
    and reduces them into bins.
     */
    public void getIntensity(Mat frame) {
        for (int i = 0; i <frame.rows(); i++) {
            for (int j = 0; j < frame.cols(); j++) {
                double[] rgb = frame.get(i, j);
                double intensity = 0.299 * rgb[0] + 0.587 * rgb[1] + 0.114 * rgb[2];
                int reducedintensity = (int) intensity / 10;
                if(reducedintensity>=25){
                    intensityBins[24]+=1;
                }
                else {
                    intensityBins[reducedintensity] += 1;
                }
            }
        }
    }

    //This method writes the contents of the intensity matrix to a file called intensity.txt
    public void writeIntensity() throws IOException {
        BufferedWriter writer = new BufferedWriter(new FileWriter("intensity.txt"));
        for (int i = 0; i < 4000; i++) {
            for (int j = 0; j < 25; j++) {

                writer.write(intensityMatrix[i][j] + " ");
            }
            writer.write("\n");
        }
        writer.close();
    }
}


