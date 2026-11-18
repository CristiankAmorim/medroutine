package br.ucsal.medroutine.ui.cuidador;

import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.appbar.MaterialToolbar;

import java.util.List;

import br.ucsal.medroutine.R;
import br.ucsal.medroutine.data.model.ErroApi;
import br.ucsal.medroutine.data.model.Notificacao;
import br.ucsal.medroutine.data.repo.Callback;
import br.ucsal.medroutine.data.repo.Repositorios;
import br.ucsal.medroutine.ui.comum.PermissoesHelper;
import br.ucsal.medroutine.ui.comum.Ui;

public class NotificacoesActivity extends AppCompatActivity {
    private SwipeRefreshLayout atualizar;
    private NotificacaoAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notificacoes);
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());
        PermissoesHelper.atualizarFaixaDemo(this, findViewById(R.id.faixa_demo));

        atualizar = findViewById(R.id.atualizar);
        atualizar.setOnRefreshListener(this::carregar);
        RecyclerView lista = findViewById(R.id.lista_notificacoes);
        lista.setLayoutManager(new LinearLayoutManager(this));
        adapter = new NotificacaoAdapter();
        lista.setAdapter(adapter);
    }

    @Override
    protected void onResume() {
        super.onResume();
        carregar();
    }

    private void carregar() {
        atualizar.setRefreshing(true);
        Repositorios.get(this).notificacoes(new Callback<List<Notificacao>>() {
            @Override
            public void sucesso(List<Notificacao> lista) {
                atualizar.setRefreshing(false);
                adapter.atualizar(lista);
                findViewById(R.id.texto_vazio).setVisibility(lista.isEmpty() ? View.VISIBLE : View.GONE);
            }

            @Override
            public void erro(ErroApi erro) {
                atualizar.setRefreshing(false);
                Ui.erro(findViewById(R.id.raiz), erro);
            }
        });
    }
}
