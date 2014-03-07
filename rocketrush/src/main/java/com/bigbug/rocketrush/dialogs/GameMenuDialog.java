package com.bigbug.rocketrush.dialogs;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;

import com.bigbug.rocketrush.R;

public class GameMenuDialog extends DialogFragment {

    private Runnable mCallback;

    public GameMenuDialog(final Runnable callback) {
        mCallback = callback;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

        builder.setItems(R.array.game_dialog_items, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {

                switch (which) {
                case 0: // restart
                    mCallback.run();
                    getActivity().finish();
                    break;
                case 1: // back
                    getActivity().finish();
                    break;
                }
            }
        });

        return builder.create();
    }
}