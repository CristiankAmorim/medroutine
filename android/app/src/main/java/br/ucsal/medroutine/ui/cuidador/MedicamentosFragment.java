package br.ucsal.medroutine.ui.cuidador;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.List;

import br.ucsal.medroutine.R;
import br.ucsal.medroutine.data.model.ErroApi;
import br.ucsal.medroutine.data.model.Medicamento;
import br.ucsal.medroutine.data.repo.Callback;
import br.ucsal.medroutine.data.repo.Repositorios;
import br.ucsal.medroutine.ui.comum.Ui;

public class MedicamentosFragment extends Fragment implements MedicamentoAdapter.Acoes {
    private long idosoId;
    private String nomeIdoso;
    private SwipeRefreshLayout atualizar;
    private MedicamentoAdapter adapter;

    public static MedicamentosFragment novo(long idosoId, String nomeIdoso) {
        MedicamentosFragment f = new MedicamentosFragment();
        Bundle b = new Bundle();
        b.putLong("idosoId", idosoId);
        b.putString("nome", nomeIdoso);
        f.setArguments(b);
        return f;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_medicamentos, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View v, @Nullable Bundle savedInstanceState) {
        idosoId = requireArguments().getLong("idosoId");
        nomeIdoso = requireArguments().getString("nome");
        atualizar = v.findViewById(R.id.atualizar);
        atualizar.setOnRefreshListener(this::carregar);
        RecyclerView lista = v.findViewById(R.id.lista_medicamentos);
        lista.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new MedicamentoAdapter(this);
        lista.setAdapter(adapter);
        v.findViewById(R.id.botao_novo).setOnClickListener(b ->
                startActivity(MedicamentoFormActivity.novo(requireContext(), idosoId, nomeIdoso)));
    }

    @Override
    public void onResume() {
        super.onResume();
        carregar();
    }

    private void carregar() {
        atualizar.setRefreshing(true);
        Repositorios.get(requireContext()).listarMedicamentos(idosoId, new Callback<List<Medicamento>>() {
            @Override
            public void sucesso(List<Medicamento> lista) {
                if (!isAdded()) return;
                atualizar.setRefreshing(false);
                adapter.atualizar(lista);
                requireView().findViewById(R.id.texto_vazio).setVisibility(lista.isEmpty() ? View.VISIBLE : View.GONE);
            }

            @Override
            public void erro(ErroApi erro) {
                if (!isAdded()) return;
                atualizar.setRefreshing(false);
                Ui.erro(requireView(), erro);
            }
        });
    }

    @Override
    public void abrir(Medicamento m) {
        if (m.ativo) {
            startActivity(MedicamentoFormActivity.editar(requireContext(), idosoId, nomeIdoso, m.id));
        } else {
            Ui.mensagem(requireView(), getString(R.string.medicamento_desativado_info));
        }
    }

    @Override
    public void opcoes(Medicamento m) {
        if (!m.ativo) {
            abrir(m);
            return;
        }
        String[] opcoes = {getString(R.string.editar), getString(R.string.desativar)};
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(m.nome + " " + m.dosagem)
                .setItems(opcoes, (d, qual) -> {
                    if (qual == 0) {
                        abrir(m);
                    } else {
                        confirmarDesativar(m);
                    }
                })
                .show();
    }

    private void confirmarDesativar(Medicamento m) {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.desativar_titulo)
                .setMessage(getString(R.string.desativar_mensagem, m.nome))
                .setPositiveButton(R.string.desativar, (d, w) ->
                        Repositorios.get(requireContext()).desativarMedicamento(m.id, new Callback<Medicamento>() {
                            @Override
                            public void sucesso(Medicamento r) {
                                if (isAdded()) carregar();
                            }

                            @Override
                            public void erro(ErroApi erro) {
                                if (isAdded()) Ui.erro(requireView(), erro);
                            }
                        }))
                .setNegativeButton(R.string.cancelar, null)
                .show();
    }
}
