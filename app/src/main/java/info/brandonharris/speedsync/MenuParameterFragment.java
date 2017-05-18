package info.brandonharris.speedsync;

import android.app.Fragment;
import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

public class MenuParameterFragment extends Fragment {
    private String parameterName;
    private String parameterValue;
    private View.OnClickListener onClickListener;

    private TextView valueText;

    public MenuParameterFragment() {
        // Required empty public constructor
    }

    public static MenuParameterFragment newInstance(String parameterName, String parameterValue) {
        MenuParameterFragment fragment = new MenuParameterFragment();
        fragment.parameterName = parameterName;
        fragment.parameterValue = parameterValue;
        return fragment;
    }

    public void setOnClickListener(View.OnClickListener onClickListener) {
        this.onClickListener = onClickListener;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View fragmentView = inflater.inflate(R.layout.fragment_menu_parameter, container, false);

        fragmentView.findViewById(R.id.parameterLayout).setOnClickListener(onClickListener);

        ((TextView) fragmentView.findViewById(R.id.nameText)).setText(parameterName);

        valueText = (TextView) fragmentView.findViewById(R.id.valueText);
        valueText.setText(parameterValue);

        return fragmentView;
    }

    public void setParameterValue(String parameterValue) {
        this.parameterValue = parameterValue;
        valueText.setText(parameterValue);
    }

    public String getParameterValue() {
        return parameterValue;
    }
}
