package com.example.ahorragas.data;

import android.content.Context;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;

public class FileJsonCache {

    private final File jsonFile;
    private final File tsFile;

    public FileJsonCache(Context context, String baseName) {
        File dir = context.getFilesDir();
        this.jsonFile = new File(dir, baseName + ".json");
        this.tsFile = new File(dir, baseName + ".ts");
    }

    public boolean hasCache() {
        return jsonFile.exists() && tsFile.exists() && jsonFile.length() > 0;
    }

    public long readTimestamp() throws Exception {
        String s = readAll(tsFile);
        return Long.parseLong(s.trim());
    }

    public String readJson() throws Exception {
        return readAll(jsonFile);
    }

    public void write(String json) throws Exception {
        writeAll(jsonFile, json);
        writeAll(tsFile, String.valueOf(System.currentTimeMillis()));
    }

    private String readAll(File f) throws Exception {
        try (FileInputStream fis = new FileInputStream(f)) {
            byte[] bytes = new byte[(int) f.length()];
            int read = fis.read(bytes);
            if (read <= 0) return "";
            return new String(bytes, StandardCharsets.UTF_8);
        }
    }

    private void writeAll(File f, String content) throws Exception {
        try (FileOutputStream fos = new FileOutputStream(f, false)) {
            fos.write(content.getBytes(StandardCharsets.UTF_8));
        }
    }
}