package br.ucsal.medroutine.ui.idoso;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.List;

import br.ucsal.medroutine.R;
import br.ucsal.medroutine.data.local.DoseLocal;
import br.ucsal.medroutine.data.model.ErroApi;
import br.ucsal.medroutine.data.repo.Callback;
import br.ucsal.medroutine.data.repo.Repositorios;
import br.ucsal.medroutine.data.sync.AgendaIdoso;
import br.ucsal.medroutine.data.sync.AtualizarAgendaWorker;
import br.ucsal.medroutine.data.sync.FilaConfirmacoes;
import br.ucsal.medroutine.session.SessionManager;
import br.ucsal.medroutine.ui.comum.Formatos;
import br.ucsal.medroutine.ui.comum.PermissoesHelper;
import br.ucsal.medroutine.ui.comum.Sessao;
import br.ucsal.medroutine.ui.comum.Ui;
import br.ucsal.medroutine.ui.inicio.BoasVindasActivity;

public class IdosoHomeActivity extends AppCompatActivity {
    private SwipeRefreshLayout atualizar;
    private DoseIdosoAdapter adapter;
    private TextView vazio;
    private View aviso;

    private final ActivityResultLauncher<String> pedirNotificacao =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), ok -> atualizarAviso());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_idoso_home);
        SessionManager sessao = SessionManager.get(this);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.getMenu().findItem(R.id.acao_trocar_demo).setVisible(sessao.isDemo());
        toolbar.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == R.id.acao_atualizar) {
                carregar();
            } else if (item.getItemId() == R.id.acao_trocar_demo) {
                BoasVindasActivity.iniciarDemo(this, false);
            } else if (item.getItemId() == R.id.acao_sair) {
                confirmarSaida();
            }
            return true;
        });

        atualizar = findViewById(R.id.atualizar);
        atualizar.setOnRefreshListener(this::carregar);
        vazio = findViewById(R.id.texto_vazio);
        aviso = findViewById(R.id.aviso_permissoes);

        RecyclerView lista = findViewById(R.id.lista_doses);
        lista.setLayoutManager(new LinearLayoutManager(this));
        adapter = new DoseIdosoAdapter(this::confirmar);
        lista.setAdapter(adapter);

        if (!sessao.isDemo()) {
            AtualizarAgendaWorker.agendarPeriodico(this);
            FilaConfirmacoes.agendarEnvio(this);
        }
        PermissoesHelper.pedirNotificacoesSeNecessario(this, pedirNotificacao);
    }

    @Override
    protected void onResume() {
        super.onResume();
        SessionManager sessao = SessionManager.get(this);
        ((TextView) findViewById(R.id.texto_data)).setText(Formatos.dataLonga(Formatos.hoje()));
        String primeiroNome = sessao.getNome().split(" ")[0];
        ((TextView) findViewById(R.id.texto_saudacao)).setText(
                getString(R.string.saudacao_nome, Formatos.saudacao(this), primeiroNome));
        PermissoesHelper.atualizarFaixaDemo(this, findViewById(R.id.faixa_demo));
        atualizarAviso();
        carregar();
    }

    private void atualizarAviso() {
        PermissoesHelper.atualizarAviso(this, aviso, true);
    }

    private void carregar() {
        atualizar.setRefreshing(true);
        if (!SessionManager.get(this).isDemo()) {
            FilaConfirmacoes.agendarEnvio(this);
        }
        AgendaIdoso.atualizar(this, new Callback<List<DoseLocal>>() {
            @Override
            public void sucesso(List<DoseLocal> doses) {
                atualizar.setRefreshing(false);
                adapter.atualizar(doses);
                vazio.setVisibility(doses.isEmpty() ? View.VISIBLE : View.GONE);
            }

            @Override
            public void erro(ErroApi erro) {
                atualizar.setRefreshing(false);
                vazio.setVisibility(View.VISIBLE);
                if (erro.status == 401) {
                    Sessao.encerrar(IdosoHomeActivity.this);
                    return;
                }
                Ui.erro(findViewById(R.id.raiz), erro);
            }
        });
    }

    private void confirmar(DoseLocal dose) {
        FilaConfirmacoes.confirmar(this, dose.id, ok -> {
            Ui.mensagem(findViewById(R.id.raiz), getString(ok ? R.string.lembrete_confirmado : R.string.lembrete_cedo));
            carregarLocal();
        });
    }

    private void carregarLocal() {
        br.ucsal.medroutine.util.Executores.disco(() -> {
            List<DoseLocal> hoje = AgendaIdoso.dosesDeHojeSincrono(getApplicationContext());
            br.ucsal.medroutine.util.Executores.principal(() -> adapter.atualizar(hoje));
        });
    }

    private void confirmarSaida() {
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.sair_deste_aparelho)
                .setMessage(R.string.sair_idoso_aviso)
                .setPositiveButton(R.string.sair, (d, w) -> Repositorios.get(this).sair(new Callback<Void>() {
                    @Override public void sucesso(Void r) { Sessao.encerrar(IdosoHomeActivity.this); }
                    @Override public void erro(ErroApi e) { Sessao.encerrar(IdosoHomeActivity.this); }
                }))
                .setNegativeButton(R.string.cancelar, null)
                .show();
    }
}
