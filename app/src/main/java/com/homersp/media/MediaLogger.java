package com.homersp.media;

import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

/**
 * Created by homer on 26/09/14.
 */
public class MediaLogger {
    private static MediaLogger sInstance = null;

    public static MediaLogger instance() {
        if(sInstance == null)
            sInstance = new MediaLogger();

        return sInstance;
    }

    private static final boolean LOG_TO_MAIN = true;

    private FileWriter mWriter = null;

    public MediaLogger() {
        /*File castTestDir = new File(Environment.getExternalStorageDirectory(), "/Android/data/com.homersp.casttest/files");
        if(castTestDir.exists()) {
            try {
                castTestDir.mkdirs();
                mWriter = new FileWriter(new File(Environment.getExternalStorageDirectory(), "/Android/data/com.homersp.casttest/files/drm_output.log"));
                mWriter.write("Starting log");

                return;
            } catch (IOException e) {
            }
        }

        File mediaShellDir = new File(Environment.getExternalStorageDirectory(), "/Android/data/com.google.android.apps.mediashell/files");
        if(mediaShellDir.exists()) {
            try {
                mediaShellDir.mkdirs();
                mWriter = new FileWriter(new File(Environment.getExternalStorageDirectory(), "/Android/data/com.google.android.apps.mediashell/files/drm_output.log"));
                mWriter.write("Starting log");

                return;
            } catch(IOException e2) {
                e2.printStackTrace();
            }
        }*/
        mWriter = null;
    }

    public void close() {
        if(mWriter != null) {
            try {
                mWriter.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void log(String tag, String data) {
        if(mWriter != null) {
            try {
                mWriter.write("--" + tag + "--");
                mWriter.write("\n");
                mWriter.write(data);
                mWriter.write("\n\n\n");
                mWriter.flush();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        if(mWriter == null || LOG_TO_MAIN) {
            Log.d("MediaLogger.HOMERSP", tag + " - " + data);
        }
    }
}
