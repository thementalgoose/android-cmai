package tmg.cmai;

import android.app.AlertDialog;
import android.app.Fragment;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.Nullable;
import android.text.Html;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;

/**
 * Created by jordan on 25/02/15.
 */
public class AppFragment extends Fragment implements View.OnClickListener {

    String apkID;
    String[] apis;
    MainItem item;

    private TextView title;
    private TextView desc;
    private Button download, install, copy, remove, uncopy;


    public AppFragment() {

    }


    public void setup(MainItem item) {
        this.apkID = item.getApk();
        this.apis = item.getApkList();
        this.item = item;
    }


    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.app_layout, container, false);

        /* Manipulate the V here */
        title = (TextView) v.findViewById(R.id.title);
        desc = (TextView) v.findViewById(R.id.description);
        String titleString = getActivity().getSharedPreferences(MainActivity.sharedPreferencesID, Context.MODE_PRIVATE).getString(apkID + "#TITLE", "null");
        if (!titleString.equals("null")) {
            title.setText(Html.fromHtml("<strong>" + titleString + "</strong>"));

        }

        /* Get dem buttons yo */
        download = (Button) v.findViewById(R.id.download);
        install = (Button) v.findViewById(R.id.install);
        copy = (Button) v.findViewById(R.id.copy);
        remove = (Button) v.findViewById(R.id.remove);
        uncopy = (Button) v.findViewById(R.id.uncopy);
        if (MainActivity.root) {
            copy.setOnClickListener(this);
            uncopy.setOnClickListener(this);
        } else {
            copy.setVisibility(View.GONE);
            uncopy.setVisibility(View.GONE);
        }
        remove.setOnClickListener(this);
        download.setOnClickListener(this);
        install.setOnClickListener(this);

        updateDesc();

        return v;
    }


    public void updateDesc() {
        String descString = getActivity().getSharedPreferences(MainActivity.sharedPreferencesID, Context.MODE_PRIVATE).getString(apkID + "#DESC", "null");
        String[] downloadsApks = item.downloadedAPIs();
        if (downloadsApks.length != 0) {
            descString += "<br/><br/>This app is downloaded in the following versions:";
            for (int i = 0;i < downloadsApks.length; i++) {
                descString += "<br/>- <strong>" + getActivity().getSharedPreferences(MainActivity.sharedPreferencesID, Context.MODE_PRIVATE).getString(downloadsApks[i], "null") + "</strong>";
            }
            install.setVisibility(View.VISIBLE);
            remove.setVisibility(View.VISIBLE);
            if (MainActivity.root)
                copy.setVisibility(View.VISIBLE);
            else
                copy.setVisibility(View.GONE);

        } else {
            descString += "<br/><br/>No versions downloaded";
            install.setVisibility(View.GONE);
            remove.setVisibility(View.GONE);
            copy.setVisibility(View.GONE);
        }

        if (new File("/system/app/" + apkID + ".apk").exists() && MainActivity.root)
            uncopy.setVisibility(View.VISIBLE);
        else
            uncopy.setVisibility(View.GONE);

        if (!descString.equals("null")) {
            desc.setText(Html.fromHtml(descString));
        }
    }


    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.download) {
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            builder.setTitle("Download " + getActivity().getSharedPreferences(MainActivity.sharedPreferencesID, Context.MODE_PRIVATE).getString(apkID + "#TITLE", "null") + " for...");
            CharSequence[] chars = new CharSequence[apis.length];
            for (int i = 0; i < chars.length; i++) {
                chars[i] = getActivity().getSharedPreferences(MainActivity.sharedPreferencesID, Context.MODE_PRIVATE).getString(apis[i], "null");
            }
            builder.setItems(chars, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    DownloadApk dlapk = new DownloadApk();
                    dlapk.execute(apis[which]);
                }
            });
            builder.create().show();
        }
        if (v.getId() == R.id.install) {
            installIntent();
        }
        if (v.getId() == R.id.remove) {
            removeFromSDCard();
        }
        if (v.getId() == R.id.copy) {
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            builder.setTitle("Which downloaded version are you copying?");
            final CharSequence[] downloadedAPKs = item.downloadedAPIs();
            builder.setItems(item.downloadedAPIsPublic(), new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    installChecker(downloadedAPKs[which].toString());
                }
            });
            builder.setNegativeButton("Cancel", null);
            builder.create().show();
        }
        if (v.getId() == R.id.uncopy) {
            removeChecker();
        }
    }


    private void removeFromSDCard() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle("Which downloads do you want to remove?");
        CharSequence[] apisPublic = item.downloadedAPIsPublic();
        final ArrayList selectedItems = new ArrayList();
        builder.setMultiChoiceItems(apisPublic, null, new DialogInterface.OnMultiChoiceClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which, boolean isChecked) {
                if (isChecked)
                    selectedItems.add(which);
                else
                    selectedItems.remove(Integer.valueOf(which));
            }
        });
        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                final String[] apis = item.downloadedAPIs();
                for (int i = 0; i < selectedItems.size(); i++) {
                    // value stored in selectedItems.get(i)
                    removeApp(apis[(Integer) selectedItems.get(i)]);
                }
                updateDesc();
                cleanupFileSystem();
            }
        });
        builder.setNegativeButton("Cancel", null);
        builder.create().show();
    }


    private void removeApp(String appID) {
        File f = new File(Environment.getExternalStorageDirectory() + "/CMApps/" + appID + "/" + apkID + ".apk");
        if (f.exists())
            f.delete();
    }


    private void cleanupFileSystem() {
        String[] apis = getActivity().getSharedPreferences(MainActivity.sharedPreferencesID, Context.MODE_PRIVATE).getString("APIS", "null").split("#");
        for (int i = 0; i < apis.length; i++) {
            File f = new File(Environment.getExternalStorageDirectory() + "/CMApps/" + apis[i]);
            try {
                if (f.listFiles().length == 0)
                    f.delete();
            } catch (NullPointerException e) {
                f.delete();
            }
        }
    }


    class DownloadApk extends AsyncTask<String, Integer, Boolean> {

        ProgressDialog progressDialog;


        @Override
        protected void onPreExecute() {

            /* Initialise Progress dialog, to show progress */
            progressDialog = new ProgressDialog(getActivity());
            progressDialog.setMessage("Downloading apk\nYour download should start in about 5 - 10 seconds. Please don't exit this dialog.");
            progressDialog.setIndeterminate(false);
            progressDialog.setMax(100);
            progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
            progressDialog.setCanceledOnTouchOutside(false);
            progressDialog.setProgressNumberFormat(null);
            progressDialog.show();
        }


        /* After the execution, refresh the selected item */
        @Override
        protected void onPostExecute(final Boolean aBoolean) {
            progressDialog.dismiss();
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (aBoolean) {
                        Toast.makeText(getActivity(), "Download successful", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(getActivity(), "Download unsuccessful. Please try again.", Toast.LENGTH_SHORT).show();
                        Log.i(MainActivity.TAG, "File length doesn't match server length");
                    }
                    updateDesc();
                }
            });
        }


        /* Update the progress dialog */
        @Override
        protected void onProgressUpdate(Integer... values) {
            super.onProgressUpdate(values);
            progressDialog.setProgress(values[0]);

        }

        @Override
        protected Boolean doInBackground(String... params) {
            try {
                String apkId = params[0];
                // apiId contains the "api15" code
                // apiID contains the "Mms" code

                Log.i(MainActivity.TAG, "Starting of download of " + apkID + " for " + apkId);
                String info = getActivity().getSharedPreferences(MainActivity.sharedPreferencesID, Context.MODE_PRIVATE).getString(apkID + "#" + apkId, "null"); // Pass though apkNames[position]
                URL url = new URL(info);
                URLConnection conn = url.openConnection();

                conn.connect();
                int fileLength = conn.getContentLength();

                InputStream input = new BufferedInputStream(url.openStream());
                OutputStream output;
                File f = new File(Environment.getExternalStorageDirectory().toString() + "/CMApps/" + apkId + "/");
                f.mkdirs();
                File saveFile = new File(f, apkID + ".apk");
                output = new FileOutputStream(saveFile);

                byte data[] = new byte[2048];
                int total = 0;
                int count;
                while ((count = input.read(data)) != -1) {
                    total += count;
                     publishProgress((int) (total * 100 / fileLength));
                    output.write(data, 0, count);
                }
                output.flush();
                output.close();
                input.close();
                Log.i(MainActivity.TAG, "File downloaded and stored in " + saveFile);
                Log.i(MainActivity.TAG, "File length: " + saveFile.length() + " / " + fileLength);
                if (saveFile.length() == fileLength) {
                    return true;
                } else {
                    removeFile(saveFile);
                    return false;
                }
            } catch (Exception e) {
                e.printStackTrace();
                Log.i(MainActivity.TAG, "Download error: " + e.toString());
                return false;
            }
        }

        public void removeFile(File file) {
            boolean succ = file.delete();
            Log.i(MainActivity.TAG, "Successfully removed the file : " + file);
        }
    }


    /* Install application intent */
    public void installIntent() {

        // Launch a dialog
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle("Install " + getActivity().getSharedPreferences(MainActivity.sharedPreferencesID, Context.MODE_PRIVATE).getString(apkID + "#TITLE", "null") + " for...");
        final CharSequence[] apis = item.downloadedAPIs();

        if (apis.length != 0) {
            CharSequence[] listItems = new CharSequence[apis.length];
            for (int i = 0; i < listItems.length; i++) {
                listItems[i] = getActivity().getSharedPreferences(MainActivity.sharedPreferencesID, Context.MODE_PRIVATE).getString(apis[i].toString(), "null");
            }

            builder.setItems(listItems, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {

                    // Install code here
                    Intent intent = new Intent(Intent.ACTION_VIEW);
                    intent.setDataAndType(Uri.fromFile(new File(Environment.getExternalStorageDirectory() + "/CMApps/" + apis[which]+ "/" + apkID + ".apk")), "application/vnd.android.package-archive");
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
                }
            });
            builder.create().show();
        } else {
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(getActivity(), "No versions downloaded.", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }


    public void copyToSystem(String api) {
        try {
            Process p = Runtime.getRuntime().exec("su");
            PrintStream stdin = new PrintStream(p.getOutputStream());

            // Establish process
            Log.i(MainActivity.TAG, "Getting SU access to command");
            stdin.println("su");

            // Mount the system as RW
            Log.i(MainActivity.TAG, "Remounting your /system partition as Read-Write");
            stdin.println("mount -o remount,rw -t yaffs2 /dev/block/mtdblock3 /system");

            // Determine if the application already exists. If so, create backup
            Log.i(MainActivity.TAG, "Checking if a backup made by this application exists");
            File f = new File("/system/app/" + apkID + ".apk");
            if (f.exists()) {
                Log.i(MainActivity.TAG, "- It does. Backing up file as /system/app/" + apkID + ".bakapk");
                stdin.println("mv /system/app/" + apkID + ".apk /system/app/" + apkID + ".bakapk");
            }

            // Copy from the SDCard to the /system/app partition
            Log.i(MainActivity.TAG, "Copying file from storage to /system/app/" + apkID + ".apk");
            stdin.println("cat " + Environment.getExternalStorageDirectory() + "/CMApps/" + api + "/" + apkID + ".apk /system/app/" + apkID + ".apk");

            // Change the permissions
            Log.i(MainActivity.TAG, "Changing the permissions of the application to 644");
            stdin.println("chmod 644 /system/app/" + apkID + ".apk");

            // Remount as read-only
            Log.i(MainActivity.TAG, "Remounting your /system partition as Read-Only");
            stdin.println("mount -o remount,ro -t yaffs2 /dev/block/mtdblock3 /system");

            // Close the stream
            Log.i(MainActivity.TAG, "Closing the stream");
            stdin.close();



            // Show the dialog for it being done
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            builder.setMessage("The app was pushed to the /system directory, and permissions fixed.");
            builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    updateDesc();
                }
            });
            builder.setOnCancelListener(new DialogInterface.OnCancelListener() {
                @Override
                public void onCancel(DialogInterface dialog) {
                    updateDesc();
                }
            });
            builder.create().show();




        } catch (Exception e) {
            Log.i(MainActivity.TAG, "Error running copy to /system. Error: " + e.toString());
            e.printStackTrace();
        }
    }


    public void installChecker(String apI) {
        final String api = apI;
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle("Copying to /system");
        String message = "<strong>Warning</strong><br/>This will modify your system partition. Do you wish to continue?";
        builder.setMessage(Html.fromHtml(message));
        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                copyToSystem(api);
            }
        });
        builder.setNegativeButton("Cancel.", null);
        builder.create().show();
    }


    public void removeChecker() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        String message = "<strong>Warning:</strong><br/>";
        if (new File("/system/app/" + apkID + ".bakapk").exists())
            message += "This will remove the currently installed app and restore the backup detected. Do you wish to continue?";
        else
            message += "A backup is not detected, therefore running this will just uninstall "
                    + getActivity().getSharedPreferences(MainActivity.sharedPreferencesID, Context.MODE_PRIVATE).getString(apkID + "#TITLE", null)
                    + " from your system. Do you wish to continue?";
        builder.setMessage(Html.fromHtml(message));
        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                removeFromSystem();
            }
        });
        builder.setNegativeButton("Cancel.", null);
        builder.create().show();
    }


    public void removeFromSystem() {
        boolean backupRestored = false;
        try {
            Process p = Runtime.getRuntime().exec("su");
            PrintStream stdin = new PrintStream(p.getOutputStream());

            // SU
            Log.i(MainActivity.TAG, "Getting SU access");
            stdin.println("su");

            // Remount as read-write
            Log.i(MainActivity.TAG, "Remounting system as RW");
            stdin.println("mount -o remount,rw -t yaffs2 /dev/block/mtdblock3 /system");

            // Remove the app from the system
            Log.i(MainActivity.TAG, "Destroying /system/app/" + apkID + ".apk");
            stdin.println("rm -r /system/app/" + apkID + ".apk");



            // Checking if there is a backup
            Log.i(MainActivity.TAG, "Checking if backup exists");
            if (new File("/system/app/" + apkID + ".bakapk").exists()) {
                Log.i(MainActivity.TAG, "- Backup exists. Restoring now");
                stdin.println("mv /system/app/" + apkID + ".bakapk /system/app/" + apkID + ".apk");
            }

            // Remounting as read-only
            Log.i(MainActivity.TAG, "Remounting as read-only");
            stdin.println("mount -o remount,ro -t yaffs2 /dev/block/mtdblock3 /system");


            Log.i(MainActivity.TAG, "Closing the stream");
            // Closing the stream
            stdin.close();



            // Show the dialog for it being done
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            String message = "The app was successfully removed";
            if (backupRestored)
                message += ", and the backup restored";

            builder.setMessage("The app was successfully removed");
            builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    updateDesc();
                }
            });
            builder.setOnCancelListener(new DialogInterface.OnCancelListener() {
                @Override
                public void onCancel(DialogInterface dialog) {
                    updateDesc();
                }
            });
            builder.create().show();
        } catch (Exception e) {
            Log.i(MainActivity.TAG, "Error running copy to /system. Error: " + e.toString());
            e.printStackTrace();
        }
    }
}
