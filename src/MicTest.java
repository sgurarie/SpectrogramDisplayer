import javax.sound.sampled.*;

public class MicTest {
    public static void main(String[] args) throws Exception {
        AudioFormat format = new AudioFormat(44100, 16, 1, true, false);
        DataLine.Info info = new DataLine.Info(TargetDataLine.class, format);

        TargetDataLine line = (TargetDataLine) AudioSystem.getLine(info);
        line.open(AudioDataStream.format);
        line.start();

        System.out.println("Line started. Waiting...");

        Thread.sleep(500);

        System.out.println("isOpen: " + line.isOpen());
        System.out.println("isRunning: " + line.isRunning());
        System.out.println("isActive: " + line.isActive());

        byte[] buffer = new byte[512];
        int read = line.read(buffer, 0, buffer.length);

        System.out.println("Bytes read: " + read);
        System.out.println("isRunning after read: " + line.isRunning());
        System.out.println("isActive after read: " + line.isActive());

        line.stop();
        line.close();
    }
}