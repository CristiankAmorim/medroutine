package br.ucsal.medroutine.data.repo;

import java.time.LocalDate;
import java.util.List;

import br.ucsal.medroutine.data.model.Agenda;
import br.ucsal.medroutine.data.model.Codigo;
import br.ucsal.medroutine.data.model.ConfirmacaoResponse;
import br.ucsal.medroutine.data.model.Dose;
import br.ucsal.medroutine.data.model.Idoso;
import br.ucsal.medroutine.data.model.Medicamento;
import br.ucsal.medroutine.data.model.MedicamentoRequest;
import br.ucsal.medroutine.data.model.Notificacao;
import br.ucsal.medroutine.data.model.NovoIdosoResponse;
import br.ucsal.medroutine.data.model.TokenResponse;

public interface MedRoutineRepository {
    void login(String email, String senha, Callback<TokenResponse> cb);

    void cadastrar(String nome, String email, String senha, boolean consentimento, Callback<TokenResponse> cb);

    void recuperarSenha(String email, Callback<Void> cb);

    void redefinirSenha(String email, String codigo, String novaSenha, Callback<Void> cb);

    void entrarComCodigo(String codigo, Callback<TokenResponse> cb);

    void registrarTokenAparelho(String token, Callback<Void> cb);

    void sair(Callback<Void> cb);

    void excluirConta(Callback<Void> cb);

    void listarIdosos(Callback<List<Idoso>> cb);

    void criarIdoso(String nome, boolean anuncioVoz, Callback<NovoIdosoResponse> cb);

    void atualizarIdoso(long idosoId, String nome, boolean anuncioVoz, Callback<Idoso> cb);

    void excluirIdoso(long idosoId, Callback<Void> cb);

    void gerarCodigoAcesso(long idosoId, Callback<Codigo> cb);

    void gerarConvite(long idosoId, Callback<Codigo> cb);

    void aceitarConvite(String codigo, Callback<Idoso> cb);

    void desvincular(long idosoId, Callback<Void> cb);

    void listarMedicamentos(long idosoId, Callback<List<Medicamento>> cb);

    void buscarMedicamento(long medicamentoId, Callback<Medicamento> cb);

    void salvarMedicamento(long idosoId, Long medicamentoId, MedicamentoRequest req, Callback<Medicamento> cb);

    void desativarMedicamento(long medicamentoId, Callback<Medicamento> cb);

    void dosesDoDia(long idosoId, LocalDate data, Callback<List<Dose>> cb);

    void historico(long idosoId, LocalDate inicio, LocalDate fim, Long medicamentoId, Callback<List<Dose>> cb);

    void notificacoes(Callback<List<Notificacao>> cb);

    void dose(long doseId, Callback<Dose> cb);

    void confirmarDose(long doseId, Callback<ConfirmacaoResponse> cb);

    void agenda(Callback<Agenda> cb);
}
