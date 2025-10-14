import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;

public class Spectogram {

    double xValue = 0;
    AudioDataStream manager;
    JPanel panel;
    JScrollPane scrollPane;
    int height = 0;
    int sampleWindowWidth = 2048;
    private double sensitivity = 5e5;
    double[][] fourierValues;
    private int imageHeight;

    public Spectogram(AudioDataStream manager) {
        this.manager = manager;
    }

    private BufferedImage image;

    public void renderPixel(int z, int p, int j, int imageHeight) {
        int red = 0;
        int green = 0;
        int blue = 0;
        red = (int) (255 * (fourierValues[z][p] / sensitivity));
        if(red > 150) {
            green = red - 150;
            if(green > 255) red += green - 255;
            if(green > 200) green = 200;
            if(red > 255) red = 255;
        }

        int rgb = blue + (green << 8) + (red << 16);

        if(imageHeight - j - 4 < 0) return;
        for(int k = 0; k < 4; k++) {
            image.setRGB(z, imageHeight - j - k - 1, rgb);
        }
    }

    public void renderImage(JFrame frame) throws InterruptedException {
        int width = manager.getSamples() / (2 * sampleWindowWidth);
        imageHeight = (sampleWindowWidth / 2) * 4;
        height = frame.getHeight();
        image = new BufferedImage(width, imageHeight, BufferedImage.TYPE_INT_RGB);
        fourierValues = new double[width][imageHeight];

        for (int z = 0; z < width; z++) {

            double[] convert = new double[sampleWindowWidth];

            for (int k = z * sampleWindowWidth, o = 0; k < (z + 1) * sampleWindowWidth; k++, o++) {
                convert[o] = manager.getIndexAmplitude(k);
            }

            FourierTransform.Complex[] array = FourierTransform.fft(convert);

            for (int j = 0, p = 0; j < imageHeight; j += 4, p++) {
                double value = Math.sqrt(array[p].imag * array[p].imag + array[p].real * array[p].real);
                fourierValues[z][p] = value;
                renderPixel(z, p, j, imageHeight);
            }
        }

        BufferedImage line = new BufferedImage(1, imageHeight, BufferedImage.TYPE_INT_RGB);
        for (int i = 0; i < 1; i++) {
            for (int j = 0; j < imageHeight; j++) {
                line.setRGB(i, j, (255 << 8));
            }
        }

        //1080 1728
        panel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
//                if(xValue > windowWidth) {
//                    currentScroll -= (int) xValue;
//                    xValue = 0;
//                }
                g.drawImage(image, 0, 0, this);
                g.drawImage(line, (int) xValue, 0, this);
            }
        };
        panel.setPreferredSize(new Dimension(image.getWidth(), image.getHeight()));
        panel.setLayout(new FlowLayout());
        scrollPane = new JScrollPane(panel);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        JScrollBar verticalBar = scrollPane.getVerticalScrollBar();
        verticalBar.setUnitIncrement(scrollSpeed);
        scrollPane.getHorizontalScrollBar().setUnitIncrement(scrollSpeed);
    }

    private int windowHeight = 0;
    private int scrollSpeed = 20;

    public void pause() {
        manager.pauseSound();
    }

    public void renderFrame(JFrame frame) {
        windowHeight = frame.getHeight();
        height = windowHeight;
        panel.repaint();
        frame.repaint();
    }


    public void reRender(JFrame frame) throws InterruptedException {

        Thread thread = new Thread(new Runnable() {
            public void run() {
                try {
                    if(!manager.initialized) {
                        manager.initializeAudio();
                    }
                    manager.playSoundInSeperateThread();
                    while (!manager.playing) {
                        Thread.sleep(10);
                    }

                    while (manager.playing) {


                        long t1 = System.nanoTime();
                        renderFrame(frame);
                        Thread.sleep(10);
                        long t2 = System.nanoTime();
                        xValue += 21.533203125 * (t2 - t1) / 1000000000.0;
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
        thread.start();
    }

    public JScrollPane getPanel() {
        return scrollPane;
    }
}
