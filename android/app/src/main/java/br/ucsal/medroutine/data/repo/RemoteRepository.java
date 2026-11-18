package br.ucsal.medroutine.data.repo;

import android.content.Context;

import java.io.IOException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

import br.ucsal.medroutine.data.model.Agenda;
import br.ucsal.medroutine.data.model.Codigo;
import br.ucsal.medroutine.data.model.ConfirmacaoResponse;
import br.ucsal.medroutine.data.model.Dose;
import br.ucsal.medroutine.data.model.ErroApi;
import br.ucsal.medroutine.data.model.Idoso;
import br.ucsal.medroutine.data.model.Medicamento;
import br.ucsal.medroutine.data.model.MedicamentoRequest;
import br.ucsal.medroutine.data.model.Notificacao;
import br.ucsal.medroutine.data.model.NovoIdosoResponse;
import br.ucsal.medroutine.data.model.Requisicoes;
import br.ucsal.medroutine.data.model.TokenResponse;
import br.ucsal.medroutine.data.remote.ApiClient;
import br.ucsal.medroutine.data.remote.ApiService;
import br.ucsal.medroutine.data.remote.GsonFactory;
import retrofit2.Call;
import retrofit2.Response;

public class RemoteRepository implements MedRoutineRepository {
    private final Context context;

    public RemoteRepository(Context context) {
        this.context = context.getApplicationContext();
    }

    private ApiService api() {
        return ApiClient.get(context);
    }

    private static <T> void executar(Call<T> call, Callback<T> cb) {
        call.enqueue(new retrofit2.Callback<T>() {
            @Override
            public void onResponse(Call<T> c, Response<T> resposta) {
                if (resposta.isSuccessful()) {
                    cb.sucesso(resposta.body());
                } else {
                    cb.erro(lerErro(resposta));
                }
            }

            @Override
            public void onFailure(Call<T> c, Throwable t) {
                cb.erro(ErroApi.conexao());
            }
        });
    }

    public static ErroApi lerErro(Response<?> resposta) {
        ErroApi erro = null;
        try {
            if (resposta.errorBody() != null) {
                erro = GsonFactory.get().fromJson(resposta.errorBody().charStream(), ErroApi.class);
            }
        } catch (RuntimeException ignorado) {
            erro = null;
        }
        if (erro == null || erro.mensagem == null) {
            erro = new ErroApi(resposta.code() == 401
                    ? "Sua sessão expirou. Entre novamente."
                    : resposta.code() == 403 ? "Você não tem acesso a estes dados." : "Não foi possível concluir a operação.",
                    null);
        }
        erro.status = resposta.code();
        return erro;
    }

    @Override
    public void login(String email, String senha, Callback<TokenResponse> cb) {
        executar(api().login(new Requisicoes.Login(email, senha)), cb);
    }

    @Override
    public void cadastrar(String nome, String email, String senha, boolean consentimento, Callback<TokenResponse> cb) {
        executar(api().cadastrar(new Requisicoes.Cadastro(nome, email, senha, consentimento)), cb);
    }

    @Override
    public void recuperarSenha(String email, Callback<Void> cb) {
        executar(api().recuperarSenha(new Requisicoes.Email(email)), cb);
    }

    @Override
    public void redefinirSenha(String email, String codigo, String novaSenha, Callback<Void> cb) {
        executar(api().redefinirSenha(new Requisicoes.RedefinirSenha(email, codigo, novaSenha)), cb);
    }

    @Override
    public void entrarComCodigo(String codigo, Callback<TokenResponse> cb) {
        executar(api().entrarComCodigo(new Requisicoes.CodigoReq(codigo)), cb);
    }

    @Override
    public void registrarTokenAparelho(String token, Callback<Void> cb) {
        executar(api().registrarTokenAparelho(new Requisicoes.Token(token)), cb);
    }

