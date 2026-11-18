package br.ucsal.medroutine.ui.cuidador;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.textfield.MaterialAutoCompleteTextView;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

import br.ucsal.medroutine.R;
import br.ucsal.medroutine.data.model.Dose;
import br.ucsal.medroutine.data.model.ErroApi;
import br.ucsal.medroutine.data.model.Medicamento;
import br.ucsal.medroutine.data.repo.Callback;
import br.ucsal.medroutine.data.repo.Repositorios;
import br.ucsal.medroutine.ui.comum.Formatos;
import br.ucsal.medroutine.ui.comum.Ui;

public class HistoricoFragment extends Fragment {
    private long idosoId;
    private LocalDate inicio = Formatos.hoje().minusDays(6);
    private LocalDate fim = Formatos.hoje();
    private Long medicamentoId;
    private final List<Medicamento> medicamentos = new ArrayList<>();
    private HistoricoAdapter adapter;

    public static HistoricoFragment novo(long idosoId) {
        HistoricoFragment f = new HistoricoFragment();
        Bundle b = new Bundle();
        b.putLong("idosoId", idosoId);
        f.setArguments(b);
        return f;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_historico, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View v, @Nullable Bundle savedInstanceState) {
        idosoId = requireArguments().getLong("idosoId");
        RecyclerView lista = v.findViewById(R.id.lista_historico);
        lista.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new HistoricoAdapter();
        lista.setAdapter(adapter);

        EditText entradaInicio = v.findViewById(R.id.entrada_inicio);
        EditText entradaFim = v.findViewById(R.id.entrada_fim);
        entradaInicio.setText(Formatos.data(inicio));
        entradaFim.setText(Formatos.data(fim));
        entradaInicio.setOnClickListener(b -> escolherData(true, entradaInicio));
        entradaFim.setOnClickListener(b -> escolherData(false, entradaFim));
        v.findViewById(R.id.botao_filtrar).setOnClickListener(b -> filtrar());

        carregarMedicamentos(v);
        filtrar();
    }

    private void escolherData(boolean ehInicio, EditText campo) {
        LocalDate atual = ehInicio ? inicio : fim;
        MaterialDatePicker<Long> picker = MaterialDatePicker.Builder.datePicker()
                .setTitleText(ehInicio ? R.string.de : R.string.ate)
                .setSelection(atual.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli())
                .build();
        picker.addOnPositiveButtonClickListener(ms -> {
            LocalDate escolhida = Instant.ofEpochMilli(ms).atZone(ZoneOffset.UTC).toLocalDate();
            if (ehInicio) inicio = escolhida; else fim = escolhida;
            campo.setText(Formatos.data(escolhida));
        });
        picker.show(getParentFragmentManager(), "data");
    }

    private void carregarMedicamentos(View v) {
        Repositorios.get(requireContext()).listarMedicamentos(idosoId, new Callback<List<Medicamento>>() {
            @Override
            public void sucesso(List<Medicamento> lista) {
                if (!isAdded()) return;
                medicamentos.clear();
                medicamentos.addAll(lista);
                List<String> nomes = new ArrayList<>();
                nomes.add(getString(R.string.todos_medicamentos));
                for (Medicamento m : lista) {
                    nomes.add(m.nome + " " + m.dosagem);
                }
                MaterialAutoCompleteTextView campo = v.findViewById(R.id.entrada_medicamento);
                campo.setAdapter(new ArrayAdapter<>(requireContext(), android.R.layout.simple_list_item_1, nomes));
                campo.setText(nomes.get(0), false);
                campo.setOnItemClickListener((p, view, pos, id) ->
                        medicamentoId = pos == 0 ? null : medicamentos.get(pos - 1).id);
            }

            @Override
            public void erro(ErroApi erro) { }
        });
    }

    private void filtrar() {
        View v = requireView();
        if (fim.isBefore(inicio)) {
            Ui.mensagem(v, getString(R.string.erro_periodo));
            return;
        }
        View progresso = v.findViewById(R.id.progresso);
        progresso.setVisibility(View.VISIBLE);
        Repositorios.get(requireContext()).historico(idosoId, inicio, fim, medicamentoId, new Callback<List<Dose>>() {
            @Override
            public void sucesso(List<Dose> doses) {
                if (!isAdded()) return;
                progresso.setVisibility(View.GONE);
                adapter.atualizar(doses);
                v.findViewById(R.id.texto_vazio).setVisibility(doses.isEmpty() ? View.VISIBLE : View.GONE);
            }

            @Override
            public void erro(ErroApi erro) {
                if (!isAdded()) return;
                progresso.setVisibility(View.GONE);
                Ui.erro(v, erro);
            }
        });
    }
}
