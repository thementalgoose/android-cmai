package tmg.cmai;

import android.content.Context;
import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by jordan on 25/02/15.
 */
public class MainItem {
    String title = "";
    String apkID = "";
    Context c;

    public MainItem(Context c, String title, String apkID) {
        this.title = title;
        this.apkID = apkID;
        this.c = c;
    }

    public MainItem(Context c, String title) {
        this.c = c;
        this.title = title;
        apkID = "";
    }

    public String getTitle() {
        return title;
    }

    public String getApk() {
        return apkID;
    }


    /* Method to return the downloaded info for the drawer
     *
     * : "4.0, 4.1, 4.2, 4.4, 5.0"
     *
     *    */
    public String getApks() {
        String returnString = "";
        // apkID holds the "Mms" operator

        if (!apkID.equals("")) {
            String[] availableAPIs = c.getSharedPreferences(MainActivity.sharedPreferencesID, c.MODE_PRIVATE).getString("APIS", "null").split("#");
            if (availableAPIs.length != 1) {
                for (int i = 0; i < availableAPIs.length; i++) {
                    String apiID = c.getSharedPreferences(MainActivity.sharedPreferencesID, c.MODE_PRIVATE).getString(apkID + "#" + availableAPIs[i], "null");
                    if (!apiID.equals("null")) {
                        String addition = c.getSharedPreferences(MainActivity.sharedPreferencesID, c.MODE_PRIVATE).getString(availableAPIs[i], "null").split(" ")[0];

                        int apiNumber = Integer.parseInt(availableAPIs[i].replace("api", ""));
                        if (apiNumber == MainActivity.API) {
                            returnString += "<strong>" + addition + "</strong>, ";
                        } else {
                            returnString += addition + ", ";
                        }
                    }

                }
            }
        }

        if (!returnString.equals("")) {
            return returnString.substring(0, returnString.length() - 2);
        } else {
            return "No versions available";
        }
    }


    /* Method to return list of spID api numbers that are available to download
     *
     * [0] - api15
     * [1] - api17
     * [2] - api19
     * [3] - etc...
     *
     *  */
    public String[] getApkList() {
        List<String> strings = new ArrayList<String>();
        if (!apkID.equals("")) {
            String[] availableAPIs = c.getSharedPreferences(MainActivity.sharedPreferencesID, c.MODE_PRIVATE).getString("APIS", "null").split("#");
            if (availableAPIs.length != 1) {
                for (int i = 0; i < availableAPIs.length; i++) {
                    String apiID = c.getSharedPreferences(MainActivity.sharedPreferencesID, c.MODE_PRIVATE).getString(apkID + "#" + availableAPIs[i], "null");
                    if (!apiID.equals("null")) {
                        strings.add(availableAPIs[i]);
                    }
                }
            }
            String[] returnTags = new String[strings.size()];
            for (int i = 0; i < returnTags.length; i++) {
                returnTags[i] = strings.get(i);
            }

            return returnTags;
        } else {
            return new String[]{"null"};
        }
    }


    /* Method to return list of spID api numbers that has been downloaded
     *
     * [0] - api15
     * [1] - api19
     * [2] - etc...
     *
     *  */
    public String[] downloadedAPIs() {
        String[] apis = c.getSharedPreferences(MainActivity.sharedPreferencesID, Context.MODE_PRIVATE).getString("APIS", "null").split("#");
        List<String> newApis = new ArrayList<String>();
        for (int i = 0; i < apis.length; i++) {
            File f = new File(Environment.getExternalStorageDirectory().toString() + "/CMApps/" + apis[i] + "/" + apkID + ".apk");
            if (f.exists())
                newApis.add(apis[i]);
        }
        String[] returnArray = new String[newApis.size()];
        for (int i = 0; i < returnArray.length; i++) {
            returnArray[i] = newApis.get(i);
        }
        return returnArray;
    }

    /* Method to return list of presentable items that it has downloaded
     *
     * [0] - 4.0 Ice Cream Sandwich
     * [1] - 4.3 Jelly Bean
     * [2] - etc...
     *
     *  */
    public String[] downloadedAPIsPublic() {
        String[] apis = downloadedAPIs();
        String[] list = new String[apis.length];
        for (int i = 0; i < apis.length; i++) {
            list[i] = c.getSharedPreferences(MainActivity.sharedPreferencesID, Context.MODE_PRIVATE).getString(apis[i], "null");
        }
        return list;
    }


    /* Method to identify if a certain api has been downloaded */
    public boolean downloaded(String api) {
        File f = new File(Environment.getExternalStorageDirectory() + "/CMApps/" + api + "/" + apkID + ".apk");
        return f.exists();
    }
}
