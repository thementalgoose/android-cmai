package tmg.cmai;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Environment;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.AdView;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;


public class MainActivity extends ActionBarActivity {

    /* Attributes */
    private DrawerLayout drawerLayout;
    private ListView drawerList;
    private ActionBarDrawerToggle drawerToggle;
    private Toolbar toolbar;
    private String activeLanguage = "Recall";
    final public static String TAG = "CMAI";
    private AdView adView;

    /* Networking hard links */
    final private String info1 = "https://dl.dropbox.com/s/r0lgorm4fouljy4/desc.txt?dl=1";
    final private String info2 = "https://dl.dropbox.com/s/r0lgorm4fouljy4/desc.txt?dl=1";
    final private String fileDIR = Environment.getExternalStorageDirectory().toString() + "/CMAppDownloaderInfo.txt";
    final public static int API = Build.VERSION.SDK_INT;
    final public static String sharedPreferencesID = "CMAI";

    public static boolean root = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        drawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawerList = (ListView) findViewById(R.id.left_drawer);
        toolbar = (Toolbar) findViewById(R.id.toolbar);

        drawerToggle = new ActionBarDrawerToggle(this, drawerLayout, toolbar, R.string.blank, R.string.blank) {
            /* Called when a drawer has settled in a completely closed state. */
            public void onDrawerClosed(View view) {
                super.onDrawerClosed(view);
                invalidateOptionsMenu(); // creates call to onPrepareOptionsMenu()
            }

            /* Called when a drawer has settled in a completely open state. */
            public void onDrawerOpened(View drawerView) {
                super.onDrawerOpened(drawerView);
                invalidateOptionsMenu(); // creates call to onPrepareOptionsMenu()
            }
        };

        /* Actionbar */
        setSupportActionBar(toolbar);
        getSupportActionBar().setHomeButtonEnabled(true);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        toolbar.setTitle(activeLanguage);

        /* Properties drawerLayout */
        drawerLayout.setDrawerListener(drawerToggle);
        drawerLayout.setStatusBarBackground(R.color.primary_dark);


