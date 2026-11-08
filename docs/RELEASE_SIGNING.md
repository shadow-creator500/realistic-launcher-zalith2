# Assinatura do APK Release

O build **Debug** usa o keystore de depuração do projeto e continua disponível sem uma chave Release própria.

Para gerar um APK **Release**, configure uma chave de assinatura que você controla. Não reutilize uma chave de terceiros sem autorização e não envie o arquivo `.jks` nem as senhas em commits, issues ou conversas.

## Secrets do GitHub Actions

Em **Settings → Secrets and variables → Actions**, crie:

| Secret | Conteúdo |
|---|---|
| `ANDROID_KEYSTORE_BASE64` | Arquivo JKS convertido para Base64 (opcional; quando omitido, o Gradle tenta usar o keystore legado do projeto) |
| `ANDROID_KEY_ALIAS` | Alias da chave dentro do JKS (obrigatório quando o JKS personalizado é usado) |
| `STORE_PASSWORD` | Senha do keystore |
| `KEY_PASSWORD` | Senha da chave |

O workflow decodifica o JKS em um arquivo temporário chamado `ZalithLauncher/realistic_launcher_release.jks`, que está ignorado pelo Git. O arquivo não é escrito no histórico do repositório nem anexado como artefato.

## Cuidados

- Guarde uma cópia segura do JKS e das duas senhas. A perda da chave impede assinar atualizações compatíveis com instalações Release já distribuídas.
- Use sempre o mesmo JKS e o mesmo alias nas próximas versões do mesmo pacote.
- Nunca publique o conteúdo Base64 do JKS. Base64 não é criptografia; o valor deve ficar somente em GitHub Actions Secrets.
- Se usar o keystore personalizado, preencha os quatro secrets listados acima.
- Builds Debug não precisam desses secrets.

## CurseForge

A chave aprovada da API CurseForge é independente da assinatura. Ela continua sendo configurada no secret `CURSEFORGE_API_KEY`; não coloque uma chave privada diretamente no código-fonte.
