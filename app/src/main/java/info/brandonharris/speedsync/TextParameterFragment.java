package info.brandonharris.speedsync;

import android.app.Activity;
import android.app.Application;
import android.app.Fragment;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;

import static info.brandonharris.speedsync.Constants.*;

public class TextParameterFragment extends Fragment {
    private String parameterName;
    private String parameterKey;
    private String parameterValue;

    private TextView valueText;

    public TextParameterFragment() {
        // Required empty public constructor
    }

    public static TextParameterFragment newInstance(Activity context, String parameterName, String parameterKey, String defaultValue) {
        TextParameterFragment fragment = new TextParameterFragment();
        fragment.parameterName = parameterName;
        fragment.parameterKey = parameterKey;
        SharedPreferences sharedPreferences = context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE);
        fragment.parameterValue = sharedPreferences.getString(parameterKey, defaultValue);
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        final View fragmentView = inflater.inflate(R.layout.fragment_text_parameter, container, false);

        //Show the input dialog when fragment is clicked on
        fragmentView.findViewById(R.id.parameterLayout).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                builder.setTitle(parameterName);

                final EditText input = new EditText(getActivity());
                if (!parameterValue.equals("None")) {
                    input.setText(parameterValue);
                }
                input.setInputType(InputType.TYPE_CLASS_TEXT);
                builder.setView(input);

                builder.setPositiveButton("Apply", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        String text = input.getText().toString();
                        setParameterValue(input.getText().toString());
                        SharedPreferences.Editor editor = getActivity().getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE).edit();
                        editor.putString(parameterKey, text);
                        editor.apply();
                    }
                });
                builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                    }
                });

                builder.show();
            }
        });

        ((TextView) fragmentView.findViewById(R.id.nameText)).setText(parameterName);

        valueText = (TextView) fragmentView.findViewById(R.id.valueText);
        valueText.setText(parameterValue);

        return fragmentView;
    }

    public void setParameterValue(String parameterValue) {
        this.parameterValue = parameterValue;
        valueText.setText(parameterValue);
    }
}
