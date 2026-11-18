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
import br.ucsal.medroutine.data.model.Idoso;

public class IdosoAdapter extends RecyclerView.Adapter<IdosoAdapter.VH> {
    public interface Acoes {
        void codigoAcesso(Idoso i);

        void convite(Idoso i);

        void opcoes(Idoso i);
    }

    private final List<Idoso> itens = new ArrayList<>();
    private final Acoes acoes;

    public IdosoAdapter(Acoes acoes) {
        this.acoes = acoes;
    }

    public void atualizar(List<Idoso> novos) {
        itens.clear();
        itens.addAll(novos);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new VH(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_idoso, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        Idoso i = itens.get(position);
        Context c = h.itemView.getContext();
        h.nome.setText(i.nome);
        String detalhe = c.getResources().getQuantityString(R.plurals.total_cuidadores, (int) i.totalCuidadores,
                (int) i.totalCuidadores);
        if (i.anuncioVoz) {
            detalhe += " · " + c.getString(R.string.voz_ativada);
        }
        h.detalhe.setText(detalhe);
        h.codigo.setOnClickListener(v -> acoes.codigoAcesso(i));
        h.convite.setOnClickListener(v -> acoes.convite(i));
        h.opcoes.setOnClickListener(v -> acoes.opcoes(i));
    }

    @Override
    public int getItemCount() {
        return itens.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        final TextView nome, detalhe;
        final View codigo, convite, opcoes;

        VH(View v) {
            super(v);
            nome = v.findViewById(R.id.texto_nome);
            detalhe = v.findViewById(R.id.texto_detalhe);
            codigo = v.findViewById(R.id.botao_codigo);
            convite = v.findViewById(R.id.botao_convite);
            opcoes = v.findViewById(R.id.botao_opcoes);
        }
    }
}
