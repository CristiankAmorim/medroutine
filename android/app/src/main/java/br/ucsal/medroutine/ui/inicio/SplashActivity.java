package br.ucsal.medroutine.ui.inicio;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import br.ucsal.medroutine.session.SessionManager;
import br.ucsal.medroutine.ui.cuidador.CuidadorMainActivity;
import br.ucsal.medroutine.ui.idoso.IdosoHomeActivity;

public class SplashActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        SessionManager sessao = SessionManager.get(this);
        Class<?> destino;
        if (!sessao.isLogado()) {
            destino = BoasVindasActivity.class;
        } else if (sessao.isIdoso()) {
            destino = IdosoHomeActivity.class;
        } else {
            destino = CuidadorMainActivity.class;
        }
        startActivity(new Intent(this, destino));
        finish();
    }
}
