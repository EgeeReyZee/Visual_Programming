# Visual Programming (MultiApp)

[![Platform](https://img.shields.io/badge/platform-Android-green.svg)](https://developer.android.com)
[![Language](https://img.shields.io/badge/language-Kotlin-purple.svg)](https://kotlinlang.org)
[![Build System](https://img.shields.io/badge/build-Gradle%20Kotlin%20DSL-blue.svg)](https://gradle.org)
[![Branch](https://img.shields.io/badge/branch-develop-orange.svg)]()

Репозиторий содержит кросс-функциональное Android-приложение, разрабатываемое в рамках дисциплины **«Визуальное программирование»**. Проект реализован на языке Kotlin и демонстрирует принципы построения современных графических интерфейсов, обработки событий, работы со встроенными датчиками и сетевого взаимодействия на мобильной платформе.

---

## 📱 Основные модули и функционал

Приложение спроектировано как комплексное решение и включает в себя следующие графические модули (Activities):

*   **Калькулятор (`CalculatorActivity`):** Полноценный визуальный калькулятор с кастомным интерфейсом кнопок и расширенным выбором диапазонов значений через `RangeSeekBar`[cite: 1].
*   **Медиаплеер (`MediaPlayerActivity`):** Музыкальный плеер со списком треков (`dialog_playlist`, `item_track`), элементами управления воспроизведением и анимациями[cite: 1].
*   **Сетевой модуль (`SocketsActivity` & `Sockets2Activity`):** Реализация визуального интерфейса для клиент-серверного взаимодействия посредством TCP/UDP сокетов[cite: 1].
*   **Геолокация (`LocationActivity`):** Работа с GPS/ГЛОНАСС координатами устройства и вывод данных на экран[cite: 1].
*   **Мониторинг сети (`CellMonitorService`):** Фоновый сервис для отслеживания состояния сотовой сети и уровня сигнала[cite: 1].
*   **Раздел в разработке (`DevelopingActivity`):** Заглушка для демонстрации плавных переходов и будущих визуальных компонентов[cite: 1].

---

## 🛠 Технологический стек

*   **Язык программирования:** Kotlin (объектно-ориентированная и функциональная разработка)[cite: 1]
*   **Архитектурные компоненты:** Android Architecture Components (Activities, Services)[cite: 1]
*   **Сборка проекта:** Gradle (Kotlin DSL, файлы сборки `*.gradle.kts`)[cite: 1]
*   **Интерфейс и дизайн:** XML Layouts, кастомные Drawable компоненты с эффектом матового стекла (`bg_glass_panel`), скругленные кнопки, кастомные селекторы для SeekBar[cite: 1].
*   **Анимации:** XML-анимации вращения (`an_rotate.xml`) и аниматоры состояний для интерактивных кнопок[cite: 1].

---

## 📂 Структура проекта

Основные компоненты приложения распределены следующим образом:

```text
app/src/main/
├── java/com/egeereyzee/multiapp/
│   ├── ui/theme/             # Стилизация приложения (Цвета, Шрифты, Темы)
│   ├── CalculatorActivity    # Логика работы калькулятора
│   ├── CellMonitorService    # Фоновая служба мониторинга сети
│   ├── LocationActivity      # Модуль работы с геопозицией
│   ├── MediaPlayerActivity   # Плеер и управление треками
│   ├── SocketsActivity       # Сетевое взаимодействие (Часть 1)
│   ├── Sockets2Activity      # Сетевое взаимодействие (Часть 2)
│   └── RangeSeekBar          # Кастомный UI-элемент выбора диапазона
└── res/
    ├── anim/                 # Анимации графических элементов
    ├── drawable/             # Кастомные UI-формы, стили кнопок, "стеклянные" панели
    ├── layout/               # XML-разметка всех экранов приложения
    └── values/               # Строковые ресурсы, цвета и темы оформления

```

---

## 🚀 Инструкция по сборке и запуску

### Требования

* **Android Studio** (рекомендуется версия Ladybug / Iguana или новее).
* **JDK 17** или выше.
* Устройство под управлением Android или настроенный эмулятор (API 26+).

### Сборка из терминала

1. Клонируйте репозиторий и перейдите в ветку разработки:

```bash
   git clone [https://github.com/EgeeReyZee/Visual_Programming.git](https://github.com/EgeeReyZee/Visual_Programming.git)
   cd Visual_Programming
   git checkout develop

```

2. Соберите проект с помощью Gradle Wrapper:
* **Для Linux / macOS:**



```bash
     ./gradlew assembleDebug
     ```
   * **Для Windows:**
```cmd
     gradlew.bat assembleDebug
     ```

3. Установите готовый APK-файл на подключенное устройство или эмулятор:
```bash
   ./gradlew installDebug

```

---

## 💻 Разработка и Git Flow

* Вся активная работа над лабораторными работами и модулями ведется в ветке `develop`.


* Ветка `main` используется исключительно для фиксации стабильных версий и сдачи готового проекта.

```

```
