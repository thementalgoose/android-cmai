package tmg.cmai;

import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
import android.support.annotation.Nullable;
import android.text.Html;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.method.LinkMovementMethod;
import android.text.method.MovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

/**
 * Created by jordan on 25/02/15.
 */
public class HomeFragment extends Fragment {

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.home_fragment, container, false);

        TextView text = (TextView) v.findViewById(R.id.text);

        if (!MainActivity.root)
            text.setText(Html.fromHtml(getActivity().getSharedPreferences(MainActivity.sharedPreferencesID, Context.MODE_PRIVATE).getString("DESCRIPTION", "null")));
        else
            text.setText(Html.fromHtml(getActivity().getSharedPreferences(MainActivity.sharedPreferencesID, Context.MODE_PRIVATE).getString("DESCRIPTION", "null") + "<br/><br/><strong>Root access enabled!</strong><br/>** If the phrase <i>\"The apk will be pushed to your re-mounted /system partition and the permissions fixed\"</i> means nothing to you, don't continue, because that's what it does. I take no responsibility for if the root actions of this application break your device. Everything <i>should</i> work. Please view \"code\" in the settings to check exactly what is running."));

        clickify(text, "Click here to enable.", new ClickSpan.OnClickListener() {
            @Override
            public void onClick() {
                Intent settingsIntent = new Intent(Settings.ACTION_SECURITY_SETTINGS);
                startActivity(settingsIntent);
            }
        });

        return v;

    }





    public static void clickify(TextView view, final String clickableText,
                                final ClickSpan.OnClickListener listener) {

        CharSequence text = view.getText();
        String string = text.toString();
        ClickSpan span = new ClickSpan(listener);

        int start = string.indexOf(clickableText);
        int end = start + clickableText.length();
        if (start == -1) return;

        if (text instanceof Spannable) {
            ((Spannable)text).setSpan(span, start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        } else {
            SpannableString s = SpannableString.valueOf(text);
            s.setSpan(span, start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            view.setText(s);
        }

        MovementMethod m = view.getMovementMethod();
        if ((m == null) || !(m instanceof LinkMovementMethod)) {
            view.setMovementMethod(LinkMovementMethod.getInstance());
        }
    }
}
