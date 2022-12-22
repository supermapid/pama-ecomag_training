package com.example.supermap;

import android.content.Context;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Locale;

public class Util {

    Context mContext;
    public Util(Context context){
        this.mContext = context;
    }

    public String convertNumber(double numberVal, String formatPattern) {
        double vc = numberVal;
        if (!formatPattern.isEmpty()) {
            formatPattern = "#,###,###.##";
        }
        NumberFormat nf = NumberFormat.getNumberInstance(Locale.US);
        DecimalFormat formatter = (DecimalFormat) nf;
        formatter.applyPattern(formatPattern);
        return formatter.format(vc);
    }

    public String convertNumberwithoutDecimal(double numberVal, String formatPattern) {
        double vc = numberVal;
        if (!formatPattern.isEmpty()) {
            formatPattern = "#,###,###";
        }
        NumberFormat nf = NumberFormat.getNumberInstance(Locale.US);
        DecimalFormat formatter = (DecimalFormat) nf;
        formatter.applyPattern(formatPattern);
        return formatter.format(vc);
    }

    public void createDirectory(String dirName) {
        File directory = new File(dirName);
        if (!directory.exists()) {
            directory.mkdirs();
        }
    }

    private void CopyRAWtoSDCard(int id, String path) throws IOException {
        InputStream in = mContext.getResources().openRawResource(id);
        FileOutputStream out = new FileOutputStream(path);
        byte[] buff = new byte[1024];
        int read = 0;
        try {
            while ((read = in.read(buff)) > 0) {
                out.write(buff, 0, read);
            }
        } finally {
            in.close();
            out.close();
        }
    }

    public void copyRAWFileifNotExists(int rawFileId, String fileDistName) throws IOException {
        File file = new File(fileDistName);
        //force to update license from package to storage

        if (file.exists()){
            file.deleteOnExit();
        }

        CopyRAWtoSDCard(rawFileId, fileDistName);
    }

    public void copyRAWFileifNotExists2(int rawFileId, String fileDistName) throws IOException {
        File file = new File(fileDistName);
        if (!file.exists()){
            CopyRAWtoSDCard(rawFileId, fileDistName);
        }
    }
}
