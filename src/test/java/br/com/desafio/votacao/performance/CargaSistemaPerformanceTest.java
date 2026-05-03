package br.com.desafio.votacao.performance;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.desafio.votacao.cpf.domain.CpfValidator;
import br.com.desafio.votacao.cpf.domain.StatusValidacaoCpf;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.ActiveProfiles;

/**
 * Carga sintética: dispara N votos em paralelo contra a aplicação real (Tomcat embarcado +
 * H2 in-memory) e mede throughput / latência (p50, p95, p99) + tempo da apuração.
 *
 * <p>Opt-in para não atrasar `mvn verify` rotineiro. Para rodar:
 * <pre>{@code
 *   mvn -Dperf.enabled=true -Dtest=CargaSistemaPerformanceTest test
 *   # parametrizando:
 *   mvn -Dperf.enabled=true -Dperf.votantes=20000 -Dperf.concorrencia=64 -Dtest=CargaSistemaPerformanceTest test
 * }</pre>
 *
 * <p>Saída na console mostra o resumo. Asserções garantem que nenhum voto é perdido (totalVotos
 * apurados = sucessos do envio) e que a taxa de erro fica abaixo de 1 % (margem para retries
 * esporádicos da camada de transporte HTTP).
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@EnabledIfSystemProperty(named = "perf.enabled", matches = "true")
class CargaSistemaPerformanceTest {

    private static final int VOTANTES = Integer.getInteger("perf.votantes", 10_000);
    private static final int CONCORRENCIA = Integer.getInteger("perf.concorrencia", 32);

    @TestConfiguration
    static class Config {
        @Bean
        @Primary
        public CpfValidator cpfValidatorPermissivo() {
            return cpf -> StatusValidacaoCpf.ABLE_TO_VOTE;
        }
    }

    @LocalServerPort
    private int porta;

    @Autowired
    private ObjectMapper mapper;

    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    @Test
    void cargaDeVotosEmParalelo() throws Exception {
        long pautaId = criarPauta("Performance " + System.currentTimeMillis(), "carga sintética");
        abrirSessao(pautaId, 60);

        ExecutorService exec = Executors.newFixedThreadPool(CONCORRENCIA);
        List<Future<Long>> futures = new ArrayList<>(VOTANTES);
        AtomicInteger erros = new AtomicInteger();
        ConcurrentHashMap<Integer, AtomicInteger> errosPorStatus = new ConcurrentHashMap<>();
        ConcurrentHashMap<Integer, String> exemploPorStatus = new ConcurrentHashMap<>();

        long inicio = System.nanoTime();
        for (int i = 0; i < VOTANTES; i++) {
            int idx = i;
            futures.add(exec.submit(() -> {
                long t0 = System.nanoTime();
                String cpf = String.format("%011d", idx);
                String body = "{\"cpf\":\"" + cpf + "\",\"voto\":\""
                        + (idx % 2 == 0 ? "SIM" : "NAO") + "\"}";
                HttpRequest req = HttpRequest.newBuilder()
                        .uri(URI.create("http://localhost:" + porta
                                + "/api/v1/pautas/" + pautaId + "/votos"))
                        .header("Content-Type", "application/json")
                        .POST(BodyPublishers.ofString(body))
                        .timeout(Duration.ofSeconds(10))
                        .build();
                HttpResponse<String> resp = http.send(req, BodyHandlers.ofString());
                if (resp.statusCode() != 201) {
                    erros.incrementAndGet();
                    int s = resp.statusCode();
                    errosPorStatus.computeIfAbsent(s, k -> new AtomicInteger()).incrementAndGet();
                    exemploPorStatus.putIfAbsent(s, resp.body());
                }
                return System.nanoTime() - t0;
            }));
        }

        List<Long> latenciasNs = new ArrayList<>(VOTANTES);
        for (Future<Long> f : futures) {
            latenciasNs.add(f.get());
        }
        long total = System.nanoTime() - inicio;
        exec.shutdown();

        long apuracaoIni = System.nanoTime();
        HttpRequest reqApur = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + porta
                        + "/api/v1/pautas/" + pautaId + "/resultado"))
                .GET()
                .build();
        HttpResponse<String> respApur = http.send(reqApur, BodyHandlers.ofString());
        long apuracaoNs = System.nanoTime() - apuracaoIni;

        latenciasNs.sort(Long::compare);
        long p50 = latenciasNs.get((int) (latenciasNs.size() * 0.50));
        long p95 = latenciasNs.get((int) (latenciasNs.size() * 0.95));
        long p99 = latenciasNs.get((int) (latenciasNs.size() * 0.99));
        double duracaoSeg = total / 1_000_000_000.0;

        JsonNode resultado = mapper.readTree(respApur.body());

        System.out.println();
        System.out.println("================ Performance Report ================");
        System.out.printf("  Votos enviados      : %,d (concorrência=%d)%n", VOTANTES, CONCORRENCIA);
        System.out.printf("  Erros HTTP          : %d (%.2f%%)%n", erros.get(), erros.get() * 100.0 / VOTANTES);
        errosPorStatus.forEach((status, contador) -> {
            String exemplo = exemploPorStatus.getOrDefault(status, "");
            String exemploCurto = exemplo.replaceAll("\\s+", " ")
                    .substring(0, Math.min(160, exemplo.length()));
            System.out.printf("    status %d            : %d  exemplo: %s%n",
                    status, contador.get(), exemploCurto);
        });
        System.out.printf("  Duração total       : %.2f s%n", duracaoSeg);
        System.out.printf("  Throughput          : %,.0f req/s%n", VOTANTES / duracaoSeg);
        System.out.printf("  Latência p50        : %.1f ms%n", p50 / 1_000_000.0);
        System.out.printf("  Latência p95        : %.1f ms%n", p95 / 1_000_000.0);
        System.out.printf("  Latência p99        : %.1f ms%n", p99 / 1_000_000.0);
        System.out.printf("  Apuração (1 query)  : %.1f ms%n", apuracaoNs / 1_000_000.0);
        System.out.printf("  totalVotos apurados : %d%n", resultado.get("totalVotos").asLong());
        System.out.println("====================================================");
        System.out.println();

        // Carga real pode ter retries esporádicos no transporte HTTP. Toleramos até 1 %.
        assertThat(erros.get())
                .as("erros HTTP devem ser inferiores a 1%% do total")
                .isLessThanOrEqualTo(VOTANTES / 100);
        assertThat(resultado.get("totalVotos").asLong())
                .as("nenhum voto pode se perder além dos contabilizados como erro")
                .isEqualTo(VOTANTES - erros.get());
    }

    private long criarPauta(String titulo, String descricao) throws Exception {
        String body = "{\"titulo\":\"" + titulo + "\",\"descricao\":\"" + descricao + "\"}";
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + porta + "/api/v1/pautas"))
                .header("Content-Type", "application/json")
                .POST(BodyPublishers.ofString(body))
                .build();
        HttpResponse<String> resp = http.send(req, BodyHandlers.ofString());
        assertThat(resp.statusCode()).isEqualTo(201);
        return mapper.readTree(resp.body()).get("id").asLong();
    }

    private void abrirSessao(long pautaId, int duracaoMin) throws Exception {
        String body = "{\"duracaoMinutos\":" + duracaoMin + "}";
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + porta
                        + "/api/v1/pautas/" + pautaId + "/sessoes"))
                .header("Content-Type", "application/json")
                .POST(BodyPublishers.ofString(body))
                .build();
        HttpResponse<String> resp = http.send(req, BodyHandlers.ofString());
        assertThat(resp.statusCode())
                .as("abertura de sessão deve responder 201, recebido %d body=%s",
                        resp.statusCode(), resp.body())
                .isEqualTo(201);
    }
}
