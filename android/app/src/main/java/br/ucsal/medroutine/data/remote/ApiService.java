package br.ucsal.medroutine.data.remote;

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
import br.ucsal.medroutine.data.model.Requisicoes;
import br.ucsal.medroutine.data.model.TokenResponse;
import br.ucsal.medroutine.data.model.Usuario;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface ApiService {
    @POST("api/auth/cadastro")
    Call<TokenResponse> cadastrar(@Body Requisicoes.Cadastro body);

    @POST("api/auth/login")
    Call<TokenResponse> login(@Body Requisicoes.Login body);

    @POST("api/auth/recuperar-senha")
    Call<Void> recuperarSenha(@Body Requisicoes.Email body);

    @POST("api/auth/redefinir-senha")
    Call<Void> redefinirSenha(@Body Requisicoes.RedefinirSenha body);

    @POST("api/auth/codigo-idoso")
    Call<TokenResponse> entrarComCodigo(@Body Requisicoes.CodigoReq body);

    @GET("api/conta")
    Call<Usuario> conta();

    @PUT("api/conta/token-aparelho")
    Call<Void> registrarTokenAparelho(@Body Requisicoes.Token body);

    @POST("api/conta/sair")
    Call<Void> sair();

    @DELETE("api/conta")
    Call<Void> excluirConta();

    @GET("api/idosos")
    Call<List<Idoso>> idosos();

    @POST("api/idosos")
    Call<NovoIdosoResponse> criarIdoso(@Body Requisicoes.IdosoReq body);

    @PUT("api/idosos/{id}")
    Call<Idoso> atualizarIdoso(@Path("id") long idosoId, @Body Requisicoes.IdosoReq body);

    @DELETE("api/idosos/{id}")
    Call<Void> excluirIdoso(@Path("id") long idosoId);

    @POST("api/idosos/{id}/codigo-acesso")
    Call<Codigo> gerarCodigoAcesso(@Path("id") long idosoId);

    @POST("api/idosos/{id}/convites")
    Call<Codigo> gerarConvite(@Path("id") long idosoId);

    @POST("api/vinculos/aceitar")
    Call<Idoso> aceitarConvite(@Body Requisicoes.CodigoReq body);

    @DELETE("api/idosos/{id}/vinculo")
    Call<Void> desvincular(@Path("id") long idosoId);

    @GET("api/idosos/{id}/medicamentos")
    Call<List<Medicamento>> medicamentos(@Path("id") long idosoId);

    @POST("api/idosos/{id}/medicamentos")
    Call<Medicamento> criarMedicamento(@Path("id") long idosoId, @Body MedicamentoRequest body);

    @GET("api/medicamentos/{id}")
    Call<Medicamento> medicamento(@Path("id") long medicamentoId);

    @PUT("api/medicamentos/{id}")
    Call<Medicamento> atualizarMedicamento(@Path("id") long medicamentoId, @Body MedicamentoRequest body);

    @POST("api/medicamentos/{id}/desativar")
    Call<Medicamento> desativarMedicamento(@Path("id") long medicamentoId);

    @GET("api/idosos/{id}/doses")
    Call<List<Dose>> dosesDoDia(@Path("id") long idosoId, @Query("data") String data);

    @GET("api/idosos/{id}/historico")
    Call<List<Dose>> historico(@Path("id") long idosoId, @Query("inicio") String inicio, @Query("fim") String fim,
                               @Query("medicamentoId") Long medicamentoId);

    @GET("api/doses/{id}")
    Call<Dose> dose(@Path("id") long doseId);

    @POST("api/doses/{id}/confirmacao")
    Call<ConfirmacaoResponse> confirmar(@Path("id") long doseId, @Body Requisicoes.Confirmar body);

    @GET("api/notificacoes")
    Call<List<Notificacao>> notificacoes();

    @GET("api/eu/agenda")
    Call<Agenda> agenda();
}
