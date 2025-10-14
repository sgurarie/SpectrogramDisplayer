import javax.sound.sampled.*;
import java.io.ByteArrayInputStream;
import java.io.IOException;

public class AudioDataStream {

    public byte[] buffer;
    private int durationMilliseconds;
    public int samplingRate; //22050 Hz
    private int samples;
    ByteArrayInputStream stream;
    public static AudioFormat format = new AudioFormat(44100, 16, 1, true, true);;
    AudioInputStream audio;
    Clip clip;

    public AudioDataStream(int durationMilliseconds) {
        this.durationMilliseconds = durationMilliseconds;
        samples = durationMilliseconds * samplingRate / 1000; // multiplied by 2 cus we r storing "short" data type in big endian format in byte array
        buffer = new byte[samples]; //sample at 2 times the maximum frequency to prevent aliasing
    }

    public AudioDataStream(int durationMilliseconds, int samples) {
        this.durationMilliseconds = durationMilliseconds;
        this.samples = samples * 2; // multiplied by 2 cus we r storing "short" data type in big endian format in byte array
        buffer = new byte[samples * 2];
    }

    public void setSamplingRate(int samplingRate, boolean signed) {
        this.samplingRate = samplingRate;
        format = new AudioFormat(samplingRate, 16, 1, signed, true);
    }

    public int getDurationMilliseconds() {
        return durationMilliseconds;
    }

    public int getSamples() {
        return samples;
    }

    public int getSampleRate() {
        return samplingRate;
    }

    public void setIndexAmplitude(short amplitude, int index) {
        buffer[2 * index + 1] = (byte) (amplitude & 0xFF);
        buffer[2 * index] = (byte) ((amplitude >> 8) & 0xFF);
    }

    public short getIndexAmplitude(int index) {
        short convert = (short) (buffer[2 * index] & 0xFF);
        short convert2 = (short) (buffer[2 * index + 1] & 0xFF);

        short amount = 8;

        return (short)  ((convert2) | (short) (convert << amount));
    }

    public void initializeAudio() {
        stream = new ByteArrayInputStream(buffer);
        audio = new AudioInputStream(stream, format, stream.available());
        try {
            clip = AudioSystem.getClip();
            clip.open(audio);
            initialized = true;
        } catch (IOException | LineUnavailableException e) {
            throw new RuntimeException(e);
        }
    }

    public boolean playing = false, initialized = false;

    public void playSound() {
        try {
            clip.start();
            while (!clip.isRunning()) {
                Thread.sleep(10);
            }
            playing = true;

            while (clip.isRunning()) {

                Thread.sleep(10);
            }
            playing = false;
            clip.stop();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    public void pauseSound() {
        if(playing)
            clip.stop();
        playing = false;
    }

    public void playSoundInSeperateThread() {
        Thread thread = new Thread(new Runnable() {
            public void run() {
                try {
                    clip.start();
                    while (!clip.isRunning()) {
                        Thread.sleep(10);
                    }
                    playing = true;

                    while (clip.isRunning()) {
                        Thread.sleep(10);
                    }

                    playing = false;
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        });

        thread.start();
    }
}
