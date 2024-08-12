package ru.dmitry.anokhin.zx.tapplayer;

import lombok.Getter;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import java.util.ArrayList;

@Getter
public class TapFile {

    private byte[] data;
    List<TapBlock> blocks = new ArrayList<>();

    public TapFile(String filename) throws IOException {
        File file = new File(filename);
        data = Files.readAllBytes(file.toPath());
        readBlocks();
    }

    private void readBlocks() {
        for(int i=0; i<data.length; i++) {
            int lowbytesize = data[i] & 0xFF;
            int highbytesize = (data[i+1] & 0xFF) << 8;
            int size = lowbytesize | highbytesize;
            int[] blockbytes = new int[size];
            int p=0;
            for(int j=i+2; j<i+2+size; j++) {
                blockbytes[p++] = data[j] & 0xFF;
            }
            blocks.add(new TapBlock(blockbytes));
            i += size + 1;
        }
    }
}
