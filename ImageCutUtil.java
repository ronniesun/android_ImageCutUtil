
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

\/**
 * Created by ronnie on 2016/5/17.
 */
public class ImageCutUtil {
    /**
     * @param srcPath 原图路径
     * @param resPath 结果路径
     */
    public static boolean cut(String srcPath, String resPath) {
        int minWidth = 640; // SCREEN_WIDTH
        int minHeight = 1136; // SCREEN_HEIGHT
//      int minWidth = 720; // SCREEN_WIDTH
//      int minHeight = 1280; // SCREEN_HEIGHT
        return cut(srcPath, resPath, minWidth, minHeight);
    }

    /**
     * @param srcPath 原图路径
     * @param resPath 结果路径
     */
    public static boolean cut(String srcPath, String resPath, int minWidth, int minHeight) {
        boolean flag = false;
        if (srcPath == null || "".equals(srcPath) || !new File(srcPath).exists()) {
            return flag;
        }

        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(srcPath, options);

        // 是否需要压缩
        if (options.outHeight > options.outWidth) {
            // 竖图
            if (options.outHeight <= minHeight && options.outWidth <= minWidth) {
                return copyFile(srcPath, resPath);
            }
        } else {
            // 横图
            if (options.outHeight <= minWidth && options.outWidth <= minHeight) {
                return copyFile(srcPath, resPath);
            }
        }

        if (options.outHeight > options.outWidth) {
            // 竖图
            options.inSampleSize = calculateInSampleSize(options, minWidth, minHeight);
        } else {
            // 横图
            options.inSampleSize = calculateInSampleSize(options, minHeight, minWidth);
        }

        options.inPreferredConfig = Bitmap.Config.ARGB_4444;
        options.inJustDecodeBounds = false;

        Bitmap srcBitmap = null;
        try {
            srcBitmap = BitmapFactory.decodeFile(srcPath, options);
        } catch (OutOfMemoryError e) {
            if (srcBitmap != null) {
                srcBitmap.recycle();
            }
            return flag;
        }
        if (srcBitmap == null) {
            return flag;
        }

        // 计算压缩比例和旋转
        int srcWidth = srcBitmap.getWidth();
        int srcHeight = srcBitmap.getHeight();
        Matrix matrix = new Matrix();
        float scaleWidth;
        float scaleHeight;
        if (options.outHeight > options.outWidth) {
            // 竖图
            scaleWidth = ((float) minWidth / srcWidth);
            scaleHeight = ((float) minHeight / srcHeight);
        } else {
            // 横图
            scaleWidth = ((float) minWidth / srcHeight);
            scaleHeight = ((float) minHeight / srcWidth);
        }

        // 等比取大的
        float scale = scaleWidth > scaleHeight ? scaleWidth : scaleHeight;
        matrix.postScale(scale, scale);
        int degree = readPictureDegree(srcPath);
        if (degree != 0) {
            matrix.postRotate(degree);
        }

        Bitmap resBitmap = null;
        try {
            resBitmap = Bitmap.createBitmap(srcBitmap, 0, 0, srcWidth, srcHeight, matrix, true);
        } catch (OutOfMemoryError e) {
            if (resBitmap != null) {
                resBitmap.recycle();
            }
            return flag;
        }
        boolean isSave = saveImage(resBitmap, resPath);
        srcBitmap.recycle();
        return isSave;
    }

    /**
     * @param bitmap
     * @param toPath
     * @return
     */
    private static boolean saveImage(Bitmap bitmap, String toPath) {
        boolean flag = false;
        FileOutputStream fos = null;
        BufferedOutputStream bos = null;
        MyLogUtil.i("toPath============" + toPath);
        try {
            File tofile = new File(toPath);
            File parent = tofile.getParentFile();
            if (!parent.exists()) {
                parent.mkdirs();
            }
            if (!tofile.exists()) {
                tofile.createNewFile();
            }
            fos = new FileOutputStream(toPath);
            bos = new BufferedOutputStream(fos);
            flag = bitmap.compress(Bitmap.CompressFormat.JPEG, 90, bos);
            bos.flush();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (fos != null) {
                try {
                    fos.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (bos != null) {
                try {
                    bos.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (bitmap != null) {
                bitmap.recycle();
            }
        }
        return flag;
    }

    /**
     * @param options
     * @param reqWidth
     * @param reqHeight
     * @return
     */
    private static int calculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight) {
        // Raw height and width of image
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;

        if (height > reqHeight || width > reqWidth) {

            final int halfHeight = height / 2;
            final int halfWidth = width / 2;

            // Calculate the largest inSampleSize value that is a power of 2 and keeps both
            // height and width larger than the requested height and width.
            while ((halfHeight / inSampleSize) > reqHeight
                    && (halfWidth / inSampleSize) > reqWidth) {
                inSampleSize *= 2;
            }
        }

        return inSampleSize;
    }

    /**
     * degree
     *
     * @param imgPath
     * @return
     */
    private static int readPictureDegree(String imgPath) {
        int degree = 0;
        try {
            ExifInterface exifInterface = new ExifInterface(imgPath);
            int orientation = exifInterface.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
            switch (orientation) {
                case ExifInterface.ORIENTATION_ROTATE_90:
                    degree = 90;
                    break;
                case ExifInterface.ORIENTATION_ROTATE_180:
                    degree = 180;
                    break;
                case ExifInterface.ORIENTATION_ROTATE_270:
                    degree = 270;
                    break;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return degree;
    }

    /**
     * 文件复制
     */
    private static boolean copyFile(String fromPath, String toPath) {
        boolean flag = false;
        FileInputStream inStream = null;
        FileOutputStream outStream = null;
        try {
            File fromfile = new File(fromPath);
            if (fromfile.exists()) {
                inStream = new FileInputStream(fromPath);
                File tofile = new File(toPath);
                File parent = tofile.getParentFile();
                if (!parent.exists()) {
                    parent.mkdirs();
                }
                if (!tofile.exists()) {
                    tofile.createNewFile();
                }
                outStream = new FileOutputStream(toPath);
                byte[] buffer = new byte[1024];
                int byteread = 0;
                while ((byteread = inStream.read(buffer)) != -1) {
                    outStream.write(buffer, 0, byteread);
                }
                flag = true;
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (inStream != null) {
                    inStream.close();
                }
                if (outStream != null) {
                    outStream.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return flag;
    }

}