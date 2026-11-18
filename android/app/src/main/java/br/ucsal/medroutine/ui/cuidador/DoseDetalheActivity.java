package br.ucsal.medroutine.ui.cuidador;

import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import br.ucsal.medroutine.R;
import br.ucsal.medroutine.data.model.ConfirmacaoResponse;
import br.ucsal.medroutine.data.model.Dose;
import br.ucsal.medroutine.data.model.ErroApi;
import br.ucsal.medroutine.data.model.Notificacao;
import br.ucsal.medroutine.data.repo.Callback;
import br.ucsal.medroutine.data.repo.Repositorios;
import br.ucsal.medroutine.domain.RegrasDose;
import br.ucsal.medroutine.session.SessionManager;
import br.ucsal.medroutine.ui.comum.Formatos;
import br.ucsal.medroutine.ui.comum.PermissoesHelper;
import br.ucsal.medroutine.ui.comum.Ui;
import br.ucsal.medroutine.ui.inicio.SplashActivity;

public class DoseDetalheActivity extends AppCompatActivity {
    private static final String EXTRA_DOSE = "doseId";
    private static final String EXTRA_CONFIRMAR = "confirmar";

    private long doseId;
    private boolean perguntarConfirmacao;

    public static Intent intent(Context context, long doseId, boolean confirmar) {
        return new Intent(context, DoseDetalheActivity.class)
                .putExtra(EXTRA_DOSE, doseId)
                .putExtra(EXTRA_CONFIRMAR, confirmar)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (!SessionManager.get(this).isCuidador()) {
            startActivity(new Intent(this, SplashActivity.class));
            finish();
            return;
        }
        setContentView(R.layout.activity_dose_detalhe);
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());
        PermissoesHelper.atualizarFaixaDemo(this, findViewById(R.id.faixa_demo));
        doseId = getIntent().getLongExtra(EXTRA_DOSE, -1);
        perguntarConfirmacao = getIntent().getBooleanExtra(EXTRA_CONFIRMAR, false);
        findViewById(R.id.botao_registrar).setOnClickListener(v -> confirmarRegistro());
    }

    @Override
    protected void onResume() {
        super.onResume();
        carregar();
    }

    private void carregar() {
        findViewById(R.id.progresso).setVisibility(View.VISIBLE);
        Repositorios.get(this).dose(doseId, new Callback<Dose>() {
            @Override
            public void sucesso(Dose d) {
                findViewById(R.id.progresso).setVisibility(View.GONE);
                exibir(d);
                if (perguntarConfirmacao && !d.estadoDose().isConfirmada()) {
                    perguntarConfirmacao = false;
                    confirmarRegistro();
                }
            }

            @Override
            public void erro(ErroApi erro) {
                findViewById(R.id.progresso).setVisibility(View.GONE);
                Ui.erro(findViewById(R.id.raiz), erro);
            }
        });
    }

    private void exibir(Dose d) {
        findViewById(R.id.conteudo).setVisibility(View.VISIBLE);
        ((TextView) findViewById(R.id.texto_idoso)).setText(d.idosoNome);
        ((TextView) findViewById(R.id.texto_medicamento)).setText(d.medicamentoNome + " " + d.dosagem);
        TextView orientacoes = findViewById(R.id.texto_orientacoes);
        orientacoes.setText(d.orientacoes);
        orientacoes.setVisibility(d.orientacoes == null ? View.GONE : View.VISIBLE);

        int cor = ContextCompat.getColor(this, Formatos.corEstado(d.estado));
        TextView estado = findViewById(R.id.texto_estado);
        estado.setText(Formatos.nomeEstado(this, d.estado));
        estado.setTextColor(cor);
        ImageView icone = findViewById(R.id.icone_estado);
        icone.setImageResource(Formatos.iconeEstado(d.estado));
        icone.setImageTintList(ColorStateList.valueOf(cor));
        ((MaterialCardView) findViewById(R.id.cartao_estado)).setStrokeColor(cor);
        ((TextView) findViewById(R.id.texto_confirmacao)).setText(HistoricoAdapter.detalhes(this, semNotificacoes(d)));

        ((TextView) findViewById(R.id.texto_previsto)).setText(getString(R.string.previsto_para,
                Formatos.hora(d.dataHoraPrevista), Formatos.data(d.dataHoraPrevista)));
        ((TextView) findViewById(R.id.texto_tolerancia)).setText(getString(R.string.tolerancia_ate,
                d.toleranciaMinutos, Formatos.hora(RegrasDose.limiteTolerancia(d.dataHoraPrevista, d.toleranciaMinutos))));

        LinearLayout lista = findViewById(R.id.lista_notificacoes);
        lista.removeAllViews();
        if (d.notificacoes == null || d.notificacoes.isEmpty()) {
            lista.addView(linha(getString(R.string.nenhuma_notificacao)));
        } else {
            for (Notificacao n : d.notificacoes) {
                lista.addView(linha(HistoricoAdapter.linhaNotificacao(this, n)));
            }
        }
        boolean confirmada = d.estadoDose().isConfirmada();
        boolean podeRegistrar = !confirmada && RegrasDose.podeConfirmar(d.dataHoraPrevista, java.time.Instant.now());
        findViewById(R.id.botao_registrar).setVisibility(podeRegistrar ? View.VISIBLE : View.GONE);
        findViewById(R.id.texto_aviso_registro).setVisibility(View.VISIBLE);
    }

    private static Dose semNotificacoes(Dose d) {
        Dose copia = new Dose();
        copia.confirmacao = d.confirmacao;
        return copia;
    }

    private TextView linha(String texto) {
        TextView t = new TextView(this);
        t.setText(texto);
        t.setTextAppearance(R.style.Texto_Corpo);
        int p = getResources().getDimensionPixelSize(R.dimen.espaco_pequeno);
        t.setPadding(0, p, 0, p);
        return t;
    }

    private void confirmarRegistro() {
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.registrar_tomada)
                .setMessage(R.string.registrar_tomada_mensagem)
                .setPositiveButton(R.string.registrar, (dialog, w) ->
                        Repositorios.get(this).confirmarDose(doseId, new Callback<ConfirmacaoResponse>() {
                            @Override
                            public void sucesso(ConfirmacaoResponse r) {
                                Ui.mensagem(findViewById(R.id.raiz), getString(r.jaConfirmada
                                        ? R.string.dose_ja_confirmada : R.string.registro_feito));
                                carregar();
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
