package br.ucsal.medroutine.notification;

import android.content.Context;
import android.util.Log;

import com.google.firebase.FirebaseApp;
import com.google.firebase.messaging.FirebaseMessaging;

import br.ucsal.medroutine.data.model.ErroApi;
import br.ucsal.medroutine.data.repo.Callback;
import br.ucsal.medroutine.data.repo.Repositorios;

public final class FirebaseHelper {
    private static final String TAG = "FirebaseHelper";

    private FirebaseHelper() { }

    public static boolean configurado(Context context) {
        return !FirebaseApp.getApps(context).isEmpty();
    }

    public static void registrarToken(Context context) {
        Context app = context.getApplicationContext();
        if (!configurado(app)) {
            Log.w(TAG, "Firebase não configurado (google-services.json ausente): push desativado.");
            return;
        }
        FirebaseMessaging.getInstance().getToken().addOnSuccessListener(token ->
                Repositorios.remoto(app).registrarTokenAparelho(token, new Callback<Void>() {
                    @Override public void sucesso(Void r) { }
                    @Override public void erro(ErroApi e) { Log.w(TAG, "Falha ao registrar token: " + e.mensagem); }
                }));
    }
}
