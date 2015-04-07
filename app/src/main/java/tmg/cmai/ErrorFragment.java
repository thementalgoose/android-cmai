package tmg.cmai;

import android.app.Fragment;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

/**
 * Created by jordan on 26/02/15.
 */
public class ErrorFragment extends Fragment {

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.error_fragment, container, false);
        TextView text = (TextView) v.findViewById(R.id.text);
        text.setText("There was an error connecting to the network. Unfortunately your list of apps cannot be downloaded." +
                "\n\nPlease make sure\n- You have an active internet connection\n\nIf this doesn't work, contact me on thementalgoose@gmail.com, and I will look at your problem directly. Apologies for any inconvienence caused.");
        return v;

    }
}