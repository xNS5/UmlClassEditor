package com.nathaniel.motus.umlclasseditor.controller;

import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.graphics.pdf.PdfDocument;
import android.graphics.pdf.PdfDocument.*;
import android.net.Uri;
import android.text.Layout;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;

import com.nathaniel.motus.umlclasseditor.model.UmlProject;
import com.nathaniel.motus.umlclasseditor.R;
import com.nathaniel.motus.umlclasseditor.view.GraphView;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Objects;

public class IOUtils{

    private IOUtils() {}


    public static void saveFileToInternalStorage(String data,File file) {
        try {
            FileWriter fileWriter=new FileWriter(file);
            fileWriter.append(data);
            fileWriter.flush();
            fileWriter.close();
            } catch (IOException e) {
            Log.i("TEST","Saving failed");
        }
    }

    public static String getFileFromInternalStorage(File file) {
        // Using a StringBuilder instead of a String
        StringBuilder projectString = new StringBuilder();
        if (file.exists()) {
            BufferedReader bufferedReader;
            try {
                bufferedReader = new BufferedReader(new FileReader(file));
                try {
                    String readString = bufferedReader.readLine();
                    while (readString != null) {
                        projectString.append(readString);
                        readString = bufferedReader.readLine();
                    }
                } finally {
                    bufferedReader.close();
                }
            } catch (IOException e) {
                Log.i("TEST","Loading failed");
            }
        }
        return projectString.toString();
    }

    public static void saveFileToExternalStorage(Context context, String data, Uri externalStorageUri) {
        try {
            OutputStream outputStream=context.getContentResolver().openOutputStream(externalStorageUri);
            outputStream.write(data.getBytes());
            outputStream.flush();
            outputStream.close();
            Log.i("TEST", "Project saved");
        } catch (IOException e) {
            Log.i("TEST","Failed saving project");
            Log.i("TEST", Objects.requireNonNull(e.getMessage()));
        }
    }

    public static void savePdfToExternalStorage(Context context, GraphView graphView, Uri externalStorageUri) {

        Bitmap main_bitmap = getBitmapFromView(graphView);
        PdfDocument document = new PdfDocument();
        PageInfo pageInfo = new PageInfo.Builder(main_bitmap.getWidth(), main_bitmap.getHeight(), 1).create();
        Page page = document.startPage(pageInfo);

        Canvas canvas = page.getCanvas();
        Paint paint = new Paint();
        paint.setColor(Color.WHITE);
        canvas.drawPaint(paint);

        Bitmap secondary_bitmap = Bitmap.createScaledBitmap(main_bitmap, main_bitmap.getWidth(), main_bitmap.getHeight(), true);

        paint.setColor(Color.BLACK);
        canvas.drawBitmap(secondary_bitmap, 0, 0, null);

        document.finishPage(page);

        try{
            document.writeTo(context.getContentResolver().openOutputStream(externalStorageUri));
            Log.i("TEST", "Project saved");
        } catch (IOException e) {
            Log.e("TEST","Failed saving project");
            Log.e("TEST", Objects.requireNonNull(e.getMessage()));
        } finally {
            document.close();
        }

    }

    private static Bitmap getBitmapFromView(View view) {
        if (view == null) return null;
        boolean drawingCacheEnabled = view.isDrawingCacheEnabled();
        boolean willNotCacheDrawing = view.willNotCacheDrawing();
        view.setDrawingCacheEnabled(true);
        view.setWillNotCacheDrawing(false);
        Bitmap drawingCache = view.getDrawingCache();
        Bitmap bitmap;
        if (null == drawingCache) {
            view.measure(View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
                    View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));
            view.layout(0, 0, view.getMeasuredWidth(), view.getMeasuredHeight());
            view.buildDrawingCache();
            drawingCache = view.getDrawingCache();
            if (drawingCache != null) {
                bitmap = Bitmap.createBitmap(drawingCache);
            } else {
                bitmap = Bitmap.createBitmap(view.getMeasuredWidth(), view.getMeasuredHeight(), Bitmap.Config.ARGB_8888);
                Canvas canvas = new Canvas(bitmap);
                view.draw(canvas);
            }
        } else {
            bitmap = Bitmap.createBitmap(drawingCache);
        }
        view.destroyDrawingCache();
        view.setWillNotCacheDrawing(willNotCacheDrawing);
        view.setDrawingCacheEnabled(drawingCacheEnabled);
        return bitmap;

    }


    public static String readFileFromExternalStorage(Context context, Uri externalStorageUri) {
        String data="";
        try {
            InputStream inputStream = context.getContentResolver().openInputStream(externalStorageUri);
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
            data = bufferedReader.readLine();
            Log.i("TEST","Project loaded");
        } catch (IOException e) {
            Log.i("TEST","Failed loading project");
        }

        return data;
    }

    public static ArrayList<String> sortedFiles(File[] file) {
        // Checks to make sure that the file array isn't null
       if(file != null){
           ArrayList<String> fileList =new ArrayList<>();
           for (File f:file) fileList.add(f.getName());
           Collections.sort(fileList);
           return fileList;
       }
       return new ArrayList<>();
    }

    public static String readRawHtmlFile(Context context,int rawId) {
        InputStream inputStream=context.getResources().openRawResource(rawId);
        ByteArrayOutputStream byteArrayOutputStream=new ByteArrayOutputStream();
        int i;
        try {
            i = inputStream.read();
            while (i != -1) {
                byteArrayOutputStream.write(i);
                i = inputStream.read();
            }
            inputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return byteArrayOutputStream.toString();
    }

//    **********************************************************************************************
//    Side utilities
//    **********************************************************************************************

    public static int getAppVersionCode(Context context) {
        PackageManager manager=context.getPackageManager();
        try {
            PackageInfo info = manager.getPackageInfo(context.getPackageName(),0);
            return info.versionCode;
        } catch (PackageManager.NameNotFoundException e) {
            return -1;
        }
    }
}
