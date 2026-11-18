package br.ucsal.medroutine.data.local;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

@Dao
public interface ConfirmacaoPendenteDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    void inserir(ConfirmacaoPendente c);

    @Query("SELECT * FROM confirmacao_pendente ORDER BY horarioAparelho")
    List<ConfirmacaoPendente> todas();

    @Query("DELETE FROM confirmacao_pendente WHERE doseId = :doseId")
    void remover(long doseId);

    @Query("UPDATE confirmacao_pendente SET tentativas = tentativas + 1 WHERE doseId = :doseId")
    void incrementarTentativas(long doseId);

    @Query("DELETE FROM confirmacao_pendente")
    void apagarTudo();
}
