package com.example.proiectfinal.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;

public class ImageCompressorHelper {

    // Setăm limitele optime pentru o poză de profil (800x800 e arhi-suficient)
    private static final int MAX_WIDTH = 800;
    private static final int MAX_HEIGHT = 800;
    private static final int COMPRESS_QUALITY = 80; // Calitate 80% (ochiul uman nu vede diferența, dar taie masiv din MB)

    public static byte[] compressImageFromUri(Context context, Uri imageUri) {
        try {
            // 1. Deschidem fișierul original de la acea adresă (Uri)
            InputStream inputStream = context.getContentResolver().openInputStream(imageUri);
            Bitmap originalBitmap = BitmapFactory.decodeStream(inputStream);

            if (inputStream != null) inputStream.close();
            if (originalBitmap == null) return null;

            // 2. Calculăm noile dimensiuni păstrând proporțiile
            int width = originalBitmap.getWidth();
            int height = originalBitmap.getHeight();
            float ratioBitmap = (float) width / (float) height;
            float ratioMax = (float) MAX_WIDTH / (float) MAX_HEIGHT;

            int finalWidth = MAX_WIDTH;
            int finalHeight = MAX_HEIGHT;

            if (ratioMax > ratioBitmap) {
                finalWidth = (int) ((float)MAX_HEIGHT * ratioBitmap);
            } else {
                finalHeight = (int) ((float)MAX_WIDTH / ratioBitmap);
            }

            // 3. Redimensionăm (Tăiem din mărime fizică)
            Bitmap resizedBitmap = Bitmap.createScaledBitmap(originalBitmap, finalWidth, finalHeight, true);

            // 4. Comprimăm (Tăiem din MB/KB)
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            // Îl transformăm în format JPEG la 80% calitate
            resizedBitmap.compress(Bitmap.CompressFormat.JPEG, COMPRESS_QUALITY, baos);

            // GATA! Returnăm poza sub formă de "șir de bytes" (perfectă pentru a fi trimisă pe net)
            return baos.toByteArray();

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}