package br.ucsal.medroutine.ui.cuidador;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import br.ucsal.medroutine.R;
import br.ucsal.medroutine.data.model.Dose;
import br.ucsal.medroutine.data.model.Notificacao;
import br.ucsal.medroutine.ui.comum.Formatos;

public class HistoricoAdapter extends RecyclerView.Adapter<HistoricoAdapter.VH> {
    private final List<Dose> doses = new ArrayList<>();

    public void atualizar(List<Dose> novas) {
        doses.clear();
        doses.addAll(novas);
        notifyDataSetChanged();
    }

    static String detalhes(Context c, Dose d) {
        StringBuilder sb = new StringBuilder();
        if (d.confirmacao != null) {
            sb.append(c.getString(R.string.confirmada_por, d.confirmacao.confirmadoPor,
                    Formatos.hora(d.confirmacao.confirmadoEm)));
            if (d.confirmacao.horarioAparelho != null
                    && !Formatos.hora(d.confirmacao.horarioAparelho).equals(Formatos.hora(d.confirmacao.confirmadoEm))) {
                sb.append(" · ").append(c.getString(d.confirmacao.peloCuidador
                        ? R.string.horario_aparelho_cuidador : R.string.horario_aparelho,
                        Formatos.hora(d.confirmacao.horarioAparelho)));
            }
        } else {
            sb.append(c.getString(R.string.sem_confirmacao));
        }
        if (d.notificacoes != null) {
            for (Notificacao n : d.notificacoes) {
                sb.append('\n').append(linhaNotificacao(c, n));
            }
        }
        return sb.toString();
    }

    static String linhaNotificacao(Context c, Notificacao n) {
        String tipo = c.getString(Notificacao.ALERTA.equals(n.tipo) ? R.string.tipo_alerta : R.string.tipo_confirmacao);
        String estado;
        if ("ENVIADA".equals(n.estadoEnvio)) {
            estado = c.getString(R.string.envio_enviada, Formatos.hora(n.enviadoEm));
        } else if ("FALHA".equals(n.estadoEnvio)) {
            estado = c.getString(R.string.envio_falha);
        } else {
            estado = c.getString(R.string.envio_pendente);
        }
        return c.getString(R.string.linha_notificacao, tipo, n.destinatario, estado,
                c.getResources().getQuantityString(R.plurals.tentativas, n.tentativas, n.tentativas));
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new VH(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_historico, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        Dose d = doses.get(position);
        Context c = h.itemView.getContext();
        h.previsto.setText(Formatos.dataHora(d.dataHoraPrevista));
        h.medicamento.setText(d.medicamentoNome + " " + (d.dosagem == null ? "" : d.dosagem));
        h.estado.setText(Formatos.nomeEstado(c, d.estado));
        h.estado.setTextColor(ContextCompat.getColor(c, Formatos.corEstado(d.estado)));
        h.detalhes.setText(detalhes(c, d));
        h.itemView.setOnClickListener(v -> c.startActivity(DoseDetalheActivity.intent(c, d.id, false)));
    }

    @Override
    public int getItemCount() {
        return doses.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        final TextView previsto, medicamento, estado, detalhes;

        VH(View v) {
            super(v);
            previsto = v.findViewById(R.id.texto_previsto);
            medicamento = v.findViewById(R.id.texto_medicamento);
            estado = v.findViewById(R.id.texto_estado);
            detalhes = v.findViewById(R.id.texto_detalhes);
        }
    }
}
