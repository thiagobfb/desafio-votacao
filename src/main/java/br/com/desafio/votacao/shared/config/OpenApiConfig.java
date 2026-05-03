package br.com.desafio.votacao.shared.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(
        info = @Info(
                title = "API de Votação Cooperativista",
                version = "v1",
                description = """
                        Backend REST para gestão de pautas, sessões e votos em assembleias cooperativistas.
                        Especificação completa em `specs/001-sistema-votacao/spec.md`."""
        )
)
public class OpenApiConfig {
}
