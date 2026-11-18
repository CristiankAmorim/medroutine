package br.ucsal.medroutine.repository;

import br.ucsal.medroutine.model.CodigoAcesso;
import br.ucsal.medroutine.model.TipoCodigo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.time.Instant;
import java.util.List;

public interface CodigoAcessoRepository extends JpaRepository<CodigoAcesso, Long> {
    @Query("select c from CodigoAcesso c where c.codigoHash = :hash and c.tipo = :tipo "
            + "and c.usadoEm is null and c.expiraEm > :agora")
    List<CodigoAcesso> findValidos(String hash, TipoCodigo tipo, Instant agora);

    @Modifying
    @Query("update CodigoAcesso c set c.usadoEm = :agora where c.usuario.id = :usuarioId "
            + "and c.tipo = :tipo and c.usadoEm is null")
    int invalidarAnteriores(Long usuarioId, TipoCodigo tipo, Instant agora);
}
