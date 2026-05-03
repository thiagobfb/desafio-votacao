# Configuração nas principais IDEs Java

Pré-requisitos comuns:
- **JDK 21+** (a aplicação compila para Java 21; JDK 25 também funciona — ver [ADR-001](adr/001-java-21-lts.md)).
- **Maven 3.9+** (ou usar o que vem embarcado na IDE).
- **Lombok plugin** (a aplicação usa Lombok somente em entidades — ver [ADR-011](adr/011-lombok-em-entidades.md)).

Após clonar o repositório:

```bash
git clone <url-do-repo>
cd desafio-votacao
mvn -q verify          # opcional: confirma que tudo compila e os 60 testes passam
```

---

## IntelliJ IDEA

### Importar

1. **File → Open** → selecione a pasta `desafio-votacao`.
2. IntelliJ detecta `pom.xml` automaticamente. Aceite "Import as Maven project".
3. Aguarde "Indexing" e "Maven import" concluírem.

### Configurar JDK

1. **File → Project Structure → Project**.
2. **SDK:** selecione um JDK 21+ (ou adicione via `Add SDK → Download JDK`).
3. **Language level:** `21 - ...`.

### Habilitar Lombok

1. **File → Settings → Plugins** → procure `Lombok`. Já vem por padrão em versões recentes do IntelliJ.
2. **File → Settings → Build, Execution, Deployment → Compiler → Annotation Processors** → marque **"Enable annotation processing"**.

### Rodar a aplicação

- Abra `VotacaoApplication.java`.
- Clique no ícone ▶ ao lado de `public static void main(...)` → **Run 'VotacaoApplication.main()'**.
- Console: `Started VotacaoApplication in N seconds`.
- Acesse: http://localhost:8080/swagger-ui.html

### Rodar os testes

- Clique-direito em `src/test/java` → **Run 'All Tests'**.
- Ou `Ctrl+Shift+F10` em qualquer arquivo de teste.
- Resultados em **Run** tab; cobertura via **Run with Coverage**.

### Profile do Spring (opcional)

- **Run → Edit Configurations → VotacaoApplication**.
- Em **Active profiles**: `postgres` (se quiser usar PostgreSQL), ou deixe vazio para `default`.

---

## Eclipse

### Importar

1. **File → Import → Maven → Existing Maven Projects**.
2. **Root Directory:** selecione a pasta `desafio-votacao`.
3. Marque o `pom.xml` listado e clique em **Finish**.
4. Eclipse baixa as dependências (status na barra inferior).

### Configurar JDK

1. **Window → Preferences → Java → Installed JREs** → adicione um JDK 21+ se ainda não houver.
2. **Project → Properties → Java Build Path → Libraries** → confirme que aparece `JRE System Library [JavaSE-21]`.
3. **Project → Properties → Java Compiler** → marque **"Enable project specific settings"** → **Compiler compliance level: 21**.

### Habilitar Lombok

Eclipse não suporta Lombok nativamente. Instale o agente:

1. Baixe `lombok.jar` em https://projectlombok.org/download
2. Em terminal: `java -jar lombok.jar` → wizard gráfico → selecione a instalação do Eclipse → **Install/Update**.
3. Reinicie o Eclipse.
4. Verifique: o **About do Eclipse** deve mostrar `Lombok v1.18.x ... is installed`.

### Rodar a aplicação

1. Clique-direito em `VotacaoApplication.java` → **Run As → Java Application**.
2. (Recomendado) instale **Spring Tools 4** (Help → Eclipse Marketplace → "Spring Tools") para suporte rico a Boot.

### Rodar os testes

- Clique-direito no projeto ou em `src/test/java` → **Run As → JUnit Test**.
- Ou clique-direito em uma classe específica → **Run As → JUnit Test**.

### Profile do Spring (opcional)

- **Run → Run Configurations → Java Application → VotacaoApplication → Arguments → VM arguments**: `-Dspring.profiles.active=postgres`.

---

## VS Code

### Pré-requisitos (extensões)

Instale, na ordem:

1. **Extension Pack for Java** (Microsoft) — inclui Java Language Support, Maven, Debugger, Test Runner.
2. **Spring Boot Extension Pack** (VMware) — inclui Spring Boot Tools, Initializr, Dashboard.
3. **Lombok Annotations Support for VS Code** (não é mais estritamente necessário em versões recentes do `vscjava.vscode-java-pack`, mas a extensão dedicada é uma garantia).

### Importar

1. **File → Open Folder** → selecione `desafio-votacao`.
2. VS Code detecta `pom.xml`. A status bar mostra "Importing Maven project".
3. Aguarde o indicador desaparecer.

### Configurar JDK

VS Code usa o JDK definido em `java.jdt.ls.java.home` ou no `JAVA_HOME`. Para garantir:

1. `Ctrl+Shift+P → Java: Configure Java Runtime → Project JDKs` → aponte para um JDK 21+.
2. Ou edite `settings.json`:

   ```json
   {
     "java.configuration.runtimes": [
       { "name": "JavaSE-21", "path": "/caminho/para/jdk-21", "default": true }
     ]
   }
   ```

### Lombok

A extensão da Microsoft já delega para `Lombok Annotations Support` em versões recentes. Se aparecer "method getXxx() not found":

1. `Ctrl+Shift+P → Java: Clean Java Language Server Workspace` → **Restart and delete**.
2. Confirme que **Lombok Annotations Support for VS Code** está habilitado (Extensions view).

### Rodar a aplicação

- Abra `VotacaoApplication.java`.
- Acima do método `main`, clique em **Run** ou **Debug**.
- O **Spring Boot Dashboard** (sidebar) também lista a app — botão ▶.
- Acesse: http://localhost:8080/swagger-ui.html

### Rodar os testes

- Em qualquer arquivo de teste, clique no ícone ▶ ao lado de `@Test`.
- Ou abra a **Testing view** (sidebar) — roda a árvore inteira de testes.
- Comando: `Ctrl+Shift+P → Java: Run Tests in Current File`.

### Profile do Spring (opcional)

Crie/edite `.vscode/launch.json`:

```json
{
  "type": "java",
  "name": "Spring Boot — postgres",
  "request": "launch",
  "mainClass": "br.com.desafio.votacao.VotacaoApplication",
  "projectName": "votacao",
  "args": "--spring.profiles.active=postgres"
}
```

---

## Solução de problemas comuns

| Sintoma | Causa provável | Solução |
|---|---|---|
| `Cannot find symbol: getXxx()` em entidade | Lombok não habilitado | Instalar plugin/agente conforme IDE |
| `UnsupportedClassVersionError 65 < 61` | JDK < 21 | Atualizar Project SDK / Compiler compliance |
| `Port 8080 already in use` | App já rodando em outro processo | `lsof -i :8080` (Linux/Mac) ou Task Manager (Windows) e matar |
| `Could not parse migration ... V1__init.sql` | Banco H2 com schema antigo | Apagar `./data/votacao.mv.db` e reiniciar |
| Tests Mockito falham com `Could not modify all classes` | JDK > 21 sem overrides | Confirmar `byte-buddy 1.17.5` e `mockito 5.14.2` no `pom.xml` (já no projeto) |
