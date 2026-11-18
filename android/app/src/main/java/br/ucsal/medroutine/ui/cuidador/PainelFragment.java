package br.ucsal.medroutine.ui.cuidador;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import java.util.List;

import br.ucsal.medroutine.R;
import br.ucsal.medroutine.data.model.Dose;
import br.ucsal.medroutine.data.model.ErroApi;
import br.ucsal.medroutine.data.repo.Callback;
import br.ucsal.medroutine.data.repo.Repositorios;
import br.ucsal.medroutine.domain.EstadoDose;
import br.ucsal.medroutine.domain.RegrasDose;
import br.ucsal.medroutine.ui.comum.Formatos;
import br.ucsal.medroutine.ui.comum.PermissoesHelper;
import br.ucsal.medroutine.ui.comum.Ui;

public class PainelFragment extends Fragment {
    private long idosoId;
    private String nome;
    private SwipeRefreshLayout atualizar;
    private DoseCuidadorAdapter adapter;

    public static PainelFragment novo(long idosoId, String nome) {
        PainelFragment f = new PainelFragment();
        Bundle b = new Bundle();
        b.putLong("idosoId", idosoId);
        b.putString("nome", nome);
        f.setArguments(b);
        return f;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_painel, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View v, @Nullable Bundle savedInstanceState) {
        idosoId = requireArguments().getLong("idosoId");
        nome = requireArguments().getString("nome");
        atualizar = v.findViewById(R.id.atualizar);
        atualizar.setOnRefreshListener(this::carregar);
        RecyclerView lista = v.findViewById(R.id.lista_doses);
        lista.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new DoseCuidadorAdapter(d -> startActivity(DoseDetalheActivity.intent(requireContext(), d.id, false)));
        lista.setAdapter(adapter);
        ((TextView) v.findViewById(R.id.texto_idoso_nome)).setText(nome);
        ((TextView) v.findViewById(R.id.texto_data)).setText(
                getString(R.string.hoje_data, Formatos.diaMes(Formatos.hoje())));
    }

    @Override
    public void onResume() {
        super.onResume();
        View v = getView();
        if (v != null) {
            PermissoesHelper.atualizarAviso((AppCompatActivity) requireActivity(),
                    v.findViewById(R.id.aviso_permissoes), false);
        }
        carregar();
    }

    private void carregar() {
        atualizar.setRefreshing(true);
        Repositorios.get(requireContext()).dosesDoDia(idosoId, Formatos.hoje(), new Callback<List<Dose>>() {
            @Override
            public void sucesso(List<Dose> doses) {
                if (!isAdded()) return;
                atualizar.setRefreshing(false);
                exibir(doses);
            }

            @Override
            public void erro(ErroApi erro) {
                if (!isAdded()) return;
                atualizar.setRefreshing(false);
                Ui.erro(requireView(), erro);
            }
        });
    }

    private void exibir(List<Dose> doses) {
        View v = requireView();
        adapter.atualizar(doses);
        v.findViewById(R.id.texto_vazio).setVisibility(doses.isEmpty() ? View.VISIBLE : View.GONE);
        int confirmadas = 0;
        Dose alerta = null;
        for (Dose d : doses) {
            if (d.estadoDose().isConfirmada()) {
                confirmadas++;
            } else if (d.estadoDose() == EstadoDose.NAO_CONFIRMADA && alerta == null) {
                alerta = d;
            }
        }
        ((TextView) v.findViewById(R.id.texto_resumo)).setText(
                getResources().getQuantityString(R.plurals.resumo_doses, doses.size(), confirmadas, doses.size()));

        View cartao = v.findViewById(R.id.cartao_alerta);
        if (alerta == null) {
            cartao.setVisibility(View.GONE);
            return;
        }
        final Dose d = alerta;
        cartao.setVisibility(View.VISIBLE);
        ((TextView) v.findViewById(R.id.texto_alerta_titulo)).setText(
                getString(R.string.alerta_painel_titulo, d.medicamentoNome, Formatos.hora(d.dataHoraPrevista)));
        ((TextView) v.findViewById(R.id.texto_alerta_detalhe)).setText(getString(R.string.alerta_painel_detalhe,
                d.toleranciaMinutos, Formatos.hora(RegrasDose.limiteTolerancia(d.dataHoraPrevista, d.toleranciaMinutos))));
        v.findViewById(R.id.botao_alerta_registrar).setOnClickListener(b ->
                startActivity(DoseDetalheActivity.intent(requireContext(), d.id, true)));
    }
}
