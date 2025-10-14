import javax.sound.sampled.UnsupportedAudioFileException;
import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;

public class Window {


    private JFrame frame;
    private JPanel overallPanel;
    public static int buttonHeight = 50;
    private JButton playPauseButton, stopButton, microphoneButton;
    private JTextField fileNameTextField;
    private ImageIcon play, pause, microphone;
    private FileManager fileManager;
    private Spectogram spectogram;
    private String fileName;
    private JLabel unsupportedFile;
    private String loadingMessage = "Loading...";
    private String failLoadText = "<html>Sorry this file failed to load<br>or the file type is not supported.<br>Only .wav audio files are supported.</html>";
    private boolean failedToLoad = false;
    private boolean recording = false;
    private MicrophoneHandler microphoneHandler;

    public Window() {


        frame = new JFrame("Sound Library");
        Image icon = new ImageIcon(System.getProperty("user.dir") + "/" + "logo.png/").getImage();
        frame.setIconImage(icon);
        Taskbar.getTaskbar().setIconImage(icon);
        frame.setBounds(100, 100, 800, 600);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        play = createIcon(System.getProperty("user.dir") + "/" + "play.png", buttonHeight, buttonHeight);
        pause = createIcon(System.getProperty("user.dir") + "/" + "pause.png", buttonHeight, buttonHeight);
        microphone = createIcon(System.getProperty("user.dir") + "/" + "microphone.png", buttonHeight, buttonHeight);
        playPauseButton = new JButton(play);
        stopButton = new JButton(createIcon(System.getProperty("user.dir") + "/" + "stop.png", buttonHeight, buttonHeight));
        microphoneButton = new JButton(microphone);

        playPauseButton.setBounds(0, 0, buttonHeight, buttonHeight);
        stopButton.setBounds(0, buttonHeight, buttonHeight, buttonHeight);
        fileNameTextField = new JTextField("Enter filename");
        playPauseButton.setPreferredSize(new Dimension(buttonHeight + 10, buttonHeight + 10));
        playPauseButton.setBorder(BorderFactory.createEmptyBorder());
        playPauseButton.setContentAreaFilled(false);
        stopButton.setPreferredSize(new Dimension(buttonHeight + 10, buttonHeight + 10));
        stopButton.setBorder(BorderFactory.createEmptyBorder());
        stopButton.setContentAreaFilled(false);
        microphoneButton.setPreferredSize(new Dimension(buttonHeight + 10, buttonHeight + 10));
        microphoneButton.setBorder(BorderFactory.createEmptyBorder());
        microphoneButton.setContentAreaFilled(false);

        overallPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        overallPanel.setBackground(Color.DARK_GRAY);
        overallPanel.add(fileNameTextField);
        overallPanel.add(playPauseButton);
        overallPanel.add(stopButton);
        overallPanel.add(microphoneButton);
        unsupportedFile = new JLabel(failLoadText);//
        unsupportedFile.setForeground(Color.DARK_GRAY);
        unsupportedFile.setFont(new Font("Arial", Font.BOLD, 16));
        overallPanel.add(unsupportedFile);
        frame.add(overallPanel, BorderLayout.NORTH);
        frame.setVisible(true);

        fileNameTextField.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent event) {
                if (spectogram != null) {
                    frame.remove(spectogram.getPanel());
                    stopPlaying();
                }
                fileName = fileNameTextField.getText();

                unsupportedFile.setText(loadingMessage);
                unsupportedFile.setForeground(Color.CYAN);
                unsupportedFile.repaint();
                try {

                    new SwingWorker<Void, Void>() {
                        @Override
                        protected Void doInBackground() {
                            try {
                                loadNewAudio();
                                failedToLoad = false;
                            } catch (Exception e) {
                                unsupportedFile.setText(failLoadText);
                                unsupportedFile.setForeground(Color.RED);
                                unsupportedFile.repaint();
                                failedToLoad = true;
                            }
                            return null;
                        }

                        @Override
                        protected void done() {
                            if(!failedToLoad) {
                                unsupportedFile.setForeground(Color.DARK_GRAY);
                                unsupportedFile.repaint();
                                SwingUtilities.invokeLater(() -> {
                                    spectogram.scrollPane.getVerticalScrollBar().setValue(spectogram.scrollPane.getVerticalScrollBar().getMaximum());
                                    spectogram.panel.repaint();
                                });
                            }
                        }
                    }.execute();



                } catch (Exception e) {
                    unsupportedFile.setText(failLoadText);
                    unsupportedFile.setForeground(Color.RED);
                    unsupportedFile.repaint();
                }
            }
        });

        playPauseButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent event) {
                if (playPauseButton.getIcon().equals(play)) {
                    try {
                        spectogram.reRender(frame);
                        playPauseButton.setIcon(pause);
                    } catch (InterruptedException e) {

                    }
                } else {
                    spectogram.pause();
                    playPauseButton.setIcon(play);
                }
            }
        });

        stopButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent event) {
                stopPlaying();
            }
        });

        microphoneButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent event) {
                if(!recording) {
                    startRecording();
                    recording = true;
                } else {
                    try {
                        stopRecording();
                        recording = false;
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        });

    }


    private void stopPlaying() {
        spectogram.pause();
        spectogram.manager.initialized = false;
        playPauseButton.setIcon(play);
        spectogram.xValue = 0;
        spectogram.renderFrame(frame);
    }

    private void startRecording() {
        if (spectogram != null) {
            spectogram.pause();
            spectogram.manager.initialized = false;
            playPauseButton.setIcon(play);
            spectogram.xValue = 0;
            frame.remove(spectogram.getPanel());
            stopPlaying();
        }

        unsupportedFile.setText(loadingMessage);
        unsupportedFile.setForeground(Color.CYAN);
        unsupportedFile.repaint();

        microphoneHandler = new MicrophoneHandler();
        microphoneHandler.startRecording();

    }

    private void stopRecording() throws InterruptedException {
        microphoneHandler.stopRecording();
        spectogram = new Spectogram(microphoneHandler.getAudioStream());
        renderSpectogram(spectogram);

        unsupportedFile.setForeground(Color.DARK_GRAY);
        unsupportedFile.repaint();
        SwingUtilities.invokeLater(() -> {
            spectogram.scrollPane.getVerticalScrollBar().setValue(spectogram.scrollPane.getVerticalScrollBar().getMaximum());
            spectogram.panel.repaint();
        });
    }

    private void loadNewAudio() throws IOException, InterruptedException {
        fileManager = new FileManager(fileName);
        spectogram = new Spectogram(fileManager.getAudioStream());
        renderSpectogram(spectogram);
    }

    private ImageIcon createIcon(String imagePath, int width, int height) {
        ImageIcon originalIcon = new ImageIcon(imagePath);
        Image scaledImage = originalIcon.getImage().getScaledInstance(width, height, Image.SCALE_SMOOTH);
        ImageIcon resizedIcon = new ImageIcon(scaledImage);
        return resizedIcon;
    }

    private void renderSpectogram(Spectogram spectogram) throws InterruptedException {
        spectogram.renderImage(frame);
        frame.add(spectogram.getPanel(), BorderLayout.CENTER);
        frame.setVisible(true);
    }
}
