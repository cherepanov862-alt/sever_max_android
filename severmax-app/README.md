# Севермакс — Android-проект (WebView + SMS)

Готовый шаблон Android-приложения: интерфейс — это HTML/CSS/JS-макет
(`app/src/main/assets/www/index.html`), который открывается во встроенном
WebView, а кнопки управления дёргают нативный мост и реально отправляют
SMS-команды на номер SIM-карты подогревателя.

## Что внутри

- `app/src/main/assets/www/index.html` — весь интерфейс (тёмная тема,
  вкладки «Управление / Расписание / Журнал / Настройки», выбор марки и
  модели авто, загрузка своего фото машины, слайдеры, автозапуск по
  времени).
- `app/src/main/java/.../MainActivity.kt` — хост-активность: поднимает
  WebView и даёт JS вызывать `Android.sendSms(number, text)`, который
  реально шлёт SMS через `SmsManager` (с запросом разрешения `SEND_SMS`
  на лету).
- `.github/workflows/android-build.yml` — при каждом пуше в `main`
  собирает debug-APK на GitHub Actions и прикладывает его как артефакт
  сборки (Actions → выбранный запуск → Artifacts → `severmax-debug-apk`).

## ⚠️ Что обязательно доделать перед реальным использованием

Текст самих SMS-команд в `index.html` (в самом низу `<script>`, блок
"Мост к SMS") — **в основном заглушка, кроме одной подтверждённой**:

```js
const CMD_START  = "ON";              // заглушка, уточни формат
const CMD_STOP   = "OFF";             // заглушка, уточни формат
const CMD_STATUS = "STATUS";          // заглушка, уточни формат
const CMD_RESET  = "RESET";           // заглушка, уточни формат
const CMD_BOOST_OFF = "XHPZ OFF";     // подтверждённая команда — отключение режима догрева
function cmdTemp(t){ return "TEMP=" + t; }   // заглушка, уточни формат
function cmdBoost(hi, lo){ return "BOOST=" + hi + "," + lo; }  // заглушка, уточни формат
```

`CMD_BOOST_OFF = "XHPZ OFF"` уже реальная команда устройства — её менять
не нужно. Остальные (`ON`, `OFF`, `STATUS`, `RESET`, `TEMP=`, `BOOST=`)
подставь точным форматом, который реально понимает Severmax 5000-4mini
(в исходном приложении на скриншотах были, например, команды вида
`▶ Запуск`, `■ Стоп`, `📊 Статус`, `/temp 65`, `/boost 90 30` — но это,
судя по всему, синтаксис Telegram-бота, а не обязательно то, что ждёт
сам GSM-модуль по SMS). Если есть инструкция на модуль — команды и,
возможно, PIN/код авторизации перед текстом команды нужно взять оттуда.

Также в этом шаблоне **не реализовано**:
- чтение входящих SMS-ответов (для журнала и статуса) — разрешения
  `RECEIVE_SMS`/`READ_SMS` уже объявлены в манифесте, но обработчик
  (BroadcastReceiver) нужно дописать;
- реальная отправка команды по расписанию в фоне (сейчас вкладка
  «Расписание» только хранит список — чтобы SMS уходила именно в
  указанное время, даже когда приложение закрыто, нужен `AlarmManager`/
  `WorkManager` и foreground-сервис или `BroadcastReceiver`, который
  переживёт перезагрузку телефона).

## Как собрать APK

### Вариант 1 — через GitHub Actions (ничего не ставить локально)

1. Создай новый пустой репозиторий на GitHub.
2. Залей туда содержимое этой папки:
   ```bash
   cd severmax-app
   git init
   git add .
   git commit -m "Initial Severmax app"
   git branch -M main
   git remote add origin https://github.com/<твой_логин>/<репозиторий>.git
   git push -u origin main
   ```
3. Открой вкладку **Actions** в репозитории — сборка запустится
   автоматически. Через пару минут зайди в завершившийся запуск и в
   разделе **Artifacts** скачай `severmax-debug-apk` (внутри — `.apk`).
4. Перекинь APK на телефон и установи (разреши установку из
   неизвестных источников, если попросит).

### Вариант 2 — локально в Android Studio

1. Открой папку `severmax-app` как проект в Android Studio (Giraffe
   или новее, с установленным Android SDK 34).
2. Дай Gradle синхронизироваться (сам подтянет зависимости).
3. Run ▶ на подключённом телефоне/эмуляторе, либо
   Build → Build Bundle(s) / APK(s) → Build APK(s).

## Структура

```
severmax-app/
├─ .github/workflows/android-build.yml   ← автосборка APK
├─ app/
│  ├─ build.gradle
│  └─ src/main/
│     ├─ AndroidManifest.xml
│     ├─ java/com/severmax/app/MainActivity.kt
│     ├─ assets/www/index.html           ← весь UI приложения
│     └─ res/                            ← иконка, тема, строки
├─ build.gradle
├─ settings.gradle
└─ gradle.properties
```
