package br.ucsal.medroutine.data.local;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Transaction;

import java.util.List;

@Dao
public abstract class DoseLocalDao {
    @Query("SELECT * FROM dose_local WHERE previstaEm >= :inicio AND previstaEm < :fim ORDER BY previstaEm")
    public abstract List<DoseLocal> doPeriodo(long inicio, long fim);

    @Query("SELECT * FROM dose_local WHERE estado IN ('PENDENTE', 'NAO_CONFIRMADA') AND previstaEm > :depoisDe ORDER BY previstaEm")
    public abstract List<DoseLocal> naoConfirmadasApos(long depoisDe);

    @Query("SELECT * FROM dose_local WHERE id = :id")
    public abstract DoseLocal porId(long id);

    @Query("SELECT id FROM dose_local WHERE aguardandoEnvio = 1")
    public abstract List<Long> idsAguardandoEnvio();

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    public abstract void inserir(List<DoseLocal> doses);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    public abstract void inserir(DoseLocal dose);

    @Query("DELETE FROM dose_local")
    public abstract void apagarTudo();

    @Query("UPDATE dose_local SET estado = :estado, confirmadaEm = :confirmadaEm, aguardandoEnvio = :aguardando WHERE id = :id")
    public abstract void atualizarEstado(long id, String estado, Long confirmadaEm, boolean aguardando);

    @Transaction
    public void substituirAgenda(List<DoseLocal> doServidor) {
        List<Long> aguardando = idsAguardandoEnvio();
        java.util.Map<Long, DoseLocal> locais = new java.util.HashMap<>();
        for (Long id : aguardando) {
            DoseLocal d = porId(id);
            if (d != null) {
                locais.put(id, d);
            }
        }
        apagarTudo();
        for (DoseLocal d : doServidor) {
            DoseLocal local = locais.get(d.id);
            if (local != null && !d.isConfirmada()) {
                d.estado = local.estado;
                d.confirmadaEm = local.confirmadaEm;
                d.aguardandoEnvio = true;
            }
        }
        inserir(doServidor);
    }
}
