package br.ucsal.medroutine.ui.cuidador;

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

import java.util.ArrayList;
import java.util.List;

import br.ucsal.medroutine.R;
import br.ucsal.medroutine.data.model.Notificacao;
import br.ucsal.medroutine.ui.comum.Formatos;

public class NotificacaoAdapter extends RecyclerView.Adapter<NotificacaoAdapter.VH> {
    private final List<Notificacao> itens = new ArrayList<>();

    public void atualizar(List<Notificacao> novas) {
        itens.clear();
        itens.addAll(novas);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new VH(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_notificacao, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        Notificacao n = itens.get(position);
        Context c = h.itemView.getContext();
        boolean alerta = Notificacao.ALERTA.equals(n.tipo);
        boolean atraso = !alerta && "CONFIRMADA_COM_ATRASO".equals(n.estadoDose);
        String medicamento = n.medicamentoNome + " " + (n.dosagem == null ? "" : n.dosagem);

        int titulo = alerta ? R.string.push_alerta_titulo : atraso ? R.string.push_atraso_titulo : R.string.push_confirmada_titulo;
        h.titulo.setText(titulo);
        h.titulo.setTextColor(ContextCompat.getColor(c, alerta || atraso ? R.color.alerta : R.color.texto));
        h.quando.setText(c.getString(R.string.idoso_e_hora, n.idosoNome, Formatos.dataHora(n.criadoEm)));
        h.corpo.setText(alerta
                ? c.getString(R.string.notificacao_alerta_corpo, medicamento, Formatos.hora(n.dataHoraPrevista), n.toleranciaMinutos)
                : c.getString(R.string.notificacao_confirmacao_corpo, medicamento, Formatos.hora(n.dataHoraPrevista)));
        h.situacao.setText(c.getString(R.string.situacao_atual, Formatos.nomeEstado(c, n.estadoDose)));

        h.icone.setImageResource(alerta ? R.drawable.ic_alerta : atraso ? R.drawable.ic_relogio : R.drawable.ic_check);
        int cor = ContextCompat.getColor(c, alerta || atraso ? R.color.alerta : R.color.confirmacao);
        h.icone.setImageTintList(ColorStateList.valueOf(cor));
        h.icone.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(c,
                alerta ? R.color.alerta_fundo : R.color.confirmacao_fundo)));
        h.itemView.setOnClickListener(v -> c.startActivity(DoseDetalheActivity.intent(c, n.doseId, false)));
    }

    @Override
    public int getItemCount() {
        return itens.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        final TextView titulo, quando, corpo, situacao;
        final ImageView icone;

        VH(View v) {
            super(v);
            titulo = v.findViewById(R.id.texto_titulo);
            quando = v.findViewById(R.id.texto_quando);
            corpo = v.findViewById(R.id.texto_corpo);
            situacao = v.findViewById(R.id.texto_situacao);
            icone = v.findViewById(R.id.icone);
        }
    }
}
