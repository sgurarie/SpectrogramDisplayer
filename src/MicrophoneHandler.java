import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.TargetDataLine;
import java.io.ByteArrayOutputStream;

public class MicrophoneHandler {

    TargetDataLine line;
    DataLine.Info info;

    ByteArrayOutputStream out  = new ByteArrayOutputStream();
    byte[] data;
    int numBytesRead;
    AudioDataStream dataStream;

    public MicrophoneHandler() {

        info = new DataLine.Info(TargetDataLine.class, AudioDataStream.format); // format is an AudioFormat object
        if (!AudioSystem.isLineSupported(info)) {
            System.out.println("Format not supported");
        }

        try {
            line = (TargetDataLine) AudioSystem.getLine(info);
            line.open(AudioDataStream.format);
            data = new byte[line.getBufferSize() / 5];
        } catch (LineUnavailableException ex) {
            System.out.println("Line unavailable: " + ex.getMessage());
        }
    }

    public void startRecording() {


        line.start();
        Thread newThread = new Thread(() -> {

           do {
                numBytesRead = line.read(data, 0, data.length);
                out.write(data, 0, numBytesRead);
            } while(line.isActive());
        });
        newThread.start();
    }

    public void stopRecording() {
        line.stop();
        line.flush();
        dataStream = new AudioDataStream((int) (out.size() * 1000 / AudioDataStream.format.getSampleRate() / 2), out.size() / 2);
        dataStream.buffer = out.toByteArray();

    }

    public AudioDataStream getAudioStream() {
        return dataStream;
    }
}
