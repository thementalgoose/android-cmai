package tmg.cmai;

import android.content.Context;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.util.List;

/**
 * Created by jordan on 25/02/15.
 */
public class MainAdapter extends ArrayAdapter<MainItem> {
    Context context;
    int resource;

    public MainAdapter(Context context, int resource, List<MainItem> objects) {
        super(context, resource, objects);
        this.context = context;
        this.resource = resource;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View v;
        MainItem item = getItem(position);
        String text = item.getTitle();
        if (text.equals("<title>")) {
            v = View.inflate(context, R.layout.main_title, null);
            return v;
        } else if (text.equals("<div>")) {
            v = View.inflate(context, R.layout.main_div, null);
            return v;
        } else {
            v = View.inflate(context, resource, null);
            TextView info = (TextView) v.findViewById(R.id.text);
            TextView apis = (TextView) v.findViewById(R.id.apis);
            String apiInfo = "";
            if (text.equals("Clear Storage") || text.equals("Code") || text.equals("About/Donate") || text.equals("Refresh")) {
                info.setText(text);
                apis.setVisibility(View.GONE);

                float scale = context.getResources().getDisplayMetrics().density;
                int dpAsPixels = (int) (16*scale + 0.5f);

                info.setPadding(dpAsPixels, dpAsPixels, dpAsPixels, dpAsPixels);

            } else {
                info.setText(Html.fromHtml("<strong>" + text + "</strong>"));
                apis.setVisibility(View.VISIBLE);
                apis.setText(Html.fromHtml(item.getApks()));
            }
            return v;
        }
    }
}
