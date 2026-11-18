package br.ucsal.medroutine.data.remote;

import android.content.Context;

import java.util.concurrent.TimeUnit;

import br.ucsal.medroutine.BuildConfig;
import br.ucsal.medroutine.session.SessionManager;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public final class ApiClient {
    private static ApiService service;
    private static String urlAtual;

    private ApiClient() { }

    public static synchronized ApiService get(Context context) {
        SessionManager sessao = SessionManager.get(context);
        String url = sessao.getUrlServidor();
        if (service == null || !url.equals(urlAtual)) {
            HttpLoggingInterceptor log = new HttpLoggingInterceptor();
            log.setLevel(BuildConfig.DEBUG ? HttpLoggingInterceptor.Level.BASIC : HttpLoggingInterceptor.Level.NONE);

            OkHttpClient client = new OkHttpClient.Builder()
                    .connectTimeout(10, TimeUnit.SECONDS)
                    .readTimeout(15, TimeUnit.SECONDS)
                    .addInterceptor(chain -> {
                        Request.Builder req = chain.request().newBuilder();
                        String token = sessao.getToken();
                        if (token != null) {
                            req.header("Authorization", "Bearer " + token);
                        }
                        return chain.proceed(req.build());
                    })
                    .addInterceptor(log)
                    .build();

            service = new Retrofit.Builder()
                    .baseUrl(url)
                    .client(client)
                    .addConverterFactory(GsonConverterFactory.create(GsonFactory.get()))
                    .build()
                    .create(ApiService.class);
            urlAtual = url;
        }
        return service;
    }
}
