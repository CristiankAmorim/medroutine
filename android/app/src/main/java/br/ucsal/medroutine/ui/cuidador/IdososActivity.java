package br.ucsal.medroutine.ui.cuidador;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.android.material.textfield.TextInputLayout;

import java.util.List;

import br.ucsal.medroutine.R;
import br.ucsal.medroutine.data.model.Codigo;
import br.ucsal.medroutine.data.model.ErroApi;
import br.ucsal.medroutine.data.model.Idoso;
import br.ucsal.medroutine.data.model.NovoIdosoResponse;
import br.ucsal.medroutine.data.repo.Callback;
import br.ucsal.medroutine.data.repo.Repositorios;
import br.ucsal.medroutine.session.SessionManager;
import br.ucsal.medroutine.ui.comum.PermissoesHelper;
import br.ucsal.medroutine.ui.comum.Ui;

public class IdososActivity extends AppCompatActivity implements IdosoAdapter.Acoes {
    private IdosoAdapter adapter;
    private View progresso;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_idosos);
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());
        PermissoesHelper.atualizarFaixaDemo(this, findViewById(R.id.faixa_demo));
        progresso = findViewById(R.id.progresso);

        RecyclerView lista = findViewById(R.id.lista_idosos);
        lista.setLayoutManager(new LinearLayoutManager(this));
        adapter = new IdosoAdapter(this);
        lista.setAdapter(adapter);

        findViewById(R.id.botao_cadastrar_idoso).setOnClickListener(v -> formularioIdoso(null));
        findViewById(R.id.botao_tenho_convite).setOnClickListener(v -> aceitarConvite());
        carregar();
    }

    private View raiz() {
        return findViewById(R.id.raiz);
    }

    private void carregar() {
        progresso.setVisibility(View.VISIBLE);
        Repositorios.get(this).listarIdosos(new Callback<List<Idoso>>() {
            @Override
            public void sucesso(List<Idoso> lista) {
                progresso.setVisibility(View.GONE);
                adapter.atualizar(lista);
                findViewById(R.id.texto_vazio).setVisibility(lista.isEmpty() ? View.VISIBLE : View.GONE);
            }

            @Override
            public void erro(ErroApi erro) {
                progresso.setVisibility(View.GONE);
                Ui.erro(raiz(), erro);
            }
        });
    }

    private void formularioIdoso(Idoso idoso) {
        View v = LayoutInflater.from(this).inflate(R.layout.dialog_idoso, null);
        TextInputLayout campo = v.findViewById(R.id.campo_nome);
        EditText nome = v.findViewById(R.id.entrada_nome);
        MaterialSwitch voz = v.findViewById(R.id.switch_voz);
        if (idoso != null) {
            nome.setText(idoso.nome);
            voz.setChecked(idoso.anuncioVoz);
        }
        AlertDialog dialogo = new MaterialAlertDialogBuilder(this)
                .setTitle(idoso == null ? R.string.cadastrar_idoso : R.string.editar_idoso)
                .setView(v)
                .setPositiveButton(R.string.salvar, null)
                .setNegativeButton(R.string.cancelar, null)
                .create();
        dialogo.setOnShowListener(d -> dialogo.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(b -> {
            String n = Ui.texto(nome);
            if (n.isEmpty()) {
                campo.setError(getString(R.string.erro_nome));
                return;
            }
            dialogo.dismiss();
            if (idoso == null) {
                criar(n, voz.isChecked());
            } else {
                atualizar(idoso, n, voz.isChecked());
            }
        }));
        dialogo.show();
    }

    private void criar(String nome, boolean voz) {
        progresso.setVisibility(View.VISIBLE);
        Repositorios.get(this).criarIdoso(nome, voz, new Callback<NovoIdosoResponse>() {
            @Override
            public void sucesso(NovoIdosoResponse r) {
                progresso.setVisibility(View.GONE);
                SessionManager.get(IdososActivity.this).setIdosoSelecionado(r.idoso.id);
                carregar();
                Dialogos.mostrarCodigo(IdososActivity.this, R.string.codigo_de_acesso,
                        getString(R.string.codigo_acesso_explicacao, r.idoso.nome), r.codigo);
            }

            @Override
            public void erro(ErroApi erro) {
                progresso.setVisibility(View.GONE);
                Ui.erro(raiz(), erro);
            }
        });
    }

    private void atualizar(Idoso idoso, String nome, boolean voz) {
        Repositorios.get(this).atualizarIdoso(idoso.id, nome, voz, new Callback<Idoso>() {
            @Override
            public void sucesso(Idoso r) {
                carregar();
            }

            @Override
            public void erro(ErroApi erro) {
                Ui.erro(raiz(), erro);
            }
        });
    }

    @Override
    public void codigoAcesso(Idoso i) {
        Repositorios.get(this).gerarCodigoAcesso(i.id, new Callback<Codigo>() {
            @Override
            public void sucesso(Codigo c) {
                Dialogos.mostrarCodigo(IdososActivity.this, R.string.codigo_de_acesso,
                        getString(R.string.codigo_acesso_explicacao, i.nome), c);
            }

            @Override
            public void erro(ErroApi erro) {
                Ui.erro(raiz(), erro);
            }
        });
    }

    @Override
    public void convite(Idoso i) {
        Repositorios.get(this).gerarConvite(i.id, new Callback<Codigo>() {
            @Override
            public void sucesso(Codigo c) {
                Dialogos.mostrarCodigo(IdososActivity.this, R.string.convidar_cuidador,
                        getString(R.string.convite_explicacao, i.nome), c);
            }

            @Override
            public void erro(ErroApi erro) {
                Ui.erro(raiz(), erro);
            }
        });
    }

    @Override
    public void opcoes(Idoso i) {
        String[] opcoes = {getString(R.string.editar), getString(R.string.deixar_de_acompanhar),
                getString(R.string.excluir_perfil_idoso)};
        new MaterialAlertDialogBuilder(this)
                .setTitle(i.nome)
                .setItems(opcoes, (d, qual) -> {
                    if (qual == 0) formularioIdoso(i);
                    else if (qual == 1) confirmarDesvincular(i);
                    else confirmarExclusao(i);
                })
                .show();
    }

    private void confirmarDesvincular(Idoso i) {
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.deixar_de_acompanhar)
                .setMessage(getString(R.string.desvincular_mensagem, i.nome))
                .setPositiveButton(R.string.confirmar, (d, w) ->
                        Repositorios.get(this).desvincular(i.id, recarregar()))
                .setNegativeButton(R.string.cancelar, null)
                .show();
    }

    private void confirmarExclusao(Idoso i) {
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.excluir_perfil_idoso)
                .setMessage(getString(R.string.excluir_idoso_mensagem, i.nome))
                .setPositiveButton(R.string.excluir, (d, w) ->
                        Repositorios.get(this).excluirIdoso(i.id, recarregar()))
                .setNegativeButton(R.string.cancelar, null)
                .show();
    }

    private Callback<Void> recarregar() {
        return new Callback<Void>() {
            @Override
            public void sucesso(Void r) {
                SessionManager.get(IdososActivity.this).setIdosoSelecionado(0);
                carregar();
            }

            @Override
            public void erro(ErroApi erro) {
                Ui.erro(raiz(), erro);
            }
        };
    }

    private void aceitarConvite() {
        View v = LayoutInflater.from(this).inflate(R.layout.dialog_texto, null);
        TextInputLayout campo = v.findViewById(R.id.campo_texto);
        EditText entrada = v.findViewById(R.id.entrada_texto);
        campo.setHint(R.string.codigo_convite);
        AlertDialog dialogo = new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.tenho_convite)
                .setMessage(R.string.convite_aceitar_explicacao)
                .setView(v)
                .setPositiveButton(R.string.confirmar, null)
                .setNegativeButton(R.string.cancelar, null)
                .create();
        dialogo.setOnShowListener(d -> dialogo.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(b -> {
            String codigo = Ui.texto(entrada);
            if (codigo.isEmpty()) {
                campo.setError(getString(R.string.erro_codigo_convite));
                return;
            }
            Repositorios.get(this).aceitarConvite(codigo, new Callback<Idoso>() {
                @Override
                public void sucesso(Idoso i) {
                    dialogo.dismiss();
                    SessionManager.get(IdososActivity.this).setIdosoSelecionado(i.id);
                    Ui.mensagem(raiz(), getString(R.string.convite_aceito, i.nome));
                    carregar();
                }

                @Override
                public void erro(ErroApi erro) {
                    campo.setError(erro.mensagem);
                }
            });
        }));
        dialogo.show();
    }
}
