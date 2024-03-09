package com.smov.gabriel.orientatree.helpers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

public class MyBroadcastReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        // Este método será llamado cuando se reciba una emisión (broadcast)

        // Verifica la acción de la intención recibida
        if (intent.getAction() != null && intent.getAction().equals("com.ejemplo.ACCION_PERSONALIZADA")) {
            // Aquí puedes realizar las acciones que desees cuando se reciba la emisión con la acción "com.ejemplo.ACCION_PERSONALIZADA"
            // Por ejemplo, mostrar un mensaje Toast
            Toast.makeText(context, "Receptor de emisión ha recibido una acción personalizada", Toast.LENGTH_SHORT).show();
        }
    }
}