        /* Toggle onClickListener */
        drawerToggle.setToolbarNavigationClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                drawerLayout.openDrawer(Gravity.LEFT | Gravity.START);
            }
        });


        /* Sync that shit */
        drawerToggle.syncState();


        /* Checking root status */
        if (API < 20)
            root = canRunRootCommands();

        /* Admob setup */
        // Developer test id: E0B8F75BF29F906755EFAF4914556452
        // Run in Emulator first!
        adView = (AdView) findViewById(R.id.adView);
        AdRequest adRequest = new AdRequest.Builder()
                .addTestDevice("E0B8F75BF29F906755EFAF4914556452")
                .build();
        adView.loadAd(adRequest);



        if (!getSharedPreferences(sharedPreferencesID, MODE_PRIVATE).getBoolean("INITIAL", false)) {
            LoadAppInfo lai = new LoadAppInfo();
            lai.execute();
        } else {
            displayHome();
            setUpDrawer();
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        /* Handle action bar item clicks here. The action bar will
         * automatically handle clicks on the Home/Up button, so long
         * as you specify a parent activity in AndroidManifest.xml. */
        int id = item.getItemId();

        if (drawerToggle.onOptionsItemSelected(item)) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }


    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        drawerToggle.syncState();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        drawerToggle.onConfigurationChanged(newConfig);
    }

    @Override
    public void onBackPressed() {
        if (drawerLayout.isDrawerOpen(Gravity.START | Gravity.LEFT)) {
            drawerLayout.closeDrawers();
            return;
        }
        super.onBackPressed();
    }




    /* OnResume methods and onPause methods - Handling the AdView once it's been dismissed */
    @Override
    protected void onResume() {
        adView.resume();
        super.onResume();
    }

    @Override
    protected void onPause() {
        adView.pause();
        super.onPause();
    }



    /* Display the home page fragment information */
    public void displayHome() {
        HomeFragment fragment = new HomeFragment();
        FragmentManager manager = getFragmentManager();
        FragmentTransaction transaction = manager.beginTransaction();
        transaction.replace(R.id.main_frag, fragment); // newInstance() is a static factory method.
        transaction.commit();
    }

    public void setUpDrawer() {
        List<MainItem> lists = new ArrayList<MainItem>();
        lists.add(new MainItem(this, "<title>"));
        String[] apks = getSharedPreferences(sharedPreferencesID, MODE_PRIVATE).getString("APPS", "null").split("#");
        if (!apks[0].equals("null")) {
            for (int i = 0; i < apks.length; i++) {
                lists.add(new MainItem(this, getSharedPreferences(sharedPreferencesID, MODE_PRIVATE).getString(apks[i] + "#TITLE", "null"), apks[i]));
            }
        }

        lists.add(new MainItem(this, "<div>"));
        lists.add(new MainItem(this, "Refresh"));
        lists.add(new MainItem(this, "About/Donate"));
        lists.add(new MainItem(this, "Code"));
        lists.add(new MainItem(this, "Clear Storage"));

        final List<MainItem> finalList = lists;

        drawerList.setAdapter(new MainAdapter(this, R.layout.main_text, lists));
        drawerList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                String list = finalList.get(position).getTitle();
                if (list.equals("Refresh")) {
                    LoadAppInfo lai = new LoadAppInfo();
                    lai.execute();
                } else if (list.equals("Clear Storage")) {
                    AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                    builder.setTitle("Clear all");
                    builder.setMessage("This will remove all the applications that have been downloaded. Continue?");
                    builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            deleteEverything(new File(Environment.getExternalStorageDirectory() + "/CMApps/"));
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    Toast.makeText(getApplicationContext(), "All apps removed.", Toast.LENGTH_SHORT).show();
                                    displayHome();
                                }
                            });
                        }
                    });
                    builder.setNegativeButton("Cancel", null);
                    builder.create().show();
                } else if (list.equals("About/Donate")) {
                    AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                    View v = View.inflate(getApplicationContext(), R.layout.about, null);
                    builder.setView(v);
                    builder.setPositiveButton("Email me", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                        /* Launch email client with my email address */
                            String myDeviceModel = android.os.Build.MODEL;
                            String androidVersion = Build.VERSION.RELEASE;

                            Intent i = new Intent(Intent.ACTION_SEND);
                            i.setType("message/rfc822");
                            i.putExtra(android.content.Intent.EXTRA_EMAIL, new String[]{"thementalgoose@gmail.com"});
                            i.putExtra(android.content.Intent.EXTRA_SUBJECT, getResources().getString(R.string.app_name));
                            if (root)
                                i.putExtra(Intent.EXTRA_TEXT, myDeviceModel + ", rooted, running " + androidVersion + "\n\n");
                            else
                                i.putExtra(Intent.EXTRA_TEXT, myDeviceModel + ", not rooted, running " + androidVersion + "\n\n");
                            startActivity(Intent.createChooser(i, "Send email"));
                        }
                    });
                    builder.setNeutralButton("Donate :)", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://www.paypal.com/cgi-bin/webscr?cmd=_donations&business=thementalgoose%40gmail%2ecom&lc=GB&no_note=0&currency_code=GBP&bn=PP%2"));
                            startActivity(browserIntent);
                        }
                    });
                    builder.setNegativeButton("Cancel", null);
                    builder.create().show();
                } else if (list.equals("Code")) {
                    AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                    builder.setTitle("Code");
                    builder.setMessage(R.string.settingsmenu_code_code);
                    builder.setPositiveButton("OK", null);
                    builder.create().show();
                } else if (!list.equals("<title>") && !list.equals("<div>")) {
                    AppFragment frag = new AppFragment();
                    frag.setup(finalList.get(position));

                    FragmentTransaction transaction = getFragmentManager().beginTransaction();
                    transaction.replace(R.id.main_frag, frag);
                    transaction.addToBackStack(null);
                    transaction.commit();
                } else {
                    displayHome();
                }
                drawerLayout.closeDrawer(Gravity.LEFT);
            }
        });
    }


    /* display Error fragment */
    public void displayError() {
        ErrorFragment fragment = new ErrorFragment();
        FragmentManager manager = getFragmentManager();
        FragmentTransaction transaction = manager.beginTransaction();
        transaction.replace(R.id.main_frag, fragment); // newInstance() is a static factory method.
        transaction.commit();
    }

    /* Load the application info */
    class LoadAppInfo extends AsyncTask<String, Integer, Boolean> {
        Dialog dialog;

        @Override
        protected void onPreExecute() {

            /* Initialise Progress dialog, to show progress */
            dialog = new Dialog(MainActivity.this);
            dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
            dialog.setContentView(R.layout.downloadinginfo);
            dialog.setCanceledOnTouchOutside(false);
            dialog.show();
        }

        @Override
        protected Boolean doInBackground(String... params) {
            try {
                URL url = new URL(info1);
                url.openConnection();

                InputStream input = new BufferedInputStream(url.openStream());
                File f = new File(fileDIR);
                OutputStream output = new FileOutputStream(f);

                byte data[] = new byte[1024];
                int total = 0;
                int count;
                while ((count = input.read(data)) != -1) {
                    total += count;
                    output.write(data, 0, count);
                }

                output.flush();
                output.close();
                input.close();

            } catch (Exception e) {
                e.printStackTrace();
                return false;
            }
            return true;
        }


        @Override
        protected void onPostExecute(Boolean aBoolean) {
            if (aBoolean) {
                try {
                    /* Code to handle the text document and importing it all into sharedPreferences */
                    BufferedReader in = new BufferedReader(new FileReader(fileDIR));
                    SharedPreferences.Editor editor = getSharedPreferences(sharedPreferencesID, MODE_PRIVATE).edit();
                    int count = 0;
                    while (in.ready()) {

                        /* This will analyse the text document and store in SharedPreferences
                         * DESCRIPTION: "Description on the front page....."
                         * APIS: "api15#api16#api17
                         * "api15": "4.0 Ice Cream Sandwich"
                         * APPS: "Mms#CMFileManager#...."
                         * "Mms"#TITLE: Messaging
                         * "Mms"#DESC: Description of the application
                         * "Mms"#"api15": https://dl.dropbox......" */

                        // Clear all existing SharedPreferences first:
                        editor.clear();

                        // Analyse the document
                        String temp = "";
                        String s = in.readLine();
                        if (count == 0) {
                            // Description text
                            editor.putString("DESCRIPTION", s);
                        }
                        if (s.equals("---VERSIONS")) {
                            while (!s.equals("---END")) {
                                s = in.readLine();
                                if (!s.equals("---END")) {
                                    String[] apis = s.split("#");
                                    editor.putString(apis[0], apis[1]);
                                    temp += apis[0] + "#";
                                }
                            }
                        }
                        if (!temp.equals("")) {
                            temp = temp.substring(0, temp.length() - 1);
                            editor.putString("APIS", temp);
                            temp = "";
                        }

                        if (s.equals("---APPLICATIONS")) {
                            while (!s.equals("---END")) {
                                s = in.readLine();
                                if (!s.equals("---END")) {
                                    String[] apps = s.split("#");
                                    temp += apps[0] + "#";
                                    editor.putString(apps[0] + "#TITLE", apps[1]);
                                    editor.putString(apps[0] + "#DESC", apps[2]);
                                    while (!s.equals("---ENDA")) {
                                        s = in.readLine();
                                        if (!s.equals("---ENDA")) {
                                            String[] apis = s.split("#");
                                            editor.putString(apps[0] + "#" + apis[0], apis[1]);
                                        }
                                    }
                                }
                            }
                        }
                        if (!temp.equals("")) {
                            temp = temp.substring(0, temp.length() - 1);
                            editor.putString("APPS", temp);
                            temp = "";
                        }
                        count++;
                    }

                    in.close();
                    editor.putBoolean("INITIAL", true);
                    editor.apply();
                    Log.i(TAG, "Finished downloading info and formatting values");

                    // Remove the text document
                    File file = new File(fileDIR);
                    boolean removed = file.delete();
                    Log.i(TAG, "Removed the text file: " + removed);


                    /* Display the home fragment */
                    displayHome();
                    setUpDrawer();

                } catch (Exception e) {
                    e.printStackTrace();
                    Log.i(TAG, "Error downloading info: " + e.toString());
                }
            } else {
                Log.i(TAG, "Download failed. Check your internet connection");

                displayError();
            }
            /* Dialog */
            dialog.dismiss();
        }
    }



    /* Function to request root commands */
    public static boolean canRunRootCommands() {
        Process suProcess;
        try {
            suProcess = Runtime.getRuntime().exec("su");

            DataOutputStream os = new DataOutputStream(suProcess.getOutputStream());
            DataInputStream osRes = new DataInputStream(suProcess.getInputStream());

            if (os != null && osRes != null) {
                // Getting the id of the current user to check if this is root
                os.writeBytes("id\n");
                os.flush();

                String currUid = osRes.readLine();
                if (null == currUid) {
                    os.writeBytes("exit\n");
                    os.flush();
                    Log.d("ROOT", "Can't get root access or denied by user");
                    return false;
                } else if (true == currUid.contains("uid=0")) {
                    os.writeBytes("exit\n");
                    os.flush();
                    Log.d("ROOT", "Root access granted");
                    return true;
                } else {
                    os.writeBytes("exit\n");
                    os.flush();
                    Log.d("ROOT", "Root access rejected: " + currUid);
                    return false;
                }
            }
        } catch (Exception e) {
            Log.d("ROOT", "Root access rejected [" + e.getClass().getName() + "] : " + e.getMessage());
            return false;
        }
        return false;
    }



    /* Delete the folder /CMApps/ and all the files within it. */
    private void deleteEverything(File fileOrDirectory) {
        if (fileOrDirectory.isDirectory())
            for (File child : fileOrDirectory.listFiles())
                deleteEverything(child);
        fileOrDirectory.delete();
    }


    /* onDestroy - Remove the adview */
    protected void onDestroy() {
        adView.destroy();
        super.onDestroy();
    }
}
