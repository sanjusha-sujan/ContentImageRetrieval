import org.apache.commons.math3.stat.descriptive.moment.StandardDeviation;
import org.bytedeco.javacv.CanvasFrame;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.imgcodecs.Imgcodecs;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.*;
import java.io.*;
import java.util.ArrayList;
import java.util.List;
import javax.imageio.ImageIO;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import javax.swing.*;

/*
Model class to hold the attributes of the gradual transition.
 */
class GradualTransistion {
    int fsStart;
    int fsEnd;
    double sum;

    GradualTransistion() {
        fsStart = 0;
        fsEnd = 0;
        sum =0.0;
    }
}

public class ShotDetection extends JFrame {

    private JButton[] shots; //creates an array of JButtons
    private GridLayout gridLayout, gridLayout1, gridLayout3, gridLayout2;
    private JPanel panelTop, panelBottom;
    private JPanel buttonPanel;
    private JLabel pageNoLabel = new JLabel();
    private JLabel pageNoLabel2 = new JLabel();
    private JLabel pageNoLabel3 = new JLabel();
    private JLabel pageNoLabel4 = new JLabel();
    private Double[][] intensityMatrix = new Double[4000][25];
    private double[] SD = new double[3999];
    List<GradualTransistion> gts = new ArrayList<>();
    List<Image> images = new ArrayList<>();
    List<Integer> displayShots = new ArrayList<>();
    int buttonCount = 0;

    public static void main(String args[]) {

        System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
        System.loadLibrary("opencv_videoio_ffmpeg420_64");

        SwingUtilities.invokeLater(() -> {
                    ShotDetection app = new ShotDetection();
                    app.setVisible(true);
                    app.setLocationRelativeTo(null);
                    app.setExtendedState(JFrame.MAXIMIZED_BOTH);
                }
        );
    }

    /*
     *Contains the layout for all the gradualtransition buttons.
     *Reading the intensity file and detecting gradualtransiotions.
     */
    public ShotDetection() {
        panelTop = new JPanel();
        buttonPanel = new JPanel();
        panelBottom = new JPanel();
        gridLayout3 = new GridLayout(1, 1, 5, 5);
        gridLayout = new GridLayout(4, 10, 50, 50);
        gridLayout1 = new GridLayout(3, 5, 50, 50);
        gridLayout2 = new GridLayout(2, 2, 5, 5);

        setLayout(gridLayout2);
        panelTop.setLayout(gridLayout3);
        add(panelTop);
        add(panelBottom);
        buttonPanel.setLayout(gridLayout);
        panelTop.add(buttonPanel);
        readIntensityFile();
        readFrames();
        gradualtransitionDetection();
        panelBottom.setLayout(gridLayout1);

        JButton previousPage = new JButton("Previous Page");
        previousPage.setPreferredSize(new Dimension(100, 200));
        JButton nextPage = new JButton("Next Page");
        nextPage.setPreferredSize(new Dimension(100, 200));
        buttonPanel.add(previousPage);
        buttonPanel.add(nextPage);
        buttonPanel.add(pageNoLabel);
        buttonPanel.add(pageNoLabel2);
        buttonPanel.add(pageNoLabel3);
        buttonPanel.add(pageNoLabel4);


        nextPage.addActionListener(new nextPageHandler());
        previousPage.addActionListener(new previousPageHandler());
        displayShots.add(5000);
        Collections.sort(displayShots);
        System.out.println("----Shot frame numbers----");
        for(int i=0;i< displayShots.size()-1;i++){
            System.out.println(displayShots.get(i));
        }
        shots = new JButton[displayShots.size()];
        for (int i = 0; i < shots.length - 1; i++) {
            shots[i] = new JButton(displayShots.get(i).toString(), new ImageIcon(images.get(displayShots.get(i)).getScaledInstance(225, 125, Image.SCALE_SMOOTH)));
            shots[i].setPreferredSize(new Dimension(100, 50));
            shots[i].addActionListener(new ShotsHandler(i));
            shots[i].setHorizontalTextPosition(JButton.CENTER);
            shots[i].setVerticalTextPosition(JButton.CENTER);
            panelBottom.add(shots[i]);
        }


        setSize(1100, 750);
        setLocationRelativeTo(null);
        displayFirstPage();
    }

    public void readFrames() {

        Mat mat = new Mat();
        org.opencv.videoio.VideoCapture camera = new org.opencv.videoio.VideoCapture("20020924_juve_dk_02a.mpg");

        if (camera.isOpened()) {
            System.out.println("Opened the video");
        } else {
            System.out.println("Failed to open the video");
        }

        int count = 0;
        System.out.println("Reading the frames .... hold tight ... it will take a while...");
        while (true) {
            if (camera.read(mat)) {
                images.add(toBufferedImage(mat));
            }
            count++;
            if (count > 5000) {
                break;
            }
        }
    }

    public BufferedImage toBufferedImage(Mat m) {
        try {
            MatOfByte mob = new MatOfByte();
            Imgcodecs.imencode(".jpg", m, mob);
            byte ba[] = mob.toArray();

            BufferedImage bi = ImageIO.read(new ByteArrayInputStream(ba));
            return bi;
        }catch (Exception ex) {

        }
        return null;
    }
    /*
    Reading the intensity file generated by readVideo class.
     */
    public void readIntensityFile() {
        Scanner read;
        int lineNumber = 0;
        try {
            read = new Scanner(new File("intensity.txt"));

            while (read.hasNextLine()) {
                String[] s = read.nextLine().split(" ");

                for (int i = 0; i < 25; i++) {
                    intensityMatrix[lineNumber][i] = Double.parseDouble(s[i]);
                }
                lineNumber++;
            }

        } catch (FileNotFoundException EE) {
            System.out.println("The file intensity.txt does not exist");
        }
    }

