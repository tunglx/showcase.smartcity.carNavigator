package hmi.parkinglot.navigation;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.here.android.mpa.common.GeoCoordinate;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.Set;
import java.util.UUID;

import hmi.parkinglot.Application;

/**
 * Transfers the route through Bluetooth to a paired device
 */
public class RouteTransfer extends AsyncTask<RouteData, Void, Integer> {

    private Handler handler;

    private void sendMessage(int result) {
        Message msg = handler.obtainMessage(0);
        Bundle bundle = new Bundle();
        bundle.putInt(Application.TRANSFER_RESULT, result);
        msg.setData(bundle);
        handler.sendMessage(msg);
    }

    public void setHandler(Handler handler) {
        this.handler = handler;
    }

    protected Integer doInBackground(RouteData... request) {
        RouteData data = request[0];
        int result = transfer(data.parkingCoordinates, data.parkingAddress,
                data.destinationCoordinates, data.destination);

        return new Integer(result);
    }

    @Override
    protected void onPostExecute(Integer result) {
        sendMessage(result.intValue());
    }

    private int transfer(GeoCoordinate originCoords, String originName,
                         GeoCoordinate destCoords, String destName) {

        if (originCoords == null || destCoords == null) {
            Log.d(Application.TAG, "Origin or destination coords are null!!!");
            return -1;
        }

        BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
        Set<BluetoothDevice> pairedDevices = adapter.getBondedDevices();
        if (pairedDevices.size() == 0) {
            Log.d(Application.TAG, "No paired devices found");
            return -1;
        }

        BluetoothDevice device = null;
        for (BluetoothDevice bt : pairedDevices) {
            Log.d(Application.TAG, "Paired Device: " + bt.getName());
            if (bt.getName().indexOf("Fiware-Here-Slave") == 0) {
                device = bt;
                break;
            }
        }

        if (device == null) {
            return -1;
        }

        Log.d(Application.TAG, "Paired device to connect to: " + device.getName());

        try {
            UUID uuid = UUID.fromString("29B966E5-FBAD-4A05-B40E-86205D77AF72");
            BluetoothSocket socket = device.createRfcommSocketToServiceRecord(uuid);
            socket.connect();
            String coordStr = originCoords.getLatitude() + "," + originCoords.getLongitude();
            coordStr += ";" + originName;
            coordStr += ";" + destCoords.getLatitude() + "," + destCoords.getLongitude();
            coordStr += ";" + destName;

            OutputStream output = socket.getOutputStream();
            output.write(coordStr.getBytes());

            Log.d(Application.TAG, "Route sent to paired device: " + coordStr);

            output.flush();

            byte[] buffer = new byte[16];
            InputStream input = socket.getInputStream();
            int bytesRead = input.read(buffer);
            String data = new String(Arrays.copyOf(buffer, bytesRead));

            if (data.equals("bye")) {
                Log.d(Application.TAG, "ACK received. Closing");
                try {
                    input.close();
                    output.close();
                    socket.close();
                } catch (IOException ioe) {
                    Log.e(Application.TAG, "Error while closing sockets: " + ioe);
                }
            }

            // Success
            return 0;
        } catch (IOException ioe) {
            Log.e(Application.TAG, "Error transferring route: " + ioe.toString());
            return -1;
        }
    }
}
