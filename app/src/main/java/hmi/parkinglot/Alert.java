package hmi.parkinglot;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;

/**
 *   Shows an alert to the user
 *
 */
public class Alert {
    public static AlertDialog show(Context ctx, String msg) {
        final AlertDialog alertDialog = new AlertDialog.Builder(ctx).create();
        alertDialog.setTitle("Error");
        alertDialog.setMessage(msg);
        alertDialog.setButton("Accept", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                alertDialog.dismiss();
            }
        });

        return alertDialog;
    }
}
