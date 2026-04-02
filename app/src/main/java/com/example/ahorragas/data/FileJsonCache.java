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
        String value = readAll(tsFile);
        return Long.parseLong(value.trim());
    }

    public String readJson() throws Exception {
        return readAll(jsonFile);
    }

    public void write(String json) throws Exception {
        writeAll(jsonFile, json);
        writeAll(tsFile, String.valueOf(System.currentTimeMillis()));
    }

    public void clear() {
        if (jsonFile.exists()) {
            //noinspection ResultOfMethodCallIgnored
            jsonFile.delete();
        }
        if (tsFile.exists()) {
            //noinspection ResultOfMethodCallIgnored
            tsFile.delete();
        }
    }

    private String readAll(File file) throws Exception {
        try (java.io.BufferedReader reader = new java.io.BufferedReader(
                new java.io.InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
            return sb.toString();
        }
    }

    private void writeAll(File file, String content) throws Exception {
        try (FileOutputStream fos = new FileOutputStream(file, false)) {
            fos.write(content.getBytes(StandardCharsets.UTF_8));
        }
    }
}
