package br.ucsal.medroutine.ui.auth;

import android.os.Bundle;
import android.util.Patterns;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.textfield.TextInputLayout;

import br.ucsal.medroutine.R;
import br.ucsal.medroutine.data.model.ErroApi;
import br.ucsal.medroutine.data.model.TokenResponse;
import br.ucsal.medroutine.data.repo.Callback;
import br.ucsal.medroutine.data.repo.Repositorios;
import br.ucsal.medroutine.ui.comum.Ui;

public class CadastroActivity extends AppCompatActivity {
    private TextInputLayout campoNome, campoEmail, campoSenha, campoConfirmar;
    private EditText nome, email, senha, confirmar;
    private CheckBox consentimento;
    private View botao, progresso;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cadastro);
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        campoNome = findViewById(R.id.campo_nome);
        campoEmail = findViewById(R.id.campo_email);
        campoSenha = findViewById(R.id.campo_senha);
        campoConfirmar = findViewById(R.id.campo_confirmar);
        nome = findViewById(R.id.entrada_nome);
        email = findViewById(R.id.entrada_email);
        senha = findViewById(R.id.entrada_senha);
        confirmar = findViewById(R.id.entrada_confirmar);
        consentimento = findViewById(R.id.check_consentimento);
        botao = findViewById(R.id.botao_criar);
        progresso = findViewById(R.id.progresso);
        botao.setOnClickListener(v -> criar());
    }

    private void criar() {
        Ui.limparErros(campoNome, campoEmail, campoSenha, campoConfirmar);
        String n = Ui.texto(nome);
        String e = Ui.texto(email);
        String s = senha.getText() == null ? "" : senha.getText().toString();
        String c = confirmar.getText() == null ? "" : confirmar.getText().toString();
        if (n.isEmpty()) {
            campoNome.setError(getString(R.string.erro_nome));
            return;
        }
        if (!Patterns.EMAIL_ADDRESS.matcher(e).matches()) {
            campoEmail.setError(getString(R.string.erro_email));
            return;
        }
        if (s.length() < 8) {
            campoSenha.setError(getString(R.string.senha_ajuda));
            return;
        }
        if (!s.equals(c)) {
            campoConfirmar.setError(getString(R.string.erro_senhas_diferentes));
            return;
        }
        if (!consentimento.isChecked()) {
            Ui.mensagem(findViewById(R.id.raiz), getString(R.string.erro_consentimento));
            return;
        }
        Ui.esconderTeclado(this);
        Ui.carregando(botao, progresso, true);
        Repositorios.remoto(this).cadastrar(n, e, s, true, new Callback<TokenResponse>() {
            @Override
            public void sucesso(TokenResponse r) {
                Ui.carregando(botao, progresso, false);
                LoginActivity.concluirLogin(CadastroActivity.this, r);
            }

            @Override
            public void erro(ErroApi erro) {
                Ui.carregando(botao, progresso, false);
                TextInputLayout campo = "email".equals(erro.campo) ? campoEmail
                        : "senha".equals(erro.campo) ? campoSenha
                        : "nome".equals(erro.campo) ? campoNome : null;
                Ui.erroNoCampo(findViewById(R.id.raiz), erro, campo);
            }
        });
    }
}
