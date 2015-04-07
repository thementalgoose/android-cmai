package tmg.cmai;

import android.text.style.ClickableSpan;
import android.view.View;

/**
 * Created by jordan on 01/03/15.
 */

public class ClickSpan extends ClickableSpan {

    private OnClickListener mListener;

    public ClickSpan(OnClickListener listener) {
        mListener = listener;
    }

    @Override
    public void onClick(View widget) {
        if (mListener != null) mListener.onClick();
    }

    public interface OnClickListener {
        void onClick();
    }
}
