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
import br.ucsal.medroutine.data.model.Dose;
import br.ucsal.medroutine.domain.EstadoDose;
import br.ucsal.medroutine.ui.comum.Formatos;

public class DoseCuidadorAdapter extends RecyclerView.Adapter<DoseCuidadorAdapter.VH> {
    public interface AoClicar {
        void clicar(Dose dose);
    }

    private final List<Dose> doses = new ArrayList<>();
    private final AoClicar aoClicar;

    public DoseCuidadorAdapter(AoClicar aoClicar) {
        this.aoClicar = aoClicar;
    }

    public void atualizar(List<Dose> novas) {
        doses.clear();
        doses.addAll(novas);
        notifyDataSetChanged();
    }

    static String textoEstado(Context c, Dose d) {
        String estado = Formatos.nomeEstado(c, d.estado);
        if (d.confirmacao != null && d.confirmacao.confirmadoEm != null) {
            java.time.Instant momento = br.ucsal.medroutine.domain.RegrasDose.momentoDaConfirmacao(
                    d.dataHoraPrevista, d.confirmacao.confirmadoEm, d.confirmacao.horarioAparelho);
            estado += " · " + Formatos.hora(momento);
            if (d.confirmacao.peloCuidador) {
                estado += " · " + c.getString(R.string.registrada_por, d.confirmacao.confirmadoPor);
            }
        }
        return estado;
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new VH(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_dose_cuidador, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        Dose d = doses.get(position);
        Context c = h.itemView.getContext();
        String nome = d.medicamentoNome + " " + (d.dosagem == null ? "" : d.dosagem);
        String estado = textoEstado(c, d);
        int cor = ContextCompat.getColor(c, Formatos.corEstado(d.estado));
        h.hora.setText(Formatos.hora(d.dataHoraPrevista));
        h.medicamento.setText(nome.trim());
        h.estado.setText(estado);
        h.estado.setTextColor(cor);
        h.icone.setImageResource(Formatos.iconeEstado(d.estado));
        h.icone.setImageTintList(ColorStateList.valueOf(cor));
        EstadoDose e = d.estadoDose();
        int fundo = e == EstadoDose.CONFIRMADA ? R.color.confirmacao_fundo
                : e == EstadoDose.PENDENTE ? R.color.primaria_clara : R.color.alerta_fundo;
        h.icone.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(c, fundo)));
        h.itemView.setContentDescription(Formatos.hora(d.dataHoraPrevista) + ", " + nome + ". " + estado);
        h.itemView.setOnClickListener(v -> aoClicar.clicar(d));
    }

    @Override
    public int getItemCount() {
        return doses.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        final TextView hora, medicamento, estado;
        final ImageView icone;

        VH(View v) {
            super(v);
            hora = v.findViewById(R.id.texto_hora);
            medicamento = v.findViewById(R.id.texto_medicamento);
            estado = v.findViewById(R.id.texto_estado);
            icone = v.findViewById(R.id.icone_estado);
        }
    }
}
