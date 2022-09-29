package com.minima.android.dependencies.backupSync.minima;

import android.content.Context;
import android.widget.Toast;

import androidx.annotation.Nullable;

import com.minima.android.service.MinimaService;

import org.minima.Minima;
import org.minima.utils.MinimaLogger;

import java.io.File;

public class MinimaBackupUtils {
   @Nullable
   public static File createBackup(Context context) {

      final Minima minima = MinimaService.minima;
      if (minima != null) {
         //Where are we going to store the file
         String filename = "minima-backup-gdrive.bak";
         File backup = new File(context.getFilesDir(), filename);
         if (backup.exists()) {
            backup.delete();
         }

         //First run a command On Minima..
         String result = minima.runMinimaCMD("backup file:" + backup.getAbsolutePath());

         //Now share this..
         //MinimaLogger.log("Backup : " + result);

         if (backup.exists()) {
            return backup;
         } else {
            return null;
         }
      } else {
         return null;
      }
   }
}
