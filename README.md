## SignService

Микросервис для подписи текста в формате **JWS compact** с алгоритмом заголовка `GOST3410-2015-512` (ГОСТ 34.10-2015), по примеру из `Example.kt`.

### API

- `POST /sign`

Тело запроса:

```json
{ "text": "Даю согласие на использование моих данных. 123456789012" }
```

Ответ:

```json
{
  "jws": "<header>.<payload>.<signature>",
  "kid": "<Subject DN>",
  "x5tS256": "<base64url(SHA-256(cert))>"
}
```

### Настройки (env / application.properties)

- `SIGN_KEYSTORE_PATH` (по умолчанию `/certs/cert.pfx`)
- `SIGN_KEYSTORE_PASSWORD` (по умолчанию `password`)
- `SIGN_PROVIDER` (по умолчанию `GAMMA`)
- `SIGN_SIGNATURE_ALGORITHM` (по умолчанию `GOST3410-2015-512`)
- `SIGN_JWS_ALGORITHM` (по умолчанию `GOST3410-2015-512`)

### Запуск в Docker

1) Положите `cert.pfx` в `./certs/` — **файл нужен и для сборки образа**: при `docker build` выполняется проверка `keytool -list -v -storetype PKCS12 -keystore ./certs/cert.pfx`.
2) Если пароль PFX не `1q2w3e4r5t`, передайте его при сборке:  
   `docker compose build --build-arg SIGN_KEYSTORE_PASSWORD=ваш_пароль`
3) Положите архив Tumar CSP в корень проекта как `./tumarCSP_linux64.tgz` (у вас он уже есть).
4) Запустите:

```bash
docker compose up --build
```

### Важно про локальные JAR'ы провайдера

`pom.xml` ожидает локальный `libs/GammaTechProvider.jar` (и другие `kz.gamma.*` зависимости при необходимости). Эти JAR'ы не коммитятся (см. `.gitignore`).

### Если Maven не качает зависимости (PKIX / corporate proxy)

Ошибка вида `PKIX path building failed` означает, что Java не доверяет сертификату прокси/интерцептора HTTPS. Обычно решается:

- добавлением корпоративного root CA в `cacerts` JDK, которым запускается Maven/Gradle, или
- настройкой Maven на использование корпоративного репозитория/зеркала.

