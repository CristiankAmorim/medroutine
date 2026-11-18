package br.ucsal.medroutine.ui.cuidador;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.ArrayList;
import java.util.List;

import br.ucsal.medroutine.R;
import br.ucsal.medroutine.data.model.ErroApi;
import br.ucsal.medroutine.data.model.Idoso;
import br.ucsal.medroutine.data.repo.Callback;
import br.ucsal.medroutine.data.repo.Repositorios;
import br.ucsal.medroutine.session.SessionManager;
import br.ucsal.medroutine.ui.comum.PermissoesHelper;
import br.ucsal.medroutine.ui.comum.Sessao;
import br.ucsal.medroutine.ui.comum.Ui;

public class CuidadorMainActivity extends AppCompatActivity {
    private final List<Idoso> idosos = new ArrayList<>();
    private Idoso selecionado;
    private int abaAtual = R.id.nav_hoje;

    private BottomNavigationView navegacao;
    private MaterialButton botaoTrocar;

    private final ActivityResultLauncher<String> pedirNotificacao =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), ok -> { });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cuidador_main);
        if (savedInstanceState != null) {
            abaAtual = savedInstanceState.getInt("aba", R.id.nav_hoje);
        }

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setOnMenuItemClickListener(item -> {
            int id = item.getItemId();
            if (id == R.id.acao_notificacoes) {
                startActivity(new Intent(this, NotificacoesActivity.class));
            } else if (id == R.id.acao_idosos) {
                startActivity(new Intent(this, IdososActivity.class));
            } else if (id == R.id.acao_conta) {
                startActivity(new Intent(this, ContaActivity.class));
            }
            return true;
        });

        navegacao = findViewById(R.id.navegacao);
        navegacao.setSelectedItemId(abaAtual);
        navegacao.setOnItemSelectedListener(item -> {
            abaAtual = item.getItemId();
            mostrarAba();
            return true;
        });

        botaoTrocar = findViewById(R.id.botao_trocar_idoso);
        botaoTrocar.setOnClickListener(v -> escolherIdoso());
        findViewById(R.id.botao_gerenciar_idosos).setOnClickListener(v ->
                startActivity(new Intent(this, IdososActivity.class)));

        PermissoesHelper.pedirNotificacoesSeNecessario(this, pedirNotificacao);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt("aba", abaAtual);
    }

    @Override
    protected void onResume() {
        super.onResume();
        PermissoesHelper.atualizarFaixaDemo(this, findViewById(R.id.faixa_demo));
        carregarIdosos();
    }

    private void carregarIdosos() {
        View progresso = findViewById(R.id.progresso);
        progresso.setVisibility(View.VISIBLE);
        Repositorios.get(this).listarIdosos(new Callback<List<Idoso>>() {
            @Override
            public void sucesso(List<Idoso> lista) {
                progresso.setVisibility(View.GONE);
                idosos.clear();
                idosos.addAll(lista);
                atualizarSelecao();
            }

            @Override
            public void erro(ErroApi erro) {
                progresso.setVisibility(View.GONE);
                if (erro.status == 401) {
                    Sessao.encerrar(CuidadorMainActivity.this);
                    return;
                }
                Ui.erro(findViewById(R.id.raiz), erro);
                if (selecionado == null) {
                    mostrarSemIdoso(true);
                }
            }
        });
    }

    private void atualizarSelecao() {
        if (idosos.isEmpty()) {
            selecionado = null;
            mostrarSemIdoso(true);
            return;
        }
        long salvo = SessionManager.get(this).getIdosoSelecionado();
        selecionado = idosos.get(0);
        for (Idoso i : idosos) {
            if (i.id == salvo) {
                selecionado = i;
            }
        }
        SessionManager.get(this).setIdosoSelecionado(selecionado.id);
        mostrarSemIdoso(false);
        botaoTrocar.setText(getString(idosos.size() > 1 ? R.string.acompanhando_trocar : R.string.acompanhando,
                selecionado.nome));
        mostrarAba();
    }

    private void mostrarSemIdoso(boolean semIdoso) {
        findViewById(R.id.grupo_sem_idoso).setVisibility(semIdoso ? View.VISIBLE : View.GONE);
        findViewById(R.id.conteudo).setVisibility(semIdoso ? View.GONE : View.VISIBLE);
        navegacao.setVisibility(semIdoso ? View.GONE : View.VISIBLE);
        botaoTrocar.setVisibility(semIdoso ? View.GONE : View.VISIBLE);
    }

    private void escolherIdoso() {
        if (idosos.size() < 2) {
            startActivity(new Intent(this, IdososActivity.class));
            return;
        }
        String[] nomes = new String[idosos.size()];
        for (int i = 0; i < idosos.size(); i++) {
            nomes[i] = idosos.get(i).nome;
        }
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.escolher_idoso)
                .setItems(nomes, (d, qual) -> {
                    SessionManager.get(this).setIdosoSelecionado(idosos.get(qual).id);
                    atualizarSelecao();
                })
                .show();
    }

    private void mostrarAba() {
        if (selecionado == null || isFinishing()) {
            return;
        }
        Fragment f;
        if (abaAtual == R.id.nav_medicamentos) {
            f = MedicamentosFragment.novo(selecionado.id, selecionado.nome);
        } else if (abaAtual == R.id.nav_historico) {
            f = HistoricoFragment.novo(selecionado.id);
        } else {
            f = PainelFragment.novo(selecionado.id, selecionado.nome);
        }
        getSupportFragmentManager().beginTransaction().replace(R.id.conteudo, f).commitAllowingStateLoss();
    }
}
