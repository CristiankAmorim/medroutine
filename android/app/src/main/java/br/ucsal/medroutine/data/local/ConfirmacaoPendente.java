package br.ucsal.medroutine.data.local;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "confirmacao_pendente")
public class ConfirmacaoPendente {
    @PrimaryKey
    public long doseId;
    public long horarioAparelho;
    public int tentativas;

    public ConfirmacaoPendente(long doseId, long horarioAparelho) {
        this.doseId = doseId;
        this.horarioAparelho = horarioAparelho;
    }
}
