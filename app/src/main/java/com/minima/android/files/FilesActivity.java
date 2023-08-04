package com.minima.android.files;

import android.Manifest;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.OpenableColumns;
import android.provider.Settings;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.Toast;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.minima.android.R;

import org.minima.objects.base.MiniData;
import org.minima.utils.MiniFile;
import org.minima.utils.MinimaLogger;
import org.minima.utils.json.JSONObject;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class FilesActivity extends AppCompatActivity {

    public static int OPEN_FILE_REQUEST     = 90;
    public static int CREATE_FILE_REQUEST   = 80;

    public File mCopyFile = null;

    ListView mFileList;

    String          mBaseDir;
    JSONObject[]    mFiles;

    @RequiresApi(api = Build.VERSION_CODES.R)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_files);

        Toolbar mToolBar = findViewById(R.id.files_toolbar);
        setSupportActionBar(mToolBar);

        mFileList = findViewById(R.id.files_list);

        //If it's Empty
        mFileList.setEmptyView(findViewById(R.id.files_empty_list_item));

        mFileList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int zPosition, long l) {

                JSONObject file  = mFiles[zPosition];
                boolean isdir    = (boolean)file.get("isdir");

                if(isdir){
                    //Load the new files..
                    String abs = (String)file.get("absolute");
                    loadFiles(abs);
                }
            }
        });

        //Register for Context menu
        registerForContextMenu(mFileList);

        //Load main path
        loadFiles(getFilesDir().getAbsolutePath());
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.files_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
            case R.id.file_add:
                openFile();
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        menu.add("Download");
        menu.add("Share");
        menu.add("Delete");
    }

    int mPreviousPos=0;

    @Override
    public boolean onContextItemSelected(MenuItem item) {

        //Get menu item info
        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
        if(info != null){
            mPreviousPos = info.position;
        }
        JSONObject file = mFiles[mPreviousPos];

        //Is it a simple delete
        String name = file.getString("name");
        File orig   = new File(file.getString("absolute"));
        if(item.getTitle().equals("Share")){
            if(orig.exists()) {
                //Get the URi
                Uri origuri = FileProvider.getUriForFile(this,"com.minima.android.provider",orig);

                //Now share that file..
                Intent intentShareFile = new Intent(Intent.ACTION_SEND);
                intentShareFile.setType("application/zip");
                intentShareFile.putExtra(Intent.EXTRA_STREAM, origuri);
                intentShareFile.putExtra(Intent.EXTRA_SUBJECT,name);
                intentShareFile.putExtra(Intent.EXTRA_TEXT, "Share this file..");
                intentShareFile.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

                Intent chooser = Intent.createChooser(intentShareFile, "Share File");

                List<ResolveInfo> resInfoList = getPackageManager().queryIntentActivities(chooser, PackageManager.MATCH_DEFAULT_ONLY);
                for (ResolveInfo resolveInfo : resInfoList) {
                    String packageName = resolveInfo.activityInfo.packageName;
                    grantUriPermission(packageName, origuri, Intent.FLAG_GRANT_WRITE_URI_PERMISSION | Intent.FLAG_GRANT_READ_URI_PERMISSION);
                }

                startActivity(chooser);
            }

        }else if(item.getTitle().equals("Download")){

            //Download the file..
            createFile("*/*",orig);

        }else if(item.getTitle().equals("Delete")){
            confirmDelete(name, orig.getAbsolutePath());
        }

        return true;
    }

    public void confirmDelete(String zName, String zPath){

        new AlertDialog.Builder(this)
                .setTitle("Delete File")
                .setMessage("Are you sure you wish to delete this file ?")
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {

                        //Check is not as root file..
                        File delfile = new File(zPath);
                        String delpath = delfile.getAbsolutePath();
                        if(delpath.endsWith("/")){
                            delpath = delpath.substring(0,delpath.length()-1);
                        }

                        //Check it..
                        File rootfiles  = new File(getFilesDir(),"1.0");
                        String rootpath = rootfiles.getAbsolutePath();
                        if(rootpath.endsWith("/")){
                            rootpath = delpath.substring(0,rootpath.length()-1);
                        }

                        //Check allowed
                        if(delpath.startsWith(rootpath)){
                            Toast.makeText(FilesActivity.this,"NOT ALLOWED to delete these files!", Toast.LENGTH_LONG).show();
                        }else{
                            Toast.makeText(FilesActivity.this,"File deleted.. "+zName, Toast.LENGTH_SHORT).show();
                            MiniFile.deleteFileOrFolder(getFilesDir().getAbsolutePath(),new File(zPath));
                        }

                        //Reload..
                        loadFiles();
                    }})
                .setNegativeButton(android.R.string.no, null).show();
    }

    private void createFile(String mimeType, File zFile) {

        //Store for later
        mCopyFile = zFile;

        //Start a save intent
        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType(mimeType);
        intent.putExtra(Intent.EXTRA_TITLE, zFile.getName());
        startActivityForResult(intent, CREATE_FILE_REQUEST);
    }

    public void loadFiles(){
        loadFiles(mBaseDir);
    }

    public void loadFiles(String zPath){

        mBaseDir     = zPath;
        File base    = new File(zPath);
        File[] files = base.listFiles();

        ArrayList<JSONObject> allfiles = new ArrayList<>();
        if(files != null) {
            for (File file : files) {
                if (!file.isHidden()) {
                    JSONObject ff = constructJSON(file);
                    allfiles.add(ff);
                }
            }
        }

        //Sort Alphabetically..
        Collections.sort(allfiles, new Comparator<JSONObject>() {
            @Override
            public int compare(JSONObject o1, JSONObject o2) {
                String fname1 = o1.getString("name");
                String fname2 = o2.getString("name");

                boolean fdir1 = (boolean)o1.get("isdir");
                boolean fdir2 = (boolean)o2.get("isdir");

                //Directories go first
                if(fdir1 != fdir2){
                    if(fdir1){
                        return -1;
                    }
                    return 1;
                }

                return fname1.compareTo(fname2);
            }
        });

        //Add in the parent folder
        String basepath = getFilesDir().getAbsolutePath();
        if(!zPath.equals(basepath)){
            File parent = base.getParentFile();
            JSONObject par = constructJSON(parent);
            par.put("name","..");
            allfiles.add(0, par);
        }

        mFiles = allfiles.toArray(new JSONObject[0]);

        //Set this..
        FilesAdapter fadap = new FilesAdapter(this, R.layout.mds_view, mFiles);

        mFileList.setAdapter(fadap);
    }

    public JSONObject constructJSON(File zFile){
        JSONObject filejson = new JSONObject();

        filejson.put("absolute", zFile.getAbsolutePath());
        filejson.put("name", zFile.getName());
        if(zFile.isFile()){
            filejson.put("size", zFile.length());
            filejson.put("time", zFile.lastModified());
        }else{
            filejson.put("size", 0);
        }
        filejson.put("name", zFile.getName());
        filejson.put("isdir", zFile.isDirectory());

        return filejson;
    }

    public void openFile() {

        //Open a file
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*");
        /*if(zMimeTypes.length>0) {
            MinimaLogger.log("Set Mime Types : "+ Arrays.toString(zMimeTypes)+" "+zMimeTypes.length);
            intent.putExtra(Intent.EXTRA_MIME_TYPES, zMimeTypes);
        }*/
        intent.addCategory(Intent.CATEGORY_OPENABLE);

        // special intent for Samsung file manager
        Intent sIntent = new Intent("com.sec.android.app.myfiles.PICK_DATA");
        sIntent.putExtra("CONTENT_TYPE", "*/*");
        sIntent.addCategory(Intent.CATEGORY_DEFAULT);

        Intent chooserIntent;
        if (getPackageManager().resolveActivity(sIntent, 0) != null){
            // it is device with Samsung file manager
            chooserIntent = Intent.createChooser(sIntent, "Open file");
            chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, new Intent[] { intent});
        } else {
            chooserIntent = Intent.createChooser(intent, "Open file");
        }

        try {
            startActivityForResult(chooserIntent, OPEN_FILE_REQUEST);
        } catch (android.content.ActivityNotFoundException ex) {
            Toast.makeText(getApplicationContext(), "No suitable File Manager was found.", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onActivityResult (int requestCode,
                                     int resultCode,
                                     Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        //Was anything returned
        if(data == null){
            return;
        }

        if(resultCode == RESULT_CANCELED){
            //Cancelled
        }else if(resultCode == RESULT_OK) {

            //Get the file URI
            Uri fileuri = data.getData();

            if(requestCode == OPEN_FILE_REQUEST) {

                //Get the filename..
                String filename = getFileName(fileuri);

                //MinimaLogger.log("OPENFILE: "+fileuri.getPath()+" "+filename);

                //Create the file
                File newfile = new File(mBaseDir, filename);

                try {
                    //Copy the file
                    copyFileToPrivate(fileuri, newfile);

                    //Reload..
                    loadFiles();

                } catch (Exception exc) {
                    MinimaLogger.log(exc);
                }
            }else if(requestCode == CREATE_FILE_REQUEST) {

                try {
                    OutputStream fileOutupStream = getContentResolver().openOutputStream(fileuri);

                    copyFileFromPrivate(mCopyFile,fileOutupStream);

                } catch (Exception e) {
                    MinimaLogger.log(e);
                }
            }
        }
    }

    public void copyFileToPrivate(Uri zFileURI, File zCopy) throws IOException {

        if(zCopy.exists()){
            zCopy.delete();
        }

        OutputStream os = null;
        InputStream is  = getContentResolver().openInputStream(zFileURI);

        try {
            os = new FileOutputStream(zCopy);
            byte[] buffer = new byte[16384];
            int length;
            while ((length = is.read(buffer)) > 0) {
                os.write(buffer, 0, length);
            }
        } finally {
            is.close();
            os.close();
        }
    }

    public static void copyFileFromPrivate(File zOrig, OutputStream zOut) throws IOException {

        FileInputStream fis = new FileInputStream(zOrig);

        try {
            byte[] buffer = new byte[16384];
            int length;
            while ((length = fis.read(buffer)) > 0) {
                zOut.write(buffer, 0, length);
            }
        } finally {
            zOut.close();
            fis.close();
        }
    }

    public static void copyDataFromPrivate(String zFilename, MiniData zOrig, OutputStream zOut) throws IOException {

        ByteArrayInputStream fis = new ByteArrayInputStream(zOrig.getBytes());

        try {
            byte[] buffer = new byte[16384];
            int length;
            while ((length = fis.read(buffer)) > 0) {
                zOut.write(buffer, 0, length);
            }
        } finally {
            zOut.close();
            fis.close();
        }
    }

    public String getFileName(Uri uri) {
        String result = null;
        if (uri.getScheme().equals("content")) {
            Cursor cursor = getContentResolver().query(uri, null, null, null, null);
            try {
                if (cursor != null && cursor.moveToFirst()) {
                    int row = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                    result = cursor.getString(row);
                }
            } finally {
                cursor.close();
            }
        }
        if (result == null) {
            result = uri.getPath();
            int cut = result.lastIndexOf('/');
            if (cut != -1) {
                result = result.substring(cut + 1);
            }
        }
        return result;
    }
}