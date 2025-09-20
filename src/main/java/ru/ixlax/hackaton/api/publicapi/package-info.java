/**
 * Public API controllers (для пользовательского сайта)
 *
 * Здесь реализованы REST-эндпоинты, доступные обычным пользователям
 * - /api/public/incidents — список последних инцидентов
 * - /api/public/places — список убежищ и аптек
 * - /api/public/stream — SSE-стрим для обновления карты и новостей
 *
 * Эти эндпоинты будут использоваться пользовательским сайтом
 */
package ru.ixlax.hackaton.api.publicapi;