    /*
    Looping through the frames
    Creating an array SD[]
    Finding Manhattan distance between the SD's.
    Finding the cuts and gradualtransitions.

     */
    public void gradualtransitionDetection() {

        List<GradualTransistion> possibleGradualTransistions = new ArrayList<>();

        for (int i = 0; i < 3999; i++) {
            SD[i] = 0.0;
            for (int j = 0; j < 25; j++) {
                SD[i] += Math.abs(intensityMatrix[i][j] - intensityMatrix[i + 1][j]);
            }
        }

        double mean = 0;
        for (int i = 0; i < 3999; i++) {
            mean = mean + SD[i];
        }

        mean = mean / 3999.0;
        StandardDeviation standardDeviation = new StandardDeviation();
        double stddev = standardDeviation.evaluate(SD, mean);

        double Tb = mean + (stddev * 11);
        double Ts = mean * 2;
        double Tor = 2;
        List<Integer> cut = new ArrayList<>();
        System.out.println("----ce-----");
        for (int i = 0; i < 3999; i++) {
            if (SD[i] >= Tb) {
                System.out.println(i + 1000 + 1);
                displayShots.add(i+1000);
                cut.add(i);
            }
        }
        GradualTransistion gt = null;
        int count = 0;
        for (int i = 0; i < 3999; i++) {

            if (Ts <= SD[i] && SD[i] < Tb && gt == null) {
                gt = new GradualTransistion();
                gt.fsStart = i + 1000;
                gt.sum += SD[i];
            }

            if (gt != null) {
                if (SD[i] < Ts) {
                    count++;
                } else {
                    count = 0;
                }

                if (count == 2) {
                    gt.fsEnd = i - 2 + 1000;
                    gt.sum -= (SD[i - 1]);
                    if (gt.fsStart != gt.fsEnd) {
                        possibleGradualTransistions.add(gt);
                    }
                    gt = null;
                    count = 0;
                    continue;
                }

                if (SD[i] > Tb) {
                    gt.fsEnd = i - 1 + 1000;
                    if (gt.fsStart != gt.fsEnd) {
                        possibleGradualTransistions.add(gt);
                    }
                    gt = null;
                    count = 0;
                    continue;
                }
                gt.sum += SD[i];
            }
        }

        count = 0;

        /*
        Finding the actual gradual transitions which are >=Tb
          */
        System.out.println("----fs+1-----");
        for (GradualTransistion gT : possibleGradualTransistions) {
            if (gT.sum >= Tb) {
                count++;
                displayShots.add(gT.fsStart);
                System.out.println((int) (gT.fsStart + 1));
                gts.add(gT);
            }
        }
    }


    /*This class implements an ActionListener for the ShotsHandler.
     *The button number is taken from the shotshandler and then capture gets the frame numbers
     *we call the display dshot method to get the video within range of frames.
     */
    private class ShotsHandler implements ActionListener {

        int buttonNo;
        ShotsHandler(int buttonNo) {
            this.buttonNo = buttonNo;
        }

        public void actionPerformed(ActionEvent e) {

            CanvasFrame canvas = new CanvasFrame("Short Display player");
            canvas.setExtendedState(JFrame.MAXIMIZED_BOTH);

            for(int i = displayShots.get(this.buttonNo); i < displayShots.get(this.buttonNo+1); i++) {
                canvas.showImage(images.get(i));
                try {
                    Thread.sleep(30);
                } catch (Exception ex) {

                }
            }

        }
    }

    private class previousPageHandler implements ActionListener {

        public void actionPerformed(ActionEvent e) {
            int startButton = buttonCount - 26;
            int endButton = buttonCount - 13;
            if (startButton >= 0) {
                panelBottom.removeAll();
                for (int i = startButton; i < endButton; i++) {
                    panelBottom.add(shots[i]);
                    buttonCount--;
                }
                panelBottom.revalidate();
                panelBottom.repaint();
            }
        }
    }

    /*This class implements an ActionListener for the nextPageButton.  The last image number to be displayed is set to the
     * current image count plus 20.  If the endImage number equals 101, then the next page button does not display any new
     * images because there are only 100 images to be displayed.  The first picture on the next page is the image located in
     * the buttonOrder array at the imageCount
     */
    private class nextPageHandler implements ActionListener {

        public void actionPerformed(ActionEvent e) {
            int endButton = buttonCount + 13;
            //System.out.println("endButton " + endButton);
            if (endButton <= 27) {
                //pageNoLabel.setText("Page " + endImage / 20 + "/" + imageSize.length / 20);
                panelBottom.removeAll();
                /*The for loop goes through the buttonOrder array starting with the startImage value
                 * and retrieves the image at that place and then adds the button to the panelBottom1.
                 */
                for (int i = buttonCount; i < endButton; i++) {
                    panelBottom.add(shots[i]);
                    buttonCount++;
                }

                panelBottom.revalidate();
                panelBottom.repaint();
            }
        }
    }

    /*This method displays the first twenty images in the panelBottom.  The for loop starts at number one and gets the image
     * number stored in the buttonOrder array and assigns the value to imageButNo.  The button associated with the image is
     * then added to panelBottom1.  The for loop continues this process until twenty images are displayed in the panelBottom1.
     *
     * In case relevance checkbox is selected then relevance checkboxes are displayed for each images.
     */
    private void displayFirstPage() {
        panelBottom.removeAll();
        for (int i = 0; i < 13; i++) {
            panelBottom.add(shots[i]);
            buttonCount++;
        }
        panelBottom.revalidate();
        panelBottom.repaint();
    }

}