    @Override
    public void sair(Callback<Void> cb) {
        executar(api().sair(), cb);
    }

    @Override
    public void excluirConta(Callback<Void> cb) {
        executar(api().excluirConta(), cb);
    }

    @Override
    public void listarIdosos(Callback<List<Idoso>> cb) {
        executar(api().idosos(), cb);
    }

    @Override
    public void criarIdoso(String nome, boolean anuncioVoz, Callback<NovoIdosoResponse> cb) {
        executar(api().criarIdoso(new Requisicoes.IdosoReq(nome, ZoneId.systemDefault().getId(), anuncioVoz)), cb);
    }

    @Override
    public void atualizarIdoso(long idosoId, String nome, boolean anuncioVoz, Callback<Idoso> cb) {
        executar(api().atualizarIdoso(idosoId, new Requisicoes.IdosoReq(nome, null, anuncioVoz)), cb);
    }

    @Override
    public void excluirIdoso(long idosoId, Callback<Void> cb) {
        executar(api().excluirIdoso(idosoId), cb);
    }

    @Override
    public void gerarCodigoAcesso(long idosoId, Callback<Codigo> cb) {
        executar(api().gerarCodigoAcesso(idosoId), cb);
    }

    @Override
    public void gerarConvite(long idosoId, Callback<Codigo> cb) {
        executar(api().gerarConvite(idosoId), cb);
    }

    @Override
    public void aceitarConvite(String codigo, Callback<Idoso> cb) {
        executar(api().aceitarConvite(new Requisicoes.CodigoReq(codigo)), cb);
    }

    @Override
    public void desvincular(long idosoId, Callback<Void> cb) {
        executar(api().desvincular(idosoId), cb);
    }

    @Override
    public void listarMedicamentos(long idosoId, Callback<List<Medicamento>> cb) {
        executar(api().medicamentos(idosoId), cb);
    }

    @Override
    public void buscarMedicamento(long medicamentoId, Callback<Medicamento> cb) {
        executar(api().medicamento(medicamentoId), cb);
    }

    @Override
    public void salvarMedicamento(long idosoId, Long medicamentoId, MedicamentoRequest req, Callback<Medicamento> cb) {
        if (medicamentoId == null) {
            executar(api().criarMedicamento(idosoId, req), cb);
        } else {
            executar(api().atualizarMedicamento(medicamentoId, req), cb);
        }
    }

    @Override
    public void desativarMedicamento(long medicamentoId, Callback<Medicamento> cb) {
        executar(api().desativarMedicamento(medicamentoId), cb);
    }

    @Override
    public void dosesDoDia(long idosoId, LocalDate data, Callback<List<Dose>> cb) {
        executar(api().dosesDoDia(idosoId, data.toString()), cb);
    }

    @Override
    public void historico(long idosoId, LocalDate inicio, LocalDate fim, Long medicamentoId, Callback<List<Dose>> cb) {
        executar(api().historico(idosoId, inicio.toString(), fim.toString(), medicamentoId), cb);
    }

    @Override
    public void notificacoes(Callback<List<Notificacao>> cb) {
        executar(api().notificacoes(), cb);
    }

    @Override
    public void dose(long doseId, Callback<Dose> cb) {
        executar(api().dose(doseId), cb);
    }

    @Override
    public void confirmarDose(long doseId, Callback<ConfirmacaoResponse> cb) {
        executar(api().confirmar(doseId, new Requisicoes.Confirmar(Instant.now())), cb);
    }

    @Override
    public void agenda(Callback<Agenda> cb) {
        executar(api().agenda(), cb);
    }

    public Response<ConfirmacaoResponse> confirmarSincrono(long doseId, Instant horarioAparelho) throws IOException {
        return api().confirmar(doseId, new Requisicoes.Confirmar(horarioAparelho)).execute();
    }

    public Response<Agenda> agendaSincrona() throws IOException {
        return api().agenda().execute();
    }
}
