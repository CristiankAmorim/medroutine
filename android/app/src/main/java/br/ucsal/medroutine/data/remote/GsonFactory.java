package br.ucsal.medroutine.data.remote;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;

import java.io.IOException;
import java.time.Instant;
import java.time.LocalDate;

public final class GsonFactory {
    private static Gson gson;

    private GsonFactory() { }

    public static synchronized Gson get() {
        if (gson == null) {
            gson = new GsonBuilder()
                    .registerTypeAdapter(Instant.class, new TypeAdapter<Instant>() {
                        @Override
                        public void write(JsonWriter out, Instant value) throws IOException {
                            if (value == null) out.nullValue(); else out.value(value.toString());
                        }

                        @Override
                        public Instant read(JsonReader in) throws IOException {
                            if (in.peek() == JsonToken.NULL) {
                                in.nextNull();
                                return null;
                            }
                            return Instant.parse(in.nextString());
                        }
                    }.nullSafe())
                    .registerTypeAdapter(LocalDate.class, new TypeAdapter<LocalDate>() {
                        @Override
                        public void write(JsonWriter out, LocalDate value) throws IOException {
                            if (value == null) out.nullValue(); else out.value(value.toString());
                        }

                        @Override
                        public LocalDate read(JsonReader in) throws IOException {
                            if (in.peek() == JsonToken.NULL) {
                                in.nextNull();
                                return null;
                            }
                            return LocalDate.parse(in.nextString());
                        }
                    }.nullSafe())
                    .create();
        }
        return gson;
    }
}
