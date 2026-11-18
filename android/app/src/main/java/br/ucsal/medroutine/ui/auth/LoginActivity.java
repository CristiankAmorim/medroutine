package br.ucsal.medroutine.ui.auth;

import android.content.Intent;
import android.os.Bundle;
import android.util.Patterns;
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
import br.ucsal.medroutine.notification.FirebaseHelper;
import br.ucsal.medroutine.session.SessionManager;
import br.ucsal.medroutine.ui.comum.Ui;
import br.ucsal.medroutine.ui.cuidador.CuidadorMainActivity;

public class LoginActivity extends AppCompatActivity {
    private TextInputLayout campoEmail;
    private TextInputLayout campoSenha;
    private EditText email;
    private EditText senha;
    private View botao;
    private View progresso;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        campoEmail = findViewById(R.id.campo_email);
        campoSenha = findViewById(R.id.campo_senha);
        email = findViewById(R.id.entrada_email);
        senha = findViewById(R.id.entrada_senha);
        botao = findViewById(R.id.botao_entrar);
        progresso = findViewById(R.id.progresso);

        botao.setOnClickListener(v -> entrar());
        findViewById(R.id.botao_esqueci).setOnClickListener(v ->
                startActivity(new Intent(this, RecuperarSenhaActivity.class)));
        findViewById(R.id.botao_criar_conta).setOnClickListener(v ->
                startActivity(new Intent(this, CadastroActivity.class)));
    }

    private void entrar() {
        Ui.limparErros(campoEmail, campoSenha);
        String e = Ui.texto(email);
        String s = senha.getText() == null ? "" : senha.getText().toString();
        if (!Patterns.EMAIL_ADDRESS.matcher(e).matches()) {
            campoEmail.setError(getString(R.string.erro_email));
            return;
        }
        if (s.isEmpty()) {
            campoSenha.setError(getString(R.string.erro_senha_vazia));
            return;
        }
        Ui.esconderTeclado(this);
        Ui.carregando(botao, progresso, true);
        Repositorios.remoto(this).login(e, s, new Callback<TokenResponse>() {
            @Override
            public void sucesso(TokenResponse r) {
                Ui.carregando(botao, progresso, false);
                concluirLogin(LoginActivity.this, r);
            }

            @Override
            public void erro(ErroApi erro) {
                Ui.carregando(botao, progresso, false);
                Ui.erro(findViewById(R.id.raiz), erro);
            }
        });
    }

    static void concluirLogin(AppCompatActivity activity, TokenResponse r) {
        SessionManager.get(activity).iniciar(r, false);
        FirebaseHelper.registrarToken(activity);
        Intent i = new Intent(activity, CuidadorMainActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        activity.startActivity(i);
        activity.finish();
    }
}
