package com.zjh.btim.Util;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.StrictMode;
import android.widget.Toast;

import java.io.File;

public class OpenFileUtil {
    private static final String[][] MATCH_ARRAY = {
            //{后缀名，    文件类型}
            {".3gp", "video/3gpp"},
            {".apk", "application/vnd.android.package-archive"},
            {".asf", "video/x-ms-asf"},
            {".avi", "video/x-msvideo"},
            {".bin", "application/octet-stream"},
            {".bmp", "image/bmp"},
            {".c", "text/plain"},
            {".class", "application/octet-stream"},
            {".conf", "text/plain"},
            {".cpp", "text/plain"},
            {".doc", "application/msword"},
            {".docx", "application/msword"},
            {".xls", "application/msword"},
            {".xlsx", "application/msword"},
            {".exe", "application/octet-stream"},
            {".gif", "image/gif"},
            {".gtar", "application/x-gtar"},
            {".gz", "application/x-gzip"},
            {".h", "text/plain"},
            {".htm", "text/html"},
            {".html", "text/html"},
            {".jar", "application/java-archive"},
            {".java", "text/plain"},
            {".jpeg", "image/jpeg"},
            {".jpg", "image/jpeg"},
            {".js", "application/x-javascript"},
            {".log", "text/plain"},
            {".m3u", "audio/x-mpegurl"},
            {".m4a", "audio/mp4a-latm"},
            {".m4b", "audio/mp4a-latm"},
            {".m4p", "audio/mp4a-latm"},
            {".m4u", "video/vnd.mpegurl"},
            {".m4v", "video/x-m4v"},
            {".mov", "video/quicktime"},
            {".mp2", "audio/x-mpeg"},
            {".mp3", "audio/x-mpeg"},
            {".mp4", "video/mp4"},
            {".mpc", "application/vnd.mpohun.certificate"},
            {".mpe", "video/mpeg"},
            {".mpeg", "video/mpeg"},
            {".mpg", "video/mpeg"},
            {".mpg4", "video/mp4"},
            {".mpga", "audio/mpeg"},
            {".msg", "application/vnd.ms-outlook"},
            {".ogg", "audio/ogg"},
            {".pdf", "application/pdf"},
            {".png", "image/png"},
            {".pps", "application/vnd.ms-powerpoint"},
            {".ppt", "application/vnd.ms-powerpoint"},
            {".prop", "text/plain"},
            {".rar", "application/x-rar-compressed"},
            {".rc", "text/plain"},
            {".rmvb", "audio/x-pn-realaudio"},
            {".rtf", "application/rtf"},
            {".sh", "text/plain"},
            {".tar", "application/x-tar"},
            {".tgz", "application/x-compressed"},
            {".txt", "text/plain"},
            {".wav", "audio/x-wav"},
            {".wma", "audio/x-ms-wma"},
            {".wmv", "audio/x-ms-wmv"},
            {".wps", "application/vnd.ms-works"},
            {".xml", "text/plain"},
            {".z", "application/x-compress"},
            {".zip", "application/zip"},
            {"", "*/*"}
    };

    /**
     * Open file by path
     *
     * @param context Context
     * @param path    File path
     */
    public static void openFileByPath(Context context, String path) {
        if (context == null || path == null)
            return;
        Intent intent = new Intent();
        //Set the Action property of the intent
        intent.setAction(Intent.ACTION_VIEW);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.addCategory(Intent.CATEGORY_DEFAULT);

        //Type of file
        String type = "";
        for (int i = 0; i < MATCH_ARRAY.length; i++) {
            //Determine the format of the file
            if (path.contains(MATCH_ARRAY[i][0])) {
                type = MATCH_ARRAY[i][1];
                break;
            }
        }
        try {
            File out = new File(path);
            Uri fileURI;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                // As file access permissions after 7.0, you can request them in androidmanifest by defining xml, or you can just skip the permissions
                // Apply in androidmanifest by defining xml
//                fileURI = FileProvider.getUriForFile(context,
//                        "com.lonelypluto.zyw_test.provider",
//                        out);
                // Skip permissions directly
                StrictMode.VmPolicy.Builder builder = new StrictMode.VmPolicy.Builder();
                StrictMode.setVmPolicy(builder.build());
                fileURI = Uri.fromFile(out);
            } else {
                fileURI = Uri.fromFile(out);
            }
            //Set the data and Type properties of the intent
            intent.setDataAndType(fileURI, type);
            //Jump
            if (context.getPackageManager().resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY) != null) {
                context.startActivity(intent);
            } else {
                Toast.makeText(context, "Aucun programme correspondant n'a été trouvé", Toast.LENGTH_SHORT).show();
            }

        } catch (Exception e) { //Lorsque le système ne contient pas le fichier permettant d'ouvrir le logiciel, le message suivant s'affiche
            Toast.makeText(context, "Impossible d'ouvrir le fichier dans ce format", Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }
    }
}
