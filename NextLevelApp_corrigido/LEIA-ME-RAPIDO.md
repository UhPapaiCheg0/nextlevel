# Next Level — Como obter o APK no seu celular

## Opção 1 — Sem instalar nada (nuvem, ~10 min)
1. Crie uma conta gratuita em https://github.com
2. Clique em "New repository", nomeie `nextlevel` (público), crie.
3. Na página do repositório: "uploading an existing file" → arraste TODO o conteúdo
   da pasta extraída de NextLevelApp.zip (mantendo a estrutura de pastas).
4. Vá em "Actions" → clique no workflow "Gerar APK do Next Level" → "Run workflow".
5. Aguarde o ícone ficar verde (~8 min) → clique no workflow → em "Artifacts"
   baixe `nextlevel-apk`.
6. Extraia o ZIP, transfira o `app-debug.apk` para o celular e instale
   (permita "instalar apps de fontes desconhecidas" quando o Android pedir).

## Opção 2 — Android Studio (guia completo no PDF)
Siga o Guia_Next_Level_Android_Studio.pdf: abra o projeto, pressione ▶ e depois
Build → Build APK(s).

## Atenção
O APK gerado é de depuração (debug), ótimo para testes no seu aparelho. Para
publicar na Play Store é preciso gerar um APK/AAB assinado de release.
