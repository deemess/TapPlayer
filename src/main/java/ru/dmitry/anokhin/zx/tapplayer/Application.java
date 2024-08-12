package ru.dmitry.anokhin.zx.tapplayer;

import javax.sound.sampled.*;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class Application {

    public static void main(String[] args) throws Exception {
        // Param for playback (input) device.
        Line.Info playbackLine = new Line.Info(SourceDataLine.class);
        // Param for capture (output) device.
        Line.Info captureLine = new Line.Info(TargetDataLine.class);

        List<Mixer.Info> playbackDevices = filterDevices(playbackLine);
        List<Mixer.Info> captureDevices = filterDevices(captureLine);

        System.out.println("\nPlayback devices:");
        printInfo(playbackDevices);
//        System.out.println("\nCapture devices:");
//        printInfo(captureDevices);

        if(args.length != 3) {
            System.out.println("\nUsage: java -jar tapplayer.jar <output device number> <volume(0..10)> <input file>");
            System.exit(1);
        }

        new Application().start(args[2], playbackDevices.get(Integer.parseInt(args[0])-1), Integer.parseInt(args[1]));
    }

    float volume = 0.1f;

    private void start(String tapfile, Mixer.Info info, int vol) throws LineUnavailableException, IOException, InterruptedException {
        volume = vol / 10f;
        TapFile tap = new TapFile(tapfile);
        AudioFormat af = new AudioFormat(44100, 8, 1, true, false);
        SourceDataLine sdl = AudioSystem.getSourceDataLine(af, info);
        sdl.open(af);
        sdl.start();

        for(TapBlock block: tap.getBlocks()) {
            //pilot pulse
            for(int p=0; p<3000; p++) {
                pilotPulse(sdl);
            }
            syncPulse(sdl); //sync pulse
            //data pulses
            for(int i=0; i<block.getData().length; i++) {
                for(int bits=7; bits>=0; bits--) {
                    if((block.getData()[i] & (1 << bits)) != 0) {
                        onePulse(sdl);
                    } else {
                        zeroPulse(sdl);
                    }
                }
            }

            Thread.sleep(2000);
        }

        sdl.drain();
        sdl.close();
    }

    //zero = (0.25-0.5 ms)
    //one = (0.5-1 ms)
    //pilot = (1-2 ms)


    private void pilotPulse(SourceDataLine sdl) {
        int len = 55;
        byte[] buff = new byte[len];
        for(int i=0; i<len/2; i++) {
            buff[i] = (byte)(120*volume);
        }
        for(int i=len/2; i<len; i++) {
            buff[i] = (byte)(-120*volume);
        }
        sdl.write(buff,0,len);
    }

    private void onePulse(SourceDataLine sdl) {
        byte[] buff = new byte[43];
        for(int i=0; i<21; i++) {
            buff[i] = (byte)(120*volume);
        }
        for(int i=21; i<43; i++) {
            buff[i] = (byte)(-120*volume);
        }
        sdl.write(buff,0,43);
    }

    private void zeroPulse(SourceDataLine sdl) {
        byte[] buff = new byte[22];
        for(int i=0; i<11; i++) {
            buff[i] = (byte)(120*volume);
        }
        for(int i=11; i<22; i++) {
            buff[i] = (byte)(-120*volume);
        }
        sdl.write(buff,0,22);
    }

    private void syncPulse(SourceDataLine sdl) {
        byte[] buff = new byte[17];
        for(int i=0; i<8; i++) {
            buff[i] = (byte)(120*volume);
        }
        for(int i=8; i<17; i++) {
            buff[i] = (byte)(-120*volume);
        }
        sdl.write(buff,0,17);
    }


    private static void printInfo(List<Mixer.Info> devices) {
        int i=1;
        for (Mixer.Info info : devices) {
            System.out.printf("%d) Name [%s] Description [%s] ", i++, info.getName(), info.getDescription());
            System.out.println(info.getDescription());
        }
    }


    private static List<Mixer.Info> filterDevices(final Line.Info supportedLine) {
        List<Mixer.Info> result = new ArrayList<>();

        for (Mixer.Info info : AudioSystem.getMixerInfo()) {
            Mixer mixer = AudioSystem.getMixer(info);
            if (mixer.isLineSupported(supportedLine)) {
                result.add(info);
            }
        }
        return result;
    }

}
