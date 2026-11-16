<div align="center">

# ⚡ MirTimeWipe (WIP / Не работает)

![Status](https://shields.io)
![Paper](https://shields.io)
![Java](https://shields.io)

**🇷🇺 Статус: Плагин временно не работает и находится в заморозке (вайпы проводятся вручную).**  
**🇺🇸 Status: The plugin is currently not working / archived (wipes are performed manually).**

</div>

---

## 🇷🇺 Русский | Описание проекта

**MirTimeWipe** — экспериментальный плагин для автоматизации сезонных вайпов на сервере **MIR-TIME** (Paper 1.21+).

### ⚠️ Текущий статус: НЕ РАБОТАЕТ
На данный момент плагин **не рекомендуется к использованию на рабочем сервере**:
* При автоматическом переносе файлов нарушается структура системных конфигураций некоторых плагинов (например, сбрасывается определение кастомных валют в `GemsEconomy` и сбиваются точки телепортации).
* Некорректно сбрасывается статистика игроков (`stats`).
* **Решение:** Проект переведён в архив; глобальные вайпы на сервере проводятся вручную при выключенном сервере для 100% сохранности данных.

### Задуманный функционал
* Двухэтапное GUI-подтверждение вайпа с 20-секундным таймером защиты от случайного нажатия.
* Доступ строго для операторов сервера (`OP`).
* Автоматическое резервное копирование старых миров и баз в папку `backup_wipe_<timestamp>`.

---

## 🇺🇸 English | Project Overview

**MirTimeWipe** is an experimental seasonal wipe automation plugin built for the **MIR-TIME** Minecraft server (Paper 1.21+).

### ⚠️ Current Status: NOT WORKING
This plugin is **currently broken and archived** — do not use it in production:
* Automatic file moving breaks internal data structures of third-party plugins (such as wiping custom currency definitions in `GemsEconomy` and breaking teleport locations).
* Player world statistics (`stats`) are not properly reset across restarts.
* **Workaround:** The plugin is discontinued; server wipes are performed manually while the server is stopped to ensure data integrity.

### Planned Features
* Two-step confirmation GUI with a 20-second safety unlock timer.
* Strict `OP`-only command access (`/wipe`).
* Automatic backup of old worlds and plugin databases into `backup_wipe_<timestamp>`.

---

<div align="center">
  <b>Project:</b> MIR-TIME &nbsp;|&nbsp; <b>Author:</b> Ars3nBar
</div>
