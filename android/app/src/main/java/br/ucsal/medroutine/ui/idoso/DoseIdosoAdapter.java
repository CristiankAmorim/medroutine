package br.ucsal.medroutine.ui.idoso;

import android.content.Context;
import android.content.res.ColorStateList;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import br.ucsal.medroutine.R;
import br.ucsal.medroutine.data.local.DoseLocal;
import br.ucsal.medroutine.domain.RegrasDose;
import br.ucsal.medroutine.ui.comum.Formatos;

public class DoseIdosoAdapter extends RecyclerView.Adapter<DoseIdosoAdapter.VH> {
    public interface AoConfirmar {
        void confirmar(DoseLocal dose);
    }

    private final List<DoseLocal> doses = new ArrayList<>();
    private final AoConfirmar aoConfirmar;

    public DoseIdosoAdapter(AoConfirmar aoConfirmar) {
        this.aoConfirmar = aoConfirmar;
    }

    public void atualizar(List<DoseLocal> novas) {
        doses.clear();
        doses.addAll(novas);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new VH(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_dose_idoso, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        DoseLocal d = doses.get(position);
        Context c = h.itemView.getContext();
        Instant agora = Instant.now();
        String nome = d.medicamentoNome + (d.dosagem == null ? "" : " " + d.dosagem);
        h.hora.setText(Formatos.hora(d.previsto()));
        h.medicamento.setText(nome);

        String situacao;
        int cor = R.color.texto_secundario;
        int icone;
        int fundoIcone = R.color.primaria_clara;
        boolean podeConfirmar = false;
        String orientacao = d.orientacoes == null ? "" : " · " + d.orientacoes;

        if (d.isConfirmada()) {
            String hora = Formatos.hora(Instant.ofEpochMilli(d.confirmadaEm == null ? d.previstaEm : d.confirmadaEm));
            situacao = d.peloCuidador && d.confirmadaPor != null
                    ? c.getString(R.string.confirmada_pelo_cuidador, d.confirmadaPor, hora)
                    : c.getString(R.string.tomado_as, hora);
            if (d.aguardandoEnvio) {
                situacao += " · " + c.getString(R.string.aguardando_internet);
            }
            cor = R.color.confirmacao;
            icone = R.drawable.ic_check;
            fundoIcone = R.color.confirmacao_fundo;
        } else if (RegrasDose.podeConfirmar(d.previsto(), agora)) {
            podeConfirmar = true;
            if (agora.isBefore(d.previsto())) {
                situacao = c.getString(R.string.daqui_a_pouco) + orientacao;
            } else if (!agora.isAfter(RegrasDose.limiteTolerancia(d.previsto(), d.toleranciaMinutos))) {
                situacao = c.getString(R.string.agora) + orientacao;
                cor = R.color.primaria;
            } else {
                situacao = c.getString(R.string.atrasado_ainda_pode);
                cor = R.color.alerta;
            }
            icone = R.drawable.ic_relogio;
        } else {
            situacao = c.getString(R.string.mais_tarde);
            icone = R.drawable.ic_relogio;
        }

        h.situacao.setText(situacao);
        h.situacao.setTextColor(ContextCompat.getColor(c, cor));
        h.icone.setImageResource(icone);
        h.icone.setImageTintList(ColorStateList.valueOf(ContextCompat.getColor(c,
                d.isConfirmada() ? R.color.confirmacao : R.color.primaria)));
        h.icone.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(c, fundoIcone)));
        h.botao.setVisibility(podeConfirmar ? View.VISIBLE : View.GONE);
        h.botao.setContentDescription(c.getString(R.string.ja_tomei_descricao, nome));
        h.botao.setOnClickListener(v -> aoConfirmar.confirmar(d));
        h.itemView.setContentDescription(Formatos.hora(d.previsto()) + ", " + nome + ". " + situacao);
    }

    @Override
    public int getItemCount() {
        return doses.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        final TextView hora, medicamento, situacao;
        final ImageView icone;
        final View botao;

        VH(View v) {
            super(v);
            hora = v.findViewById(R.id.texto_hora);
            medicamento = v.findViewById(R.id.texto_medicamento);
            situacao = v.findViewById(R.id.texto_situacao);
            icone = v.findViewById(R.id.icone_estado);
            botao = v.findViewById(R.id.botao_tomei);
        }
    }
}
