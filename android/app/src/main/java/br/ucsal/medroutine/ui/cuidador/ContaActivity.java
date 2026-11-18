package br.ucsal.medroutine.ui.cuidador;

import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import br.ucsal.medroutine.BuildConfig;
import br.ucsal.medroutine.R;
import br.ucsal.medroutine.data.model.ErroApi;
import br.ucsal.medroutine.data.repo.Callback;
import br.ucsal.medroutine.data.repo.Repositorios;
import br.ucsal.medroutine.demo.DemoRepository;
import br.ucsal.medroutine.notification.FirebaseHelper;
import br.ucsal.medroutine.session.SessionManager;
import br.ucsal.medroutine.ui.comum.PermissoesHelper;
import br.ucsal.medroutine.ui.comum.Sessao;
import br.ucsal.medroutine.ui.comum.Ui;
import br.ucsal.medroutine.ui.inicio.BoasVindasActivity;
import br.ucsal.medroutine.util.Executores;

public class ContaActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_conta);
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());
        SessionManager sessao = SessionManager.get(this);
        PermissoesHelper.atualizarFaixaDemo(this, findViewById(R.id.faixa_demo));

        ((TextView) findViewById(R.id.texto_nome)).setText(sessao.getNome());
        ((TextView) findViewById(R.id.texto_email)).setText(sessao.getEmail());
        ((TextView) findViewById(R.id.texto_versao)).setText(getString(R.string.versao, BuildConfig.VERSION_NAME));

        findViewById(R.id.grupo_demo).setVisibility(sessao.isDemo() ? View.VISIBLE : View.GONE);
        findViewById(R.id.botao_demo_idoso).setOnClickListener(v -> BoasVindasActivity.iniciarDemo(this, true));
        findViewById(R.id.botao_demo_reiniciar).setOnClickListener(v -> Executores.disco(() -> {
            DemoRepository.get(this).reiniciar();
            Executores.principal(() -> Ui.mensagem(findViewById(R.id.raiz), getString(R.string.demo_reiniciada)));
        }));

        findViewById(R.id.botao_config_notificacoes).setOnClickListener(v -> startActivity(
                new Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                        .putExtra(Settings.EXTRA_APP_PACKAGE, getPackageName())));
        findViewById(R.id.botao_sair).setOnClickListener(v -> sair());
        findViewById(R.id.botao_excluir_conta).setOnClickListener(v -> confirmarExclusao());
    }

    @Override
    protected void onResume() {
        super.onResume();
        boolean notif = PermissoesHelper.notificacoesPermitidas(this);
        ((TextView) findViewById(R.id.texto_perm_notificacoes)).setText(
                notif ? R.string.perm_notificacoes_ok : R.string.perm_notificacoes_negada);
        boolean demo = SessionManager.get(this).isDemo();
        ((TextView) findViewById(R.id.texto_perm_push)).setText(demo ? R.string.push_demo
                : FirebaseHelper.configurado(this) ? R.string.push_ok : R.string.push_nao_configurado);
    }

    private void sair() {
        Repositorios.get(this).sair(new Callback<Void>() {
            @Override public void sucesso(Void r) { Sessao.encerrar(ContaActivity.this); }
            @Override public void erro(ErroApi e) { Sessao.encerrar(ContaActivity.this); }
        });
    }

    private void confirmarExclusao() {
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.excluir_conta)
                .setMessage(R.string.excluir_conta_mensagem)
                .setPositiveButton(R.string.excluir, (d, w) -> Repositorios.get(this).excluirConta(new Callback<Void>() {
                    @Override
                    public void sucesso(Void r) {
                        Sessao.encerrar(ContaActivity.this);
                    }

                    @Override
                    public void erro(ErroApi erro) {
                        Ui.erro(findViewById(R.id.raiz), erro);
                    }
                }))
                .setNegativeButton(R.string.cancelar, null)
                .show();
    }
}
