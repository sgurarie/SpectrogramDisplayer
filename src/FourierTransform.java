import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;

public class FourierTransform {

    public double[][][] domain; //Index 1 is frequency, index 2 is time, index 3 is cos if 0 and sin if 1
    public double[] zeroFrequency, nOverTwoFreq;
    int millisecondDuration;
    double minFrequency, maxFrequency;
    int windowMillis;
    static int sampleSize = 2048;

    public FourierTransform(int millisecondDuration, int samplingRate) {
        setup(millisecondDuration, samplingRate);
    }

    public void setup(int millisecondDuration, int samplingRate) {
        this.millisecondDuration = millisecondDuration;
        this.minFrequency = (double) samplingRate / sampleSize;
        this.maxFrequency = (double) samplingRate / 2;
        this.windowMillis = (int) (1000.0 / minFrequency);
        this.domain = new double[(int) (maxFrequency / minFrequency - 1)][sampleSize][2];
        zeroFrequency = new double[sampleSize];
        nOverTwoFreq = new double[sampleSize];
    }

    public AudioDataStream synthesis() {
        AudioDataStream audio = new AudioDataStream(millisecondDuration);

        for(int j = 0; j < domain[0].length; j++) { //j is orthogonal frequency id where 0 is minFrequency, 1 is 2 * minFreq, 2 is 3 * minFreq
            for (int k = j * (audio.getSampleRate() * windowMillis / 1000), o = 0; k < (j + 1) * (audio.getSampleRate() * windowMillis / 1000); k++, o++) {
                double sum = zeroFrequency[j];
                sum += nOverTwoFreq[j];
                for(int i = 0; i < domain.length; i++) {
                    double argument = 2 * Math.PI * o * (i + 1) / (audio.getSampleRate() * windowMillis / 1000.0);
                    sum += domain[i][j][0] * Math.cos(argument);
                    sum += domain[i][j][1] * Math.sin(argument);
                }

                audio.setIndexAmplitude((short) sum, k);
            }
        }
        return audio;
    }

    public void fourierTransform(AudioDataStream audio) { //n^2 time complexity
        setup(audio.getDurationMilliseconds(), audio.getSampleRate());

        for(int j = 0; j < domain[0].length; j++) {
            for(int i = 0; i < domain.length; i++) {


                double sumSin = 0;
                double sumCos = 0;

                for(int k = j * (audio.getSampleRate() * windowMillis / 1000), o = 0; k < (j + 1) * (audio.getSampleRate() * windowMillis / 1000); k++, o++) {
                    double argument = 2 * Math.PI * o * (i + 1) / (audio.getSampleRate() * windowMillis / 1000.0);
                    sumSin += audio.getIndexAmplitude(k) * Math.sin(argument);
                    sumCos += audio.getIndexAmplitude(k) * Math.cos(argument);
                }

                domain[i][j][0] = ((2000.0 / (windowMillis * audio.getSampleRate())) * sumCos);
                domain[i][j][1] = ((2000.0 / (windowMillis * audio.getSampleRate())) * sumSin);

            }

            double zeroSum = 0;
            double nOverTwoSum = 0;

            for(int k = j * (audio.getSampleRate() * windowMillis / 1000), o = 0; k < (j + 1) * (audio.getSampleRate() * windowMillis / 1000); k++, o++) {
                zeroSum += audio.getIndexAmplitude(k);
                double argument = 2 * Math.PI * o * (domain.length + 1) / (audio.getSampleRate() * windowMillis / 1000.0);
                nOverTwoSum += audio.getIndexAmplitude(k) * Math.cos(argument);
            }

            zeroFrequency[j] = ((1000.0 / (windowMillis * audio.getSampleRate())) * zeroSum);
            nOverTwoFreq[j] = ((1000.0 / (windowMillis * audio.getSampleRate())) * nOverTwoSum);
        }
    }

    public static Complex[] fft(double[] x) { //nlogn time complexity
        int n = x.length;

        if ((n & (n - 1)) != 0) {
            throw new IllegalArgumentException("Input size must be a power of 2");
        }

        if (n == 1) {
            return new Complex[]{new Complex(x[0], 0)};
        }

        double[] even = new double[n / 2];
        double[] odd = new double[n / 2];

        for (int i = 0; i < n / 2; i++) {
            even[i] = x[2 * i];
            odd[i] = x[2 * i + 1];
        }

        Complex[] Xeven = fft(even);
        Complex[] Xodd = fft(odd);

        Complex[] result = new Complex[n];
        for (int k = 0; k < n / 2; k++) {
            double angle = -2 * Math.PI * k / n;
            Complex t = new Complex(Math.cos(angle), Math.sin(angle)).multiply(Xodd[k]);
            result[k] = Xeven[k].add(t);
            result[k + n / 2] = Xeven[k].subtract(t);
        }

        return result;
    }

    public static Complex[] fft(Complex[] x) {
        int n = x.length;

        if ((n & (n - 1)) != 0) {
            throw new IllegalArgumentException("Input size must be a power of 2");
        }

        if (n == 1) {
            return new Complex[]{x[0]};
        }

        Complex[] even = new Complex[n / 2];
        Complex[] odd = new Complex[n / 2];
        for (int i = 0; i < n / 2; i++) {
            even[i] = x[2 * i];
            odd[i] = x[2 * i + 1];
        }

        Complex[] Xeven = fft(even);
        Complex[] Xodd = fft(odd);

        Complex[] result = new Complex[n];
        for (int k = 0; k < n / 2; k++) {
            double angle = -2 * Math.PI * k / n;
            Complex t = new Complex(Math.cos(angle), Math.sin(angle)).multiply(Xodd[k]);
            result[k] = Xeven[k].add(t);
            result[k + n / 2] = Xeven[k].subtract(t);
        }

        return result;
    }

    public static Complex[] ifft(Complex[] X) {
        int n = X.length;

        Complex[] conjugated = new Complex[n];
        for (int i = 0; i < n; i++) {
            conjugated[i] = X[i].conjugate();
        }
        Complex[] fftResult = fft(conjugated);

        Complex[] result = new Complex[n];
        for (int i = 0; i < n; i++) {
            result[i] = fftResult[i].conjugate().scale(1.0 / n);
        }

        return result;
    }


    public static class Complex {
        public double real;
        public double imag;

        public Complex(double real, double imag) {
            this.real = real;
            this.imag = imag;
        }

        public Complex add(Complex other) {
            return new Complex(this.real + other.real, this.imag + other.imag);
        }

        public Complex subtract(Complex other) {
            return new Complex(this.real - other.real, this.imag - other.imag);
        }

        public Complex multiply(Complex other) {
            return new Complex(
                    this.real * other.real - this.imag * other.imag,
                    this.real * other.imag + this.imag * other.real
            );
        }

        public Complex conjugate() {
            return new Complex(this.real, -this.imag);
        }

        public Complex scale(double other) {
            return new Complex(this.real * other, this.imag * other);
        }

        public double magnitude() {
            return Math.sqrt(this.real * this.real + this.imag + this.imag);
        }


        @Override
        public String toString() {
            return String.format("%f + %fi", real, imag);
        }
    }

    public void setFrequency(int frequencyID, int time, short amplitude) {
        domain[frequencyID][time][0] = amplitude;
    }

    public double getAmplitude(int frequencyID, int time) {
        return Math.sqrt(domain[frequencyID][time][0] * domain[frequencyID][time][0] + domain[frequencyID][time][1] * domain[frequencyID][time][1]);
    }

    public int getSize(int frequencyID) {
        return domain[frequencyID].length;
    }
}
