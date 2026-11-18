package br.ucsal.medroutine.ui.idoso;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.textfield.TextInputLayout;

import br.ucsal.medroutine.R;
import br.ucsal.medroutine.data.model.ErroApi;
import br.ucsal.medroutine.data.model.TokenResponse;
import br.ucsal.medroutine.data.repo.Callback;
import br.ucsal.medroutine.data.repo.Repositorios;
import br.ucsal.medroutine.data.sync.AtualizarAgendaWorker;
import br.ucsal.medroutine.notification.FirebaseHelper;
import br.ucsal.medroutine.session.SessionManager;
import br.ucsal.medroutine.ui.comum.Ui;

public class IdosoCodigoActivity extends AppCompatActivity {
    private TextInputLayout campo;
    private EditText codigo;
    private View botao, progresso;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_idoso_codigo);
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        campo = findViewById(R.id.campo_codigo);
        codigo = findViewById(R.id.entrada_codigo);
        botao = findViewById(R.id.botao_entrar);
        progresso = findViewById(R.id.progresso);
        botao.setOnClickListener(v -> entrar());
    }

    private void entrar() {
        campo.setError(null);
        String c = Ui.texto(codigo);
        if (c.length() != 6) {
            campo.setError(getString(R.string.erro_codigo));
            return;
        }
        Ui.esconderTeclado(this);
        Ui.carregando(botao, progresso, true);
        Repositorios.remoto(this).entrarComCodigo(c, new Callback<TokenResponse>() {
            @Override
            public void sucesso(TokenResponse r) {
                SessionManager.get(IdosoCodigoActivity.this).iniciar(r, false);
                FirebaseHelper.registrarToken(IdosoCodigoActivity.this);
                AtualizarAgendaWorker.agendarPeriodico(IdosoCodigoActivity.this);
                Intent i = new Intent(IdosoCodigoActivity.this, IdosoHomeActivity.class)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(i);
                finish();
            }

            @Override
            public void erro(ErroApi erro) {
                Ui.carregando(botao, progresso, false);
                campo.setError(erro.mensagem);
            }
        });
    }
}
