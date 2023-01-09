package com.minima.android.ui.files;

import android.Manifest;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.SubMenu;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.FileProvider;
import androidx.documentfile.provider.DocumentFile;
import androidx.fragment.app.Fragment;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.minima.android.MainActivity;
import com.minima.android.R;

import org.minima.Minima;
import org.minima.utils.MiniFile;
import org.minima.utils.MiniFormat;
import org.minima.utils.MinimaLogger;
import org.minima.utils.json.JSONArray;
import org.minima.utils.json.JSONObject;
import org.minima.utils.json.parser.JSONParser;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class FilesFragment extends Fragment {

    MainActivity mMain;
    View mRoot;

    ListView mFileList;

    String          mBaseDir;
    JSONObject[]    mFiles;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {

        setHasOptionsMenu(true);

        View root = inflater.inflate(R.layout.fragment_files, container, false);

        mFileList = root.findViewById(R.id.files_list);

        //If it's Empty
        mFileList.setEmptyView(root.findViewById(R.id.files_empty_list_item));

        mFileList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int zPosition, long l) {

                JSONObject file  = mFiles[zPosition];
                boolean isdir    = (boolean)file.get("isdir");

                MinimaLogger.log("FILE CHOSEN : "+file.toString());

                if(isdir){
                    //Load the new files..
                    String abs = (String)file.get("absolute");
                    loadFiles(abs);
                }else{

                    //Do you want to downlaod..


                }

            }
        });

        //Register for Context menu
        registerForContextMenu(mFileList);

        FloatingActionButton fab = root.findViewById(R.id.fab_files);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mMain.openFile(MainActivity.REQUEST_COPYFILE_TO_INTERNAL);
            }
        });

        //Get the Main Activity
        mMain = (MainActivity)getActivity();

        mRoot = root;

        loadFiles(mMain.getFilesDir().getAbsolutePath());

        mMain.mFileFragment = this;

        //Make sure we have write permission
        mMain.checkPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE, mMain.REQUEST_WRITEPERMISSIONS);

        return root;
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
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
                Uri origuri = FileProvider.getUriForFile(mMain,"com.minima.android.provider",orig);

                //Now share that file..
                Intent intentShareFile = new Intent(Intent.ACTION_SEND);
                intentShareFile.setType("application/zip");
                intentShareFile.putExtra(Intent.EXTRA_STREAM, origuri);
                intentShareFile.putExtra(Intent.EXTRA_SUBJECT,name);
                intentShareFile.putExtra(Intent.EXTRA_TEXT, "Share this file..");
                intentShareFile.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

                Intent chooser = Intent.createChooser(intentShareFile, "Share File");

                List<ResolveInfo> resInfoList = mMain.getPackageManager().queryIntentActivities(chooser, PackageManager.MATCH_DEFAULT_ONLY);
                for (ResolveInfo resolveInfo : resInfoList) {
                    String packageName = resolveInfo.activityInfo.packageName;
                    mMain.grantUriPermission(packageName, origuri, Intent.FLAG_GRANT_WRITE_URI_PERMISSION | Intent.FLAG_GRANT_READ_URI_PERMISSION);
                }

                startActivity(chooser);
            }

        }else if(item.getTitle().equals("Delete")){
            Toast.makeText(mMain,"DELETE "+name, Toast.LENGTH_SHORT).show();

            //Delete this file..
            MiniFile.deleteFileOrFolder(mMain.getFilesDir().getAbsolutePath(),orig);

            //Reload..
            loadFiles();
        }

        return true;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        menu.clear();
        super.onCreateOptionsMenu(menu, inflater);
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
        String basepath = mMain.getFilesDir().getAbsolutePath();
        if(!zPath.equals(basepath)){
            File parent = base.getParentFile();
            JSONObject par = constructJSON(parent);
            par.put("name","..");
            allfiles.add(0, par);
        }

        mFiles = allfiles.toArray(new JSONObject[0]);

        //Set this..
        FilesAdapter fadap = new FilesAdapter(mMain, R.layout.mds_view, mFiles);

        mFileList.setAdapter(fadap);
    }

    public JSONObject constructJSON(File zFile){
        JSONObject filejson = new JSONObject();

        filejson.put("absolute", zFile.getAbsolutePath());
        filejson.put("name", zFile.getName());
        if(zFile.isFile()){
            filejson.put("size", zFile.length());
        }else{
            filejson.put("size", 0);
        }
        filejson.put("name", zFile.getName());
        filejson.put("isdir", zFile.isDirectory());

        return filejson;
    }
}