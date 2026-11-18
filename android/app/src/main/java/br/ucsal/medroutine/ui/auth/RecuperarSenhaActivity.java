package br.ucsal.medroutine.ui.auth;

import android.os.Bundle;
import android.util.Patterns;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.textfield.TextInputLayout;

import br.ucsal.medroutine.R;
import br.ucsal.medroutine.data.model.ErroApi;
import br.ucsal.medroutine.data.repo.Callback;
import br.ucsal.medroutine.data.repo.Repositorios;
import br.ucsal.medroutine.ui.comum.Ui;

public class RecuperarSenhaActivity extends AppCompatActivity {
    private TextInputLayout campoEmail, campoCodigo, campoNovaSenha;
    private EditText email, codigo, novaSenha;
    private View progresso;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_recuperar_senha);
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        campoEmail = findViewById(R.id.campo_email);
        campoCodigo = findViewById(R.id.campo_codigo);
        campoNovaSenha = findViewById(R.id.campo_nova_senha);
        email = findViewById(R.id.entrada_email);
        codigo = findViewById(R.id.entrada_codigo);
        novaSenha = findViewById(R.id.entrada_nova_senha);
        progresso = findViewById(R.id.progresso);

        findViewById(R.id.botao_enviar_codigo).setOnClickListener(this::enviarCodigo);
        findViewById(R.id.botao_redefinir).setOnClickListener(this::redefinir);
    }

    private void enviarCodigo(View botao) {
        campoEmail.setError(null);
        String e = Ui.texto(email);
        if (!Patterns.EMAIL_ADDRESS.matcher(e).matches()) {
            campoEmail.setError(getString(R.string.erro_email));
            return;
        }
        Ui.carregando(botao, progresso, true);
        Repositorios.remoto(this).recuperarSenha(e, new Callback<Void>() {
            @Override
            public void sucesso(Void r) {
                Ui.carregando(botao, progresso, false);
                findViewById(R.id.grupo_redefinir).setVisibility(View.VISIBLE);
                codigo.requestFocus();
            }

            @Override
            public void erro(ErroApi erro) {
                Ui.carregando(botao, progresso, false);
                Ui.erro(findViewById(R.id.raiz), erro);
            }
        });
    }

    private void redefinir(View botao) {
        Ui.limparErros(campoCodigo, campoNovaSenha);
        String c = Ui.texto(codigo);
        String s = novaSenha.getText() == null ? "" : novaSenha.getText().toString();
        if (c.length() != 6) {
            campoCodigo.setError(getString(R.string.erro_codigo));
            return;
        }
        if (s.length() < 8) {
            campoNovaSenha.setError(getString(R.string.senha_ajuda));
            return;
        }
        Ui.carregando(botao, progresso, true);
        Repositorios.remoto(this).redefinirSenha(Ui.texto(email), c, s, new Callback<Void>() {
            @Override
            public void sucesso(Void r) {
                Toast.makeText(RecuperarSenhaActivity.this, R.string.senha_redefinida, Toast.LENGTH_LONG).show();
                finish();
            }

            @Override
            public void erro(ErroApi erro) {
                Ui.carregando(botao, progresso, false);
                Ui.erroNoCampo(findViewById(R.id.raiz), erro, "novaSenha".equals(erro.campo) ? campoNovaSenha : campoCodigo);
            }
        });
    }
}
