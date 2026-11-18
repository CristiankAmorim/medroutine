package br.ucsal.medroutine.data.local;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

@Database(entities = {DoseLocal.class, ConfirmacaoPendente.class}, version = 2, exportSchema = false)
public abstract class AppDatabase extends RoomDatabase {
    private static volatile AppDatabase instancia;

    static final Migration MIGRACAO_1_2 = new Migration(1, 2) {
        @Override
        public void migrate(SupportSQLiteDatabase db) {
            db.execSQL("ALTER TABLE dose_local ADD COLUMN peloCuidador INTEGER NOT NULL DEFAULT 0");
            db.execSQL("ALTER TABLE dose_local ADD COLUMN confirmadaPor TEXT");
        }
    };

    public abstract DoseLocalDao doses();

    public abstract ConfirmacaoPendenteDao confirmacoesPendentes();

    public static AppDatabase get(Context context) {
        if (instancia == null) {
            synchronized (AppDatabase.class) {
                if (instancia == null) {
                    instancia = Room.databaseBuilder(context.getApplicationContext(), AppDatabase.class,
                            "medroutine.db").addMigrations(MIGRACAO_1_2).build();
                }
            }
        }
        return instancia;
    }

    public void limpar() {
        doses().apagarTudo();
        confirmacoesPendentes().apagarTudo();
    }
}
