package br.ucsal.medroutine.ui.cuidador;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import br.ucsal.medroutine.R;
import br.ucsal.medroutine.data.model.Horario;
import br.ucsal.medroutine.data.model.Medicamento;
import br.ucsal.medroutine.ui.comum.Formatos;

public class MedicamentoAdapter extends RecyclerView.Adapter<MedicamentoAdapter.VH> {
    public interface Acoes {
        void abrir(Medicamento m);

        void opcoes(Medicamento m);
    }

    private final List<Medicamento> itens = new ArrayList<>();
    private final Acoes acoes;

    public MedicamentoAdapter(Acoes acoes) {
        this.acoes = acoes;
    }

    public void atualizar(List<Medicamento> novos) {
        itens.clear();
        itens.addAll(novos);
        notifyDataSetChanged();
    }

    static String resumoHorarios(Context c, Medicamento m) {
        if (m.horarios == null || m.horarios.isEmpty()) {
            return c.getString(R.string.sem_horarios);
        }
        Horario primeiro = m.horarios.get(0);
        StringBuilder horas = new StringBuilder();
        boolean mesmaTolerancia = true;
        for (Horario h : m.horarios) {
            if (horas.length() > 0) horas.append(", ");
            horas.append(h.hora);
            if (h.toleranciaMinutos != null && !h.toleranciaMinutos.equals(primeiro.toleranciaMinutos)) {
                mesmaTolerancia = false;
            }
        }
        String tolerancia = mesmaTolerancia
                ? c.getString(R.string.tolerancia_resumo, primeiro.toleranciaMinutos == null ? 30 : primeiro.toleranciaMinutos)
                : c.getString(R.string.tolerancias_variadas);
        return Formatos.frequencia(c, primeiro.tipoFrequencia, primeiro.intervaloHoras) + " · " + horas + " · " + tolerancia;
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new VH(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_medicamento, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        Medicamento m = itens.get(position);
        Context c = h.itemView.getContext();
        h.nome.setText(m.nome + " " + m.dosagem);
        h.horarios.setText(m.ativo ? resumoHorarios(c, m) : c.getString(R.string.sem_lembretes));
        h.periodo.setText(m.dataFim == null
                ? c.getString(R.string.periodo_desde, Formatos.data(m.dataInicio))
                : c.getString(R.string.periodo_entre, Formatos.data(m.dataInicio), Formatos.data(m.dataFim)));
        h.inativo.setVisibility(m.ativo ? View.GONE : View.VISIBLE);
        h.itemView.setAlpha(m.ativo ? 1f : 0.7f);
        h.itemView.setOnClickListener(v -> acoes.abrir(m));
        h.opcoes.setOnClickListener(v -> acoes.opcoes(m));
    }

    @Override
    public int getItemCount() {
        return itens.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        final TextView nome, horarios, periodo, inativo;
        final View opcoes;

        VH(View v) {
            super(v);
            nome = v.findViewById(R.id.texto_nome);
            horarios = v.findViewById(R.id.texto_horarios);
            periodo = v.findViewById(R.id.texto_periodo);
            inativo = v.findViewById(R.id.texto_inativo);
            opcoes = v.findViewById(R.id.botao_opcoes);
        }
    }
}
