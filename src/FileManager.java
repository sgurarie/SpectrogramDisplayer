import javax.sound.sampled.*;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Arrays;

public class FileManager {
    private AudioInputStream stream;
    private AudioDataStream target;
    private byte[] buffering;

    private void read(int sampleSize, boolean isBigEndian, int channels) throws IOException {

        if((sampleSize != 1 && sampleSize != 2 && sampleSize != 4) || channels > 2) {
            throw new IllegalArgumentException("Unsupported format\nYou provided sample size: " + sampleSize + ", channels: " + channels);
        }

        if(channels == 2) {
            sampleSize /= 2;
        }

        for(int i = 0; i < buffering.length; i += sampleSize) {
            byte[] next = Arrays.copyOfRange(buffering, i, i + sampleSize * channels);
            int buffer = 0;
            for(int j = 0; j < sampleSize; j++) {
                if (isBigEndian) {
                    buffer += (next[j] & 0xFF) << ((sampleSize - j - 1) * 8);
                } else {
                    buffer += (next[j] & 0xFF) << (j * 8);
                }
            }

            if(sampleSize == 4) {
                buffer = (int) (buffer * ((double) Short.MAX_VALUE / Integer.MAX_VALUE));
            }

            target.setIndexAmplitude((short) buffer, i / (sampleSize * channels));
            if(channels == 2) {
                i += sampleSize;
            }
        }

        buffering = null;
    }

    public static File opening(String fileName) {
        return new File(System.getProperty("user.dir")  + "/" + fileName).getAbsoluteFile();
    }

    FileManager(String fileName) throws IOException {
        System.out.println("Current directory: " + System.getProperty("user.dir"));
        try {
            stream = AudioSystem.getAudioInputStream(opening(fileName));
        } catch (UnsupportedAudioFileException | IOException e) {
            throw new RuntimeException(e);
        }

        buffering = stream.readAllBytes();

        target = new AudioDataStream((int) (buffering.length * 1000.0 / (stream.getFormat().getSampleRate() * 2)), buffering.length);
        target.setSamplingRate((int) stream.getFormat().getSampleRate(), true);
        read(stream.getFormat().getFrameSize(), stream.getFormat().isBigEndian(), stream.getFormat().getChannels());
        stream.close();
    }

    public AudioDataStream getAudioStream() {
        return target;
    }

}
